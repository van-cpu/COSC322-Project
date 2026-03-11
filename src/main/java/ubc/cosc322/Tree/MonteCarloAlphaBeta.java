package ubc.cosc322.Tree;

import ubc.cosc322.Heuristic.MinDistHeuristic;
import java.util.*;

/**
 * Hybrid search: MCTS + Alpha-Beta Pruning.
 * White == 1, Black == 2, Arrow == 3  (per COSC322Test board encoding)
 */
public class MonteCarloAlphaBeta {
  private final Random random = new Random();
  private final int SIMULATION_LIMIT = 5000;
  private final double EXPLORATION_CONSTANT = Math.sqrt(2);
  private static final int BOUND = 10;

  // Cap on moves considered per node to prevent memory explosion in open positions
  private static final int MAX_MOVES_PER_NODE = 30;
  // Depth for random rollout during MCTS simulation
  private static final int ROLLOUT_DEPTH = 12;

  private static final int[][] DIRECTIONS = {
    {-1, 0}, {1, 0}, {0, -1}, {0, 1},
    {-1, -1}, {-1, 1}, {1, -1}, {1, 1}
  };

  private static boolean isWithinBoundary(int x, int y) {
    return (x >= 0) && (x < BOUND) && (y >= 0) && (y < BOUND);
  }

  private boolean hasNoValidMoves(boolean isWhiteQueen, int[][] gameBoard) {
    return generatePossibleMoves(isWhiteQueen, gameBoard).isEmpty();
  }

  public int[][] performSearch(boolean isWhiteQueen, int[][] gameBoard) {
    if (hasNoValidMoves(isWhiteQueen, gameBoard)) return null;

    int arrowCount = 0;
    for (int[] row : gameBoard) for (int cell : row) if (cell == 3) arrowCount++;

    if (arrowCount >= 45 && isPlayersSeparated(gameBoard, isWhiteQueen)) {
      System.out.println("> Switching to Endgame Alpha-Beta (separated regions).");
      // Use deeper search + endgame heuristic once regions are separated
      return alphaBetaSearch(gameBoard, 4, Integer.MIN_VALUE, Integer.MAX_VALUE, true, isWhiteQueen, true);
    }

    System.out.println("> Running MCTS.");
    Node root = new Node(null, null, gameBoard, isWhiteQueen);
    root.untriedMoves = generatePossibleMoves(isWhiteQueen, gameBoard);
    Collections.shuffle(root.untriedMoves, random);
    if (root.untriedMoves.size() > MAX_MOVES_PER_NODE) {
      root.untriedMoves = new ArrayList<>(root.untriedMoves.subList(0, MAX_MOVES_PER_NODE));
    }

    for (int i = 0; i < SIMULATION_LIMIT; i++) {
      Node node = selectNode(root);
      node = expandNode(node);
      int result = simulatePlayout(node, isWhiteQueen);
      backpropagateResult(node, result);
    }
    return getBestMoveFromNode(root);
  }

  /**
   * BFS from current player's queens; checks if opponent queens are reachable.
   * Returns true (separated) only if NO opponent queen is reachable.
   * White == 1, Black == 2
   */
  private boolean isPlayersSeparated(int[][] gameBoard, boolean isWhiteQueen) {
    int myType  = isWhiteQueen ? 1 : 2;
    int oppType = isWhiteQueen ? 2 : 1;

    int[][] dist = new int[10][10];
    for (int i = 0; i < 10; i++) Arrays.fill(dist[i], Integer.MAX_VALUE);

    Queue<int[]> queue = new LinkedList<>();
    for (int i = 0; i < 10; i++) {
      for (int j = 0; j < 10; j++) {
        if (gameBoard[i][j] == myType) {
          queue.add(new int[]{i, j, 0});   // {row, col, dist}
          dist[i][j] = 0;
        }
      }
    }

    while (!queue.isEmpty()) {
      int[] curr = queue.poll();
      // curr[0]=row(Y), curr[1]=col(X)
      // DIRECTIONS: {dx, dy} = {col_delta, row_delta}
      for (int[] dir : DIRECTIONS) {
        int nX = curr[1] + dir[0];
        int nY = curr[0] + dir[1];
        while (isWithinBoundary(nX, nY) && gameBoard[nY][nX] == 0) {
          if (dist[nY][nX] == Integer.MAX_VALUE) {
            dist[nY][nX] = curr[2] + 1;
            queue.add(new int[]{nY, nX, curr[2] + 1});
          }
          nX += dir[0];
          nY += dir[1];
        }
      }
    }

    for (int i = 0; i < 10; i++) {
      for (int j = 0; j < 10; j++) {
        if (gameBoard[i][j] == oppType && dist[i][j] != Integer.MAX_VALUE) return false;
      }
    }
    return true;
  }

