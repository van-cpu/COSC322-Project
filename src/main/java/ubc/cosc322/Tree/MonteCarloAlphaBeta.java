package ubc.cosc322.Tree;

import ubc.cosc322.Heuristic.MinDistHeuristic;
import java.util.*;

/**
 * Hybrid search: MCTS + Alpha-Beta Pruning.
 * Corrected naming conventions and utility method calls.
 */
public class MonteCarloAlphaBeta {
  private final Random random = new Random();
  private final int SIMULATION_LIMIT = 5000;
  private final double EXPLORATION_CONSTANT = Math.sqrt(2);
  private static final int BOUND = 10;

  // Cap on moves considered per node to prevent memory explosion in open positions
  private static final int MAX_MOVES_PER_NODE = 30;
  
  private static final int[][] DIRECTIONS = {
    {-1, 0}, {1, 0}, {0, -1}, {0, 1}, 
    {-1, -1}, {-1, 1}, {1, -1}, {1, 1}
  };

  private static boolean isWithinBoundary(int x, int y){
    return (x >= 0) && (x < BOUND) && (y >= 0) && (y < BOUND);
  }

  private boolean hasNoValidMoves(boolean isWhiteQueen, int[][] gameBoard) {
    return generatePossibleMoves(isWhiteQueen, gameBoard).isEmpty();
  }

  public int[][] performSearch(boolean isWhiteQueen, int[][] gameBoard) {
    if (hasNoValidMoves(isWhiteQueen, gameBoard)) return null;

    int arrowCount = 0;
    for(int[] row : gameBoard) for(int cell : row) if(cell == 3) arrowCount++;

    if (arrowCount >= 45 && isPlayersSeparated(gameBoard)) {
      System.out.println("> Switching to Memory-Efficient Alpha-Beta.");
      return alphaBetaSearch(gameBoard, 2, Integer.MIN_VALUE, Integer.MAX_VALUE, true, isWhiteQueen);
    }

    System.out.println("> Running MCTS.");
    Node root = new Node(null, null, gameBoard, isWhiteQueen);
    // Pre-generate and store the unvisited moves list on the root node
    root.untriedMoves = generatePossibleMoves(isWhiteQueen, gameBoard);
    Collections.shuffle(root.untriedMoves, random);
    // Cap moves to avoid memory explosion in wide-open early game positions
    if (root.untriedMoves.size() > MAX_MOVES_PER_NODE) {
      root.untriedMoves = root.untriedMoves.subList(0, MAX_MOVES_PER_NODE);
    }

    for (int i = 0; i < SIMULATION_LIMIT; i++) {
      Node node = selectNode(root);
      node = expandNode(node);   // Lazy: expands exactly ONE child per call
      int result = simulatePlayout(node);
      backpropagateResult(node, result);
    }
    return getBestMoveFromNode(root);
  }

  private void printMoveDetails(String strategy, int[][] move) {
    if (move != null) {
      System.out.println(">>> [" + strategy + "] Best Move Found:");
      System.out.println("    Queen: [" + move[0][0] + "," + move[0][1] + "] -> [" + move[1][0] + "," + move[1][1] + "]");
      System.out.println("    Arrow: [" + move[2][0] + "," + move[2][1] + "]");
    }
  }

  private boolean isPlayersSeparated(int[][] gameBoard) {
    int[][] distToWhite = new int[10][10];
    for (int i = 0; i < 10; i++){
      Arrays.fill(distToWhite[i], Integer.MAX_VALUE);
    }
    
    Queue<int[]> queue = new LinkedList<>();
    for (int i = 0; i < 10; i++) {
      for (int j = 0; j < 10; j++) {
        if (gameBoard[i][j] == 2) { 
          queue.add(new int[]{i, j, 0});
          distToWhite[i][j] = 0;
        }
      }
    }
    
    while(!queue.isEmpty()){
      int[] curr = queue.poll();
      for(int[] dir : DIRECTIONS){
        int nX = curr[1] + dir[0], nY = curr[0] + dir[1];
        while(isWithinBoundary(nX, nY) && gameBoard[nY][nX] == 0){
          if(distToWhite[nY][nX] == Integer.MAX_VALUE){
            distToWhite[nY][nX] = curr[2] + 1;
            queue.add(new int[]{nY, nX, curr[2] + 1});
          }
          nX += dir[0]; nY += dir[1];
        }
      }
    }

    for (int i = 0; i < 10; i++) {
      for (int j = 0; j < 10; j++) {
        if (gameBoard[i][j] == 1 && distToWhite[i][j] != Integer.MAX_VALUE) return false;
      }
    }
    return true;
  }

