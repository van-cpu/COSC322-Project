package ubc.cosc322.engine;

public interface AmazonPlayer {
    String getName();

    Move chooseMove(Board board, boolean isBlack);
}
