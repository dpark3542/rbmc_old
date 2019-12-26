package game;

import bots.AndyNewman;
import bots.Bot;
import engine.BoardState;
import engine.Move;

import java.util.Scanner;

/**
 * Class for starting a local game between two bots.
 */
// TODO: timer
public class LocalGame {
    private Bot a, b; // a is white, b is black

    public LocalGame(Bot a, Bot b) {
        this.a = a;
        this.b = b;
    }

    public int run(int moveCountLimit, boolean verbose, boolean debug) {
        BoardState bs = new BoardState();
        boolean capture = false;
        int sq = -1, sense;
        Move move;

        a.gameStart(true);
        b.gameStart(false);
        for (int mc = 1; mc <= moveCountLimit; mc++) {
            // white turn
            if (verbose) {
                System.out.println("White move " + mc);
                System.out.println(bs);
            }
            if (debug) {
                System.out.println("White hypotheses: " + ((AndyNewman) a).numHypotheses());
            }
            a.turnStart(capture, sq);
            if (debug) {
                System.out.println("White hypotheses: " + ((AndyNewman) a).numHypotheses());
                if (!((AndyNewman) a).checkHypotheses(bs)) {
                    System.out.println("White does not have correct hypothesis.");
                    System.out.println("Print all hypotheses? (y/n): ");
                    Scanner sc = new Scanner(System.in);
                    if (sc.next().equals("y")) {
                        ((AndyNewman) a).printHypotheses();
                    }
                    return -1;
                }
            }
            sense = a.sensePhase();
            if (verbose) {
                System.out.println("White sense: " + "abcdefgh".substring(sense % 8, sense % 8 + 1) + (sense / 8 + 1));
            }
            a.senseResults(bs.sense(true, sense));
            if (debug) {
                System.out.println("White hypotheses: " + ((AndyNewman) a).numHypotheses());
                if (!((AndyNewman) a).checkHypotheses(bs)) {
                    System.out.println("White does not have correct hypothesis.");
                    System.out.println("Print all hypotheses? (y/n): ");
                    Scanner sc = new Scanner(System.in);
                    if (sc.next().equals("y")) {
                        ((AndyNewman) a).printHypotheses();
                    }
                    return -1;
                }
            }
            move = bs.identifyWhiteMove(a.movePhase());
            if (verbose) {
                System.out.println("White move: " + move);
            }
            // TODO: handle promotion after, not during movePhase
            bs.whiteMove(move);
            if (bs.getBlackKing() == 0) {
                a.gameOver();
                b.gameOver();
                if (verbose) {
                    System.out.println("White wins by capture.");
                    System.out.println(bs);
                }
                return -1;
            }
            if (move.isInvalidMove) {
                a.moveResults(move.from, false);
            }
            else if (move.isBlockedMove) {
                a.moveResults(move.block, move.isCapture);
            }
            else {
                a.moveResults(move.to, move.isCapture);
            }
            if (move.isCapture) {
                capture = true;
                if (move.isEPCapture) {
                    sq = 8 * (move.from / 8) + (move.to % 8);
                }
                else {
                    sq = move.block == -1 ? move.to : move.block;
                }
            }
            else {
                capture = false;
                sq = -1;
            }
            if (move.isPromotion) {
                a.promotePhase();
                a.promoteResults();
            }
            if (debug) {
                System.out.println("White hypotheses: " + ((AndyNewman) a).numHypotheses());
                if (!((AndyNewman) a).checkHypotheses(bs)) {
                    System.out.println("White does not have correct hypothesis.");
                    System.out.println("Print all hypotheses? (y/n): ");
                    Scanner sc = new Scanner(System.in);
                    if (sc.next().equals("y")) {
                        ((AndyNewman) a).printHypotheses();
                    }
                    return -1;
                }
            }
            // black turn
            if (verbose) {
                System.out.println("Black move " + mc);
                System.out.println(bs);
            }
            if (debug) {
                System.out.println("Black hypotheses: " + ((AndyNewman) b).numHypotheses());
            }
            b.turnStart(capture, sq);
            if (debug) {
                System.out.println("Black hypotheses: " + ((AndyNewman) b).numHypotheses());
                if (!((AndyNewman) b).checkHypotheses(bs)) {
                    System.out.println("Black does not have correct hypothesis.");
                    System.out.println("Print all hypotheses? (y/n): ");
                    Scanner sc = new Scanner(System.in);
                    if (sc.next().equals("y")) {
                        ((AndyNewman) b).printHypotheses();
                    }
                    return -1;
                }
            }
            sense = b.sensePhase();
            if (verbose) {
                System.out.println("Black sense: " + "abcdefgh".substring(sense % 8, sense % 8 + 1) + (sense / 8 + 1));
            }
            b.senseResults(bs.sense(false, sense));
            if (debug) {
                System.out.println("Black hypotheses: " + ((AndyNewman) b).numHypotheses());
                if (!((AndyNewman) b).checkHypotheses(bs)) {
                    System.out.println("Black does not have correct hypothesis.");
                    System.out.println("Print all hypotheses? (y/n): ");
                    Scanner sc = new Scanner(System.in);
                    if (sc.next().equals("y")) {
                        ((AndyNewman) b).printHypotheses();
                    }
                    return -1;
                }
            }
            move = bs.identifyBlackMove(b.movePhase());
            if (verbose) {
                System.out.println("Black move: " + move);
            }
            bs.blackMove(move);
            if (bs.getWhiteKing() == 0) {
                a.gameOver();
                b.gameOver();
                if (verbose) {
                    System.out.println("Black wins by capture.");
                    System.out.println(bs);
                }
                return 1;
            }
            if (move.isInvalidMove) {
                b.moveResults(move.from, false);
            }
            else if (move.isBlockedMove) {
                b.moveResults(move.block, move.isCapture);
            }
            else {
                b.moveResults(move.to, move.isCapture);
            }
            if (move.isCapture) {
                capture = true;
                if (move.isEPCapture) {
                    sq = 8 * (move.from / 8) + (move.to % 8);
                }
                else {
                    sq = move.block == -1 ? move.to : move.block;
                }
            }
            else {
                capture = false;
                sq = -1;
            }
            if (move.isPromotion) {
                b.promotePhase();
                b.promoteResults();
            }
            if (debug) {
                System.out.println("Black hypotheses: " + ((AndyNewman) b).numHypotheses());
                if (!((AndyNewman) b).checkHypotheses(bs)) {
                    System.out.println("Black does not have correct hypothesis.");
                    System.out.println("Print all hypotheses? (y/n): ");
                    Scanner sc = new Scanner(System.in);
                    if (sc.next().equals("y")) {
                        ((AndyNewman) b).printHypotheses();
                    }
                    return -1;
                }
            }
        }
        if (verbose) {
            System.out.println("Tie");
            System.out.println(bs);
        }
        return 0;
    }

    public static void main(String[] args) {
        LocalGame bg = new LocalGame(new AndyNewman(), new AndyNewman());
        bg.run(40, true, true);
    }
}