  private int[][] alphaBetaSearch(int[][] gameBoard, int depth, int alpha, int beta, boolean maxPlayer, boolean isWhite) {
    List<int[]> moves = generatePossibleMovesFlat(isWhite, gameBoard);
    if (moves.isEmpty()) return null;

    int[] bestMove = null;
    int bestValue = maxPlayer ? Integer.MIN_VALUE : Integer.MAX_VALUE;
    int qType = isWhite ? 2 : 1;

    for (int[] move : moves) {
      applyMove(gameBoard, move, qType);
      int val = minimax(gameBoard, depth - 1, alpha, beta, !maxPlayer, !isWhite);
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
    return (bestMove != null) ? new int[][]{{bestMove[0], bestMove[1]}, {bestMove[2], bestMove[3]}, {bestMove[4], bestMove[5]}} : null;
  }

  private int minimax(int[][] gameBoard, int depth, int alpha, int beta, boolean maxPlayer, boolean isWhite) {
    if (depth == 0) return MinDistHeuristic.evalGameBoard(gameBoard, isWhite);
    
    List<int[]> moves = generatePossibleMovesFlat(isWhite, gameBoard);
    if (moves.isEmpty()) return maxPlayer ? Integer.MIN_VALUE + 1 : Integer.MAX_VALUE - 1;

    int value = maxPlayer ? Integer.MIN_VALUE : Integer.MAX_VALUE;
    int qType = isWhite ? 2 : 1;

    for (int[] move : moves) {
      applyMove(gameBoard, move, qType);
      int res = minimax(gameBoard, depth - 1, alpha, beta, !maxPlayer, !isWhite);
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
    int qType = isWhite ? 2 : 1;
    for (int i = 0; i < 10; i++) {
      for (int j = 0; j < 10; j++) {
        if (gameBoard[i][j] == qType) {
          for (int[] d1 : DIRECTIONS) {
            int nY = i + d1[1], nX = j + d1[0];
            while (isWithinBoundary(nX, nY) && gameBoard[nY][nX] == 0) {
              for (int[] d2 : DIRECTIONS) {
                int aY = nY + d2[1], aX = nX + d2[0];
                while (isWithinBoundary(aX, aY) && (gameBoard[aY][aX] == 0 || (aY == i && aX == j))) {
                  moves.add(new int[]{i, j, nY, nX, aY, aX});
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
   * UCT selection: traverse down the tree picking the best UCT child.
   * Stops when we reach a node that still has untried moves (not fully expanded).
   */
  private Node selectNode(Node node) {
    while (node.untriedMoves != null && node.untriedMoves.isEmpty() && !node.children.isEmpty()) {
      Node bestChild = null;
      double bestUCT = Double.NEGATIVE_INFINITY;
      for (Node child : node.children) {
        double uct = ((double) child.winCount / (child.visitCount + 1)) + 
                     EXPLORATION_CONSTANT * Math.sqrt(Math.log(node.visitCount + 1) / (child.visitCount + 1));
        if (uct > bestUCT) { 
          bestUCT = uct; 
          bestChild = child; 
        }
      }
      node = bestChild;
    }
    return node;
  }

  /**
   * LAZY expansion: pop exactly ONE untried move and create ONE child node.
   * This is the key fix — the original code created ALL children at once,
   * each with a full board copy, which caused the OutOfMemoryError.
   */
  private Node expandNode(Node node) {
    // If this node has never been initialised with untried moves, do it now
    if (node.untriedMoves == null) {
      node.untriedMoves = generatePossibleMoves(node.isWhiteQueen, node.gameBoard);
      Collections.shuffle(node.untriedMoves, random);
      // Cap to prevent explosion in wide-open positions
      if (node.untriedMoves.size() > MAX_MOVES_PER_NODE) {
        node.untriedMoves = node.untriedMoves.subList(0, MAX_MOVES_PER_NODE);
      }
    }

    // If there are no moves at all, return this node as a terminal
    if (node.untriedMoves.isEmpty()) {
      return node;
    }

    // Pop one untried move and create exactly one child
    int[][] move = node.untriedMoves.remove(node.untriedMoves.size() - 1);
    int[][] nextBoard = applyMove(node.gameBoard, move);
    Node child = new Node(node, move, nextBoard, !node.isWhiteQueen);
    // Initialise child's untried list eagerly so selectNode can check it
    child.untriedMoves = new ArrayList<>(); // will be populated on first expandNode call
    node.children.add(child);
    return child;
  }

  private int simulatePlayout(Node node) {
    return MinDistHeuristic.evalGameBoard(node.gameBoard, node.isWhiteQueen) > 0 ? 1 : 0;
  }

  private void backpropagateResult(Node node, int result) {
    while (node != null) {
      node.visitCount++;
      node.winCount += result;
      node = node.parent;
    }
  }

  /**
   * Finds the child node with the most visits.
   */
  private int[][] getBestMoveFromNode(Node root) {
    Node bestNode = null;
    int mostVisits = -1;

    for (int i = 0; i < root.children.size(); i++) {
      Node current = root.children.get(i);
      if (current.visitCount > mostVisits) {
        mostVisits = current.visitCount;
        bestNode = current;
      }
    }
    return (bestNode != null) ? bestNode.move : null;
  }

  /**
   * Creates a copy of the board and applies the move (Queen move + Arrow shot).
   */
  private int[][] applyMove(int[][] gameBoard, int[][] move) {
    int[][] nextBoard = new int[10][10];
    for (int i = 0; i < 10; i++) {
      for (int j = 0; j < 10; j++) {
        nextBoard[i][j] = gameBoard[i][j];
      }
    }

    int qY = move[0][0], qX = move[0][1];
    int nY = move[1][0], nX = move[1][1];
    int aY = move[2][0], aX = move[2][1];

    int queenType = nextBoard[qY][qX];
    nextBoard[qY][qX] = 0;
    nextBoard[nY][nX] = queenType;
    nextBoard[aY][aX] = 3;

    return nextBoard;
  }

  private List<int[][]> generatePossibleMoves(boolean isWhite, int[][] gameBoard) {
    List<int[][]> moves = new ArrayList<>();
    int qType = isWhite ? 2 : 1;
    for (int i = 0; i < 10; i++) {
      for (int j = 0; j < 10; j++) {
        if (gameBoard[i][j] == qType) {
          for (int[] d1 : DIRECTIONS) {
            int nY = i + d1[1], nX = j + d1[0];
            while (isWithinBoundary(nX, nY) && gameBoard[nY][nX] == 0) {
              for (int[] d2 : DIRECTIONS) {
                int aY = nY + d2[1], aX = nX + d2[0];
                while (isWithinBoundary(aX, aY) && (gameBoard[aY][aX] == 0 || (aY == i && aX == j))) {
                  moves.add(new int[][]{{i, j}, {nY, nX}, {aY, aX}});
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
  
  private void applyMove(int[][] board, int[] move, int queenType) {
    board[move[0]][move[1]] = 0;
    board[move[2]][move[3]] = queenType;
    board[move[4]][move[5]] = 3;
  }

  private void unmakeMove(int[][] board, int[] move) {
    int queenType = board[move[2]][move[3]];
    board[move[2]][move[3]] = 0;
    board[move[0]][move[1]] = queenType;
    board[move[4]][move[5]] = 0;
  }
}