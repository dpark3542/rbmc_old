package game;

import java.util.*;

/**
 * Created by dpark3542 on 7/27/2017.
 */
public class BoardState {
	// bitboards
	private long board, whiteBoard, blackBoard;
	private long whitePawns, whiteKnights, whiteBishops, whiteRooks, whiteQueens, whiteKing;
	private long blackPawns, blackKnights, blackBishops, blackRooks, blackQueens, blackKing;

	// mailboxes
	private Piece[] whiteMailbox, blackMailbox;

	// castling rights and en passant
	private boolean whiteKingside, whiteQueenside, blackKingside, blackQueenside;
	private boolean ep;
	private int eps;
	// previous values
	private ArrayDeque<Boolean> prevWhiteKingside, prevWhiteQueenside, prevBlackKingside, prevBlackQueenside;
	private ArrayDeque<Boolean> prevEp;
	private ArrayDeque<Integer> prevEps;

	// end game
	public boolean endGame;
	private boolean hasWhiteKing, hasBlackKing;

	// zobrist hashing
	//	private int hc;
	//	private static final long seed = 0;

	// pre-calculated bitboards
	private static final long[] sq;
	private static final Map<Long, Integer> convert;
	private static final long[] rankMask, fileMask, diagonalMask, antiDiagonalMask;
	private static final long[] kingMoves, knightMoves;
	private static final long[] northMask, eastMask, southMask, westMask, northEastMask, southEastMask, southWestMask, northWestMask;
	//    private static final Map<Piece, List<Integer>> wt, bt;

	static {
		sq = new long[64];
		convert = new HashMap<>();
		sq[0] = Long.MIN_VALUE; // square 0 must be separately initialized because Math.pow(2, 63) returns 2^63-1
		convert.put(Long.MIN_VALUE, 0);
		for (int i = 1; i < 64; i++) {
			sq[i] = (long) Math.pow(2, 63 - i);
			convert.put((long) Math.pow(2, 63 - i), i);
		}

		rankMask = new long[64];
		for (int j = 0; j < 8; j++) {
			long x = 0;
			for (int i = 0; i < 8; i++) {
				x += sq[8 * j + i];
			}
			for (int i = 0; i < 8; i++) {
				rankMask[8 * j + i] += x - sq[8 * j + i];
			}
		}

		fileMask = new long[64];
		for (int i = 0; i < 64; i++) {
			for (int j = 0; j < 8; j++) {
				fileMask[i] += sq[8 * j + (i % 8)];
			}
			fileMask[i] -= sq[i];
		}

		diagonalMask = new long[64];
		for (int j = 0; j < 8; j++) {
			for (int i = 0; i < 8; i++) {
				for (int dx = 1; i + dx < 8 && j + dx < 8; dx++) {
					diagonalMask[8 * j + i] += sq[8 * (j + dx) + i + dx];
				}
				for (int dx = -1; i + dx >= 0 && j + dx >= 0; dx--) {
					diagonalMask[8 * j + i] += sq[8 * (j + dx) + i + dx];
				}
			}
		}

		antiDiagonalMask = new long[64];
		for (int j = 0; j < 8; j++) {
			for (int i = 0; i < 8; i++) {
				for (int dx = 1; i + dx < 8 && j - dx >= 0; dx++) {
					antiDiagonalMask[8 * j + i] += sq[8 * (j - dx) + i + dx];
				}
				for (int dx = -1; i + dx >= 0 && j - dx < 8; dx--) {
					antiDiagonalMask[8 * j + i] += sq[8 * (j - dx) + i + dx];
				}
			}
		}

		kingMoves = new long[64];
		for (int j = 0; j < 8; j++) {
			for (int i = 0; i < 8; i++) {
				for (int y = j - 1; y <= j + 1; y++) {
					for (int x = i - 1; x <= i + 1; x++) {
						if (0 <= x && x < 8 && 0 <= y && y < 8 && (x != i || y != j)) {
							kingMoves[8 * j + i] += sq[8 * y + x];
						}
					}
				}
			}
		}

		knightMoves = new long[64];
		int[] dx = { 1, 2, 2, 1, -1, -2, -2, -1 }, dy = { 2, 1, -1, -2, -2, -1, 1, 2 };
		for (int j = 0; j < 8; j++) {
			for (int i = 0; i < 8; i++) {
				for (int k = 0; k < 8; k++) {
					int x = i + dx[k], y = j + dy[k];
					if (0 <= x && x < 8 && 0 <= y && y < 8) {
						knightMoves[8 * j + i] += sq[8 * y + x];
					}
				}
			}
		}

		northMask = new long[64];
		for (int i = 0; i < 64; i++) {
			for (int x = i + 8; x < 64; x += 8) {
				northMask[i] += sq[x];
			}
		}

		eastMask = new long[64];
		for (int i = 0; i < 64; i++) {
			for (int x = i + 1; x % 8 != 0; x++) {
				eastMask[i] += sq[x];
			}
		}

		southMask = new long[64];
		for (int i = 0; i < 64; i++) {
			for (int x = i - 8; x >= 0; x -= 8) {
				southMask[i] += sq[x];
			}
		}

		westMask = new long[64];
		for (int i = 0; i < 64; i++) {
			for (int x = i - 1; x >= 0 && x % 8 != 7; x--) {
				westMask[i] += sq[x];
			}
		}

		northEastMask = new long[64];
		for (int j = 0; j < 8; j++) {
			for (int i = 0; i < 8; i++) {
				for (int d = 1; i + d < 8 && j + d < 8; d++) {
					northEastMask[8 * j + i] += sq[8 * (j + d) + i + d];
				}
			}
		}

		southWestMask = new long[64];
		for (int j = 0; j < 8; j++) {
			for (int i = 0; i < 8; i++) {
				for (int d = -1; i + d >= 0 && j + d >= 0; d--) {
					southWestMask[8 * j + i] += sq[8 * (j + d) + i + d];
				}
			}
		}

		southEastMask = new long[64];
		for (int j = 0; j < 8; j++) {
			for (int i = 0; i < 8; i++) {
				for (int d = 1; i + d < 8 && j - d >= 0; d++) {
					southEastMask[8 * j + i] += sq[8 * (j - d) + i + d];
				}
			}
		}

		northWestMask = new long[64];
		for (int j = 0; j < 8; j++) {
			for (int i = 0; i < 8; i++) {
				for (int d = -1; i + d >= 0 && j - d < 8; d--) {
					northWestMask[8 * j + i] += sq[8 * (j - d) + i + d];
				}
			}
		}

		//        Random rng = new Random(seed);
		//        wt = new HashMap<>();
		//        bt = new HashMap<>();
		//        for (Piece p : Piece.values()) {
		//        	List<Integer> l = new ArrayList<>();
		//        	for (int i = 0; i < 64; i++) {
		//        		l.add(rng.nextInt());
		//			}
		//        	wt.put(p, l);
		//        	if (p == Piece.NONE) {
		//        		bt.put(p, l);
		//			}
		//			else {
		//				List<Integer> m = new ArrayList<>();
		//				for (int i = 0; i < 64; i++) {
		//					m.add(rng.nextInt());
		//				}
		//				bt.put(p, m);
		//			}
		//		}
	}

	public BoardState() {
		board = 0b1111111111111111000000000000000000000000000000001111111111111111L;
		whiteBoard = 0b1111111111111111000000000000000000000000000000000000000000000000L;
		blackBoard = 0b0000000000000000000000000000000000000000000000001111111111111111L;

		whitePawns = 0b0000000011111111000000000000000000000000000000000000000000000000L;
		whiteKnights = 0b0100001000000000000000000000000000000000000000000000000000000000L;
		whiteBishops = 0b0010010000000000000000000000000000000000000000000000000000000000L;
		whiteRooks = 0b1000000100000000000000000000000000000000000000000000000000000000L;
		whiteQueens = 0b0001000000000000000000000000000000000000000000000000000000000000L;
		whiteKing = 0b0000100000000000000000000000000000000000000000000000000000000000L;

		blackPawns = 0b0000000000000000000000000000000000000000000000001111111100000000L;
		blackKnights = 0b0000000000000000000000000000000000000000000000000000000001000010L;
		blackBishops = 0b0000000000000000000000000000000000000000000000000000000000100100L;
		blackRooks = 0b0000000000000000000000000000000000000000000000000000000010000001L;
		blackQueens = 0b0000000000000000000000000000000000000000000000000000000000010000L;
		blackKing = 0b0000000000000000000000000000000000000000000000000000000000001000L;

		whiteMailbox = new Piece[] { Piece.ROOK, Piece.KNIGHT, Piece.BISHOP, Piece.QUEEN, Piece.KING, Piece.BISHOP, Piece.KNIGHT, Piece.ROOK, Piece.PAWN, Piece.PAWN, Piece.PAWN, Piece.PAWN, Piece.PAWN, Piece.PAWN, Piece.PAWN, Piece.PAWN, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE };
		blackMailbox = new Piece[] { Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.PAWN, Piece.PAWN, Piece.PAWN, Piece.PAWN, Piece.PAWN, Piece.PAWN, Piece.PAWN, Piece.PAWN, Piece.ROOK, Piece.KNIGHT, Piece.BISHOP, Piece.QUEEN, Piece.KING, Piece.BISHOP, Piece.KNIGHT, Piece.ROOK };

		whiteKingside = true;
		whiteQueenside = true;
		blackKingside = true;
		blackQueenside = true;
		prevWhiteKingside = new ArrayDeque<>();
		prevWhiteQueenside = new ArrayDeque<>();
		prevBlackKingside = new ArrayDeque<>();
		prevBlackQueenside = new ArrayDeque<>();

		ep = false;
		endGame = false;

		prevEp = new ArrayDeque<>();
		prevEps = new ArrayDeque<>();

		hasWhiteKing = hasBlackKing = true;

		//		hc = 0;
	}

	public BoardState(String fen) {
		hasWhiteKing = hasBlackKing = false;
		String[] s = fen.split(" ");
		whiteMailbox = new Piece[64];
		blackMailbox = new Piece[64];
		for (int i = 0; i < 64; i++) {
			whiteMailbox[i] = Piece.NONE;
			blackMailbox[i] = Piece.NONE;
		}
		String[] t = s[0].split("/");
		for (int j = 7; j >= 0; j--) {
			int cur = 8 * j;
			for (int i = 0; i < t[7 - j].length(); i++) {
				char c = t[7 - j].charAt(i);
				if ('A' <= c && c <= 'Z') {
					whiteBoard += sq[cur];
					board += sq[cur];
				} else if ('a' <= c && c <= 'z') {
					blackBoard += sq[cur];
					board += sq[cur];
				}
				switch (c) {
				case 'P':
					whitePawns += sq[cur];
					whiteMailbox[cur] = Piece.PAWN;
					cur++;
					break;
				case 'N':
					whiteKnights += sq[cur];
					whiteMailbox[cur] = Piece.KNIGHT;
					cur++;
					break;
				case 'B':
					whiteBishops += sq[cur];
					whiteMailbox[cur] = Piece.BISHOP;
					cur++;
					break;
				case 'R':
					whiteRooks += sq[cur];
					whiteMailbox[cur] = Piece.ROOK;
					cur++;
					break;
				case 'Q':
					whiteQueens += sq[cur];
					whiteMailbox[cur] = Piece.QUEEN;
					cur++;
					break;
				case 'K':
					whiteKing += sq[cur];
					whiteMailbox[cur] = Piece.KING;
					hasWhiteKing = true;
					cur++;
					break;
				case 'p':
					blackPawns += sq[cur];
					blackMailbox[cur] = Piece.PAWN;
					cur++;
					break;
				case 'n':
					blackKnights += sq[cur];
					blackMailbox[cur] = Piece.KNIGHT;
					cur++;
					break;
				case 'b':
					blackBishops += sq[cur];
					blackMailbox[cur] = Piece.BISHOP;
					cur++;
					break;
				case 'r':
					blackRooks += sq[cur];
					blackMailbox[cur] = Piece.ROOK;
					cur++;
					break;
				case 'q':
					blackQueens += sq[cur];
					blackMailbox[cur] = Piece.QUEEN;
					cur++;
					break;
				case 'k':
					blackKing += sq[cur];
					blackMailbox[cur] = Piece.KING;
					hasBlackKing = true;
					cur++;
					break;
				default:
					cur += c - '0';
				}
			}
		}

		whiteKingside = s[2].contains("K");
		whiteQueenside = s[2].contains("Q");
		blackKingside = s[2].contains("k");
		blackQueenside = s[2].contains("q");

		ep = !s[3].equals("-");
		if (ep) {
			eps = 8 * (s[3].charAt(1) - '1') + s[3].charAt(0) - 'a';
		}

		prevWhiteKingside = new ArrayDeque<>();
		prevWhiteQueenside = new ArrayDeque<>();
		prevBlackKingside = new ArrayDeque<>();
		prevBlackQueenside = new ArrayDeque<>();

		ep = !s[3].equals("-");
		if (ep) {
			eps = 8 * (s[3].charAt(1) - '1') + s[3].charAt(0) - 'a';
		} else {
			eps = 0;
		}
		prevEp = new ArrayDeque<>();
		prevEps = new ArrayDeque<>();

		endGame = !(hasWhiteKing && hasBlackKing);
	}

	// NOTE: square mappings of input and bitboards are different!
	public BoardState(char[][] c, boolean wk, boolean wq, boolean bk, boolean bq, int epx, int epy) {
		hasWhiteKing = hasBlackKing = false;
		whiteMailbox = new Piece[64];
		blackMailbox = new Piece[64];
		for (int i = 0; i < 64; i++) {
			whiteMailbox[i] = Piece.NONE;
			blackMailbox[i] = Piece.NONE;
		}
		int cur = 0;
		for (int j = 7; j >= 0; j--) {
			for (int i = 0; i < 8; i++) {
				char ch = c[j][i];
				if ('A' <= ch && ch <= 'Z') {
					whiteBoard += sq[cur];
					board += sq[cur];
				} else if ('a' <= ch && ch <= 'z') {
					blackBoard += sq[cur];
					board += sq[cur];
				}
				switch (ch) {
				case 'P':
					whitePawns += sq[cur];
					whiteMailbox[cur] = Piece.PAWN;
					cur++;
					break;
				case 'N':
					whiteKnights += sq[cur];
					whiteMailbox[cur] = Piece.KNIGHT;
					cur++;
					break;
				case 'B':
					whiteBishops += sq[cur];
					whiteMailbox[cur] = Piece.BISHOP;
					cur++;
					break;
				case 'R':
					whiteRooks += sq[cur];
					whiteMailbox[cur] = Piece.ROOK;
					cur++;
					break;
				case 'Q':
					whiteQueens += sq[cur];
					whiteMailbox[cur] = Piece.QUEEN;
					cur++;
					break;
				case 'K':
					whiteKing += sq[cur];
					whiteMailbox[cur] = Piece.KING;
					hasWhiteKing = true;
					cur++;
					break;
				case 'p':
					blackPawns += sq[cur];
					blackMailbox[cur] = Piece.PAWN;
					cur++;
					break;
				case 'n':
					blackKnights += sq[cur];
					blackMailbox[cur] = Piece.KNIGHT;
					cur++;
					break;
				case 'b':
					blackBishops += sq[cur];
					blackMailbox[cur] = Piece.BISHOP;
					cur++;
					break;
				case 'r':
					blackRooks += sq[cur];
					blackMailbox[cur] = Piece.ROOK;
					cur++;
					break;
				case 'q':
					blackQueens += sq[cur];
					blackMailbox[cur] = Piece.QUEEN;
					cur++;
					break;
				case 'k':
					blackKing += sq[cur];
					blackMailbox[cur] = Piece.KING;
					hasBlackKing = true;
					cur++;
					break;
				default:
					cur++;
					break;
				}
			}

			whiteKingside = wk;
			whiteQueenside = wq;
			blackKingside = bk;
			blackQueenside = bq;

			ep = epx != -1;
			if (ep) {
				this.eps = 8 * (7 - epy) + epx;
			}

			prevWhiteKingside = new ArrayDeque<>();
			prevWhiteQueenside = new ArrayDeque<>();
			prevBlackKingside = new ArrayDeque<>();
			prevBlackQueenside = new ArrayDeque<>();

			ep = epx != -1;
			if (ep) {
				eps = 8 * (7 - epy) + epx;
			} else {
				eps = 0;
			}

			prevEp = new ArrayDeque<>();
			prevEps = new ArrayDeque<>();

			endGame = !(hasWhiteKing && hasBlackKing);
		}
	}

