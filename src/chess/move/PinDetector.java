package chess.move;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import chess.board.ChessBoard;
import chess.board.Position;
import chess.exceptions.IllegalMoveException;
import chess.exceptions.InvalidPositionException;
import chess.pieces.Bishop;
import chess.pieces.King;
import chess.pieces.Pawn;
import chess.pieces.Piece;
import chess.pieces.Queen;
import chess.pieces.Rook;

/**
 * Detects pins and self-check conditions.
 * <p>
 * Absolute pins are pieces shielding their own king from a sliding enemy piece.
 * Relative pins are pieces shielding another friendly non-king piece from a
 * sliding enemy piece. Relative pins are useful information, but only absolute
 * pins and other self-check positions make a move illegal.
 */
public class PinDetector {

    private static final int[][] DIRECTIONS = {
            {-1, 0},
            {1, 0},
            {0, -1},
            {0, 1},
            {-1, -1},
            {-1, 1},
            {1, -1},
            {1, 1}
    };

    private final CheckDetector checkDetector;

    /**
     * Creates a pin detector with its own check detector.
     */
    public PinDetector() {
        this(new CheckDetector());
    }

    /**
     * Creates a pin detector with an injected check detector.
     *
     * @param checkDetector detector used to test self-check after simulation
     */
    public PinDetector(CheckDetector checkDetector) {
        if (checkDetector == null) {
            throw new IllegalArgumentException("Check detector cannot be null.");
        }

        this.checkDetector = checkDetector;
    }

    /**
     * Finds all absolute pins against the requested side's king.
     *
     * @param chessBoard current board state
     * @param whiteSide {@code true} for white, {@code false} for black
     * @return absolute pin records
     */
    public List<Pin> findAbsolutePins(ChessBoard chessBoard, boolean whiteSide) {
        if (chessBoard == null) {
            throw new InvalidPositionException("Board cannot be null when detecting pins.", null);
        }

        Position kingPosition = checkDetector.findKingPosition(chessBoard, whiteSide);
        return findPinsFromProtectedSquare(chessBoard, kingPosition, whiteSide, PinType.ABSOLUTE);
    }

    /**
     * Finds relative pins for a side.
     * <p>
     * A relative pin means a friendly piece is shielding another friendly
     * non-king piece from a sliding enemy attack. These moves are still legal
     * unless they also expose the king.
     *
     * @param chessBoard current board state
     * @param whiteSide {@code true} for white, {@code false} for black
     * @return relative pin records
     */
    public List<Pin> findRelativePins(ChessBoard chessBoard, boolean whiteSide) {
        if (chessBoard == null) {
            throw new InvalidPositionException("Board cannot be null when detecting pins.", null);
        }

        List<Pin> pins = new ArrayList<>();

        for (int row = 0; row < ChessBoard.BOARD_SIZE; row++) {
            for (int column = 0; column < ChessBoard.BOARD_SIZE; column++) {
                Position protectedPosition = new Position(row, column);
                Piece protectedPiece = chessBoard.getPiece(protectedPosition);

                if (protectedPiece != null && protectedPiece.isWhite() == whiteSide
                        && !(protectedPiece instanceof King)) {
                    pins.addAll(findPinsFromProtectedSquare(chessBoard, protectedPosition, whiteSide,
                            PinType.RELATIVE));
                }
            }
        }

        return pins;
    }

    /**
     * Checks whether a piece is absolutely pinned to its own king.
     *
     * @param chessBoard current board state
     * @param piecePosition piece to inspect
     * @return {@code true} if the piece is absolutely pinned
     */
    public boolean isAbsolutelyPinned(ChessBoard chessBoard, Position piecePosition) {
        Pin pin = getAbsolutePin(chessBoard, piecePosition);
        return pin != null;
    }

    /**
     * Gets the absolute pin affecting a piece.
     *
     * @param chessBoard current board state
     * @param piecePosition piece to inspect
     * @return pin record, or {@code null} if the piece is not absolutely pinned
     */
    public Pin getAbsolutePin(ChessBoard chessBoard, Position piecePosition) {
        if (chessBoard == null || piecePosition == null) {
            throw new InvalidPositionException("Board and piece position are required for pin detection.", null);
        }

        Piece piece = chessBoard.getPiece(piecePosition);
        if (piece == null || piece instanceof King) {
            return null;
        }

        for (Pin pin : findAbsolutePins(chessBoard, piece.isWhite())) {
            if (pin.getPinnedPosition().equals(piecePosition)) {
                return pin;
            }
        }

        return null;
    }

