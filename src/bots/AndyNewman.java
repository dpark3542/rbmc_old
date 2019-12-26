package bots;

import engine.BoardState;
import engine.Move;
import engine.Piece;

import java.util.*;

// TODO: consider:
// utilizing clock
// promoting to rook or bishop instead of knight or queen to break enemy's multi-hypothesis
// continually making invalid moves if enemy has multi-hypothesis
public class AndyNewman implements Bot {
    private long seed;
    private boolean color; // color of our player, true for white, false for black
    private int moveCount; // fullmove number
    private Set<BoardState> hypotheses; // set of all possible board states
    private int sense; // sense we made
    private Move move; // move we made

    public AndyNewman() {
        seed = System.currentTimeMillis();
    }

    public AndyNewman(long seed) {
        this.seed = seed;
    }

    public void gameStart(boolean color) {
        this.color = color;
        moveCount = 0;
        hypotheses = new HashSet<>();
        hypotheses.add(new BoardState());
    }

    public void gameStart(boolean color, BoardState bs) {
        this.color = color;
        moveCount = 0;
        hypotheses = new HashSet<>();
        hypotheses.add(bs.copy());
    }

    public void turnStart(boolean capture, int sq) {
        moveCount++;
        if (color && moveCount == 1) {
            return;
        }

        Set<BoardState> newHypotheses = new HashSet<>();
        // no pieces got captured
        if (!capture) {
            for (BoardState bs : hypotheses) {
                List<Move> moveList = color ? bs.blackMoves() : bs.whiteMoves();
                for (Move m : moveList) {
                    if (!m.isCapture) {
                        if (color) {
                            bs.blackMove(m);
                            newHypotheses.add(bs.copy());
                            bs.undoBlackMove(m);
                        }
                        else {
                            bs.whiteMove(m);
                            newHypotheses.add(bs.copy());
                            bs.undoWhiteMove(m);
                        }
                    }
                }
            }
        }
        // piece got captured
        else {
            for (BoardState bs : hypotheses) {
                List<Move> moveList = color ? bs.blackCaptureMoves() : bs.whiteCaptureMoves();
                for (Move m : moveList) {
                    if ((m.isBlockedMove && m.block == sq) || (!m.isBlockedMove && m.to == sq) || (move.isDPMove && m.isEPCapture && m.to % 8 == move.to % 8)) {
                        if (color) {
                            bs.blackMove(m);
                            newHypotheses.add(bs.copy());
                            bs.undoBlackMove(m);
                        }
                        else {
                            bs.whiteMove(m);
                            newHypotheses.add(bs.copy());
                            bs.undoWhiteMove(m);
                        }
                    }
                }
            }
        }
        hypotheses = newHypotheses;
    }

    public int sensePhase() {
        List<Map<Integer, Integer>> counts = new ArrayList<>();
        for (int i = 0; i < 55; i++) {
            counts.add(new HashMap<>());
        }
        for (BoardState bs : hypotheses) {
            for (int i = 1; i <= 6; i++) {
                for (int j = 1; j <= 6; j++) {
                    int x = 8 * j + i, s = bs.sense(color, x);
                    counts.get(x).put(s, counts.get(x).getOrDefault(s, 0) + 1);
                }
            }
        }
        int pos = 0;
        double maxScore = 0;
        for (int i = 1; i <= 6; i++) {
            for (int j = 1; j <= 6; j++) {
                int x = 8 * j + i;
                double expectedValue = 0;
                int minimum = 0;
                for (int c : counts.get(x).values()) {
                    expectedValue += c * c;
                    minimum = Math.max(minimum, c);
                }
                expectedValue /= hypotheses.size();

                double score = expectedValue;

                if (maxScore < score) {
                    maxScore = score;
                    pos = x;
                }
            }
        }
        sense = pos;
        return sense;
    }

    public void senseResults(int results) {
        for (Iterator<BoardState> it = hypotheses.iterator(); it.hasNext(); ) {
            BoardState bs = it.next();
            if (results != bs.sense(color, sense)) {
                it.remove();
            }
        }
    }

    private static final int maxScore = 1000, minScore = -1000;

    private static int staticEvaluate(BoardState bs) {
        if (bs.getBlackKing() == 0) {
            return maxScore;
        }
        if (bs.getWhiteKing() == 0) {
            return minScore;
        }
        int sc = 0;
        sc += 9 * (Long.bitCount(bs.getWhiteQueens()) - Long.bitCount(bs.getBlackQueens()));
        sc += 7 * (Long.bitCount(bs.getWhiteKnights()) - Long.bitCount(bs.getBlackKnights()));
        sc += 5 * (Long.bitCount(bs.getWhiteRooks()) - Long.bitCount(bs.getBlackRooks()));
        sc += 3 * (Long.bitCount(bs.getWhiteBishops()) - Long.bitCount(bs.getBlackBishops()));
        sc += Long.bitCount(bs.getWhitePawns()) - Long.bitCount(bs.getBlackPawns());
        return sc;
    }

