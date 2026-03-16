package ubc.cosc322.engine;

import ubc.cosc322.engine.Board.Tile;
import java.util.List;

/**
 * Territory-based AI using AlphaBeta search with the Voronoi territory
 * evaluation from TerritoryEval.
 *
 * At depth=1 this is effectively greedy best-first search.
 * At depth=2+ it adds opponent response consideration via alpha-beta.
 */
public class TerritoryAI implements AmazonPlayer {

    private final int maxDepth;

    public TerritoryAI() {
        this(1);
    }

    public TerritoryAI(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    @Override
    public String getName() {
        return "TerritoryAI (depth=" + maxDepth + ")";
    }

    @Override
    public Move chooseMove(Board board, boolean isBlack) {
        Tile myQueen = isBlack ? Tile.BLACK_QUEEN : Tile.WHITE_QUEEN;

        List<int[]> queens = board.findMovableQueens(myQueen);
        Move bestMove  = null;
        int  bestScore = Integer.MIN_VALUE;

        for (int[] q : queens) {
            int qr = q[0], qc = q[1];
            board.set(qr, qc, Tile.FREE);

            for (int[] m : board.validMoves(qr, qc)) {
                int mr = m[0], mc = m[1];
                board.set(mr, mc, myQueen);

                for (int[] a : board.validMoves(mr, mc)) {
                    int ar = a[0], ac = a[1];
                    Tile arrowTile = isBlack ? Tile.BLACK_ARROW : Tile.WHITE_ARROW;
                    board.set(ar, ac, arrowTile);

                    int score = (maxDepth <= 1)
                        ? TerritoryEval.evaluate(board, isBlack)
                        : alphaBeta(board, maxDepth - 1,
                                    Integer.MIN_VALUE, Integer.MAX_VALUE,
                                    !isBlack, isBlack);

                    if (score > bestScore) {
                        bestScore = score;
                        bestMove  = new Move(qr, qc, mr, mc, ar, ac);
                    }

                    board.set(ar, ac, Tile.FREE);
                }

                board.set(mr, mc, Tile.FREE);
            }

            board.set(qr, qc, myQueen);
        }

        return bestMove;
    }

    private int alphaBeta(Board board, int depth, int alpha, int beta,
                          boolean currentIsBlack, boolean rootIsBlack) {
        Tile currentQueen = currentIsBlack ? Tile.BLACK_QUEEN : Tile.WHITE_QUEEN;

        if (depth == 0 || !board.hasValidMove(currentQueen)) {
            return TerritoryEval.evaluate(board, rootIsBlack);
        }

        boolean maximizing = (currentIsBlack == rootIsBlack);
        List<int[]> queens = board.findMovableQueens(currentQueen);

        if (maximizing) {
            int maxEval = Integer.MIN_VALUE;
            outer:
            for (int[] q : queens) {
                int qr = q[0], qc = q[1];
                board.set(qr, qc, Tile.FREE);

                for (int[] m : board.validMoves(qr, qc)) {
                    int mr = m[0], mc = m[1];
                    board.set(mr, mc, currentQueen);

                    for (int[] a : board.validMoves(mr, mc)) {
                        int ar = a[0], ac = a[1];
                        Tile arrowTile = currentIsBlack ? Tile.BLACK_ARROW : Tile.WHITE_ARROW;
                        board.set(ar, ac, arrowTile);

                        int eval = alphaBeta(board, depth - 1, alpha, beta,
                                             !currentIsBlack, rootIsBlack);
                        maxEval = Math.max(maxEval, eval);
                        alpha   = Math.max(alpha,   eval);

                        board.set(ar, ac, Tile.FREE);
                        if (beta <= alpha) { board.set(mr, mc, Tile.FREE); board.set(qr, qc, currentQueen); break outer; }
                    }
                    board.set(mr, mc, Tile.FREE);
                }
                board.set(qr, qc, currentQueen);
            }
            return maxEval;

        } else {
            int minEval = Integer.MAX_VALUE;
            outer:
            for (int[] q : queens) {
                int qr = q[0], qc = q[1];
                board.set(qr, qc, Tile.FREE);

                for (int[] m : board.validMoves(qr, qc)) {
                    int mr = m[0], mc = m[1];
                    board.set(mr, mc, currentQueen);

                    for (int[] a : board.validMoves(mr, mc)) {
                        int ar = a[0], ac = a[1];
                        Tile arrowTile = currentIsBlack ? Tile.BLACK_ARROW : Tile.WHITE_ARROW;
                        board.set(ar, ac, arrowTile);

                        int eval = alphaBeta(board, depth - 1, alpha, beta,
                                             !currentIsBlack, rootIsBlack);
                        minEval = Math.min(minEval, eval);
                        beta    = Math.min(beta,    eval);

                        board.set(ar, ac, Tile.FREE);
                        if (beta <= alpha) { board.set(mr, mc, Tile.FREE); board.set(qr, qc, currentQueen); break outer; }
                    }
                    board.set(mr, mc, Tile.FREE);
                }
                board.set(qr, qc, currentQueen);
            }
            return minEval;
        }
    }
}
