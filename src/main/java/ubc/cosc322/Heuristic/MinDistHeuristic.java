package ubc.cosc322.Heuristic;

import java.util.*;

public class MinDistHeuristic {

  private static boolean isWithinBoundary(int x, int y){
    // Returns true if x,y coordinates' values are from 0-9 
    return (x >= 0) && (x < 10) && (y >= 0) && (y < 10);
  }

}
