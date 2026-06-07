package chess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import chess.board.ChessBoard;
import chess.board.Position;
import chess.exceptions.InvalidPositionException;
import chess.move.GameStateDetector;
import chess.move.GameStateDetector.GameState;
import chess.move.GameStateDetector.GameStateType;
import chess.move.Move;
import chess.pieces.King;
import chess.pieces.Queen;
import chess.pieces.Rook;

class CheckmateTests {

    @Test
    void detectorRecognizesCornerQueenAndKingCheckmate() {
        ChessBoard board = new ChessBoard();
        board.clearBoard();
        board.placePiece(new King(false), pos(0, 0));
        board.placePiece(new Queen(true), pos(1, 1));
        board.placePiece(new King(true), pos(2, 2));
        GameStateDetector detector = new GameStateDetector();

        GameState state = detector.analyze(board, false);

        assertEquals(GameStateType.CHECKMATE, state.getType());
        assertFalse(state.isWhiteTurn());
        assertEquals("Black", state.getPlayerName());
        assertTrue(detector.isCheckmate(board, false));
        assertFalse(detector.hasLegalMove(board, false));
        assertEquals(List.of(), state.getLegalMoves());
    }

    @Test
    void detectorRecognizesStalemateWhenKingIsTrappedButNotChecked() {
        ChessBoard board = new ChessBoard();
        board.clearBoard();
        board.placePiece(new King(false), pos(0, 0));
        board.placePiece(new Queen(true), pos(1, 2));
        board.placePiece(new King(true), pos(2, 2));
        GameStateDetector detector = new GameStateDetector();

        GameState state = detector.analyze(board, false);

        assertEquals(GameStateType.STALEMATE, state.getType());
        assertTrue(detector.isStalemate(board, false));
        assertFalse(detector.isCheckmate(board, false));
        assertFalse(detector.hasLegalMove(board, false));
    }

    @Test
    void detectorReportsCheckWhenSideHasLegalEscape() {
        ChessBoard board = new ChessBoard();
        board.clearBoard();
        board.placePiece(new King(true), pos(7, 4));
        board.placePiece(new King(false), pos(0, 4));
        board.placePiece(new Rook(false), pos(7, 0));
        GameStateDetector detector = new GameStateDetector();

        GameState state = detector.analyze(board, true);

        assertEquals(GameStateType.CHECK, state.getType());
        assertTrue(state.isWhiteTurn());
        assertEquals("White", state.getPlayerName());
        assertFalse(detector.isCheckmate(board, true));
        assertTrue(detector.hasLegalMove(board, true));
    }

    @Test
    void detectorReportsActiveStateForNormalOpeningPosition() {
        ChessBoard board = new ChessBoard();
        board.setupStartingPosition();
        GameStateDetector detector = new GameStateDetector();

        GameState state = detector.analyze(board, true);

        assertEquals(GameStateType.ACTIVE, state.getType());
        assertTrue(state.isWhiteTurn());
        assertEquals("White", state.getPlayerName());
        assertTrue(detector.hasLegalMove(board, true));
        assertFalse(detector.isCheckmate(board, true));
        assertFalse(detector.isStalemate(board, true));
    }

    @Test
    void foolMateSequenceEndsInWhiteCheckmate() {
        ChessBoard board = new ChessBoard();
        board.setupStartingPosition();
        board.movePiece(new Move(pos(6, 5), pos(5, 5)));
        board.movePiece(new Move(pos(1, 4), pos(3, 4)));
        board.movePiece(new Move(pos(6, 6), pos(4, 6)));
        board.movePiece(new Move(pos(0, 3), pos(4, 7)));

        GameStateDetector detector = new GameStateDetector();
        GameState state = detector.analyze(board, true);

        assertEquals(GameStateType.CHECKMATE, state.getType());
        assertTrue(detector.isCheckmate(board, true));
        assertFalse(detector.hasLegalMove(board, true));
    }

    @Test
    void checkmateCanBeAvoidedByCapturingUndefendedCheckingPiece() {
        ChessBoard board = new ChessBoard();
        board.clearBoard();
        board.placePiece(new King(false), pos(0, 0));
        board.placePiece(new Queen(true), pos(1, 1));
        board.placePiece(new King(true), pos(3, 3));
        GameStateDetector detector = new GameStateDetector();

        GameState state = detector.analyze(board, false);

        assertEquals(GameStateType.CHECK, state.getType());
        assertFalse(detector.isCheckmate(board, false));
        assertTrue(detector.getAllLegalMoves(board, false).stream()
                .anyMatch(move -> move.getDestination().equals(pos(1, 1))));
    }

    @Test
    void detectorRejectsNullBoardInput() {
        GameStateDetector detector = new GameStateDetector();

        assertThrows(InvalidPositionException.class, () -> detector.analyze(null, true));
        assertThrows(InvalidPositionException.class, () -> detector.getAllLegalMoves(null, true));
    }

    private static Position pos(int row, int column) {
        return new Position(row, column);
    }
}
