package chess.interfaces;

import java.util.List;

import chess.board.ChessBoard;
import chess.board.Position;

/**
 * Defines the movement responsibility for chess pieces.
 * <p>
 * This interface exists so movement calculation can vary by piece type without
 * forcing the board model or UI classes to know each piece's movement rules.
 * It follows the Interface Segregation Principle by exposing only movement
 * behavior, separate from attacking, saving, or observing game state.
 */
public interface Movable {

    /**
     * Calculates legal destination positions for this object.
     *
     * @param currentPosition the object's current position
     * @param chessBoard the board state used for calculation
     * @return legal destination positions
     */
    List<Position> getLegalMoves(Position currentPosition, ChessBoard chessBoard);
}
