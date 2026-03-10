package ubc.cosc322.engine;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;

import ubc.cosc322.engine.Board.Tile;

public final class TerritoryEval {
    private static final int WIN_SCORE = 100000;
    private static final int LOSS_SCORE = -100000;

    private TerritoryEval() {
    }

    public static int evaluate(Board board, boolean isBlack) {
        Tile mine = isBlack ? Tile.BLACK_QUEEN : Tile.WHITE_QUEEN;
        Tile theirs = isBlack ? Tile.WHITE_QUEEN : Tile.BLACK_QUEEN;

        int myMobility = board.totalMobility(mine);
        int theirMobility = board.totalMobility(theirs);
        if (myMobility == 0) {
            return LOSS_SCORE;
        }
        if (theirMobility == 0) {
            return WIN_SCORE;
        }

        int[] territory = computeTerritory(board);
        int myTerritory = isBlack ? territory[1] : territory[0];
        int theirTerritory = isBlack ? territory[0] : territory[1];

        int centerDelta = centerScore(board, mine) - centerScore(board, theirs);
        return 5 * (myTerritory - theirTerritory) + (myMobility - theirMobility) + centerDelta;
    }

    private static int[] computeTerritory(Board board) {
        int size = Board.SIZE;
        int[][] distance = new int[size][size];
        int[][] owner = new int[size][size];
        for (int row = 0; row < size; row++) {
            Arrays.fill(distance[row], Integer.MAX_VALUE);
            Arrays.fill(owner[row], -1);
        }

        Queue<int[]> queue = new ArrayDeque<>();
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                Tile tile = board.get(row, col);
                if (tile == Tile.WHITE_QUEEN) {
                    distance[row][col] = 0;
                    owner[row][col] = 0;
                    queue.add(new int[]{row, col, 0});
                } else if (tile == Tile.BLACK_QUEEN) {
                    distance[row][col] = 0;
                    owner[row][col] = 1;
                    queue.add(new int[]{row, col, 1});
                }
            }
        }

        int[][] directions = {
            {-1, 0}, {1, 0}, {0, -1}, {0, 1},
            {-1, -1}, {-1, 1}, {1, -1}, {1, 1}
        };

        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int row = current[0];
            int col = current[1];
            int side = current[2];
            int nextDistance = distance[row][col] + 1;

            for (int[] direction : directions) {
                int nextRow = row + direction[0];
                int nextCol = col + direction[1];
                if (nextRow < 0 || nextRow >= size || nextCol < 0 || nextCol >= size) {
                    continue;
                }
                if (board.get(nextRow, nextCol) != Tile.FREE) {
                    continue;
                }

                if (nextDistance < distance[nextRow][nextCol]) {
                    distance[nextRow][nextCol] = nextDistance;
                    owner[nextRow][nextCol] = side;
                    queue.add(new int[]{nextRow, nextCol, side});
                } else if (nextDistance == distance[nextRow][nextCol] && owner[nextRow][nextCol] != side) {
                    owner[nextRow][nextCol] = -1;
                }
            }
        }

        int[] totals = new int[2];
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                if (owner[row][col] >= 0) {
                    totals[owner[row][col]]++;
                }
            }
        }
        return totals;
    }

    private static int centerScore(Board board, Tile queenType) {
        int score = 0;
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                if (board.get(row, col) == queenType) {
                    double rowDist = Math.abs(row - 4.5);
                    double colDist = Math.abs(col - 4.5);
                    score += (int) (4.5 - Math.max(rowDist, colDist));
                }
            }
        }
        return score;
    }
}
