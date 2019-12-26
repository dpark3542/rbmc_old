package bots;

import engine.BoardState;
import engine.Move;
import engine.Piece;

public interface Bot {
    /**
     * Initializes a new game with player color.
     * True for white, false for black.
     *
     * @param color color bot is to play
     */
    void gameStart(boolean color);

    /**
     * Initializes a game to a specified board state with player color.
     * True for white, false for black.
     *
     * @param color color bot is to play
     * @param bs    initial board state
     */
    void gameStart(boolean color, BoardState bs);

    /**
     * Starts turn with capture information.
     *
     * @param capture boolean if capture was made previous enemy turn
     * @param sq     square where our piece was captured in previous enemy turn
     */
    void turnStart(boolean capture, int sq);

    /**
     * Executes sense phase.
     *
     * @return square to sense
     */
    int sensePhase();

    /**
     * Interprets sense results.
     *
     * @param results sense results represented as integer
     */
    void senseResults(int results);

    /**
     * Executes move phase.
     *
     * @return move to make
     */
    Move movePhase();

    /**
     * Interprets move results.
     *
     * @param end square where the piece we moved ended
     * @param capture boolean if an enemy piece was captured
     */
    void moveResults(int end, boolean capture);

    /**
     * Executes promotion phase.
     *
     * @return piece to promote to
     */
    Piece promotePhase();

    /**
     * Interprets promotion results.
     */
    void promoteResults();

    /**
     * Ends game.
     */
    void gameOver();
}
