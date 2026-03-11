package ubc.cosc322;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import java.util.Map;

import sfs2x.client.entities.Room;
import ubc.cosc322.Tree.MonteCarloAlphaBeta;
import ygraph.ai.smartfox.games.BaseGameGUI;
import ygraph.ai.smartfox.games.GameClient;
import ygraph.ai.smartfox.games.GameMessage;
import ygraph.ai.smartfox.games.GamePlayer;
import ygraph.ai.smartfox.games.amazons.AmazonsGameMessage;

/**
 * An example illustrating how to implement a GamePlayer
 * @author Yong Gao (yong.gao@ubc.ca)
 * Jan 5, 2021
 * Run on cmd line with: mvn exec:java -Dexec.mainClass="ubc.cosc322.COSC322Test" -Dexec.args="cosc322 cosc322"
 * mvn exec:java -Dexec.mainClass="ubc.cosc322.COSC322Test" -Dexec.args="cosc322 cosc322" -Dexec.vmArgs="-Xmx2g"
 */
public class COSC322Test extends GamePlayer{

    private GameClient gameClient = null; 
    private BaseGameGUI gamegui = null;
	
    private String userName = null;
    private String passwd = null;

    /* Instance variables */
    private boolean isWhiteQueen;
    private int[][] gameBoard;

    /**
     * FIX: track game-over so makeBestMove() is never called after the game ends.
     * Without this flag, the server times out waiting for a move that will never come,
     * producing the "Timeout Count = N" spam seen when a player has no moves left.
     */
    private boolean gameOver = false;
	
    /**
     * The main method
     * @param args for name and passwd (current, any string would work)
     */
    public static void main(String[] args) {				 
    	COSC322Test player = new COSC322Test(args[0], args[1]);

    	if(player.getGameGUI() == null) {
    		player.Go();
    	}
    	else {
    		BaseGameGUI.sys_setup();
            java.awt.EventQueue.invokeLater(new Runnable() {
                public void run() {
                	player.Go();
                }
            });
    	}
    }
	
    /**
     * Any name and passwd 
     * @param userName
     * @param passwd
     */
    public COSC322Test(String userName, String passwd) {
    	this.userName = userName;
    	this.passwd = passwd;
    	
    	//To make a GUI-based player, create an instance of BaseGameGUI
    	//and implement the method getGameGUI() accordingly
    	this.gamegui = new BaseGameGUI(this);
    }
 
    @Override
    public void onLogin() {
        System.out.println("Congratualations!!! "
        + "I am called because the server indicated that the login is successfully");
        System.out.println("The next step is to find a room and join it: "
        + "the gameClient instance created in my constructor knows how!"); 
        userName = gameClient.getUserName(); 
        
        // Attempt to get the initial list
        List<Room> rooms = gameClient.getRoomList();
        
        System.out.println("The available room/roms is/are:");

        for(int i = 0; i < rooms.size(); i++){
            System.out.println(rooms.get(i).getName());
        }

        userName = gameClient.getUserName();
        if(getGameGUI() != null){
            getGameGUI().setRoomInformation(gameClient.getRoomList());
        }
    }

    @Override
    public boolean handleGameMessage(String messageType, Map<String, Object> msgDetails) {
        
        if (messageType.equals(GameMessage.GAME_ACTION_START)) {
            // FIX: reset game-over flag at the start of every new game
            gameOver = false;

            isWhiteQueen = msgDetails.get(AmazonsGameMessage.PLAYER_WHITE).equals(getGameClient().getUserName());

            if(!isWhiteQueen){
                makeBestMove();
            }
        } else if (messageType.equals(GameMessage.GAME_STATE_BOARD)) {
						/*
						0 for Empty
						1 White Queen
						2 Black Queen
						3 Arrow

						The array received is:
						Received game message - Type: cosc322.game-state.board, Details: 
						{game-state=
						[0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0-10
						0, 0, 0, 0, 2, 0, 0, 2, 0, 0, 0, 11-21
						0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 22-32
						0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 33-43
						0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 2, 44-54
						0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 55-65
						0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 66-76
						0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 77-87
						0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 88-98
						0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 99-109
						0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0]} 110-120
						(total 121 entries)

						Initial Board from warmup 2 run is: - 0 indexed
						Black Queens (3, 0), (0, 3), (0, 6), (3, 9)
						White Queens (6, 0), (9, 3), (9, 6), (6, 9)
						
						Top row and the leftest column are paddings 
						2 (black) at 15, 18, 45, 54
						1 (white) at 78, 87, 114, 117
						(i + 1) * 11 + (j + 1)
						Black Queen(3, 0) = (3 + 1) * 11 + (0 + 1) = 45
						Black Queen(0, 3) = (0 + 1) * 11 + (3 + 1) = 15
						Black Queen(0, 6) = (0 + 1) * 11 + (6 + 1) = 18
						Black Queen(3, 9) = (3 + 1) * 11 + (9 + 1) = 54

						The above verified that 2 stands for Black Queen
						Also, 1 for White Queen, and how translation of the
						msgDetails of AmazonsGameMessage
						*/
            ArrayList<Integer> initialBoardArray = (ArrayList<Integer>) msgDetails.get(AmazonsGameMessage.GAME_STATE);
            this.gameBoard = new int[10][10];
            
            for(int i = 0; i < 10; i++){
                for(int j = 0; j < 10; j++){
                    int index = (i + 1) * 11 + (j + 1);
                    gameBoard[i][j] = initialBoardArray.get(index);
                }
            }

            printGameBoard();
            getGameGUI().setGameState(initialBoardArray);
            
            if (isWhiteQueen) {
                System.out.println("Playing as White Queen");
            }else{
                System.out.println("Playing as Black Queen");
            }
        } else if(messageType.equals(GameMessage.GAME_ACTION_MOVE)) {
            // FIX: if the game is already over (we have no moves), ignore further
            // GAME_ACTION_MOVE events instead of calling makeBestMove() again and
            // generating the endless "Timeout Count = N" loop.
            if (gameOver) {
                System.out.println("Game is over. Ignoring incoming move.");
                return true;
            }

            getGameGUI().updateGameState(msgDetails);
            updateGameBoard(msgDetails);
            printGameBoard();
            makeBestMove();
        } else {
            return false;
        }

        return true;    
    }
    
