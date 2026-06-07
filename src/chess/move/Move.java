package chess.move;

import chess.board.Position;
import chess.exceptions.InvalidPositionException;
import chess.pieces.Piece;

/**
 * Represents one requested chess move from a source square to a destination
 * square.
 * <p>
 * The source and destination are required. The moved and captured pieces are
 * optional metadata that can be filled in by the board or controller after the
 * move is evaluated.
 */
public class Move {

    private final Position source;
    private final Position destination;
    private Piece movedPiece;
    private Piece capturedPiece;
    private Position enPassantCapturedPosition;
    private String promotionSymbol;

    /**
     * Creates a move request.
     *
     * @param source source square
     * @param destination destination square
     */
    public Move(Position source, Position destination) {
        if (source == null || destination == null) {
            throw new InvalidPositionException("Source and destination positions cannot be null.", null);
        }

        this.source = source;
        this.destination = destination;
    }

    /**
     * Gets the source square.
     *
     * @return source position
     */
    public Position getSource() {
        return source;
    }

    /**
     * Gets the destination square.
     *
     * @return destination position
     */
    public Position getDestination() {
        return destination;
    }

    /**
     * Gets the moved piece.
     *
     * @return moved piece, or {@code null} if not assigned
     */
    public Piece getMovedPiece() {
        return movedPiece;
    }

    /**
     * Sets the moved piece metadata.
     *
     * @param movedPiece moved piece
     */
    public void setMovedPiece(Piece movedPiece) {
        this.movedPiece = movedPiece;
    }

    /**
     * Gets the captured piece.
     *
     * @return captured piece, or {@code null} if no capture occurred
     */
    public Piece getCapturedPiece() {
        return capturedPiece;
    }

    /**
     * Sets the captured piece metadata.
     *
     * @param capturedPiece captured piece, or {@code null}
     */
    public void setCapturedPiece(Piece capturedPiece) {
        this.capturedPiece = capturedPiece;
    }

    /**
     * Gets the captured pawn square for an en passant move.
     *
     * @return captured pawn position, or {@code null}
     */
    public Position getEnPassantCapturedPosition() {
        return enPassantCapturedPosition;
    }

    /**
     * Sets the captured pawn square for an en passant move.
     *
     * @param enPassantCapturedPosition captured pawn position
     */
    public void setEnPassantCapturedPosition(Position enPassantCapturedPosition) {
        this.enPassantCapturedPosition = enPassantCapturedPosition;
    }

    /**
     * Checks whether this move is an en passant capture.
     *
     * @return {@code true} if en passant capture metadata is present
     */
    public boolean isEnPassant() {
        return enPassantCapturedPosition != null;
    }

    /**
     * Gets the promotion piece code.
     *
     * @return one of Q, R, B, N, or {@code null}
     */
    public String getPromotionSymbol() {
        return promotionSymbol;
    }

    /**
     * Sets the promotion piece code.
     *
     * @param promotionSymbol one of Q, R, B, N
     */
    public void setPromotionSymbol(String promotionSymbol) {
        this.promotionSymbol = promotionSymbol;
    }

    /**
     * Checks whether the move is a castling move.
     *
     * @return {@code true} if a king moves two files
     */
    public boolean isCastling() {
        return Math.abs(destination.getColumn() - source.getColumn()) == 2;
    }

    @Override
    public String toString() {
        return "Move{source=" + source + ", destination=" + destination + "}";
    }
}
