package chess.exceptions;

import javax.swing.JOptionPane;

/**
 * Thrown when a requested chess move is not legal for the current board state.
 */
public class IllegalMoveException extends ChessException {

    private static final long serialVersionUID = 1L;
    private static final String TITLE = "Illegal Move";

    /**
     * Creates an exception with a default message.
     */
    public IllegalMoveException() {
        this("Illegal move. Please choose a valid destination square.");
    }

    /**
     * Creates an exception with a meaningful message.
     *
     * @param message error message
     */
    public IllegalMoveException(String message) {
        super(message, TITLE, JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Creates an exception with a meaningful message and cause for logging.
     *
     * @param message error message
     * @param cause original cause
     */
    public IllegalMoveException(String message, Throwable cause) {
        super(message, cause, TITLE, JOptionPane.WARNING_MESSAGE);
    }
}
