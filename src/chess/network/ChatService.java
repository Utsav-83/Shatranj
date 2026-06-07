package chess.network;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

/**
 * RMI contract for multiplayer chat.
 */
public interface ChatService extends Remote {

    /**
     * Registers a player in the chat service.
     *
     * @param playerName player display name
     * @return chat session id
     * @throws RemoteException if the remote call fails
     */
    String joinChat(String playerName) throws RemoteException;

    /**
     * Removes a player from the chat service.
     *
     * @param sessionId chat session id
     * @throws RemoteException if the remote call fails
     */
    void leaveChat(String sessionId) throws RemoteException;

    /**
     * Sends a chat message.
     *
     * @param sessionId sender session id
     * @param text message text
     * @throws RemoteException if the remote call fails
     */
    void sendMessage(String sessionId, String text) throws RemoteException;

    /**
     * Gets messages newer than the supplied message id.
     *
     * @param sessionId chat session id
     * @param afterMessageId last message already received, or {@code 0}
     * @return new messages
     * @throws RemoteException if the remote call fails
     */
    List<ChatMessage> getMessagesSince(String sessionId, int afterMessageId) throws RemoteException;

    /**
     * Updates the player's connection heartbeat.
     *
     * @param sessionId chat session id
     * @throws RemoteException if the remote call fails
     */
    void heartbeat(String sessionId) throws RemoteException;

    /**
     * Gets the complete message history.
     *
     * @return all stored chat messages
     * @throws RemoteException if the remote call fails
     */
    List<ChatMessage> getMessageHistory() throws RemoteException;

    /**
     * Immutable RMI chat message.
     */
    final class ChatMessage implements Serializable {

        private static final long serialVersionUID = 1L;

        private final int messageId;
        private final String senderName;
        private final String text;
        private final long timestampMillis;
        private final boolean systemMessage;

        /**
         * Creates a message.
         *
         * @param messageId message id assigned by the server
         * @param senderName sender name
         * @param text message text
         * @param timestampMillis server timestamp
         * @param systemMessage {@code true} for join/leave/server messages
         */
        public ChatMessage(int messageId, String senderName, String text, long timestampMillis,
                boolean systemMessage) {
            this.messageId = messageId;
            this.senderName = senderName;
            this.text = text;
            this.timestampMillis = timestampMillis;
            this.systemMessage = systemMessage;
        }

        public int getMessageId() {
            return messageId;
        }

        public String getSenderName() {
            return senderName;
        }

        public String getText() {
            return text;
        }

        public long getTimestampMillis() {
            return timestampMillis;
        }

        public boolean isSystemMessage() {
            return systemMessage;
        }
    }

    /**
     * Serializable chat state snapshot.
     */
    final class ChatSnapshot implements Serializable {

        private static final long serialVersionUID = 1L;

        private final ArrayList<ChatMessage> messages;
        private final ArrayList<String> connectedPlayers;

        /**
         * Creates a snapshot.
         *
         * @param messages chat messages
         * @param connectedPlayers connected player names
         */
        public ChatSnapshot(List<ChatMessage> messages, List<String> connectedPlayers) {
            this.messages = new ArrayList<>(messages);
            this.connectedPlayers = new ArrayList<>(connectedPlayers);
        }

        public List<ChatMessage> getMessages() {
            return new ArrayList<>(messages);
        }

        public List<String> getConnectedPlayers() {
            return new ArrayList<>(connectedPlayers);
        }
    }
}
