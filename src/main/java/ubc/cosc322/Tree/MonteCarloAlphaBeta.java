package ubc.cosc322.Tree;

import ubc.cosc322.Heuristic.MinDistHeuristic;
import java.util.*;

/**
 * Hybrid search: time-bounded MCTS + Alpha-Beta Pruning.
 *
 * Board encoding: White=1, Black=2, Arrow=3, Empty=0
 * gameBoard[row][col] == gameBoard[Y][X]
 * DIRECTIONS: {dx=col_delta, dy=row_delta}
 *
 * Arrow generation rule (Game of Amazons):
 *   After queen moves from (qRow,qCol) → (nRow,nCol), the arrow is fired
 *   from (nRow,nCol) in any queen-move direction. The arrow slides until
 *   it hits a non-empty cell or the boundary. The only exception is the
 *   queen's OLD square (qRow,qCol) which is now empty and may be passed
 *   through OR landed on. The queen's NEW square (nRow,nCol) blocks the
 *   arrow like any other occupied square — the arrow starts one step away
 *   so it can never land there.
 */
public class MonteCarloAlphaBeta {

  private final Random random = new Random();

  // Time budget per move (milliseconds). Leave 1 s margin for overhead.
  private static final long TIME_LIMIT_MS = 29_000;

  // Cap moves per MCTS node to prevent memory explosion in open positions
  private static final int MAX_MOVES_PER_NODE = 30;

  // Depth for random rollout during MCTS simulation phase
  private static final int ROLLOUT_DEPTH = 6;

  private static final double EXPLORATION_CONSTANT = Math.sqrt(2);
  private static final int BOUND = 10;

  // Alpha-Beta depth: 4 ply for endgame (narrow boards are fast)
  private static final int AB_DEPTH = 4;

  private static final int[][] DIRECTIONS = {
    {-1, 0}, {1, 0}, {0, -1}, {0, 1},
    {-1, -1}, {-1, 1}, {1, -1}, {1, 1}
  };

  // ── Public entry point ────────────────────────────────────────────────────

  public int[][] performSearch(boolean isWhiteQueen, int[][] gameBoard) {
    // Verify we have legal moves before spending time searching
    if (generatePossibleMoves(isWhiteQueen, gameBoard).isEmpty()) return null;

    int arrowCount = 0;
    for (int[] row : gameBoard) for (int cell : row) if (cell == 3) arrowCount++;

    // Switch to endgame Alpha-Beta once regions are separated
    if (arrowCount >= 45 && isPlayersSeparated(gameBoard, isWhiteQueen)) {
      System.out.println("> Switching to Endgame Alpha-Beta (separated regions).");
      return alphaBetaSearch(gameBoard, AB_DEPTH,
          Integer.MIN_VALUE, Integer.MAX_VALUE, true, isWhiteQueen, true);
    }

    System.out.println("> Running MCTS (time limit: " + (TIME_LIMIT_MS / 1000) + "s).");
    return runMCTS(isWhiteQueen, gameBoard);
  }

  // ── MCTS (time-bounded) ───────────────────────────────────────────────────

  private int[][] runMCTS(boolean isWhiteQueen, int[][] gameBoard) {
    Node root = new Node(null, null, gameBoard, isWhiteQueen);
    root.untriedMoves = generatePossibleMoves(isWhiteQueen, gameBoard);
    Collections.shuffle(root.untriedMoves, random);
    if (root.untriedMoves.size() > MAX_MOVES_PER_NODE)
      root.untriedMoves = new ArrayList<>(root.untriedMoves.subList(0, MAX_MOVES_PER_NODE));

    long deadline = System.currentTimeMillis() + TIME_LIMIT_MS;
    int iterations = 0;

    while (System.currentTimeMillis() < deadline) {
      Node node = selectNode(root);
      node = expandNode(node);
      int result = simulatePlayout(node, isWhiteQueen);
      backpropagateResult(node, result);
      iterations++;
    }

    System.out.println("> MCTS completed " + iterations + " iterations.");
    return getBestMoveFromNode(root);
  }

  // ── MCTS components ───────────────────────────────────────────────────────

