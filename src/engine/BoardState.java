package engine;

import java.util.*;

public class BoardState {
    //region board variables
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
    private static final long[][] slide;
//    private static final Map<Piece, List<Integer>> wt, bt;

    // piece order
    private static final Map<Piece, Integer> str;

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
        int[] dx = {1, 2, 2, 1, -1, -2, -2, -1}, dy = {2, 1, -1, -2, -2, -1, 1, 2};
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

        slide = new long[64][64];
        for (int i = 0; i < 64; i++) {
            for (int j = i + 16; j < 64; j += 8) {
                slide[i][j] = slide[i][j - 8] + sq[j - 8];
            }
            for (int j = i + 2; j < 8 * (i / 8) + 8; j++) {
                slide[i][j] = slide[i][j - 1] + sq[j - 1];
            }
            for (int j = i + 18; j < 64 && j % 8 != 0 && j % 8 != 1; j += 9) {
                slide[i][j] = slide[i][j - 9] + sq[j - 9];
            }
            for (int j = i + 14; j < 64 && j % 8 != 7 && j % 8 != 6; j += 7) {
                slide[i][j] = slide[i][j - 7] + sq[j - 7];
            }
        }
        for (int j = 0; j < 64; j++) {
            for (int i = j; i < 64; i++) {
                slide[i][j] = slide[j][i];
            }
        }

        str = new HashMap<>();
        str.put(Piece.NONE, 0);
        str.put(Piece.PAWN, 1);
        str.put(Piece.ROOK, 2);
        str.put(Piece.BISHOP, 3);
        str.put(Piece.QUEEN, 4);
        str.put(Piece.KING, 5);
        str.put(Piece.KNIGHT, 6);

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
    //endregion

    //region constructors
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

        whiteMailbox = new Piece[] {
                Piece.ROOK, Piece.KNIGHT, Piece.BISHOP, Piece.QUEEN, Piece.KING, Piece.BISHOP, Piece.KNIGHT, Piece.ROOK,
                Piece.PAWN, Piece.PAWN, Piece.PAWN, Piece.PAWN, Piece.PAWN, Piece.PAWN, Piece.PAWN, Piece.PAWN,
                Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE,
                Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE,
                Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE,
                Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE,
                Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE,
                Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE
        };
        blackMailbox = new Piece[] {
                Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE,
                Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE,
                Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE,
                Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE,
                Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE,
                Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE, Piece.NONE,
                Piece.PAWN, Piece.PAWN, Piece.PAWN, Piece.PAWN, Piece.PAWN, Piece.PAWN, Piece.PAWN, Piece.PAWN,
                Piece.ROOK, Piece.KNIGHT, Piece.BISHOP, Piece.QUEEN, Piece.KING, Piece.BISHOP, Piece.KNIGHT, Piece.ROOK
        };

        whiteKingside = true;
        whiteQueenside = true;
        blackKingside = true;
        blackQueenside = true;
        prevWhiteKingside = new ArrayDeque<>();
        prevWhiteQueenside = new ArrayDeque<>();
        prevBlackKingside = new ArrayDeque<>();
        prevBlackQueenside = new ArrayDeque<>();

        ep = false;

        prevEp = new ArrayDeque<>();
        prevEps = new ArrayDeque<>();

        hasWhiteKing = true;
        hasBlackKing = true;

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
                }
                else if ('a' <= c && c <= 'z') {
                    blackBoard += sq[cur];
                    board += sq[cur];
                }
                switch (c) {
                    case 'P':
                        whitePawns = whitePawns + sq[cur];
                        whiteMailbox[cur] = Piece.PAWN;
                        cur++;
                        break;
                    case 'N':
                        whiteKnights = whiteKnights + sq[cur];
                        whiteMailbox[cur] = Piece.KNIGHT;
                        cur++;
                        break;
                    case 'B':
                        whiteBishops = whiteBishops + sq[cur];
                        whiteMailbox[cur] = Piece.BISHOP;
                        cur++;
                        break;
                    case 'R':
                        whiteRooks = whiteRooks + sq[cur];
                        whiteMailbox[cur] = Piece.ROOK;
                        cur++;
                        break;
                    case 'Q':
                        whiteQueens = whiteQueens + sq[cur];
                        whiteMailbox[cur] = Piece.QUEEN;
                        cur++;
                        break;
                    case 'K':
                        whiteKing = whiteKing + sq[cur];
                        whiteMailbox[cur] = Piece.KING;
                        hasWhiteKing = true;
                        cur++;
                        break;
                    case 'p':
                        blackPawns = blackPawns + sq[cur];
                        blackMailbox[cur] = Piece.PAWN;
                        cur++;
                        break;
                    case 'n':
                        blackKnights = blackKnights + sq[cur];
                        blackMailbox[cur] = Piece.KNIGHT;
                        cur++;
                        break;
                    case 'b':
                        blackBishops = blackBishops + sq[cur];
                        blackMailbox[cur] = Piece.BISHOP;
                        cur++;
                        break;
                    case 'r':
                        blackRooks = blackRooks + sq[cur];
                        blackMailbox[cur] = Piece.ROOK;
                        cur++;
                        break;
                    case 'q':
                        blackQueens = blackQueens + sq[cur];
                        blackMailbox[cur] = Piece.QUEEN;
                        cur++;
                        break;
                    case 'k':
                        blackKing = blackKing + sq[cur];
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
        }
        else {
            eps = 0;
        }
        prevEp = new ArrayDeque<>();
        prevEps = new ArrayDeque<>();
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

        hasWhiteKing = bs.hasWhiteKing;
        hasBlackKing = bs.hasBlackKing;

