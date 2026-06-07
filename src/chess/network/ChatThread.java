package chess.network;

import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * RMI chat host/client helper with background polling for real-time messages.
 */
public class ChatThread extends UnicastRemoteObject implements ChatService, AutoCloseable {

    private static final long serialVersionUID = 1L;
    public static final String DEFAULT_SERVICE_NAME = "ChessChatService";
    public static final int DEFAULT_PORT = 1100;
    private static final long POLL_INTERVAL_MILLIS = 500L;
    private static final long HEARTBEAT_INTERVAL_MILLIS = 3000L;
    private static final long DISCONNECT_TIMEOUT_MILLIS = 15000L;

    private final transient Object lock;
    private final transient List<ChatMessage> serverMessageHistory;
    private final transient List<ChatMessage> localMessageHistory;
    private final transient Map<String, String> playerNamesBySessionId;
    private final transient Map<String, Long> lastHeartbeatBySessionId;
    private final transient Set<String> disconnectedSessionIds;
    private final transient ExecutorService executorService;
    private final transient ChatListener listener;

    private transient Registry registry;
    private transient ChatService remoteService;
    private transient Future<?> pollingTask;
    private String serviceName;
    private String sessionId;
    private String playerName;
    private int lastMessageId;
    private boolean hosting;
    private boolean connected;

    /**
     * Receives chat events from the polling worker.
     */
    public interface ChatListener {

        /**
         * Called when a new message arrives.
         *
         * @param message chat message
         */
        void onMessageReceived(ChatMessage message);

        /**
         * Called when chat disconnects.
         *
         * @param message disconnect reason
         */
        void onDisconnected(String message);
    }

    /**
     * Creates a chat thread.
     *
     * @param listener listener callback, or {@code null}
     * @throws RemoteException if RMI export fails
     */
    public ChatThread(ChatListener listener) throws RemoteException {
        super();
        lock = new Object();
        serverMessageHistory = new ArrayList<>();
        localMessageHistory = new ArrayList<>();
        playerNamesBySessionId = new HashMap<>();
        lastHeartbeatBySessionId = new HashMap<>();
        disconnectedSessionIds = new HashSet<>();
        executorService = Executors.newSingleThreadExecutor(new NamedThreadFactory("RmiChatThread"));
        this.listener = listener;
    }

    /**
     * Hosts chat locally and joins as the host player.
     *
     * @param playerName host player name
     * @throws RemoteException if hosting fails
     */
    public void hostChat(String playerName) throws RemoteException {
        hostChat(playerName, DEFAULT_PORT, DEFAULT_SERVICE_NAME);
    }

    /**
     * Hosts chat locally and joins as the host player.
     *
     * @param playerName host player name
     * @param port RMI registry port
     * @param serviceName service name
     * @throws RemoteException if hosting fails
     */
    public void hostChat(String playerName, int port, String serviceName) throws RemoteException {
        String bindingName = normalizeServiceName(serviceName);
        registry = createOrGetRegistry(port);

        try {
            registry.bind(bindingName, this);
        } catch (AlreadyBoundException exception) {
            registry.rebind(bindingName, this);
        }

        synchronized (lock) {
            hosting = true;
            this.serviceName = bindingName;
            remoteService = this;
        }

        connectToService(this, playerName);
    }

    /**
     * Joins a remote chat service.
     *
     * @param host host address
     * @param playerName player name
     * @throws RemoteException if join fails
     */
    public void joinChat(String host, String playerName) throws RemoteException {
        joinChat(host, DEFAULT_PORT, DEFAULT_SERVICE_NAME, playerName);
    }

    /**
     * Joins a remote chat service.
     *
     * @param host host address
     * @param port RMI registry port
     * @param serviceName service name
     * @param playerName player name
     * @throws RemoteException if join fails
     */
    public void joinChat(String host, int port, String serviceName, String playerName) throws RemoteException {
        Registry remoteRegistry = LocateRegistry.getRegistry(host, port);
        ChatService service;

        try {
            service = (ChatService) remoteRegistry.lookup(normalizeServiceName(serviceName));
        } catch (NotBoundException exception) {
            throw new RemoteException("Chat service is not hosted: " + serviceName, exception);
        }

        connectToService(service, playerName);
    }

