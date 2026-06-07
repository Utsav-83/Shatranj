package chess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import chess.board.ChessBoard;
import chess.board.Position;
import chess.exceptions.IllegalMoveException;
import chess.exceptions.InvalidPositionException;
import chess.move.Move;
import chess.move.MoveCommand;
import chess.move.MoveHistory;
import chess.move.MoveValidator;
import chess.pieces.Bishop;
import chess.pieces.King;
import chess.pieces.Knight;
import chess.pieces.Pawn;
import chess.pieces.Piece;
import chess.pieces.Queen;
import chess.pieces.Rook;

class MoveTests {

    @Test
    void moveStoresRequiredPositionsAndOptionalMetadata() {
        Move move = new Move(pos(6, 4), pos(4, 4));
        Piece pawn = new Pawn(true);
        Piece captured = new Pawn(false);

        move.setMovedPiece(pawn);
        move.setCapturedPiece(captured);
        move.setPromotionSymbol("N");
        move.setEnPassantCapturedPosition(pos(5, 5));

        assertEquals(pos(6, 4), move.getSource());
        assertEquals(pos(4, 4), move.getDestination());
        assertSame(pawn, move.getMovedPiece());
        assertSame(captured, move.getCapturedPiece());
        assertEquals("N", move.getPromotionSymbol());
        assertTrue(move.isEnPassant());
        assertTrue(move.toString().contains("source"));
        assertThrows(InvalidPositionException.class, () -> new Move(null, pos(4, 4)));
        assertThrows(InvalidPositionException.class, () -> new Move(pos(6, 4), null));
    }

    @Test
    void validatorRejectsEmptyWrongTurnSameSquareOwnCaptureAndKingCapture() {
        ChessBoard board = kingsOnlyBoard();
        MoveValidator validator = new MoveValidator();
        board.placePiece(new Rook(true), pos(4, 4));
        board.placePiece(new Bishop(true), pos(4, 6));

        assertThrows(IllegalMoveException.class, () -> validator.validateMove(new Move(pos(3, 3), pos(3, 4)),
                board, true));
        assertThrows(IllegalMoveException.class, () -> validator.validateMove(new Move(pos(4, 4), pos(4, 4)),
                board, true));
        assertThrows(IllegalMoveException.class, () -> validator.validateMove(new Move(pos(4, 4), pos(4, 5)),
                board, false));
        assertThrows(IllegalMoveException.class, () -> validator.validateMove(new Move(pos(4, 4), pos(4, 6)),
                board, true));
        assertThrows(IllegalMoveException.class, () -> validator.validateMove(new Move(pos(4, 4), pos(0, 4)),
                board, true));
    }

    @Test
    void boardMoveCapturesEnemyAndUpdatesLastMoveAndMovedFlag() {
        ChessBoard board = kingsOnlyBoard();
        Rook rook = new Rook(true);
        Pawn target = new Pawn(false);
        board.placePiece(rook, pos(4, 4));
        board.placePiece(target, pos(4, 7));

        Move move = new Move(pos(4, 4), pos(4, 7));
        Piece captured = board.movePiece(move);

        assertSame(target, captured);
        assertSame(rook, board.getPiece(pos(4, 7)));
        assertNull(board.getPiece(pos(4, 4)));
        assertTrue(rook.hasMoved());
        assertSame(move, board.getLastMove());
        assertSame(target, move.getCapturedPiece());
    }

    @Test
    void pawnPromotionDefaultsToQueenAndSupportsAllRequestedPieces() {
        assertPromotion(null, Queen.class);
        assertPromotion("Q", Queen.class);
        assertPromotion("R", Rook.class);
        assertPromotion("B", Bishop.class);
        assertPromotion("N", Knight.class);
    }

    @Test
    void validatorRejectsInvalidPromotionSymbolBeforeMoveExecutes() {
        ChessBoard board = kingsOnlyBoard();
        Pawn pawn = new Pawn(true);
        board.placePiece(pawn, pos(1, 0));
        Move move = new Move(pos(1, 0), pos(0, 0));
        move.setPromotionSymbol("K");

        assertFalse(new MoveValidator().isLegalMove(move, board, true));
        assertThrows(IllegalMoveException.class, () -> new MoveValidator().validateMove(move, board, true));
    }

    @Test
    void enPassantTargetIsCreatedCapturedAndCleared() {
        ChessBoard board = kingsOnlyBoard();
        Pawn whitePawn = new Pawn(true);
        Pawn blackPawn = new Pawn(false);
        board.placePiece(whitePawn, pos(3, 4));
        board.placePiece(blackPawn, pos(1, 5));

        board.movePiece(new Move(pos(1, 5), pos(3, 5)));
        assertEquals(pos(2, 5), board.getEnPassantTarget());

        Move enPassant = new Move(pos(3, 4), pos(2, 5));
        assertTrue(new MoveValidator().isLegalMove(enPassant, board, true));
        Piece captured = board.movePiece(enPassant);

        assertSame(blackPawn, captured);
        assertSame(whitePawn, board.getPiece(pos(2, 5)));
        assertNull(board.getPiece(pos(3, 5)));
        assertTrue(enPassant.isEnPassant());
        assertEquals(pos(3, 5), enPassant.getEnPassantCapturedPosition());
        assertNull(board.getEnPassantTarget());
    }

