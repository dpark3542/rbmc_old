package game;

import bots.Bot;
import engine.Move;
import net.maritimecloud.internal.core.javax.json.Json;
import net.maritimecloud.internal.core.javax.json.JsonObject;
import net.maritimecloud.internal.core.javax.json.JsonReader;
import net.maritimecloud.internal.core.javax.json.JsonValue;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Class for starting a game with another bot/player on the JHU APL rbmc server.
 */
public class ServerGame {
    private Bot bot;
    private String gameId, playerKey;
    private static final String rbmcURL = "http://rbmc-dev:8080";
    private boolean color;

    public ServerGame(Bot bot, String gameId, String playerKey) {
        this.bot = bot;
        this.gameId = gameId;
        this.playerKey = playerKey;
    }

    private static String convert(int x) {
        return "abcdefgh".substring(x % 8, x % 8 + 1) + (x / 8 + 1);
    }

    private static int convert(String s) {
        return (s.charAt(1) - (int) '1') * 8 + s.charAt(0) - (int) 'a';
    }

    /**
     * Makes a GET request to specified url.
     *
     * @param url URL to make request to
     * @return JsonObject containing server response
     */
    private static JsonObject get(String url) throws IOException {
        URL website = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) website.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("GET");

        JsonReader in = Json.createReader(new InputStreamReader(connection.getInputStream()));
        JsonObject input = in.readObject();
        in.close();

        connection.disconnect();

        return input;
    }

    /**
     * Makes a POST request to specified url.
     *
     * @param url URL to make request to
     * @param obj Json data to send
     * @return JsonObject containing server response
     */
    private static JsonObject post(String url, JsonObject obj) throws IOException {
        URL website = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) website.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");

        OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
        out.write(obj.toString());
        out.flush();
        out.close();

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            System.err.println(responseCode);
            throw new RuntimeException();
        }

        JsonReader in = Json.createReader(new InputStreamReader(connection.getInputStream()));
        JsonObject input = in.readObject();
        in.close();

        connection.disconnect();

        return input;
    }

    public void run() throws IOException {
        // TODO: gameStart (initialize color)
        while (true) {
            JsonObject response = get(rbmcURL + "/games/info?gameId=" + gameId + "&playerKey=" + playerKey);
            if (response.getJsonObject("gameInfo").getString("playerTurn").equals(color ? "white" : "black")) {
                switch (response.getJsonObject("gameInfo").getString("playerPhase")) {
                    case "sense":
                        bot.turnStart(response.getJsonObject("gameInfo").getJsonObject("news").getBoolean("pieceCaptured"), convert(response.getJsonObject("gameInfo").getJsonObject("news").getString("captureSquare", "a1")));
                        int x = bot.sensePhase();
                        if (!response.getJsonObject("gameInfo").getString("playerPhase").equals("sense")) {
                            throw new RuntimeException();
                        }
                        JsonObject results = post(rbmcURL + "/games/act", Json.createObjectBuilder()
                                .add("playerKey", playerKey)
                                .add("gameId", gameId)
                                .add("pieceId", "")
                                .add("action", "sense")
                                .add("parameters", Json.createObjectBuilder()
                                        .add("san", convert(x))
                                        .add("x", x % 8)
                                        .add("y", x / 8)
                                        .add("senseType", "truthWindow")
                                        .add("effectedLocations", Json.createArrayBuilder()
                                                .add(Json.createObjectBuilder()
                                                        .add("san", convert(x - 9))
                                                        .add("x", (x - 9) % 8)
                                                        .add("y", (x - 9) / 8))
                                                .add(Json.createObjectBuilder()
                                                        .add("san", convert(x - 1))
                                                        .add("x", (x - 1) % 8)
                                                        .add("y", (x - 1) / 8))
                                                .add(Json.createObjectBuilder()
                                                        .add("san", convert(x + 7))
                                                        .add("x", (x + 7) % 8)
                                                        .add("y", (x + 7) / 8))
                                                .add(Json.createObjectBuilder()
                                                        .add("san", convert(x - 8))
                                                        .add("x", (x - 8) % 8)
                                                        .add("y", (x - 8) / 8))
                                                .add(Json.createObjectBuilder()
                                                        .add("san", convert(x))
                                                        .add("x", x % 8)
                                                        .add("y", x / 8))
                                                .add(Json.createObjectBuilder()
                                                        .add("san", convert(x + 8))
                                                        .add("x", (x + 8) % 8)
                                                        .add("y", (x + 8) / 8))
                                                .add(Json.createObjectBuilder()
                                                        .add("san", convert(x - 7))
                                                        .add("x", (x - 7) % 8)
                                                        .add("y", (x - 7) / 8))
                                                .add(Json.createObjectBuilder()
                                                        .add("san", convert(x + 1))
                                                        .add("x", (x + 1) % 8)
                                                        .add("y", (x + 1) / 8))
                                                .add(Json.createObjectBuilder()
                                                        .add("san", convert(x + 9))
                                                        .add("x", (x + 9) % 8)
                                                        .add("y", (x + 9) / 8))
                                        )
                                )
                                .add("inferredFEN", "")
                                .build()
                        );
                        // TODO: senseResults
                        break;
                    case "move":
                        Move move = bot.movePhase();
                        JsonObject repsonse;
                        for (JsonValue action : get(rbmcURL + "/games/validActions?gameId=" + gameId + "&playerKey=" + playerKey).getJsonObject("validActions").getJsonArray("actionsList")) {
                            JsonObject act = (JsonObject) action;
                            if (act.getJsonObject("parameters").getJsonObject("from").getString("san").equals(convert(move.from)) && act.getJsonObject("parameters").getJsonObject("to").getString("san").equals(convert(move.to))) {
                                response = post(rbmcURL + "/games/act", Json.createObjectBuilder()
                                        .add("playerKey", playerKey)
                                        .add("gameId", gameId)
                                        .add("pieceId", "")
                                        .add("action", "move")
                                        .add("parameters", act.getJsonObject("parameters"))
                                        .add("inferredFEN", "")
                                        .build()
                                );
                                break;
                            }
                        }
                        // TODO: moveResults
                        break;
                    case "promote":
                        // TODO
                        break;
                }
            }
        }
    }
}
