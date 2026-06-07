package chess.move;

import java.util.ArrayList;
import java.util.List;

import chess.board.ChessBoard;
import chess.board.Position;
import chess.exceptions.IllegalMoveException;
import chess.exceptions.InvalidPositionException;
import chess.pieces.King;
import chess.pieces.Pawn;
import chess.pieces.Piece;

/**
 * Validates whether a requested move is legal for the current board state and
 * side to move.
 * <p>
 * This class rejects empty-source moves, wrong-turn moves, own-piece captures,
 * blocked paths, board-boundary violations, and destinations not generated for
 * the selected piece.
 */
public class MoveValidator {

    private final MoveGenerator moveGenerator;
    private final PinDetector pinDetector;
    private final CheckDetector checkDetector;

    /**
     * Creates a validator with its own move generator.
     */
    public MoveValidator() {
        this(new MoveGenerator(), new PinDetector(), new CheckDetector());
    }

    /**
     * Creates a validator with an injected move generator.
     *
     * @param moveGenerator move generator to use
     */
    public MoveValidator(MoveGenerator moveGenerator) {
        this(moveGenerator, new PinDetector(), new CheckDetector());
    }

    /**
     * Creates a validator with injected rule helpers.
     *
     * @param moveGenerator move generator to use
     * @param pinDetector pin detector to use for self-check prevention
     */
    public MoveValidator(MoveGenerator moveGenerator, PinDetector pinDetector) {
        this(moveGenerator, pinDetector, new CheckDetector());
    }

    /**
     * Creates a validator with injected rule helpers.
     *
     * @param moveGenerator move generator to use
     * @param pinDetector pin detector to use for self-check prevention
     * @param checkDetector check detector to use for castling attack checks
     */
    public MoveValidator(MoveGenerator moveGenerator, PinDetector pinDetector, CheckDetector checkDetector) {
        if (moveGenerator == null || pinDetector == null || checkDetector == null) {
            throw new IllegalArgumentException("Move generator, pin detector, and check detector cannot be null.");
        }

        this.moveGenerator = moveGenerator;
        this.pinDetector = pinDetector;
        this.checkDetector = checkDetector;
    }

    /**
     * Checks whether a move is legal for the current turn.
     *
     * @param move requested move
     * @param chessBoard board state
     * @param whiteTurn {@code true} when white is to move
     * @return {@code true} if the move is legal
     */
    public boolean isLegalMove(Move move, ChessBoard chessBoard, boolean whiteTurn) {
        try {
            validateMove(move, chessBoard, whiteTurn);
            return true;
        } catch (IllegalMoveException | InvalidPositionException exception) {
            return false;
        }
    }

    /**
     * Validates a move and throws an exception when it is illegal.
     *
     * @param move requested move
     * @param chessBoard board state
     * @param whiteTurn {@code true} when white is to move
     */
    public void validateMove(Move move, ChessBoard chessBoard, boolean whiteTurn) {
        if (move == null || chessBoard == null) {
            throw new IllegalMoveException("Move and board are required before validation.");
        }

        Position source = move.getSource();
        Position destination = move.getDestination();

        if (source.equals(destination)) {
            throw new IllegalMoveException("Source and destination must be different.");
        }

        Piece movingPiece = chessBoard.getPiece(source);
        if (movingPiece == null) {
            throw new IllegalMoveException("Cannot move from an empty square.");
        }

        if (movingPiece.isWhite() != whiteTurn) {
            throw new IllegalMoveException("It is not this piece's turn.");
        }

        Piece destinationPiece = chessBoard.getPiece(destination);
        if (destinationPiece != null && destinationPiece.isWhite() == movingPiece.isWhite()) {
            throw new IllegalMoveException("Cannot capture your own piece.");
        }

        if (destinationPiece instanceof King) {
            throw new IllegalMoveException("The king cannot be captured. This position is check.");
        }

        List<Position> legalMoves = moveGenerator.generateLegalMoves(movingPiece, source, chessBoard);
        if (!legalMoves.contains(destination)) {
            throw new IllegalMoveException("Illegal move for selected piece.");
        }

        if (movingPiece instanceof King && move.isCastling()) {
            validateCastling(move, chessBoard, movingPiece);
        }

        if (movingPiece instanceof Pawn && isPromotionDestination(destination, movingPiece.isWhite())
                && !isValidPromotionSymbol(move.getPromotionSymbol())) {
            throw new IllegalMoveException("Pawn promotion must choose queen, rook, bishop, or knight.");
        }

        if (pinDetector.wouldLeaveKingInCheck(chessBoard, move)) {
            if (pinDetector.isAbsolutelyPinned(chessBoard, source)) {
                throw new IllegalMoveException("Illegal move. This piece is pinned and moving it exposes the king.");
            }

            throw new IllegalMoveException("Illegal move. You cannot leave your king in check.");
        }
    }