  /**
   * @param useEndgameHeuristic  when true, use evalEndgameMobility (territory count)
   *                             instead of the general heuristic. This prevents the
   *                             "waste moves by shooting arrow on own vacated square"
   *                             pattern because territory count correctly rewards
   *                             moves that block opponent squares.
   */
  private int[][] alphaBetaSearch(int[][] gameBoard, int depth, int alpha, int beta,
                                   boolean maxPlayer, boolean isWhite,
                                   boolean useEndgameHeuristic) {
    List<int[]> moves = generatePossibleMovesFlat(isWhite, gameBoard);
    if (moves.isEmpty()) return null;

    int[] bestMove = null;
    int bestValue = maxPlayer ? Integer.MIN_VALUE : Integer.MAX_VALUE;
    int qType = isWhite ? 1 : 2;

    for (int[] move : moves) {
      applyMove(gameBoard, move, qType);
      int val = minimax(gameBoard, depth - 1, alpha, beta, !maxPlayer, !isWhite, useEndgameHeuristic);
      unmakeMove(gameBoard, move);

      if (maxPlayer) {
        if (val > bestValue) { bestValue = val; bestMove = move; }
        alpha = Math.max(alpha, bestValue);
      } else {
        if (val < bestValue) { bestValue = val; bestMove = move; }
        beta = Math.min(beta, bestValue);
      }
      if (beta <= alpha) break;
    }
    return (bestMove != null)
        ? new int[][]{{bestMove[0], bestMove[1]}, {bestMove[2], bestMove[3]}, {bestMove[4], bestMove[5]}}
        : null;
  }

  private int minimax(int[][] gameBoard, int depth, int alpha, int beta,
                       boolean maxPlayer, boolean isWhite, boolean useEndgameHeuristic) {
    if (depth == 0) {
      return useEndgameHeuristic
          ? MinDistHeuristic.evalEndgameMobility(gameBoard, isWhite)
          : MinDistHeuristic.evalGameBoard(gameBoard, isWhite);
    }

    List<int[]> moves = generatePossibleMovesFlat(isWhite, gameBoard);
    if (moves.isEmpty()) {
      // Terminal: current player has no moves and loses
      // Return extreme value from root player's perspective
      // isWhite here is the current player; if current player == root player, that's a loss
      return maxPlayer ? Integer.MIN_VALUE + 1 : Integer.MAX_VALUE - 1;
    }

    int value = maxPlayer ? Integer.MIN_VALUE : Integer.MAX_VALUE;
    int qType = isWhite ? 1 : 2;

    for (int[] move : moves) {
      applyMove(gameBoard, move, qType);
      int res = minimax(gameBoard, depth - 1, alpha, beta, !maxPlayer, !isWhite, useEndgameHeuristic);
      unmakeMove(gameBoard, move);

      if (maxPlayer) {
        value = Math.max(value, res);
        alpha = Math.max(alpha, value);
      } else {
        value = Math.min(value, res);
        beta = Math.min(beta, value);
      }
      if (beta <= alpha) break;
    }
    return value;
  }

