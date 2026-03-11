package ubc.cosc322.Tree;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a node in the Search Tree for the Game of Amazons.
 */
class Node {
    // The move that led to this state: {{qOldY, qOldX}, {qNewY, qNewX}, {arrowY, arrowX}}
    int[][] move;
    int[][] gameBoard;
    boolean isWhiteQueen;

    Node parent;
    List<Node> children = new ArrayList<>();

    /**
     * Lazy expansion: moves not yet turned into child nodes.
     * null  = "not initialised yet" (will be populated on first expandNode call)
     * empty = "fully expanded" (all moves have been tried)
     */
    List<int[][]> untriedMoves = null;

    int winCount = 0;
    int visitCount = 0;
    boolean isFullyExpanded = false;

    Node(Node parent, int[][] move, int[][] gameBoard, boolean isWhiteQueen) {
        this.parent = parent;
        this.move = move;
        this.gameBoard = gameBoard;
        this.isWhiteQueen = isWhiteQueen;
    }
}