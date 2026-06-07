package chess.threading;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import chess.board.Position;
import chess.move.Move;

/**
 * Processes move requests on a separate worker thread.
 * <p>
 * Producers call {@link #submitMove(Move)} from any thread. The internal
 * blocking queue serializes requests, so only one move is handled at a time.
 */
public class MoveListenerThread implements AutoCloseable {

    private final Object lifecycleLock;
    private final BlockingQueue<Move> moveQueue;
    private final ExecutorService executorService;
    private final MoveHandler moveHandler;

    private Future<?> listenerTask;
    private boolean running;
    private boolean closed;

    /**
     * Handles moves on the listener worker thread.
     */
    public interface MoveHandler {

        /**
         * Processes one queued move.
         *
         * @param move move request
         */
        void onMoveReceived(Move move);
    }

    /**
     * Creates a move listener.
     *
     * @param moveHandler handler to receive moves
     */
    public MoveListenerThread(MoveHandler moveHandler) {
        if (moveHandler == null) {
            throw new IllegalArgumentException("Move handler cannot be null.");
        }

        this.lifecycleLock = new Object();
        this.moveQueue = new LinkedBlockingQueue<>();
        this.moveHandler = moveHandler;
        this.executorService = Executors.newSingleThreadExecutor(new NamedThreadFactory("MoveListenerThread"));
    }

    /**
     * Starts the listener thread.
     */
    public void start() {
        synchronized (lifecycleLock) {
            ensureOpen();
            if (running) {
                return;
            }

            running = true;
            listenerTask = executorService.submit(this::listenForMoves);
        }
    }

    /**
     * Queues a move for asynchronous handling.
     *
     * @param move move to submit
     */
    public void submitMove(Move move) {
        if (move == null) {
            throw new IllegalArgumentException("Move cannot be null.");
        }

        synchronized (lifecycleLock) {
            ensureOpen();
        }

        moveQueue.offer(copyMove(move));
    }

    /**
     * Returns the number of queued moves waiting to be processed.
     *
     * @return pending move count
     */
    public int getPendingMoveCount() {
        return moveQueue.size();
    }

    /**
     * Stops the worker and clears queued moves.
     */
    @Override
    public void close() {
        synchronized (lifecycleLock) {
            closed = true;
            running = false;
        }

        moveQueue.clear();

        if (listenerTask != null) {
            listenerTask.cancel(true);
        }

        executorService.shutdownNow();
    }

    /**
     * Waits briefly for the listener worker to stop.
     *
     * @param timeout timeout value
     * @param unit timeout unit
     * @return {@code true} if stopped
     * @throws InterruptedException if interrupted while waiting
     */
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executorService.awaitTermination(timeout, unit);
    }

    private void listenForMoves() {
        while (!Thread.currentThread().isInterrupted()) {
            synchronized (lifecycleLock) {
                if (!running || closed) {
                    return;
                }
            }

            try {
                Move move = moveQueue.take();
                moveHandler.onMoveReceived(move);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private Move copyMove(Move original) {
        Position source = original.getSource();
        Position destination = original.getDestination();
        Move copy = new Move(source, destination);
        copy.setMovedPiece(original.getMovedPiece());
        copy.setCapturedPiece(original.getCapturedPiece());
        copy.setEnPassantCapturedPosition(original.getEnPassantCapturedPosition());
        copy.setPromotionSymbol(original.getPromotionSymbol());
        return copy;
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Move listener is already closed.");
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