    /**
     * Sends a chat message through the connected service.
     *
     * @param text message text
     * @throws RemoteException if sending fails
     */
    public void sendChatMessage(String text) throws RemoteException {
        ChatService serviceSnapshot;
        String sessionSnapshot;

        synchronized (lock) {
            ensureConnected();
            serviceSnapshot = remoteService;
            sessionSnapshot = sessionId;
        }

        serviceSnapshot.sendMessage(sessionSnapshot, text);
    }

    /**
     * Gets local cached message history.
     *
     * @return message history copy
     */
    public List<ChatMessage> getLocalMessageHistory() {
        synchronized (lock) {
            return new ArrayList<>(localMessageHistory);
        }
    }

    @Override
    public String joinChat(String playerName) throws RemoteException {
        String safeName = normalizePlayerName(playerName);
        String newSessionId = UUID.randomUUID().toString();

        synchronized (lock) {
            playerNamesBySessionId.put(newSessionId, safeName);
            lastHeartbeatBySessionId.put(newSessionId, System.currentTimeMillis());
            disconnectedSessionIds.remove(newSessionId);
            appendMessage("System", safeName + " joined chat.", true);
        }

        return newSessionId;
    }

    @Override
    public void leaveChat(String sessionId) throws RemoteException {
        synchronized (lock) {
            requireKnownSession(sessionId);
            String leavingPlayer = playerNamesBySessionId.get(sessionId);
            disconnectedSessionIds.add(sessionId);
            lastHeartbeatBySessionId.remove(sessionId);
            appendMessage("System", leavingPlayer + " left chat.", true);
        }
    }

    @Override
    public void sendMessage(String sessionId, String text) throws RemoteException {
        String cleanText = text == null ? "" : text.trim();
        if (cleanText.isEmpty()) {
            throw new RemoteException("Message cannot be empty.");
        }

        synchronized (lock) {
            requireConnectedSession(sessionId);
            lastHeartbeatBySessionId.put(sessionId, System.currentTimeMillis());
            appendMessage(playerNamesBySessionId.get(sessionId), cleanText, false);
        }
    }

    @Override
    public List<ChatMessage> getMessagesSince(String sessionId, int afterMessageId) throws RemoteException {
        synchronized (lock) {
            requireConnectedSession(sessionId);
            lastHeartbeatBySessionId.put(sessionId, System.currentTimeMillis());

            List<ChatMessage> result = new ArrayList<>();
            for (ChatMessage message : serverMessageHistory) {
                if (message.getMessageId() > afterMessageId) {
                    result.add(message);
                }
            }

            return result;
        }
    }

    @Override
    public void heartbeat(String sessionId) throws RemoteException {
        synchronized (lock) {
            requireKnownSession(sessionId);
            lastHeartbeatBySessionId.put(sessionId, System.currentTimeMillis());
            disconnectedSessionIds.remove(sessionId);
        }
    }

    @Override
    public List<ChatMessage> getMessageHistory() throws RemoteException {
        synchronized (lock) {
            return new ArrayList<>(serverMessageHistory);
        }
    }

    /**
     * Stops chat, unbinds hosted service, and shuts down the polling worker.
     */
    @Override
    public void close() {
        ChatService serviceSnapshot;
        String sessionSnapshot;
        boolean hostingSnapshot;
        String serviceNameSnapshot;

        synchronized (lock) {
            connected = false;
            serviceSnapshot = remoteService;
            sessionSnapshot = sessionId;
            hostingSnapshot = hosting;
            serviceNameSnapshot = serviceName;
        }

        if (pollingTask != null) {
            pollingTask.cancel(true);
        }

        if (serviceSnapshot != null && sessionSnapshot != null) {
            try {
                serviceSnapshot.leaveChat(sessionSnapshot);
            } catch (RemoteException exception) {
                // Remote chat may already be unavailable.
            }
        }

        if (hostingSnapshot && registry != null && serviceNameSnapshot != null) {
            try {
                registry.unbind(serviceNameSnapshot);
            } catch (RemoteException | NotBoundException exception) {
                // Binding may already be gone.
            }
        }

        executorService.shutdownNow();

        try {
            UnicastRemoteObject.unexportObject(this, true);
        } catch (RemoteException exception) {
            // Already unexported.
        }
    }

