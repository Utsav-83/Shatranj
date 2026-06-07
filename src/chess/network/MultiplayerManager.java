package chess.network;

import java.rmi.RemoteException;

import chess.board.Position;
import chess.move.Move;

/**
 * High-level facade for hosting, joining, sending, and receiving multiplayer
 * chess moves.
 */
public class MultiplayerManager implements AutoCloseable {

    private final Object lock;
    private final MultiplayerEventListener listener;

    private ChessServer server;
    private ChessClient client;
    private ChessService.PlayerSession session;

    /**
     * Receives multiplayer lifecycle events.
     */
    public interface MultiplayerEventListener {

        /**
         * Called when a remote move arrives.
         *
         * @param move converted local move
         */
        void onMoveReceived(Move move);

        /**
         * Called when a disconnect is detected.
         *
         * @param message disconnect message
         */
        void onDisconnected(String message);
    }

    /**
     * Creates a multiplayer manager.
     *
     * @param listener event listener, or {@code null}
     */
    public MultiplayerManager(MultiplayerEventListener listener) {
        this.lock = new Object();
        this.listener = listener;
    }

    /**
     * Hosts a game and joins it as the host player.
     *
     * @param playerName host player name
     * @throws RemoteException if hosting or joining fails
     */
    public void hostGame(String playerName) throws RemoteException {
        hostGame(playerName, ChessServer.DEFAULT_PORT, ChessServer.DEFAULT_SERVICE_NAME);
    }

    /**
     * Hosts a game and joins it as the host player.
     *
     * @param playerName host player name
     * @param port RMI registry port
     * @param serviceName service binding name
     * @throws RemoteException if hosting or joining fails
     */
    public void hostGame(String playerName, int port, String serviceName) throws RemoteException {
        ChessServer createdServer = new ChessServer();
        try {
            createdServer.hostGame(port, serviceName);

            synchronized (lock) {
                server = createdServer;
            }

            joinGame("localhost", port, serviceName, playerName);
        } catch (RemoteException | RuntimeException exception) {
            createdServer.stopServer();
            synchronized (lock) {
                if (server == createdServer) {
                    server = null;
                }
            }
            throw exception;
        }
    }

    /**
     * Joins a hosted game.
     *
     * @param host host address
     * @param playerName player name
     * @throws RemoteException if joining fails
     */
    public void joinGame(String host, String playerName) throws RemoteException {
        joinGame(host, ChessServer.DEFAULT_PORT, ChessServer.DEFAULT_SERVICE_NAME, playerName);
    }

    /**
     * Joins a hosted game.
     *
     * @param host host address
     * @param port RMI registry port
     * @param serviceName service binding name
     * @param playerName player name
     * @throws RemoteException if joining fails
     */
    public void joinGame(String host, int port, String serviceName, String playerName) throws RemoteException {
        ChessClient createdClient = new ChessClient(new ChessClient.MultiplayerListener() {
            @Override
            public void onRemoteMove(ChessService.RemoteMove remoteMove) {
                notifyMoveReceived(toLocalMove(remoteMove));
            }

            @Override
            public void onDisconnected(String message) {
                notifyDisconnected(message);
            }
        });

        ChessService.PlayerSession joinedSession = createdClient.joinGame(host, port, serviceName, playerName);

        synchronized (lock) {
            if (client != null) {
                client.close();
            }

            client = createdClient;
            session = joinedSession;
        }
    }

    /**
     * Sends a local move to the remote game.
     *
     * @param move local move
     * @throws RemoteException if sending fails
     */
    public void sendMove(Move move) throws RemoteException {
        ChessClient clientSnapshot;

        synchronized (lock) {
            if (client == null || !client.isConnected()) {
                throw new IllegalStateException("No multiplayer game is connected.");
            }

            clientSnapshot = client;
        }

        clientSnapshot.sendMove(toRemoteMove(move));
    }

    /**
     * Gets the assigned multiplayer session.
     *
     * @return session, or {@code null}
     */
    public ChessService.PlayerSession getSession() {
        synchronized (lock) {
            return session;
        }
    }

    /**
     * Checks whether this manager is hosting a game.
     *
     * @return {@code true} if hosting
     */
    public boolean isHosting() {
        synchronized (lock) {
            return server != null;
        }
    }

    /**
     * Checks whether this manager is connected to a game.
     *
     * @return {@code true} if connected
     */
    public boolean isConnected() {
        synchronized (lock) {
            return client != null && client.isConnected();
        }
    }

    /**
     * Disconnects from the current game and stops hosting if necessary.
     */
    @Override
    public void close() {
        ChessClient clientSnapshot;
        ChessServer serverSnapshot;

        synchronized (lock) {
            clientSnapshot = client;
            serverSnapshot = server;
            client = null;
            server = null;
            session = null;
        }

        if (clientSnapshot != null) {
            clientSnapshot.close();
        }

        if (serverSnapshot != null) {
            serverSnapshot.stopServer();
        }
    }

    private ChessService.RemoteMove toRemoteMove(Move move) {
        if (move == null) {
            throw new IllegalArgumentException("Move cannot be null.");
        }

        return new ChessService.RemoteMove(move.getSource().getRow(), move.getSource().getColumn(),
                move.getDestination().getRow(), move.getDestination().getColumn(), move.getPromotionSymbol());
    }

    private Move toLocalMove(ChessService.RemoteMove remoteMove) {
        Position source = new Position(remoteMove.getSourceRow(), remoteMove.getSourceColumn());
        Position destination = new Position(remoteMove.getDestinationRow(), remoteMove.getDestinationColumn());
        Move move = new Move(source, destination);
        move.setPromotionSymbol(remoteMove.getPromotionSymbol());
        return move;
    }

    private void notifyMoveReceived(Move move) {
        if (listener != null) {
            listener.onMoveReceived(move);
        }
    }

    private void notifyDisconnected(String message) {
        if (listener != null) {
            listener.onDisconnected(message);
        }
    }
}
