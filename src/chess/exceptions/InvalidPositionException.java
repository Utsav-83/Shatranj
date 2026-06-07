package chess.exceptions;

import javax.swing.JOptionPane;

/**
 * Thrown when a board position is outside the 8x8 chess board.
 */
public class InvalidPositionException extends ChessException {

    private static final long serialVersionUID = 1L;
    private static final String TITLE = "Invalid Position";

    /**
     * Creates an exception with a default message.
     */
    public InvalidPositionException() {
        this("Invalid board position. Row and column must be between 0 and 7.");
    }

    /**
     * Creates an exception for a specific row and column.
     *
     * @param row invalid row
     * @param column invalid column
     */
    public InvalidPositionException(int row, int column) {
        this("Invalid board position: row " + row + ", column " + column
                + ". Row and column must be between 0 and 7.");
    }

    /**
     * Creates an exception with a meaningful message.
     *
     * @param message error message
     */
    public InvalidPositionException(String message) {
        super(message, TITLE, JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Creates an exception with a meaningful message and cause for logging.
     *
     * @param message error message
     * @param cause original cause
     */
    public InvalidPositionException(String message, Throwable cause) {
        super(message, cause, TITLE, JOptionPane.ERROR_MESSAGE);
    }
}