	public BoardState(BoardState bs) {
		board = bs.board;
		whiteBoard = bs.whiteBoard;
		blackBoard = bs.blackBoard;

		whitePawns = bs.whitePawns;
		whiteKnights = bs.whiteKnights;
		whiteBishops = bs.whiteBishops;
		whiteRooks = bs.whiteRooks;
		whiteQueens = bs.whiteQueens;
		whiteKing = bs.whiteKing;

		blackPawns = bs.blackPawns;
		blackKnights = bs.blackKnights;
		blackBishops = bs.blackBishops;
		blackRooks = bs.blackRooks;
		blackQueens = bs.blackQueens;
		blackKing = bs.blackKing;

		whiteMailbox = bs.whiteMailbox.clone();
		blackMailbox = bs.blackMailbox.clone();

		whiteKingside = bs.whiteKingside;
		whiteQueenside = bs.whiteQueenside;
		blackKingside = bs.blackKingside;
		blackQueenside = bs.blackQueenside;

		ep = bs.ep;
		eps = bs.eps;

		prevWhiteKingside = bs.prevWhiteKingside.clone();
		prevWhiteQueenside = bs.prevWhiteQueenside.clone();
		prevBlackKingside = bs.prevBlackKingside.clone();
		prevBlackQueenside = bs.prevBlackQueenside.clone();

		prevEp = bs.prevEp.clone();
		prevEps = bs.prevEps.clone();

		endGame = bs.endGame;

		hasWhiteKing = bs.hasWhiteKing;
		hasBlackKing = bs.hasBlackKing;

		//		hc = bs.hc;
	}

	private long slideAttacks(int x, long mask) {
		long forward = board & mask, reverse = Long.reverse(forward);
		forward -= sq[x];
		reverse -= sq[x ^ 63];
		forward ^= Long.reverse(reverse);
		forward &= mask;
		return forward;
	}

	private long pseudoWhiteSlideAttacks(int x, long mask) {
		long forward = whiteBoard & mask, reverse = Long.reverse(forward);
		forward -= sq[x];
		reverse -= sq[x ^ 63];
		forward ^= Long.reverse(reverse);
		forward &= mask;
		return forward;
	}

	private long pseudoBlackSlideAttacks(int x, long mask) {
		long forward = blackBoard & mask, reverse = Long.reverse(forward);
		forward -= sq[x];
		reverse -= sq[x ^ 63];
		forward ^= Long.reverse(reverse);
		forward &= mask;
		return forward;
	}

	public List<Move> whiteMoves() {
		List<Move> moves = new ArrayList<>();
		if (endGame) {
			return moves;
		}
		boolean possibleInvalidMove = false;
		for (long b = whiteBoard; b != 0; b &= b - 1) {
			int from = convert.get(b & -b);
			switch (whiteMailbox[from]) {
			case KNIGHT:
				long knightMap = knightMoves[from] & ~whiteBoard;
				// quiet moves
				for (long nb = knightMap & ~blackBoard; nb != 0; nb &= nb - 1) {
					int to = convert.get(nb & -nb);
					moves.add(new Move(Piece.KNIGHT, from, to));
				}
				// captures
				for (long nb = knightMap & blackBoard; nb != 0; nb &= nb - 1) {
					int to = convert.get(nb & -nb);
					moves.add(new Move(Piece.KNIGHT, from, to, blackMailbox[to]));
				}
				break;
			case ROOK:
				long rookMap = (slideAttacks(from, rankMask[from]) ^ slideAttacks(from, fileMask[from])) & ~whiteBoard;
				// quiet moves
				for (long rb = rookMap & ~blackBoard; rb != 0; rb &= rb - 1) {
					int to = convert.get(rb & -rb);
					moves.add(new Move(Piece.ROOK, from, to));
				}
				// captures
				for (long rb = rookMap & blackBoard; rb != 0; rb &= rb - 1) {
					int to = convert.get(rb & -rb);
					moves.add(new Move(Piece.ROOK, from, to, blackMailbox[to]));
				}
				break;
			case BISHOP:
				long bishopMap = (slideAttacks(from, diagonalMask[from]) ^ slideAttacks(from, antiDiagonalMask[from])) & ~whiteBoard;
				// quiet moves
				for (long bb = bishopMap & ~blackBoard; bb != 0; bb &= bb - 1) {
					int to = convert.get(bb & -bb);
					moves.add(new Move(Piece.BISHOP, from, to));
				}
				// captures
				for (long bb = bishopMap & blackBoard; bb != 0; bb &= bb - 1) {
					int to = convert.get(bb & -bb);
					moves.add(new Move(Piece.BISHOP, from, to, blackMailbox[to]));
				}
				break;
			case QUEEN:
				long queenMap = (slideAttacks(from, rankMask[from]) ^ slideAttacks(from, fileMask[from]) ^ slideAttacks(from, diagonalMask[from]) ^ slideAttacks(from, antiDiagonalMask[from])) & ~whiteBoard;
				// quiet moves
				for (long qb = queenMap & ~blackBoard; qb != 0; qb &= qb - 1) {
					int to = convert.get(qb & -qb);
					moves.add(new Move(Piece.QUEEN, from, to));
				}
				// captures
				for (long qb = queenMap & blackBoard; qb != 0; qb &= qb - 1) {
					int to = convert.get(qb & -qb);
					moves.add(new Move(Piece.QUEEN, from, to, blackMailbox[to]));
				}
				break;
			case KING:
				long kingMap = kingMoves[from] & ~whiteBoard;
				// quiet moves
				for (long kb = kingMap & ~blackBoard; kb != 0; kb &= kb - 1) {
					int to = convert.get(kb & -kb);
					moves.add(new Move(Piece.KING, from, to));
				}
				// captures
				for (long kb = kingMap & blackBoard; kb != 0; kb &= kb - 1) {
					int to = convert.get(kb & -kb);
					moves.add(new Move(Piece.KING, from, to, blackMailbox[to]));
				}
				// castling
				if (whiteKingside && whiteMailbox[5] == Piece.NONE && whiteMailbox[6] == Piece.NONE) {
					if (blackMailbox[5] == Piece.NONE && blackMailbox[6] == Piece.NONE) {
						Move move = new Move(Piece.KING, 4, 6);
						move.isKingsideCastle = true;
						moves.add(move);
					} else {
						possibleInvalidMove = true;
					}
				}
				if (whiteQueenside && whiteMailbox[3] == Piece.NONE && whiteMailbox[2] == Piece.NONE && whiteMailbox[1] == Piece.NONE) {
					if (blackMailbox[3] == Piece.NONE && blackMailbox[2] == Piece.NONE && blackMailbox[1] == Piece.NONE) {
						Move move = new Move(Piece.KING, 4, 2);
						move.isQueensideCastle = true;
						moves.add(move);
					} else {
						possibleInvalidMove = true;
					}
				}
				break;
			}
		}
		// pawns moves are handled separately
		// left captures
		long a = whitePawns >>> 7;
		long b = a & 0b0000000000000000000000000000000000000000000000000000000011111110L;
		long c = a & 0b1111111011111110111111101111111011111110111111101111111000000000L;
		long d = a & 0b1111111011111110111111101111111011111110111111101111111011111110L;
		// left captures, no promotion, no en passant
		for (long lb = c & blackBoard; lb != 0; lb &= lb - 1) {
			int to = convert.get(lb & -lb);
			moves.add(new Move(Piece.PAWN, to - 7, to, blackMailbox[to]));
		}
		// left captures, promotion
		for (long lb = b & blackBoard; lb != 0; lb &= lb - 1) {
			int to = convert.get(lb & -lb);
			moves.add(new Move(Piece.PAWN, Piece.QUEEN, to - 7, to, blackMailbox[to]));
			moves.add(new Move(Piece.PAWN, Piece.KNIGHT, to - 7, to, blackMailbox[to]));
			moves.add(new Move(Piece.PAWN, Piece.ROOK, to - 7, to, blackMailbox[to]));
			moves.add(new Move(Piece.PAWN, Piece.BISHOP, to - 7, to, blackMailbox[to]));
		}
		// left captures, en passant
		if (ep && (d & sq[eps]) != 0) {
			Move move = new Move(Piece.PAWN, eps - 7, eps, Piece.PAWN);
			move.isEPCapture = true;
			moves.add(move);
		}
		// left invalid moves
		else if ((d & ~board) != 0) {
			possibleInvalidMove = true;
		}
		// right captures
		a = whitePawns >>> 9;
		b = a & 0b0000000000000000000000000000000000000000000000000000000001111111L;
		c = a & 0b0111111101111111011111110111111101111111011111110111111100000000L;
		d = a & 0b0111111101111111011111110111111101111111011111110111111101111111L;
		// right captures, no promotion, no en passant
		for (long lb = c & blackBoard; lb != 0; lb &= lb - 1) {
			int to = convert.get(lb & -lb);
			moves.add(new Move(Piece.PAWN, to - 9, to, blackMailbox[to]));
		}
		// right captures, promotion
		for (long lb = b & blackBoard; lb != 0; lb &= lb - 1) {
			int to = convert.get(lb & -lb);
			moves.add(new Move(Piece.PAWN, Piece.QUEEN, to - 9, to, blackMailbox[to]));
			moves.add(new Move(Piece.PAWN, Piece.KNIGHT, to - 9, to, blackMailbox[to]));
			moves.add(new Move(Piece.PAWN, Piece.ROOK, to - 9, to, blackMailbox[to]));
			moves.add(new Move(Piece.PAWN, Piece.BISHOP, to - 9, to, blackMailbox[to]));
		}
		// right captures, en passant
		if (ep && (d & sq[eps]) != 0) {
			Move move = new Move(Piece.PAWN, eps - 9, eps, Piece.PAWN);
			move.isEPCapture = true;
			moves.add(move);
		}
		// right invalid moves
		else if ((d & ~board) != 0) {
			possibleInvalidMove = true;
		}
		// single push
		long singleMap = whitePawns >>> 8;
		for (long sb = singleMap & ~board; sb != 0; sb &= sb - 1) {
			int to = convert.get(sb & -sb);
			moves.add(new Move(Piece.PAWN, to - 8, to));
		}
		// single push blocked
		if ((singleMap & blackBoard) != 0) {
			possibleInvalidMove = true;
		}
		// double push
		long doubleMap = (whitePawns >>> 16) & 0b0000000000000000000000001111111100000000000000000000000000000000L & ((singleMap & ~board) >>> 8);
		for (long db = doubleMap & ~board; db != 0; db &= db - 1) {
			int to = convert.get(db & -db);
			Move move = new Move(Piece.PAWN, to - 16, to);
			move.isDPMove = true;
			moves.add(move);
		}
		// invalid move
		if (possibleInvalidMove) {
			Move move = new Move();
			move.isInvalidMove = true;
			moves.add(move);
		}
		return moves;
	}

	public List<Move> blackMoves() {
		List<Move> moves = new ArrayList<>();
		if (endGame) {
			return moves;
		}
		boolean possibleInvalidMove = false;
		for (long b = blackBoard; b != 0; b &= b - 1) {
			int from = convert.get(b & -b);
			switch (blackMailbox[from]) {
			case KNIGHT:
				long knightMap = knightMoves[from] & ~blackBoard;
				// quiet moves
				for (long nb = knightMap & ~whiteBoard; nb != 0; nb &= nb - 1) {
					int to = convert.get(nb & -nb);
					moves.add(new Move(Piece.KNIGHT, from, to));
				}
				// captures
				for (long nb = knightMap & whiteBoard; nb != 0; nb &= nb - 1) {
					int to = convert.get(nb & -nb);
					moves.add(new Move(Piece.KNIGHT, from, to, whiteMailbox[to]));
				}
				break;
			case ROOK:
				long rookMap = (slideAttacks(from, rankMask[from]) ^ slideAttacks(from, fileMask[from])) & ~blackBoard;
				// quiet moves
				for (long rb = rookMap & ~whiteBoard; rb != 0; rb &= rb - 1) {
					int to = convert.get(rb & -rb);
					moves.add(new Move(Piece.ROOK, from, to));
				}
				// captures
				for (long rb = rookMap & whiteBoard; rb != 0; rb &= rb - 1) {
					int to = convert.get(rb & -rb);
					moves.add(new Move(Piece.ROOK, from, to, whiteMailbox[to]));
				}
				break;
			case BISHOP:
				long bishopMap = (slideAttacks(from, diagonalMask[from]) ^ slideAttacks(from, antiDiagonalMask[from])) & ~blackBoard;
				// quiet moves
				for (long bb = bishopMap & ~whiteBoard; bb != 0; bb &= bb - 1) {
					int to = convert.get(bb & -bb);
					moves.add(new Move(Piece.BISHOP, from, to));
				}
				// captures
				for (long bb = bishopMap & whiteBoard; bb != 0; bb &= bb - 1) {
					int to = convert.get(bb & -bb);
					moves.add(new Move(Piece.BISHOP, from, to, whiteMailbox[to]));
				}
				break;
			case QUEEN:
				long queenMap = (slideAttacks(from, rankMask[from]) ^ slideAttacks(from, fileMask[from]) ^ slideAttacks(from, diagonalMask[from]) ^ slideAttacks(from, antiDiagonalMask[from])) & ~blackBoard;
				// quiet moves
				for (long qb = queenMap & ~whiteBoard; qb != 0; qb &= qb - 1) {
					int to = convert.get(qb & -qb);
					moves.add(new Move(Piece.QUEEN, from, to));
				}
				// captures
				for (long qb = queenMap & whiteBoard; qb != 0; qb &= qb - 1) {
					int to = convert.get(qb & -qb);
					moves.add(new Move(Piece.QUEEN, from, to, whiteMailbox[to]));
				}
				break;
			case KING:
				long kingMap = kingMoves[from] & ~blackBoard;
				// quiet moves
				for (long kb = kingMap & ~whiteBoard; kb != 0; kb &= kb - 1) {
					int to = convert.get(kb & -kb);
					moves.add(new Move(Piece.KING, from, to));
				}
				// captures
				for (long kb = kingMap & whiteBoard; kb != 0; kb &= kb - 1) {
					int to = convert.get(kb & -kb);
					moves.add(new Move(Piece.KING, from, to, whiteMailbox[to]));
				}
				// castling
				if (blackKingside && blackMailbox[61] == Piece.NONE && blackMailbox[62] == Piece.NONE) {
					if (whiteMailbox[61] == Piece.NONE && whiteMailbox[62] == Piece.NONE) {
						Move move = new Move(Piece.KING, 60, 62);
						move.isKingsideCastle = true;
						moves.add(move);
					} else {
						possibleInvalidMove = true;
					}
				}
				if (blackQueenside && blackMailbox[59] == Piece.NONE && blackMailbox[58] == Piece.NONE && blackMailbox[57] == Piece.NONE) {
					if (whiteMailbox[59] == Piece.NONE && whiteMailbox[58] == Piece.NONE && whiteMailbox[57] == Piece.NONE) {
						Move move = new Move(Piece.KING, 60, 58);
						move.isQueensideCastle = true;
						moves.add(move);
					} else {
						possibleInvalidMove = true;
					}
				}
				break;
			}
		}
		// pawns moves are handled separately
		// left captures
		long a = blackPawns << 9;
		long b = a & 0b1111111000000000000000000000000000000000000000000000000000000000L;
		long c = a & 0b0000000011111110111111101111111011111110111111101111111011111110L;
		long d = a & 0b1111111011111110111111101111111011111110111111101111111011111110L;
		// left captures, no promotion, no en passant
		for (long lb = c & whiteBoard; lb != 0; lb &= lb - 1) {
			int to = convert.get(lb & -lb);
			moves.add(new Move(Piece.PAWN, to + 9, to, whiteMailbox[to]));
		}
		// left captures, promotion
		for (long lb = b & whiteBoard; lb != 0; lb &= lb - 1) {
			int to = convert.get(lb & -lb);
			moves.add(new Move(Piece.PAWN, Piece.QUEEN, to + 9, to, whiteMailbox[to]));
			moves.add(new Move(Piece.PAWN, Piece.KNIGHT, to + 9, to, whiteMailbox[to]));
			moves.add(new Move(Piece.PAWN, Piece.ROOK, to + 9, to, whiteMailbox[to]));
			moves.add(new Move(Piece.PAWN, Piece.BISHOP, to + 9, to, whiteMailbox[to]));
		}
		// left captures, en passant
		if (ep && (d & sq[eps]) != 0) {
			Move move = new Move(Piece.PAWN, eps + 9, eps, Piece.PAWN);
			move.isEPCapture = true;
			moves.add(move);
		}
		// left invalid moves
		else if ((d & ~board) != 0) {
			possibleInvalidMove = true;
		}
		// right captures
		a = blackPawns << 7;
		b = a & 0b0111111100000000000000000000000000000000000000000000000000000000L;
		c = a & 0b0000000001111111011111110111111101111111011111110111111101111111L;
		d = a & 0b0111111101111111011111110111111101111111011111110111111101111111L;
		// right captures, no promotion, no en passant
		for (long lb = c & whiteBoard; lb != 0; lb &= lb - 1) {
			int to = convert.get(lb & -lb);
			moves.add(new Move(Piece.PAWN, to + 7, to, whiteMailbox[to]));
		}
		// right captures, promotion
		for (long lb = b & whiteBoard; lb != 0; lb &= lb - 1) {
			int to = convert.get(lb & -lb);
			moves.add(new Move(Piece.PAWN, Piece.QUEEN, to + 7, to, whiteMailbox[to]));
			moves.add(new Move(Piece.PAWN, Piece.KNIGHT, to + 7, to, whiteMailbox[to]));
			moves.add(new Move(Piece.PAWN, Piece.ROOK, to + 7, to, whiteMailbox[to]));
			moves.add(new Move(Piece.PAWN, Piece.BISHOP, to + 7, to, whiteMailbox[to]));
		}
		// right captures, en passant
		if (ep && (d & sq[eps]) != 0) {
			Move move = new Move(Piece.PAWN, eps + 7, eps, Piece.PAWN);
			move.isEPCapture = true;
			moves.add(move);
		}
		// right invalid moves
		else if ((d & ~board) != 0) {
			possibleInvalidMove = true;
		}
		// single push
		long singleMap = blackPawns << 8;
		for (long sb = singleMap & ~board; sb != 0; sb &= sb - 1) {
			int to = convert.get(sb & -sb);
			moves.add(new Move(Piece.PAWN, to + 8, to));
		}
		// single push blocked
		if ((singleMap & whiteBoard) != 0) {
			possibleInvalidMove = true;
		}
		// double push
		long doubleMap = (blackPawns << 16) & 0b0000000000000000000000000000000011111111000000000000000000000000L & ((singleMap & ~board) << 8);
		for (long db = doubleMap & ~board; db != 0; db &= db - 1) {
			int to = convert.get(db & -db);
			Move move = new Move(Piece.PAWN, to + 16, to);
			move.isDPMove = true;
			moves.add(move);
		}
		// invalid move
		if (possibleInvalidMove) {
			Move move = new Move();
			move.isInvalidMove = true;
			moves.add(move);
		}
		return moves;
	}

