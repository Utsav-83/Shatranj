package chess.interfaces;

/**
 * Defines save and load behavior for game state.
 * <p>
 * This interface exists so persistence can be handled through a stable contract
 * without coupling callers to a concrete board implementation or storage format.
 * That supports the Dependency Inversion Principle.
 */
public interface SerializableGame {

    /**
     * Serializes the current game state.
     *
     * @return a text representation of the game state
     */
    String serializeGame();

    /**
     * Restores game state from serialized text.
     *
     * @param data serialized game data
     * @throws IllegalArgumentException if the data cannot be loaded
     */
    void deserializeGame(String data);
}
