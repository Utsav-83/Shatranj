package chess.move;

import java.util.List;
import java.util.ArrayList;

import chess.board.ChessBoard;
import chess.board.Position;
import chess.exceptions.CheckException;
import chess.exceptions.InvalidPositionException;
import chess.pieces.King;
import chess.pieces.Piece;

/**
 * Detects whether a king is currently under attack.
 * <p>
 * Check detection is kept separate from the GUI and board model so it can be
 * reused by validators, controllers, and future checkmate/stalemate logic.
 */
public class CheckDetector {

    private final MoveGenerator moveGenerator;

    /**
     * Creates a detector with its own move generator.
     */
    public CheckDetector() {
        this(new MoveGenerator());
    }

    /**
     * Creates a detector with an injected move generator.
     *
     * @param moveGenerator generator used to evaluate enemy attacks
     */
    public CheckDetector(MoveGenerator moveGenerator) {
        if (moveGenerator == null) {
            throw new IllegalArgumentException("Move generator cannot be null.");
        }

        this.moveGenerator = moveGenerator;
    }

    /**
     * Determines whether the specified color's king is attacked.
     *
     * @param chessBoard current board state
     * @param whiteKing {@code true} to check the white king, {@code false} for black
     * @return {@code true} if the king is in check
     * @throws CheckException if the king cannot be found
     */
    public boolean isKingInCheck(ChessBoard chessBoard, boolean whiteKing) {
        return !findCheckingPieces(chessBoard, whiteKing).isEmpty();
    }

    /**
     * Finds the king for a color.
     *
     * @param chessBoard current board state
     * @param whiteKing {@code true} for white king, {@code false} for black king
     * @return king position
     * @throws CheckException if the king is missing
     */
    public Position findKingPosition(ChessBoard chessBoard, boolean whiteKing) {
        if (chessBoard == null) {
            throw new InvalidPositionException("Board cannot be null when locating the king.", null);
        }

        for (int row = 0; row < ChessBoard.BOARD_SIZE; row++) {
            for (int column = 0; column < ChessBoard.BOARD_SIZE; column++) {
                Position position = new Position(row, column);
                Piece piece = chessBoard.getPiece(position);

                if (piece instanceof King && piece.isWhite() == whiteKing) {
                    return position;
                }
            }
        }

        throw new CheckException((whiteKing ? "White" : "Black") + " king is missing from the board.");
    }

    /**
     * Finds all enemy pieces currently attacking the requested king.
     *
     * @param chessBoard current board state
     * @param whiteKing {@code true} to inspect the white king, {@code false} for black
     * @return positions of enemy pieces attacking the king
     * @throws CheckException if the king cannot be found
     */
    public List<Position> findCheckingPieces(ChessBoard chessBoard, boolean whiteKing) {
        if (chessBoard == null) {
            throw new InvalidPositionException("Board cannot be null when checking for check.", null);
        }

        Position kingPosition = findKingPosition(chessBoard, whiteKing);
        return findAttackers(chessBoard, kingPosition, !whiteKing);
    }

    /**
     * Determines whether a square is attacked by any piece of the requested color.
     *
     * @param chessBoard current board state
     * @param targetPosition square to test
     * @param byWhitePieces {@code true} to test white attacks, {@code false} for black
     * @return {@code true} if the square is attacked
     */
    public boolean isSquareAttacked(ChessBoard chessBoard, Position targetPosition, boolean byWhitePieces) {
        return !findAttackers(chessBoard, targetPosition, byWhitePieces).isEmpty();
    }

    /**
     * Finds all pieces of the requested color attacking a target square.
     *
     * @param chessBoard current board state
     * @param targetPosition square to test
     * @param byWhitePieces {@code true} to test white attacks, {@code false} for black
     * @return positions of attacking pieces
     */
    public List<Position> findAttackers(ChessBoard chessBoard, Position targetPosition, boolean byWhitePieces) {
        if (chessBoard == null || targetPosition == null) {
            throw new InvalidPositionException("Board and target position are required for attack detection.", null);
        }

        List<Position> attackers = new ArrayList<>();

        for (int row = 0; row < ChessBoard.BOARD_SIZE; row++) {
            for (int column = 0; column < ChessBoard.BOARD_SIZE; column++) {
                Position enemyPosition = new Position(row, column);
                Piece enemyPiece = chessBoard.getPiece(enemyPosition);

                if (enemyPiece != null && enemyPiece.isWhite() == byWhitePieces
                        && attacksTarget(enemyPiece, enemyPosition, targetPosition, chessBoard)) {
                    attackers.add(enemyPosition);
                }
            }
        }

        return attackers;
    }

    /**
     * Tests whether one enemy piece attacks a target square.
     *
     * @param enemyPiece attacking piece
     * @param enemyPosition attacking piece position
     * @param targetPosition target square
     * @param chessBoard current board state
     * @return {@code true} if the piece attacks the target
     */
    private boolean attacksTarget(Piece enemyPiece, Position enemyPosition, Position targetPosition,
            ChessBoard chessBoard) {
        List<Position> attacks = moveGenerator.generateAttackPositions(enemyPiece, enemyPosition, chessBoard);
        return attacks.contains(targetPosition);
    }
}
