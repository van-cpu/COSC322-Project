package ubc.cosc322.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * 10x10 Game of Amazons board.
 * Tiles are accessed as board[row][col] (y, x).
 */
public class Board {

    public static final int SIZE = 10;

    public enum Tile {
        FREE, WHITE_QUEEN, BLACK_QUEEN, WHITE_BOW, BLACK_BOW, WHITE_ARROW, BLACK_ARROW
    }

    private Tile[][] grid;

    // Standard starting positions (row, col)
    private static final int[][] BLACK_START = {{0, 3}, {0, 6}, {3, 0}, {3, 9}};
    private static final int[][] WHITE_START = {{6, 0}, {6, 9}, {9, 3}, {9, 6}};

    public Board() {
        grid = new Tile[SIZE][SIZE];
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                grid[r][c] = Tile.FREE;
        for (int[] pos : BLACK_START) grid[pos[0]][pos[1]] = Tile.BLACK_QUEEN;
        for (int[] pos : WHITE_START) grid[pos[0]][pos[1]] = Tile.WHITE_QUEEN;
    }

    /** Deep copy constructor */
    public Board(Board other) {
        grid = new Tile[SIZE][SIZE];
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                grid[r][c] = other.grid[r][c];
    }

    public Tile get(int row, int col) {
        return grid[row][col];
    }

    public void set(int row, int col, Tile tile) {
        grid[row][col] = tile;
    }

    /** Find all positions of a given tile type that have at least one valid move. */
    public List<int[]> findMovableQueens(Tile queenType) {
        List<int[]> result = new ArrayList<int[]>();
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                if (grid[r][c] == queenType && !validMoves(r, c).isEmpty())
                    result.add(new int[]{r, c});
        return result;
    }

    /** Find all positions of a given tile type (regardless of mobility). */
    public List<int[]> findAll(Tile queenType) {
        List<int[]> result = new ArrayList<int[]>();
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                if (grid[r][c] == queenType)
                    result.add(new int[]{r, c});
        return result;
    }

    /** Get all valid queen-style moves from (row, col). */
    public List<int[]> validMoves(int row, int col) {
        List<int[]> moves = new ArrayList<int[]>();
        int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1},{-1,-1},{-1,1},{1,-1},{1,1}};
        for (int[] d : dirs) {
            int r = row + d[0], c = col + d[1];
            while (r >= 0 && r < SIZE && c >= 0 && c < SIZE && grid[r][c] == Tile.FREE) {
                moves.add(new int[]{r, c});
                r += d[0];
                c += d[1];
            }
        }
        return moves;
    }

    /** Apply a full turn: move queen from (qr,qc) to (mr,mc), shoot arrow to (ar,ac). */
    public void applyMove(Move move, boolean isBlack) {
        Tile bow = isBlack ? Tile.BLACK_BOW : Tile.WHITE_BOW;
        Tile arrow = isBlack ? Tile.BLACK_ARROW : Tile.WHITE_ARROW;
        Tile queen = isBlack ? Tile.BLACK_QUEEN : Tile.WHITE_QUEEN;

        grid[move.queenRow][move.queenCol] = Tile.FREE;
        grid[move.moveRow][move.moveCol] = bow;
        // Arrow shot from new queen position
        grid[move.arrowRow][move.arrowCol] = arrow;
        // The bow becomes the queen's resting place
        grid[move.moveRow][move.moveCol] = queen;
    }

    /** Count total valid moves for all queens of a given type. */
    public int totalMobility(Tile queenType) {
        int total = 0;
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                if (grid[r][c] == queenType)
                    total += validMoves(r, c).size();
        return total;
    }

    /** Check if a player has any valid moves. */
    public boolean hasValidMove(Tile queenType) {
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                if (grid[r][c] == queenType && !validMoves(r, c).isEmpty())
                    return true;
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("  0 1 2 3 4 5 6 7 8 9\n");
        for (int r = 0; r < SIZE; r++) {
            sb.append(r).append(' ');
            for (int c = 0; c < SIZE; c++) {
                switch (grid[r][c]) {
                    case FREE:        sb.append(". "); break;
                    case WHITE_QUEEN: sb.append("W "); break;
                    case BLACK_QUEEN: sb.append("B "); break;
                    case WHITE_BOW:   sb.append("w "); break;
                    case BLACK_BOW:   sb.append("b "); break;
                    case WHITE_ARROW: sb.append("x "); break;
                    case BLACK_ARROW: sb.append("X "); break;
                }
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
