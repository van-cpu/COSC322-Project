package ubc.cosc322.engine;

import java.util.List;

import ubc.cosc322.engine.Board.Tile;

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
        Tile myArrow = isBlack ? Tile.BLACK_ARROW : Tile.WHITE_ARROW;

        List<int[]> queens = board.findMovableQueens(myQueen);
        Move bestMove = null;
        int bestScore = Integer.MIN_VALUE;

        for (int[] queen : queens) {
            int queenRow = queen[0];
            int queenCol = queen[1];
            board.set(queenRow, queenCol, Tile.FREE);

            List<int[]> queenMoves = board.validMoves(queenRow, queenCol);
            for (int[] move : queenMoves) {
                int moveRow = move[0];
                int moveCol = move[1];
                board.set(moveRow, moveCol, myQueen);

                List<int[]> arrows = board.validMoves(moveRow, moveCol);
                for (int[] arrow : arrows) {
                    int arrowRow = arrow[0];
                    int arrowCol = arrow[1];
                    board.set(arrowRow, arrowCol, myArrow);

                    int score;
                    if (maxDepth <= 1) {
                        score = TerritoryEval.evaluate(board, isBlack);
                    } else {
                        score = alphaBeta(board, maxDepth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, !isBlack, isBlack);
                    }

                    if (score > bestScore) {
                        bestScore = score;
                        bestMove = new Move(queenRow, queenCol, moveRow, moveCol, arrowRow, arrowCol);
                    }

                    board.set(arrowRow, arrowCol, Tile.FREE);
                }

                board.set(moveRow, moveCol, Tile.FREE);
            }

            board.set(queenRow, queenCol, myQueen);
        }

        return bestMove;
    }

    private int alphaBeta(
        Board board,
        int depth,
        int alpha,
        int beta,
        boolean currentIsBlack,
        boolean rootIsBlack
    ) {
        Tile currentQueen = currentIsBlack ? Tile.BLACK_QUEEN : Tile.WHITE_QUEEN;
        Tile currentArrow = currentIsBlack ? Tile.BLACK_ARROW : Tile.WHITE_ARROW;

        if (depth == 0 || !board.hasValidMove(currentQueen)) {
            return TerritoryEval.evaluate(board, rootIsBlack);
        }

        boolean maximizing = currentIsBlack == rootIsBlack;
        List<int[]> queens = board.findMovableQueens(currentQueen);

        if (maximizing) {
            int best = Integer.MIN_VALUE;
            for (int[] queen : queens) {
                int queenRow = queen[0];
                int queenCol = queen[1];
                board.set(queenRow, queenCol, Tile.FREE);

                List<int[]> queenMoves = board.validMoves(queenRow, queenCol);
                for (int[] move : queenMoves) {
                    int moveRow = move[0];
                    int moveCol = move[1];
                    board.set(moveRow, moveCol, currentQueen);

                    List<int[]> arrows = board.validMoves(moveRow, moveCol);
                    for (int[] arrow : arrows) {
                        int arrowRow = arrow[0];
                        int arrowCol = arrow[1];
                        board.set(arrowRow, arrowCol, currentArrow);

                        int value = alphaBeta(board, depth - 1, alpha, beta, !currentIsBlack, rootIsBlack);
                        board.set(arrowRow, arrowCol, Tile.FREE);

                        if (value > best) {
                            best = value;
                        }
                        if (value > alpha) {
                            alpha = value;
                        }
                        if (alpha >= beta) {
                            board.set(moveRow, moveCol, Tile.FREE);
                            board.set(queenRow, queenCol, currentQueen);
                            return best;
                        }
                    }

                    board.set(moveRow, moveCol, Tile.FREE);
                }

                board.set(queenRow, queenCol, currentQueen);
            }
            return best;
        }

        int best = Integer.MAX_VALUE;
        for (int[] queen : queens) {
            int queenRow = queen[0];
            int queenCol = queen[1];
            board.set(queenRow, queenCol, Tile.FREE);

            List<int[]> queenMoves = board.validMoves(queenRow, queenCol);
            for (int[] move : queenMoves) {
                int moveRow = move[0];
                int moveCol = move[1];
                board.set(moveRow, moveCol, currentQueen);

                List<int[]> arrows = board.validMoves(moveRow, moveCol);
                for (int[] arrow : arrows) {
                    int arrowRow = arrow[0];
                    int arrowCol = arrow[1];
                    board.set(arrowRow, arrowCol, currentArrow);

                    int value = alphaBeta(board, depth - 1, alpha, beta, !currentIsBlack, rootIsBlack);
                    board.set(arrowRow, arrowCol, Tile.FREE);

                    if (value < best) {
                        best = value;
                    }
                    if (value < beta) {
                        beta = value;
                    }
                    if (alpha >= beta) {
                        board.set(moveRow, moveCol, Tile.FREE);
                        board.set(queenRow, queenCol, currentQueen);
                        return best;
                    }
                }

                board.set(moveRow, moveCol, Tile.FREE);
            }

            board.set(queenRow, queenCol, currentQueen);
        }

        return best;
    }
}
