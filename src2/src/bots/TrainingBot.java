package bots;

import game.BoardState;
import game.BotGame;
import game.Move;
import game.Piece;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

import rbmc.RBMC;

public class TrainingBot implements Bot {
	public double[] genes = new double[103];
	public double[][] convolutions = new double[26][15 * 15];
	public int score = 0;
	private int color; // 1 for white, -1 for black
	private int moveCount;
	public Move[] lastMoves = new Move[2];

	private BoardState boardState;
	private boolean kingside, queenside;

	private Move move;

	public TrainingBot(boolean read) {
		if (!read) {
			Random rnd = new Random();
			for (int i = 0; i < genes.length; i++)
				genes[i] = (Math.random() - .5);
			for (int i = 0; i < convolutions.length; i++)
				for (int j = 0; j < convolutions[i].length; j++)
					convolutions[i][j] = (Math.random() - .5);
		}
		else {
			File f = new File("Genes.txt");
			try {
				Scanner sc = new Scanner(f);
				for (int i = 0; i < genes.length; i++)
					genes[i] = sc.nextDouble() + (Math.random() - .5) / 1000;
				for (int i = 0; i < convolutions.length; i++)
					for (int j = 0; j < convolutions[i].length; j++)
						convolutions[i][j] = sc.nextDouble() + (Math.random() - .5) / 1000;
				sc.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	public TrainingBot() {
		for (int i = 0; i < genes.length; i++)
			genes[i] = RBMC.bots.get((int) (Math.random() * RBMC.bots.size())).genes[i] + (Math.random() - .5) / 10;
		for (int i = 0; i < convolutions.length; i++)
			for (int j = 0; j < convolutions[i].length; j++)
				convolutions[i][j] = RBMC.bots.get((int)(Math.random() * RBMC.bots.size())).convolutions[i][j] + (Math.random() - .5) / 10;
	}

	public void gameStart(int color, BotGame game) {
		this.color = color;
		moveCount = 0;
	}

	public void turnStart(int[] capture) {
		moveCount++;
	}

	public int[] sensePhase() {
		return new int[] {-1, -1};
	}

	public void senseResults(int[][] results) {
		char[][] tmp = new char[8][8];
		for (int[] r : results) {
			tmp[r[0]][r[1]] = (char) r[2];
		}
		if (color == 1) {
			boardState = new BoardState(tmp, kingside, queenside, true, true, -1, -1);
		}
		else {
			boardState = new BoardState(tmp, true, true, kingside, queenside, -1, -1);
		}
	}

	public int[] movePhase() {
		double maxScore = -Double.MAX_VALUE;
		Move bestMove = new Move();
		List<Move> moveList = color == 1 ? boardState.whiteMoves() : boardState.blackMoves();
		for (Move move : moveList) {
			if(lastMoves[1] != null && move.basicEquals(lastMoves[1]))
				continue;
			if (move.isInvalidMove) {
				continue;
			}
			if (color == 1) {
				boardState.whiteMove(move);
			}
			else {
				boardState.blackMove(move);
			}
			double score = color * alphabeta(boardState, 2, -Double.MAX_VALUE, Double.MAX_VALUE, color != 1);
			//double score = color * boardState.score(genes, convolutions);
			if (score >= maxScore) {
				bestMove = move;
				maxScore = score;
			}
			if (color == 1) {
				boardState.undoWhiteMove(move);
			}
			else {
				boardState.undoBlackMove(move);
			}
		}
		move = bestMove;
		int yi = 7 - (bestMove.from / 8), xi = bestMove.from % 8, yf = 7 - (bestMove.to / 8), xf = bestMove.to % 8;
		lastMoves[1] = lastMoves[0];
		lastMoves[0] = move;
		return new int[] {yi, xi, yf, xf};
	}


	public void moveResults(int[] turnRes) {
		int ey = turnRes[0], ex = turnRes[1], cy = turnRes[2], cx = turnRes[3];
		int end = 8 * (7 - ey) + ex, cap = 8 * (7 - cy) + cx;
		if (move.from != end) {
			if (move.p == Piece.KING) {
				kingside = false;
				queenside = false;
			}
			else if (move.p == Piece.ROOK) {
				if (move.from == 7 || move.from == 63) {
					kingside = false;
				}
				else if (move.from == 0 || move.from == 56) {
					queenside = false;
				}
			}
		}
	}

	public double alphabeta(BoardState bs, int depth, double alpha, double beta, boolean maximizingPlayer) {
		// TODO What if at depth - 2 we have seen 2 moves that end in same
		// position?
		if (depth == 0 || bs.endGame)
			return bs.score(genes, convolutions);
		if (maximizingPlayer) {
			double v = -Double.MAX_VALUE;
			for (Move m : bs.whiteMoves()) {
				bs.whiteMove(m);
				v = max(v, alphabeta(bs, depth - 1, alpha, beta, !maximizingPlayer));
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
				v = min(v, alphabeta(bs, depth - 1, alpha, beta, !maximizingPlayer));
				beta = min(beta, v);
				bs.undoBlackMove(m);
				if (beta <= alpha)
					break;
			}
			return v;
		}
	}

	public static double min(double a, double b) {
		return a < b ? a : b;
	}

	public static double max(double a, double b) {
		return a > b ? a : b;
	}

	public char promotePhase() {
		return color == 1 ? 'Q' : 'q';
	}

	public void promoteResults() {

	}

	public void gameOver() {

	}

	public BotGame getGame() {
		return null;
	}
}
