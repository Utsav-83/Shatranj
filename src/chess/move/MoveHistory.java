package chess.move;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Stores executed commands and supports undo/redo.
 */
public class MoveHistory {

    private final Deque<Command> undoStack;
    private final Deque<Command> redoStack;

    /**
     * Creates an empty command history.
     */
    public MoveHistory() {
        undoStack = new ArrayDeque<>();
        redoStack = new ArrayDeque<>();
    }

    /**
     * Executes a command and stores it for undo.
     *
     * @param command command to execute
     */
    public void execute(Command command) {
        if (command == null) {
            throw new IllegalArgumentException("Command cannot be null.");
        }

        command.execute();
        undoStack.push(command);
        redoStack.clear();
    }

    /**
     * Undoes the most recent command.
     *
     * @return {@code true} if a command was undone
     */
    public boolean undo() {
        return undoCommand() != null;
    }

    /**
     * Redoes the most recently undone command.
     *
     * @return {@code true} if a command was redone
     */
    public boolean redo() {
        return redoCommand() != null;
    }

    /**
     * Undoes and returns the command that was undone.
     *
     * @return undone command, or {@code null}
     */
    public Command undoCommand() {
        if (undoStack.isEmpty()) {
            return null;
        }

        Command command = undoStack.pop();
        command.undo();
        redoStack.push(command);
        return command;
    }

    /**
     * Redoes and returns the command that was redone.
     *
     * @return redone command, or {@code null}
     */
    public Command redoCommand() {
        if (redoStack.isEmpty()) {
            return null;
        }

        Command command = redoStack.pop();
        command.redo();
        undoStack.push(command);
        return command;
    }

    /**
     * Checks whether undo is available.
     *
     * @return {@code true} if undo can run
     */
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    /**
     * Checks whether redo is available.
     *
     * @return {@code true} if redo can run
     */
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    /**
     * Clears all command history.
     */
    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }
}
