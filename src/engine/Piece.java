package engine;

public enum Piece {
    PAWN, ROOK, BISHOP, QUEEN, KING, KNIGHT, NONE;

    public String toString() {
        switch (this) {
            case PAWN:
                return "pawn";
            case ROOK:
                return "rook";
            case BISHOP:
                return "bishop";
            case QUEEN:
                return "queen";
            case KING:
                return "king";
            case KNIGHT:
                return "knight";
            case NONE:
                return "none";
        }
        throw new RuntimeException("invalid piece type");
    }
}
