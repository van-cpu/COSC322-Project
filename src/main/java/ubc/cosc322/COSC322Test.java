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
 * Game of Amazons AI player — Team 18
 *
 * Board encoding: White=1, Black=2, Arrow=3, Empty=0
 * gameBoard[row][col] where row=0 is bottom row.
 * Server coords are 1-based [row, col]: get(0)=row, get(1)=col.
 *
 * Turn order:
 *   GAME_STATE_BOARD  → initialise gameBoard (arrives before GAME_ACTION_START)
 *   GAME_ACTION_START → Black moves first immediately; White waits
 *   GAME_ACTION_MOVE  → opponent moved; we respond (unless gameOver)
 */
public class COSC322Test extends GamePlayer {

    private GameClient   gameClient = null;
    private BaseGameGUI  gamegui    = null;
    private String       userName   = null;
    private String       passwd     = null;

    /* Game state */
    private boolean isWhiteQueen = false;
    private int[][] gameBoard    = null;
    private boolean gameOver     = false;

    // ── Entry point ───────────────────────────────────────────────────────────

    public static void main(String[] args) {
        COSC322Test player = new COSC322Test(args[0], args[1]);
        if (player.getGameGUI() == null) {
            player.Go();
        } else {
            BaseGameGUI.sys_setup();
            java.awt.EventQueue.invokeLater(() -> player.Go());
        }
    }

