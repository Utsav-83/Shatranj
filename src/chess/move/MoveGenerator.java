package chess.move;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import chess.board.ChessBoard;
import chess.board.Position;
import chess.pieces.Bishop;
import chess.pieces.King;
import chess.pieces.Knight;
import chess.pieces.Pawn;
import chess.pieces.Piece;
import chess.pieces.Queen;
import chess.pieces.Rook;

/**
 * Generates pseudo-legal chess moves for individual pieces.
 * <p>
 * This class handles board boundaries, blocked paths, captures, castling
 * candidates, and en passant candidates. It does not decide whether castling
 * squares are attacked; the validator handles that.
 */
public class MoveGenerator {

    private static final int[][] ROOK_DIRECTIONS = {
            {-1, 0},
            {1, 0},
            {0, -1},
            {0, 1}
    };

    private static final int[][] BISHOP_DIRECTIONS = {
            {-1, -1},
            {-1, 1},
            {1, -1},
            {1, 1}
    };

    private static final int[][] QUEEN_DIRECTIONS = {
            {-1, -1},
            {-1, 0},
            {-1, 1},
            {0, -1},
            {0, 1},
            {1, -1},
            {1, 0},
            {1, 1}
    };

    private static final int[][] KING_OFFSETS = {
            {-1, -1},
            {-1, 0},
            {-1, 1},
            {0, -1},
            {0, 1},
            {1, -1},
            {1, 0},
            {1, 1}
    };

    private static final int[][] KNIGHT_OFFSETS = {
            {-2, -1},
            {-2, 1},
            {-1, -2},
            {-1, 2},
            {1, -2},
            {1, 2},
            {2, -1},
            {2, 1}
    };

    /**
     * Generates all legal destinations for a piece from the given position.
     *
     * @param piece piece to move
     * @param currentPosition current piece position
     * @param chessBoard board state
     * @return legal destination positions
     */
    public List<Position> generateLegalMoves(Piece piece, Position currentPosition, ChessBoard chessBoard) {
        validateInputs(piece, currentPosition, chessBoard);

        if (piece instanceof Pawn) {
            return generatePawnMoves(piece, currentPosition, chessBoard);
        }

        if (piece instanceof Knight) {
            return generateStepMoves(piece, currentPosition, chessBoard, KNIGHT_OFFSETS);
        }

        if (piece instanceof Bishop) {
            return generateSlidingMoves(piece, currentPosition, chessBoard, BISHOP_DIRECTIONS);
        }

        if (piece instanceof Rook) {
            return generateSlidingMoves(piece, currentPosition, chessBoard, ROOK_DIRECTIONS);
        }

        if (piece instanceof Queen) {
            return generateSlidingMoves(piece, currentPosition, chessBoard, QUEEN_DIRECTIONS);
        }

        if (piece instanceof King) {
            return generateKingMoves(piece, currentPosition, chessBoard);
        }

        return Collections.emptyList();
    }

    /**
     * Generates attacked squares for a piece.
     *
     * @param piece piece to inspect
     * @param currentPosition current piece position
     * @param chessBoard board state
     * @return attacked positions
     */
    public List<Position> generateAttackPositions(Piece piece, Position currentPosition, ChessBoard chessBoard) {
        validateInputs(piece, currentPosition, chessBoard);

        if (piece instanceof Pawn) {
            return generatePawnAttacks(piece, currentPosition);
        }

        if (piece instanceof King) {
            return generateStepMoves(piece, currentPosition, chessBoard, KING_OFFSETS);
        }

        return generateLegalMoves(piece, currentPosition, chessBoard);
    }

