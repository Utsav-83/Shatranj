package chess.pieces;

import java.util.List;

import chess.board.ChessBoard;
import chess.board.Position;
import chess.interfaces.Attackable;
import chess.interfaces.Movable;
import chess.move.MoveGenerator;

/**
 * Base class for every chess piece.
 * <p>
 * Pieces implement {@link Movable} and {@link Attackable} so the rest of the
 * program can ask for movement and attack behavior through interfaces. Concrete
 * piece classes will override these methods when movement rules are added.
 */
public abstract class Piece implements Movable, Attackable {

    private static final MoveGenerator MOVE_GENERATOR = new MoveGenerator();

    protected boolean white;
    private boolean moved;

    /**
     * Creates a chess piece.
     *
     * @param white {@code true} for a white piece, {@code false} for black
     */
    public Piece(boolean white) {
        this.white = white;
        this.moved = false;
    }

    /**
     * Checks whether this is a white piece.
     *
     * @return {@code true} if the piece is white
     */
    public boolean isWhite() {
        return white;
    }

    /**
     * Checks whether this piece has moved from its original square.
     *
     * @return {@code true} if the piece has moved
     */
    public boolean hasMoved() {
        return moved;
    }

    /**
     * Updates whether this piece has moved.
     *
     * @param moved {@code true} after the piece moves
     */
    public void setMoved(boolean moved) {
        this.moved = moved;
    }

    /**
     * Gets the display symbol for this piece.
     *
     * @return the piece symbol
     */
    public abstract String getSymbol();

    /**
     * Gets legal moves for this piece from the shared move generator.
     *
     * @param currentPosition the current position
     * @param chessBoard the board state
     * @return legal destination positions
     */
    @Override
    public List<Position> getLegalMoves(Position currentPosition, ChessBoard chessBoard) {
        return MOVE_GENERATOR.generateLegalMoves(this, currentPosition, chessBoard);
    }

    /**
     * Gets attacked squares for this piece from the shared move generator.
     *
     * @param currentPosition the current position
     * @param chessBoard the board state
     * @return attacked positions
     */
    @Override
    public List<Position> getAttackPositions(Position currentPosition, ChessBoard chessBoard) {
        return MOVE_GENERATOR.generateAttackPositions(this, currentPosition, chessBoard);
    }
}