//		hc = bs.hc;
    }
    //endregion

    //region getter methods
    public long getWhitePawns() {
        return whitePawns;
    }

    public long getWhiteKnights() {
        return whiteKnights;
    }

    public long getWhiteBishops() {
        return whiteBishops;
    }

    public long getWhiteRooks() {
        return whiteRooks;
    }

    public long getWhiteQueens() {
        return whiteQueens;
    }

    public long getWhiteKing() {
        return whiteKing;
    }

    public long getBlackPawns() {
        return blackPawns;
    }

    public long getBlackKnights() {
        return blackKnights;
    }

    public long getBlackBishops() {
        return blackBishops;
    }

    public long getBlackRooks() {
        return blackRooks;
    }

    public long getBlackQueens() {
        return blackQueens;
    }

    public long getBlackKing() {
        return blackKing;
    }
    //endregion

    //region move generation methods
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

    // identifies if white minimal move is invalid, blocked, dp push, etc.
    // defines all instance variables of move
    // assumes move is already valid (move would be valid on board without enemy pieces)
    // warning: will edit move
    public Move identifyWhiteMove(Move move) {
        move.p = whiteMailbox[move.from];

        switch (move.p) {
            case PAWN:
                // attempted capture
                if (move.to - move.from == 7 || move.to - move.from == 9) {
                    // successful no en passant capture
                    if (blackMailbox[move.to] != Piece.NONE) {
                        move.q = blackMailbox[move.to];
                        move.isCapture = true;
                        // promotion
                        if (move.to >= 56) {
                            move.isPromotion = true;
                        }
                    }
                    // en passant capture
                    else if (ep && move.to == eps) {
                        move.q = Piece.PAWN;
                        move.isCapture = true;
                        move.isEPCapture = true;
                    }
                    // invalid capture
                    else {
                        move.isInvalidMove = true;
                    }
                }
                // attempted single push
                else if (move.to - move.from == 8) {
                    // invalid single push
                    if (blackMailbox[move.to] != Piece.NONE) {
                        move.isInvalidMove = true;
                    }
                    // successful single push
                    else {
                        // promotion
                        if (move.to >= 56) {
                            move.isPromotion = true;
                        }
                    }
                }
                // attempted double push
                else {
                    // invalid double push
                    if (blackMailbox[move.from + 8] != Piece.NONE) {
                        move.isInvalidMove = true;
                    }
                    // blocked double push
                    else if (blackMailbox[move.to] != Piece.NONE) {
                        move.block = move.from + 8;
                        move.isBlockedMove = true;
                    }
                    // successful double push
                    else {
                        move.isDPMove = true;
                    }
                }
                break;
            case KNIGHT:
                // capture
                if (blackMailbox[move.to] != Piece.NONE) {
                    move.q = blackMailbox[move.to];
                    move.isCapture = true;
                }
                // quiet
                break;
            case ROOK:
            case BISHOP:
            case QUEEN:
                long slideMap = slide[move.from][move.to];
                // successful move
                if ((slideMap & blackBoard) == 0) {
                    // capture
                    if (blackMailbox[move.to] != Piece.NONE) {
                        move.q = blackMailbox[move.to];
                        move.isCapture = true;
                    }
                    // quiet
                }
                // blocked move
                else {
                    long blockMap = slideMap & blackBoard;
                    // north, northeast, east, northwest: least significant bit
                    if (move.to > move.from) {
                        move.block = convert.get(Long.highestOneBit(blockMap));
                    }
                    // southeast, south, southwest, west: most significant bit
                    else {
                        move.block = convert.get(Long.lowestOneBit(blockMap));
                    }
                    move.q = blackMailbox[move.block];
                    move.isCapture = true;
                    move.isBlockedMove = true;
                }
                break;
            case KING:
                // attempted kingside castle
                if (move.to - move.from == 2) {
                    // successful kingside castle
                    if (blackMailbox[5] == Piece.NONE && blackMailbox[6] == Piece.NONE) {
                        move.isKingsideCastle = true;
                    }
                    // invalid kingside castle
                    else {
                        move.isInvalidMove = true;
                    }
                }
                // attempted queenside castle
                else if (move.from - move.to == 2) {
                    // successful queenside castle
                    if (blackMailbox[1] == Piece.NONE && blackMailbox[2] == Piece.NONE && blackMailbox[3] == Piece.NONE) {
                        move.isQueensideCastle = true;
                    }
                    // invalid queenside castle
                    else {
                        move.isInvalidMove = true;
                    }
                }
                // capture
                else if (blackMailbox[move.to] != Piece.NONE) {
                    move.q = blackMailbox[move.to];
                    move.isCapture = true;
                }
                // quiet
                break;
        }

        return move;
    }

    public Move identifyBlackMove(Move move) {
        move.p = blackMailbox[move.from];

        switch (move.p) {
            case PAWN:
                // attempted capture
                if (move.from - move.to == 7 || move.from - move.to == 9) {
                    // successful no en passant capture
                    if (whiteMailbox[move.to] != Piece.NONE) {
                        move.q = whiteMailbox[move.to];
                        move.isCapture = true;
                        // promotion
                        if (move.to <= 7) {
                            move.isPromotion = true;
                        }
                    }
                    // en passant capture
                    else if (ep && move.to == eps) {
                        move.q = Piece.PAWN;
                        move.isCapture = true;
                        move.isEPCapture = true;
                    }
                    // invalid capture
                    else {
                        move.isInvalidMove = true;
                    }
                }
                // attempted single push
                else if (move.from - move.to == 8) {
                    // invalid single push
                    if (whiteMailbox[move.to] != Piece.NONE) {
                        move.isInvalidMove = true;
                    }
                    // successful single push
                    else {
                        // promotion
                        if (move.to <= 7) {
                            move.isPromotion = true;
                        }
                    }
                }
                // attempted double push
                else {
                    // invalid double push
                    if (whiteMailbox[move.from - 8] != Piece.NONE) {
                        move.isInvalidMove = true;
                    }
                    // blocked double push
                    else if (whiteMailbox[move.to] != Piece.NONE) {
                        move.block = move.from - 8;
                        move.isBlockedMove = true;
                    }
                    // successful double push
                    else {
                        move.isDPMove = true;
                    }
                }
                break;
            case KNIGHT:
                // capture
                if (whiteMailbox[move.to] != Piece.NONE) {
                    move.q = whiteMailbox[move.to];
                    move.isCapture = true;
                }
                // quiet
                break;
            case ROOK:
            case BISHOP:
            case QUEEN:
                long slideMap = slide[move.from][move.to];
                // successful move
                if ((slideMap & whiteBoard) == 0) {
                    // capture
                    if (whiteMailbox[move.to] != Piece.NONE) {
                        move.q = whiteMailbox[move.to];
                        move.isCapture = true;
                    }
                    // quiet
                }
                // blocked move
                else {
                    long blockMap = slideMap & whiteBoard;
                    // north, northeast, east, northwest: least significant bit
                    if (move.to > move.from) {
                        move.block = convert.get(Long.highestOneBit(blockMap));
                    }
                    // southeast, south, southwest, west: most significant bit
                    else {
                        move.block = convert.get(Long.lowestOneBit(blockMap));
                    }
                    move.q = whiteMailbox[move.block];
                    move.isCapture = true;
                    move.isBlockedMove = true;
                }
                break;
            case KING:
                // attempted kingside castle
                if (move.to - move.from == 2) {
                    // successful kingside castle
                    if (whiteMailbox[61] == Piece.NONE && whiteMailbox[62] == Piece.NONE) {
                        move.isKingsideCastle = true;
                    }
                    // invalid kingside castle
                    else {
                        move.isInvalidMove = true;
                    }
                }
                // attempted queenside castle
                else if (move.from - move.to == 2) {
                    // successful queenside castle
                    if (whiteMailbox[57] == Piece.NONE && whiteMailbox[58] == Piece.NONE && whiteMailbox[59] == Piece.NONE) {
                        move.isQueensideCastle = true;
                    }
                    // invalid queenside castle
                    else {
                        move.isInvalidMove = true;
                    }
                }
                // capture
                else if (whiteMailbox[move.to] != Piece.NONE) {
                    move.q = whiteMailbox[move.to];
                    move.isCapture = true;
                }
                // quiet
                break;
        }

        return move;
    }

    public List<Move> whiteMoves() {
        List<Move> moves = new ArrayList<>();
        if (!hasWhiteKing || !hasBlackKing) {
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
                        }
                        else {
                            possibleInvalidMove = true;
                        }
                    }
                    if (whiteQueenside && whiteMailbox[3] == Piece.NONE && whiteMailbox[2] == Piece.NONE && whiteMailbox[1] == Piece.NONE) {
                        if (blackMailbox[3] == Piece.NONE && blackMailbox[2] == Piece.NONE && blackMailbox[1] == Piece.NONE) {
                            Move move = new Move(Piece.KING, 4, 2);
                            move.isQueensideCastle = true;
                            moves.add(move);
                        }
                        else {
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
            moves.add(new Move(Piece.PAWN, Piece.QUEEN,to - 7, to, blackMailbox[to]));
            moves.add(new Move(Piece.PAWN, Piece.KNIGHT,to - 7, to, blackMailbox[to]));
            moves.add(new Move(Piece.PAWN, Piece.ROOK,to - 7, to, blackMailbox[to]));
            moves.add(new Move(Piece.PAWN, Piece.BISHOP,to - 7, to, blackMailbox[to]));
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
            moves.add(new Move(Piece.PAWN, Piece.QUEEN,to - 9, to, blackMailbox[to]));
            moves.add(new Move(Piece.PAWN, Piece.KNIGHT,to - 9, to, blackMailbox[to]));
            moves.add(new Move(Piece.PAWN, Piece.ROOK,to - 9, to, blackMailbox[to]));
            moves.add(new Move(Piece.PAWN, Piece.BISHOP,to - 9, to, blackMailbox[to]));
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
        // single push invalid
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
        if (!hasWhiteKing || !hasBlackKing) {
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
                        }
                        else {
                            possibleInvalidMove = true;
                        }
                    }
                    if (blackQueenside && blackMailbox[59] == Piece.NONE && blackMailbox[58] == Piece.NONE && blackMailbox[57] == Piece.NONE) {
                        if (whiteMailbox[59] == Piece.NONE && whiteMailbox[58] == Piece.NONE && whiteMailbox[57] == Piece.NONE) {
                            Move move = new Move(Piece.KING, 60, 58);
                            move.isQueensideCastle = true;
                            moves.add(move);
                        }
                        else {
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
            moves.add(new Move(Piece.PAWN, Piece.QUEEN,to + 9, to, whiteMailbox[to]));
            moves.add(new Move(Piece.PAWN, Piece.KNIGHT,to + 9, to, whiteMailbox[to]));
            moves.add(new Move(Piece.PAWN, Piece.ROOK,to + 9, to, whiteMailbox[to]));
            moves.add(new Move(Piece.PAWN, Piece.BISHOP,to + 9, to, whiteMailbox[to]));
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
            moves.add(new Move(Piece.PAWN, Piece.QUEEN,to + 7, to, whiteMailbox[to]));
            moves.add(new Move(Piece.PAWN, Piece.KNIGHT,to + 7, to, whiteMailbox[to]));
            moves.add(new Move(Piece.PAWN, Piece.ROOK,to + 7, to, whiteMailbox[to]));
            moves.add(new Move(Piece.PAWN, Piece.BISHOP,to + 7, to, whiteMailbox[to]));
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
        // single push invalid
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
        if (!hasWhiteKing || !hasBlackKing) {
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
                        moves.add(new Move(Piece.ROOK, from, cap, blackMailbox[cap]));
                        // blocked moves
                        long rookNorthPseudoMap = pseudoWhiteSlideAttacks(from, northMask[from]) & ~whiteBoard;
                        for (long rb = rookNorthPseudoMap ^ rookNorthMap; rb != 0; rb &= rb - 1) {
                            int to = convert.get(rb & -rb);
                            Move move = new Move(Piece.ROOK, from, to, cap, blackMailbox[cap]);
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
                        moves.add(new Move(Piece.ROOK, from, cap, blackMailbox[cap]));
                        // blocked moves
                        long rookSouthPseudoMap = pseudoWhiteSlideAttacks(from, southMask[from]) & ~whiteBoard;
                        for (long rb = rookSouthPseudoMap ^ rookSouthMap; rb != 0; rb &= rb - 1) {
                            int to = convert.get(rb & -rb);
                            Move move = new Move(Piece.ROOK, from, to, cap, blackMailbox[cap]);
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
                        moves.add(new Move(Piece.ROOK, from, cap, blackMailbox[cap]));
                        // blocked moves
                        long rookEastPseudoMap = pseudoWhiteSlideAttacks(from, eastMask[from]) & ~whiteBoard;
                        for (long rb = rookEastPseudoMap ^ rookEastMap; rb != 0; rb &= rb - 1) {
                            int to = convert.get(rb & -rb);
                            Move move = new Move(Piece.ROOK, from, to, cap, blackMailbox[cap]);
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
                        moves.add(new Move(Piece.ROOK, from, cap, blackMailbox[cap]));
                        // blocked moves
                        long rookWestPseudoMap = pseudoWhiteSlideAttacks(from, westMask[from]) & ~whiteBoard;
                        for (long rb = rookWestPseudoMap ^ rookWestMap; rb != 0; rb &= rb - 1) {
                            int to = convert.get(rb & -rb);
                            Move move = new Move(Piece.ROOK, from, to, cap, blackMailbox[cap]);
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
                        moves.add(new Move(Piece.BISHOP, from, cap, blackMailbox[cap]));
                        // blocked moves
                        long bishopNorthEastPseudoMap = pseudoWhiteSlideAttacks(from, northEastMask[from]) & ~whiteBoard;
                        for (long bb = bishopNorthEastPseudoMap ^ bishopNorthEastMap; bb != 0; bb &= bb - 1) {
                            int to = convert.get(bb & -bb);
                            Move move = new Move(Piece.BISHOP, from, to, cap, blackMailbox[cap]);
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
                        moves.add(new Move(Piece.BISHOP, from, cap, blackMailbox[cap]));
                        // blocked moves
                        long bishopSouthEastPseudoMap = pseudoWhiteSlideAttacks(from, southEastMask[from]) & ~whiteBoard;
                        for (long bb = bishopSouthEastPseudoMap ^ bishopSouthEastMap; bb != 0; bb &= bb - 1) {
                            int to = convert.get(bb & -bb);
                            Move move = new Move(Piece.BISHOP, from, to, cap, blackMailbox[cap]);
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
                        moves.add(new Move(Piece.BISHOP, from, cap, blackMailbox[cap]));
                        // blocked moves
                        long bishopSouthWestPseudoMap = pseudoWhiteSlideAttacks(from, southWestMask[from]) & ~whiteBoard;
                        for (long bb = bishopSouthWestPseudoMap ^ bishopSouthWestMap; bb != 0; bb &= bb - 1) {
                            int to = convert.get(bb & -bb);
                            Move move = new Move(Piece.BISHOP, from, to, cap, blackMailbox[cap]);
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
                        moves.add(new Move(Piece.BISHOP, from, cap, blackMailbox[cap]));
                        // blocked moves
                        long bishopNorthWestPseudoMap = pseudoWhiteSlideAttacks(from, northWestMask[from]) & ~whiteBoard;
                        for (long bb = bishopNorthWestPseudoMap ^ bishopNorthWestMap; bb != 0; bb &= bb - 1) {
                            int to = convert.get(bb & -bb);
                            Move move = new Move(Piece.BISHOP, from, to, cap, blackMailbox[cap]);
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
                        moves.add(new Move(Piece.QUEEN, from, cap, blackMailbox[cap]));
                        // blocked moves
                        long queenNorthPseudoMap = pseudoWhiteSlideAttacks(from, northMask[from]) & ~whiteBoard;
                        for (long qb = queenNorthPseudoMap ^ queenNorthMap; qb != 0; qb &= qb - 1) {
                            int to = convert.get(qb & -qb);
                            Move move = new Move(Piece.QUEEN, from, to, cap, blackMailbox[cap]);
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
                        moves.add(new Move(Piece.QUEEN, from, cap, blackMailbox[cap]));
                        // blocked moves
                        long queenSouthPseudoMap = pseudoWhiteSlideAttacks(from, southMask[from]) & ~whiteBoard;
                        for (long qb = queenSouthPseudoMap ^ queenSouthMap; qb != 0; qb &= qb - 1) {
                            int to = convert.get(qb & -qb);
                            Move move = new Move(Piece.QUEEN, from, to, cap, blackMailbox[cap]);
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
                        moves.add(new Move(Piece.QUEEN, from, cap, blackMailbox[cap]));
                        // blocked moves
                        long queenEastPseudoMap = pseudoWhiteSlideAttacks(from, eastMask[from]) & ~whiteBoard;
                        for (long qb = queenEastPseudoMap ^ queenEastMap; qb != 0; qb &= qb - 1) {
                            int to = convert.get(qb & -qb);
                            Move move = new Move(Piece.QUEEN, from, to, cap, blackMailbox[cap]);
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
                        moves.add(new Move(Piece.QUEEN, from, cap, blackMailbox[cap]));
                        // blocked moves
                        long queenWestPseudoMap = pseudoWhiteSlideAttacks(from, westMask[from]) & ~whiteBoard;
                        for (long qb = queenWestPseudoMap ^ queenWestMap; qb != 0; qb &= qb - 1) {
                            int to = convert.get(qb & -qb);
                            Move move = new Move(Piece.QUEEN, from, to, cap, blackMailbox[cap]);
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
                        moves.add(new Move(Piece.QUEEN, from, cap, blackMailbox[cap]));
                        // blocked moves
                        long queenNorthEastPseudoMap = pseudoWhiteSlideAttacks(from, northEastMask[from]) & ~whiteBoard;
                        for (long qb = queenNorthEastPseudoMap ^ queenNorthEastMap; qb != 0; qb &= qb - 1) {
                            int to = convert.get(qb & -qb);
                            Move move = new Move(Piece.QUEEN, from, to, cap, blackMailbox[cap]);
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
                        moves.add(new Move(Piece.QUEEN, from, cap, blackMailbox[cap]));
                        // blocked moves
                        long queenSouthEastPseudoMap = pseudoWhiteSlideAttacks(from, southEastMask[from]) & ~whiteBoard;
                        for (long qb = queenSouthEastPseudoMap ^ queenSouthEastMap; qb != 0; qb &= qb - 1) {
                            int to = convert.get(qb & -qb);
                            Move move = new Move(Piece.QUEEN, from, to, cap, blackMailbox[cap]);
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
                        moves.add(new Move(Piece.QUEEN, from, cap, blackMailbox[cap]));
                        // blocked moves
                        long queenSouthWestPseudoMap = pseudoWhiteSlideAttacks(from, southWestMask[from]) & ~whiteBoard;
                        for (long qb = queenSouthWestPseudoMap ^ queenSouthWestMap; qb != 0; qb &= qb - 1) {
                            int to = convert.get(qb & -qb);
                            Move move = new Move(Piece.QUEEN, from, to, cap, blackMailbox[cap]);
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
                        moves.add(new Move(Piece.QUEEN, from, cap, blackMailbox[cap]));
                        // blocked moves
                        long queenNorthWestPseudoMap = pseudoWhiteSlideAttacks(from, northWestMask[from]) & ~whiteBoard;
                        for (long qb = queenNorthWestPseudoMap ^ queenNorthWestMap; qb != 0; qb &= qb - 1) {
                            int to = convert.get(qb & -qb);
                            Move move = new Move(Piece.QUEEN, from, to, cap, blackMailbox[cap]);
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
                        }
                        else {
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
                        }
                        else {
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
            moves.add(new Move(Piece.PAWN, Piece.QUEEN,to - 7, to, blackMailbox[to]));
            moves.add(new Move(Piece.PAWN, Piece.KNIGHT,to - 7, to, blackMailbox[to]));
            moves.add(new Move(Piece.PAWN, Piece.ROOK,to - 7, to, blackMailbox[to]));
            moves.add(new Move(Piece.PAWN, Piece.BISHOP,to - 7, to, blackMailbox[to]));
        }
        // invalid left captures, promotion
        for (long lb = b & ~board; lb != 0; lb &= lb - 1) {
            int to = convert.get(lb & -lb);
            Move move = new Move(Piece.PAWN, Piece.QUEEN,to - 7, to);
            move.isInvalidMove = true;
            moves.add(move);
            move = new Move(Piece.PAWN, Piece.KNIGHT,to - 7, to);
            move.isInvalidMove = true;
            moves.add(move);
            move = new Move(Piece.PAWN, Piece.ROOK,to - 7, to);
            move.isInvalidMove = true;
            moves.add(move);
            move = new Move(Piece.PAWN, Piece.BISHOP,to - 7, to);
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
            moves.add(new Move(Piece.PAWN, Piece.QUEEN,to - 9, to, blackMailbox[to]));
            moves.add(new Move(Piece.PAWN, Piece.KNIGHT,to - 9, to, blackMailbox[to]));
            moves.add(new Move(Piece.PAWN, Piece.ROOK,to - 9, to, blackMailbox[to]));
            moves.add(new Move(Piece.PAWN, Piece.BISHOP,to - 9, to, blackMailbox[to]));
        }
        // invalid right captures, promotion
        for (long lb = b & ~board; lb != 0; lb &= lb - 1) {
            int to = convert.get(lb & -lb);
            Move move = new Move(Piece.PAWN, Piece.QUEEN,to - 9, to);
            move.isInvalidMove = true;
            moves.add(move);
            move = new Move(Piece.PAWN, Piece.KNIGHT,to - 9, to);
            move.isInvalidMove = true;
            moves.add(move);
            move = new Move(Piece.PAWN, Piece.ROOK,to - 9, to);
            move.isInvalidMove = true;
            moves.add(move);
            move = new Move(Piece.PAWN, Piece.BISHOP,to - 9, to);
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
        // single push invalid
        for (long sb = singleMap & blackBoard; sb != 0; sb &= sb - 1) {
            int to = convert.get(sb & -sb);
            Move move = new Move(Piece.PAWN, to - 8, to);
            move.isInvalidMove = true;
            moves.add(move);
        }
        // double push
        long doubleMap = (whitePawns >>> 16) & 0b0000000000000000000000001111111100000000000000000000000000000000L;
        for (long db = doubleMap & ~board & ((singleMap & ~board) >>> 8); db != 0; db &= db - 1) {
            int to = convert.get(db & -db);
            Move move = new Move(Piece.PAWN, to - 16, to);
            move.isDPMove = true;
            moves.add(move);
        }
        // double push blocked
        for (long db = doubleMap & blackBoard & ((singleMap & ~board) >>> 8); db != 0; db &= db - 1) {
            int to = convert.get(db & -db);
            Move move = new Move(Piece.PAWN, to - 16, to, to - 8);
            moves.add(move);
        }
        // double push invalid
        for (long db = doubleMap & ~whiteBoard & ((singleMap & blackBoard) >>> 8); db != 0; db &= db - 1) {
            int to = convert.get(db & -db);
            Move move = new Move(Piece.PAWN, to - 16, to);
            move.isInvalidMove = true;
            moves.add(move);
        }
        return moves;
    }

    public List<Move> blackPseudoMoves() {
        List<Move> moves = new ArrayList<>();
        if (!hasWhiteKing || !hasBlackKing) {
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
                        moves.add(new Move(Piece.ROOK, from, cap, whiteMailbox[cap]));
                        // blocked moves
                        long rookNorthPseudoMap = pseudoBlackSlideAttacks(from, northMask[from]) & ~blackBoard;
                        for (long rb = rookNorthPseudoMap ^ rookNorthMap; rb != 0; rb &= rb - 1) {
                            int to = convert.get(rb & -rb);
                            Move move = new Move(Piece.ROOK, from, to, cap, whiteMailbox[cap]);
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
                        moves.add(new Move(Piece.ROOK, from, cap, whiteMailbox[cap]));
                        // blocked moves
                        long rookSouthPseudoMap = pseudoBlackSlideAttacks(from, southMask[from]) & ~blackBoard;
                        for (long rb = rookSouthPseudoMap ^ rookSouthMap; rb != 0; rb &= rb - 1) {
                            int to = convert.get(rb & -rb);
                            Move move = new Move(Piece.ROOK, from, to, cap, whiteMailbox[cap]);
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
                        moves.add(new Move(Piece.ROOK, from, cap, whiteMailbox[cap]));
                        // blocked moves
                        long rookEastPseudoMap = pseudoBlackSlideAttacks(from, eastMask[from]) & ~blackBoard;
                        for (long rb = rookEastPseudoMap ^ rookEastMap; rb != 0; rb &= rb - 1) {
                            int to = convert.get(rb & -rb);
                            Move move = new Move(Piece.ROOK, from, to, cap, whiteMailbox[cap]);
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
                        moves.add(new Move(Piece.ROOK, from, cap, whiteMailbox[cap]));
                        // blocked moves
                        long rookWestPseudoMap = pseudoBlackSlideAttacks(from, westMask[from]) & ~blackBoard;
                        for (long rb = rookWestPseudoMap ^ rookWestMap; rb != 0; rb &= rb - 1) {
                            int to = convert.get(rb & -rb);
                            Move move = new Move(Piece.ROOK, from, to, cap, whiteMailbox[cap]);
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
                        moves.add(new Move(Piece.BISHOP, from, cap, whiteMailbox[cap]));
                        // blocked moves
                        long bishopNorthEastPseudoMap = pseudoBlackSlideAttacks(from, northEastMask[from]) & ~blackBoard;
                        for (long bb = bishopNorthEastPseudoMap ^ bishopNorthEastMap; bb != 0; bb &= bb - 1) {
                            int to = convert.get(bb & -bb);
                            Move move = new Move(Piece.BISHOP, from, to, cap, whiteMailbox[cap]);
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
                        moves.add(new Move(Piece.BISHOP, from, cap, whiteMailbox[cap]));
                        // blocked moves
                        long bishopSouthEastPseudoMap = pseudoBlackSlideAttacks(from, southEastMask[from]) & ~blackBoard;
                        for (long bb = bishopSouthEastPseudoMap ^ bishopSouthEastMap; bb != 0; bb &= bb - 1) {
                            int to = convert.get(bb & -bb);
                            Move move = new Move(Piece.BISHOP, from, to, cap, whiteMailbox[cap]);
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
                        moves.add(new Move(Piece.BISHOP, from, cap, whiteMailbox[cap]));
                        // blocked moves
                        long bishopSouthWestPseudoMap = pseudoBlackSlideAttacks(from, southWestMask[from]) & ~blackBoard;
                        for (long bb = bishopSouthWestPseudoMap ^ bishopSouthWestMap; bb != 0; bb &= bb - 1) {
                            int to = convert.get(bb & -bb);
                            Move move = new Move(Piece.BISHOP, from, to, cap, whiteMailbox[cap]);
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
                        moves.add(new Move(Piece.BISHOP, from, cap, whiteMailbox[cap]));
                        // blocked moves
                        long bishopNorthWestPseudoMap = pseudoBlackSlideAttacks(from, northWestMask[from]) & ~blackBoard;
                        for (long bb = bishopNorthWestPseudoMap ^ bishopNorthWestMap; bb != 0; bb &= bb - 1) {
                            int to = convert.get(bb & -bb);
                            Move move = new Move(Piece.BISHOP, from, to, cap, whiteMailbox[cap]);
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
                        moves.add(new Move(Piece.QUEEN, from, cap, whiteMailbox[cap]));
                        // blocked moves
                        long queenNorthPseudoMap = pseudoBlackSlideAttacks(from, northMask[from]) & ~blackBoard;
                        for (long qb = queenNorthPseudoMap ^ queenNorthMap; qb != 0; qb &= qb - 1) {
                            int to = convert.get(qb & -qb);
                            Move move = new Move(Piece.QUEEN, from, to, cap, whiteMailbox[cap]);
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
                        moves.add(new Move(Piece.QUEEN, from, cap, whiteMailbox[cap]));
                        // blocked moves
                        long queenSouthPseudoMap = pseudoBlackSlideAttacks(from, southMask[from]) & ~blackBoard;
                        for (long qb = queenSouthPseudoMap ^ queenSouthMap; qb != 0; qb &= qb - 1) {
                            int to = convert.get(qb & -qb);
                            Move move = new Move(Piece.QUEEN, from, to, cap, whiteMailbox[cap]);
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
                        moves.add(new Move(Piece.QUEEN, from, cap, whiteMailbox[cap]));
                        // blocked moves
                        long queenEastPseudoMap = pseudoBlackSlideAttacks(from, eastMask[from]) & ~blackBoard;
                        for (long qb = queenEastPseudoMap ^ queenEastMap; qb != 0; qb &= qb - 1) {
                            int to = convert.get(qb & -qb);
                            Move move = new Move(Piece.QUEEN, from, to, cap, whiteMailbox[cap]);
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
                        moves.add(new Move(Piece.QUEEN, from, cap, whiteMailbox[cap]));
                        // blocked moves
                        long queenWestPseudoMap = pseudoBlackSlideAttacks(from, westMask[from]) & ~blackBoard;
                        for (long qb = queenWestPseudoMap ^ queenWestMap; qb != 0; qb &= qb - 1) {
                            int to = convert.get(qb & -qb);
                            Move move = new Move(Piece.QUEEN, from, to, cap, whiteMailbox[cap]);
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
                        moves.add(new Move(Piece.QUEEN, from, cap, whiteMailbox[cap]));
                        // blocked moves
                        long queenNorthEastPseudoMap = pseudoBlackSlideAttacks(from, northEastMask[from]) & ~blackBoard;
                        for (long qb = queenNorthEastPseudoMap ^ queenNorthEastMap; qb != 0; qb &= qb - 1) {
                            int to = convert.get(qb & -qb);
                            Move move = new Move(Piece.QUEEN, from, to, cap, whiteMailbox[cap]);
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
                        moves.add(new Move(Piece.QUEEN, from, cap, whiteMailbox[cap]));
                        // blocked moves
                        long queenSouthEastPseudoMap = pseudoBlackSlideAttacks(from, southEastMask[from]) & ~blackBoard;
                        for (long qb = queenSouthEastPseudoMap ^ queenSouthEastMap; qb != 0; qb &= qb - 1) {
                            int to = convert.get(qb & -qb);
                            Move move = new Move(Piece.QUEEN, from, to, cap, whiteMailbox[cap]);
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
                        moves.add(new Move(Piece.QUEEN, from, cap, whiteMailbox[cap]));
                        // blocked moves
                        long queenSouthWestPseudoMap = pseudoBlackSlideAttacks(from, southWestMask[from]) & ~blackBoard;
                        for (long qb = queenSouthWestPseudoMap ^ queenSouthWestMap; qb != 0; qb &= qb - 1) {
                            int to = convert.get(qb & -qb);
                            Move move = new Move(Piece.QUEEN, from, to, cap, whiteMailbox[cap]);
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
                        moves.add(new Move(Piece.QUEEN, from, cap, whiteMailbox[cap]));
                        // blocked moves
                        long queenNorthWestPseudoMap = pseudoBlackSlideAttacks(from, northWestMask[from]) & ~blackBoard;
                        for (long qb = queenNorthWestPseudoMap ^ queenNorthWestMap; qb != 0; qb &= qb - 1) {
                            int to = convert.get(qb & -qb);
                            Move move = new Move(Piece.QUEEN, from, to, cap, whiteMailbox[cap]);
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
                        }
                        else {
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
                        }
                        else {
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
        // single push invalid
        for (long sb = singleMap & whiteBoard; sb != 0; sb &= sb - 1) {
            int to = convert.get(sb & -sb);
            Move move = new Move(Piece.PAWN, to + 8, to);
            move.isInvalidMove = true;
            moves.add(move);
        }
        // double push
        long doubleMap = (blackPawns << 16) & 0b0000000000000000000000000000000011111111000000000000000000000000L;
        for (long db = doubleMap & ~board & ((singleMap & ~board) << 8); db != 0; db &= db - 1) {
            int to = convert.get(db & -db);
            Move move = new Move(Piece.PAWN, to + 16, to);
            move.isDPMove = true;
            moves.add(move);
        }
        // double push blocked
        for (long db = doubleMap & whiteBoard & ((singleMap & ~board) << 8); db != 0; db &= db - 1) {
            int to = convert.get(db & -db);
            Move move = new Move(Piece.PAWN, to + 16, to, to + 8);
            moves.add(move);
        }
        // double push invalid
        for (long db = doubleMap & ~blackBoard & ((singleMap & whiteBoard) << 8); db != 0; db &= db - 1) {
            int to = convert.get(db & -db);
            Move move = new Move(Piece.PAWN, to + 16, to);
            move.isInvalidMove = true;
            moves.add(move);
        }
        return moves;
    }

    // TODO: create quite move generator and square attacked by move generator
    public List<Move> whiteCaptureMoves() {
        List<Move> moves = new ArrayList<>();
        if (!hasWhiteKing || !hasBlackKing) {
            return moves;
        }
        for (long b = whiteBoard; b != 0; b &= b - 1) {
            int from = convert.get(b & -b);
            switch (whiteMailbox[from]) {
                case KNIGHT:
                    long knightMap = knightMoves[from] & ~whiteBoard;
                    // captures
                    for (long nb = knightMap & blackBoard; nb != 0; nb &= nb - 1) {
                        int to = convert.get(nb & -nb);
                        moves.add(new Move(Piece.KNIGHT, from, to, blackMailbox[to]));
                    }
                    break;
                case ROOK:
                    long rookMap = (slideAttacks(from, rankMask[from]) ^ slideAttacks(from, fileMask[from])) & ~whiteBoard;
                    // captures
                    for (long rb = rookMap & blackBoard; rb != 0; rb &= rb - 1) {
                        int to = convert.get(rb & -rb);
                        moves.add(new Move(Piece.ROOK, from, to, blackMailbox[to]));
                    }
                    break;
                case BISHOP:
                    long bishopMap = (slideAttacks(from, diagonalMask[from]) ^ slideAttacks(from, antiDiagonalMask[from])) & ~whiteBoard;
                    // captures
                    for (long bb = bishopMap & blackBoard; bb != 0; bb &= bb - 1) {
                        int to = convert.get(bb & -bb);
                        moves.add(new Move(Piece.BISHOP, from, to, blackMailbox[to]));
                    }
                    break;
                case QUEEN:
                    long queenMap = (slideAttacks(from, rankMask[from]) ^ slideAttacks(from, fileMask[from]) ^ slideAttacks(from, diagonalMask[from]) ^ slideAttacks(from, antiDiagonalMask[from])) & ~whiteBoard;
                    // captures
                    for (long qb = queenMap & blackBoard; qb != 0; qb &= qb - 1) {
                        int to = convert.get(qb & -qb);
                        moves.add(new Move(Piece.QUEEN, from, to, blackMailbox[to]));
                    }
                    break;
                case KING:
                    long kingMap = kingMoves[from] & ~whiteBoard;
                    // captures
                    for (long kb = kingMap & blackBoard; kb != 0; kb &= kb - 1) {
                        int to = convert.get(kb & -kb);
                        moves.add(new Move(Piece.KING, from, to, blackMailbox[to]));
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
        return moves;
    }

    public List<Move> blackCaptureMoves() {
        List<Move> moves = new ArrayList<>();
        if (!hasWhiteKing || !hasBlackKing) {
            return moves;
        }
        for (long b = blackBoard; b != 0; b &= b - 1) {
            int from = convert.get(b & -b);
            switch (blackMailbox[from]) {
                case KNIGHT:
                    long knightMap = knightMoves[from] & ~blackBoard;
                    // captures
                    for (long nb = knightMap & whiteBoard; nb != 0; nb &= nb - 1) {
                        int to = convert.get(nb & -nb);
                        moves.add(new Move(Piece.KNIGHT, from, to, whiteMailbox[to]));
                    }
                    break;
                case ROOK:
                    long rookMap = (slideAttacks(from, rankMask[from]) ^ slideAttacks(from, fileMask[from])) & ~blackBoard;
                    // captures
                    for (long rb = rookMap & whiteBoard; rb != 0; rb &= rb - 1) {
                        int to = convert.get(rb & -rb);
                        moves.add(new Move(Piece.ROOK, from, to, whiteMailbox[to]));
                    }
                    break;
                case BISHOP:
                    long bishopMap = (slideAttacks(from, diagonalMask[from]) ^ slideAttacks(from, antiDiagonalMask[from])) & ~blackBoard;
                    // captures
                    for (long bb = bishopMap & whiteBoard; bb != 0; bb &= bb - 1) {
                        int to = convert.get(bb & -bb);
                        moves.add(new Move(Piece.BISHOP, from, to, whiteMailbox[to]));
                    }
                    break;
                case QUEEN:
                    long queenMap = (slideAttacks(from, rankMask[from]) ^ slideAttacks(from, fileMask[from]) ^ slideAttacks(from, diagonalMask[from]) ^ slideAttacks(from, antiDiagonalMask[from])) & ~blackBoard;
                    // captures
                    for (long qb = queenMap & whiteBoard; qb != 0; qb &= qb - 1) {
                        int to = convert.get(qb & -qb);
                        moves.add(new Move(Piece.QUEEN, from, to, whiteMailbox[to]));
                    }
                    break;
                case KING:
                    long kingMap = kingMoves[from] & ~blackBoard;
                    // captures
                    for (long kb = kingMap & whiteBoard; kb != 0; kb &= kb - 1) {
                        int to = convert.get(kb & -kb);
                        moves.add(new Move(Piece.KING, from, to, whiteMailbox[to]));
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
        return moves;
    }
    //endregion

    //region make move methods
    private void whiteMoveQuiet(Move m) {
        long fromBB = sq[m.from], toBB = sq[m.to], fromToBB = fromBB ^ toBB;
        switch (m.p) {
            case KNIGHT:
                whiteKnights = whiteKnights ^ fromToBB;
                break;
            case ROOK:
                whiteRooks = whiteRooks ^ fromToBB;
                if (whiteKingside && m.from == 7) {
                    whiteKingside = false;
                }
                if (whiteQueenside && m.from == 0) {
                    whiteQueenside = false;
                }
                break;
            case BISHOP:
                whiteBishops = whiteBishops ^ fromToBB;
                break;
            case QUEEN:
                whiteQueens = whiteQueens ^ fromToBB;
                break;
            case KING:
                whiteKing = whiteKing ^ fromToBB;
                whiteKingside = false;
                whiteQueenside = false;
                break;
            case PAWN:
                whitePawns = whitePawns ^ fromToBB;
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
                blackKnights = blackKnights ^ fromToBB;
                break;
            case ROOK:
                blackRooks = blackRooks ^ fromToBB;
                if (blackKingside && m.from == 63) {
                    blackKingside = false;
                }
                if (blackQueenside && m.from == 56) {
                    blackQueenside = false;
                }
                break;
            case BISHOP:
                blackBishops = blackBishops ^ fromToBB;
                break;
            case QUEEN:
                blackQueens = blackQueens ^ fromToBB;
                break;
            case KING:
                blackKing = blackKing ^ fromToBB;
                blackKingside = false;
                blackQueenside = false;
                break;
            case PAWN:
                blackPawns = blackPawns ^ fromToBB;
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
                whiteKnights = whiteKnights ^ fromToBB;
                break;
            case ROOK:
                whiteRooks = whiteRooks ^ fromToBB;
                if (whiteKingside && m.from == 63) {
                    whiteKingside = false;
                }
                if (whiteQueenside && m.from == 56) {
                    whiteQueenside = false;
                }
                break;
            case BISHOP:
                whiteBishops = whiteBishops ^ fromToBB;
                break;
            case QUEEN:
                whiteQueens = whiteQueens ^ fromToBB;
                break;
            case KING:
                whiteKing = whiteKing ^ fromToBB;
                whiteKingside = false;
                whiteQueenside = false;
                break;
            case PAWN:
                whitePawns = whitePawns ^ fromToBB;
                break;
        }
        whiteBoard ^= fromToBB;
        switch (m.q) {
            case KNIGHT:
                blackKnights = blackKnights ^ toBB;
                break;
            case ROOK:
                blackRooks = blackRooks ^ toBB;
                break;
            case BISHOP:
                blackBishops = blackBishops ^ toBB;
                break;
            case QUEEN:
                blackQueens = blackQueens ^ toBB;
                break;
            case KING:
                blackKing = blackKing ^ toBB;
                hasBlackKing = false;
                break;
            case PAWN:
                blackPawns = blackPawns ^ toBB;
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
                blackKnights = blackKnights ^ fromToBB;
                break;
            case ROOK:
                blackRooks = blackRooks ^ fromToBB;
                if (blackKingside && m.from == 63) {
                    blackKingside = false;
                }
                if (blackQueenside && m.from == 56) {
                    blackQueenside = false;
                }
                break;
            case BISHOP:
                blackBishops = blackBishops ^ fromToBB;
                break;
            case QUEEN:
                blackQueens = blackQueens ^ fromToBB;
                break;
            case KING:
                blackKing = blackKing ^ fromToBB;
                blackKingside = false;
                blackQueenside = false;
                break;
            case PAWN:
                blackPawns = blackPawns ^ fromToBB;
                break;
        }
        blackBoard ^= fromToBB;
        switch (m.q) {
            case KNIGHT:
                whiteKnights = whiteKnights ^ toBB;
                break;
            case ROOK:
                whiteRooks = whiteRooks ^ toBB;
                break;
            case BISHOP:
                whiteBishops = whiteBishops ^ toBB;
                break;
            case QUEEN:
                whiteQueens = whiteQueens ^ toBB;
                break;
            case KING:
                whiteKing = whiteKing ^ toBB;
                hasWhiteKing = false;
                break;
            case PAWN:
                whitePawns = whitePawns ^ toBB;
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
        whitePawns = whitePawns ^ fromBB;
        if (m.r == Piece.QUEEN) {
            whiteQueens = whiteQueens ^ toBB;
        }
        else {
            whiteBishops = whiteBishops ^ toBB;
        }
        if (m.isCapture) {
            switch (m.q) {
                case KNIGHT:
                    blackKnights = blackKnights ^ toBB;
                    break;
                case ROOK:
                    blackRooks = blackRooks ^ toBB;
                    break;
                case BISHOP:
                    blackBishops = blackBishops ^ toBB;
                    break;
                case QUEEN:
                    blackQueens = blackQueens ^ toBB;
                    break;
                case KING:
                    blackKing = blackKing ^ toBB;
                    hasBlackKing = false;
                    break;
                case PAWN:
                    blackPawns = blackPawns ^ toBB;
                    break;
            }

//			hc ^= bt.get(blackMailbox[m.to]).get(m.to);
//			hc ^= bt.get(Piece.NONE).get(m.to);
            blackMailbox[m.to] = Piece.NONE;

            blackBoard ^= toBB;
            board ^= fromBB;
        }
        else {
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
        blackPawns = blackPawns ^ fromBB;
        if (m.r == Piece.QUEEN) {
            blackQueens = blackQueens ^ toBB;
        }
        else {
            blackBishops = blackBishops ^ toBB;
        }
        if (m.isCapture) {
            switch (m.q) {
                case KNIGHT:
                    whiteKnights = whiteKnights ^ toBB;
                    break;
                case ROOK:
                    whiteRooks = whiteRooks ^ toBB;
                    break;
                case BISHOP:
                    whiteBishops = whiteBishops ^ toBB;
                    break;
                case QUEEN:
                    whiteQueens = whiteQueens ^ toBB;
                    break;
                case KING:
                    whiteKing = whiteKing ^ toBB;
                    hasWhiteKing = false;
                    break;
                case PAWN:
                    whitePawns = whitePawns ^ toBB;
                    break;
            }

//			hc ^= wt.get(whiteMailbox[m.to]).get(m.to);
//			hc ^= wt.get(Piece.NONE).get(m.to);
            whiteMailbox[m.to] = Piece.NONE;

            whiteBoard ^= toBB;
            board ^= fromBB;
        }
        else {
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
        blackPawns = blackPawns ^ sq[cap];
        blackBoard ^= sq[cap];
        board ^= sq[cap];

//        hc ^= bt.get(blackMailbox[cap]).get(cap);
//        hc ^= bt.get(Piece.NONE).get(cap);
        blackMailbox[cap] = Piece.NONE;
    }

    private void blackEPCapture(Move m) {
        blackMoveQuiet(m);
        int cap = 8 * (m.from / 8) + (m.to % 8);
        whitePawns = whitePawns ^ sq[cap];
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
        }
        else {
            whiteMoveCapture(new Move(m.p, m.from, m.block, m.q));
        }
    }

    private void blackBlockedMove(Move m) {
        if (m.p == Piece.PAWN) {
            blackMoveQuiet(new Move(m.p, m.from, m.block));
        }
        else {
            blackMoveCapture(new Move(m.p, m.from, m.block, m.q));
        }
    }

    // assumes move is not minimal and has specifications about special moves
    public void whiteMove(Move m) {
        prevWhiteKingside.addLast(whiteKingside);
        prevWhiteQueenside.addLast(whiteQueenside);
        prevEp.addLast(ep);
        prevEps.addLast(eps);
        ep = false;
        if (m.isInvalidMove) {
            whiteInvalidMove();
        }
        else if (m.isBlockedMove) {
            whiteBlockedMove(m);
        }
        else if (m.isEPCapture) {
            whiteEPCapture(m);
        }
        else if (m.isPromotion) {
            whitePromote(m);
        }
        else if (m.isCapture) {
            whiteMoveCapture(m);
        }
        else if (m.isKingsideCastle) {
            whiteKingsideCastle();
        }
        else if (m.isQueensideCastle) {
            whiteQueensideCastle();
        }
        else if (m.isDPMove) {
            whiteDPMove(m);
        }
        else {
            whiteMoveQuiet(m);
        }
    }

    // assumes move is not minimal and has specifications about special moves
    public void blackMove(Move m) {
        prevBlackKingside.addLast(blackKingside);
        prevBlackQueenside.addLast(blackQueenside);
        prevEp.addLast(ep);
        prevEps.addLast(eps);
        ep = false;
        if (m.isInvalidMove) {
            blackInvalidMove();
        }
        else if (m.isBlockedMove) {
            blackBlockedMove(m);
        }
        else if (m.isEPCapture) {
            blackEPCapture(m);
        }
        else if (m.isPromotion) {
            blackPromote(m);
        }
        else if (m.isCapture) {
            blackMoveCapture(m);
        }
        else if (m.isKingsideCastle) {
            blackKingsideCastle();
        }
        else if (m.isQueensideCastle) {
            blackQueensideCastle();
        }
        else if (m.isDPMove) {
            blackDPMove(m);
        }
        else {
            blackMoveQuiet(m);
        }
    }
    //endregion

    //region undo move methods
    private void undoWhiteMoveQuiet(Move m) {
        long fromBB = sq[m.from], toBB = sq[m.to], fromToBB = fromBB ^ toBB;
        switch (m.p) {
            case KNIGHT:
                whiteKnights = whiteKnights ^ fromToBB;
                break;
            case ROOK:
                whiteRooks = whiteRooks ^ fromToBB;
                break;
            case BISHOP:
                whiteBishops = whiteBishops ^ fromToBB;
                break;
            case QUEEN:
                whiteQueens = whiteQueens ^ fromToBB;
                break;
            case KING:
                whiteKing = whiteKing ^ fromToBB;
                break;
            case PAWN:
                whitePawns = whitePawns ^ fromToBB;
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
                blackKnights = blackKnights ^ fromToBB;
                break;
            case ROOK:
                blackRooks = blackRooks ^ fromToBB;
                break;
            case BISHOP:
                blackBishops = blackBishops ^ fromToBB;
                break;
            case QUEEN:
                blackQueens = blackQueens ^ fromToBB;
                break;
            case KING:
                blackKing = blackKing ^ fromToBB;
                break;
            case PAWN:
                blackPawns = blackPawns ^ fromToBB;
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
                whiteKnights = whiteKnights ^ fromToBB;
                break;
            case ROOK:
                whiteRooks = whiteRooks ^ fromToBB;
                break;
            case BISHOP:
                whiteBishops = whiteBishops ^ fromToBB;
                break;
            case QUEEN:
                whiteQueens = whiteQueens ^ fromToBB;
                break;
            case KING:
                whiteKing = whiteKing ^ fromToBB;
                break;
            case PAWN:
                whitePawns = whitePawns ^ fromToBB;
                break;
        }
        whiteBoard ^= fromToBB;
        switch (m.q) {
            case KNIGHT:
                blackKnights = blackKnights ^ toBB;
                break;
            case ROOK:
                blackRooks = blackRooks ^ toBB;
                break;
            case BISHOP:
                blackBishops = blackBishops ^ toBB;
                break;
            case QUEEN:
                blackQueens = blackQueens ^ toBB;
                break;
            case KING:
                blackKing = blackKing ^ toBB;
                break;
            case PAWN:
                blackPawns = blackPawns ^ toBB;
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
                blackKnights = blackKnights ^ fromToBB;
                break;
            case ROOK:
                blackRooks = blackRooks ^ fromToBB;
                break;
            case BISHOP:
                blackBishops = blackBishops ^ fromToBB;
                break;
            case QUEEN:
                blackQueens = blackQueens ^ fromToBB;
                break;
            case KING:
                blackKing = blackKing ^ fromToBB;
                break;
            case PAWN:
                blackPawns = blackPawns ^ fromToBB;
                break;
        }
        blackBoard ^= fromToBB;
        switch (m.q) {
            case KNIGHT:
                whiteKnights = whiteKnights ^ toBB;
                break;
            case ROOK:
                whiteRooks = whiteRooks ^ toBB;
                break;
            case BISHOP:
                whiteBishops = whiteBishops ^ toBB;
                break;
            case QUEEN:
                whiteQueens = whiteQueens ^ toBB;
                break;
            case KING:
                whiteKing = whiteKing ^ toBB;
                break;
            case PAWN:
                whitePawns = whitePawns ^ toBB;
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
        whitePawns = whitePawns ^ fromBB;
        if (m.r == Piece.QUEEN) {
            whiteQueens = whiteQueens ^ toBB;
        }
        else {
            whiteBishops = whiteBishops ^ toBB;
        }
        if (m.isCapture) {
            switch (m.q) {
                case KNIGHT:
                    blackKnights = blackKnights ^ toBB;
                    break;
                case ROOK:
                    blackRooks = blackRooks ^ toBB;
                    break;
                case BISHOP:
                    blackBishops = blackBishops ^ toBB;
                    break;
                case QUEEN:
                    blackQueens = blackQueens ^ toBB;
                    break;
                case KING:
                    blackKing = blackKing ^ toBB;
                    break;
                case PAWN:
                    blackPawns = blackPawns ^ toBB;
                    break;
            }

//            hc ^= bt.get(blackMailbox[m.to]).get(m.to);
//            hc ^= bt.get(m.q).get(m.to);
            blackMailbox[m.to] = m.q;

            blackBoard ^= toBB;
            board ^= fromBB;
        }
        else {
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
        blackPawns = blackPawns ^ fromBB;
        if (m.r == Piece.QUEEN) {
            blackQueens = blackQueens ^ toBB;
        }
        else {
            blackBishops = blackBishops ^ toBB;
        }
        if (m.isCapture) {
            switch (m.q) {
                case KNIGHT:
                    whiteKnights = whiteKnights ^ toBB;
                    break;
                case ROOK:
                    whiteRooks = whiteRooks ^ toBB;
                    break;
                case BISHOP:
                    whiteBishops = whiteBishops ^ toBB;
                    break;
                case QUEEN:
                    whiteQueens = whiteQueens ^ toBB;
                    break;
                case KING:
                    whiteKing = whiteKing ^ toBB;
                    break;
                case PAWN:
                    whitePawns = whitePawns ^ toBB;
                    break;
            }

//            hc ^= wt.get(whiteMailbox[m.to]).get(m.to);
//            hc ^= wt.get(m.q).get(m.to);
            whiteMailbox[m.to] = m.q;

            whiteBoard ^= toBB;
            board ^= fromBB;
        }
        else {
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
        blackPawns = blackPawns ^ sq[cap];
        blackBoard ^= sq[cap];
        board ^= sq[cap];

//        hc ^= bt.get(blackMailbox[cap]).get(cap);
//        hc ^= bt.get(Piece.PAWN).get(cap);
        blackMailbox[cap] = Piece.PAWN;
    }

    private void undoBlackEPCapture(Move m) {
        undoBlackMoveQuiet(m);
        int cap = 8 * (m.from / 8) + (m.to % 8);
        whitePawns = whitePawns ^ sq[cap];
        whiteBoard ^= sq[cap];
        board ^= sq[cap];

//        hc ^= wt.get(whiteMailbox[cap]).get(cap);
//        hc ^= wt.get(Piece.PAWN).get(cap);
        whiteMailbox[cap] = Piece.PAWN;
    }

    private void undoWhiteInvalidMove() {}

    private void undoBlackInvalidMove() {}

    private void undoWhiteBlockedMove(Move m) {
        if (m.p == Piece.PAWN) {
            undoWhiteMoveQuiet(new Move(m.p, m.from, m.block));
        }
        else {
            undoWhiteMoveCapture(new Move(m.p, m.from, m.block, m.q));
        }
    }

    private void undoBlackBlockedMove(Move m) {
        if (m.p == Piece.PAWN) {
            undoBlackMoveQuiet(new Move(m.p, m.from, m.block));
        }
        else {
            undoBlackMoveCapture(new Move(m.p, m.from, m.block, m.q));
        }
    }

    public void undoWhiteMove(Move m) {
        if (m.isInvalidMove) {
            undoWhiteInvalidMove();
        }
        else if (m.isBlockedMove) {
            undoWhiteBlockedMove(m);
        }
        else if (m.isEPCapture) {
            undoWhiteEPCapture(m);
        }
        else if (m.isPromotion) {
            undoWhitePromote(m);
        }
        else if (m.isCapture) {
            undoWhiteMoveCapture(m);
        }
        else if (m.isKingsideCastle) {
            undoWhiteKingsideCastle();
        }
        else if (m.isQueensideCastle) {
            undoWhiteQueensideCastle();
        }
        else if (m.isDPMove) {
            undoWhiteDPMove(m);
        }
        else {
            undoWhiteMoveQuiet(m);
        }
        whiteKingside = prevWhiteKingside.pollLast();
        whiteQueenside = prevWhiteQueenside.pollLast();
        ep = prevEp.pollLast();
        eps = prevEps.pollLast();
        hasBlackKing = true;
        hasWhiteKing = true;
    }

    public void undoBlackMove(Move m) {
        if (m.isInvalidMove) {
            undoBlackInvalidMove();
        }
        else if (m.isBlockedMove) {
            undoBlackBlockedMove(m);
        }
        else if (m.isEPCapture) {
            undoBlackEPCapture(m);
        }
        else if (m.isPromotion) {
            undoBlackPromote(m);
        }
        else if (m.isCapture) {
            undoBlackMoveCapture(m);
        }
        else if (m.isKingsideCastle) {
            undoBlackKingsideCastle();
        }
        else if (m.isQueensideCastle) {
            undoBlackQueensideCastle();
        }
        else if (m.isDPMove) {
            undoBlackDPMove(m);
        }
        else {
            undoBlackMoveQuiet(m);
        }
        blackKingside = prevBlackKingside.pollLast();
        blackQueenside = prevBlackQueenside.pollLast();
        ep = prevEp.pollLast();
        eps = prevEps.pollLast();
        hasBlackKing = true;
        hasWhiteKing = true;
    }
    //endregion

    //region utility methods
    public int sense(boolean color, int x) {
        if (color) {
            return str.get(blackMailbox[x - 9]) + 7 * str.get(blackMailbox[x - 8]) + 49 * str.get(blackMailbox[x - 7])
                    + 343 * str.get(blackMailbox[x - 1]) + 2401 * str.get(blackMailbox[x]) + 16807 * str.get(blackMailbox[x + 1])
                    + 117649 * str.get(blackMailbox[x + 7]) + 823543 * str.get(blackMailbox[x + 8]) + 5764801 * str.get(blackMailbox[x + 9]);
        }
        else {
            return str.get(whiteMailbox[x - 9]) + 7 * str.get(whiteMailbox[x - 8]) + 49 * str.get(whiteMailbox[x - 7])
                    + 343 * str.get(whiteMailbox[x - 1]) + 2401 * str.get(whiteMailbox[x]) + 16807 * str.get(whiteMailbox[x + 1])
                    + 117649 * str.get(whiteMailbox[x + 7]) + 823543 * str.get(whiteMailbox[x + 8]) + 5764801 * str.get(whiteMailbox[x + 9]);
        }
    }

    public void clearCache() {
        prevWhiteKingside.clear();
        prevWhiteQueenside.clear();
        prevBlackKingside.clear();
        prevBlackQueenside.clear();
        prevEp.clear();
        prevEps.clear();
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
                switch (x[8 * j + i]) {
                    case PAWN:
                        sb.append("P");
                        break;
                    case ROOK:
                        sb.append("R");
                        break;
                    case BISHOP:
                        sb.append("B");
                        break;
                    case QUEEN:
                        sb.append("Q");
                        break;
                    case KING:
                        sb.append("K");
                        break;
                    case KNIGHT:
                        sb.append("N");
                        break;
                    default:
                        sb.append(".");
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public String fenString(boolean color) {
        StringBuilder sb = new StringBuilder();
        for (int j = 7; j >= 0; j--) {
            int cnt = 0;
            for (int i = 0; i < 8; i++) {
                Piece w = whiteMailbox[8 * j + i], b = blackMailbox[8 * j + i];
                if (w != Piece.NONE) {
                    if (cnt != 0) {
                        sb.append(cnt);
                        cnt = 0;
                    }
                    switch (w) {
                        case PAWN:
                            sb.append("P");
                            break;
                        case ROOK:
                            sb.append("R");
                            break;
                        case BISHOP:
                            sb.append("B");
                            break;
                        case QUEEN:
                            sb.append("Q");
                            break;
                        case KING:
                            sb.append("K");
                            break;
                        case KNIGHT:
                            sb.append("N");
                            break;
                    }
                }
                else if (b != Piece.NONE) {
                    if (cnt != 0) {
                        sb.append(cnt);
                        cnt = 0;
                    }
                    switch (b) {
                        case PAWN:
                            sb.append("p");
                            break;
                        case ROOK:
                            sb.append("r");
                            break;
                        case BISHOP:
                            sb.append("b");
                            break;
                        case QUEEN:
                            sb.append("q");
                            break;
                        case KING:
                            sb.append("k");
                            break;
                        case KNIGHT:
                            sb.append("n");
                            break;
                    }
                }
                else {
                    cnt++;
                }
            }
            if (cnt != 0) {
                sb.append(cnt);
            }
            if (j != 0) {
                sb.append("/");
            }
        }
        sb.append(" ");
        if (color) {
            sb.append("w");
        }
        else {
            sb.append("b");
        }
        sb.append(" ");
        if (whiteKingside) {
            sb.append("K");
        }
        if (whiteQueenside) {
            sb.append("Q");
        }
        if (blackKingside) {
            sb.append("k");
        }
        if (blackQueenside) {
            sb.append("q");
        }
        if (!whiteKingside && !whiteQueenside && !blackKingside && !blackQueenside) {
            sb.append("-");
        }
        sb.append(" ");
        if (ep) {
            sb.append("abcdefgh".charAt(eps % 8));
            sb.append(eps / 8 + 1);
        }
        else {
            sb.append("-");
        }
        return sb.toString();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int j = 7; j >= 0; j--) {
            for (int i = 0; i < 8; i++) {
                Piece w = whiteMailbox[8 * j + i], b = blackMailbox[8 * j + i];
                if (w != Piece.NONE) {
                    switch (w) {
                        case PAWN:
                            sb.append("P");
                            break;
                        case ROOK:
                            sb.append("R");
                            break;
                        case BISHOP:
                            sb.append("B");
                            break;
                        case QUEEN:
                            sb.append("Q");
                            break;
                        case KING:
                            sb.append("K");
                            break;
                        case KNIGHT:
                            sb.append("N");
                            break;
                    }
                }
                else {
                    switch (b) {
                        case PAWN:
                            sb.append("p");
                            break;
                        case ROOK:
                            sb.append("r");
                            break;
                        case BISHOP:
                            sb.append("b");
                            break;
                        case QUEEN:
                            sb.append("q");
                            break;
                        case KING:
                            sb.append("k");
                            break;
                        case KNIGHT:
                            sb.append("n");
                            break;
                        case NONE:
                            sb.append(".");
                            break;
                    }
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public boolean equals(Object o) {
        if (o instanceof BoardState) {
            BoardState bs = (BoardState) o;
            return equals(bs);
        }
        return false;
    }

    // TODO:
    // a boardstate with everything equal but with lesser castling rights than another boardstate
    // should be removed from hypotheses but is not because it is considered unequal
    public boolean equals(BoardState bs) {
        return whitePawns == bs.whitePawns && whiteKnights == bs.whiteKnights && whiteBishops == bs.whiteBishops &&
                whiteRooks == bs.whiteRooks && whiteQueens == bs.whiteQueens && whiteKing == bs.whiteKing &&
                blackPawns == bs.blackPawns && blackKnights == bs.blackKnights && blackBishops == bs.blackBishops &&
                blackRooks == bs.blackRooks && blackQueens == bs.blackQueens && blackKing == bs.blackKing &&
                whiteKingside == bs.whiteKingside && whiteQueenside == bs.whiteQueenside &&
                blackKingside == bs.blackKingside && blackQueenside == bs.blackQueenside && (ep ? (bs.ep && eps == bs.eps) : !bs.ep);
    }

    // TODO: test good hash code functions
    public int hashCode() {
//		return hc;
        return (int) board;
    }
    //endregion

//    public boolean colorCheck(BoardState bs, boolean color) {
//        return color ? bs.whiteBoard == whiteBoard : bs.blackBoard == blackBoard;
//    }
//
//    public void check() {
//        for (int i = 0; i < 64; i++) {
//            switch (whiteMailbox[i]) {
//                case PAWN:
//                    if ((whitePawns & sq[i]) == 0) {
//                        throw new RuntimeException();
//                    }
//                    break;
//                case KNIGHT:
//                    if ((whiteKnights & sq[i]) == 0) {
//                        throw new RuntimeException();
//                    }
//                    break;
//                case BISHOP:
//                    if ((whiteBishops & sq[i]) == 0) {
//                        throw new RuntimeException();
//                    }
//                    break;
//                case ROOK:
//                    if ((whiteRooks & sq[i]) == 0) {
//                        throw new RuntimeException();
//                    }
//                    break;
//                case QUEEN:
//                    if ((whiteQueens & sq[i]) == 0) {
//                        throw new RuntimeException();
//                    }
//                    break;
//                case KING:
//                    if ((whiteKing & sq[i]) == 0) {
//                        throw new RuntimeException();
//                    }
//                    break;
//            }
//            switch (blackMailbox[i]) {
//                case PAWN:
//                    if ((blackPawns & sq[i]) == 0) {
//                        throw new RuntimeException();
//                    }
//                    break;
//                case KNIGHT:
//                    if ((blackKnights & sq[i]) == 0) {
//                        throw new RuntimeException();
//                    }
//                    break;
//                case BISHOP:
//                    if ((blackBishops & sq[i]) == 0) {
//                        throw new RuntimeException();
//                    }
//                    break;
//                case ROOK:
//                    if ((blackRooks & sq[i]) == 0) {
//                        throw new RuntimeException();
//                    }
//                    break;
//                case QUEEN:
//                    if ((blackQueens & sq[i]) == 0) {
//                        throw new RuntimeException();
//                    }
//                    break;
//                case KING:
//                    if ((blackKing & sq[i]) == 0) {
//                        throw new RuntimeException();
//                    }
//                    break;
//            }
//        }
//    }
}