    /**
     * Generates pawn movement including blocked paths and diagonal captures.
     *
     * @param piece pawn
     * @param currentPosition current pawn position
     * @param chessBoard board state
     * @return legal pawn destinations
     */
    private List<Position> generatePawnMoves(Piece piece, Position currentPosition, ChessBoard chessBoard) {
        List<Position> moves = new ArrayList<>();
        int direction = piece.isWhite() ? -1 : 1;
        int startRow = piece.isWhite() ? 6 : 1;
        int row = currentPosition.getRow();
        int column = currentPosition.getColumn();
        int forwardRow = row + direction;

        if (Position.isValid(forwardRow, column)) {
            Position forward = new Position(forwardRow, column);

            if (chessBoard.getPiece(forward) == null) {
                moves.add(forward);
                addPawnDoubleMove(moves, currentPosition, chessBoard, direction, startRow);
            }
        }

        addPawnCapture(moves, piece, currentPosition, chessBoard, direction, -1);
        addPawnCapture(moves, piece, currentPosition, chessBoard, direction, 1);
        addEnPassantCapture(moves, piece, currentPosition, chessBoard, direction, -1);
        addEnPassantCapture(moves, piece, currentPosition, chessBoard, direction, 1);

        return moves;
    }

    /**
     * Adds a pawn's first double-step move when both squares are empty.
     *
     * @param moves move list to update
     * @param currentPosition current pawn position
     * @param chessBoard board state
     * @param direction pawn forward direction
     * @param startRow pawn starting row
     */
    private void addPawnDoubleMove(List<Position> moves, Position currentPosition, ChessBoard chessBoard,
            int direction, int startRow) {
        int row = currentPosition.getRow();
        int column = currentPosition.getColumn();
        int doubleForwardRow = row + (direction * 2);

        if (row == startRow && Position.isValid(doubleForwardRow, column)) {
            Position doubleForward = new Position(doubleForwardRow, column);

            if (chessBoard.getPiece(doubleForward) == null) {
                moves.add(doubleForward);
            }
        }
    }

    /**
     * Adds a diagonal pawn capture when an opponent piece is present.
     *
     * @param moves move list to update
     * @param piece pawn
     * @param currentPosition current pawn position
     * @param chessBoard board state
     * @param rowDirection pawn forward direction
     * @param columnDirection diagonal direction
     */
    private void addPawnCapture(List<Position> moves, Piece piece, Position currentPosition, ChessBoard chessBoard,
            int rowDirection, int columnDirection) {
        int row = currentPosition.getRow() + rowDirection;
        int column = currentPosition.getColumn() + columnDirection;

        if (!Position.isValid(row, column)) {
            return;
        }

        Position destination = new Position(row, column);
        Piece destinationPiece = chessBoard.getPiece(destination);

        if (destinationPiece != null && destinationPiece.isWhite() != piece.isWhite()) {
            moves.add(destination);
        }
    }

    /**
     * Adds an en passant capture candidate when the board target permits it.
     *
     * @param moves move list to update
     * @param piece pawn
     * @param currentPosition current pawn position
     * @param chessBoard board state
     * @param rowDirection pawn forward direction
     * @param columnDirection diagonal direction
     */
    private void addEnPassantCapture(List<Position> moves, Piece piece, Position currentPosition,
            ChessBoard chessBoard, int rowDirection, int columnDirection) {
        Position enPassantTarget = chessBoard.getEnPassantTarget();
        if (enPassantTarget == null) {
            return;
        }

        int row = currentPosition.getRow() + rowDirection;
        int column = currentPosition.getColumn() + columnDirection;

        if (Position.isValid(row, column) && enPassantTarget.equals(new Position(row, column))) {
            Position capturedPawnPosition = new Position(currentPosition.getRow(), column);
            Piece capturedPawn = chessBoard.getPiece(capturedPawnPosition);

            if (capturedPawn instanceof Pawn && capturedPawn.isWhite() != piece.isWhite()) {
                moves.add(enPassantTarget);
            }
        }
    }

    /**
     * Generates king moves including castling candidates.
     *
     * @param piece king
     * @param currentPosition current king position
     * @param chessBoard board state
     * @return king move candidates
     */
    private List<Position> generateKingMoves(Piece piece, Position currentPosition, ChessBoard chessBoard) {
        List<Position> moves = generateStepMoves(piece, currentPosition, chessBoard, KING_OFFSETS);
        addCastlingMove(moves, piece, currentPosition, chessBoard, true);
        addCastlingMove(moves, piece, currentPosition, chessBoard, false);
        return moves;
    }

