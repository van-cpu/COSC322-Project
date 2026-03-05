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
    // Returns true if x,y coordinates' values are from 0-9 
    return (x >= 0) && (x < BOUND) && (y >= 0) && (y < BOUND);
  }

  // Implement the breadth first search
  private static void breadthFirstSearch(Queue<int[]> toVisit, int[][] gameBoard, int[][] distances){
    while(!toVisit.isEmpty()){
      // pop and return the item at the front of the toVisit queue
      int[] current = toVisit.poll();
      int x = current[1];
      int y = current[0];
      int dist = current[2];

      // Loop through each of the directions, and slide in a straight line until we hit obstacle
      for(int[] dir : DIRECTIONS){
        int newX = x + dir[0]; // dir{x, y} while gameBoard is (y, x)
        int newY = y + dir[1];

        // Check whether newX and newY are within boundary of gameBoard is whether it is empty 0
        while(isWithinBoundary(newX, newY) && gameBoard[newY][newX] == 0){
          // Do the shortest path check
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

  // When our queens are completely surrounded with only 1 to 3 mobility available
  // give a dangerPenalty in this situation
  private static int evalImminentTraps(int[][] gameBoard, int queenType){
    int trapPenalty = 0;

    for(int i = 0; i < BOUND; i++){
      for( int j = 0; j < BOUND; j++){
        if(gameBoard[i][j] == queenType){
          int mobility = 0;
          // Check for each possible move directions
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
            // the closer our queen to be trapped, the greater the penalty
            trapPenalty += (4 - mobility) * 10;
          }
        }
      }
    }
    return -trapPenalty;
  }

  // Evaluate the mobility of the Queen on gameBoard (for the queen type) given the current
  // phase of the game, it counts # of squares the queen can move in a straight direction(line)
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

  // Calculate the distance between our own Queens, if too close, penalize them
  // This is for us to prevent self-blocking during the game
  // We don't want our own queens to bundle up and limit each other's movements
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
        // Calculate the "Manhattan distance"
        int distance = Math.abs(q1[0] - q2[0]) + Math.abs(q1[1] - q2[1]);
        // If they're within distance 2, penalize 10; within distance 3, penalize 5
        if(distance <= 2){
          spacePenalty += 10;
        }else if(distance <= 3){
          spacePenalty += 5;
        }
      }
    }
    return -spacePenalty;
  }

  // This calculates the urgency for a single queen, it helps the AI to decide
  // which one of our queens to choose to move with urgency
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
    // the lower the mobility, the more urgent it is for a specific queen
    return mobility;
  }

  // To calculate penalty for enemy if our queens aggressively trap the opponent queens
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

  // For a single square on the gameBoard, calculate the density of arrows within the vicinity
  // To identify the dead corner zones and avoid to move our own queens into those zones
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

    // Fill the distance 2D array
    for(int i = 0; i < BOUND; i++){
      Arrays.fill(distToBlackQueen[i], Integer.MAX_VALUE);
      Arrays.fill(distToWhiteQueen[i], Integer.MAX_VALUE);
    }

    // White queens
    for(int i = 0; i < BOUND; i++){
      for (int j = 0; j < BOUND; j++){
        if(gameBoard[i][j] == WHITE){
          toVisit.add(new int[]{i, j, 0});
          distToWhiteQueen[i][j] = 0;
        }
      }
    }
    breadthFirstSearch(toVisit, gameBoard, distToWhiteQueen);

    // clear the toVisit
    toVisit.clear();

    // Black queens
    for(int i = 0; i < BOUND; i++){
      for (int j = 0; j < BOUND; j++){
        if(gameBoard[i][j] == BLACK){
          toVisit.add(new int[]{i, j, 0});
          distToBlackQueen[i][j] = 0;
        }
      }
    }
    breadthFirstSearch(toVisit, gameBoard, distToBlackQueen);

    // Arrow count
    for(int[] row : gameBoard){
      for(int square : row){
        if(square == ARROW ){
          arrowCount++;
        }
      }
    }

    // Use the arrowCount to define the phase that we're in the game
    if(arrowCount < 15){
      phase = "opening";
    }else if(arrowCount < 45){
      phase = "mid";
    }else{
      phase = "end";
    }

    // Center control reward bonus points
    for(int i = 0; i < BOUND; i++){
      for(int j = 0; j < BOUND; j++){
        // Territory Calculation
        if(gameBoard[i][j] == 0){
          int whiteDist = distToWhiteQueen[i][j];
          int blackDist = distToBlackQueen[i][j];

          // Increate weight for territory in end phase of game
          int territoryWeight = phase.equals("end") ? 5 : 1;

          if(whiteDist < blackDist) whiteQScore += territoryWeight;
          else if(blackDist < whiteDist) blackQScore += territoryWeight;
        }

        // Center control reward in the opening phase
        if(i >= 3 && i <= 6 && j >= 3 && j <= 6){
          if(gameBoard[i][j] == WHITE) centerControlReward += 3;
          if(gameBoard[i][j] == BLACK) centerControlReward -= 3;
        }
      }
    }

    // Mobility and Trapping Logic
    whiteQScore += evalQueenMobility(gameBoard, WHITE, phase);
    blackQScore += evalQueenMobility(gameBoard, BLACK, phase);

    if(!phase.equals("opening")){
      whiteQScore += evalTrappingOpponentQueen(gameBoard, BLACK);
      blackQScore += evalTrappingOpponentQueen(gameBoard, WHITE);
    }

    // Spacing evaluation
    whiteQScore += evalImminentTraps(gameBoard, WHITE);
    blackQScore += evalImminentTraps(gameBoard, BLACK);

    whiteQScore += evalOwnQueenSpacing(gameBoard, WHITE);
    blackQScore += evalOwnQueenSpacing(gameBoard, BLACK);

    // Calculate final score
    int finalScore = whiteQScore - blackQScore;

    if(phase.equals("opening")){
      finalScore += centerControlReward;
    }

    // return score relative to the pov of the request queen
    return isWhiteQueen ? finalScore : -finalScore;
  }
}
