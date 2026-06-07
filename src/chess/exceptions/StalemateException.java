package chess.exceptions;

import javax.swing.JOptionPane;

/**
 * Thrown when the current player has no legal moves and is not in check.
 */
public class StalemateException extends ChessException {

    private static final long serialVersionUID = 1L;
    private static final String TITLE = "Stalemate";

    /**
     * Creates an exception with a default message.
     */
    public StalemateException() {
        this("Stalemate. The game is a draw.");
    }

    /**
     * Creates an exception with a meaningful message.
     *
     * @param message error message
     */
    public StalemateException(String message) {
        super(message, TITLE, JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Creates an exception with a meaningful message and cause for logging.
     *
     * @param message error message
     * @param cause original cause
     */
    public StalemateException(String message, Throwable cause) {
        super(message, cause, TITLE, JOptionPane.INFORMATION_MESSAGE);
    }
}
