package ubc.cosc322.engine;

/**
 * A complete turn in the Game of Amazons:
 * 1. Select queen at (queenRow, queenCol)
 * 2. Move queen to (moveRow, moveCol)
 * 3. Shoot arrow to (arrowRow, arrowCol)
 */
public class Move {
    public final int queenRow, queenCol;
    public final int moveRow, moveCol;
    public final int arrowRow, arrowCol;

    public Move(int queenRow, int queenCol, int moveRow, int moveCol, int arrowRow, int arrowCol) {
        this.queenRow = queenRow;
        this.queenCol = queenCol;
        this.moveRow = moveRow;
        this.moveCol = moveCol;
        this.arrowRow = arrowRow;
        this.arrowCol = arrowCol;
    }

    @Override
    public String toString() {
        return String.format("Queen(%d,%d) -> (%d,%d), Arrow -> (%d,%d)",
                queenRow, queenCol, moveRow, moveCol, arrowRow, arrowCol);
    }
}
