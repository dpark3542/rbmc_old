package engine;

public class Move {
    public int from = -1; // square moving piece is coming from
    public int to = -1; // square moving piece is going to
    public int block = -1; // square where piece will end up in a blocked move

    public Piece p = Piece.NONE; // piece to be moved
    public Piece q = Piece.NONE; // piece to be captured
    public Piece r = Piece.NONE; // piece to be promoted to

    public boolean isCapture; // if move results in a capture
    public boolean isKingsideCastle; // if move is a kingside castle
    public boolean isQueensideCastle; // if move is a queenside castle
    public boolean isDPMove; // if move is a double pawn push
    public boolean isEPCapture; // if move is an en passant, where our pawn captured an enemy pawn
    public boolean isPromotion; // if move results in promotion
    public boolean isInvalidMove; // if move will result in no change in the board
    public boolean isBlockedMove; // if move will not be executed correctly but still change the board

    public Move() {}

    // quiet move constructor
    public Move(Piece p, int from, int to) {
        this.from = from;
        this.to = to;
        this.p = p;
    }

    // capture move constructor
    public Move(Piece p, int from, int to, Piece q) {
        this.from = from;
        this.to = to;
        this.p = p;
        this.q = q;
        isCapture = true;
    }

    // promotion quiet move constructor
    public Move(Piece p, Piece r, int from, int to) {
        this.from = from;
        this.to = to;
        this.p = p;
        this.r = r;
        isPromotion = true;
    }

    // promotion capture move constructor
    public Move(Piece p, Piece r, int from, int to, Piece q) {
        this.from = from;
        this.to = to;
        this.p = p;
        this.q = q;
        this.r = r;
        isCapture = true;
        isPromotion = true;
    }

    // blocked move constructor (pawns)
    public Move(Piece p, int from, int to, int block) {
        this.from = from;
        this.to = to;
        this.block = block;
        this.p = p;
        isBlockedMove = true;
    }

    // blocked move constructor (captures)
    public Move(Piece p, int from, int to, int block, Piece q) {
        this.from = from;
        this.to = to;
        this.block = block;
        this.p = p;
        this.q = q;
        isCapture = true;
        isBlockedMove = true;
    }

    // minimal move constructor
    // contains smallest amount of information needed to uniquely identify a move
    public Move(int from, int to, Piece r) {
        this.from = from;
        this.to = to;
        this.r = r;
    }

    // returns a minimal copy
    public Move copy() {
        return new Move(from, to, r);
    }

    public String toString() {
        if (from == -1 && to == -1) {
            return "invalid move";
        }
        return "abcdefgh".substring(from % 8, from % 8 + 1) + (from / 8 + 1) + " " + "abcdefgh".substring(to % 8, to % 8 + 1) + (to / 8 + 1);
    }

    /**
     * Compares two moves.
     * Only the from square, to square, and promotion piece are checked.
     *
     * @param o object to compare move to
     * @return boolean if moves are equal
     */
    public boolean equals(Object o) {
        if (o instanceof Move) {
            Move m = (Move) o;
            return from == m.from && to == m.to && r == m.r;
        }
        return false;
    }

    public int hashCode() {
        return 64 * from + to;
    }
}
