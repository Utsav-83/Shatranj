package chess.exceptions;

import java.awt.Component;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Base class for chess-specific application exceptions.
 * <p>
 * Each exception carries enough UI metadata to log itself and display a
 * meaningful Swing dialog without duplicating that behavior in every GUI class.
 */
public abstract class ChessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String dialogTitle;
    private final int dialogMessageType;

    /**
     * Creates a chess exception with GUI metadata.
     *
     * @param message meaningful error message
     * @param dialogTitle dialog title shown in the GUI
     * @param dialogMessageType Swing message type
     */
    protected ChessException(String message, String dialogTitle, int dialogMessageType) {
        super(message);
        this.dialogTitle = dialogTitle;
        this.dialogMessageType = dialogMessageType;
    }

    /**
     * Creates a chess exception with GUI metadata and a cause for logging.
     *
     * @param message meaningful error message
     * @param cause original cause
     * @param dialogTitle dialog title shown in the GUI
     * @param dialogMessageType Swing message type
     */
    protected ChessException(String message, Throwable cause, String dialogTitle, int dialogMessageType) {
        super(message, cause);
        this.dialogTitle = dialogTitle;
        this.dialogMessageType = dialogMessageType;
    }

    /**
     * Gets the dialog title associated with this exception.
     *
     * @return GUI dialog title
     */
    public String getDialogTitle() {
        return dialogTitle;
    }

    /**
     * Gets the Swing dialog message type associated with this exception.
     *
     * @return {@link JOptionPane} message type
     */
    public int getDialogMessageType() {
        return dialogMessageType;
    }

    /**
     * Logs this exception with the supplied logger.
     *
     * @param logger logger to write to
     * @param level log severity
     */
    public void log(Logger logger, Level level) {
        if (logger == null) {
            throw new IllegalArgumentException("Logger cannot be null.");
        }

        Level effectiveLevel = level == null ? Level.WARNING : level;
        logger.log(effectiveLevel, getMessage(), this);
    }

    /**
     * Displays this exception in a Swing dialog.
     *
     * @param parent parent component used to locate the owning window
     */
    public void showDialog(Component parent) {
        Component owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        JOptionPane.showMessageDialog(owner, getMessage(), dialogTitle, dialogMessageType);
    }

    /**
     * Logs this exception and displays it in a Swing dialog.
     *
     * @param logger logger to write to
     * @param level log severity
     * @param parent parent component used to locate the owning window
     */
    public void logAndShow(Logger logger, Level level, Component parent) {
        log(logger, level);
        showDialog(parent);
    }
}
