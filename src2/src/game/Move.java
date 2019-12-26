package game;

/**
 * Created by dpark3542 on 7/28/2017.
 */
public class Move {
    public int from, to, block;
    public Piece p, q, r;
    public boolean isCapture, isKingsideCastle, isQueensideCastle, isDPMove, isEPCapture, isPromotion, isInvalidMove, isBlockedMove;

    public boolean basicEquals(Move m){
    	if(from == m.from && to == m.to && p == m.p && q == m.q)
    		return true;
    	return false;
    }
    
    
    public Move() {
        isCapture = false;
        isKingsideCastle = false;
        isQueensideCastle = false;
        isDPMove = false;
        isEPCapture = false;
        isPromotion = false;
        isInvalidMove = false;
        isBlockedMove = false;
    }

    // quiet move constructor
    public Move(Piece p, int from, int to) {
        this.from = from;
        this.to = to;
        this.p = p;
        isCapture = false;
        isKingsideCastle = false;
        isQueensideCastle = false;
        isDPMove = false;
        isEPCapture = false;
        isPromotion = false;
        isInvalidMove = false;
        isBlockedMove = false;
    }

    // capture move constructor
    public Move(Piece p, int from, int to, Piece q) {
        this.from = from;
        this.to = to;
        this.p = p;
        this.q = q;
        isCapture = true;
        isKingsideCastle = false;
        isQueensideCastle = false;
        isDPMove = false;
        isEPCapture = false;
        isPromotion = false;
        isInvalidMove = false;
        isBlockedMove = false;
    }

    // promotion quiet move constructor
    public Move(Piece p, Piece r, int from, int to) {
        this.from = from;
        this.to = to;
        this.p = p;
        this.r = r;
        isCapture = false;
        isKingsideCastle = false;
        isQueensideCastle = false;
        isDPMove = false;
        isEPCapture = false;
        isPromotion = true;
        isInvalidMove = false;
        isBlockedMove = false;
    }

    // promotion capture move constructor
    public Move(Piece p, Piece r, int from, int to, Piece q) {
        this.from = from;
        this.to = to;
        this.p = p;
        this.q = q;
        this.r = r;
        isCapture = true;
        isKingsideCastle = false;
        isQueensideCastle = false;
        isDPMove = false;
        isEPCapture = false;
        isPromotion = true;
        isInvalidMove = false;
        isBlockedMove = false;
    }

    // blocked move constructor
    public Move(Piece p, int from, int to, int block) {
        this.from = from;
        this.to = to;
        this.block = block;
        this.p = p;
        isCapture = false;
        isKingsideCastle = false;
        isQueensideCastle = false;
        isDPMove = false;
        isEPCapture = false;
        isPromotion = false;
        isInvalidMove = false;
        isBlockedMove = true;
    }

    public boolean equals(Move m) {
        return from == m.from && to == m.to && r == m.r;
    }
}
