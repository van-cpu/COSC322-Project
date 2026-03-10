package ubc.cosc322.engine;

public class Move {
    public final int queenRow;
    public final int queenCol;
    public final int moveRow;
    public final int moveCol;
    public final int arrowRow;
    public final int arrowCol;

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
        return String.format(
            "Queen(%d,%d) -> (%d,%d), Arrow -> (%d,%d)",
            queenRow,
            queenCol,
            moveRow,
            moveCol,
            arrowRow,
            arrowCol
        );
    }
}
