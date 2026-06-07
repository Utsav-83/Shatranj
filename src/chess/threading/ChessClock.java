package chess.threading;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Runs a chess clock on its own worker thread.
 * <p>
 * All mutable clock state is protected by {@code lock}. Listener callbacks are
 * invoked outside synchronized blocks to avoid deadlocks with UI code.
 */
public class ChessClock implements AutoCloseable {

    private static final long TICK_SLEEP_MILLIS = 100L;

    private final Object lock;
    private final ExecutorService executorService;
    private final ClockListener listener;

    private Future<?> clockTask;
    private long whiteRemainingMillis;
    private long blackRemainingMillis;
    private boolean whiteTurn;
    private boolean running;
    private boolean closed;

    /**
     * Receives clock updates from the worker thread.
     */
    public interface ClockListener {

        /**
         * Called after the active player's time changes.
         *
         * @param whiteRemainingMillis white time remaining
         * @param blackRemainingMillis black time remaining
         * @param whiteTurn {@code true} when white is the active clock
         */
        void onTick(long whiteRemainingMillis, long blackRemainingMillis, boolean whiteTurn);

        /**
         * Called once when a player's clock reaches zero.
         *
         * @param whiteFlagged {@code true} when white ran out of time
         */
        void onTimeExpired(boolean whiteFlagged);
    }

    /**
     * Creates a clock with equal time for both players.
     *
     * @param initialMillis initial time in milliseconds
     * @param listener callback listener, or {@code null}
     */
    public ChessClock(long initialMillis, ClockListener listener) {
        this(initialMillis, initialMillis, listener);
    }

    /**
     * Creates a clock with separate white and black times.
     *
     * @param whiteInitialMillis white initial time
     * @param blackInitialMillis black initial time
     * @param listener callback listener, or {@code null}
     */
    public ChessClock(long whiteInitialMillis, long blackInitialMillis, ClockListener listener) {
        if (whiteInitialMillis < 0 || blackInitialMillis < 0) {
            throw new IllegalArgumentException("Clock times cannot be negative.");
        }

        this.lock = new Object();
        this.whiteRemainingMillis = whiteInitialMillis;
        this.blackRemainingMillis = blackInitialMillis;
        this.whiteTurn = true;
        this.listener = listener;
        this.executorService = Executors.newSingleThreadExecutor(new NamedThreadFactory("ChessClock"));
    }

    /**
     * Starts or resumes the clock.
     */
    public void start() {
        synchronized (lock) {
            ensureOpen();
            if (running) {
                return;
            }

            running = true;
            clockTask = executorService.submit(this::runClock);
        }
    }

    /**
     * Pauses the clock without resetting remaining time.
     */
    public void pause() {
        synchronized (lock) {
            running = false;
        }
    }

    /**
     * Switches the active side after a completed move.
     */
    public void switchTurn() {
        synchronized (lock) {
            whiteTurn = !whiteTurn;
        }
        publishTick();
    }

    /**
     * Sets the active side explicitly.
     *
     * @param whiteTurn {@code true} to make white active
     */
    public void setWhiteTurn(boolean whiteTurn) {
        synchronized (lock) {
            this.whiteTurn = whiteTurn;
        }
        publishTick();
    }

    /**
     * Resets both clocks and makes white active.
     *
     * @param whiteMillis white time
     * @param blackMillis black time
     */
    public void reset(long whiteMillis, long blackMillis) {
        if (whiteMillis < 0 || blackMillis < 0) {
            throw new IllegalArgumentException("Clock times cannot be negative.");
        }

        synchronized (lock) {
            whiteRemainingMillis = whiteMillis;
            blackRemainingMillis = blackMillis;
            whiteTurn = true;
            running = false;
        }
        publishTick();
    }

    /**
     * Gets white's remaining time.
     *
     * @return white time in milliseconds
     */
    public long getWhiteRemainingMillis() {
        synchronized (lock) {
            return whiteRemainingMillis;
        }
    }

    /**
     * Gets black's remaining time.
     *
     * @return black time in milliseconds
     */
    public long getBlackRemainingMillis() {
        synchronized (lock) {
            return blackRemainingMillis;
        }
    }

    /**
     * Checks whether the clock is currently running.
     *
     * @return {@code true} if running
     */
    public boolean isRunning() {
        synchronized (lock) {
            return running;
        }
    }

    /**
     * Stops the clock worker and releases its executor.
     */
    @Override
    public void close() {
        synchronized (lock) {
            closed = true;
            running = false;
        }

        if (clockTask != null) {
            clockTask.cancel(true);
        }

        executorService.shutdownNow();
    }

    /**
     * Waits briefly for the clock worker to stop.
     *
     * @param timeout timeout value
     * @param unit timeout unit
     * @return {@code true} if stopped
     * @throws InterruptedException if interrupted while waiting
     */
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executorService.awaitTermination(timeout, unit);
    }

    private void runClock() {
        long lastUpdate = System.currentTimeMillis();

        while (!Thread.currentThread().isInterrupted()) {
            boolean expired = false;
            boolean expiredWhite = false;
            long whiteSnapshot;
            long blackSnapshot;
            boolean whiteTurnSnapshot;

            synchronized (lock) {
                if (!running || closed) {
                    return;
                }

                long now = System.currentTimeMillis();
                long elapsed = now - lastUpdate;
                lastUpdate = now;

                if (whiteTurn) {
                    whiteRemainingMillis = Math.max(0L, whiteRemainingMillis - elapsed);
                    expired = whiteRemainingMillis == 0L;
                    expiredWhite = true;
                } else {
                    blackRemainingMillis = Math.max(0L, blackRemainingMillis - elapsed);
                    expired = blackRemainingMillis == 0L;
                }

                if (expired) {
                    running = false;
                }

                whiteSnapshot = whiteRemainingMillis;
                blackSnapshot = blackRemainingMillis;
                whiteTurnSnapshot = whiteTurn;
            }

            notifyTick(whiteSnapshot, blackSnapshot, whiteTurnSnapshot);

            if (expired) {
                notifyExpired(expiredWhite);
                return;
            }

            try {
                Thread.sleep(TICK_SLEEP_MILLIS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void publishTick() {
        long whiteSnapshot;
        long blackSnapshot;
        boolean whiteTurnSnapshot;

        synchronized (lock) {
            whiteSnapshot = whiteRemainingMillis;
            blackSnapshot = blackRemainingMillis;
            whiteTurnSnapshot = whiteTurn;
        }

        notifyTick(whiteSnapshot, blackSnapshot, whiteTurnSnapshot);
    }

    private void notifyTick(long whiteMillis, long blackMillis, boolean activeWhite) {
        if (listener != null) {
            listener.onTick(whiteMillis, blackMillis, activeWhite);
        }
    }

    private void notifyExpired(boolean whiteFlagged) {
        if (listener != null) {
            listener.onTimeExpired(whiteFlagged);
        }
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Clock is already closed.");
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