    /**
     * Gets legal moves for a piece only if it belongs to the current turn.
     *
     * @param source source position
     * @param chessBoard board state
     * @param whiteTurn {@code true} when white is to move
     * @return legal destination positions
     */
    public List<Position> getLegalMovesForTurn(Position source, ChessBoard chessBoard, boolean whiteTurn) {
        if (source == null || chessBoard == null) {
            throw new InvalidPositionException("Source position and board are required.", null);
        }

        Piece piece = chessBoard.getPiece(source);
        if (piece == null || piece.isWhite() != whiteTurn) {
            return java.util.Collections.emptyList();
        }

        List<Position> pseudoLegalMoves = moveGenerator.generateLegalMoves(piece, source, chessBoard);
        List<Position> legalMoves = new ArrayList<>(pinDetector.filterSelfCheckMoves(chessBoard, source,
                pseudoLegalMoves));
        legalMoves.removeIf(destination -> chessBoard.getPiece(destination) instanceof King);
        legalMoves.removeIf(destination -> !isLegalMove(new Move(source, destination), chessBoard, whiteTurn));
        return legalMoves;
    }

    /**
     * Validates official castling restrictions after pseudo move generation.
     *
     * @param move requested castling move
     * @param chessBoard board state
     * @param king king piece
     */
    private void validateCastling(Move move, ChessBoard chessBoard, Piece king) {
        boolean kingSide = move.getDestination().getColumn() > move.getSource().getColumn();

        if (!chessBoard.canCastle(king.isWhite(), kingSide)) {
            throw new IllegalMoveException("Castling is not allowed after the king or rook has moved.");
        }

        if (checkDetector.isKingInCheck(chessBoard, king.isWhite())) {
            throw new IllegalMoveException("Cannot castle while in check.");
        }

        int row = move.getSource().getRow();
        int direction = kingSide ? 1 : -1;

        for (int step = 1; step <= 2; step++) {
            Position transitSquare = new Position(row, move.getSource().getColumn() + (direction * step));

            if (checkDetector.isSquareAttacked(chessBoard, transitSquare, !king.isWhite())) {
                throw new IllegalMoveException("Cannot castle through or into check.");
            }
        }
    }

    /**
     * Checks whether a pawn destination is the promotion rank.
     *
     * @param destination destination square
     * @param whitePawn {@code true} for white pawn
     * @return {@code true} if promotion is required
     */
    private boolean isPromotionDestination(Position destination, boolean whitePawn) {
        return whitePawn ? destination.getRow() == 0 : destination.getRow() == ChessBoard.BOARD_SIZE - 1;
    }

    /**
     * Checks promotion piece code.
     *
     * @param promotionSymbol promotion code
     * @return {@code true} if valid or absent
     */
    private boolean isValidPromotionSymbol(String promotionSymbol) {
        return promotionSymbol == null || "Q".equals(promotionSymbol) || "R".equals(promotionSymbol)
                || "B".equals(promotionSymbol) || "N".equals(promotionSymbol);
    }
}