	public List<Move> whitePseudoMoves() {
		List<Move> moves = new ArrayList<>();
		if (endGame) {
			return moves;
		}
		for (long b = whiteBoard; b != 0; b &= b - 1) {
			int from = convert.get(b & -b);
			switch (whiteMailbox[from]) {
			case KNIGHT:
				long knightMap = knightMoves[from] & ~whiteBoard;
				// quiet moves
				for (long nb = knightMap & ~blackBoard; nb != 0; nb &= nb - 1) {
					int to = convert.get(nb & -nb);
					moves.add(new Move(Piece.KNIGHT, from, to));
				}
				// captures
				for (long nb = knightMap & blackBoard; nb != 0; nb &= nb - 1) {
					int to = convert.get(nb & -nb);
					moves.add(new Move(Piece.KNIGHT, from, to, blackMailbox[to]));
				}
				break;
			case ROOK:
				// north moves
				long rookNorthMap = slideAttacks(from, northMask[from]) & ~whiteBoard;
				// quiet moves
				for (long rb = rookNorthMap & ~blackBoard; rb != 0; rb &= rb - 1) {
					int to = convert.get(rb & -rb);
					moves.add(new Move(Piece.ROOK, from, to));
				}
				// capture moves
				long rookNorthCaptureMap = rookNorthMap & blackBoard;
				if (rookNorthCaptureMap != 0) {
					int cap = convert.get(Long.highestOneBit(rookNorthCaptureMap));
					moves.add(new Move(Piece.ROOK, from, cap));
					// blocked moves
					long rookNorthPseudoMap = pseudoWhiteSlideAttacks(from, northMask[from]) & ~whiteBoard;
					for (long rb = rookNorthPseudoMap ^ rookNorthMap; rb != 0; rb &= rb - 1) {
						int to = convert.get(rb & -rb);
						Move move = new Move(Piece.ROOK, from, to, cap);
						moves.add(move);
					}
				}

				// south moves
				long rookSouthMap = slideAttacks(from, southMask[from]) & ~whiteBoard;
				// quiet moves
				for (long rb = rookSouthMap & ~blackBoard; rb != 0; rb &= rb - 1) {
					int to = convert.get(rb & -rb);
					moves.add(new Move(Piece.ROOK, from, to));
				}
				// capture moves
				long rookSouthCaptureMap = rookSouthMap & blackBoard;
				if (rookSouthCaptureMap != 0) {
					int cap = convert.get(rookSouthCaptureMap & -rookSouthCaptureMap);
					moves.add(new Move(Piece.ROOK, from, cap));
					// blocked moves
					long rookSouthPseudoMap = pseudoWhiteSlideAttacks(from, southMask[from]) & ~whiteBoard;
					for (long rb = rookSouthPseudoMap ^ rookSouthMap; rb != 0; rb &= rb - 1) {
						int to = convert.get(rb & -rb);
						Move move = new Move(Piece.ROOK, from, to, cap);
						moves.add(move);
					}
				}

				// east moves
				long rookEastMap = slideAttacks(from, eastMask[from]) & ~whiteBoard;
				// quiet moves
				for (long rb = rookEastMap & ~blackBoard; rb != 0; rb &= rb - 1) {
					int to = convert.get(rb & -rb);
					moves.add(new Move(Piece.ROOK, from, to));
				}
				// capture moves
				long rookEastCaptureMap = rookEastMap & blackBoard;
				if (rookEastCaptureMap != 0) {
					int cap = convert.get(Long.highestOneBit(rookEastCaptureMap));
					moves.add(new Move(Piece.ROOK, from, cap));
					// blocked moves
					long rookEastPseudoMap = pseudoWhiteSlideAttacks(from, eastMask[from]) & ~whiteBoard;
					for (long rb = rookEastPseudoMap ^ rookEastMap; rb != 0; rb &= rb - 1) {
						int to = convert.get(rb & -rb);
						Move move = new Move(Piece.ROOK, from, to, cap);
						moves.add(move);
					}
				}

				// west moves
				long rookWestMap = slideAttacks(from, westMask[from]) & ~whiteBoard;
				// quiet moves
				for (long rb = rookWestMap & ~blackBoard; rb != 0; rb &= rb - 1) {
					int to = convert.get(rb & -rb);
					moves.add(new Move(Piece.ROOK, from, to));
				}
				// capture moves
				long rookWestCaptureMap = rookWestMap & blackBoard;
				if (rookWestCaptureMap != 0) {
					int cap = convert.get(rookWestCaptureMap & -rookWestCaptureMap);
					moves.add(new Move(Piece.ROOK, from, cap));
					// blocked moves
					long rookWestPseudoMap = pseudoWhiteSlideAttacks(from, westMask[from]) & ~whiteBoard;
					for (long rb = rookWestPseudoMap ^ rookWestMap; rb != 0; rb &= rb - 1) {
						int to = convert.get(rb & -rb);
						Move move = new Move(Piece.ROOK, from, to, cap);
						moves.add(move);
					}
				}
				break;
			case BISHOP:
				// northEast moves
				long bishopNorthEastMap = slideAttacks(from, northEastMask[from]) & ~whiteBoard;
				// quiet moves
				for (long bb = bishopNorthEastMap & ~blackBoard; bb != 0; bb &= bb - 1) {
					int to = convert.get(bb & -bb);
					moves.add(new Move(Piece.BISHOP, from, to));
				}
				// capture moves
				long bishopNorthEastCaptureMap = bishopNorthEastMap & blackBoard;
				if (bishopNorthEastCaptureMap != 0) {
					int cap = convert.get(Long.highestOneBit(bishopNorthEastCaptureMap));
					moves.add(new Move(Piece.BISHOP, from, cap));
					// blocked moves
					long bishopNorthEastPseudoMap = pseudoWhiteSlideAttacks(from, northEastMask[from]) & ~whiteBoard;
					for (long bb = bishopNorthEastPseudoMap ^ bishopNorthEastMap; bb != 0; bb &= bb - 1) {
						int to = convert.get(bb & -bb);
						Move move = new Move(Piece.BISHOP, from, to, cap);
						moves.add(move);
					}
				}

				// southEast moves
				long bishopSouthEastMap = slideAttacks(from, southEastMask[from]) & ~whiteBoard;
				// quiet moves
				for (long bb = bishopSouthEastMap & ~blackBoard; bb != 0; bb &= bb - 1) {
					int to = convert.get(bb & -bb);
					moves.add(new Move(Piece.BISHOP, from, to));
				}
				// capture moves
				long bishopSouthEastCaptureMap = bishopSouthEastMap & blackBoard;
				if (bishopSouthEastCaptureMap != 0) {
					int cap = convert.get(bishopSouthEastCaptureMap & -bishopSouthEastCaptureMap);
					moves.add(new Move(Piece.BISHOP, from, cap));
					// blocked moves
					long bishopSouthEastPseudoMap = pseudoWhiteSlideAttacks(from, southEastMask[from]) & ~whiteBoard;
					for (long bb = bishopSouthEastPseudoMap ^ bishopSouthEastMap; bb != 0; bb &= bb - 1) {
						int to = convert.get(bb & -bb);
						Move move = new Move(Piece.BISHOP, from, to, cap);
						moves.add(move);
					}
				}

				// southWest moves
				long bishopSouthWestMap = slideAttacks(from, southWestMask[from]) & ~whiteBoard;
				// quiet moves
				for (long bb = bishopSouthWestMap & ~blackBoard; bb != 0; bb &= bb - 1) {
					int to = convert.get(bb & -bb);
					moves.add(new Move(Piece.BISHOP, from, to));
				}
				// capture moves
				long bishopSouthWestCaptureMap = bishopSouthWestMap & blackBoard;
				if (bishopSouthWestCaptureMap != 0) {
					int cap = convert.get(bishopSouthWestCaptureMap & -bishopSouthWestCaptureMap);
					moves.add(new Move(Piece.BISHOP, from, cap));
					// blocked moves
					long bishopSouthWestPseudoMap = pseudoWhiteSlideAttacks(from, southWestMask[from]) & ~whiteBoard;
					for (long bb = bishopSouthWestPseudoMap ^ bishopSouthWestMap; bb != 0; bb &= bb - 1) {
						int to = convert.get(bb & -bb);
						Move move = new Move(Piece.BISHOP, from, to, cap);
						moves.add(move);
					}
				}

				// northWest moves
				long bishopNorthWestMap = slideAttacks(from, northWestMask[from]) & ~whiteBoard;
				// quiet moves
				for (long bb = bishopNorthWestMap & ~blackBoard; bb != 0; bb &= bb - 1) {
					int to = convert.get(bb & -bb);
					moves.add(new Move(Piece.BISHOP, from, to));
				}
				// capture moves
				long bishopNorthWestCaptureMap = bishopNorthWestMap & blackBoard;
				if (bishopNorthWestCaptureMap != 0) {
					int cap = convert.get(Long.highestOneBit(bishopNorthWestCaptureMap));
					moves.add(new Move(Piece.BISHOP, from, cap));
					// blocked moves
					long bishopNorthWestPseudoMap = pseudoWhiteSlideAttacks(from, northWestMask[from]) & ~whiteBoard;
					for (long bb = bishopNorthWestPseudoMap ^ bishopNorthWestMap; bb != 0; bb &= bb - 1) {
						int to = convert.get(bb & -bb);
						Move move = new Move(Piece.BISHOP, from, to, cap);
						moves.add(move);
					}
				}
				break;
			case QUEEN:
				// north moves
				long queenNorthMap = slideAttacks(from, northMask[from]) & ~whiteBoard;
				// quiet moves
				for (long qb = queenNorthMap & ~blackBoard; qb != 0; qb &= qb - 1) {
					int to = convert.get(qb & -qb);
					moves.add(new Move(Piece.QUEEN, from, to));
				}
				// capture moves
				long queenNorthCaptureMap = queenNorthMap & blackBoard;
				if (queenNorthCaptureMap != 0) {
					int cap = convert.get(Long.highestOneBit(queenNorthCaptureMap));
					moves.add(new Move(Piece.QUEEN, from, cap));
					// blocked moves
					long queenNorthPseudoMap = pseudoWhiteSlideAttacks(from, northMask[from]) & ~whiteBoard;
					for (long qb = queenNorthPseudoMap ^ queenNorthMap; qb != 0; qb &= qb - 1) {
						int to = convert.get(qb & -qb);
						Move move = new Move(Piece.QUEEN, from, to, cap);
						moves.add(move);
					}
				}

				// south moves
				long queenSouthMap = slideAttacks(from, southMask[from]) & ~whiteBoard;
				// quiet moves
				for (long qb = queenSouthMap & ~blackBoard; qb != 0; qb &= qb - 1) {
					int to = convert.get(qb & -qb);
					moves.add(new Move(Piece.QUEEN, from, to));
				}
				// capture moves
				long queenSouthCaptureMap = queenSouthMap & blackBoard;
				if (queenSouthCaptureMap != 0) {
					int cap = convert.get(queenSouthCaptureMap & -queenSouthCaptureMap);
					moves.add(new Move(Piece.QUEEN, from, cap));
					// blocked moves
					long queenSouthPseudoMap = pseudoWhiteSlideAttacks(from, southMask[from]) & ~whiteBoard;
					for (long qb = queenSouthPseudoMap ^ queenSouthMap; qb != 0; qb &= qb - 1) {
						int to = convert.get(qb & -qb);
						Move move = new Move(Piece.QUEEN, from, to, cap);
						moves.add(move);
					}
				}

				// east moves
				long queenEastMap = slideAttacks(from, eastMask[from]) & ~whiteBoard;
				// quiet moves
				for (long qb = queenEastMap & ~blackBoard; qb != 0; qb &= qb - 1) {
					int to = convert.get(qb & -qb);
					moves.add(new Move(Piece.QUEEN, from, to));
				}
				// capture moves
				long queenEastCaptureMap = queenEastMap & blackBoard;
				if (queenEastCaptureMap != 0) {
					int cap = convert.get(Long.highestOneBit(queenEastCaptureMap));
					moves.add(new Move(Piece.QUEEN, from, cap));
					// blocked moves
					long queenEastPseudoMap = pseudoWhiteSlideAttacks(from, eastMask[from]) & ~whiteBoard;
					for (long qb = queenEastPseudoMap ^ queenEastMap; qb != 0; qb &= qb - 1) {
						int to = convert.get(qb & -qb);
						Move move = new Move(Piece.QUEEN, from, to, cap);
						moves.add(move);
					}
				}

				// west moves
				long queenWestMap = slideAttacks(from, westMask[from]) & ~whiteBoard;
				// quiet moves
				for (long qb = queenWestMap & ~blackBoard; qb != 0; qb &= qb - 1) {
					int to = convert.get(qb & -qb);
					moves.add(new Move(Piece.QUEEN, from, to));
				}
				// capture moves
				long queenWestCaptureMap = queenWestMap & blackBoard;
				if (queenWestCaptureMap != 0) {
					int cap = convert.get(queenWestCaptureMap & -queenWestCaptureMap);
					moves.add(new Move(Piece.QUEEN, from, cap));
					// blocked moves
					long queenWestPseudoMap = pseudoWhiteSlideAttacks(from, westMask[from]) & ~whiteBoard;
					for (long qb = queenWestPseudoMap ^ queenWestMap; qb != 0; qb &= qb - 1) {
						int to = convert.get(qb & -qb);
						Move move = new Move(Piece.QUEEN, from, to, cap);
						moves.add(move);
					}
				}
				// northEast moves
				long queenNorthEastMap = slideAttacks(from, northEastMask[from]) & ~whiteBoard;
				// quiet moves
				for (long qb = queenNorthEastMap & ~blackBoard; qb != 0; qb &= qb - 1) {
					int to = convert.get(qb & -qb);
					moves.add(new Move(Piece.QUEEN, from, to));
				}
				// capture moves
				long queenNorthEastCaptureMap = queenNorthEastMap & blackBoard;
				if (queenNorthEastCaptureMap != 0) {
					int cap = convert.get(Long.highestOneBit(queenNorthEastCaptureMap));
					moves.add(new Move(Piece.QUEEN, from, cap));
					// blocked moves
					long queenNorthEastPseudoMap = pseudoWhiteSlideAttacks(from, northEastMask[from]) & ~whiteBoard;
					for (long qb = queenNorthEastPseudoMap ^ queenNorthEastMap; qb != 0; qb &= qb - 1) {
						int to = convert.get(qb & -qb);
						Move move = new Move(Piece.QUEEN, from, to, cap);
						moves.add(move);
					}
				}

				// southEast moves
				long queenSouthEastMap = slideAttacks(from, southEastMask[from]) & ~whiteBoard;
				// quiet moves
				for (long qb = queenSouthEastMap & ~blackBoard; qb != 0; qb &= qb - 1) {
					int to = convert.get(qb & -qb);
					moves.add(new Move(Piece.QUEEN, from, to));
				}
				// capture moves
				long queenSouthEastCaptureMap = queenSouthEastMap & blackBoard;
				if (queenSouthEastCaptureMap != 0) {
					int cap = convert.get(queenSouthEastCaptureMap & -queenSouthEastCaptureMap);
					moves.add(new Move(Piece.QUEEN, from, cap));
					// blocked moves
					long queenSouthEastPseudoMap = pseudoWhiteSlideAttacks(from, southEastMask[from]) & ~whiteBoard;
					for (long qb = queenSouthEastPseudoMap ^ queenSouthEastMap; qb != 0; qb &= qb - 1) {
						int to = convert.get(qb & -qb);
						Move move = new Move(Piece.QUEEN, from, to, cap);
						moves.add(move);
					}
				}

				// southWest moves
				long queenSouthWestMap = slideAttacks(from, southWestMask[from]) & ~whiteBoard;
				// quiet moves
				for (long qb = queenSouthWestMap & ~blackBoard; qb != 0; qb &= qb - 1) {
					int to = convert.get(qb & -qb);
					moves.add(new Move(Piece.QUEEN, from, to));
				}
				// capture moves
				long queenSouthWestCaptureMap = queenSouthWestMap & blackBoard;
				if (queenSouthWestCaptureMap != 0) {
					int cap = convert.get(queenSouthWestCaptureMap & -queenSouthWestCaptureMap);
					moves.add(new Move(Piece.QUEEN, from, cap));
					// blocked moves
					long queenSouthWestPseudoMap = pseudoWhiteSlideAttacks(from, southWestMask[from]) & ~whiteBoard;
					for (long qb = queenSouthWestPseudoMap ^ queenSouthWestMap; qb != 0; qb &= qb - 1) {
						int to = convert.get(qb & -qb);
						Move move = new Move(Piece.QUEEN, from, to, cap);
						moves.add(move);
					}
				}

				// northWest moves
				long queenNorthWestMap = slideAttacks(from, northWestMask[from]) & ~whiteBoard;
				// quiet moves
				for (long qb = queenNorthWestMap & ~blackBoard; qb != 0; qb &= qb - 1) {
					int to = convert.get(qb & -qb);
					moves.add(new Move(Piece.QUEEN, from, to));
				}
				// capture moves
				long queenNorthWestCaptureMap = queenNorthWestMap & blackBoard;
				if (queenNorthWestCaptureMap != 0) {
					int cap = convert.get(Long.highestOneBit(queenNorthWestCaptureMap));
					moves.add(new Move(Piece.QUEEN, from, cap));
					// blocked moves
					long queenNorthWestPseudoMap = pseudoWhiteSlideAttacks(from, northWestMask[from]) & ~whiteBoard;
					for (long qb = queenNorthWestPseudoMap ^ queenNorthWestMap; qb != 0; qb &= qb - 1) {
						int to = convert.get(qb & -qb);
						Move move = new Move(Piece.QUEEN, from, to, cap);
						moves.add(move);
					}
				}
				break;
			case KING:
				long kingMap = kingMoves[from] & ~whiteBoard;
				// quiet moves
				for (long kb = kingMap & ~blackBoard; kb != 0; kb &= kb - 1) {
					int to = convert.get(kb & -kb);
					moves.add(new Move(Piece.KING, from, to));
				}
				// captures
				for (long kb = kingMap & blackBoard; kb != 0; kb &= kb - 1) {
					int to = convert.get(kb & -kb);
					moves.add(new Move(Piece.KING, from, to, blackMailbox[to]));
				}
				// castling
				if (whiteKingside && whiteMailbox[5] == Piece.NONE && whiteMailbox[6] == Piece.NONE) {
					if (blackMailbox[5] == Piece.NONE && blackMailbox[6] == Piece.NONE) {
						Move move = new Move(Piece.KING, 4, 6);
						move.isKingsideCastle = true;
						moves.add(move);
					} else {
						Move move = new Move(Piece.KING, 4, 6);
						move.isInvalidMove = true;
						moves.add(move);
					}
				}
				if (whiteQueenside && whiteMailbox[3] == Piece.NONE && whiteMailbox[2] == Piece.NONE && whiteMailbox[1] == Piece.NONE) {
					if (blackMailbox[3] == Piece.NONE && blackMailbox[2] == Piece.NONE && blackMailbox[1] == Piece.NONE) {
						Move move = new Move(Piece.KING, 4, 2);
						move.isQueensideCastle = true;
						moves.add(move);
					} else {
						Move move = new Move(Piece.KING, 4, 2);
						move.isInvalidMove = true;
						moves.add(move);
					}
				}
				break;
			}
		}
		// pawns moves are handled separately
		// left captures
		long a = whitePawns >>> 7;
		long b = a & 0b0000000000000000000000000000000000000000000000000000000011111110L;
		long c = a & 0b1111111011111110111111101111111011111110111111101111111000000000L;
		// left captures, no promotion, no en passant
		for (long lb = c & blackBoard; lb != 0; lb &= lb - 1) {
			int to = convert.get(lb & -lb);
			moves.add(new Move(Piece.PAWN, to - 7, to, blackMailbox[to]));
		}
		// invalid left captures, no promotion, no en passant
		long invalidLeft = c & ~board;
		if (ep) {
			invalidLeft = (invalidLeft | sq[eps]) ^ sq[eps];
		}
		for (long lb = invalidLeft; lb != 0; lb &= lb - 1) {
			int to = convert.get(lb & -lb);
			Move move = new Move(Piece.PAWN, to - 7, to);
			move.isInvalidMove = true;
			moves.add(move);
		}
		// left captures, promotion
		for (long lb = b & blackBoard; lb != 0; lb &= lb - 1) {
			int to = convert.get(lb & -lb);
			moves.add(new Move(Piece.PAWN, Piece.QUEEN, to - 7, to, blackMailbox[to]));
			moves.add(new Move(Piece.PAWN, Piece.KNIGHT, to - 7, to, blackMailbox[to]));
			moves.add(new Move(Piece.PAWN, Piece.ROOK, to - 7, to, blackMailbox[to]));
			moves.add(new Move(Piece.PAWN, Piece.BISHOP, to - 7, to, blackMailbox[to]));
		}
		// invalid left captures, promotion
		for (long lb = b & ~board; lb != 0; lb &= lb - 1) {
			int to = convert.get(lb & -lb);
			Move move = new Move(Piece.PAWN, Piece.QUEEN, to - 7, to);
			move.isInvalidMove = true;
			moves.add(move);
			move = new Move(Piece.PAWN, Piece.KNIGHT, to - 7, to);
			move.isInvalidMove = true;
			moves.add(move);
			move = new Move(Piece.PAWN, Piece.ROOK, to - 7, to);
			move.isInvalidMove = true;
			moves.add(move);
			move = new Move(Piece.PAWN, Piece.BISHOP, to - 7, to);
			move.isInvalidMove = true;
			moves.add(move);
		}
		// left captures, en passant
		if (ep && (c & sq[eps]) != 0) {
			Move move = new Move(Piece.PAWN, eps - 7, eps, Piece.PAWN);
			move.isEPCapture = true;
			moves.add(move);
		}
		// right captures
		a = whitePawns >>> 9;
		b = a & 0b0000000000000000000000000000000000000000000000000000000001111111L;
		c = a & 0b0111111101111111011111110111111101111111011111110111111100000000L;
		// right captures, no promotion, no en passant
		for (long lb = c & blackBoard; lb != 0; lb &= lb - 1) {
			int to = convert.get(lb & -lb);
			moves.add(new Move(Piece.PAWN, to - 9, to, blackMailbox[to]));
		}
		// invalid right captures, no promotion, no en passant
		long invalidRight = c & ~board;
		if (ep) {
			invalidRight = (invalidRight | sq[eps]) ^ sq[eps];
		}
		for (long lb = invalidRight; lb != 0; lb &= lb - 1) {
			int to = convert.get(lb & -lb);
			Move move = new Move(Piece.PAWN, to - 9, to);
			move.isInvalidMove = true;
			moves.add(move);
		}
		// right captures, promotion
		for (long lb = b & blackBoard; lb != 0; lb &= lb - 1) {
			int to = convert.get(lb & -lb);
			moves.add(new Move(Piece.PAWN, Piece.QUEEN, to - 9, to, blackMailbox[to]));
			moves.add(new Move(Piece.PAWN, Piece.KNIGHT, to - 9, to, blackMailbox[to]));
			moves.add(new Move(Piece.PAWN, Piece.ROOK, to - 9, to, blackMailbox[to]));
			moves.add(new Move(Piece.PAWN, Piece.BISHOP, to - 9, to, blackMailbox[to]));
		}
		// invalid right captures, promotion
		for (long lb = b & ~board; lb != 0; lb &= lb - 1) {
			int to = convert.get(lb & -lb);
			Move move = new Move(Piece.PAWN, Piece.QUEEN, to - 9, to);
			move.isInvalidMove = true;
			moves.add(move);
			move = new Move(Piece.PAWN, Piece.KNIGHT, to - 9, to);
			move.isInvalidMove = true;
			moves.add(move);
			move = new Move(Piece.PAWN, Piece.ROOK, to - 9, to);
			move.isInvalidMove = true;
			moves.add(move);
			move = new Move(Piece.PAWN, Piece.BISHOP, to - 9, to);
			move.isInvalidMove = true;
			moves.add(move);
		}
		// right captures, en passant
		if (ep && (c & sq[eps]) != 0) {
			Move move = new Move(Piece.PAWN, eps - 9, eps, Piece.PAWN);
			move.isEPCapture = true;
			moves.add(move);
		}
		// single push
		long singleMap = whitePawns >>> 8;
		for (long sb = singleMap & ~board; sb != 0; sb &= sb - 1) {
			int to = convert.get(sb & -sb);
			moves.add(new Move(Piece.PAWN, to - 8, to));
		}
		// single push blocked
		for (long sb = singleMap & blackBoard; sb != 0; sb &= sb - 1) {
			int to = convert.get(sb & -sb);
			Move move = new Move(Piece.PAWN, to - 8, to);
			move.isInvalidMove = true;
			moves.add(move);
		}
		// double push
		long doubleMap = (whitePawns >>> 16) & 0b0000000000000000000000001111111100000000000000000000000000000000L & ((singleMap & ~board) >>> 8);
		for (long db = doubleMap & ~board; db != 0; db &= db - 1) {
			int to = convert.get(db & -db);
			Move move = new Move(Piece.PAWN, to - 16, to);
			move.isDPMove = true;
			moves.add(move);
		}
		// double push blocked
		for (long db = doubleMap & blackBoard; db != 0; db &= db - 1) {
			int to = convert.get(db & -db);
			Move move = new Move(Piece.PAWN, to - 16, to, to - 8);
			moves.add(move);
		}
		return moves;
	}