  private Node selectNode(Node node) {
    while (node.untriedMoves != null && node.untriedMoves.isEmpty()
        && !node.children.isEmpty()) {
      Node best = null;
      double bestUCT = Double.NEGATIVE_INFINITY;
      for (Node child : node.children) {
        double uct = ((double) child.winCount / (child.visitCount + 1))
            + EXPLORATION_CONSTANT
            * Math.sqrt(Math.log(node.visitCount + 1) / (child.visitCount + 1));
        if (uct > bestUCT) { bestUCT = uct; best = child; }
      }
      if (best == null) break;
      node = best;
    }
    return node;
  }

  private Node expandNode(Node node) {
    if (node.untriedMoves == null) {
      node.untriedMoves = generatePossibleMoves(node.isWhiteQueen, node.gameBoard);
      Collections.shuffle(node.untriedMoves, random);
      if (node.untriedMoves.size() > MAX_MOVES_PER_NODE)
        node.untriedMoves = new ArrayList<>(node.untriedMoves.subList(0, MAX_MOVES_PER_NODE));
    }
    if (node.untriedMoves.isEmpty()) return node;

    int[][] move = node.untriedMoves.remove(node.untriedMoves.size() - 1);
    int[][] nextBoard = applyMoveCopy(node.gameBoard, move);
    Node child = new Node(node, move, nextBoard, !node.isWhiteQueen);
    child.untriedMoves = new ArrayList<>();
    node.children.add(child);
    return child;
  }

  private int simulatePlayout(Node node, boolean rootIsWhite) {
    int[][] board = copyBoard(node.gameBoard);
    boolean current = node.isWhiteQueen;
    for (int d = 0; d < ROLLOUT_DEPTH; d++) {
      List<int[][]> moves = generatePossibleMoves(current, board);
      if (moves.isEmpty()) {
        // Current player trapped → they lose
        return (current != rootIsWhite) ? 1 : 0;
      }
      int[][] move = moves.get(random.nextInt(Math.min(moves.size(), 10)));
      int qY = move[0][0], qX = move[0][1];
      int nY = move[1][0], nX = move[1][1];
      int aY = move[2][0], aX = move[2][1];
      int qt = board[qY][qX];
      board[qY][qX] = 0;
      board[nY][nX] = qt;
      board[aY][aX] = 3;
      current = !current;
    }
    int score = MinDistHeuristic.evalGameBoard(board, rootIsWhite);
    return score > 0 ? 1 : 0;
  }

  private void backpropagateResult(Node node, int result) {
    while (node != null) {
      node.visitCount++;
      node.winCount += result;
      node = node.parent;
    }
  }

  private int[][] getBestMoveFromNode(Node root) {
    Node best = null;
    int mostVisits = -1;
    for (Node c : root.children) {
      if (c.visitCount > mostVisits) { mostVisits = c.visitCount; best = c; }
    }
    return (best != null) ? best.move : null;
  }

  // ── Alpha-Beta (endgame) ──────────────────────────────────────────────────

  private int[][] alphaBetaSearch(int[][] gameBoard, int depth, int alpha, int beta,
                                   boolean maxPlayer, boolean isWhite,
                                   boolean useEndgameHeuristic) {
    List<int[]> moves = generatePossibleMovesFlat(isWhite, gameBoard);
    if (moves.isEmpty()) return null;

    int[] bestMove = null;
    int bestVal = maxPlayer ? Integer.MIN_VALUE : Integer.MAX_VALUE;
    int qType   = isWhite ? 1 : 2;

    for (int[] move : moves) {
      applyMoveInPlace(gameBoard, move, qType);
      int val = minimax(gameBoard, depth - 1, alpha, beta,
          !maxPlayer, !isWhite, useEndgameHeuristic);
      unmakeMove(gameBoard, move);

      if (maxPlayer ? val > bestVal : val < bestVal) {
        bestVal  = val;
        bestMove = move;
      }
      if (maxPlayer) alpha = Math.max(alpha, bestVal);
      else           beta  = Math.min(beta,  bestVal);
      if (beta <= alpha) break;
    }
    return (bestMove == null) ? null
        : new int[][]{{bestMove[0], bestMove[1]},
                      {bestMove[2], bestMove[3]},
                      {bestMove[4], bestMove[5]}};
  }

