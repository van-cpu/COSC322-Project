package ubc.cosc322.engine;

/**
 * Interface for an Amazons AI player.
 */
public interface AmazonPlayer {

    /** Return a name for display purposes. */
    String getName();

    /**
     * Choose a move given the current board state.
     * @param board the current board (do not modify it)
     * @param isBlack true if this player is playing black
     * @return the chosen move
     */
    Move chooseMove(Board board, boolean isBlack);
}