    private void connectToService(ChatService service, String playerName) throws RemoteException {
        synchronized (lock) {
            if (connected) {
                throw new IllegalStateException("Chat is already connected.");
            }
        }

        String joinedSessionId = service.joinChat(playerName);

        synchronized (lock) {
            remoteService = service;
            sessionId = joinedSessionId;
            this.playerName = normalizePlayerName(playerName);
            connected = true;
            lastMessageId = 0;
            localMessageHistory.clear();
            pollingTask = executorService.submit(this::pollMessages);
        }
    }

    private void pollMessages() {
        long lastHeartbeat = 0L;

        while (!Thread.currentThread().isInterrupted()) {
            ChatService serviceSnapshot;
            String sessionSnapshot;

            synchronized (lock) {
                if (!connected || remoteService == null || sessionId == null) {
                    return;
                }

                serviceSnapshot = remoteService;
                sessionSnapshot = sessionId;
            }

            try {
                long now = System.currentTimeMillis();
                if (now - lastHeartbeat >= HEARTBEAT_INTERVAL_MILLIS) {
                    serviceSnapshot.heartbeat(sessionSnapshot);
                    lastHeartbeat = now;
                }

                List<ChatMessage> newMessages = serviceSnapshot.getMessagesSince(sessionSnapshot, getLastMessageId());
                for (ChatMessage message : newMessages) {
                    rememberRemoteMessage(message);
                    notifyMessageReceived(message);
                }

                if (hosting) {
                    markDisconnectedSessions();
                }

                Thread.sleep(POLL_INTERVAL_MILLIS);
            } catch (RemoteException exception) {
                disconnect("Chat disconnected: " + exception.getMessage());
                return;
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void appendMessage(String senderName, String text, boolean systemMessage) {
        int messageId = serverMessageHistory.size() + 1;
        serverMessageHistory.add(new ChatMessage(messageId, senderName, text, System.currentTimeMillis(),
                systemMessage));
    }

    private void rememberRemoteMessage(ChatMessage message) {
        synchronized (lock) {
            if (message.getMessageId() <= lastMessageId) {
                return;
            }

            localMessageHistory.add(message);
            lastMessageId = message.getMessageId();
        }
    }

    private int getLastMessageId() {
        synchronized (lock) {
            return lastMessageId;
        }
    }

    private void markDisconnectedSessions() {
        long now = System.currentTimeMillis();

        synchronized (lock) {
            for (Map.Entry<String, Long> entry : new ArrayList<>(lastHeartbeatBySessionId.entrySet())) {
                if (now - entry.getValue() > DISCONNECT_TIMEOUT_MILLIS) {
                    String disconnectedSessionId = entry.getKey();
                    if (disconnectedSessionIds.add(disconnectedSessionId)) {
                        String disconnectedPlayer = playerNamesBySessionId.get(disconnectedSessionId);
                        appendMessage("System", disconnectedPlayer + " disconnected.", true);
                    }

                    lastHeartbeatBySessionId.remove(disconnectedSessionId);
                }
            }
        }
    }

    private void notifyMessageReceived(ChatMessage message) {
        if (listener != null) {
            listener.onMessageReceived(message);
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

    private void requireKnownSession(String sessionId) throws RemoteException {
        if (sessionId == null || !playerNamesBySessionId.containsKey(sessionId)) {
            throw new RemoteException("Unknown chat session.");
        }
    }

    private void requireConnectedSession(String sessionId) throws RemoteException {
        requireKnownSession(sessionId);

        if (disconnectedSessionIds.contains(sessionId)) {
            throw new RemoteException("Chat session is disconnected.");
        }
    }

    private void ensureConnected() {
        if (!connected || remoteService == null || sessionId == null) {
            throw new IllegalStateException("Chat is not connected.");
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

    private String normalizePlayerName(String playerName) {
        return playerName == null || playerName.trim().isEmpty() ? "Player" : playerName.trim();
    }

    private String normalizeServiceName(String serviceName) {
        return serviceName == null || serviceName.trim().isEmpty() ? DEFAULT_SERVICE_NAME : serviceName.trim();
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