  private List<int[]> generatePossibleMovesFlat(boolean isWhite, int[][] gameBoard) {
    List<int[]> moves = new ArrayList<>();
    int qType = isWhite ? 1 : 2;
    for (int i = 0; i < 10; i++) {
      for (int j = 0; j < 10; j++) {
        if (gameBoard[i][j] == qType) {
          for (int[] d1 : DIRECTIONS) {
            int nY = i + d1[1], nX = j + d1[0];
            while (isWithinBoundary(nX, nY) && gameBoard[nY][nX] == 0) {
              for (int[] d2 : DIRECTIONS) {
                int aY = nY + d2[1], aX = nX + d2[0];
                while (isWithinBoundary(aX, aY)) {
                  int cell = gameBoard[aY][aX];
                  boolean isOldSquare = (aY == i && aX == j);
                  boolean isNewSquare = (aY == nY && aX == nX);
                  if (cell != 0 && !isOldSquare) break;
                  if (!isNewSquare) {
                    moves.add(new int[]{i, j, nY, nX, aY, aX});
                  }
                  aX += d2[0]; aY += d2[1];
                }
              }
              nX += d1[0]; nY += d1[1];
            }
          }
        }
      }
    }
    return moves;
  }

  /**
   * UCT selection: descend the tree following the best UCT child.
   * Stops when we find a node that still has untried moves.
   */
  private Node selectNode(Node node) {
    while (node.untriedMoves != null && node.untriedMoves.isEmpty() && !node.children.isEmpty()) {
      Node bestChild = null;
      double bestUCT = Double.NEGATIVE_INFINITY;
      for (Node child : node.children) {
        double uct = ((double) child.winCount / (child.visitCount + 1))
            + EXPLORATION_CONSTANT * Math.sqrt(Math.log(node.visitCount + 1) / (child.visitCount + 1));
        if (uct > bestUCT) {
          bestUCT = uct;
          bestChild = child;
        }
      }
      if (bestChild == null) break;
      node = bestChild;
    }
    return node;
  }

  /**
   * LAZY expansion: pop exactly ONE untried move and create ONE child node.
   */
  private Node expandNode(Node node) {
    if (node.untriedMoves == null) {
      node.untriedMoves = generatePossibleMoves(node.isWhiteQueen, node.gameBoard);
      Collections.shuffle(node.untriedMoves, random);
      if (node.untriedMoves.size() > MAX_MOVES_PER_NODE) {
        node.untriedMoves = new ArrayList<>(node.untriedMoves.subList(0, MAX_MOVES_PER_NODE));
      }
    }

    if (node.untriedMoves.isEmpty()) return node;

    int[][] move = node.untriedMoves.remove(node.untriedMoves.size() - 1);
    int[][] nextBoard = applyMove(node.gameBoard, move);
    Node child = new Node(node, move, nextBoard, !node.isWhiteQueen);
    child.untriedMoves = new ArrayList<>();
    node.children.add(child);
    return child;
  }

