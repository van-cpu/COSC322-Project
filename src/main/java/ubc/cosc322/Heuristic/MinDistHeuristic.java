package ubc.cosc322.Heuristic;

import java.util.*;

public class MinDistHeuristic {

  // Possible moving directions of the queen
  private static final int[][] DIRECTIONS = {
    {-1, 0}, {1, 0}, {0, -1}, {0, 1}, // Orthogonal
    {-1, -1}, {-1, 1}, {1, -1}, {1, 1} // Diagonal
  };

  // Boundary of the gameBoard
  private static final int BOUND = 10;

  private static boolean isWithinBoundary(int x, int y){
    return (x >= 0) && (x < BOUND) && (y >= 0) && (y < BOUND);
  }

  private static void breadthFirstSearch(Queue<int[]> toVisit, int[][] gameBoard, int[][] distances){
    while(!toVisit.isEmpty()){
      int[] current = toVisit.poll();
      int x = current[1];
      int y = current[0];
      int dist = current[2];

      for(int[] dir : DIRECTIONS){
        int newX = x + dir[0];
        int newY = y + dir[1];

        while(isWithinBoundary(newX, newY) && gameBoard[newY][newX] == 0){
          if(distances[newY][newX] > dist + 1){
            distances[newY][newX] = dist + 1;
            toVisit.add(new int[]{newY, newX, dist + 1});
          }
          newX += dir[0];
          newY += dir[1];
        }
      }
    }
  }

  private static int evalImminentTraps(int[][] gameBoard, int queenType){
    int trapPenalty = 0;

    for(int i = 0; i < BOUND; i++){
      for( int j = 0; j < BOUND; j++){
        if(gameBoard[i][j] == queenType){
          int mobility = 0;
          for(int [] dir : DIRECTIONS){
            int x = j + dir[0];
            int y = i + dir[1];
            while(isWithinBoundary(x, y) && gameBoard[y][x] == 0){
              mobility ++;
              x += dir[0];
              y += dir[1];
            }
          }
          if(mobility <= 3 && mobility > 0){
            trapPenalty += (4 - mobility) * 10;
          }
        }
      }
    }
    return -trapPenalty;
  }

  private static int evalQueenMobility(int[][] gameBoard, int queenType, String phase){
    int mobilityPoint = 0;
    int trapPenalty = -20;

    for(int i = 0; i < BOUND; i++){
      for(int j = 0; j < BOUND; j++){
        if(gameBoard[i][j] == queenType){
          int movingOptions = 0;
          for(int[] dir : DIRECTIONS){
            int x = j + dir[0];
            int y = i + dir[1];

            while(isWithinBoundary(x, y) && gameBoard[y][x] == 0){
              movingOptions++;
              x += dir[0];
              y += dir[1];
            }
          }
          if(movingOptions == 0){
            mobilityPoint += trapPenalty;
          }else{
            if(phase.equals("opening")){
              mobilityPoint += movingOptions;
            }else if(phase.equals("mid")){
              mobilityPoint += movingOptions * 2;
            }else{
              mobilityPoint += movingOptions * 3;
            }
          }
        }
      }
    }
    return mobilityPoint;
  }

  private static int evalOwnQueenSpacing(int[][] gameBoard, int queenType){
    List<int[]> queens = new ArrayList<>();
    int spacePenalty = 0;

    for(int i = 0; i < BOUND; i++){
      for(int j = 0; j < BOUND; j++){
        if(gameBoard[i][j] == queenType){
          queens.add(new int[]{i, j});
        }
      }
    }

    for(int i = 0; i < queens.size(); i++){
      for(int j = i + 1; j < queens.size(); j++){
        int[] q1 = queens.get(i);
        int[] q2 = queens.get(j);
        int distance = Math.abs(q1[0] - q2[0]) + Math.abs(q1[1] - q2[1]);
        if(distance <= 2){
          spacePenalty += 10;
        }else if(distance <= 3){
          spacePenalty += 5;
        }
      }
    }
    return -spacePenalty;
  }

  public static int evalQueenUrgency(int[] queen, int[][] gameBoard){
    int x = queen[1];
    int y = queen[0];
    int mobility = 0;

    for(int[] dir : DIRECTIONS){
      int dX = x + dir[0];
      int dY = y + dir[1];

      while(isWithinBoundary(dX, dY) && gameBoard[dY][dX] == 0){
        mobility++;
        dX += dir[0];
        dY += dir[1];
      }
    }
    return mobility;
  }

  private static int evalTrappingOpponentQueen(int[][] gameBoard, int opponentQueenType){
    int penaltyForEnemy = 0;

    for(int i = 0; i < BOUND; i++){
      for(int j = 0; j < BOUND; j++){
        if(gameBoard[i][j] == opponentQueenType){
          int mobility = 0;
          for(int[] dir : DIRECTIONS){
            int x = j + dir[0];
            int y = i + dir[1];
            while(isWithinBoundary(x, y) && gameBoard[y][x] == 0){
              mobility++;
              x += dir[0];
              y += dir[1];
            }
          }
          if (mobility <= 2){
            penaltyForEnemy += 10;
          }
        }
      }
    }
    return penaltyForEnemy;
  }

  public static int evalArrowCount(int[][] gameBoard, int row, int col){
    int arrowCount = 0;
    for(int[] dir : DIRECTIONS){
      int x = col + dir[0];
      int y = row + dir[1];

      if(isWithinBoundary(x, y) && gameBoard[y][x] == 3){
        arrowCount++;
      }
    }
    return arrowCount;
  }