  private int minimax(int[][] gameBoard, int depth, int alpha, int beta,
                       boolean maxPlayer, boolean isWhite,
                       boolean useEndgameHeuristic) {
    if (depth == 0) {
      return useEndgameHeuristic
          ? MinDistHeuristic.evalEndgameMobility(gameBoard, isWhite)
          : MinDistHeuristic.evalGameBoard(gameBoard, isWhite);
    }
    List<int[]> moves = generatePossibleMovesFlat(isWhite, gameBoard);
    if (moves.isEmpty())
      return maxPlayer ? Integer.MIN_VALUE + 1 : Integer.MAX_VALUE - 1;

    int value  = maxPlayer ? Integer.MIN_VALUE : Integer.MAX_VALUE;
    int qType  = isWhite ? 1 : 2;
    for (int[] move : moves) {
      applyMoveInPlace(gameBoard, move, qType);
      int res = minimax(gameBoard, depth - 1, alpha, beta,
          !maxPlayer, !isWhite, useEndgameHeuristic);
      unmakeMove(gameBoard, move);
      if (maxPlayer) { value = Math.max(value, res); alpha = Math.max(alpha, value); }
      else           { value = Math.min(value, res); beta  = Math.min(beta,  value); }
      if (beta <= alpha) break;
    }
    return value;
  }

  // ── Separation detection (BFS) ────────────────────────────────────────────

  private boolean isPlayersSeparated(int[][] gameBoard, boolean isWhiteQueen) {
    int myType  = isWhiteQueen ? 1 : 2;
    int oppType = isWhiteQueen ? 2 : 1;
    int[][] dist = new int[10][10];
    for (int[] row : dist) Arrays.fill(row, Integer.MAX_VALUE);

    Queue<int[]> queue = new LinkedList<>();
    for (int i = 0; i < 10; i++)
      for (int j = 0; j < 10; j++)
        if (gameBoard[i][j] == myType) { dist[i][j] = 0; queue.add(new int[]{i, j, 0}); }

    while (!queue.isEmpty()) {
      int[] cur = queue.poll();
      for (int[] dir : DIRECTIONS) {
        int nx = cur[1] + dir[0], ny = cur[0] + dir[1];
        while (isInBounds(nx, ny) && gameBoard[ny][nx] == 0) {
          if (dist[ny][nx] == Integer.MAX_VALUE) {
            dist[ny][nx] = cur[2] + 1;
            queue.add(new int[]{ny, nx, cur[2] + 1});
          }
          nx += dir[0]; ny += dir[1];
        }
      }
    }
    for (int i = 0; i < 10; i++)
      for (int j = 0; j < 10; j++)
        if (gameBoard[i][j] == oppType && dist[i][j] != Integer.MAX_VALUE) return false;
    return true;
  }

  // ── Move generation ───────────────────────────────────────────────────────

  /**
   * Generate all legal moves as {oldPos, newPos, arrowPos} int[3][2] triples.
   *
   * Arrow rule: fires from (nRow,nCol). Slides until hitting any non-empty
   * cell OR the boundary. The OLD queen square (qRow,qCol) is now empty so
   * the arrow may pass through it or land on it. The NEW queen square
   * (nRow,nCol) is occupied by the queen — the arrow loop starts one step
   * AWAY from (nRow,nCol) so it never visits that square.
   */
  private List<int[][]> generatePossibleMoves(boolean isWhite, int[][] board) {
    List<int[][]> moves = new ArrayList<>();
    int qType = isWhite ? 1 : 2;

    for (int qRow = 0; qRow < 10; qRow++) {
      for (int qCol = 0; qCol < 10; qCol++) {
        if (board[qRow][qCol] != qType) continue;

        // Queen slides in direction d1
        for (int[] d1 : DIRECTIONS) {
          int nRow = qRow + d1[1], nCol = qCol + d1[0];
          while (isInBounds(nCol, nRow) && board[nRow][nCol] == 0) {

            // Arrow fires from (nRow,nCol) in direction d2, starting one step away
            for (int[] d2 : DIRECTIONS) {
              int aRow = nRow + d2[1], aCol = nCol + d2[0];
              while (isInBounds(aCol, aRow)) {
                int cell = board[aRow][aCol];
                // Old queen square is empty → arrow may pass through or land here
                boolean isOldSquare = (aRow == qRow && aCol == qCol);
                if (cell != 0 && !isOldSquare) break; // blocked
                moves.add(new int[][]{{qRow, qCol}, {nRow, nCol}, {aRow, aCol}});
                aCol += d2[0]; aRow += d2[1];
              }
            }
            nCol += d1[0]; nRow += d1[1];
          }
        }
      }
    }
    return moves;
  }

