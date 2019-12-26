package bots;

import engine.BoardState;
import engine.Move;
import engine.Piece;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

// TODO
public class HumanBot implements Bot {
    private boolean color; // color of our player, true for white, false for black
    private int moveCount; // fullmove number
    private BoardState bs; // current boardstate
    private InputReader in = new InputReader(System.in);

    private static String convert(int x) {
        return "abcdefgh".substring(x % 8, x % 8 + 1) + (x / 8 + 1);
    }

    private static int convert(String s) {
        return (s.charAt(1) - (int) '1') * 8 + s.charAt(0) - (int) 'a';
    }

    public void gameStart(boolean color) {
        System.out.println("Your color: " + (color ? "white" : "black"));
        bs = new BoardState();
    }

    public void gameStart(boolean color, BoardState bs) {
        System.out.println("Your color: " + (color ? "white" : "black"));
        this.bs = bs.copy();
    }

    public void turnStart(boolean capture, int sq) {
        if (capture) {
            System.out.println("Piece captured at " + convert(sq));
        }
        // remove piece at square
        System.out.println(bs);
        System.out.print("Your turn: ");
    }

    public int sensePhase() {
        System.out.print("Sense: ");
        return convert(in.next());
    }

    public void senseResults(int results) {
        // change bs according to sense results
        System.out.println(bs);
    }

    public Move movePhase() {
        System.out.println("Move: ");
        int from = convert(in.next()), to = convert(in.next());
        // handle promotion
        // return move
        return null;
    }

    public void moveResults(int end, boolean capture) {

    }

    public Piece promotePhase() {
        return null;
    }

    public void promoteResults() {

    }

    public void gameOver() {
        System.out.println("Game over.");
        color = false;
        moveCount = -1;
        bs = null;
    }

    private class InputReader {
        private BufferedReader br;
        private StringTokenizer st;

        public InputReader(InputStream stream) {
            br = new BufferedReader(new InputStreamReader(stream), 32768);
            st = null;
        }

        public boolean hasNext() {
            return st.hasMoreTokens();
        }

        public String next() {
            while (st == null || !st.hasMoreTokens()) {
                try {
                    st = new StringTokenizer(br.readLine());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return st.nextToken();
        }

        public boolean nextBoolean() {
            String s = next();
            if (s.equals("true") || s.equals("True")) {
                return true;
            }
            if (s.equals("false") || s.equals("False")) {
                return false;
            }
            throw new RuntimeException("Input is not boolean.");
        }

        public int nextInt() {
            return Integer.parseInt(next());
        }
    }
}
