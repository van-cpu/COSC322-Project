
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

	private Board board = null;
	private boolean playAsBlack = false;
	private boolean myTurn = false;

	private final AmazonPlayer ai = new TerritoryAI(1);

	public static void main(String[] args) {
		COSC322Test player = new COSC322Test(args[0], args[1]);

		if (player.getGameGUI() == null) {
			player.Go();
		} else {
			BaseGameGUI.sys_setup();
			java.awt.EventQueue.invokeLater(player::Go);
		}
	}

	public COSC322Test(String userName, String passwd) {
		this.userName = userName;
		this.passwd = passwd;
		this.gamegui = new BaseGameGUI(this);
	}

	@Override
	public void onLogin() {
		System.out.println("The available room/rooms is/are: ");
		List<Room> rooms = gameClient.getRoomList();
		for (Room room : rooms) {
			System.out.println("- " + room.getName());
		}

		if (rooms != null && !rooms.isEmpty()) {
			String roomName = rooms.get(0).getName();
			System.out.println("Joining room: " + roomName);
			gameClient.joinRoom(roomName);
		} else {
			System.out.println("No room available.");
		}

		userName = gameClient.getUserName();

		if (gamegui != null) {
			gamegui.setRoomInformation(gameClient.getRoomList());
		}
	}

	@Override
	public boolean handleGameMessage(String messageType, Map<String, Object> msgDetails) {
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

			if (gamegui != null) {
				gamegui.updateGameState(msgDetails);
			}

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
