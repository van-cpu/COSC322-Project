
package ubc.cosc322;

import java.util.ArrayList;
import java.util.List;

import java.util.Map;

import sfs2x.client.entities.Room;
import ygraph.ai.smartfox.games.BaseGameGUI;
import ygraph.ai.smartfox.games.GameClient;
import ygraph.ai.smartfox.games.GameMessage;
import ygraph.ai.smartfox.games.GamePlayer;
import ygraph.ai.smartfox.games.amazons.AmazonsGameMessage;

/**
 * An example illustrating how to implement a GamePlayer
 * @author Yong Gao (yong.gao@ubc.ca)
 * Jan 5, 2021
 *
 */
public class COSC322Test extends GamePlayer{

    private GameClient gameClient = null; 
    private BaseGameGUI gamegui = null;
	
    private String userName = null;
    private String passwd = null;
 
	
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
//    	System.out.println("Congratualations!!! "
//    			+ "I am called because the server indicated that the login is successfully");
//    	System.out.println("The next step is to find a room and join it: "
//    			+ "the gameClient instance created in my constructor knows how!"); 
    	
    	// Implement the room list
    	System.out.println("The available room/rooms is/are: ");
    	List<Room> rooms = gameClient.getRoomList();
    	for(Room room : rooms) {
    		System.out.println("- " + room.getName());
    	}
    	
    	//Joining a room
    	if(rooms != null && !rooms.isEmpty()) {
    		String roomName = rooms.get(0).getName();
    		System.out.println("Joining room: " + roomName);
    		gameClient.joinRoom(roomName);
    	}else {
    		System.out.println("No room available.");
    	}
	    userName = gameClient.getUserName(); 
	    
	    
	    if(gamegui != null) {
	        gamegui.setRoomInformation(gameClient.getRoomList());
	    }
    }

    @Override
    public boolean handleGameMessage(String messageType, Map<String, Object> msgDetails) {
    	//This method will be called by the GameClient when it receives a game-related message
    	//from the server.
	
    	//For a detailed description of the message types and format, 
    	//see the method GamePlayer.handleGameMessage() in the game-client-api document. 
    	System.out.print("Received game message - ");
        System.out.print("Type: " + messageType);
        System.out.print(", Details: " + msgDetails.toString() + "\n");

        if (messageType.equals(GameMessage.GAME_STATE_BOARD)) {
            ArrayList<Integer> boardState = (ArrayList<Integer>) msgDetails.get(AmazonsGameMessage.GAME_STATE);
            if (boardState != null) {
                this.gamegui.setGameState(boardState);
            }
        } else if (messageType.equals(GameMessage.GAME_ACTION_START)) {
            String black = (String) msgDetails.get(AmazonsGameMessage.PLAYER_BLACK);
            String white = (String) msgDetails.get(AmazonsGameMessage.PLAYER_WHITE);
            System.out.println("Black: " + black + ", White: " + white);
            this.gamegui.updateGameState(msgDetails);
        } else if (messageType.equals(GameMessage.GAME_ACTION_MOVE)) {
            this.gamegui.updateGameState(msgDetails);
        }

        return true;   	
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
