package ubc.cosc322;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sfs2x.client.entities.Room;
import ygraph.ai.smartfox.games.BaseGameGUI;
import ygraph.ai.smartfox.games.GameClient;
import ygraph.ai.smartfox.games.GameMessage;
import ygraph.ai.smartfox.games.GamePlayer;
import ygraph.ai.smartfox.games.amazons.AmazonsGameMessage;

import ubc.cosc322.engine.AmazonPlayer;
import ubc.cosc322.engine.Board;
import ubc.cosc322.engine.Board.Tile;
import ubc.cosc322.engine.Move;
import ubc.cosc322.engine.TerritoryAI;

/**
 * Main game client connecting to SmartFoxServer.
 *
 * Server board encoding (ArrayList<Integer>, 121 elements):
 *   index = 11 * row + col  (row and col are 1-indexed, 1-10)
 *   0 = FREE, 1 = WHITE_QUEEN, 2 = BLACK_QUEEN, 3 = ARROW
 *
 * Positions in sendMoveMessage / GAME_ACTION_MOVE messages:
 *   ArrayList<Integer> of [row, col] (1-indexed)
 *
 * Run with:
 *   mvn exec:java -Dexec.mainClass="ubc.cosc322.COSC322Test" -Dexec.args="username password"
 */
public class COSC322Test extends GamePlayer {

    private GameClient gameClient = null;
    private BaseGameGUI gamegui = null;

    private String userName = null;
    private String passwd = null;

    // --- Game state ---
    private Board board = null;
    private boolean isBlack = false;
    private boolean myTurn  = false;
    private boolean gameOver = false;

    // AI to use — TerritoryAI(1) = greedy Voronoi territory + mobility + centrality
    private final AmazonPlayer ai = new TerritoryAI(1);

    // -----------------------------------------------------------------------

    public static void main(String[] args) {
        COSC322Test player = new COSC322Test(args[0], args[1]);

        if (player.getGameGUI() == null) {
            player.Go();
        } else {
            BaseGameGUI.sys_setup();
            java.awt.EventQueue.invokeLater(new Runnable() {
                public void run() {
                    player.Go();
                }
            });
        }
    }

    public COSC322Test(String userName, String passwd) {
        this.userName = userName;
        this.passwd   = passwd;
        this.gamegui  = new BaseGameGUI(this);
    }

    // -----------------------------------------------------------------------
    // GamePlayer callbacks
    // -----------------------------------------------------------------------

    @Override
    public void onLogin() {
        System.out.println("Login successful!");

        List<Room> rooms = gameClient.getRoomList();
        System.out.println("Available rooms:");
        for (Room room : rooms) {
            System.out.println("  - " + room.getName());
        }

        userName = gameClient.getUserName();

        if (gamegui != null) {
            gamegui.setRoomInformation(gameClient.getRoomList());
        }
    }

    @Override
    public boolean handleGameMessage(String messageType, Map<String, Object> msgDetails) {
        System.out.println("Received: " + messageType);

        if (messageType.equals(GameMessage.GAME_STATE_BOARD)) {
            // Initial board state sent by the server at game start.
            ArrayList<Integer> boardState =
                    (ArrayList<Integer>) msgDetails.get(AmazonsGameMessage.GAME_STATE);
            if (boardState != null) {
                board = parseBoardState(boardState);
                System.out.println("Board initialised from server state.");
                System.out.println(board);
                if (gamegui != null) gamegui.setGameState(boardState);
            }

        } else if (messageType.equals(GameMessage.GAME_ACTION_START)) {
            gameOver = false;

            String black = (String) msgDetails.get(AmazonsGameMessage.PLAYER_BLACK);
            String white = (String) msgDetails.get(AmazonsGameMessage.PLAYER_WHITE);
            System.out.println("Black: " + black + ", White: " + white);

            isBlack = userName.equals(black);
            myTurn  = isBlack;   // black moves first
            System.out.println("I am " + (isBlack ? "BLACK" : "WHITE")
                    + " — playing with " + ai.getName());

            if (gamegui != null) gamegui.updateGameState(msgDetails);

            // If we're black, we move first.
            if (myTurn) {
                scheduleMove();
            }

        } else if (messageType.equals(GameMessage.GAME_ACTION_MOVE)) {
            if (gameOver) {
                System.out.println("Game is over. Ignoring incoming move.");
                return true;
            }

            ArrayList<Integer> qCurr =
                    (ArrayList<Integer>) msgDetails.get(AmazonsGameMessage.QUEEN_POS_CURR);
            ArrayList<Integer> qNext =
                    (ArrayList<Integer>) msgDetails.get(AmazonsGameMessage.QUEEN_POS_NEXT);
            ArrayList<Integer> arrowPos =
                    (ArrayList<Integer>) msgDetails.get(AmazonsGameMessage.ARROW_POS);

            if (qCurr != null && qNext != null && arrowPos != null && board != null) {
                // Server uses 1-indexed coordinates; our Board uses 0-indexed.
                Move serverMove = new Move(
                        qCurr.get(0) - 1,    qCurr.get(1) - 1,
                        qNext.get(0) - 1,    qNext.get(1) - 1,
                        arrowPos.get(0) - 1,  arrowPos.get(1) - 1
                );

                boolean wasMyTurn = myTurn;
                myTurn = !myTurn;

                if (!wasMyTurn) {
                    // Opponent's move: update our board, refresh GUI, then respond.
                    board.applyMove(serverMove, !isBlack);
                    if (gamegui != null) gamegui.updateGameState(msgDetails);
                    System.out.println("Opponent played: " + serverMove);
                    System.out.println(board);
                    scheduleMove();
                } else {
                    // Our own move echoed back by the server: just refresh the GUI.
                    if (gamegui != null) gamegui.updateGameState(msgDetails);
                }
            }
        }

        return true;
    }

