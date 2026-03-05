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
    
    int winCount = 0;
    int visitCount = 0;
    boolean isFullyExpanded = false;

    /**
     * Constructor for a Tree Node
     * @param parent The parent node in the tree
     * @param move The move applied to reach this node
     * @param gameBoard The resulting state of the board
     * @param isWhiteQueen True if the next player to move is White
     */
    Node(Node parent, int[][] move, int[][] gameBoard, boolean isWhiteQueen) {
        this.parent = parent;
        this.move = move;
        this.gameBoard = gameBoard;
        this.isWhiteQueen = isWhiteQueen;
    }
}