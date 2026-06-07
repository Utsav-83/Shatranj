package chess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import chess.board.ChessBoard;
import chess.board.Position;
import chess.board.Square;
import chess.exceptions.InvalidPositionException;
import chess.pieces.Bishop;
import chess.pieces.King;
import chess.pieces.Knight;
import chess.pieces.Pawn;
import chess.pieces.Piece;
import chess.pieces.Queen;
import chess.pieces.Rook;

class PieceTests {

    @Test
    void pieceSymbolsColorsAndMovedFlagAreConsistent() {
        Piece whiteKing = new King(true);
        Piece blackQueen = new Queen(false);
        Piece whitePawn = new Pawn(true);

        assertTrue(whiteKing.isWhite());
        assertFalse(blackQueen.isWhite());
        assertEquals("WK", whiteKing.getSymbol());
        assertEquals("BQ", blackQueen.getSymbol());
        assertEquals("WP", whitePawn.getSymbol());
        assertFalse(whitePawn.hasMoved());

        whitePawn.setMoved(true);

        assertTrue(whitePawn.hasMoved());
    }

    @Test
    void startingPositionPlacesAllPiecesOnExpectedSquares() {
        ChessBoard board = new ChessBoard();
        board.setupStartingPosition();

        assertInstanceOf(Rook.class, board.getPiece(pos(0, 0)));
        assertInstanceOf(Knight.class, board.getPiece(pos(0, 1)));
        assertInstanceOf(Bishop.class, board.getPiece(pos(0, 2)));
        assertInstanceOf(Queen.class, board.getPiece(pos(0, 3)));
        assertInstanceOf(King.class, board.getPiece(pos(0, 4)));
        assertInstanceOf(Pawn.class, board.getPiece(pos(1, 7)));

        assertInstanceOf(Rook.class, board.getPiece(pos(7, 7)));
        assertInstanceOf(Knight.class, board.getPiece(pos(7, 6)));
        assertInstanceOf(Bishop.class, board.getPiece(pos(7, 5)));
        assertInstanceOf(Queen.class, board.getPiece(pos(7, 3)));
        assertInstanceOf(King.class, board.getPiece(pos(7, 4)));
        assertInstanceOf(Pawn.class, board.getPiece(pos(6, 0)));

        assertEquals(32, countPieces(board));
        assertNull(board.getEnPassantTarget());
        assertNull(board.getLastMove());
    }

    @Test
    void positionAndSquareRejectInvalidInputsAndExposeState() {
        Position position = pos(3, 4);
        Square square = new Square(position);
        Piece rook = new Rook(true);

        assertEquals(3, position.getRow());
        assertEquals(4, position.getColumn());
        assertEquals(position, square.getPosition());
        assertFalse(square.hasPiece());

        square.setPiece(rook);

        assertTrue(square.hasPiece());
        assertEquals(rook, square.removePiece());
        assertFalse(square.hasPiece());
        assertThrows(InvalidPositionException.class, () -> new Position(-1, 0));
        assertThrows(InvalidPositionException.class, () -> new Position(0, 8));
        assertThrows(IllegalArgumentException.class, () -> new Square(null));
    }

    @Test
    void pawnMovesIncludeOpeningDoubleStepCapturesAndBlockedEdges() {
        ChessBoard board = kingsOnlyBoard();
        Pawn whitePawn = new Pawn(true);
        Pawn blackPawn = new Pawn(false);
        board.placePiece(whitePawn, pos(6, 3));
        board.placePiece(blackPawn, pos(1, 4));

        assertMoves(whitePawn.getLegalMoves(pos(6, 3), board), pos(5, 3), pos(4, 3));
        assertMoves(blackPawn.getLegalMoves(pos(1, 4), board), pos(2, 4), pos(3, 4));

        board.placePiece(new Knight(false), pos(5, 2));
        board.placePiece(new Bishop(false), pos(5, 4));
        board.placePiece(new Rook(true), pos(5, 3));

        assertMoves(whitePawn.getLegalMoves(pos(6, 3), board), pos(5, 2), pos(5, 4));
        assertMoves(whitePawn.getAttackPositions(pos(6, 3), board), pos(5, 2), pos(5, 4));
    }

    @Test
    void slidingPiecesStopAtFriendlyPiecesAndCanCaptureEnemies() {
        ChessBoard board = kingsOnlyBoard();
        Queen queen = new Queen(true);
        board.placePiece(queen, pos(4, 4));
        board.placePiece(new Pawn(true), pos(4, 6));
        board.placePiece(new Bishop(false), pos(4, 2));
        board.placePiece(new Knight(false), pos(2, 2));

        List<Position> moves = queen.getLegalMoves(pos(4, 4), board);

        assertTrue(moves.contains(pos(4, 5)));
        assertFalse(moves.contains(pos(4, 6)));
        assertFalse(moves.contains(pos(4, 7)));
        assertTrue(moves.contains(pos(4, 2)));
        assertFalse(moves.contains(pos(4, 1)));
        assertTrue(moves.contains(pos(2, 2)));
        assertFalse(moves.contains(pos(1, 1)));
    }

    @Test
    void knightMovesIgnoreBlockersButNotFriendlyDestination() {
        ChessBoard board = kingsOnlyBoard();
        Knight knight = new Knight(true);
        board.placePiece(knight, pos(4, 4));
        board.placePiece(new Pawn(true), pos(2, 3));
        board.placePiece(new Pawn(false), pos(2, 5));

        List<Position> moves = knight.getLegalMoves(pos(4, 4), board);

        assertFalse(moves.contains(pos(2, 3)));
        assertTrue(moves.contains(pos(2, 5)));
        assertTrue(moves.contains(pos(3, 2)));
        assertTrue(moves.contains(pos(6, 5)));
        assertEquals(7, moves.size());
    }

    @Test
    void kingMovesStayOnBoardAndIncludeAvailableCastlingCandidates() {
        ChessBoard board = new ChessBoard();
        board.clearBoard();
        King whiteKing = new King(true);
        board.placePiece(whiteKing, pos(7, 4));
        board.placePiece(new Rook(true), pos(7, 0));
        board.placePiece(new Rook(true), pos(7, 7));
        board.placePiece(new King(false), pos(0, 4));

        List<Position> moves = whiteKing.getLegalMoves(pos(7, 4), board);

        assertTrue(moves.contains(pos(7, 6)));
        assertTrue(moves.contains(pos(7, 2)));
        assertTrue(moves.contains(pos(6, 4)));
        assertTrue(new King(false).getLegalMoves(pos(0, 4), board).stream()
                .allMatch(move -> Position.isValid(move.getRow(), move.getColumn())));
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

    private static void assertMoves(List<Position> actualMoves, Position... expectedMoves) {
        assertEquals(expectedMoves.length, actualMoves.size());
        for (Position expectedMove : expectedMoves) {
            assertTrue(actualMoves.contains(expectedMove), "Missing move " + expectedMove);
        }
    }

    private static int countPieces(ChessBoard board) {
        int count = 0;
        for (int row = 0; row < ChessBoard.BOARD_SIZE; row++) {
            for (int column = 0; column < ChessBoard.BOARD_SIZE; column++) {
                if (board.getPiece(pos(row, column)) != null) {
                    count++;
                }
            }
        }
        return count;
    }
}