    public COSC322Test(String userName, String passwd) {
        this.userName = userName;
        this.passwd   = passwd;
        this.gamegui  = new BaseGameGUI(this);
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Override
    public void onLogin() {
        System.out.println("Congratualations!!! I am called because the server indicated that the login is successfully");
        System.out.println("The next step is to find a room and join it: the gameClient instance created in my constructor knows how!");
        userName = gameClient.getUserName();
        List<Room> rooms = gameClient.getRoomList();
        System.out.println("The available room/roms is/are:");
        for (Room room : rooms) System.out.println(room.getName());
        if (getGameGUI() != null) getGameGUI().setRoomInformation(gameClient.getRoomList());
    }

    // ── Message handler ───────────────────────────────────────────────────────

    @Override
    public boolean handleGameMessage(String messageType, Map<String, Object> msgDetails) {

        // 1. Board initialisation — always arrives before GAME_ACTION_START
        if (messageType.equals(GameMessage.GAME_STATE_BOARD)) {
            ArrayList<Integer> flat =
                (ArrayList<Integer>) msgDetails.get(AmazonsGameMessage.GAME_STATE);
            gameBoard = new int[10][10];
            for (int i = 0; i < 10; i++)
                for (int j = 0; j < 10; j++)
                    gameBoard[i][j] = flat.get((i + 1) * 11 + (j + 1));
            printGameBoard();
            getGameGUI().setGameState(flat);
            System.out.println("Playing as " + (isWhiteQueen ? "White" : "Black") + " Queen");

        // 2. Game start — determine colour, Black moves immediately
        } else if (messageType.equals(GameMessage.GAME_ACTION_START)) {
            gameOver     = false;
            isWhiteQueen = msgDetails.get(AmazonsGameMessage.PLAYER_WHITE)
                                     .equals(getGameClient().getUserName());
            System.out.println("Game started. Playing as "
                + (isWhiteQueen ? "White" : "Black") + " Queen");

            // Black moves first. White waits for the first GAME_ACTION_MOVE.
            if (!isWhiteQueen) {
                if (gameBoard == null) {
                    System.err.println("ERROR: gameBoard not initialised yet on GAME_ACTION_START.");
                } else {
                    makeBestMove();
                }
            }

        // 3. Opponent moved — apply their move, then respond
        } else if (messageType.equals(GameMessage.GAME_ACTION_MOVE)) {
            getGameGUI().updateGameState(msgDetails);
            updateGameBoard(msgDetails);   // keep our local board in sync
            printGameBoard();
            if (!gameOver) makeBestMove();

        } else {
            return false;
        }
        return true;
    }

    // ── Search + commit our move ──────────────────────────────────────────────

    private void makeBestMove() {
        if (gameOver)       return;
        if (gameBoard == null) { System.err.println("ERROR: gameBoard is null."); return; }

        System.out.println("> Initiating Hybrid Search for "
            + (isWhiteQueen ? "White" : "Black") + "...");
        System.out.println("Playing as " + (isWhiteQueen ? "White" : "Black") + " Queen");

        MonteCarloAlphaBeta engine = new MonteCarloAlphaBeta();
        int[][] bestMove = engine.performSearch(isWhiteQueen, this.gameBoard);

        if (bestMove != null) {
            // bestMove = {{oldRow,oldCol},{newRow,newCol},{arrRow,arrCol}} (0-based)
            // Server expects 1-based values with get(0)=row, get(1)=col
            int oldRow = bestMove[0][0] + 1, oldCol = bestMove[0][1] + 1;
            int newRow = bestMove[1][0] + 1, newCol = bestMove[1][1] + 1;
            int arrRow = bestMove[2][0] + 1, arrCol = bestMove[2][1] + 1;

            System.out.println("Best Move Found: " + oldRow + "," + oldCol
                + " → " + newRow + "," + newCol
                + ", arrow:[" + arrRow + "," + arrCol + "]");

            Map<String, Object> msg = new HashMap<>();
            msg.put(AmazonsGameMessage.QUEEN_POS_CURR,
                new ArrayList<>(Arrays.asList(oldRow, oldCol)));
            msg.put(AmazonsGameMessage.QUEEN_POS_NEXT,
                new ArrayList<>(Arrays.asList(newRow, newCol)));
            msg.put(AmazonsGameMessage.ARROW_POS,
                new ArrayList<>(Arrays.asList(arrRow, arrCol)));

            // Commit: server → GUI → local board (all three must stay in sync)
            getGameClient().sendMoveMessage(msg);
            getGameGUI().updateGameState(msg);
            updateGameBoard(msg);

            System.out.println("Move successfully committed to server: " + msg);

        } else {
            gameOver = true;
            System.out.println("==============================================");
            System.out.println("  GAME OVER — "
                + (isWhiteQueen ? "White" : "Black") + " has no valid moves.");
            System.out.println("  WINNER: " + (isWhiteQueen ? "BLACK" : "WHITE"));
            System.out.println("==============================================");
        }
    }

    // ── Apply any move (ours or opponent's) to the local board ───────────────

    private void updateGameBoard(Map<String, Object> msgDetails) {
        if (gameBoard == null) {
            System.err.println("ERROR: updateGameBoard called with null gameBoard.");
            return;
        }
        ArrayList<Integer> qOld = (ArrayList<Integer>) msgDetails.get(AmazonsGameMessage.QUEEN_POS_CURR);
        ArrayList<Integer> qNew = (ArrayList<Integer>) msgDetails.get(AmazonsGameMessage.QUEEN_POS_NEXT);
        ArrayList<Integer> arr  = (ArrayList<Integer>) msgDetails.get(AmazonsGameMessage.ARROW_POS);

        // get(0)=row (Y), get(1)=col (X); subtract 1 for 0-based
        int oldRow = qOld.get(0) - 1, oldCol = qOld.get(1) - 1;
        int newRow = qNew.get(0) - 1, newCol = qNew.get(1) - 1;
        int arrRow = arr.get(0)  - 1, arrCol = arr.get(1)  - 1;

        int queen = gameBoard[oldRow][oldCol];
        gameBoard[oldRow][oldCol] = 0;
        gameBoard[newRow][newCol] = queen;

        // Safety: never overwrite a queen with an arrow
        int target = gameBoard[arrRow][arrCol];
        if (target == 1 || target == 2) {
            System.err.println("WARNING: arrow would overwrite queen at ["
                + arrRow + "," + arrCol + "]! Skipping arrow placement.");
        } else {
            gameBoard[arrRow][arrCol] = 3;
        }
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private void printGameBoard() {
        if (gameBoard == null) { System.out.println("Game Board State is null"); return; }
        System.out.println("Current Game Board State is:");
        for (int i = 9; i >= 0; i--) System.out.println(Arrays.toString(gameBoard[i]));
        System.out.println();
    }

    @Override public String      userName()      { return userName; }
    @Override public GameClient  getGameClient() { return gameClient; }
    @Override public BaseGameGUI getGameGUI()    { return gamegui; }
    @Override public void        connect()       { gameClient = new GameClient(userName, passwd, this); }

} // end of class