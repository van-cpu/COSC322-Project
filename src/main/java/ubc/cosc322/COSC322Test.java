
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
 */
public class COSC322Test extends GamePlayer{

    private GameClient gameClient = null; 
    private BaseGameGUI gamegui = null;
	
    private String userName = null;
    private String passwd = null;

		/* Instance variables new */
		private boolean isWhiteQueen;
		private int[][] gameBoard;
	
    /**
     * The main method
     * @param args for name and passwd (current, any string would work)
     */
    public static void main(String[] args) {				 
    	COSC322Test player = new COSC322Test(args[0], args[1]);
    	player.connect();
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
				System.out.println(">>> [DEBUG] Login successful.");
				
				// 1. Fetch current room list from the server
				List<Room> rooms = gameClient.getRoomList();
				System.out.println("Available rooms:");
				
				// Print rooms to console for debugging
				for(Room r : rooms) {
						if(r != null) {
								System.out.println("- " + r.getName() + " | ID: " + r.getId() + " | Users: " + r.getUserList().size());
						}
				}
				
				// Pass room info to GUI
				if(gamegui != null) {
						gamegui.setRoomInformation(rooms);
				}
		}

		@Override
		public boolean handleGameMessage(String messageType, Map<String, Object> msgDetails) {
				// 1. Safe Room ID Debugging
				for (Room r : gameClient.getRoomList()) {
						if (r != null && r.isJoined()) {
								System.out.println(">>> [DEBUG] Currently in Room ID: " + r.getId());
								break;
						}
				}
				
				System.out.print("Received game message - ");
				System.out.print("Type: " + messageType);
				System.out.print(", Details: " + msgDetails.toString() + "\n");

				if (messageType.equals(GameMessage.GAME_ACTION_START)) {
						// Determine role
						isWhiteQueen = msgDetails.get(AmazonsGameMessage.PLAYER_WHITE).equals(getGameClient().getUserName());
						System.out.println(">>> [COSC322] Server assigned role: " + (isWhiteQueen ? "White" : "Black"));

						if(!isWhiteQueen){
								System.out.println(">>> [COSC322] Black starts. Calculating move...");
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
				} else if(messageType.equals(GameMessage.GAME_ACTION_MOVE)) {
						getGameGUI().updateGameState(msgDetails);
						updateGameBoard(msgDetails);
						printGameBoard();
						makeBestMove();
				} else {
						return false;
				}

				return true;    
		}
    
		/* This method is to be implemented */
		private void makeBestMove(){
			System.out.println("> Initiating Hybrid Search for " + (isWhiteQueen ? "White" : "Black") + "...");

			// 1. Instantiate the hybrid search engine
			MonteCarloAlphaBeta searchEngine = new MonteCarloAlphaBeta();
			
			// 2. Perform the hybrid search 
			int[][] bestMove = searchEngine.performSearch(isWhiteQueen, this.gameBoard);

			if (bestMove != null) {
				// 3. Convert 0-based indices to 1-based server indices
				// Result: [0]=Old, [1]=New, [2]=Arrow
				ArrayList<Integer> queenCurr = new ArrayList<>(Arrays.asList(bestMove[0][0] + 1, bestMove[0][1] + 1));
				ArrayList<Integer> queenNext = new ArrayList<>(Arrays.asList(bestMove[1][0] + 1, bestMove[1][1] + 1));
				ArrayList<Integer> arrowPos = new ArrayList<>(Arrays.asList(bestMove[2][0] + 1, bestMove[2][1] + 1));

				// 4. Wrap coordinates in the required message structure
				Map<String, Object> moveMessage = new HashMap<>();
				moveMessage.put(AmazonsGameMessage.QUEEN_POS_CURR, queenCurr);
				moveMessage.put(AmazonsGameMessage.QUEEN_POS_NEXT, queenNext);
				moveMessage.put(AmazonsGameMessage.ARROW_POS, arrowPos);

				// 5. Commit move: Send to server, update GUI, and sync local gameBoard
				// Using getGameClient() and getGameGUI() as per your class implementation
				getGameClient().sendMoveMessage(moveMessage);
				getGameGUI().updateGameState(moveMessage);
				
				// Using your defined method: updateGameBoard()
				updateGameBoard(moveMessage); 

				System.out.println("> Move successfully committed to server.");
			} else {
				System.out.println("> No valid moves remaining.");
			}
		}

		private void printGameBoard(){
			if(this.gameBoard == null){
				System.out.println("Game Board State is null");
				return;
			}
			System.out.println("Current Game Board State is:");
			// Printing the board state in reverse order of int[][] gameBoard
			/*
						Current Game Board State is:
			[0, 0, 0, 1, 0, 0, 1, 0, 0, 0]
			[0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
			[0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
			[1, 0, 0, 0, 0, 0, 0, 0, 0, 1]
			[0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
			[0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
			[2, 0, 0, 0, 0, 0, 0, 0, 0, 2]
			[0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
			[0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
			[0, 0, 0, 2, 0, 0, 2, 0, 0, 0]
			*/
			for(int i = 9; i >= 0; i--){
				System.out.println(Arrays.toString(gameBoard[i]));
			}
			System.out.println();
		}

		private void updateGameBoard(Map<String, Object> msgDetails){
			if(gameBoard == null){
				System.out.println("An Error Has Occurred: Game Board is Null");
			}
			
			// Extract QUEEN_POS_CURR, QUEEN_POS_NEXT, ARROW_POS from msgDetails and convert to ArrayList<Integer>
			ArrayList<Integer> queenOldPos = (ArrayList<Integer>) msgDetails.get(AmazonsGameMessage.QUEEN_POS_CURR);
			ArrayList<Integer> queenNewPos = (ArrayList<Integer>) msgDetails.get(AmazonsGameMessage.QUEEN_POS_NEXT);
			ArrayList<Integer> arrow = (ArrayList<Integer>) msgDetails.get(AmazonsGameMessage.ARROW_POS);
			System.out.println(queenOldPos.toString());
			System.out.println(queenNewPos.toString());

			// The position indices for game server and GUI is 1-based, from 1 to 10, we need to convert to 0 to 9
			// Position get 1 returns X index, and get 0 returns Y index, all subtract 1 to convert to 0-base
			int oldX = queenOldPos.get(1) - 1;
			int oldY = queenOldPos.get(0) - 1;
			int newX = queenNewPos.get(1) - 1;
			int newY = queenNewPos.get(0) - 1;
			int arrowX = arrow.get(1) - 1;
			int arrowY = arrow.get(0) - 1;

			// get the value of the queen (black or white) at the old position
			int movingQueen = gameBoard[oldY][oldX];

			// Set the newY,newX for the movingQueen value
			gameBoard[newY][newX] = movingQueen;
			// Clear the Queen value at the old indices because the queen has moved, empty field has value 0
			gameBoard[oldY][oldX] = 0;

			// Place the arrow, arrow has value of 3 as always
			gameBoard[arrowY][arrowX] = 3;
		}

		
		public void onRoomListUpdate() {
				System.out.println(">>> [DEBUG] Room list updated by server. Manual joining enabled.");
				// List<Room> rooms = gameClient.getRoomList();
				// for (Room r : rooms) {
				// 		if (r.getName().equals("Okanagan Lake")) {
				// 				System.out.println(">>> [DEBUG] Okanagan Lake current users: " + r.getUserList().size());
								
				// 				// If the room now has 1 user and we aren't in it yet, join!
				// 				if (r.getUserList().size() == 1 && !r.isJoined()) {
				// 						System.out.println(">>> [SYNC] Host detected. Joining Okanagan Lake...");
				// 						gameClient.joinRoom(r.getName());
				// 				}
				// 		}
				// }
				if (gamegui != null) {
        gamegui.setRoomInformation(gameClient.getRoomList());
    		}
		}
    
    @Override
    public String userName() {
    	return userName;
    }

		@Override
		public GameClient getGameClient() {
			// TODO Auto-generated method stub
			return this.gameClient;
		}

		@Override
		public BaseGameGUI getGameGUI() {
			// TODO Auto-generated method stub
			// Updated this to return the gamegui
			return  this.gamegui;
		}

		@Override
		public void connect() {
			// TODO Auto-generated method stub
				gameClient = new GameClient(userName, passwd, this);			
		}

 
}//end of class
