package ubc.cosc322.Tree;

import ubc.cosc322.Heuristic.MinDistHeuristic;
import java.util.*;

/**
 * Hybrid search: MCTS + Alpha-Beta Pruning.
 * Corrected naming conventions and utility method calls.
 */
public class MonteCarloAlphaBeta {
  private final Random random = new Random();
  private final int SIMULATION_LIMIT = 1000; // Reduced from 5000 to prevent OutOfMemoryError
  private final double EXPLORATION_CONSTANT = Math.sqrt(2);
  private static final int BOUND = 10;
  
  private static final int[][] DIRECTIONS = {
    {-1, 0}, {1, 0}, {0, -1}, {0, 1}, 
    {-1, -1}, {-1, 1}, {1, -1}, {1, 1}
  };

  private static boolean isWithinBoundary(int x, int y){
    // Returns true if x,y coordinates' values are from 0-9 
    return (x >= 0) && (x < BOUND) && (y >= 0) && (y < BOUND);
  }

  private boolean hasNoValidMoves(boolean isWhiteQueen, int[][] gameBoard) {
    // Use the existing generatePossibleMoves logic: 
    // If the list is empty, the player has no moves.
    return generatePossibleMoves(isWhiteQueen, gameBoard).isEmpty();
  }

  // Inside MonteCarloAlphaBeta.java

public int[][] performSearch(boolean isWhiteQueen, int[][] gameBoard) {
    if (hasNoValidMoves(isWhiteQueen, gameBoard)) {
        return null; 
    }

    // NEW: Count arrows to determine phase
    int arrowCount = 0;
    for(int[] row : gameBoard) {
        for(int cell : row) if(cell == 3) arrowCount++;
    }

    // Only switch to Alpha-Beta if the game is in the "end" phase (many arrows)
    if (arrowCount >= 45 && isPlayersSeparated(gameBoard)) {
        System.out.println("> SEPARATION DETECTED: Switching to Alpha-Beta Solver.");
        // Reduce depth to 2 to ensure we don't timeout
        int[][] abMove = alphaBetaSearch(gameBoard, 2, Integer.MIN_VALUE, Integer.MAX_VALUE, true, isWhiteQueen);
        printMoveDetails("Alpha-Beta", abMove);
        return abMove;
    }

    // MID-GAME/OPENING: Use MCTS
    System.out.println("> Running MCTS.");
    Node root = new Node(null, null, gameBoard, isWhiteQueen);
    for (int i = 0; i < SIMULATION_LIMIT; i++) {
        Node node = selectNode(root);
        if (!node.isFullyExpanded) node = expandNode(node);
        int result = simulatePlayout(node);
        backpropagateResult(node, result);
    }
    return getBestMoveFromNode(root);
  }

  // Print method for performSearch() to help debug
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
    List<int[][]> moves = generatePossibleMoves(isWhite, gameBoard);
    if (moves.isEmpty()) return null;

    int[][] bestMove = null;
    int bestValue = maxPlayer ? Integer.MIN_VALUE : Integer.MAX_VALUE;

    for (int[][] move : moves) {
      int val = minimax(applyMove(gameBoard, move), depth - 1, alpha, beta, !maxPlayer, !isWhite);
      if (maxPlayer) {
        if (val > bestValue) { bestValue = val; bestMove = move; }
        alpha = Math.max(alpha, bestValue);
      } else {
        if (val < bestValue) { bestValue = val; bestMove = move; }
        beta = Math.min(beta, bestValue);
      }
      if (beta <= alpha) break;
    }
    return bestMove;
  }

  private int minimax(int[][] gameBoard, int depth, int alpha, int beta, boolean maxPlayer, boolean isWhite) {
    if (depth == 0) return MinDistHeuristic.evalGameBoard(gameBoard, isWhite);
    
    List<int[][]> moves = generatePossibleMoves(isWhite, gameBoard);
    if (moves.isEmpty()) return maxPlayer ? Integer.MIN_VALUE + 1 : Integer.MAX_VALUE - 1;

    int value = maxPlayer ? Integer.MIN_VALUE : Integer.MAX_VALUE;
    for (int[][] move : moves) {
      int res = minimax(applyMove(gameBoard, move), depth - 1, alpha, beta, !maxPlayer, !isWhite);
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

  private Node selectNode(Node node) {
    while (!node.children.isEmpty()) {
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

  private Node expandNode(Node node) {
    List<int[][]> moves = generatePossibleMoves(node.isWhiteQueen, node.gameBoard);
    for (int[][] move : moves) {
      node.children.add(new Node(node, move, applyMove(node.gameBoard, move), !node.isWhiteQueen));
    }
    node.isFullyExpanded = true;
    return node.children.isEmpty() ? node : node.children.get(random.nextInt(node.children.size()));
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
  // Manually copy the 2D array
  int[][] nextBoard = new int[10][10];
  for (int i = 0; i < 10; i++) {
    for (int j = 0; j < 10; j++) {
        nextBoard[i][j] = gameBoard[i][j];
    }
  }

  // Indices: move[0] = old position, move[1] = new position, move[2] = arrow
  int qY = move[0][0], qX = move[0][1];
  int nY = move[1][0], nX = move[1][1];
  int aY = move[2][0], aX = move[2][1];

  int queenType = nextBoard[qY][qX]; // Is it 1 or 2?
  nextBoard[qY][qX] = 0;             // Remove from old
  nextBoard[nY][nX] = queenType;     // Place at new
  nextBoard[aY][aX] = 3;             // Place arrow

  return nextBoard;
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
}