import game.BoardState;
import game.Move;

import java.util.List;

public class Perft {
	public static long search(BoardState bs, int depth, int color) throws Exception {
		if (depth == 0) {
			return 1;
		}
		long tot = 0;
		List<Move> l = color == 1 ? bs.whiteMoves() : bs.blackMoves();
		for (Move m : l) {
//			BoardState copy = bs.copy();
			if (color == 1) {
				bs.whiteMove(m);
			}
			else {
				bs.blackMove(m);
			}
			tot += search(bs, depth - 1, -color);
			if (color == 1) {
				bs.undoWhiteMove(m);
			}
			else {
				bs.undoBlackMove(m);
			}
//			if (!bs.equals(copy) || !bs.toString().equals(copy.toString())) {
//				System.out.println(copy);
//				System.out.println(bs);
//				System.out.println(m.from + " " + m.to + " " + m.p + " " + m.isEPCapture);
//				throw new Exception("test");
//			}
		}
		return tot;
	}

	public static void main(String[] args) throws Exception {
		BoardState bs = new BoardState();
		long x = System.nanoTime();
		long n = search(bs, 4, 1);
		long y = System.nanoTime();
		System.out.println(bs.equals(new BoardState()));
		System.out.println("Nodes: " + n);
		System.out.println("Time: " + (y - x) / 1000000000.0);
		System.out.println("nps: " + n * 1000000000.0 / (y - x));
	}
}
