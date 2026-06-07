package chess.persistence;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.util.List;

import chess.board.ChessBoard;
import chess.move.Move;

/**
 * Saves chess games using Java object serialization.
 */
public class SaveGame {

    /**
     * Saves an already-built game state.
     *
     * @param gameState state to save
     * @param file target file
     * @throws IOException if saving fails
     */
    public void save(GameState gameState, File file) throws IOException {
        if (gameState == null) {
            throw new IllegalArgumentException("Game state cannot be null.");
        }

        if (file == null) {
            throw new IllegalArgumentException("Save file cannot be null.");
        }

        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create save directory: " + parent);
        }

        try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(file))) {
            outputStream.writeObject(gameState);
        }
    }

    /**
     * Saves an already-built game state.
     *
     * @param gameState state to save
     * @param path target path
     * @throws IOException if saving fails
     */
    public void save(GameState gameState, Path path) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("Save path cannot be null.");
        }

        save(gameState, path.toFile());
    }

    /**
     * Builds and saves a game state from live game values.
     *
     * @param chessBoard board to serialize
     * @param moves move history to serialize
     * @param whiteTurn {@code true} if white is to move
     * @param whiteRemainingMillis white clock time
     * @param blackRemainingMillis black clock time
     * @param file target file
     * @throws IOException if saving fails
     */
    public void save(ChessBoard chessBoard, List<Move> moves, boolean whiteTurn, long whiteRemainingMillis,
            long blackRemainingMillis, File file) throws IOException {
        save(GameState.fromBoard(chessBoard, moves, whiteTurn, whiteRemainingMillis, blackRemainingMillis), file);
    }

    /**
     * Convenience static save method.
     *
     * @param gameState state to save
     * @param file target file
     * @throws IOException if saving fails
     */
    public static void saveGame(GameState gameState, File file) throws IOException {
        new SaveGame().save(gameState, file);
    }
}
