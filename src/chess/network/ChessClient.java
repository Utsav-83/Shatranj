package chess.network;

import java.rmi.RemoteException;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * RMI client used to join a hosted chess game and synchronize moves.
 */
public class ChessClient implements AutoCloseable {

    private static final long POLL_INTERVAL_MILLIS = 500L;
    private static final long HEARTBEAT_INTERVAL_MILLIS = 3000L;

    private final Object lock;
    private final ExecutorService executorService;
    private final MultiplayerListener listener;

    private ChessService service;
    private ChessService.PlayerSession session;
    private Future<?> syncTask;
    private int lastMoveNumber;
    private boolean connected;

    /**
     * Receives remote multiplayer events.
     */
    public interface MultiplayerListener {

        /**
         * Called for every synchronized move from the host.
         *
         * @param move remote move
         */
        void onRemoteMove(ChessService.RemoteMove move);

        /**
         * Called when the client detects a remote disconnect or network failure.
         *
         * @param message disconnect message
         */
        void onDisconnected(String message);
    }

    /**
     * Creates a client.
     *
     * @param listener listener callback, or {@code null}
     */
    public ChessClient(MultiplayerListener listener) {
        this.lock = new Object();
        this.listener = listener;
        this.executorService = Executors.newSingleThreadExecutor(new NamedThreadFactory("ChessClientSync"));
    }

    /**
     * Joins a game using the default service name.
     *
     * @param host host address
     * @param port RMI registry port
     * @param playerName player name
     * @return assigned player session
     * @throws RemoteException if join fails
     */
    public ChessService.PlayerSession joinGame(String host, int port, String playerName) throws RemoteException {
        return joinGame(host, port, ChessServer.DEFAULT_SERVICE_NAME, playerName);
    }

    /**
     * Joins a game and starts background synchronization.
     *
     * @param host host address
     * @param port RMI registry port
     * @param serviceName service binding name
     * @param playerName player name
     * @return assigned player session
     * @throws RemoteException if join fails
     */
    public ChessService.PlayerSession joinGame(String host, int port, String serviceName, String playerName)
            throws RemoteException {
        synchronized (lock) {
            if (connected) {
                throw new IllegalStateException("Client is already connected.");
            }
        }

        Registry registry = LocateRegistry.getRegistry(host, port);
        ChessService remoteService;
        try {
            remoteService = (ChessService) registry.lookup(serviceName);
        } catch (NotBoundException exception) {
            throw new RemoteException("Chess service is not hosted: " + serviceName, exception);
        }
        ChessService.PlayerSession joinedSession = remoteService.joinGame(playerName);

        synchronized (lock) {
            service = remoteService;
            session = joinedSession;
            connected = true;
            lastMoveNumber = 0;
            syncTask = executorService.submit(this::synchronizeLoop);
        }

        return joinedSession;
    }

    /**
     * Sends a move to the host.
     *
     * @param move move to send
     * @throws RemoteException if sending fails
     */
    public void sendMove(ChessService.RemoteMove move) throws RemoteException {
        ChessService serviceSnapshot;
        String playerIdSnapshot;

        synchronized (lock) {
            ensureConnected();
            serviceSnapshot = service;
            playerIdSnapshot = session.getPlayerId();
        }

        serviceSnapshot.submitMove(playerIdSnapshot, move);
    }

    /**
     * Gets the local player session.
     *
     * @return session, or {@code null} if not connected
     */
    public ChessService.PlayerSession getSession() {
        synchronized (lock) {
            return session;
        }
    }

    /**
     * Checks whether the client is connected.
     *
     * @return {@code true} if connected
     */
    public boolean isConnected() {
        synchronized (lock) {
            return connected;
        }
    }

    /**
     * Leaves the game and stops synchronization.
     */
    @Override
    public void close() {
        ChessService serviceSnapshot;
        String playerIdSnapshot;

        synchronized (lock) {
            connected = false;
            serviceSnapshot = service;
            playerIdSnapshot = session == null ? null : session.getPlayerId();
        }

        if (syncTask != null) {
            syncTask.cancel(true);
        }

        if (serviceSnapshot != null && playerIdSnapshot != null) {
            try {
                serviceSnapshot.leaveGame(playerIdSnapshot);
            } catch (RemoteException exception) {
                // The host may already be unreachable.
            }
        }

        executorService.shutdownNow();
    }

    /**
     * Waits for the sync worker to stop.
     *
     * @param timeout timeout value
     * @param unit timeout unit
     * @return {@code true} if stopped
     * @throws InterruptedException if interrupted while waiting
     */
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executorService.awaitTermination(timeout, unit);
    }

    private void synchronizeLoop() {
        long lastHeartbeat = 0L;

        while (!Thread.currentThread().isInterrupted()) {
            ChessService serviceSnapshot;
            String playerIdSnapshot;

            synchronized (lock) {
                if (!connected || service == null || session == null) {
                    return;
                }

                serviceSnapshot = service;
                playerIdSnapshot = session.getPlayerId();
            }

            try {
                long now = System.currentTimeMillis();
                if (now - lastHeartbeat >= HEARTBEAT_INTERVAL_MILLIS) {
                    serviceSnapshot.heartbeat(playerIdSnapshot);
                    lastHeartbeat = now;
                }

                List<ChessService.RemoteMove> newMoves = serviceSnapshot.getMovesSince(playerIdSnapshot,
                        getLastMoveNumber());
                for (ChessService.RemoteMove move : newMoves) {
                    setLastMoveNumber(move.getMoveNumber());
                    if (!playerIdSnapshot.equals(move.getPlayerId())) {
                        notifyRemoteMove(move);
                    }
                }

                Thread.sleep(POLL_INTERVAL_MILLIS);
            } catch (RemoteException exception) {
                disconnect("Disconnected from host: " + exception.getMessage());
                return;
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private int getLastMoveNumber() {
        synchronized (lock) {
            return lastMoveNumber;
        }
    }

    private void setLastMoveNumber(int lastMoveNumber) {
        synchronized (lock) {
            this.lastMoveNumber = Math.max(this.lastMoveNumber, lastMoveNumber);
        }
    }

    private void disconnect(String message) {
        synchronized (lock) {
            connected = false;
        }

        if (listener != null) {
            listener.onDisconnected(message);
        }
    }

    private void notifyRemoteMove(ChessService.RemoteMove move) {
        if (listener != null) {
            listener.onRemoteMove(move);
        }
    }

    private void ensureConnected() {
        if (!connected || service == null || session == null) {
            throw new IllegalStateException("Client is not connected.");
        }
    }

    private static final class NamedThreadFactory implements ThreadFactory {

        private final String threadName;

        private NamedThreadFactory(String threadName) {
            this.threadName = threadName;
        }

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, threadName);
            thread.setDaemon(true);
            return thread;
        }
    }
}
