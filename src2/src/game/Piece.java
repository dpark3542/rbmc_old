package game;

/**
 * Created by dpark3542 on 7/28/2017.
 */
public enum Piece {
    PAWN, ROOK, BISHOP, QUEEN, KING, KNIGHT, NONE;

    public String toString() {
        switch(this) {
            case PAWN:
                return "P";
            case ROOK:
                return "R";
            case BISHOP:
                return "B";
            case QUEEN:
                return "Q";
            case KING:
                return "K";
            case KNIGHT:
                return "N";
        }
        return ".";
    }
}
