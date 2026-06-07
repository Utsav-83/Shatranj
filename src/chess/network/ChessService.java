package chess.network;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Remote multiplayer contract for hosting and joining a chess game.
 */
public interface ChessService extends Remote {

    /**
     * Joins the hosted game.
     *
     * @param playerName player display name
     * @return assigned player session
     * @throws RemoteException if the remote call fails
     */
    PlayerSession joinGame(String playerName) throws RemoteException;

    /**
     * Leaves the hosted game.
     *
     * @param playerId player id returned by {@link #joinGame(String)}
     * @throws RemoteException if the remote call fails
     */
    void leaveGame(String playerId) throws RemoteException;

    /**
     * Submits a move from one player to the shared move log.
     *
     * @param playerId player id
     * @param move move to synchronize
     * @throws RemoteException if the remote call fails
     */
    void submitMove(String playerId, RemoteMove move) throws RemoteException;

    /**
     * Gets all moves after the supplied move index.
     *
     * @param playerId player id
     * @param afterMoveNumber last move number already seen, or {@code 0}
     * @return moves newer than {@code afterMoveNumber}
     * @throws RemoteException if the remote call fails
     */
    List<RemoteMove> getMovesSince(String playerId, int afterMoveNumber) throws RemoteException;

    /**
     * Updates the player's heartbeat so the host knows the player is connected.
     *
     * @param playerId player id
     * @throws RemoteException if the remote call fails
     */
    void heartbeat(String playerId) throws RemoteException;

    /**
     * Gets a snapshot of the multiplayer game state.
     *
     * @return current game state
     * @throws RemoteException if the remote call fails
     */
    GameSnapshot getSnapshot() throws RemoteException;

    /**
     * Immutable remote move value transferred over RMI.
     */
    final class RemoteMove implements Serializable {

        private static final long serialVersionUID = 1L;

        private final int moveNumber;
        private final String playerId;
        private final int sourceRow;
        private final int sourceColumn;
        private final int destinationRow;
        private final int destinationColumn;
        private final String promotionSymbol;
        private final long timestampMillis;

        /**
         * Creates a move before it is assigned a move number by the server.
         *
         * @param sourceRow source row
         * @param sourceColumn source column
         * @param destinationRow destination row
         * @param destinationColumn destination column
         * @param promotionSymbol promotion symbol, or {@code null}
         */
        public RemoteMove(int sourceRow, int sourceColumn, int destinationRow, int destinationColumn,
                String promotionSymbol) {
            this(0, null, sourceRow, sourceColumn, destinationRow, destinationColumn, promotionSymbol,
                    System.currentTimeMillis());
        }

        /**
         * Creates a fully populated remote move.
         *
         * @param moveNumber move number assigned by the host
         * @param playerId player that submitted the move
         * @param sourceRow source row
         * @param sourceColumn source column
         * @param destinationRow destination row
         * @param destinationColumn destination column
         * @param promotionSymbol promotion symbol, or {@code null}
         * @param timestampMillis server timestamp
         */
        public RemoteMove(int moveNumber, String playerId, int sourceRow, int sourceColumn, int destinationRow,
                int destinationColumn, String promotionSymbol, long timestampMillis) {
            this.moveNumber = moveNumber;
            this.playerId = playerId;
            this.sourceRow = sourceRow;
            this.sourceColumn = sourceColumn;
            this.destinationRow = destinationRow;
            this.destinationColumn = destinationColumn;
            this.promotionSymbol = promotionSymbol;
            this.timestampMillis = timestampMillis;
        }

        public int getMoveNumber() {
            return moveNumber;
        }

        public String getPlayerId() {
            return playerId;
        }

        public int getSourceRow() {
            return sourceRow;
        }

        public int getSourceColumn() {
            return sourceColumn;
        }

        public int getDestinationRow() {
            return destinationRow;
        }

        public int getDestinationColumn() {
            return destinationColumn;
        }

        public String getPromotionSymbol() {
            return promotionSymbol;
        }

        public long getTimestampMillis() {
            return timestampMillis;
        }
    }

    /**
     * Player assignment returned by the host.
     */
    final class PlayerSession implements Serializable {

        private static final long serialVersionUID = 1L;

        private final String playerId;
        private final String playerName;
        private final boolean whitePlayer;

        public PlayerSession(String playerId, String playerName, boolean whitePlayer) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.whitePlayer = whitePlayer;
        }

        public String getPlayerId() {
            return playerId;
        }

        public String getPlayerName() {
            return playerName;
        }

        public boolean isWhitePlayer() {
            return whitePlayer;
        }
    }

    /**
     * Serializable snapshot of players, moves, and connection state.
     */
    final class GameSnapshot implements Serializable {

        private static final long serialVersionUID = 1L;

        private final ArrayList<PlayerSession> players;
        private final ArrayList<RemoteMove> moves;
        private final ArrayList<String> disconnectedPlayerIds;

        public GameSnapshot(List<PlayerSession> players, List<RemoteMove> moves, List<String> disconnectedPlayerIds) {
            this.players = new ArrayList<>(players);
            this.moves = new ArrayList<>(moves);
            this.disconnectedPlayerIds = new ArrayList<>(disconnectedPlayerIds);
        }

        public List<PlayerSession> getPlayers() {
            return Collections.unmodifiableList(new ArrayList<>(players));
        }

        public List<RemoteMove> getMoves() {
            return Collections.unmodifiableList(new ArrayList<>(moves));
        }

        public List<String> getDisconnectedPlayerIds() {
            return Collections.unmodifiableList(new ArrayList<>(disconnectedPlayerIds));
        }
    }
}