    // -----------------------------------------------------------------------
    // Board / move helpers
    // -----------------------------------------------------------------------

    /**
     * Parse the 121-element ArrayList from GAME_STATE_BOARD into our Board.
     *
     * Server encoding: index = 11 * row + col  (row, col in 1..10)
     * Values: 0=FREE, 1=WHITE_QUEEN, 2=BLACK_QUEEN, 3=ARROW
     */
    private static Board parseBoardState(ArrayList<Integer> state) {
        Board b = new Board();
        // Clear default starting positions — we'll set from server data.
        for (int r = 0; r < Board.SIZE; r++)
            for (int c = 0; c < Board.SIZE; c++)
                b.set(r, c, Tile.FREE);

        for (int r = 1; r <= Board.SIZE; r++) {
            for (int c = 1; c <= Board.SIZE; c++) {
                int val = state.get(11 * r + c);
                Tile tile;
                switch (val) {
                    case 1:  tile = Tile.WHITE_QUEEN; break;
                    case 2:  tile = Tile.BLACK_QUEEN; break;
                    case 3:  tile = Tile.BLACK_ARROW; break;
                    default: tile = Tile.FREE;         break;
                }
                b.set(r - 1, c - 1, tile);
            }
        }
        return b;
    }

    /**
     * Run the AI on a background thread so we don't block the SFS2X event thread.
     */
    private void scheduleMove() {
        final Board snapshot = new Board(board);
        final boolean asBlack = isBlack;

        Thread t = new Thread(new Runnable() {
            public void run() {
                System.out.println("AI thinking (" + ai.getName() + ", "
                        + (asBlack ? "BLACK" : "WHITE") + ")...");

                Move move = ai.chooseMove(snapshot, asBlack);
                if (move == null) {
                    gameOver = true;
                    String winner = asBlack ? "WHITE" : "BLACK";
                    System.out.println("==============================================");
                    System.out.println("  GAME OVER - " + (asBlack ? "Black" : "White") + " has no valid moves.");
                    System.out.println("  WINNER: " + winner);
                    System.out.println("==============================================");
                    return;
                }

                System.out.println("AI chose: " + move);

                // Apply to our tracked board
                board.applyMove(move, asBlack);

                // Send to server (1-indexed coordinates, Map-based API)
                Map<String, Object> moveMsg = new HashMap<String, Object>();
                moveMsg.put(AmazonsGameMessage.QUEEN_POS_CURR,
                        new ArrayList<Integer>(Arrays.asList(move.queenRow + 1, move.queenCol + 1)));
                moveMsg.put(AmazonsGameMessage.QUEEN_POS_NEXT,
                        new ArrayList<Integer>(Arrays.asList(move.moveRow + 1, move.moveCol + 1)));
                moveMsg.put(AmazonsGameMessage.ARROW_POS,
                        new ArrayList<Integer>(Arrays.asList(move.arrowRow + 1, move.arrowCol + 1)));

                getGameClient().sendMoveMessage(moveMsg);
                if (gamegui != null) gamegui.updateGameState(moveMsg);

                System.out.println("Move sent to server: " + move);
                System.out.println(board);
            }
        }, "ai-thread");
        t.setDaemon(true);
        t.start();
    }

    // -----------------------------------------------------------------------
    // GamePlayer abstract method implementations
    // -----------------------------------------------------------------------

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