  /**
   * Proper random rollout: play ROLLOUT_DEPTH random half-moves and evaluate leaf.
   */
  private int simulatePlayout(Node node, boolean rootIsWhite) {
    int[][] board = copyBoard(node.gameBoard);
    boolean currentIsWhite = node.isWhiteQueen;

    for (int depth = 0; depth < ROLLOUT_DEPTH; depth++) {
      List<int[][]> moves = generatePossibleMoves(currentIsWhite, board);
      if (moves.isEmpty()) {
        // Current player trapped → they lose
        boolean rootWins = (currentIsWhite != rootIsWhite);
        return rootWins ? 1 : 0;
      }
      int[][] move = moves.get(random.nextInt(Math.min(moves.size(), 10)));
      int qY = move[0][0], qX = move[0][1];
      int nY = move[1][0], nX = move[1][1];
      int aY = move[2][0], aX = move[2][1];
      int queenType = board[qY][qX];
      board[qY][qX] = 0;
      board[nY][nX] = queenType;
      board[aY][aX] = 3;
      currentIsWhite = !currentIsWhite;
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
    Node bestNode = null;
    int mostVisits = -1;
    for (Node current : root.children) {
      if (current.visitCount > mostVisits) {
        mostVisits = current.visitCount;
        bestNode = current;
      }
    }
    return (bestNode != null) ? bestNode.move : null;
  }

  /** Copies the board and applies a move. Does NOT modify the input board. */
  private int[][] applyMove(int[][] gameBoard, int[][] move) {
    int[][] nextBoard = copyBoard(gameBoard);
    int qY = move[0][0], qX = move[0][1];
    int nY = move[1][0], nX = move[1][1];
    int aY = move[2][0], aX = move[2][1];
    int queenType = nextBoard[qY][qX];
    nextBoard[qY][qX] = 0;
    nextBoard[nY][nX] = queenType;
    // Safety: never overwrite a queen with an arrow
    int arrowTarget = nextBoard[aY][aX];
    if (arrowTarget == 1 || arrowTarget == 2) {
      System.err.println("WARNING: applyMove (MCTS) tried to overwrite queen at ["
          + aY + "," + aX + "] with arrow! Move rejected.");
      return nextBoard;
    }
    nextBoard[aY][aX] = 3;
    return nextBoard;
  }

  private int[][] copyBoard(int[][] gameBoard) {
    int[][] copy = new int[10][10];
    for (int i = 0; i < 10; i++) copy[i] = Arrays.copyOf(gameBoard[i], 10);
    return copy;
  }

  private List<int[][]> generatePossibleMoves(boolean isWhite, int[][] gameBoard) {
    List<int[][]> moves = new ArrayList<>();
    int qType = isWhite ? 1 : 2;
    for (int i = 0; i < 10; i++) {
      for (int j = 0; j < 10; j++) {
        if (gameBoard[i][j] == qType) {
          for (int[] d1 : DIRECTIONS) {
            int nY = i + d1[1], nX = j + d1[0];
            while (isWithinBoundary(nX, nY) && gameBoard[nY][nX] == 0) {
              // Arrow fires from (nY,nX). It may pass through the queen's OLD square (i,j)
              // because that square is now empty. But it CANNOT land on (nY,nX) itself
              // (the queen is standing there) and cannot land on (i,j) — old square is
              // already vacated, which is valid to land on.
              for (int[] d2 : DIRECTIONS) {
                int aY = nY + d2[1], aX = nX + d2[0];
                while (isWithinBoundary(aX, aY)) {
                  int cell = gameBoard[aY][aX];
                  boolean isOldQueenSquare = (aY == i && aX == j);
                  boolean isNewQueenSquare = (aY == nY && aX == nX);
                  // Arrow can pass through old square, but cannot pass through anything else non-empty
                  // Arrow cannot land on new queen square
                  if (cell != 0 && !isOldQueenSquare) break; // blocked by obstacle
                  if (!isNewQueenSquare) {
                    moves.add(new int[][]{{i, j}, {nY, nX}, {aY, aX}});
                  }
                  aX += d2[0]; aY += d2[1];
                }
              }
              nX += d1[0]; nY += d1[1];
            }
          }
        }
      }
    }
    return moves;
  }

  // In-place apply for Alpha-Beta (unmakeMove restores the board)
  private void applyMove(int[][] board, int[] move, int queenType) {
    board[move[0]][move[1]] = 0;
    board[move[2]][move[3]] = queenType;
    // Safety: never overwrite a queen with an arrow
    int arrowTarget = board[move[4]][move[5]];
    if (arrowTarget == 1 || arrowTarget == 2) {
      System.err.println("WARNING: applyMove tried to overwrite queen at ["
          + move[4] + "," + move[5] + "] with arrow! Move rejected.");
      return;
    }
    board[move[4]][move[5]] = 3;
  }

  private void unmakeMove(int[][] board, int[] move) {
    int queenType = board[move[2]][move[3]];
    board[move[2]][move[3]] = 0;
    board[move[0]][move[1]] = queenType;
    board[move[4]][move[5]] = 0;
  }
}