  /** Flat int[6] version for Alpha-Beta: {qRow, qCol, nRow, nCol, aRow, aCol} */
  private List<int[]> generatePossibleMovesFlat(boolean isWhite, int[][] board) {
    List<int[]> moves = new ArrayList<>();
    int qType = isWhite ? 1 : 2;

    for (int qRow = 0; qRow < 10; qRow++) {
      for (int qCol = 0; qCol < 10; qCol++) {
        if (board[qRow][qCol] != qType) continue;

        for (int[] d1 : DIRECTIONS) {
          int nRow = qRow + d1[1], nCol = qCol + d1[0];
          while (isInBounds(nCol, nRow) && board[nRow][nCol] == 0) {

            for (int[] d2 : DIRECTIONS) {
              int aRow = nRow + d2[1], aCol = nCol + d2[0];
              while (isInBounds(aCol, aRow)) {
                int cell = board[aRow][aCol];
                boolean isOldSquare = (aRow == qRow && aCol == qCol);
                if (cell != 0 && !isOldSquare) break;
                moves.add(new int[]{qRow, qCol, nRow, nCol, aRow, aCol});
                aCol += d2[0]; aRow += d2[1];
              }
            }
            nCol += d1[0]; nRow += d1[1];
          }
        }
      }
    }
    return moves;
  }

  // ── Board manipulation ────────────────────────────────────────────────────

  /** Returns a new board with the move applied (used by MCTS). */
  private int[][] applyMoveCopy(int[][] board, int[][] move) {
    int[][] next = copyBoard(board);
    int qRow = move[0][0], qCol = move[0][1];
    int nRow = move[1][0], nCol = move[1][1];
    int aRow = move[2][0], aCol = move[2][1];
    int qt = next[qRow][qCol];
    next[qRow][qCol] = 0;
    next[nRow][nCol] = qt;
    // Safety guard
    if (next[aRow][aCol] == 1 || next[aRow][aCol] == 2) {
      System.err.println("WARNING: applyMoveCopy would overwrite queen at ["
          + aRow + "," + aCol + "]. Move skipped.");
      return next;
    }
    next[aRow][aCol] = 3;
    return next;
  }

  /** In-place apply for Alpha-Beta (pair with unmakeMove). */
  private void applyMoveInPlace(int[][] board, int[] move, int queenType) {
    board[move[0]][move[1]] = 0;
    board[move[2]][move[3]] = queenType;
    if (board[move[4]][move[5]] == 1 || board[move[4]][move[5]] == 2) {
      System.err.println("WARNING: applyMoveInPlace would overwrite queen at ["
          + move[4] + "," + move[5] + "]. Arrow skipped.");
      return;
    }
    board[move[4]][move[5]] = 3;
  }

  private void unmakeMove(int[][] board, int[] move) {
    int qt = board[move[2]][move[3]];
    board[move[2]][move[3]] = 0;
    board[move[0]][move[1]] = qt;
    board[move[4]][move[5]] = 0;
  }

  private int[][] copyBoard(int[][] board) {
    int[][] copy = new int[10][10];
    for (int i = 0; i < 10; i++) copy[i] = Arrays.copyOf(board[i], 10);
    return copy;
  }

  private static boolean isInBounds(int x, int y) {
    return x >= 0 && x < BOUND && y >= 0 && y < BOUND;
  }
}