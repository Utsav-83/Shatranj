package chess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import chess.board.ChessBoard;
import chess.board.Position;
import chess.exceptions.CheckException;
import chess.exceptions.IllegalMoveException;
import chess.exceptions.InvalidPositionException;
import chess.move.CheckDetector;
import chess.move.Move;
import chess.move.MoveValidator;
import chess.move.PinDetector;
import chess.move.PinDetector.Pin;
import chess.move.PinDetector.PinType;
import chess.pieces.Bishop;
import chess.pieces.King;
import chess.pieces.Knight;
import chess.pieces.Pawn;
import chess.pieces.Queen;
import chess.pieces.Rook;

class CheckTests {

    @Test
    void checkDetectorFindsKingsAndReportsNoCheckOnQuietBoard() {
        ChessBoard board = kingsOnlyBoard();
        CheckDetector detector = new CheckDetector();

        assertEquals(pos(7, 4), detector.findKingPosition(board, true));
        assertEquals(pos(0, 4), detector.findKingPosition(board, false));
        assertFalse(detector.isKingInCheck(board, true));
        assertFalse(detector.isKingInCheck(board, false));
    }

    @Test
    void checkDetectorFindsSlidingKnightAndPawnChecks() {
        assertSingleChecker(new Rook(false), pos(7, 0), pos(7, 4));
        assertSingleChecker(new Bishop(false), pos(4, 1), pos(7, 4));
        assertSingleChecker(new Queen(false), pos(4, 4), pos(7, 4));
        assertSingleChecker(new Knight(false), pos(5, 3), pos(7, 4));
        assertSingleChecker(new Pawn(false), pos(6, 3), pos(7, 4));
    }

    @Test
    void blockedSliderDoesNotGiveCheckUntilBlockerMoves() {
        ChessBoard board = kingsOnlyBoard();
        CheckDetector detector = new CheckDetector();
        board.placePiece(new Rook(false), pos(7, 0));
        board.placePiece(new Bishop(true), pos(7, 2));

        assertFalse(detector.isKingInCheck(board, true));

        board.removePiece(pos(7, 2));

        assertTrue(detector.isKingInCheck(board, true));
        assertEquals(List.of(pos(7, 0)), detector.findCheckingPieces(board, true));
    }

    @Test
    void squareAttackDetectionUsesPawnAttackSquaresNotPawnForwardMoves() {
        ChessBoard board = kingsOnlyBoard();
        CheckDetector detector = new CheckDetector();
        board.placePiece(new Pawn(false), pos(3, 3));

        assertTrue(detector.isSquareAttacked(board, pos(4, 2), false));
        assertTrue(detector.isSquareAttacked(board, pos(4, 4), false));
        assertFalse(detector.isSquareAttacked(board, pos(4, 3), false));
    }

    @Test
    void missingKingAndNullInputsThrowUsefulExceptions() {
        ChessBoard board = new ChessBoard();
        board.clearBoard();
        board.placePiece(new King(false), pos(0, 4));
        CheckDetector detector = new CheckDetector();

        assertThrows(CheckException.class, () -> detector.findKingPosition(board, true));
        assertThrows(InvalidPositionException.class, () -> detector.findKingPosition(null, true));
        assertThrows(InvalidPositionException.class, () -> detector.findAttackers(board, null, true));
    }

    @Test
    void pinnedPieceCannotExposeKingButMayMoveAlongPinLine() {
        ChessBoard board = kingsOnlyBoard();
        board.placePiece(new Rook(true), pos(7, 2));
        board.placePiece(new Rook(false), pos(7, 0));
        MoveValidator validator = new MoveValidator();

        assertFalse(validator.isLegalMove(new Move(pos(7, 2), pos(6, 2)), board, true));
        assertThrows(IllegalMoveException.class, () -> validator.validateMove(new Move(pos(7, 2), pos(6, 2)),
                board, true));
        assertTrue(validator.isLegalMove(new Move(pos(7, 2), pos(7, 0)), board, true));
    }

    @Test
    void pinDetectorReportsAbsoluteAndRelativePins() {
        ChessBoard board = kingsOnlyBoard();
        PinDetector detector = new PinDetector();
        board.placePiece(new Bishop(true), pos(6, 4));
        board.placePiece(new Rook(false), pos(5, 4));

        List<Pin> absolutePins = detector.findAbsolutePins(board, true);

        assertEquals(1, absolutePins.size());
        assertEquals(PinType.ABSOLUTE, absolutePins.get(0).getType());
        assertEquals(pos(6, 4), absolutePins.get(0).getPinnedPosition());
        assertEquals(pos(7, 4), absolutePins.get(0).getProtectedPosition());
        assertEquals(pos(5, 4), absolutePins.get(0).getAttackerPosition());
        assertTrue(detector.isAbsolutelyPinned(board, pos(6, 4)));

        board.placePiece(new Queen(true), pos(4, 4));
        board.placePiece(new Rook(true), pos(3, 4));
        board.placePiece(new Rook(false), pos(2, 4));

        List<Pin> relativePins = detector.findRelativePins(board, true);

        assertTrue(relativePins.stream().anyMatch(pin -> pin.getType() == PinType.RELATIVE
                && pin.getPinnedPosition().equals(pos(3, 4))
                && pin.getProtectedPosition().equals(pos(4, 4))
                && pin.getAttackerPosition().equals(pos(2, 4))));
    }

    @Test
    void validatorRejectsMovingKingIntoCheckAndAllowsEscapingCheck() {
        ChessBoard board = kingsOnlyBoard();
        board.placePiece(new Rook(false), pos(0, 5));
        MoveValidator validator = new MoveValidator();

        assertFalse(validator.isLegalMove(new Move(pos(7, 4), pos(7, 5)), board, true));

        ChessBoard checkedBoard = kingsOnlyBoard();
        checkedBoard.placePiece(new Rook(false), pos(7, 0));

        assertTrue(new CheckDetector().isKingInCheck(checkedBoard, true));
        assertTrue(validator.isLegalMove(new Move(pos(7, 4), pos(6, 4)), checkedBoard, true));
    }

    private static void assertSingleChecker(chess.pieces.Piece checker, Position checkerPosition,
            Position kingPosition) {
        ChessBoard board = new ChessBoard();
        board.clearBoard();
        board.placePiece(new King(true), kingPosition);
        board.placePiece(new King(false), pos(0, 7));
        board.placePiece(checker, checkerPosition);
        CheckDetector detector = new CheckDetector();

        assertTrue(detector.isKingInCheck(board, true));
        assertEquals(List.of(checkerPosition), detector.findCheckingPieces(board, true));
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