    /**
     * Determines whether a move would leave the moving side's king in check.
     *
     * @param chessBoard current board state
     * @param move move to simulate
     * @return {@code true} if the king would be attacked after the move
     */
    public boolean wouldLeaveKingInCheck(ChessBoard chessBoard, Move move) {
        if (chessBoard == null || move == null) {
            throw new InvalidPositionException("Board and move are required for self-check detection.", null);
        }

        Piece movingPiece = chessBoard.getPiece(move.getSource());
        if (movingPiece == null) {
            throw new IllegalMoveException("Cannot test self-check from an empty square.");
        }

        Piece capturedPiece = chessBoard.getPiece(move.getDestination());
        Position enPassantCapturedPosition = getEnPassantCapturedPosition(chessBoard, move, movingPiece);
        Piece enPassantCapturedPiece = enPassantCapturedPosition == null ? null
                : chessBoard.getPiece(enPassantCapturedPosition);

        chessBoard.getSquare(move.getSource()).removePiece();
        if (enPassantCapturedPosition != null) {
            chessBoard.getSquare(enPassantCapturedPosition).removePiece();
        }
        chessBoard.getSquare(move.getDestination()).setPiece(movingPiece);

        try {
            return checkDetector.isKingInCheck(chessBoard, movingPiece.isWhite());
        } finally {
            chessBoard.getSquare(move.getDestination()).setPiece(capturedPiece);
            if (enPassantCapturedPosition != null) {
                chessBoard.getSquare(enPassantCapturedPosition).setPiece(enPassantCapturedPiece);
            }
            chessBoard.getSquare(move.getSource()).setPiece(movingPiece);
        }
    }

    /**
     * Gets the captured pawn position for a simulated en passant move.
     *
     * @param chessBoard current board state
     * @param move move to inspect
     * @param movingPiece moving piece
     * @return captured pawn position, or {@code null}
     */
    private Position getEnPassantCapturedPosition(ChessBoard chessBoard, Move move, Piece movingPiece) {
        if (!(movingPiece instanceof Pawn) || chessBoard.getEnPassantTarget() == null
                || !move.getDestination().equals(chessBoard.getEnPassantTarget())
                || chessBoard.getPiece(move.getDestination()) != null
                || Math.abs(move.getDestination().getColumn() - move.getSource().getColumn()) != 1) {
            return null;
        }

        int captureRow = movingPiece.isWhite() ? move.getDestination().getRow() + 1
                : move.getDestination().getRow() - 1;
        if (!Position.isValid(captureRow, move.getDestination().getColumn())) {
            return null;
        }

        Position capturedPosition = new Position(captureRow, move.getDestination().getColumn());
        Piece capturedPiece = chessBoard.getPiece(capturedPosition);
        return capturedPiece instanceof Pawn && capturedPiece.isWhite() != movingPiece.isWhite()
                ? capturedPosition : null;
    }

    /**
     * Filters pseudo-legal moves to moves that do not expose the king.
     *
     * @param chessBoard current board state
     * @param source source position
     * @param pseudoLegalMoves generated piece moves
     * @return legal moves after self-check prevention
     */
    public List<Position> filterSelfCheckMoves(ChessBoard chessBoard, Position source,
            List<Position> pseudoLegalMoves) {
        if (chessBoard == null || source == null || pseudoLegalMoves == null) {
            throw new InvalidPositionException("Board, source, and moves are required for self-check filtering.",
                    null);
        }

        if (pseudoLegalMoves.isEmpty()) {
            return Collections.emptyList();
        }

        List<Position> legalMoves = new ArrayList<>();

        for (Position destination : pseudoLegalMoves) {
            Move move = new Move(source, destination);

            if (!wouldLeaveKingInCheck(chessBoard, move)) {
                legalMoves.add(destination);
            }
        }

        return legalMoves;
    }

