
package ubc.cosc322;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sfs2x.client.entities.Room;
import ubc.cosc322.engine.AmazonPlayer;
import ubc.cosc322.engine.Board;
import ubc.cosc322.engine.Board.Tile;
import ubc.cosc322.engine.Move;
import ubc.cosc322.engine.TerritoryAI;
import ygraph.ai.smartfox.games.BaseGameGUI;
import ygraph.ai.smartfox.games.GameClient;
import ygraph.ai.smartfox.games.GameMessage;
import ygraph.ai.smartfox.games.GamePlayer;
import ygraph.ai.smartfox.games.amazons.AmazonsGameMessage;

public class COSC322Test extends GamePlayer {

	private GameClient gameClient = null;
	private BaseGameGUI gamegui = null;

	private String userName = null;
	private String passwd = null;
	private final boolean guiMode;
	private final String requestedRoomName;

	private Board board = null;
	private boolean playAsBlack = false;
	private boolean myTurn = false;
	private boolean roomJoinRequested = false;
	private long lastRoomRefreshMs = 0L;

	private static final long ROOM_REFRESH_DEBOUNCE_MS = 1000L;

	private final AmazonPlayer ai = new TerritoryAI(1);

	public static void main(String[] args) {
		if (args.length < 2) {
			throw new IllegalArgumentException("Expected username and password arguments.");
		}

		boolean useGui = args.length > 2 && "--gui".equalsIgnoreCase(args[2]);
		String roomName = null;
		if (!useGui) {
			if (args.length < 3) {
				throw new IllegalArgumentException("Non-GUI mode requires a room name as the third argument.");
			}
			roomName = args[2];
		}

		COSC322Test player = new COSC322Test(args[0], args[1], useGui, roomName);

		if (player.getGameGUI() == null) {
			player.Go();
		} else {
			BaseGameGUI.sys_setup();
			java.awt.EventQueue.invokeLater(player::Go);
		}
	}

	public COSC322Test(String userName, String passwd, boolean useGui, String requestedRoomName) {
		this.userName = userName;
		this.passwd = passwd;
		this.guiMode = useGui;
		this.requestedRoomName = requestedRoomName;
		this.gamegui = useGui ? new BaseGameGUI(this) : null;
	}

	@Override
	public void onLogin() {
		userName = gameClient.getUserName();
		roomJoinRequested = false;
		System.out.println("The available room/rooms is/are: ");
		List<Room> rooms = gameClient.getRoomList();
		refreshRoomInformation(rooms, false);

		if (rooms == null || rooms.isEmpty()) {
			System.out.println("No room available.");
			return;
		}

		for (Room room : rooms) {
			System.out.println("- " + room.getName() + " (" + room.getUserCount() + "/" + room.getMaxUsers() + ")");
		}

		if (guiMode) {
			System.out.println("GUI mode: select a room once in the room panel to join.");
			return;
		}

		joinRoomOnce(requestedRoomName, rooms);
	}

	@Override
	public boolean handleGameMessage(String messageType, Map<String, Object> msgDetails) {
		if (isRoomMetadataMessage(messageType)) {
			refreshRoomInformation(gameClient.getRoomList(), true);
		}

		System.out.print("Received game message - ");
		System.out.print("Type: " + messageType);
		System.out.print(", Details: " + msgDetails + "\n");

		if (messageType.equals(GameMessage.GAME_STATE_BOARD)) {
			ArrayList<Integer> boardState = extractIntegerList(msgDetails.get(AmazonsGameMessage.GAME_STATE));
			if (boardState != null) {
				board = parseBoardState(boardState);
				if (gamegui != null) {
					gamegui.setGameState(boardState);
				}
			}
			return true;
		}

		if (messageType.equals(GameMessage.GAME_ACTION_START)) {
			String blackPlayer = (String) msgDetails.get(AmazonsGameMessage.PLAYER_BLACK);
			String whitePlayer = (String) msgDetails.get(AmazonsGameMessage.PLAYER_WHITE);
			System.out.println("Black: " + blackPlayer + ", White: " + whitePlayer);

			playAsBlack = userName.equals(blackPlayer);
			myTurn = playAsBlack;

			if (myTurn) {
				scheduleMove();
			}
			return true;
		}

		if (messageType.equals(GameMessage.GAME_ACTION_MOVE)) {
			ArrayList<Integer> queenCurrent = extractIntegerList(msgDetails.get(AmazonsGameMessage.QUEEN_POS_CURR));
			ArrayList<Integer> queenNext = extractIntegerList(msgDetails.get(AmazonsGameMessage.QUEEN_POS_NEXT));
			ArrayList<Integer> arrowPosition = extractIntegerList(msgDetails.get(AmazonsGameMessage.ARROW_POS));

			if (board != null && queenCurrent != null && queenNext != null && arrowPosition != null) {
				Move move = new Move(
					queenCurrent.get(0) - 1,
					queenCurrent.get(1) - 1,
					queenNext.get(0) - 1,
					queenNext.get(1) - 1,
					arrowPosition.get(0) - 1,
					arrowPosition.get(1) - 1
				);

				boolean wasMyTurn = myTurn;
				myTurn = !myTurn;

				if (!wasMyTurn) {
					board.applyMove(move, !playAsBlack);
					if (gamegui != null) {
						gamegui.updateGameState(msgDetails);
					}
					scheduleMove();
				} else if (gamegui != null) {
					gamegui.updateGameState(msgDetails);
				}
			}
			return true;
		}

		return false;
	}