    private int alphaBeta(BoardState bs, boolean player, int alpha, int beta, int depth) {
        if (depth == 0) {
            return staticEvaluate(bs);
        }
        if (player) {
            int v = minScore;
            for (Move m : bs.whiteMoves()) {
                bs.whiteMove(m);

                v = Math.max(v, alphaBeta(bs, false, alpha, beta, depth - 1));
                alpha = Math.max(alpha, v);

                bs.undoWhiteMove(m);

                if (beta <= alpha) {
                    break;
                }
            }
            return v;
        }
        else {
            int v = maxScore;
            for (Move m : bs.blackMoves()) {
                bs.blackMove(m);

                v = Math.min(v, alphaBeta(bs, true, alpha, beta, depth - 1));
                beta = Math.min(beta, v);

                bs.undoBlackMove(m);

                if (beta <= alpha) {
                    break;
                }
            }
            return v;
        }
    }

    public Move movePhase() {
        Map<Move, Integer> scores = new HashMap<>(); // contains scores of each move

        BoardState tmp = hypotheses.iterator().next();
        for (Move pm : color ? tmp.whitePseudoMoves() : tmp.blackPseudoMoves()) {
            scores.put(pm.copy(), color ? maxScore : minScore);
        }

        for (BoardState bs : hypotheses) {
            List<Move> moveList = color ? bs.whitePseudoMoves() : bs.blackPseudoMoves();
            for (Move pm : moveList) {
                Move m = color ? bs.identifyWhiteMove(pm.copy()) : bs.identifyBlackMove(pm.copy());
                if (color) {
                    bs.whiteMove(m);
                }
                else {
                    bs.blackMove(m);
                }

                int depth = 0;
                int score = alphaBeta(bs, !color, minScore, maxScore, depth);
                if (m.isInvalidMove) {
                    score += color ? -100 : 100;
                }
                scores.put(pm, color ? Math.min(scores.get(pm), score) : Math.max(scores.get(pm), score)); // scores of worst possible scenarios are taken for each move

                if (color) {
                    bs.undoWhiteMove(m);
                }
                else {
                    bs.undoBlackMove(m);
                }
            }
        }

        List<Move> bestMoves = new ArrayList<>();
        int bestScore = color ? minScore : maxScore;
        for (Map.Entry<Move, Integer> entry : scores.entrySet()) {
            Move cur = entry.getKey();
            int score = entry.getValue();
            if ((color && bestScore < score) || (!color && bestScore > score)) {
                bestMoves = new ArrayList<>();
                bestMoves.add(cur);
                bestScore = score;
            }
            else if (bestScore == score) {
                bestMoves.add(cur);
            }
        }
        Random rand = new Random(seed);
        move = bestMoves.get(rand.nextInt(bestMoves.size()));
        return move;
    }

    public void moveResults(int end, boolean capture) {
        Set<BoardState> newHypotheses = new HashSet<>();
        boolean isInvalid, isBlocked;

        // move successful
        if (end == move.to) {
            isInvalid = false;
            isBlocked = false;
        }
        // invalid move
        else if (end == move.from) {
            isInvalid = true;
            isBlocked = false;
        }
        // blocked move
        else {
            isInvalid = false;
            isBlocked = true;
        }

        for (BoardState bs : hypotheses) {
            Move m = color ? bs.identifyWhiteMove(move.copy()) : bs.identifyBlackMove(move.copy());
            if (m.isCapture == capture && m.isInvalidMove == isInvalid && m.isBlockedMove == isBlocked && m.isBlockedMove == (m.block == end)) {
                if (color) {
                    bs.whiteMove(m);
                }
                else {
                    bs.blackMove(m);
                }
                bs.clearCache();
                newHypotheses.add(bs);
            }
        }

        hypotheses = newHypotheses;
    }

    public Piece promotePhase() {
        return move.r;
    }

    public void promoteResults() {
    }

    public void gameOver() {
        color = false;
        moveCount = -1;
        hypotheses = null;
        sense = -1;
        move = null;
    }

    public boolean checkHypotheses(BoardState bs) {
        return hypotheses.contains(bs);
    }

    public int numHypotheses() {
        return hypotheses.size();
    }

    public void printHypotheses() {
        for (BoardState bs : hypotheses) {
            System.out.println(bs);
        }
    }
}
