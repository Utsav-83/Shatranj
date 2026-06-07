package chess.persistence;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Path;

import chess.board.ChessBoard;

/**
 * Loads chess games saved by {@link SaveGame}.
 */
public class LoadGame {

    /**
     * Loads a serialized game state.
     *
     * @param file save file
     * @return loaded game state
     * @throws IOException if reading fails
     * @throws ClassNotFoundException if the saved object type is unavailable
     */
    public GameState load(File file) throws IOException, ClassNotFoundException {
        if (file == null) {
            throw new IllegalArgumentException("Load file cannot be null.");
        }

        try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(file))) {
            Object loadedObject = inputStream.readObject();
            if (!(loadedObject instanceof GameState)) {
                throw new IOException("File does not contain a saved chess game.");
            }

            return (GameState) loadedObject;
        }
    }

    /**
     * Loads a serialized game state.
     *
     * @param path save path
     * @return loaded game state
     * @throws IOException if reading fails
     * @throws ClassNotFoundException if the saved object type is unavailable
     */
    public GameState load(Path path) throws IOException, ClassNotFoundException {
        if (path == null) {
            throw new IllegalArgumentException("Load path cannot be null.");
        }

        return load(path.toFile());
    }

    /**
     * Loads a state and restores its board data.
     *
     * @param file save file
     * @param chessBoard board to restore
     * @return loaded game state
     * @throws IOException if reading fails
     * @throws ClassNotFoundException if the saved object type is unavailable
     */
    public GameState loadIntoBoard(File file, ChessBoard chessBoard) throws IOException, ClassNotFoundException {
        GameState gameState = load(file);
        gameState.restoreBoard(chessBoard);
        return gameState;
    }

    /**
     * Convenience static load method.
     *
     * @param file save file
     * @return loaded game state
     * @throws IOException if reading fails
     * @throws ClassNotFoundException if the saved object type is unavailable
     */
    public static GameState loadGame(File file) throws IOException, ClassNotFoundException {
        return new LoadGame().load(file);
    }
}
