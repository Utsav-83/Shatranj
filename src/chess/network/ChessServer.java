package chess.network;

import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * RMI chess host. It owns the shared move log and player connection state.
 */
public class ChessServer extends UnicastRemoteObject implements ChessService {

    private static final long serialVersionUID = 1L;
    public static final String DEFAULT_SERVICE_NAME = "ChessService";
    public static final int DEFAULT_PORT = 1099;
    private static final int MAX_PLAYERS = 2;
    private static final long DISCONNECT_TIMEOUT_MILLIS = 15000L;

    private final transient Object lock;
    private final transient List<PlayerSession> players;
    private final transient List<RemoteMove> moves;
    private final transient Map<String, Long> lastHeartbeatByPlayerId;
    private final transient Set<String> disconnectedPlayerIds;
    private final transient ScheduledExecutorService disconnectExecutor;

    private transient Registry registry;
    private String serviceName;
    private int port;
    private boolean hosted;

    /**
     * Creates an RMI chess server.
     *
     * @throws RemoteException if export fails
     */
    public ChessServer() throws RemoteException {
        super();
        lock = new Object();
        players = new ArrayList<>();
        moves = new ArrayList<>();
        lastHeartbeatByPlayerId = new HashMap<>();
        disconnectedPlayerIds = new HashSet<>();
        disconnectExecutor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("ChessServerMonitor"));
    }

    /**
     * Hosts a game on the default RMI port.
     *
     * @throws RemoteException if hosting fails
     */
    public void hostGame() throws RemoteException {
        hostGame(DEFAULT_PORT, DEFAULT_SERVICE_NAME);
    }

    /**
     * Hosts a game and binds this service in the local registry.
     *
     * @param port RMI registry port
     * @param serviceName service binding name
     * @throws RemoteException if hosting fails
     */
    public void hostGame(int port, String serviceName) throws RemoteException {
        synchronized (lock) {
            if (hosted) {
                return;
            }
        }

        this.port = port;
        this.serviceName = serviceName == null || serviceName.trim().isEmpty() ? DEFAULT_SERVICE_NAME : serviceName;
        registry = createOrGetRegistry(port);

        try {
            registry.bind(this.serviceName, this);
        } catch (AlreadyBoundException exception) {
            registry.rebind(this.serviceName, this);
        }

        synchronized (lock) {
            hosted = true;
        }

        disconnectExecutor.scheduleAtFixedRate(this::markDisconnectedPlayers, 5L, 5L, TimeUnit.SECONDS);
    }

    /**
     * Stops hosting and unbinds the service.
     */
    public void stopServer() {
        synchronized (lock) {
            hosted = false;
        }

        disconnectExecutor.shutdownNow();

        if (registry != null && serviceName != null) {
            try {
                registry.unbind(serviceName);
            } catch (RemoteException | NotBoundException exception) {
                // Service may already be gone; stop remains idempotent.
            }
        }

        try {
            UnicastRemoteObject.unexportObject(this, true);
        } catch (RemoteException exception) {
            // Already unexported.
        }
    }

    @Override
    public PlayerSession joinGame(String playerName) throws RemoteException {
        String safeName = playerName == null || playerName.trim().isEmpty() ? "Player" : playerName.trim();

        synchronized (lock) {
            if (players.size() >= MAX_PLAYERS) {
                throw new RemoteException("Game already has two players.");
            }

            boolean whitePlayer = players.isEmpty();
            PlayerSession session = new PlayerSession(UUID.randomUUID().toString(), safeName, whitePlayer);
            players.add(session);
            lastHeartbeatByPlayerId.put(session.getPlayerId(), System.currentTimeMillis());
            disconnectedPlayerIds.remove(session.getPlayerId());
            return session;
        }
    }

    @Override
    public void leaveGame(String playerId) throws RemoteException {
        synchronized (lock) {
            requireKnownPlayer(playerId);
            disconnectedPlayerIds.add(playerId);
            lastHeartbeatByPlayerId.remove(playerId);
        }
    }

    @Override
    public void submitMove(String playerId, RemoteMove move) throws RemoteException {
        if (move == null) {
            throw new RemoteException("Move cannot be null.");
        }

        synchronized (lock) {
            requireConnectedPlayer(playerId);
            validateRemoteMove(move);
            validateTurn(playerId);
            int moveNumber = moves.size() + 1;
            RemoteMove storedMove = new RemoteMove(moveNumber, playerId, move.getSourceRow(), move.getSourceColumn(),
                    move.getDestinationRow(), move.getDestinationColumn(), move.getPromotionSymbol(),
                    System.currentTimeMillis());
            moves.add(storedMove);
            lastHeartbeatByPlayerId.put(playerId, System.currentTimeMillis());
        }
    }

    @Override
    public List<RemoteMove> getMovesSince(String playerId, int afterMoveNumber) throws RemoteException {
        synchronized (lock) {
            requireConnectedPlayer(playerId);
            lastHeartbeatByPlayerId.put(playerId, System.currentTimeMillis());

            List<RemoteMove> result = new ArrayList<>();
            for (RemoteMove move : moves) {
                if (move.getMoveNumber() > afterMoveNumber) {
                    result.add(move);
                }
            }

            return result;
        }
    }

    @Override
    public void heartbeat(String playerId) throws RemoteException {
        synchronized (lock) {
            requireKnownPlayer(playerId);
            lastHeartbeatByPlayerId.put(playerId, System.currentTimeMillis());
            disconnectedPlayerIds.remove(playerId);
        }
    }

    @Override
    public GameSnapshot getSnapshot() throws RemoteException {
        synchronized (lock) {
            return new GameSnapshot(Collections.unmodifiableList(new ArrayList<>(players)),
                    Collections.unmodifiableList(new ArrayList<>(moves)),
                    Collections.unmodifiableList(new ArrayList<>(disconnectedPlayerIds)));
        }
    }

    private Registry createOrGetRegistry(int port) throws RemoteException {
        try {
            Registry existingRegistry = LocateRegistry.getRegistry(port);
            existingRegistry.list();
            return existingRegistry;
        } catch (RemoteException exception) {
            return LocateRegistry.createRegistry(port);
        }
    }

    private void markDisconnectedPlayers() {
        long now = System.currentTimeMillis();

        synchronized (lock) {
            for (PlayerSession player : players) {
                Long lastHeartbeat = lastHeartbeatByPlayerId.get(player.getPlayerId());
                if (lastHeartbeat != null && now - lastHeartbeat > DISCONNECT_TIMEOUT_MILLIS) {
                    disconnectedPlayerIds.add(player.getPlayerId());
                    lastHeartbeatByPlayerId.remove(player.getPlayerId());
                }
            }
        }
    }

    private void requireKnownPlayer(String playerId) throws RemoteException {
        for (PlayerSession player : players) {
            if (player.getPlayerId().equals(playerId)) {
                return;
            }
        }

        throw new RemoteException("Unknown player.");
    }

    private void requireConnectedPlayer(String playerId) throws RemoteException {
        requireKnownPlayer(playerId);

        if (disconnectedPlayerIds.contains(playerId)) {
            throw new RemoteException("Player is disconnected.");
        }
    }

    private void validateRemoteMove(RemoteMove move) throws RemoteException {
        if (!isBoardCoordinate(move.getSourceRow(), move.getSourceColumn())
                || !isBoardCoordinate(move.getDestinationRow(), move.getDestinationColumn())) {
            throw new RemoteException("Move contains coordinates outside the board.");
        }

        String promotionSymbol = move.getPromotionSymbol();
        if (promotionSymbol != null && !"Q".equals(promotionSymbol) && !"R".equals(promotionSymbol)
                && !"B".equals(promotionSymbol) && !"N".equals(promotionSymbol)) {
            throw new RemoteException("Invalid promotion symbol.");
        }
    }

    private void validateTurn(String playerId) throws RemoteException {
        PlayerSession player = findPlayer(playerId);
        boolean whiteToMove = moves.size() % 2 == 0;

        if (player == null || player.isWhitePlayer() != whiteToMove) {
            throw new RemoteException("It is not this player's turn.");
        }
    }

    private PlayerSession findPlayer(String playerId) {
        for (PlayerSession player : players) {
            if (player.getPlayerId().equals(playerId)) {
                return player;
            }
        }

        return null;
    }

    private boolean isBoardCoordinate(int row, int column) {
        return row >= 0 && row < 8 && column >= 0 && column < 8;
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