  /**
   * Fix end game issue: counts reachable squares for each player using a
   * flood-fill (BFS treating the board like a simple graph of empty squares
   * reachable by queen moves).  The player with more reachable squares wins.
   *
   * This replaces the general heuristic once players are fully separated,
   * because territory count is the exact win condition at that point.
   * Each reachable square is worth +1 for its owner; the final score is
   * (myReachable - oppReachable) * a large weight so Alpha-Beta always
   * prefers moves that maximise territory over trivial self-preservation moves.
   */
  public static int evalEndgameMobility(int[][] gameBoard, boolean isWhiteQueen) {
    final int WHITE = 1;
    final int BLACK = 2;
    int myType  = isWhiteQueen ? WHITE : BLACK;
    int oppType = isWhiteQueen ? BLACK : WHITE;

    int myReachable  = countReachableSquares(gameBoard, myType);
    int oppReachable = countReachableSquares(gameBoard, oppType);

    // Large weight so this dominates any other term: surviving more squares = winning
    return (myReachable - oppReachable) * 100;
  }

  /**
   * BFS flood-fill from all queens of the given type.
   * Counts the total number of distinct empty squares reachable by queen moves.
   */
  private static int countReachableSquares(int[][] gameBoard, int queenType) {
    boolean[][] visited = new boolean[BOUND][BOUND];
    Queue<int[]> queue = new LinkedList<>();

    // Seed from every queen of this type
    for (int i = 0; i < BOUND; i++) {
      for (int j = 0; j < BOUND; j++) {
        if (gameBoard[i][j] == queenType) {
          // The queen's own square is not "empty" but we use it as a source
          visited[i][j] = true;
          queue.add(new int[]{i, j});
        }
      }
    }

    int count = 0;
    while (!queue.isEmpty()) {
      int[] curr = queue.poll();
      for (int[] dir : DIRECTIONS) {
        // dir[0]=dx(col), dir[1]=dy(row)
        int nX = curr[1] + dir[0];
        int nY = curr[0] + dir[1];
        while (isWithinBoundary(nX, nY) && gameBoard[nY][nX] == 0) {
          if (!visited[nY][nX]) {
            visited[nY][nX] = true;
            count++;
            queue.add(new int[]{nY, nX});
          }
          nX += dir[0];
          nY += dir[1];
        }
      }
    }
    return count;
  }

  public static int evalGameBoard(int[][]gameBoard, boolean isWhiteQueen){
    final int WHITE = 1;
    final int BLACK = 2;
    final int ARROW = 3;

    int blackQScore = 0;
    int whiteQScore = 0;
    Queue<int[]> toVisit = new LinkedList<>();
    int arrowCount = 0;
    String phase;
    int centerControlReward = 0;

    int[][] distToBlackQueen = new int[10][10];
    int[][] distToWhiteQueen = new int[10][10];

    for(int i = 0; i < BOUND; i++){
      Arrays.fill(distToBlackQueen[i], Integer.MAX_VALUE);
      Arrays.fill(distToWhiteQueen[i], Integer.MAX_VALUE);
    }

    // White queens BFS
    for(int i = 0; i < BOUND; i++){
      for (int j = 0; j < BOUND; j++){
        if(gameBoard[i][j] == WHITE){
          toVisit.add(new int[]{i, j, 0});
          distToWhiteQueen[i][j] = 0;
        }
      }
    }
    breadthFirstSearch(toVisit, gameBoard, distToWhiteQueen);
    toVisit.clear();

    // Black queens BFS
    for(int i = 0; i < BOUND; i++){
      for (int j = 0; j < BOUND; j++){
        if(gameBoard[i][j] == BLACK){
          toVisit.add(new int[]{i, j, 0});
          distToBlackQueen[i][j] = 0;
        }
      }
    }
    breadthFirstSearch(toVisit, gameBoard, distToBlackQueen);

    // Count arrows and determine phase
    for(int[] row : gameBoard){
      for(int square : row){
        if(square == ARROW) arrowCount++;
      }
    }

    if(arrowCount < 15){
      phase = "opening";
    }else if(arrowCount < 45){
      phase = "mid";
    }else{
      phase = "end";
    }

    // Territory + center control
    for(int i = 0; i < BOUND; i++){
      for(int j = 0; j < BOUND; j++){
        if(gameBoard[i][j] == 0){
          int whiteDist = distToWhiteQueen[i][j];
          int blackDist = distToBlackQueen[i][j];
          int territoryWeight = phase.equals("end") ? 5 : 1;

          if(whiteDist < blackDist) whiteQScore += territoryWeight;
          else if(blackDist < whiteDist) blackQScore += territoryWeight;
        }

        if(i >= 3 && i <= 6 && j >= 3 && j <= 6){
          if(gameBoard[i][j] == WHITE) centerControlReward += 3;
          if(gameBoard[i][j] == BLACK) centerControlReward -= 3;
        }
      }
    }

    // Mobility and trapping
    whiteQScore += evalQueenMobility(gameBoard, WHITE, phase);
    blackQScore += evalQueenMobility(gameBoard, BLACK, phase);

    if(!phase.equals("opening")){
      whiteQScore += evalTrappingOpponentQueen(gameBoard, BLACK);
      blackQScore += evalTrappingOpponentQueen(gameBoard, WHITE);
    }

    whiteQScore += evalImminentTraps(gameBoard, WHITE);
    blackQScore += evalImminentTraps(gameBoard, BLACK);

    whiteQScore += evalOwnQueenSpacing(gameBoard, WHITE);
    blackQScore += evalOwnQueenSpacing(gameBoard, BLACK);

    int finalScore = whiteQScore - blackQScore;

    if(phase.equals("opening")){
      finalScore += centerControlReward;
    }

    return isWhiteQueen ? finalScore : -finalScore;
  }
}