package chess.threading;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Handles chat messages on a separate worker thread.
 * <p>
 * Message submission is thread-safe. Chat history is guarded by {@code lock}
 * and exposed as a defensive copy to prevent race conditions.
 */
public class ChatThread implements AutoCloseable {

    private final Object lock;
    private final BlockingQueue<ChatMessage> outgoingQueue;
    private final List<ChatMessage> messageHistory;
    private final ExecutorService executorService;
    private final ChatListener listener;

    private Future<?> chatTask;
    private boolean running;
    private boolean closed;

    /**
     * Receives chat events from the chat worker thread.
     */
    public interface ChatListener {

        /**
         * Called when a message is accepted and stored.
         *
         * @param message stored chat message
         */
        void onMessage(ChatMessage message);
    }

    /**
     * Immutable chat message value.
     */
    public static final class ChatMessage {

        private final String sender;
        private final String text;
        private final LocalDateTime timestamp;

        /**
         * Creates a chat message.
         *
         * @param sender sender name
         * @param text message text
         * @param timestamp message time
         */
        public ChatMessage(String sender, String text, LocalDateTime timestamp) {
            if (sender == null || sender.trim().isEmpty()) {
                throw new IllegalArgumentException("Sender cannot be empty.");
            }

            if (text == null || text.trim().isEmpty()) {
                throw new IllegalArgumentException("Message text cannot be empty.");
            }

            if (timestamp == null) {
                throw new IllegalArgumentException("Timestamp cannot be null.");
            }

            this.sender = sender.trim();
            this.text = text.trim();
            this.timestamp = timestamp;
        }

        /**
         * Gets the sender.
         *
         * @return sender name
         */
        public String getSender() {
            return sender;
        }

        /**
         * Gets the message text.
         *
         * @return message text
         */
        public String getText() {
            return text;
        }

        /**
         * Gets the timestamp.
         *
         * @return timestamp
         */
        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        @Override
        public String toString() {
            return "[" + timestamp + "] " + sender + ": " + text;
        }
    }

    /**
     * Creates a chat worker.
     *
     * @param listener listener callback, or {@code null}
     */
    public ChatThread(ChatListener listener) {
        this.lock = new Object();
        this.outgoingQueue = new LinkedBlockingQueue<>();
        this.messageHistory = new ArrayList<>();
        this.listener = listener;
        this.executorService = Executors.newSingleThreadExecutor(new NamedThreadFactory("ChatThread"));
    }

    /**
     * Starts the chat worker.
     */
    public void start() {
        synchronized (lock) {
            ensureOpen();
            if (running) {
                return;
            }

            running = true;
            chatTask = executorService.submit(this::processMessages);
        }
    }

    /**
     * Sends a chat message asynchronously.
     *
     * @param sender sender name
     * @param text message text
     */
    public void sendMessage(String sender, String text) {
        ChatMessage message = new ChatMessage(sender, text, LocalDateTime.now());

        synchronized (lock) {
            ensureOpen();
        }

        outgoingQueue.offer(message);
    }

    /**
     * Gets a stable snapshot of chat history.
     *
     * @return immutable copy of message history
     */
    public List<ChatMessage> getMessageHistory() {
        synchronized (lock) {
            return Collections.unmodifiableList(new ArrayList<>(messageHistory));
        }
    }

    /**
     * Clears stored message history.
     */
    public void clearHistory() {
        synchronized (lock) {
            messageHistory.clear();
        }
    }

    /**
     * Stops the chat worker and clears pending outgoing messages.
     */
    @Override
    public void close() {
        synchronized (lock) {
            closed = true;
            running = false;
        }

        outgoingQueue.clear();

        if (chatTask != null) {
            chatTask.cancel(true);
        }

        executorService.shutdownNow();
    }

    /**
     * Waits briefly for the chat worker to stop.
     *
     * @param timeout timeout value
     * @param unit timeout unit
     * @return {@code true} if stopped
     * @throws InterruptedException if interrupted while waiting
     */
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executorService.awaitTermination(timeout, unit);
    }

    private void processMessages() {
        while (!Thread.currentThread().isInterrupted()) {
            synchronized (lock) {
                if (!running || closed) {
                    return;
                }
            }

            try {
                ChatMessage message = outgoingQueue.take();
                storeAndPublish(message);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void storeAndPublish(ChatMessage message) {
        synchronized (lock) {
            messageHistory.add(message);
        }

        if (listener != null) {
            listener.onMessage(message);
        }
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Chat thread is already closed.");
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
