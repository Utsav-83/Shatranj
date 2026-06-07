package chess.board;

import chess.pieces.Piece;

/**
 * Represents one square on a chess board.
 * <p>
 * A square always has a position and may optionally contain a chess piece.
 */
public class Square {

    private final Position position;
    private Piece piece;

    /**
     * Creates an empty square at the given position.
     *
     * @param position the square position
     * @throws IllegalArgumentException if position is {@code null}
     */
    public Square(Position position) {
        if (position == null) {
            throw new IllegalArgumentException("Position cannot be null.");
        }

        this.position = position;
    }

    /**
     * Gets this square's position.
     *
     * @return the square position
     */
    public Position getPosition() {
        return position;
    }

    /**
     * Gets the piece currently placed on this square.
     *
     * @return the piece, or {@code null} if the square is empty
     */
    public Piece getPiece() {
        return piece;
    }

    /**
     * Places a piece on this square.
     *
     * @param piece the piece to place, or {@code null} to clear the square
     */
    public void setPiece(Piece piece) {
        this.piece = piece;
    }

    /**
     * Removes and returns the piece on this square.
     *
     * @return the removed piece, or {@code null} if the square was already empty
     */
    public Piece removePiece() {
        Piece removedPiece = piece;
        piece = null;
        return removedPiece;
    }

    /**
     * Checks whether this square currently contains a piece.
     *
     * @return {@code true} if a piece is present
     */
    public boolean hasPiece() {
        return piece != null;
    }
}
