package chess.move;

import chess.board.ChessBoard;
import chess.board.Position;
import chess.pieces.King;
import chess.pieces.Piece;

/**
 * Command that executes one chess move and restores the exact board state on
 * undo.
 */
public class MoveCommand implements Command {

    private final ChessBoard chessBoard;
    private final Move move;

    private Piece movedPiece;
    private Piece destinationPieceBeforeMove;
    private Piece capturedPiece;
    private Piece promotedPiece;
    private boolean movedPieceMovedBefore;
    private boolean capturedPieceMovedBefore;
    private Position previousEnPassantTarget;
    private Position nextEnPassantTarget;
    private Move previousLastMove;
    private Move nextLastMove;

    private Position rookSource;
    private Position rookDestination;
    private Piece castlingRook;
    private boolean castlingRookMovedBefore;

    /**
     * Creates a move command.
     *
     * @param chessBoard board to mutate
     * @param move move to execute
     */
    public MoveCommand(ChessBoard chessBoard, Move move) {
        if (chessBoard == null || move == null) {
            throw new IllegalArgumentException("Board and move are required.");
        }

        this.chessBoard = chessBoard;
        this.move = move;
    }

    /**
     * Executes the move and records enough state to undo it.
     */
    @Override
    public void execute() {
        captureStateBeforeMove();
        capturedPiece = chessBoard.movePiece(move);
        if (capturedPiece != null && destinationPieceBeforeMove == null) {
            capturedPieceMovedBefore = capturedPiece.hasMoved();
        }
        promotedPiece = chessBoard.getPiece(move.getDestination());
        nextEnPassantTarget = chessBoard.getEnPassantTarget();
        nextLastMove = chessBoard.getLastMove();
        captureCastlingStateAfterMove();
    }

    /**
     * Restores the board to the state before this move.
     */
    @Override
    public void undo() {
        restoreMovedPiece();
        restoreCapturedPiece();
        restoreCastlingRook();
        movedPiece.setMoved(movedPieceMovedBefore);
        restoreMoveState(previousLastMove, previousEnPassantTarget);
        chessBoard.notifyObservers();
    }

    /**
     * Replays this move from the restored pre-move state.
     */
    @Override
    public void redo() {
        execute();
    }

    /**
     * Gets the move represented by this command.
     *
     * @return move
     */
    public Move getMove() {
        return move;
    }

    /**
     * Gets the captured piece from the executed move.
     *
     * @return captured piece, or {@code null}
     */
    public Piece getCapturedPiece() {
        return capturedPiece;
    }

    /**
     * Gets the moved piece.
     *
     * @return moved piece
     */
    public Piece getMovedPiece() {
        return movedPiece;
    }

    /**
     * Captures board state before execution.
     */
    private void captureStateBeforeMove() {
        clearExecutionSnapshot();
        movedPiece = chessBoard.getPiece(move.getSource());
        destinationPieceBeforeMove = chessBoard.getPiece(move.getDestination());
        movedPieceMovedBefore = movedPiece.hasMoved();
        previousEnPassantTarget = chessBoard.getEnPassantTarget();
        previousLastMove = chessBoard.getLastMove();

        if (destinationPieceBeforeMove != null) {
            capturedPieceMovedBefore = destinationPieceBeforeMove.hasMoved();
        }

        captureCastlingStateBeforeMove();
    }

    /**
     * Clears per-execution state before this command is executed or redone.
     */
    private void clearExecutionSnapshot() {
        destinationPieceBeforeMove = null;
        capturedPiece = null;
        promotedPiece = null;
        capturedPieceMovedBefore = false;
        nextEnPassantTarget = null;
        nextLastMove = null;
        rookSource = null;
        rookDestination = null;
        castlingRook = null;
        castlingRookMovedBefore = false;
    }

    /**
     * Captures rook state before a castling move.
     */
    private void captureCastlingStateBeforeMove() {
        if (!(movedPiece instanceof King) || !move.isCastling()) {
            return;
        }

        int row = move.getSource().getRow();
        boolean kingSide = move.getDestination().getColumn() > move.getSource().getColumn();
        rookSource = new Position(row, kingSide ? 7 : 0);
        rookDestination = new Position(row, kingSide ? 5 : 3);
        castlingRook = chessBoard.getPiece(rookSource);

        if (castlingRook != null) {
            castlingRookMovedBefore = castlingRook.hasMoved();
        }
    }

    /**
     * Updates castling rook reference after execution.
     */
    private void captureCastlingStateAfterMove() {
        if (rookDestination != null) {
            castlingRook = chessBoard.getPiece(rookDestination);
        }
    }

    /**
     * Restores source and destination squares for the moved piece.
     */
    private void restoreMovedPiece() {
        chessBoard.getSquare(move.getDestination()).removePiece();
        chessBoard.getSquare(move.getSource()).setPiece(movedPiece);
    }

    /**
     * Restores captured pieces, including en passant captures.
     */
    private void restoreCapturedPiece() {
        if (move.isEnPassant()) {
            chessBoard.getSquare(move.getDestination()).removePiece();

            if (capturedPiece != null) {
                capturedPiece.setMoved(capturedPieceMovedBefore);
                chessBoard.getSquare(move.getEnPassantCapturedPosition()).setPiece(capturedPiece);
            }

            return;
        }

        if (capturedPiece != null) {
            capturedPiece.setMoved(capturedPieceMovedBefore);
            chessBoard.getSquare(move.getDestination()).setPiece(capturedPiece);
        } else {
            chessBoard.getSquare(move.getDestination()).setPiece(destinationPieceBeforeMove);
        }
    }

    /**
     * Restores castling rook to its original square.
     */
    private void restoreCastlingRook() {
        if (rookSource == null || rookDestination == null || castlingRook == null) {
            return;
        }

        chessBoard.getSquare(rookDestination).removePiece();
        chessBoard.getSquare(rookSource).setPiece(castlingRook);
        castlingRook.setMoved(castlingRookMovedBefore);
    }

    /**
     * Restores board-level move state.
     *
     * @param lastMove last move to restore
     * @param enPassantTarget en passant target to restore
     */
    private void restoreMoveState(Move lastMove, Position enPassantTarget) {
        chessBoard.setLastMove(lastMove);
        chessBoard.setEnPassantTarget(enPassantTarget);
    }
}