	public List<Move> blackPseudoMoves() {
		List<Move> moves = new ArrayList<>();
		if (endGame) {
			return moves;
		}
		for (long b = blackBoard; b != 0; b &= b - 1) {
			int from = convert.get(b & -b);
			switch (blackMailbox[from]) {
			case KNIGHT:
				long knightMap = knightMoves[from] & ~blackBoard;
				// quiet moves
				for (long nb = knightMap & ~whiteBoard; nb != 0; nb &= nb - 1) {
					int to = convert.get(nb & -nb);
					moves.add(new Move(Piece.KNIGHT, from, to));
				}
				// captures
				for (long nb = knightMap & whiteBoard; nb != 0; nb &= nb - 1) {
					int to = convert.get(nb & -nb);
					moves.add(new Move(Piece.KNIGHT, from, to, whiteMailbox[to]));
				}
				break;
			case ROOK:
				// north moves
				long rookNorthMap = slideAttacks(from, northMask[from]) & ~blackBoard;
				// quiet moves
				for (long rb = rookNorthMap & ~whiteBoard; rb != 0; rb &= rb - 1) {
					int to = convert.get(rb & -rb);
					moves.add(new Move(Piece.ROOK, from, to));
				}
				// capture moves
				long rookNorthCaptureMap = rookNorthMap & whiteBoard;
				if (rookNorthCaptureMap != 0) {
					int cap = convert.get(Long.highestOneBit(rookNorthCaptureMap));
					moves.add(new Move(Piece.ROOK, from, cap));
					// blocked moves
					long rookNorthPseudoMap = pseudoBlackSlideAttacks(from, northMask[from]) & ~blackBoard;
					for (long rb = rookNorthPseudoMap ^ rookNorthMap; rb != 0; rb &= rb - 1) {
						int to = convert.get(rb & -rb);
						Move move = new Move(Piece.ROOK, from, to, cap);
						moves.add(move);
					}
				}

				// south moves
				long rookSouthMap = slideAttacks(from, southMask[from]) & ~blackBoard;
				// quiet moves
				for (long rb = rookSouthMap & ~whiteBoard; rb != 0; rb &= rb - 1) {
					int to = convert.get(rb & -rb);
					moves.add(new Move(Piece.ROOK, from, to));
				}
				// capture moves
				long rookSouthCaptureMap = rookSouthMap & whiteBoard;
				if (rookSouthCaptureMap != 0) {
					int cap = convert.get(rookSouthCaptureMap & -rookSouthCaptureMap);
					moves.add(new Move(Piece.ROOK, from, cap));
					// blocked moves
					long rookSouthPseudoMap = pseudoBlackSlideAttacks(from, southMask[from]) & ~blackBoard;
					for (long rb = rookSouthPseudoMap ^ rookSouthMap; rb != 0; rb &= rb - 1) {
						int to = convert.get(rb & -rb);
						Move move = new Move(Piece.ROOK, from, to, cap);
						moves.add(move);
					}
				}

				// east moves
				long rookEastMap = slideAttacks(from, eastMask[from]) & ~blackBoard;
				// quiet moves
				for (long rb = rookEastMap & ~whiteBoard; rb != 0; rb &= rb - 1) {
					int to = convert.get(rb & -rb);
					moves.add(new Move(Piece.ROOK, from, to));
				}
				// capture moves
				long rookEastCaptureMap = rookEastMap & whiteBoard;
				if (rookEastCaptureMap != 0) {
					int cap = convert.get(Long.highestOneBit(rookEastCaptureMap));
					moves.add(new Move(Piece.ROOK, from, cap));
					// blocked moves
					long rookEastPseudoMap = pseudoBlackSlideAttacks(from, eastMask[from]) & ~blackBoard;
					for (long rb = rookEastPseudoMap ^ rookEastMap; rb != 0; rb &= rb - 1) {
						int to = convert.get(rb & -rb);
						Move move = new Move(Piece.ROOK, from, to, cap);
						moves.add(move);
					}
				}

				// west moves
				long rookWestMap = slideAttacks(from, westMask[from]) & ~blackBoard;
				// quiet moves
				for (long rb = rookWestMap & ~whiteBoard; rb != 0; rb &= rb - 1) {
					int to = convert.get(rb & -rb);
					moves.add(new Move(Piece.ROOK, from, to));
				}
				// capture moves
				long rookWestCaptureMap = rookWestMap & whiteBoard;
				if (rookWestCaptureMap != 0) {
					int cap = convert.get(rookWestCaptureMap & -rookWestCaptureMap);
					moves.add(new Move(Piece.ROOK, from, cap));
					// blocked moves
					long rookWestPseudoMap = pseudoBlackSlideAttacks(from, westMask[from]) & ~blackBoard;
					for (long rb = rookWestPseudoMap ^ rookWestMap; rb != 0; rb &= rb - 1) {
						int to = convert.get(rb & -rb);
						Move move = new Move(Piece.ROOK, from, to, cap);
						moves.add(move);
					}
				}
				break;
			case BISHOP:
				// northEast moves
				long bishopNorthEastMap = slideAttacks(from, northEastMask[from]) & ~blackBoard;
				// quiet moves
				for (long bb = bishopNorthEastMap & ~whiteBoard; bb != 0; bb &= bb - 1) {
					int to = convert.get(bb & -bb);
					moves.add(new Move(Piece.BISHOP, from, to));
				}
				// capture moves
				long bishopNorthEastCaptureMap = bishopNorthEastMap & whiteBoard;
				if (bishopNorthEastCaptureMap != 0) {
					int cap = convert.get(Long.highestOneBit(bishopNorthEastCaptureMap));
					moves.add(new Move(Piece.BISHOP, from, cap));
					// blocked moves
					long bishopNorthEastPseudoMap = pseudoBlackSlideAttacks(from, northEastMask[from]) & ~blackBoard;
					for (long bb = bishopNorthEastPseudoMap ^ bishopNorthEastMap; bb != 0; bb &= bb - 1) {
						int to = convert.get(bb & -bb);
						Move move = new Move(Piece.BISHOP, from, to, cap);
						moves.add(move);
					}
				}

				// southEast moves
				long bishopSouthEastMap = slideAttacks(from, southEastMask[from]) & ~blackBoard;
				// quiet moves
				for (long bb = bishopSouthEastMap & ~whiteBoard; bb != 0; bb &= bb - 1) {
					int to = convert.get(bb & -bb);
					moves.add(new Move(Piece.BISHOP, from, to));
				}
				// capture moves
				long bishopSouthEastCaptureMap = bishopSouthEastMap & whiteBoard;
				if (bishopSouthEastCaptureMap != 0) {
					int cap = convert.get(bishopSouthEastCaptureMap & -bishopSouthEastCaptureMap);
					moves.add(new Move(Piece.BISHOP, from, cap));
					// blocked moves
					long bishopSouthEastPseudoMap = pseudoBlackSlideAttacks(from, southEastMask[from]) & ~blackBoard;
					for (long bb = bishopSouthEastPseudoMap ^ bishopSouthEastMap; bb != 0; bb &= bb - 1) {
						int to = convert.get(bb & -bb);
						Move move = new Move(Piece.BISHOP, from, to, cap);
						moves.add(move);
					}
				}

				// southWest moves
				long bishopSouthWestMap = slideAttacks(from, southWestMask[from]) & ~blackBoard;
				// quiet moves
				for (long bb = bishopSouthWestMap & ~whiteBoard; bb != 0; bb &= bb - 1) {
					int to = convert.get(bb & -bb);
					moves.add(new Move(Piece.BISHOP, from, to));
				}
				// capture moves
				long bishopSouthWestCaptureMap = bishopSouthWestMap & whiteBoard;
				if (bishopSouthWestCaptureMap != 0) {
					int cap = convert.get(bishopSouthWestCaptureMap & -bishopSouthWestCaptureMap);
					moves.add(new Move(Piece.BISHOP, from, cap));
					// blocked moves
					long bishopSouthWestPseudoMap = pseudoBlackSlideAttacks(from, southWestMask[from]) & ~blackBoard;
					for (long bb = bishopSouthWestPseudoMap ^ bishopSouthWestMap; bb != 0; bb &= bb - 1) {
						int to = convert.get(bb & -bb);
						Move move = new Move(Piece.BISHOP, from, to, cap);
						moves.add(move);
					}
				}

				// northWest moves
				long bishopNorthWestMap = slideAttacks(from, northWestMask[from]) & ~blackBoard;
				// quiet moves
				for (long bb = bishopNorthWestMap & ~whiteBoard; bb != 0; bb &= bb - 1) {
					int to = convert.get(bb & -bb);
					moves.add(new Move(Piece.BISHOP, from, to));
				}
				// capture moves
				long bishopNorthWestCaptureMap = bishopNorthWestMap & whiteBoard;
				if (bishopNorthWestCaptureMap != 0) {
					int cap = convert.get(Long.highestOneBit(bishopNorthWestCaptureMap));
					moves.add(new Move(Piece.BISHOP, from, cap));
					// blocked moves
					long bishopNorthWestPseudoMap = pseudoBlackSlideAttacks(from, northWestMask[from]) & ~blackBoard;
					for (long bb = bishopNorthWestPseudoMap ^ bishopNorthWestMap; bb != 0; bb &= bb - 1) {
						int to = convert.get(bb & -bb);
						Move move = new Move(Piece.BISHOP, from, to, cap);
						moves.add(move);
					}
				}
				break;
			case QUEEN:
				// north moves
				long queenNorthMap = slideAttacks(from, northMask[from]) & ~blackBoard;
				// quiet moves
				for (long qb = queenNorthMap & ~whiteBoard; qb != 0; qb &= qb - 1) {
					int to = convert.get(qb & -qb);
					moves.add(new Move(Piece.QUEEN, from, to));
				}
				// capture moves
				long queenNorthCaptureMap = queenNorthMap & whiteBoard;
				if (queenNorthCaptureMap != 0) {
					int cap = convert.get(Long.highestOneBit(queenNorthCaptureMap));
					moves.add(new Move(Piece.QUEEN, from, cap));
					// blocked moves
					long queenNorthPseudoMap = pseudoBlackSlideAttacks(from, northMask[from]) & ~blackBoard;
					for (long qb = queenNorthPseudoMap ^ queenNorthMap; qb != 0; qb &= qb - 1) {
						int to = convert.get(qb & -qb);
						Move move = new Move(Piece.QUEEN, from, to, cap);
						moves.add(move);
					}
				}

				// south moves
				long queenSouthMap = slideAttacks(from, southMask[from]) & ~blackBoard;
				// quiet moves
				for (long qb = queenSouthMap & ~whiteBoard; qb != 0; qb &= qb - 1) {
					int to = convert.get(qb & -qb);
					moves.add(new Move(Piece.QUEEN, from, to));
				}
				// capture moves
				long queenSouthCaptureMap = queenSouthMap & whiteBoard;
				if (queenSouthCaptureMap != 0) {
					int cap = convert.get(queenSouthCaptureMap & -queenSouthCaptureMap);
					moves.add(new Move(Piece.QUEEN, from, cap));
					// blocked moves
					long queenSouthPseudoMap = pseudoBlackSlideAttacks(from, southMask[from]) & ~blackBoard;
					for (long qb = queenSouthPseudoMap ^ queenSouthMap; qb != 0; qb &= qb - 1) {
						int to = convert.get(qb & -qb);
						Move move = new Move(Piece.QUEEN, from, to, cap);
						moves.add(move);
					}
				}

				// east moves
				long queenEastMap = slideAttacks(from, eastMask[from]) & ~blackBoard;
				// quiet moves
				for (long qb = queenEastMap & ~whiteBoard; qb != 0; qb &= qb - 1) {
					int to = convert.get(qb & -qb);
					moves.add(new Move(Piece.QUEEN, from, to));
				}
				// capture moves
				long queenEastCaptureMap = queenEastMap & whiteBoard;
				if (queenEastCaptureMap != 0) {
					int cap = convert.get(Long.highestOneBit(queenEastCaptureMap));
					moves.add(new Move(Piece.QUEEN, from, cap));
					// blocked moves
					long queenEastPseudoMap = pseudoBlackSlideAttacks(from, eastMask[from]) & ~blackBoard;
					for (long qb = queenEastPseudoMap ^ queenEastMap; qb != 0; qb &= qb - 1) {
						int to = convert.get(qb & -qb);
						Move move = new Move(Piece.QUEEN, from, to, cap);
						moves.add(move);
					}
				}

				// west moves
				long queenWestMap = slideAttacks(from, westMask[from]) & ~blackBoard;
				// quiet moves
				for (long qb = queenWestMap & ~whiteBoard; qb != 0; qb &= qb - 1) {
					int to = convert.get(qb & -qb);
					moves.add(new Move(Piece.QUEEN, from, to));
				}
				// capture moves
				long queenWestCaptureMap = queenWestMap & whiteBoard;
				if (queenWestCaptureMap != 0) {
					int cap = convert.get(queenWestCaptureMap & -queenWestCaptureMap);
					moves.add(new Move(Piece.QUEEN, from, cap));
					// blocked moves
					long queenWestPseudoMap = pseudoBlackSlideAttacks(from, westMask[from]) & ~blackBoard;
					for (long qb = queenWestPseudoMap ^ queenWestMap; qb != 0; qb &= qb - 1) {
						int to = convert.get(qb & -qb);
						Move move = new Move(Piece.QUEEN, from, to, cap);
						moves.add(move);
					}
				}
				// northEast moves
				long queenNorthEastMap = slideAttacks(from, northEastMask[from]) & ~blackBoard;
				// quiet moves
				for (long qb = queenNorthEastMap & ~whiteBoard; qb != 0; qb &= qb - 1) {
					int to = convert.get(qb & -qb);
					moves.add(new Move(Piece.QUEEN, from, to));
				}
				// capture moves
				long queenNorthEastCaptureMap = queenNorthEastMap & whiteBoard;
				if (queenNorthEastCaptureMap != 0) {
					int cap = convert.get(Long.highestOneBit(queenNorthEastCaptureMap));
					moves.add(new Move(Piece.QUEEN, from, cap));
					// blocked moves
					long queenNorthEastPseudoMap = pseudoBlackSlideAttacks(from, northEastMask[from]) & ~blackBoard;
					for (long qb = queenNorthEastPseudoMap ^ queenNorthEastMap; qb != 0; qb &= qb - 1) {
						int to = convert.get(qb & -qb);
						Move move = new Move(Piece.QUEEN, from, to, cap);
						moves.add(move);
					}
				}

				// southEast moves
				long queenSouthEastMap = slideAttacks(from, southEastMask[from]) & ~blackBoard;
				// quiet moves
				for (long qb = queenSouthEastMap & ~whiteBoard; qb != 0; qb &= qb - 1) {
					int to = convert.get(qb & -qb);
					moves.add(new Move(Piece.QUEEN, from, to));
				}
				// capture moves
				long queenSouthEastCaptureMap = queenSouthEastMap & whiteBoard;
				if (queenSouthEastCaptureMap != 0) {
					int cap = convert.get(queenSouthEastCaptureMap & -queenSouthEastCaptureMap);
					moves.add(new Move(Piece.QUEEN, from, cap));
					// blocked moves
					long queenSouthEastPseudoMap = pseudoBlackSlideAttacks(from, southEastMask[from]) & ~blackBoard;
					for (long qb = queenSouthEastPseudoMap ^ queenSouthEastMap; qb != 0; qb &= qb - 1) {
						int to = convert.get(qb & -qb);
						Move move = new Move(Piece.QUEEN, from, to, cap);
						moves.add(move);
					}
				}

				// southWest moves
				long queenSouthWestMap = slideAttacks(from, southWestMask[from]) & ~blackBoard;
				// quiet moves
				for (long qb = queenSouthWestMap & ~whiteBoard; qb != 0; qb &= qb - 1) {
					int to = convert.get(qb & -qb);
					moves.add(new Move(Piece.QUEEN, from, to));
				}
				// capture moves
				long queenSouthWestCaptureMap = queenSouthWestMap & whiteBoard;
				if (queenSouthWestCaptureMap != 0) {
					int cap = convert.get(queenSouthWestCaptureMap & -queenSouthWestCaptureMap);
					moves.add(new Move(Piece.QUEEN, from, cap));
					// blocked moves
					long queenSouthWestPseudoMap = pseudoBlackSlideAttacks(from, southWestMask[from]) & ~blackBoard;
					for (long qb = queenSouthWestPseudoMap ^ queenSouthWestMap; qb != 0; qb &= qb - 1) {
						int to = convert.get(qb & -qb);
						Move move = new Move(Piece.QUEEN, from, to, cap);
						moves.add(move);
					}
				}

				// northWest moves
				long queenNorthWestMap = slideAttacks(from, northWestMask[from]) & ~blackBoard;
				// quiet moves
				for (long qb = queenNorthWestMap & ~whiteBoard; qb != 0; qb &= qb - 1) {
					int to = convert.get(qb & -qb);
					moves.add(new Move(Piece.QUEEN, from, to));
				}
				// capture moves
				long queenNorthWestCaptureMap = queenNorthWestMap & whiteBoard;
				if (queenNorthWestCaptureMap != 0) {
					int cap = convert.get(Long.highestOneBit(queenNorthWestCaptureMap));
					moves.add(new Move(Piece.QUEEN, from, cap));
					// blocked moves
					long queenNorthWestPseudoMap = pseudoBlackSlideAttacks(from, northWestMask[from]) & ~blackBoard;
					for (long qb = queenNorthWestPseudoMap ^ queenNorthWestMap; qb != 0; qb &= qb - 1) {
						int to = convert.get(qb & -qb);
						Move move = new Move(Piece.QUEEN, from, to, cap);
						moves.add(move);
					}
				}
				break;
			case KING:
				long kingMap = kingMoves[from] & ~blackBoard;
				// quiet moves
				for (long kb = kingMap & ~whiteBoard; kb != 0; kb &= kb - 1) {
					int to = convert.get(kb & -kb);
					moves.add(new Move(Piece.KING, from, to));
				}
				// captures
				for (long kb = kingMap & whiteBoard; kb != 0; kb &= kb - 1) {
					int to = convert.get(kb & -kb);
					moves.add(new Move(Piece.KING, from, to, whiteMailbox[to]));
				}
				// castling
				if (blackKingside && blackMailbox[61] == Piece.NONE && blackMailbox[62] == Piece.NONE) {
					if (whiteMailbox[61] == Piece.NONE && whiteMailbox[62] == Piece.NONE) {
						Move move = new Move(Piece.KING, 60, 62);
						move.isKingsideCastle = true;
						moves.add(move);
					} else {
						Move move = new Move(Piece.KING, 60, 62);
						move.isInvalidMove = true;
						moves.add(move);
					}
				}
				if (blackQueenside && blackMailbox[59] == Piece.NONE && blackMailbox[58] == Piece.NONE && blackMailbox[57] == Piece.NONE) {
					if (whiteMailbox[59] == Piece.NONE && whiteMailbox[58] == Piece.NONE && whiteMailbox[57] == Piece.NONE) {
						Move move = new Move(Piece.KING, 60, 58);
						move.isQueensideCastle = true;
						moves.add(move);
					} else {
						Move move = new Move(Piece.KING, 60, 58);
						move.isInvalidMove = true;
						moves.add(move);
					}
				}

				break;
			}
		}
		// pawns moves are handled separately
		// left captures
		long a = blackPawns << 9;
		long b = a & 0b1111111000000000000000000000000000000000000000000000000000000000L;
		long c = a & 0b0000000011111110111111101111111011111110111111101111111011111110L;
		// left captures, no promotion, no en passant
		for (long lb = c & whiteBoard; lb != 0; lb &= lb - 1) {
			int to = convert.get(lb & -lb);
			moves.add(new Move(Piece.PAWN, to + 9, to, whiteMailbox[to]));
		}
		// invalid left captures, no promotion, no en passant
		long invalidLeft = c & ~board;
		if (ep) {
			invalidLeft = (invalidLeft | sq[eps]) ^ sq[eps];
		}
		for (long lb = invalidLeft; lb != 0; lb &= lb - 1) {
			int to = convert.get(lb & -lb);
			Move move = new Move(Piece.PAWN, to + 9, to);
			move.isInvalidMove = true;
			moves.add(move);
		}
		// left captures, promotion
		for (long lb = b & whiteBoard; lb != 0; lb &= lb - 1) {
			int to = convert.get(lb & -lb);
			moves.add(new Move(Piece.PAWN, Piece.QUEEN, to + 9, to, whiteMailbox[to]));
			moves.add(new Move(Piece.PAWN, Piece.KNIGHT, to + 9, to, whiteMailbox[to]));
			moves.add(new Move(Piece.PAWN, Piece.ROOK, to + 9, to, whiteMailbox[to]));
			moves.add(new Move(Piece.PAWN, Piece.BISHOP, to + 9, to, whiteMailbox[to]));
		}
		// invalid left captures, promotion
		for (long lb = b & ~board; lb != 0; lb &= lb - 1) {
			int to = convert.get(lb & -lb);
			Move move = new Move(Piece.PAWN, Piece.QUEEN, to + 9, to);
			move.isInvalidMove = true;
			moves.add(move);
			move = new Move(Piece.PAWN, Piece.KNIGHT, to + 9, to);
			move.isInvalidMove = true;
			moves.add(move);
			move = new Move(Piece.PAWN, Piece.ROOK, to + 9, to);
			move.isInvalidMove = true;
			moves.add(move);
			move = new Move(Piece.PAWN, Piece.BISHOP, to + 9, to);
			move.isInvalidMove = true;
			moves.add(move);
		}
		// left captures, en passant
		if (ep && (c & sq[eps]) != 0) {
			Move move = new Move(Piece.PAWN, eps + 9, eps, Piece.PAWN);
			move.isEPCapture = true;
			moves.add(move);
		}
		// right captures
		a = blackPawns << 7;
		b = a & 0b0111111100000000000000000000000000000000000000000000000000000000L;
		c = a & 0b0000000001111111011111110111111101111111011111110111111101111111L;
		// right captures, no promotion, no en passant
		for (long lb = c & whiteBoard; lb != 0; lb &= lb - 1) {
			int to = convert.get(lb & -lb);
			moves.add(new Move(Piece.PAWN, to + 7, to, whiteMailbox[to]));
		}
		// invalid right captures, no promotion, no en passant
		long invalidRight = c & ~board;
		if (ep) {
			invalidRight = (invalidRight | sq[eps]) ^ sq[eps];
		}
		for (long lb = invalidRight; lb != 0; lb &= lb - 1) {
			int to = convert.get(lb & -lb);
			Move move = new Move(Piece.PAWN, to + 7, to);
			move.isInvalidMove = true;
			moves.add(move);
		}
		// right captures, promotion
		for (long lb = b & whiteBoard; lb != 0; lb &= lb - 1) {
			int to = convert.get(lb & -lb);
			moves.add(new Move(Piece.PAWN, Piece.QUEEN, to + 7, to, whiteMailbox[to]));
			moves.add(new Move(Piece.PAWN, Piece.KNIGHT, to + 7, to, whiteMailbox[to]));
			moves.add(new Move(Piece.PAWN, Piece.ROOK, to + 7, to, whiteMailbox[to]));
			moves.add(new Move(Piece.PAWN, Piece.BISHOP, to + 7, to, whiteMailbox[to]));
		}
		// invalid right captures, promotion
		for (long lb = b & ~board; lb != 0; lb &= lb - 1) {
			int to = convert.get(lb & -lb);
			Move move = new Move(Piece.PAWN, Piece.QUEEN, to + 7, to);
			move.isInvalidMove = true;
			moves.add(move);
			move = new Move(Piece.PAWN, Piece.KNIGHT, to + 7, to);
			move.isInvalidMove = true;
			moves.add(move);
			move = new Move(Piece.PAWN, Piece.ROOK, to + 7, to);
			move.isInvalidMove = true;
			moves.add(move);
			move = new Move(Piece.PAWN, Piece.BISHOP, to + 7, to);
			move.isInvalidMove = true;
			moves.add(move);
		}
		// right captures, en passant
		if (ep && (c & sq[eps]) != 0) {
			Move move = new Move(Piece.PAWN, eps + 7, eps, Piece.PAWN);
			move.isEPCapture = true;
			moves.add(move);
		}
		// single push
		long singleMap = blackPawns << 8;
		for (long sb = singleMap & ~board; sb != 0; sb &= sb - 1) {
			int to = convert.get(sb & -sb);
			moves.add(new Move(Piece.PAWN, to + 8, to));
		}
		// single push blocked
		for (long sb = singleMap & whiteBoard; sb != 0; sb &= sb - 1) {
			int to = convert.get(sb & -sb);
			Move move = new Move(Piece.PAWN, to + 8, to);
			move.isInvalidMove = true;
			moves.add(move);
		}
		// double push
		long doubleMap = (blackPawns << 16) & 0b0000000000000000000000000000000011111111000000000000000000000000L & ((singleMap & ~board) << 8);
		for (long db = doubleMap & ~board; db != 0; db &= db - 1) {
			int to = convert.get(db & -db);
			Move move = new Move(Piece.PAWN, to + 16, to);
			move.isDPMove = true;
			moves.add(move);
		}
		// double push blocked
		for (long db = doubleMap & blackBoard; db != 0; db &= db - 1) {
			int to = convert.get(db & -db);
			Move move = new Move(Piece.PAWN, to + 16, to, to + 8);
			moves.add(move);
		}
		return moves;
	}

