package chess.exceptions;

import javax.swing.JOptionPane;

/**
 * Thrown when the current player is in checkmate.
 */
public class CheckmateException extends CheckException {

    private static final long serialVersionUID = 1L;
    private static final String TITLE = "Checkmate";

    /**
     * Creates an exception with a default message.
     */
    public CheckmateException() {
        super("Checkmate. The game is over.");
    }

    /**
     * Creates an exception with a meaningful message.
     *
     * @param message error message
     */
    public CheckmateException(String message) {
        super(message, TITLE, JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Creates an exception with a meaningful message and cause for logging.
     *
     * @param message error message
     * @param cause original cause
     */
    public CheckmateException(String message, Throwable cause) {
        super(message, cause, TITLE, JOptionPane.ERROR_MESSAGE);
    }
}
