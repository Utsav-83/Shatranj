package chess.exceptions;

import javax.swing.JOptionPane;

/**
 * Thrown when a move or board state results in a king being in check.
 */
public class CheckException extends ChessException {

    private static final long serialVersionUID = 1L;
    private static final String TITLE = "Check";

    /**
     * Creates an exception with a default message.
     */
    public CheckException() {
        this("Check. The king is under attack.");
    }

    /**
     * Creates an exception with a meaningful message.
     *
     * @param message error message
     */
    public CheckException(String message) {
        super(message, TITLE, JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Creates an exception with a meaningful message and cause for logging.
     *
     * @param message error message
     * @param cause original cause
     */
    public CheckException(String message, Throwable cause) {
        super(message, cause, TITLE, JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Creates a check-related exception with custom dialog metadata.
     *
     * @param message error message
     * @param dialogTitle dialog title shown in the GUI
     * @param dialogMessageType Swing message type
     */
    protected CheckException(String message, String dialogTitle, int dialogMessageType) {
        super(message, dialogTitle, dialogMessageType);
    }

    /**
     * Creates a check-related exception with custom dialog metadata and cause.
     *
     * @param message error message
     * @param cause original cause
     * @param dialogTitle dialog title shown in the GUI
     * @param dialogMessageType Swing message type
     */
    protected CheckException(String message, Throwable cause, String dialogTitle, int dialogMessageType) {
        super(message, cause, dialogTitle, dialogMessageType);
    }
}