	private void whiteMoveQuiet(Move m) {
		long fromBB = sq[m.from], toBB = sq[m.to], fromToBB = fromBB ^ toBB;
		switch (m.p) {
		case KNIGHT:
			whiteKnights ^= fromToBB;
			break;
		case ROOK:
			whiteRooks ^= fromToBB;
			if (whiteKingside && m.from == 7) {
				whiteKingside = false;
			}
			if (whiteQueenside && m.from == 0) {
				whiteQueenside = false;
			}
			break;
		case BISHOP:
			whiteBishops ^= fromToBB;
			break;
		case QUEEN:
			whiteQueens ^= fromToBB;
			break;
		case KING:
			whiteKing ^= fromToBB;
			whiteKingside = false;
			whiteQueenside = false;
			break;
		case PAWN:
			whitePawns ^= fromToBB;
			break;
		}
		whiteBoard ^= fromToBB;
		board ^= fromToBB;

		//        hc ^= wt.get(whiteMailbox[m.from]).get(m.from);
		//        hc ^= wt.get(Piece.NONE).get(m.from);
		whiteMailbox[m.from] = Piece.NONE;

		//        hc ^= wt.get(whiteMailbox[m.to]).get(m.to);
		//        hc ^= wt.get(m.p).get(m.to);
		whiteMailbox[m.to] = m.p;
	}

