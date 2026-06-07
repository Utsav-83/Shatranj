package chess.move;

/**
 * Command interface for actions that can be executed, undone, and redone.
 */
public interface Command {

    /**
     * Executes the command.
     */
    void execute();

    /**
     * Undoes the command.
     */
    void undo();

    /**
     * Redoes the command.
     */
    void redo();
}