    /**
     * Finds pins along every ray from a protected square.
     *
     * @param chessBoard current board state
     * @param protectedPosition king or valuable friendly piece
     * @param friendlyWhite color of the protected side
     * @param pinType absolute or relative
     * @return pin records found from this protected square
     */
    private List<Pin> findPinsFromProtectedSquare(ChessBoard chessBoard, Position protectedPosition,
            boolean friendlyWhite, PinType pinType) {
        List<Pin> pins = new ArrayList<>();

        for (int[] direction : DIRECTIONS) {
            Pin pin = findPinInDirection(chessBoard, protectedPosition, direction, friendlyWhite, pinType);

            if (pin != null) {
                pins.add(pin);
            }
        }

        return pins;
    }

    /**
     * Scans one line from a protected square looking for friendly piece then
     * enemy slider.
     *
     * @param chessBoard current board state
     * @param protectedPosition king or valuable friendly piece
     * @param direction row and column step
     * @param friendlyWhite color of the protected side
     * @param pinType absolute or relative
     * @return pin record, or {@code null}
     */
    private Pin findPinInDirection(ChessBoard chessBoard, Position protectedPosition, int[] direction,
            boolean friendlyWhite, PinType pinType) {
        Position pinnedPosition = null;
        int row = protectedPosition.getRow() + direction[0];
        int column = protectedPosition.getColumn() + direction[1];

        while (Position.isValid(row, column)) {
            Position currentPosition = new Position(row, column);
            Piece currentPiece = chessBoard.getPiece(currentPosition);

            if (currentPiece != null) {
                if (pinnedPosition == null) {
                    if (currentPiece.isWhite() != friendlyWhite || currentPiece instanceof King) {
                        return null;
                    }

                    pinnedPosition = currentPosition;
                } else {
                    if (currentPiece.isWhite() != friendlyWhite
                            && canSlideAlongDirection(currentPiece, direction)) {
                        return new Pin(pinType, pinnedPosition, protectedPosition, currentPosition);
                    }

                    return null;
                }
            }

            row += direction[0];
            column += direction[1];
        }

        return null;
    }

    /**
     * Checks whether a piece attacks along a ray direction.
     *
     * @param piece attacking piece
     * @param direction row and column step
     * @return {@code true} if the piece can slide along that direction
     */
    private boolean canSlideAlongDirection(Piece piece, int[] direction) {
        boolean straight = direction[0] == 0 || direction[1] == 0;
        boolean diagonal = Math.abs(direction[0]) == Math.abs(direction[1]);

        if (piece instanceof Queen) {
            return straight || diagonal;
        }

        if (piece instanceof Rook) {
            return straight;
        }

        if (piece instanceof Bishop) {
            return diagonal;
        }

        return false;
    }

    /**
     * Type of pin discovered on the board.
     */
    public enum PinType {
        ABSOLUTE,
        RELATIVE
    }

    /**
     * Immutable data describing one pin.
     */
    public static final class Pin {

        private final PinType type;
        private final Position pinnedPosition;
        private final Position protectedPosition;
        private final Position attackerPosition;

        /**
         * Creates a pin record.
         *
         * @param type absolute or relative
         * @param pinnedPosition friendly piece being pinned
         * @param protectedPosition king or friendly piece being shielded
         * @param attackerPosition enemy sliding piece causing the pin
         */
        private Pin(PinType type, Position pinnedPosition, Position protectedPosition, Position attackerPosition) {
            this.type = type;
            this.pinnedPosition = pinnedPosition;
            this.protectedPosition = protectedPosition;
            this.attackerPosition = attackerPosition;
        }

        /**
         * Gets the pin type.
         *
         * @return absolute or relative
         */
        public PinType getType() {
            return type;
        }

        /**
         * Gets the pinned friendly piece position.
         *
         * @return pinned piece position
         */
        public Position getPinnedPosition() {
            return pinnedPosition;
        }

        /**
         * Gets the protected king or friendly piece position.
         *
         * @return protected position
         */
        public Position getProtectedPosition() {
            return protectedPosition;
        }

        /**
         * Gets the enemy sliding attacker position.
         *
         * @return attacker position
         */
        public Position getAttackerPosition() {
            return attackerPosition;
        }

        @Override
        public String toString() {
            return "Pin{type=" + type + ", pinnedPosition=" + pinnedPosition
                    + ", protectedPosition=" + protectedPosition
                    + ", attackerPosition=" + attackerPosition + "}";
        }
    }
}
