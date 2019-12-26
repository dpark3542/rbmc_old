package bots;

import game.BoardState;
import game.BotGame;
import game.Move;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class NeuralBot implements Bot {
	private int color; // 1 for white, -1 for black
	private int moveCount;

	private Set<BoardState> hypotheses;

	private Map<BoardState, Integer> whiteDepths, blackDepths;
	private Map<BoardState, Double> whiteScores, blackScores;

	private int sense;
	private Move move;

	private double[] genes = new double[3598];
	public double[][] convolutions = new double[28][15 * 15];

	public NeuralBot(boolean read) {
		if (!read) {
			Random rnd = new Random();
			for (int i = 0; i < genes.length; i++)
				genes[i] = Math.tan(rnd.nextInt());
		}
		else {
			File f = new File("Genes.txt");
			try {
				Scanner sc = new Scanner(f);
				for (int i = 0; i < genes.length; i++)
					genes[i] = sc.nextDouble()
							+ Math.tan(Math.random() * Math.PI) / 40;
				sc.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	public NeuralBot(NeuralBot p1, NeuralBot p2) {
		for (int i = 0; i < genes.length; i++)
			genes[i] = (Math.random() < .5 ? p1.genes[i] : p2.genes[i])
					+ Math.tan(Math.random() * Math.PI) / 40;
	}

	public void gameStart(int color, BotGame game) {
		this.color = color;
		moveCount = 0;
		hypotheses = new HashSet<>();
		hypotheses.add(new BoardState());
		whiteDepths = new HashMap<>();
		blackDepths = new HashMap<>();
		whiteScores = new HashMap<>();
		blackScores = new HashMap<>();
	}

	public void turnStart(int[] capture) {
		moveCount++;
		if (color == 1 && moveCount == 1) {
			return;
		}
		Set<BoardState> newHypotheses = new HashSet<>();
		// no capture
		if (capture[0] == -1) {
			for (BoardState bs : hypotheses) {
				List<Move> moveList = color == 1 ? bs.blackMoves() : bs.whiteMoves();
				for (Move move : moveList) {
					if (!move.isCapture) {
						if (color == 1) {
							bs.blackMove(move);
							newHypotheses.add(bs.copy());
							bs.undoBlackMove(move);
						}
						else {
							bs.whiteMove(move);
							newHypotheses.add(bs.copy());
							bs.undoWhiteMove(move);
						}
					}
				}
			}
		}
		else {
			int cap = 8 * (7 - capture[0]) + capture[1];
			for (BoardState bs : hypotheses) {
				List<Move> moveList = color == 1 ? bs.blackMoves() : bs.whiteMoves();
				for (Move move : moveList) {
					if (move.isCapture && move.to == cap) {
						if (color == 1) {
							bs.blackMove(move);
							newHypotheses.add(bs.copy());
							bs.undoBlackMove(move);
						}
						else {
							bs.whiteMove(move);
							newHypotheses.add(bs.copy());
							bs.undoWhiteMove(move);
						}
					}
				}
			}
		}
		hypotheses = newHypotheses;
	}

	public int[] sensePhase() {
		List<Map<String, Integer>> counts = new ArrayList<>();
		for (int i = 0; i < 55; i++)
			counts.add(new HashMap<String, Integer>());
		for (BoardState bs : hypotheses) {
			for (int i = 1; i <= 6; i++) {
				for (int j = 1; j <= 6; j++) {
					int x = 8 * j + i;
					String s = bs.sense(x);
					counts.get(x).put(s, counts.get(x).getOrDefault(s, 0) + 1);
				}
			}
		}
		int pos = 0, minE = 0;
		for (int i = 0; i < 54; i++) {
			int E = 0;
			for (int c : counts.get(i).values()) {
				E += c * c;
			}
			if (E >= minE) {
				pos = i;
				minE = E;
			}
		}
		sense = pos;
		return new int[] {7 - pos / 8, pos % 8};
	}

	public void senseResults(int[][] results) {
		String s = "";
		for (int i = 0; i < 9; i++) {
			s += (char) results[i][2];
		}
		for (Iterator<BoardState> it = hypotheses.iterator(); it.hasNext();) {
			BoardState bs = it.next();
			if (!s.equals(bs.sense(sense))) {
				it.remove();
			}
		}
	}

	private static double min(double a, double b) {
		return a < b ? a : b;
	}

	private static double max(double a, double b) {
		return a > b ? a : b;
	}

	private double alphabeta(BoardState bs, int depth, double alpha, double beta, boolean maximizingPlayer) {
		if (depth == 0 || bs.endGame)
			return bs.score(genes, convolutions, color);
		if (maximizingPlayer) {
			double v = -Double.MAX_VALUE;
			for (Move m : bs.whiteMoves()) {
				bs.whiteMove(m);
				if (whiteDepths.containsKey(bs) && depth - 1 <= whiteDepths.get(bs)) {
					v = max(v, whiteScores.get(bs));
				}
				else {
					double score = alphabeta(bs, depth - 1, alpha, beta, false);
					v = max(v, score);
					whiteDepths.put(bs, depth - 1);
					whiteScores.put(bs, score);
				}
				alpha = max(alpha, v);
				bs.undoWhiteMove(m);
				if (beta <= alpha)
					break;
			}
			return v;
		} else {
			double v = Double.MAX_VALUE;
			for (Move m : bs.blackMoves()) {
				bs.blackMove(m);
				if (blackDepths.containsKey(bs) && depth - 1 <= blackDepths.get(bs)) {
					v = min(v, blackScores.get(bs));
				}
				else {
					double score = alphabeta(bs, depth - 1, alpha, beta, true);
					v = min(v, score);
					blackDepths.put(bs, depth - 1);
					blackScores.put(bs, score);
				}
				beta = min(beta, v);
				bs.undoBlackMove(m);
				if (beta <= alpha)
					break;
			}
			return v;
		}
	}

	public int[] movePhase() {
		Map<Move, Double> scores = new HashMap<>();
		for (BoardState bs : hypotheses) {
			List<Move> moveList = color == 1 ? bs.whitePseudoMoves() : bs.blackPseudoMoves();
			for (Move move : moveList) {
				if (color == 1) {
					bs.whiteMove(move);
				}
				else {
					bs.blackMove(move);
				}
				double score = alphabeta(bs, 3, -Double.MAX_VALUE, Double.MAX_VALUE, color != 1);
				scores.put(move, scores.getOrDefault(bs, 0.0) + score);
				if (color == 1) {
					bs.undoWhiteMove(move);
				}
				else {
					bs.undoBlackMove(move);
				}
			}
		}
		Move bestMove = new Move();
		double maxScore = -Double.MAX_VALUE;
		for (Iterator<Map.Entry<Move, Double>> it = scores.entrySet().iterator(); it.hasNext();) {
			Map.Entry<Move, Double> entry = it.next();
			Move move = entry.getKey();
			double score = entry.getValue();
			if (score >= maxScore) {
				bestMove = move;
				maxScore = score;
			}
		}
		move = bestMove;
		int yi = 7 - (bestMove.from / 8), xi = bestMove.from % 8, yf = 7 - (bestMove.to / 8), xf = bestMove.to % 8;
		return new int[] {yi, xi, yf, xf};
	}

	public void moveResults(int[] turnRes) {
		int ey = turnRes[0], ex = turnRes[1], cy = turnRes[2], cx = turnRes[3];
		int end = 8 * (7 - ey) + ex, cap = 8 * (7 - cy) + cx;
		Set<BoardState> newHypotheses = new HashSet<>();
		// move successful
		if (end == move.to) {
			for (BoardState bs : hypotheses) {
				if (color == 1) {
					for (Move m : bs.whiteMoves()) {
						if (move.equals(m) && ((cx != -1) == m.isCapture)) {
							bs.whiteMove(m);
							newHypotheses.add(bs);
							break;
						}
					}
				}
				else {
					for (Move m : bs.blackMoves()) {
						if (move.equals(m) && ((cx != -1) == m.isCapture)) {
							bs.blackMove(m);
							newHypotheses.add(bs);
							break;
						}
					}
				}
			}
		}
		// invalid move
		else if (end == move.from) {
			for (BoardState bs : hypotheses) {
				if (color == 1) {
					if (!bs.whiteMoves().contains(move)) {
						newHypotheses.add(bs);
					}
				}
				else {
					if (!bs.blackMoves().contains(move)) {
						newHypotheses.add(bs);
					}
				}
			}
		}
		// blocked move
		else {
			Move blockedMove = new Move(move.p, move.from, end);
			blockedMove.r = move.r;
			for (BoardState bs : hypotheses) {
				if (color == 1) {
					List<Move> moveList = bs.whiteMoves();
					if (moveList.contains(move)) {
						continue;
					}
					for (Move m : moveList) {
						if (blockedMove.equals(m) && ((cx != -1) == m.isCapture)) {
							bs.whiteMove(m);
							newHypotheses.add(bs);
							break;
						}
					}
				}
				else {
					List<Move> moveList = bs.blackMoves();
					if (moveList.contains(move)) {
						continue;
					}
					for (Move m : moveList) {
						if (blockedMove.equals(m) && ((cx != -1) == m.isCapture)) {
							bs.blackMove(m);
							newHypotheses.add(bs);
							break;
						}
					}
				}
			}
		}
		hypotheses = newHypotheses;
	}

	public char promotePhase() {
		return move.r.toString().charAt(0);
	}

	public void promoteResults() {

	}

	public void gameOver() {

	}

//	public void printBoardStates() {
//		if (hypotheses.size() == 0) {
//			System.out.println("No more hypotheses.");
//		}
//		for (BoardState bs : hypotheses) {
//			System.out.println("Hypothesis:\n" + bs.toString());
//		}
//	}

	public BotGame getGame() {
		return null;
	}
}
