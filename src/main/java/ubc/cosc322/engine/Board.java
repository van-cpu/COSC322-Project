package ubc.cosc322.engine;

import java.util.ArrayList;
import java.util.List;

public class Board {
    public static final int SIZE = 10;

    public enum Tile {
        FREE,
        WHITE_QUEEN,
        BLACK_QUEEN,
        WHITE_BOW,
        BLACK_BOW,
        WHITE_ARROW,
        BLACK_ARROW
    }

    private static final int[][] BLACK_START = {{0, 3}, {0, 6}, {3, 0}, {3, 9}};
    private static final int[][] WHITE_START = {{6, 0}, {6, 9}, {9, 3}, {9, 6}};
    private final Tile[][] grid;

    public Board() {
        this.grid = new Tile[SIZE][SIZE];
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                grid[row][col] = Tile.FREE;
            }
        }
        for (int[] pos : BLACK_START) {
            grid[pos[0]][pos[1]] = Tile.BLACK_QUEEN;
        }
        for (int[] pos : WHITE_START) {
            grid[pos[0]][pos[1]] = Tile.WHITE_QUEEN;
        }
    }

    public Board(Board other) {
        this.grid = new Tile[SIZE][SIZE];
        for (int row = 0; row < SIZE; row++) {
            System.arraycopy(other.grid[row], 0, grid[row], 0, SIZE);
        }
    }

    public Tile get(int row, int col) {
        return grid[row][col];
    }

    public void set(int row, int col, Tile tile) {
        grid[row][col] = tile;
    }

    public List<int[]> findMovableQueens(Tile queenType) {
        List<int[]> queens = new ArrayList<>();
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (grid[row][col] == queenType && !validMoves(row, col).isEmpty()) {
                    queens.add(new int[]{row, col});
                }
            }
        }
        return queens;
    }

    public List<int[]> validMoves(int row, int col) {
        List<int[]> moves = new ArrayList<>();
        int[][] directions = {
            {-1, 0}, {1, 0}, {0, -1}, {0, 1},
            {-1, -1}, {-1, 1}, {1, -1}, {1, 1}
        };

        for (int[] direction : directions) {
            int nextRow = row + direction[0];
            int nextCol = col + direction[1];
            while (nextRow >= 0 && nextRow < SIZE && nextCol >= 0 && nextCol < SIZE && grid[nextRow][nextCol] == Tile.FREE) {
                moves.add(new int[]{nextRow, nextCol});
                nextRow += direction[0];
                nextCol += direction[1];
            }
        }

        return moves;
    }

    public void applyMove(Move move, boolean isBlack) {
        Tile queen = isBlack ? Tile.BLACK_QUEEN : Tile.WHITE_QUEEN;
        Tile arrow = isBlack ? Tile.BLACK_ARROW : Tile.WHITE_ARROW;

        grid[move.queenRow][move.queenCol] = Tile.FREE;
        grid[move.moveRow][move.moveCol] = queen;
        grid[move.arrowRow][move.arrowCol] = arrow;
    }

    public int totalMobility(Tile queenType) {
        int total = 0;
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (grid[row][col] == queenType) {
                    total += validMoves(row, col).size();
                }
            }
        }
        return total;
    }

    public boolean hasValidMove(Tile queenType) {
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (grid[row][col] == queenType && !validMoves(row, col).isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }
}