	private void blackMoveQuiet(Move m) {
		long fromBB = sq[m.from], toBB = sq[m.to], fromToBB = fromBB ^ toBB;
		switch (m.p) {
		case KNIGHT:
			blackKnights ^= fromToBB;
			break;
		case ROOK:
			blackRooks ^= fromToBB;
			if (blackKingside && m.from == 63) {
				blackKingside = false;
			}
			if (blackQueenside && m.from == 56) {
				blackQueenside = false;
			}
			break;
		case BISHOP:
			blackBishops ^= fromToBB;
			break;
		case QUEEN:
			blackQueens ^= fromToBB;
			break;
		case KING:
			blackKing ^= fromToBB;
			blackKingside = false;
			blackQueenside = false;
			break;
		case PAWN:
			blackPawns ^= fromToBB;
			break;
		}
		blackBoard ^= fromToBB;
		board ^= fromToBB;

		//        hc ^= bt.get(blackMailbox[m.from]).get(m.from);
		//        hc ^= bt.get(Piece.NONE).get(m.from);
		blackMailbox[m.from] = Piece.NONE;

		//        hc ^= bt.get(blackMailbox[m.to]).get(m.to);
		//        hc ^= bt.get(m.p).get(m.to);
		blackMailbox[m.to] = m.p;
	}

	private void whiteMoveCapture(Move m) {
		long fromBB = sq[m.from], toBB = sq[m.to], fromToBB = fromBB ^ toBB;
		switch (m.p) {
		case KNIGHT:
			whiteKnights ^= fromToBB;
			break;
		case ROOK:
			whiteRooks ^= fromToBB;
			if (whiteKingside && m.from == 63) {
				whiteKingside = false;
			}
			if (whiteQueenside && m.from == 56) {
				whiteQueenside = false;
			}
			break;
		case BISHOP:
			whiteBishops ^= fromToBB;
			break;
		case QUEEN:
			whiteQueens ^= fromToBB;
			break;
		case KING:
			whiteKing ^= fromToBB;
			whiteKingside = false;
			whiteQueenside = false;
			break;
		case PAWN:
			whitePawns ^= fromToBB;
			break;
		}
		whiteBoard ^= fromToBB;
		switch (m.q) {
		case KNIGHT:
			blackKnights ^= toBB;
			break;
		case ROOK:
			blackRooks ^= toBB;
			break;
		case BISHOP:
			blackBishops ^= toBB;
			break;
		case QUEEN:
			blackQueens ^= toBB;
			break;
		case KING:
			blackKing ^= toBB;
			endGame = true;
			hasBlackKing = false;
			break;
		case PAWN:
			blackPawns ^= toBB;
			break;
		}
		blackBoard ^= toBB;
		board ^= fromBB;

		//		hc ^= wt.get(whiteMailbox[m.from]).get(m.from);
		//		hc ^= wt.get(Piece.NONE).get(m.from);
		whiteMailbox[m.from] = Piece.NONE;

		//		hc ^= wt.get(whiteMailbox[m.to]).get(m.to);
		//		hc ^= wt.get(m.p).get(m.to);
		whiteMailbox[m.to] = m.p;

		//		hc ^= bt.get(blackMailbox[m.to]).get(m.to);
		//		hc ^= bt.get(Piece.NONE).get(m.to);
		blackMailbox[m.to] = Piece.NONE;
	}

	private void blackMoveCapture(Move m) {
		long fromBB = sq[m.from], toBB = sq[m.to], fromToBB = fromBB ^ toBB;
		switch (m.p) {
		case KNIGHT:
			blackKnights ^= fromToBB;
			break;
		case ROOK:
			blackRooks ^= fromToBB;
			if (blackKingside && m.from == 63) {
				blackKingside = false;
			}
			if (blackQueenside && m.from == 56) {
				blackQueenside = false;
			}
			break;
		case BISHOP:
			blackBishops ^= fromToBB;
			break;
		case QUEEN:
			blackQueens ^= fromToBB;
			break;
		case KING:
			blackKing ^= fromToBB;
			blackKingside = false;
			blackQueenside = false;
			break;
		case PAWN:
			blackPawns ^= fromToBB;
			break;
		}
		blackBoard ^= fromToBB;
		switch (m.q) {
		case KNIGHT:
			whiteKnights ^= toBB;
			break;
		case ROOK:
			whiteRooks ^= toBB;
			break;
		case BISHOP:
			whiteBishops ^= toBB;
			break;
		case QUEEN:
			whiteQueens ^= toBB;
			break;
		case KING:
			whiteKing ^= toBB;
			endGame = true;
			hasWhiteKing = false;
			break;
		case PAWN:
			whitePawns ^= toBB;
			break;
		}
		whiteBoard ^= toBB;
		board ^= fromBB;

		//		hc ^= bt.get(blackMailbox[m.from]).get(m.from);
		//		hc ^= bt.get(Piece.NONE).get(m.from);
		blackMailbox[m.from] = Piece.NONE;

		//		hc ^= bt.get(blackMailbox[m.to]).get(m.to);
		//		hc ^= bt.get(m.p).get(m.to);
		blackMailbox[m.to] = m.p;

		//		hc ^= wt.get(whiteMailbox[m.to]).get(m.to);
		//		hc ^= wt.get(Piece.NONE).get(m.to);
		whiteMailbox[m.to] = Piece.NONE;
	}

	private void whiteKingsideCastle() {
		whiteMoveQuiet(new Move(Piece.KING, 4, 6));
		whiteMoveQuiet(new Move(Piece.ROOK, 7, 5));
	}

	private void blackKingsideCastle() {
		blackMoveQuiet(new Move(Piece.KING, 60, 62));
		blackMoveQuiet(new Move(Piece.ROOK, 63, 61));
	}

	private void whiteQueensideCastle() {
		whiteMoveQuiet(new Move(Piece.KING, 4, 2));
		whiteMoveQuiet(new Move(Piece.ROOK, 0, 3));
	}

	private void blackQueensideCastle() {
		blackMoveQuiet(new Move(Piece.KING, 60, 58));
		blackMoveQuiet(new Move(Piece.ROOK, 56, 59));
	}

	private void whiteDPMove(Move m) {
		whiteMoveQuiet(m);
		ep = true;
		eps = (m.from + m.to) / 2;
	}

	private void blackDPMove(Move m) {
		blackMoveQuiet(m);
		ep = true;
		eps = (m.from + m.to) / 2;
	}

	private void whitePromote(Move m) {
		long fromBB = sq[m.from], toBB = sq[m.to], fromToBB = fromBB ^ toBB;
		whitePawns ^= fromBB;
		if (m.r == Piece.QUEEN) {
			whiteQueens ^= toBB;
		} else {
			whiteBishops ^= toBB;
		}
		if (m.isCapture) {
			switch (m.q) {
			case KNIGHT:
				blackKnights ^= toBB;
				break;
			case ROOK:
				blackRooks ^= toBB;
				break;
			case BISHOP:
				blackBishops ^= toBB;
				break;
			case QUEEN:
				blackQueens ^= toBB;
				break;
			case KING:
				blackKing ^= toBB;
				endGame = true;
				hasBlackKing = false;
				break;
			case PAWN:
				blackPawns ^= toBB;
				break;
			}

			//			hc ^= bt.get(blackMailbox[m.to]).get(m.to);
			//			hc ^= bt.get(Piece.NONE).get(m.to);
			blackMailbox[m.to] = Piece.NONE;

			blackBoard ^= toBB;
			board ^= fromBB;
		} else {
			board ^= fromToBB;
		}
		whiteBoard ^= fromToBB;

		//		hc ^= wt.get(whiteMailbox[m.from]).get(m.from);
		//		hc ^= wt.get(Piece.NONE).get(m.from);
		whiteMailbox[m.from] = Piece.NONE;

		//		hc ^= wt.get(whiteMailbox[m.to]).get(m.to);
		//		hc ^= wt.get(m.r).get(m.to);
		whiteMailbox[m.to] = m.r;
		ep = false;
	}

