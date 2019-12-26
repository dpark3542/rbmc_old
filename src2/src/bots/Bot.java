package bots;

import game.BotGame;

/**
 * Created by parkd1 on 7/13/2017.
 */
public interface Bot {
    void gameStart(int color, BotGame game);
    void turnStart(int[] capture);
    int[] sensePhase();
    void senseResults(int[][] results);
    int[] movePhase();
    void moveResults(int[] turnRes);
    char promotePhase();
    void promoteResults();
    void gameOver();
    BotGame getGame();
}