    @Test
    void castlingMovesKingAndRookAndCanBeUndone() {
        ChessBoard board = new ChessBoard();
        board.clearBoard();
        board.placePiece(new King(true), pos(7, 4));
        board.placePiece(new Rook(true), pos(7, 7));
        board.placePiece(new King(false), pos(0, 4));
        Move move = new Move(pos(7, 4), pos(7, 6));

        assertTrue(new MoveValidator().isLegalMove(move, board, true));

        MoveCommand command = new MoveCommand(board, move);
        command.execute();

        assertInstanceOf(King.class, board.getPiece(pos(7, 6)));
        assertInstanceOf(Rook.class, board.getPiece(pos(7, 5)));
        assertNull(board.getPiece(pos(7, 4)));
        assertNull(board.getPiece(pos(7, 7)));

        command.undo();

        assertInstanceOf(King.class, board.getPiece(pos(7, 4)));
        assertInstanceOf(Rook.class, board.getPiece(pos(7, 7)));
        assertFalse(board.getPiece(pos(7, 4)).hasMoved());
        assertFalse(board.getPiece(pos(7, 7)).hasMoved());
    }

    @Test
    void castlingIsIllegalAfterRookMovesOrThroughAttackedSquare() {
        ChessBoard movedRookBoard = new ChessBoard();
        movedRookBoard.clearBoard();
        movedRookBoard.placePiece(new King(true), pos(7, 4));
        Rook rook = new Rook(true);
        rook.setMoved(true);
        movedRookBoard.placePiece(rook, pos(7, 7));
        movedRookBoard.placePiece(new King(false), pos(0, 4));

        assertFalse(new MoveValidator().isLegalMove(new Move(pos(7, 4), pos(7, 6)), movedRookBoard, true));

        ChessBoard attackedPathBoard = new ChessBoard();
        attackedPathBoard.clearBoard();
        attackedPathBoard.placePiece(new King(true), pos(7, 4));
        attackedPathBoard.placePiece(new Rook(true), pos(7, 7));
        attackedPathBoard.placePiece(new King(false), pos(0, 4));
        attackedPathBoard.placePiece(new Rook(false), pos(0, 5));

        assertFalse(new MoveValidator().isLegalMove(new Move(pos(7, 4), pos(7, 6)), attackedPathBoard, true));
    }

    @Test
    void moveHistorySupportsExecuteUndoRedoAndClear() {
        ChessBoard board = kingsOnlyBoard();
        board.placePiece(new Rook(true), pos(5, 0));
        MoveHistory history = new MoveHistory();
        MoveCommand command = new MoveCommand(board, new Move(pos(5, 0), pos(5, 5)));

        history.execute(command);

        assertTrue(history.canUndo());
        assertFalse(history.canRedo());
        assertInstanceOf(Rook.class, board.getPiece(pos(5, 5)));

        assertTrue(history.undo());
        assertInstanceOf(Rook.class, board.getPiece(pos(5, 0)));
        assertTrue(history.canRedo());

        assertTrue(history.redo());
        assertInstanceOf(Rook.class, board.getPiece(pos(5, 5)));

        history.clear();

        assertFalse(history.canUndo());
        assertFalse(history.canRedo());
        assertFalse(history.undo());
        assertFalse(history.redo());
        assertThrows(IllegalArgumentException.class, () -> history.execute(null));
    }

    @Test
    void boardSerializationPreservesPiecesMovedFlagsAndEnPassantTarget() {
        ChessBoard board = new ChessBoard();
        board.setupStartingPosition();
        board.movePiece(new Move(pos(6, 4), pos(4, 4)));
        String serialized = board.serializeGame();

        ChessBoard loaded = new ChessBoard();
        loaded.deserializeGame(serialized);

        assertEquals(pos(5, 4), loaded.getEnPassantTarget());
        assertInstanceOf(Pawn.class, loaded.getPiece(pos(4, 4)));
        assertTrue(loaded.getPiece(pos(4, 4)).hasMoved());
        assertEquals(serialized, loaded.serializeGame());
        assertThrows(IllegalArgumentException.class, () -> loaded.deserializeGame(null));
        assertThrows(IllegalArgumentException.class, () -> loaded.deserializeGame("bad data"));
    }

    private static void assertPromotion(String symbol, Class<? extends Piece> expectedType) {
        ChessBoard board = kingsOnlyBoard();
        Pawn pawn = new Pawn(true);
        board.placePiece(pawn, pos(1, 0));
        Move move = new Move(pos(1, 0), pos(0, 0));
        move.setPromotionSymbol(symbol);

        assertTrue(new MoveValidator().isLegalMove(move, board, true));
        board.movePiece(move);

        assertInstanceOf(expectedType, board.getPiece(pos(0, 0)));
        assertTrue(board.getPiece(pos(0, 0)).isWhite());
        assertTrue(board.getPiece(pos(0, 0)).hasMoved());
    }

    private static ChessBoard kingsOnlyBoard() {
        ChessBoard board = new ChessBoard();
        board.clearBoard();
        board.placePiece(new King(true), pos(7, 4));
        board.placePiece(new King(false), pos(0, 4));
        return board;
    }

    private static Position pos(int row, int column) {
        return new Position(row, column);
    }
}