	private synchronized boolean joinRoomOnce(String roomName, List<Room> rooms) {
		if (roomJoinRequested) {
			System.out.println("Ignoring duplicate join request.");
			return false;
		}

		if (roomName == null || roomName.trim().isEmpty()) {
			System.out.println("No target room provided for joining.");
			return false;
		}

		if (rooms != null) {
			Room target = findRoomByName(rooms, roomName);
			if (target == null) {
				System.out.println("Requested room not found: " + roomName);
				return false;
			}
			if (target.getUserCount() >= target.getMaxUsers()) {
				System.out.println("Requested room is full: " + roomName);
				return false;
			}
		}

		System.out.println("Joining room: " + roomName);
		gameClient.joinRoom(roomName);
		roomJoinRequested = true;
		return true;
	}

	private void refreshRoomInformation(List<Room> rooms, boolean debounced) {
		if (gamegui != null && rooms != null) {
			long now = System.currentTimeMillis();
			if (debounced && now - lastRoomRefreshMs < ROOM_REFRESH_DEBOUNCE_MS) {
				return;
			}
			gamegui.setRoomInformation(rooms);
			lastRoomRefreshMs = now;
		}
	}

	private static Room findRoomByName(List<Room> rooms, String roomName) {
		for (Room room : rooms) {
			if (room.getName().equals(roomName)) {
				return room;
			}
		}
		return null;
	}

	private static boolean isRoomMetadataMessage(String messageType) {
		if (messageType == null) {
			return false;
		}

		String lower = messageType.toLowerCase();
		return lower.contains("room") || lower.contains("occup") || lower.contains("join") || lower.contains("leave");
	}

	private static Board parseBoardState(ArrayList<Integer> state) {
		Board parsed = new Board();

		for (int row = 0; row < Board.SIZE; row++) {
			for (int col = 0; col < Board.SIZE; col++) {
				parsed.set(row, col, Tile.FREE);
			}
		}

		for (int row = 1; row <= Board.SIZE; row++) {
			for (int col = 1; col <= Board.SIZE; col++) {
				int value = state.get(11 * row + col);
				Tile tile;
				switch (value) {
					case 1:
						tile = Tile.WHITE_QUEEN;
						break;
					case 2:
						tile = Tile.BLACK_QUEEN;
						break;
					case 3:
						tile = Tile.BLACK_ARROW;
						break;
					default:
						tile = Tile.FREE;
						break;
				}
				parsed.set(row - 1, col - 1, tile);
			}
		}

		return parsed;
	}

	private void scheduleMove() {
		final Board snapshot = new Board(board);
		final boolean moveAsBlack = playAsBlack;

		Thread worker = new Thread(() -> {
			Move chosenMove = ai.chooseMove(snapshot, moveAsBlack);
			if (chosenMove == null) {
				return;
			}

			board.applyMove(chosenMove, moveAsBlack);

			ArrayList<Integer> queenCurrent = toServerPosition(chosenMove.queenRow, chosenMove.queenCol);
			ArrayList<Integer> queenNext = toServerPosition(chosenMove.moveRow, chosenMove.moveCol);
			ArrayList<Integer> arrowPosition = toServerPosition(chosenMove.arrowRow, chosenMove.arrowCol);

			Map<String, Object> moveMessage = new HashMap<>();
			moveMessage.put(AmazonsGameMessage.QUEEN_POS_CURR, queenCurrent);
			moveMessage.put(AmazonsGameMessage.QUEEN_POS_NEXT, queenNext);
			moveMessage.put(AmazonsGameMessage.ARROW_POS, arrowPosition);
			gameClient.sendMoveMessage(moveMessage);
		}, "ai-thread");

		worker.setDaemon(true);
		worker.start();
	}

	private static ArrayList<Integer> toServerPosition(int row, int col) {
		ArrayList<Integer> position = new ArrayList<>();
		position.add(row + 1);
		position.add(col + 1);
		return position;
	}

	private static ArrayList<Integer> extractIntegerList(Object value) {
		if (!(value instanceof List<?>)) {
			return null;
		}

		ArrayList<Integer> numbers = new ArrayList<>();
		for (Object item : (List<?>) value) {
			if (item instanceof Integer) {
				numbers.add((Integer) item);
			} else if (item instanceof Number) {
				numbers.add(((Number) item).intValue());
			} else {
				return null;
			}
		}

		return numbers;
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
}