    private void makeBestMove(){
        // Safety guard: never attempt a move after the game has ended
        if (gameOver) return;

        System.out.println("> Initiating Hybrid Search for " + (isWhiteQueen ? "White" : "Black") + "...");

        if(isWhiteQueen){
            System.out.println("Playing as White Queen");
        }else{
            System.out.println("Playing as Black Queen");
        }

        // 1. Instantiate the hybrid search engine
        MonteCarloAlphaBeta searchEngine = new MonteCarloAlphaBeta();
        
        // 2. Perform the hybrid search 
        int[][] bestMove = searchEngine.performSearch(isWhiteQueen, this.gameBoard);

        if (bestMove != null) {
            // 3. Convert 0-based indices to 1-based server indices
            // Result: [0]=Old, [1]=New, [2]=Arrow
            System.out.println("Best Move Found: " + (bestMove[0][0] + 1) + "," + (bestMove[0][1] + 1) + " → " + (bestMove[1][0] + 1) + "," + (bestMove[1][1] + 1));

            // 4. Wrap coordinates in the required message structure
            Map<String, Object> moveMessage = new HashMap<>();
            moveMessage.put(AmazonsGameMessage.QUEEN_POS_CURR, new ArrayList<>(Arrays.asList(bestMove[0][0] + 1, bestMove[0][1] + 1)));
            moveMessage.put(AmazonsGameMessage.QUEEN_POS_NEXT, new ArrayList<>(Arrays.asList(bestMove[1][0] + 1, bestMove[1][1] + 1)));
            moveMessage.put(AmazonsGameMessage.ARROW_POS, new ArrayList<>(Arrays.asList(bestMove[2][0] + 1, bestMove[2][1] + 1)));

            // 5. Commit move: Send to server, update GUI, and sync local gameBoard
            getGameClient().sendMoveMessage(moveMessage);
            getGameGUI().updateGameState(moveMessage);
            updateGameBoard(moveMessage); 

            System.out.println("Move successfully committed to server: " + moveMessage);
        } else {
            // FIX: set gameOver flag and announce the result clearly.
            // The Game of Amazons rule: the player who cannot move LOSES.
            // So if we have no moves, the opponent wins.
            gameOver = true;
            String winner = isWhiteQueen ? "BLACK" : "WHITE";
            System.out.println("==============================================");
            System.out.println("  GAME OVER — " + (isWhiteQueen ? "White" : "Black") + " has no valid moves.");
            System.out.println("  WINNER: " + winner);
            System.out.println("==============================================");
        }
    }

    private void printGameBoard(){
        if(this.gameBoard == null){
            System.out.println("Game Board State is null");
            return;
        }
        System.out.println("Current Game Board State is:");
        for(int i = 9; i >= 0; i--){
            System.out.println(Arrays.toString(gameBoard[i]));
        }
        System.out.println();
    }

    private void updateGameBoard(Map<String, Object> msgDetails){
        if(gameBoard == null){
            System.out.println("An Error Has Occurred: Game Board is Null");
            return;
        }
        
        ArrayList<Integer> queenOldPos = (ArrayList<Integer>) msgDetails.get(AmazonsGameMessage.QUEEN_POS_CURR);
        ArrayList<Integer> queenNewPos = (ArrayList<Integer>) msgDetails.get(AmazonsGameMessage.QUEEN_POS_NEXT);
        ArrayList<Integer> arrow = (ArrayList<Integer>) msgDetails.get(AmazonsGameMessage.ARROW_POS);

        // The position indices for game server and GUI is 1-based (1–10); convert to 0-based (0–9)
        // get(0) = row (Y), get(1) = col (X)
        int oldX = queenOldPos.get(1) - 1;
        int oldY = queenOldPos.get(0) - 1;
        int newX = queenNewPos.get(1) - 1;
        int newY = queenNewPos.get(0) - 1;
        int arrowX = arrow.get(1) - 1;
        int arrowY = arrow.get(0) - 1;

        int movingQueen = gameBoard[oldY][oldX];
        gameBoard[newY][newX] = movingQueen;
        gameBoard[oldY][oldX] = 0;
        gameBoard[arrowY][arrowX] = 3;
    }
    
    @Override
    public String userName() {
    	return userName;
    }

    @Override
    public GameClient getGameClient() {
        return this.gameClient;
    }

    @Override
    public BaseGameGUI getGameGUI() {
        return this.gamegui;
    }

    @Override
    public void connect() {
        gameClient = new GameClient(userName, passwd, this);			
    }

}//end of class