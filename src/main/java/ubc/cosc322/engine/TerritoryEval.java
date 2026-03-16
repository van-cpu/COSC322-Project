package ubc.cosc322.engine;

import ubc.cosc322.engine.Board.Tile;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;

/**
 * Composite evaluation function for Game of Amazons.
 *
 * Priority ordering (weighted sum):
 *   1. Territory control  — Voronoi flood-fill from all queens.
 *   2. Mobility differential — total legal moves differential.
 *   3. Centrality — queens near the centre are harder to trap.
 *
 * Returns a value from the perspective of the "isBlack" player.
 * Positive = good for isBlack, negative = bad.
 */
public class TerritoryEval {

    private static final int W_TERRITORY = 5;
    private static final int W_MOBILITY  = 1;
    private static final int W_CENTRAL   = 1;

    private static final int WIN_SCORE  =  100000;
    private static final int LOSS_SCORE = -100000;

    /** Evaluate the board from the perspective of the isBlack player. */
    public static int evaluate(Board board, boolean isBlack) {
        Tile myQueen  = isBlack ? Tile.BLACK_QUEEN : Tile.WHITE_QUEEN;
        Tile oppQueen = isBlack ? Tile.WHITE_QUEEN : Tile.BLACK_QUEEN;

        int myMobility  = board.totalMobility(myQueen);
        int oppMobility = board.totalMobility(oppQueen);

        if (myMobility  == 0) return LOSS_SCORE;
        if (oppMobility == 0) return WIN_SCORE;

        int[] voronoi = voronoiTerritory(board);
        int myTerritory  = isBlack ? voronoi[1] : voronoi[0];
        int oppTerritory = isBlack ? voronoi[0] : voronoi[1];

        int centrality = centralityScore(board, myQueen)
                       - centralityScore(board, oppQueen);

        return W_TERRITORY * (myTerritory  - oppTerritory)
             + W_MOBILITY  * (myMobility   - oppMobility)
             + W_CENTRAL   *  centrality;
    }

    /**
     * Voronoi territory via BFS.
     * Returns int[2]: { white_territory_count, black_territory_count }.
     */
    public static int[] voronoiTerritory(Board board) {
        int S = Board.SIZE;
        int[][] dist  = new int[S][S];
        int[][] owner = new int[S][S];

        for (int[] row : dist)  Arrays.fill(row, Integer.MAX_VALUE);
        for (int[] row : owner) Arrays.fill(row, -1);

        Queue<int[]> q = new ArrayDeque<int[]>();

        for (int r = 0; r < S; r++) {
            for (int c = 0; c < S; c++) {
                Tile t = board.get(r, c);
                if (t == Tile.WHITE_QUEEN) {
                    dist[r][c] = 0; owner[r][c] = 0; q.add(new int[]{r, c, 0});
                } else if (t == Tile.BLACK_QUEEN) {
                    dist[r][c] = 0; owner[r][c] = 1; q.add(new int[]{r, c, 1});
                }
            }
        }

        int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1},{-1,-1},{-1,1},{1,-1},{1,1}};

        while (!q.isEmpty()) {
            int[] cur = q.poll();
            int r = cur[0], c = cur[1], own = cur[2];
            int nextDist = dist[r][c] + 1;

            for (int[] d : dirs) {
                int nr = r + d[0], nc = c + d[1];
                if (nr < 0 || nr >= S || nc < 0 || nc >= S) continue;
                if (board.get(nr, nc) != Tile.FREE)          continue;

                if (nextDist < dist[nr][nc]) {
                    dist[nr][nc]  = nextDist;
                    owner[nr][nc] = own;
                    q.add(new int[]{nr, nc, own});
                } else if (nextDist == dist[nr][nc] && owner[nr][nc] != own) {
                    owner[nr][nc] = -1;
                }
            }
        }

        int[] counts = new int[2];
        for (int r = 0; r < S; r++)
            for (int c = 0; c < S; c++)
                if (owner[r][c] >= 0) counts[owner[r][c]]++;

        return counts;
    }

    private static int centralityScore(Board board, Tile queenType) {
        int score = 0;
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                if (board.get(r, c) == queenType) {
                    double dr = Math.abs(r - 4.5);
                    double dc = Math.abs(c - 4.5);
                    score += (int)(4.5 - Math.max(dr, dc));
                }
            }
        }
        return score;
    }
}