	private void blackPromote(Move m) {
		long fromBB = sq[m.from], toBB = sq[m.to], fromToBB = fromBB ^ toBB;
		blackPawns ^= fromBB;
		if (m.r == Piece.QUEEN) {
			blackQueens ^= toBB;
		} else {
			blackBishops ^= toBB;
		}
		if (m.isCapture) {
			switch (m.q) {
			case KNIGHT:
				whiteKnights ^= toBB;
				break;
			case ROOK:
				whiteRooks ^= toBB;
				break;
			case BISHOP:
				whiteBishops ^= toBB;
				break;
			case QUEEN:
				whiteQueens ^= toBB;
				break;
			case KING:
				whiteKing ^= toBB;
				endGame = true;
				hasWhiteKing = false;
				break;
			case PAWN:
				whitePawns ^= toBB;
				break;
			}

			//			hc ^= wt.get(whiteMailbox[m.to]).get(m.to);
			//			hc ^= wt.get(Piece.NONE).get(m.to);
			whiteMailbox[m.to] = Piece.NONE;

			whiteBoard ^= toBB;
			board ^= fromBB;
		} else {
			board ^= fromToBB;
		}
		blackBoard ^= fromToBB;

		//		hc ^= bt.get(blackMailbox[m.from]).get(m.from);
		//		hc ^= bt.get(Piece.NONE).get(m.from);
		blackMailbox[m.from] = Piece.NONE;

		//		hc ^= bt.get(blackMailbox[m.to]).get(m.to);
		//		hc ^= bt.get(m.r).get(m.to);
		blackMailbox[m.to] = m.r;
		ep = false;
	}

	private void whiteEPCapture(Move m) {
		whiteMoveQuiet(m);
		int cap = 8 * (m.from / 8) + (m.to % 8);
		blackPawns ^= sq[cap];
		blackBoard ^= sq[cap];
		board ^= sq[cap];

		//        hc ^= bt.get(blackMailbox[cap]).get(cap);
		//        hc ^= bt.get(Piece.NONE).get(cap);
		blackMailbox[cap] = Piece.NONE;
	}

	private void blackEPCapture(Move m) {
		blackMoveQuiet(m);
		int cap = 8 * (m.from / 8) + (m.to % 8);
		whitePawns ^= sq[cap];
		whiteBoard ^= sq[cap];
		board ^= sq[cap];

		//        hc ^= wt.get(whiteMailbox[cap]).get(cap);
		//        hc ^= wt.get(Piece.NONE).get(cap);
		whiteMailbox[cap] = Piece.NONE;
	}

	private void whiteInvalidMove() {

	}

	private void blackInvalidMove() {

	}

	private void whiteBlockedMove(Move m) {
		if (m.p == Piece.PAWN) {
			whiteMoveQuiet(new Move(m.p, m.from, m.block));
		} else {
			whiteMoveCapture(new Move(m.p, m.from, m.block, m.q));
		}
	}

	private void blackBlockedMove(Move m) {
		if (m.p == Piece.PAWN) {
			blackMoveQuiet(new Move(m.p, m.from, m.block));
		} else {
			blackMoveCapture(new Move(m.p, m.from, m.block, m.q));
		}
	}

	public void whiteMove(Move m) {
		prevWhiteKingside.addLast(whiteKingside);
		prevWhiteQueenside.addLast(whiteQueenside);
		prevEp.addLast(ep);
		prevEps.addLast(eps);
		ep = false;
		if (m.isInvalidMove) {
			whiteInvalidMove();
		} else if (m.isBlockedMove) {
			whiteBlockedMove(m);
		} else if (m.isEPCapture) {
			whiteEPCapture(m);
		} else if (m.isPromotion) {
			whitePromote(m);
		} else if (m.isCapture) {
			whiteMoveCapture(m);
		} else if (m.isKingsideCastle) {
			whiteKingsideCastle();
		} else if (m.isQueensideCastle) {
			whiteQueensideCastle();
		} else if (m.isDPMove) {
			whiteDPMove(m);
		} else {
			whiteMoveQuiet(m);
		}
	}

	public void blackMove(Move m) {
		prevBlackKingside.addLast(blackKingside);
		prevBlackQueenside.addLast(blackQueenside);
		prevEp.addLast(ep);
		prevEps.addLast(eps);
		ep = false;
		if (m.isInvalidMove) {
			blackInvalidMove();
		} else if (m.isBlockedMove) {
			blackBlockedMove(m);
		} else if (m.isEPCapture) {
			blackEPCapture(m);
		} else if (m.isPromotion) {
			blackPromote(m);
		} else if (m.isCapture) {
			blackMoveCapture(m);
		} else if (m.isKingsideCastle) {
			blackKingsideCastle();
		} else if (m.isQueensideCastle) {
			blackQueensideCastle();
		} else if (m.isDPMove) {
			blackDPMove(m);
		} else {
			blackMoveQuiet(m);
		}
	}

	private void undoWhiteMoveQuiet(Move m) {
		long fromBB = sq[m.from], toBB = sq[m.to], fromToBB = fromBB ^ toBB;
		switch (m.p) {
		case KNIGHT:
			whiteKnights ^= fromToBB;
			break;
		case ROOK:
			whiteRooks ^= fromToBB;
			break;
		case BISHOP:
			whiteBishops ^= fromToBB;
			break;
		case QUEEN:
			whiteQueens ^= fromToBB;
			break;
		case KING:
			whiteKing ^= fromToBB;
			break;
		case PAWN:
			whitePawns ^= fromToBB;
			break;
		}
		whiteBoard ^= fromToBB;
		board ^= fromToBB;

		//        hc ^= wt.get(whiteMailbox[m.from]).get(m.from);
		//        hc ^= wt.get(m.p).get(m.from);
		whiteMailbox[m.from] = m.p;

		//        hc ^= wt.get(whiteMailbox[m.to]).get(m.to);
		//        hc ^= wt.get(Piece.NONE).get(m.to);
		whiteMailbox[m.to] = Piece.NONE;
	}

	private void undoBlackMoveQuiet(Move m) {
		long fromBB = sq[m.from], toBB = sq[m.to], fromToBB = fromBB ^ toBB;
		switch (m.p) {
		case KNIGHT:
			blackKnights ^= fromToBB;
			break;
		case ROOK:
			blackRooks ^= fromToBB;
			break;
		case BISHOP:
			blackBishops ^= fromToBB;
			break;
		case QUEEN:
			blackQueens ^= fromToBB;
			break;
		case KING:
			blackKing ^= fromToBB;
			break;
		case PAWN:
			blackPawns ^= fromToBB;
			break;
		}
		blackBoard ^= fromToBB;
		board ^= fromToBB;

		//        hc ^= bt.get(blackMailbox[m.from]).get(m.from);
		//        hc ^= bt.get(m.p).get(m.from);
		blackMailbox[m.from] = m.p;

		//        hc ^= bt.get(blackMailbox[m.to]).get(m.to);
		//        hc ^= bt.get(Piece.NONE).get(m.to);
		blackMailbox[m.to] = Piece.NONE;
	}

	private void undoWhiteMoveCapture(Move m) {
		long fromBB = sq[m.from], toBB = sq[m.to], fromToBB = fromBB ^ toBB;
		switch (m.p) {
		case KNIGHT:
			whiteKnights ^= fromToBB;
			break;
		case ROOK:
			whiteRooks ^= fromToBB;
			break;
		case BISHOP:
			whiteBishops ^= fromToBB;
			break;
		case QUEEN:
			whiteQueens ^= fromToBB;
			break;
		case KING:
			whiteKing ^= fromToBB;
			break;
		case PAWN:
			whitePawns ^= fromToBB;
			break;
		}
		whiteBoard ^= fromToBB;
		switch (m.q) {
		case KNIGHT:
			blackKnights ^= toBB;
			break;
		case ROOK:
			blackRooks ^= toBB;
			break;
		case BISHOP:
			blackBishops ^= toBB;
			break;
		case QUEEN:
			blackQueens ^= toBB;
			break;
		case KING:
			blackKing ^= toBB;
			break;
		case PAWN:
			blackPawns ^= toBB;
			break;
		}
		blackBoard ^= toBB;
		board ^= fromBB;

		//        hc ^= wt.get(whiteMailbox[m.from]).get(m.from);
		//        hc ^= wt.get(m.p).get(m.from);
		whiteMailbox[m.from] = m.p;

		//        hc ^= wt.get(whiteMailbox[m.to]).get(m.to);
		//        hc ^= wt.get(Piece.NONE).get(m.to);
		whiteMailbox[m.to] = Piece.NONE;

		//        hc ^= bt.get(blackMailbox[m.to]).get(m.to);
		//        hc ^= bt.get(m.q).get(m.to);
		blackMailbox[m.to] = m.q;
	}

	private void undoBlackMoveCapture(Move m) {
		long fromBB = sq[m.from], toBB = sq[m.to], fromToBB = fromBB ^ toBB;
		switch (m.p) {
		case KNIGHT:
			blackKnights ^= fromToBB;
			break;
		case ROOK:
			blackRooks ^= fromToBB;
			break;
		case BISHOP:
			blackBishops ^= fromToBB;
			break;
		case QUEEN:
			blackQueens ^= fromToBB;
			break;
		case KING:
			blackKing ^= fromToBB;
			break;
		case PAWN:
			blackPawns ^= fromToBB;
			break;
		}
		blackBoard ^= fromToBB;
		switch (m.q) {
		case KNIGHT:
			whiteKnights ^= toBB;
			break;
		case ROOK:
			whiteRooks ^= toBB;
			break;
		case BISHOP:
			whiteBishops ^= toBB;
			break;
		case QUEEN:
			whiteQueens ^= toBB;
			break;
		case KING:
			whiteKing ^= toBB;
			break;
		case PAWN:
			whitePawns ^= toBB;
			break;
		}
		whiteBoard ^= toBB;
		board ^= fromBB;

		//        hc ^= bt.get(blackMailbox[m.from]).get(m.from);
		//        hc ^= bt.get(m.p).get(m.from);
		blackMailbox[m.from] = m.p;

		//        hc ^= bt.get(blackMailbox[m.to]).get(m.to);
		//        hc ^= bt.get(Piece.NONE).get(m.to);
		blackMailbox[m.to] = Piece.NONE;

		//        hc ^= wt.get(whiteMailbox[m.to]).get(m.to);
		//        hc ^= wt.get(m.q).get(m.to);
		whiteMailbox[m.to] = m.q;
	}

	private void undoWhiteKingsideCastle() {
		undoWhiteMoveQuiet(new Move(Piece.KING, 4, 6));
		undoWhiteMoveQuiet(new Move(Piece.ROOK, 7, 5));
	}

	private void undoBlackKingsideCastle() {
		undoBlackMoveQuiet(new Move(Piece.KING, 60, 62));
		undoBlackMoveQuiet(new Move(Piece.ROOK, 63, 61));
	}

	private void undoWhiteQueensideCastle() {
		undoWhiteMoveQuiet(new Move(Piece.KING, 4, 2));
		undoWhiteMoveQuiet(new Move(Piece.ROOK, 0, 3));
	}

	private void undoBlackQueensideCastle() {
		undoBlackMoveQuiet(new Move(Piece.KING, 60, 58));
		undoBlackMoveQuiet(new Move(Piece.ROOK, 56, 59));
	}

	private void undoWhiteDPMove(Move m) {
		undoWhiteMoveQuiet(m);
	}

	private void undoBlackDPMove(Move m) {
		undoBlackMoveQuiet(m);
	}

	private void undoWhitePromote(Move m) {
		long fromBB = sq[m.from], toBB = sq[m.to], fromToBB = fromBB ^ toBB;
		whitePawns ^= fromBB;
		if (m.r == Piece.QUEEN) {
			whiteQueens ^= toBB;
		} else {
			whiteBishops ^= toBB;
		}
		if (m.isCapture) {
			switch (m.q) {
			case KNIGHT:
				blackKnights ^= toBB;
				break;
			case ROOK:
				blackRooks ^= toBB;
				break;
			case BISHOP:
				blackBishops ^= toBB;
				break;
			case QUEEN:
				blackQueens ^= toBB;
				break;
			case KING:
				blackKing ^= toBB;
				break;
			case PAWN:
				blackPawns ^= toBB;
				break;
			}

			//            hc ^= bt.get(blackMailbox[m.to]).get(m.to);
			//            hc ^= bt.get(m.q).get(m.to);
			blackMailbox[m.to] = m.q;

			blackBoard ^= toBB;
			board ^= fromBB;
		} else {
			board ^= fromToBB;
		}
		whiteBoard ^= fromToBB;

		//        hc ^= wt.get(whiteMailbox[m.from]).get(m.from);
		//        hc ^= wt.get(m.p).get(m.from);
		whiteMailbox[m.from] = m.p;

		//        hc ^= wt.get(whiteMailbox[m.to]).get(m.to);
		//        hc ^= wt.get(Piece.NONE).get(m.to);
		whiteMailbox[m.to] = Piece.NONE;
	}

	private void undoBlackPromote(Move m) {
		long fromBB = sq[m.from], toBB = sq[m.to], fromToBB = fromBB ^ toBB;
		blackPawns ^= fromBB;
		if (m.r == Piece.QUEEN) {
			blackQueens ^= toBB;
		} else {
			blackBishops ^= toBB;
		}
		if (m.isCapture) {
			switch (m.q) {
			case KNIGHT:
				whiteKnights ^= toBB;
				break;
			case ROOK:
				whiteRooks ^= toBB;
				break;
			case BISHOP:
				whiteBishops ^= toBB;
				break;
			case QUEEN:
				whiteQueens ^= toBB;
				break;
			case KING:
				whiteKing ^= toBB;
				break;
			case PAWN:
				whitePawns ^= toBB;
				break;
			}

			//            hc ^= wt.get(whiteMailbox[m.to]).get(m.to);
			//            hc ^= wt.get(m.q).get(m.to);
			whiteMailbox[m.to] = m.q;

			whiteBoard ^= toBB;
			board ^= fromBB;
		} else {
			board ^= fromToBB;
		}
		blackBoard ^= fromToBB;

		//        hc ^= bt.get(blackMailbox[m.from]).get(m.from);
		//        hc ^= bt.get(m.p).get(m.from);
		blackMailbox[m.from] = m.p;

		//        hc ^= bt.get(blackMailbox[m.to]).get(m.to);
		//        hc ^= bt.get(Piece.NONE).get(m.to);
		blackMailbox[m.to] = Piece.NONE;
	}

	private void undoWhiteEPCapture(Move m) {
		undoWhiteMoveQuiet(m);
		int cap = 8 * (m.from / 8) + (m.to % 8);
		blackPawns ^= sq[cap];
		blackBoard ^= sq[cap];
		board ^= sq[cap];

		//        hc ^= bt.get(blackMailbox[cap]).get(cap);
		//        hc ^= bt.get(Piece.PAWN).get(cap);
		blackMailbox[cap] = Piece.PAWN;
	}

	private void undoBlackEPCapture(Move m) {
		undoBlackMoveQuiet(m);
		int cap = 8 * (m.from / 8) + (m.to % 8);
		whitePawns ^= sq[cap];
		whiteBoard ^= sq[cap];
		board ^= sq[cap];

		//        hc ^= wt.get(whiteMailbox[cap]).get(cap);
		//        hc ^= wt.get(Piece.PAWN).get(cap);
		whiteMailbox[cap] = Piece.PAWN;
	}

