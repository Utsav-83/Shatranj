package chess.audio;

import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Plays chess sound effects asynchronously.
 * <p>
 * Custom WAV files can be registered per sound event. If no file is available,
 * the manager plays a short generated tone so the feature works without extra
 * assets.
 */
public class SoundManager implements AutoCloseable {

    private static final int SAMPLE_RATE = 44100;
    private static final int GENERATED_VOLUME = 48;

    private final Map<SoundEvent, URL> soundUrls;
    private final ExecutorService executorService;
    private volatile boolean enabled;
    private volatile boolean closed;

    /**
     * Chess sound events.
     */
    public enum SoundEvent {
        MOVE,
        CAPTURE,
        CHECK,
        CHECKMATE,
        PROMOTION
    }

    /**
     * Creates a sound manager.
     */
    public SoundManager() {
        soundUrls = new EnumMap<>(SoundEvent.class);
        executorService = Executors.newSingleThreadExecutor(new NamedThreadFactory("SoundManager"));
        enabled = true;
        loadDefaultResources();
    }

    /**
     * Enables or disables sound playback.
     *
     * @param enabled {@code true} to enable sounds
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Checks whether sound is enabled.
     *
     * @return {@code true} if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Registers a WAV file for a sound event.
     *
     * @param event sound event
     * @param file WAV file
     */
    public void registerSound(SoundEvent event, File file) {
        if (event == null || file == null) {
            throw new IllegalArgumentException("Sound event and file are required.");
        }

        try {
            soundUrls.put(event, file.toURI().toURL());
        } catch (IOException exception) {
            throw new IllegalArgumentException("Invalid sound file: " + file, exception);
        }
    }

    /**
     * Registers a classpath WAV resource for a sound event.
     *
     * @param event sound event
     * @param resourcePath classpath resource path
     */
    public void registerSound(SoundEvent event, String resourcePath) {
        if (event == null || resourcePath == null || resourcePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Sound event and resource path are required.");
        }

        URL resource = SoundManager.class.getResource(resourcePath);
        if (resource == null) {
            throw new IllegalArgumentException("Sound resource not found: " + resourcePath);
        }

        soundUrls.put(event, resource);
    }

    /**
     * Plays the standard move sound.
     */
    public void playMoveSound() {
        play(SoundEvent.MOVE);
    }

    /**
     * Plays the capture sound.
     */
    public void playCaptureSound() {
        play(SoundEvent.CAPTURE);
    }

    /**
     * Plays the check sound.
     */
    public void playCheckSound() {
        play(SoundEvent.CHECK);
    }

    /**
     * Plays the checkmate sound.
     */
    public void playCheckmateSound() {
        play(SoundEvent.CHECKMATE);
    }

    /**
     * Plays the promotion sound.
     */
    public void playPromotionSound() {
        play(SoundEvent.PROMOTION);
    }

    /**
     * Plays a sound event asynchronously.
     *
     * @param event event to play
     */
    public void play(SoundEvent event) {
        if (event == null || !enabled || closed) {
            return;
        }

        try {
            executorService.submit(() -> playNow(event));
        } catch (RejectedExecutionException exception) {
            // Playback was requested during shutdown; safely ignore it.
        }
    }

    /**
     * Stops accepting new playback tasks.
     */
    @Override
    public void close() {
        closed = true;
        executorService.shutdownNow();
    }

    private void loadDefaultResources() {
        registerDefaultResource(SoundEvent.MOVE, "/sounds/move.wav");
        registerDefaultResource(SoundEvent.CAPTURE, "/sounds/capture.wav");
        registerDefaultResource(SoundEvent.CHECK, "/sounds/check.wav");
        registerDefaultResource(SoundEvent.CHECKMATE, "/sounds/checkmate.wav");
        registerDefaultResource(SoundEvent.PROMOTION, "/sounds/promotion.wav");
    }

    private void registerDefaultResource(SoundEvent event, String resourcePath) {
        URL resource = SoundManager.class.getResource(resourcePath);
        if (resource != null) {
            soundUrls.put(event, resource);
        }
    }

    private void playNow(SoundEvent event) {
        URL soundUrl = soundUrls.get(event);

        if (soundUrl != null && playClip(soundUrl)) {
            return;
        }

        playGeneratedTone(event);
    }

    private boolean playClip(URL soundUrl) {
        Clip clip = null;
        try (AudioInputStream inputStream = AudioSystem.getAudioInputStream(soundUrl)) {
            clip = AudioSystem.getClip();
            clip.open(inputStream);
            clip.start();

            while (clip.isRunning()) {
                Thread.sleep(10L);
            }

            clip.close();
            return true;
        } catch (IOException | LineUnavailableException | UnsupportedAudioFileException exception) {
            return false;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return true;
        } finally {
            if (clip != null && clip.isOpen()) {
                clip.close();
            }
        }
    }

    private void playGeneratedTone(SoundEvent event) {
        Tone tone = toneFor(event);
        AudioFormat format = new AudioFormat(SAMPLE_RATE, 8, 1, true, false);
        SourceDataLine line = null;

        try {
            line = AudioSystem.getSourceDataLine(format);
            line.open(format);
            line.start();

            for (int frequency : tone.frequencies) {
                writeTone(line, frequency, tone.durationMillis);
            }

            line.drain();
            line.stop();
        } catch (LineUnavailableException exception) {
            Toolkit.getDefaultToolkit().beep();
        } finally {
            if (line != null) {
                line.close();
            }
        }
    }

    private void writeTone(SourceDataLine line, int frequency, int durationMillis) {
        int sampleCount = (durationMillis * SAMPLE_RATE) / 1000;
        byte[] data = new byte[sampleCount];

        for (int index = 0; index < sampleCount; index++) {
            double angle = (2.0 * Math.PI * index * frequency) / SAMPLE_RATE;
            data[index] = (byte) (Math.sin(angle) * GENERATED_VOLUME);
        }

        line.write(data, 0, data.length);
    }

    private Tone toneFor(SoundEvent event) {
        switch (event) {
            case CAPTURE:
                return new Tone(new int[] {220, 180}, 70);
            case CHECK:
                return new Tone(new int[] {740, 880}, 90);
            case CHECKMATE:
                return new Tone(new int[] {880, 660, 440}, 120);
            case PROMOTION:
                return new Tone(new int[] {523, 659, 784}, 85);
            case MOVE:
            default:
                return new Tone(new int[] {440}, 70);
        }
    }

    private static final class Tone {

        private final int[] frequencies;
        private final int durationMillis;

        private Tone(int[] frequencies, int durationMillis) {
            this.frequencies = frequencies;
            this.durationMillis = durationMillis;
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
