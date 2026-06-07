package chess.interfaces;

import java.util.List;

import chess.board.ChessBoard;
import chess.board.Position;

/**
 * Defines the attacking responsibility for chess pieces.
 * <p>
 * This interface exists because attack squares are not always identical to
 * movement squares, especially for pawns and king safety checks. Separating it
 * from {@link Movable} keeps each contract focused and supports SOLID designs.
 */
public interface Attackable {

    /**
     * Calculates positions attacked by this object.
     *
     * @param currentPosition the object's current position
     * @param chessBoard the board state used for calculation
     * @return attacked positions
     */
    List<Position> getAttackPositions(Position currentPosition, ChessBoard chessBoard);
}