	private void undoWhiteInvalidMove() {
	}

	private void undoBlackInvalidMove() {
	}

	private void undoWhiteBlockedMove(Move m) {
		if (m.p == Piece.PAWN) {
			undoWhiteMoveQuiet(new Move(m.p, m.from, m.block));
		} else {
			undoWhiteMoveCapture(new Move(m.p, m.from, m.block, m.q));
		}
	}

	private void undoBlackBlockedMove(Move m) {
		if (m.p == Piece.PAWN) {
			undoWhiteMoveQuiet(new Move(m.p, m.from, m.block));
		} else {
			undoWhiteMoveCapture(new Move(m.p, m.from, m.block, m.q));
		}
	}

	public void undoWhiteMove(Move m) {
		if (m.isInvalidMove) {
			undoWhiteInvalidMove();
		} else if (m.isBlockedMove) {
			undoWhiteBlockedMove(m);
		} else if (m.isEPCapture) {
			undoWhiteEPCapture(m);
		} else if (m.isPromotion) {
			undoWhitePromote(m);
		} else if (m.isCapture) {
			undoWhiteMoveCapture(m);
		} else if (m.isKingsideCastle) {
			undoWhiteKingsideCastle();
		} else if (m.isQueensideCastle) {
			undoWhiteQueensideCastle();
		} else if (m.isDPMove) {
			undoWhiteDPMove(m);
		} else {
			undoWhiteMoveQuiet(m);
		}
		whiteKingside = prevWhiteKingside.pollLast();
		whiteQueenside = prevWhiteQueenside.pollLast();
		ep = prevEp.pollLast();
		eps = prevEps.pollLast();
		endGame = false;
		hasBlackKing = hasWhiteKing = true;
	}

	public void undoBlackMove(Move m) {
		if (m.isEPCapture) {
			undoBlackEPCapture(m);
		} else if (m.isCapture) {
			undoBlackMoveCapture(m);
		} else if (m.isKingsideCastle) {
			undoBlackKingsideCastle();
		} else if (m.isQueensideCastle) {
			undoBlackQueensideCastle();
		} else if (m.isDPMove) {
			undoBlackDPMove(m);
		} else if (m.isPromotion) {
			undoBlackPromote(m);
		} else if (m.isInvalidMove) {
			undoBlackInvalidMove();
		} else {
			undoBlackMoveQuiet(m);
		}
		// note: ep does not need to be updated because when searching the game tree, every undo is followed by a new
		// move that rewrites ep anyway.
		blackKingside = prevBlackKingside.pollLast();
		blackQueenside = prevBlackQueenside.pollLast();
		ep = prevEp.pollLast();
		eps = prevEps.pollLast();
		endGame = false;
		hasBlackKing = hasWhiteKing = true;
	}

	public String sense(int x) {
		StringBuilder sb = new StringBuilder();
		for (int j = 7; j >= -9; j -= 8) {
			for (int i = 0; i <= 2; i++) {
				Piece w = whiteMailbox[x + j + i], b = blackMailbox[x + j + i];
				if (w != Piece.NONE) {
					sb.append(w);
				} else {
					sb.append(b.toString().toLowerCase());
				}
			}
		}
		return sb.toString();
	}

	// color is player to move
	public double scoreOld(double[] genes, double[][] convolutions, int color) {
		if (!hasWhiteKing)
			return -10000000;
		if (!hasBlackKing)
			return 10000000;

		ArrayList<double[][]> l2 = new ArrayList<>();
		l2.add(new double[6][64]);
		l2.add(new double[6][64]);
		l2.add(new double[5][64]);
		l2.add(new double[4][64]);
		l2.add(new double[3][64]);
		l2.add(new double[2][64]);
		l2.add(new double[1][64]);

		// white
		for (long b = whiteBoard; b != 0; b &= b - 1) {
			int from = convert.get(b & -b);
			switch (whiteMailbox[from]) {
			case KNIGHT:
				long knightMap = knightMoves[from] & ~whiteBoard;
				for (long nb = knightMap; nb != 0; nb &= nb - 1) {
					int to = convert.get(nb & -nb);
					l2.get(0)[0][to] += genes[0 + 64] * genes[to];
				}
				break;
			case ROOK:
				long rookMap = (slideAttacks(from, rankMask[from]) ^ slideAttacks(from, fileMask[from])) & ~whiteBoard;
				for (long rb = rookMap; rb != 0; rb &= rb - 1) {
					int to = convert.get(rb & -rb);
					l2.get(0)[1][to] += genes[1 + 64] * genes[to];
				}
				break;
			case BISHOP:
				long bishopMap = (slideAttacks(from, diagonalMask[from]) ^ slideAttacks(from, antiDiagonalMask[from])) & ~whiteBoard;
				for (long bb = bishopMap; bb != 0; bb &= bb - 1) {
					int to = convert.get(bb & -bb);
					l2.get(0)[2][to] += genes[2 + 64] * genes[to];
				}
				break;
			case QUEEN:
				long queenMap = (slideAttacks(from, rankMask[from]) ^ slideAttacks(from, fileMask[from]) ^ slideAttacks(from, diagonalMask[from]) ^ slideAttacks(from, antiDiagonalMask[from])) & ~whiteBoard;
				for (long qb = queenMap; qb != 0; qb &= qb - 1) {
					int to = convert.get(qb & -qb);
					l2.get(0)[3][to] += genes[3 + 64] * genes[to];
				}
				break;
			case KING:
				long kingMap = kingMoves[from] & ~whiteBoard;
				for (long kb = kingMap; kb != 0; kb &= kb - 1) {
					int to = convert.get(kb & -kb);
					l2.get(0)[4][to] += genes[4 + 64] * genes[to];
				}
				break;
			}
		}
		// pawns moves are handled separately
		// left captures
		long a = whitePawns >>> 7;
		long d = a & 0b1111111011111110111111101111111011111110111111101111111011111110L;
		// left captures, no promotion, no en passant
		for (long lb = d & blackBoard; lb != 0; lb &= lb - 1) {
			int to = convert.get(lb & -lb);
			l2.get(0)[5][to] += genes[5 + 64] * genes[to];
		}
		// left captures, en passant
		if (ep && (d & sq[eps]) != 0 && color == 1) {
			l2.get(0)[5][eps - 8] += genes[5 + 64] * genes[eps - 8];
		}
		// right captures
		a = whitePawns >>> 9;
		d = a & 0b0111111101111111011111110111111101111111011111110111111101111111L;
		// right captures, no promotion, no en passant
		for (long lb = d & blackBoard; lb != 0; lb &= lb - 1) {
			int to = convert.get(lb & -lb);
			l2.get(0)[5][to] += genes[5 + 64] * genes[to];
		}
		// right captures, en passant
		if (ep && (d & sq[eps]) != 0 && color == 1) {
			l2.get(0)[5][eps - 8] += genes[5 + 64] * genes[eps - 8];
		}
		// black
		for (long b = blackBoard; b != 0; b &= b - 1) {
			int from = convert.get(b & -b);
			switch (blackMailbox[from]) {
			case KNIGHT:
				long knightMap = knightMoves[from] & ~blackBoard;
				for (long nb = knightMap; nb != 0; nb &= nb - 1) {
					int to = convert.get(nb & -nb);
					l2.get(0)[0][to] -= genes[6 + 64] * genes[to];
				}
				break;
			case ROOK:
				long rookMap = (slideAttacks(from, rankMask[from]) ^ slideAttacks(from, fileMask[from])) & ~blackBoard;
				for (long rb = rookMap; rb != 0; rb &= rb - 1) {
					int to = convert.get(rb & -rb);
					l2.get(0)[1][to] -= genes[7 + 64] * genes[to];
				}
				break;
			case BISHOP:
				long bishopMap = (slideAttacks(from, diagonalMask[from]) ^ slideAttacks(from, antiDiagonalMask[from])) & ~blackBoard;
				for (long bb = bishopMap; bb != 0; bb &= bb - 1) {
					int to = convert.get(bb & -bb);
					l2.get(0)[2][to] -= genes[8 + 64] * genes[to];
				}
				break;
			case QUEEN:
				long queenMap = (slideAttacks(from, rankMask[from]) ^ slideAttacks(from, fileMask[from]) ^ slideAttacks(from, diagonalMask[from]) ^ slideAttacks(from, antiDiagonalMask[from])) & ~blackBoard;
				for (long qb = queenMap; qb != 0; qb &= qb - 1) {
					int to = convert.get(qb & -qb);
					l2.get(0)[3][to] -= genes[9 + 64] * genes[to];
				}
				break;
			case KING:
				long kingMap = kingMoves[from] & ~blackBoard;
				for (long kb = kingMap; kb != 0; kb &= kb - 1) {
					int to = convert.get(kb & -kb);
					l2.get(0)[4][to] -= genes[10 + 64] * genes[to];
				}
				break;
			}
		}
		a = blackPawns << 9;
		d = a & 0b1111111011111110111111101111111011111110111111101111111011111110L;
		// left captures, no promotion, no en passant
		for (long lb = d & whiteBoard; lb != 0; lb &= lb - 1) {
			int to = convert.get(lb & -lb);
			l2.get(0)[5][to] -= genes[11 + 64] * genes[to];
		}
		// left captures, en passant
		if (ep && (d & sq[eps]) != 0) {
			l2.get(0)[5][eps + 8] -= genes[11 + 64] * genes[eps + 8];
		}
		// right captures
		a = blackPawns << 7;
		d = a & 0b0111111101111111011111110111111101111111011111110111111101111111L;
		// right captures, no promotion, no en passant
		for (long lb = d & whiteBoard; lb != 0; lb &= lb - 1) {
			int to = convert.get(lb & -lb);
			l2.get(0)[5][to] -= genes[11 + 64] * genes[to];
		}
		// right captures, en passant
		if (ep && (d & sq[eps]) != 0) {
			l2.get(0)[5][eps + 8] -= genes[11 + 64] * genes[eps + 8];
		}
		return 0; //Outdated method
	}

	// color is player to move
	public double score(double[] genes, double[][] convolutions) {
		if (!hasWhiteKing)
			return -10000000;
		if (!hasBlackKing)
			return 10000000;

		ArrayList<double[][]> l2 = new ArrayList<>();
		l2.add(new double[6][64]);
		l2.add(new double[3][64]);
		l2.add(new double[2][64]);
		l2.add(new double[1][64]);

		for (int sq = 0; sq < 64; sq++) {
			switch (whiteMailbox[sq]) {
			case PAWN:
				l2.get(0)[0][sq] = 100;
				break;
			case KNIGHT:
				l2.get(0)[1][sq] = 100;
				break;
			case BISHOP:
				l2.get(0)[2][sq] = 100;
				break;
			case ROOK:
				l2.get(0)[3][sq] = 100;
				break;
			case QUEEN:
				l2.get(0)[4][sq] = 100;
				break;
			case KING:
				l2.get(0)[5][sq] = 100;
				break;
			}
			switch (blackMailbox[sq]) {
			case PAWN:
				l2.get(0)[0][sq] = -100;
				break;
			case KNIGHT:
				l2.get(0)[1][sq] = -100;
				break;
			case BISHOP:
				l2.get(0)[2][sq] = -100;
				break;
			case ROOK:
				l2.get(0)[3][sq] = -100;
				break;
			case QUEEN:
				l2.get(0)[4][sq] = -100;
				break;
			case KING:
				l2.get(0)[5][sq] = -100;
				break;
			}
		}

		double out = 0;

		int lap = 64 + 13;

		//System.out.println(l2.get(0)[1][1]);
		int convNo = 0;
		for (int l = 0; l < l2.size() - 1; l++) { // In each layer,
			for (int c = 0; c < l2.get(l + 1).length; c++) { // For board c in (l+1)
				for (int b = 0; b < l2.get(l).length; b++) { // For each board
					convolute(l2.get(l)[b], genes[lap], l2.get(l + 1)[c], convolutions[convNo++]); // Convolute the board and add it to the next layer.
					lap++;
				}
				activate(l2.get(l + 1)[c]);
			}
		}

		for (int sq = 0; sq < 64; sq++)
			out += l2.get(l2.size() - 1)[0][sq];

		//System.out.println(lap);
		return out;
	}

	public static void printArray(double matrix[][]) {
		for (int row = 0; row < matrix.length; row++)
			for (int column = 0; column < matrix[row].length; column++)
				System.out.print(matrix[row][column] + " ");
	}

	public static void printArray(double matrix[]) {
		for (int row = 0; row < matrix.length; row++)
			System.out.println(matrix[row] + " ");
	}

	private static void convolute(double[] in, double weight, double[] out, double[] convolution) {
		double[] edit = new double[in.length];
		for (int i = 0; i < in.length; i++) {
			edit[i] = out[i];
			double here = 0;
			for (int j = 0; j < in.length; j++)
				here += in[j] + convolution[j % 8 - i % 8 + 7 + 8 * (j / 8 + i / 8 + 7)];
			edit[i] += here * weight;
		}
		for (int i = 0; i < 64; i++)
			out[i] = edit[i];
	}

	public static double activate(double in) {
		return softsign(in);
	}

	private static void activate(double[] in) {
		for (int i = 0; i < in.length; i++)
			// -1 to avoid activating bias
			in[i] = activate(in[i]);
	}

	public static double softsign(double x) {
		return x / (1 + abs(x));
	}

	public static double sigmoid(double x) {
		return 1 / (1 + Math.exp(x));
	}

	public static double nlln(double x) {
		if (x > 0)
			return Math.log(x + 1);
		return -Math.log(1 - x);
	}

	public static double relu(double x) {
		return x > 0 ? x : 0;
	}

	private static double abs(double a) {
		return a > 0 ? a : -a;
	}

	private static int sqrt(int x) {
		if (x == 64)
			return 8;
		if (x == 36)
			return 6;
		if (x == 16)
			return 4;
		return 2;
	}

	public BoardState copy() {
		return new BoardState(this);
	}

	public static String bitBoardToString(long x) {
		long k1 = 0x5555555555555555L, k2 = 0x3333333333333333L, k4 = 0x0f0f0f0f0f0f0f0fL;
		x = ((x >>> 1) & k1) + 2 * (x & k1);
		x = ((x >>> 2) & k2) + 4 * (x & k2);
		x = ((x >>> 4) & k4) + 16 * (x & k4);
		StringBuilder sb = new StringBuilder();
		for (int j = 7; j >= 0; j--) {
			for (int i = 0; i < 8; i++) {
				sb.append(x & 1);
				x = x >>> 1;
			}
			sb.append('\n');
		}
		return sb.toString();
	}

	public static String mailboxToString(Piece[] x) {
		StringBuilder sb = new StringBuilder();
		for (int j = 7; j >= 0; j--) {
			for (int i = 0; i < 8; i++) {
				sb.append(x[8 * j + i]);
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int j = 7; j >= 0; j--) {
			for (int i = 0; i < 8; i++) {
				Piece w = whiteMailbox[8 * j + i], b = blackMailbox[8 * j + i];
				if (w != Piece.NONE) {
					sb.append(w);
				} else {
					sb.append(b.toString().toLowerCase());
				}
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	public boolean equals(BoardState bs) {
		return whitePawns == bs.whitePawns && whiteKnights == bs.whiteKnights && whiteBishops == bs.whiteBishops && whiteRooks == bs.whiteRooks && whiteQueens == bs.whiteQueens && whiteKing == bs.whiteKing && blackPawns == bs.blackPawns && blackKnights == bs.blackKnights && blackBishops == bs.blackBishops && blackRooks == bs.blackRooks && blackQueens == bs.blackQueens && blackKing == bs.blackKing && whiteKingside == bs.whiteKingside && whiteQueenside == bs.whiteQueenside && blackKingside == bs.blackKingside && blackQueenside == bs.blackQueenside && (ep ? (bs.ep && eps == bs.eps) : !bs.ep);
	}

	public int hashCode() {
		//		return hc;
		return 0;
	}
}