    /**
     * Adds a castling candidate when rights exist and path squares are empty.
     *
     * @param moves move list to update
     * @param piece king
     * @param currentPosition current king position
     * @param chessBoard board state
     * @param kingSide {@code true} for kingside
     */
    private void addCastlingMove(List<Position> moves, Piece piece, Position currentPosition,
            ChessBoard chessBoard, boolean kingSide) {
        int row = piece.isWhite() ? 7 : 0;
        if (currentPosition.getRow() != row || currentPosition.getColumn() != 4
                || !chessBoard.canCastle(piece.isWhite(), kingSide)) {
            return;
        }

        int firstColumn = kingSide ? 5 : 1;
        int lastColumn = kingSide ? 6 : 3;

        for (int column = firstColumn; column <= lastColumn; column++) {
            if (chessBoard.getPiece(new Position(row, column)) != null) {
                return;
            }
        }

        moves.add(new Position(row, kingSide ? 6 : 2));
    }

    /**
     * Generates pawn attack squares without requiring an enemy piece.
     *
     * @param piece pawn
     * @param currentPosition current pawn position
     * @return attacked positions
     */
    private List<Position> generatePawnAttacks(Piece piece, Position currentPosition) {
        List<Position> attacks = new ArrayList<>();
        int direction = piece.isWhite() ? -1 : 1;
        int row = currentPosition.getRow() + direction;
        int leftColumn = currentPosition.getColumn() - 1;
        int rightColumn = currentPosition.getColumn() + 1;

        if (Position.isValid(row, leftColumn)) {
            attacks.add(new Position(row, leftColumn));
        }

        if (Position.isValid(row, rightColumn)) {
            attacks.add(new Position(row, rightColumn));
        }

        return attacks;
    }

    /**
     * Generates moves for sliding pieces until blocked by a piece or boundary.
     *
     * @param piece moving piece
     * @param currentPosition current piece position
     * @param chessBoard board state
     * @param directions row and column direction pairs
     * @return legal sliding destinations
     */
    private List<Position> generateSlidingMoves(Piece piece, Position currentPosition, ChessBoard chessBoard,
            int[][] directions) {
        List<Position> moves = new ArrayList<>();

        for (int[] direction : directions) {
            int row = currentPosition.getRow() + direction[0];
            int column = currentPosition.getColumn() + direction[1];

            while (Position.isValid(row, column)) {
                Position destination = new Position(row, column);
                Piece destinationPiece = chessBoard.getPiece(destination);

                if (destinationPiece == null) {
                    moves.add(destination);
                } else {
                    if (destinationPiece.isWhite() != piece.isWhite()) {
                        moves.add(destination);
                    }

                    break;
                }

                row += direction[0];
                column += direction[1];
            }
        }

        return moves;
    }

    /**
     * Generates moves for fixed-offset pieces such as the king and knight.
     *
     * @param piece moving piece
     * @param currentPosition current piece position
     * @param chessBoard board state
     * @param offsets row and column offsets
     * @return legal step destinations
     */
    private List<Position> generateStepMoves(Piece piece, Position currentPosition, ChessBoard chessBoard,
            int[][] offsets) {
        List<Position> moves = new ArrayList<>();

        for (int[] offset : offsets) {
            int row = currentPosition.getRow() + offset[0];
            int column = currentPosition.getColumn() + offset[1];

            if (!Position.isValid(row, column)) {
                continue;
            }

            Position destination = new Position(row, column);
            Piece destinationPiece = chessBoard.getPiece(destination);

            if (destinationPiece == null || destinationPiece.isWhite() != piece.isWhite()) {
                moves.add(destination);
            }
        }

        return moves;
    }

    /**
     * Validates common generator inputs.
     *
     * @param piece piece to move
     * @param currentPosition current position
     * @param chessBoard board state
     */
    private void validateInputs(Piece piece, Position currentPosition, ChessBoard chessBoard) {
        if (piece == null || currentPosition == null || chessBoard == null) {
            throw new IllegalArgumentException("Piece, position, and board are required.");
        }
    }
}
