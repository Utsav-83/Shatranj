package chess.move;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import chess.board.ChessBoard;
import chess.board.Position;
import chess.exceptions.InvalidPositionException;
import chess.pieces.Piece;

/**
 * Detects checkmate and stalemate for the side to move.
 * <p>
 * The detector analyzes every legal move for the current player. This covers
 * king escape squares, capturing the checking piece, and blocking a sliding
 * check because all of those are simply legal moves that leave the king safe.
 */
public class GameStateDetector {

    private final CheckDetector checkDetector;
    private final MoveValidator moveValidator;

    /**
     * Creates a detector with default rule helpers.
     */
    public GameStateDetector() {
        this(new CheckDetector(), new MoveValidator());
    }

    /**
     * Creates a detector with injected rule helpers.
     *
     * @param checkDetector check detector to use
     * @param moveValidator move validator to use
     */
    public GameStateDetector(CheckDetector checkDetector, MoveValidator moveValidator) {
        if (checkDetector == null || moveValidator == null) {
            throw new IllegalArgumentException("Check detector and move validator cannot be null.");
        }

        this.checkDetector = checkDetector;
        this.moveValidator = moveValidator;
    }

    /**
     * Analyzes the current game state for one side.
     *
     * @param chessBoard current board state
     * @param whiteTurn {@code true} when white is to move
     * @return checkmate, stalemate, check, or active state
     */
    public GameState analyze(ChessBoard chessBoard, boolean whiteTurn) {
        validateBoard(chessBoard);

        boolean inCheck = checkDetector.isKingInCheck(chessBoard, whiteTurn);
        List<Move> legalMoves = getAllLegalMoves(chessBoard, whiteTurn);

        if (legalMoves.isEmpty() && inCheck) {
            return GameState.checkmate(whiteTurn);
        }

        if (legalMoves.isEmpty()) {
            return GameState.stalemate(whiteTurn);
        }

        if (inCheck) {
            return GameState.check(whiteTurn);
        }

        return GameState.active(whiteTurn);
    }

    /**
     * Checks whether the side to move is checkmated.
     *
     * @param chessBoard current board state
     * @param whiteTurn {@code true} when white is to move
     * @return {@code true} if the side is checkmated
     */
    public boolean isCheckmate(ChessBoard chessBoard, boolean whiteTurn) {
        return analyze(chessBoard, whiteTurn).getType() == GameStateType.CHECKMATE;
    }

    /**
     * Checks whether the side to move is stalemated.
     *
     * @param chessBoard current board state
     * @param whiteTurn {@code true} when white is to move
     * @return {@code true} if the side is stalemated
     */
    public boolean isStalemate(ChessBoard chessBoard, boolean whiteTurn) {
        return analyze(chessBoard, whiteTurn).getType() == GameStateType.STALEMATE;
    }

    /**
     * Gets every legal move for the side to move.
     *
     * @param chessBoard current board state
     * @param whiteTurn {@code true} when white is to move
     * @return legal moves for all pieces of the side
     */
    public List<Move> getAllLegalMoves(ChessBoard chessBoard, boolean whiteTurn) {
        validateBoard(chessBoard);

        List<Move> legalMoves = new ArrayList<>();

        for (int row = 0; row < ChessBoard.BOARD_SIZE; row++) {
            for (int column = 0; column < ChessBoard.BOARD_SIZE; column++) {
                Position source = new Position(row, column);
                Piece piece = chessBoard.getPiece(source);

                if (piece != null && piece.isWhite() == whiteTurn) {
                    addLegalMovesForPiece(chessBoard, whiteTurn, source, legalMoves);
                }
            }
        }

        return legalMoves;
    }

    /**
     * Checks whether the side to move has at least one legal escape.
     *
     * @param chessBoard current board state
     * @param whiteTurn {@code true} when white is to move
     * @return {@code true} if any legal move exists
     */
    public boolean hasLegalMove(ChessBoard chessBoard, boolean whiteTurn) {
        return !getAllLegalMoves(chessBoard, whiteTurn).isEmpty();
    }

    /**
     * Adds every legal destination for one piece.
     *
     * @param chessBoard current board state
     * @param whiteTurn {@code true} when white is to move
     * @param source source square
     * @param legalMoves output move list
     */
    private void addLegalMovesForPiece(ChessBoard chessBoard, boolean whiteTurn, Position source,
            List<Move> legalMoves) {
        List<Position> destinations = moveValidator.getLegalMovesForTurn(source, chessBoard, whiteTurn);

        for (Position destination : destinations) {
            legalMoves.add(new Move(source, destination));
        }
    }

    /**
     * Validates board input.
     *
     * @param chessBoard board to check
     */
    private void validateBoard(ChessBoard chessBoard) {
        if (chessBoard == null) {
            throw new InvalidPositionException("Board cannot be null when detecting game state.", null);
        }
    }

    /**
     * Possible game states for the side to move.
     */
    public enum GameStateType {
        ACTIVE,
        CHECK,
        CHECKMATE,
        STALEMATE
    }

    /**
     * Immutable result of game-state analysis.
     */
    public static final class GameState {

        private final GameStateType type;
        private final boolean whiteTurn;

        /**
         * Creates a game-state result.
         *
         * @param type state type
         * @param whiteTurn side to move
         */
        private GameState(GameStateType type, boolean whiteTurn) {
            this.type = type;
            this.whiteTurn = whiteTurn;
        }

        /**
         * Creates an active-state result.
         *
         * @param whiteTurn side to move
         * @return active result
         */
        public static GameState active(boolean whiteTurn) {
            return new GameState(GameStateType.ACTIVE, whiteTurn);
        }

        /**
         * Creates a check-state result.
         *
         * @param whiteTurn side to move
         * @return check result
         */
        public static GameState check(boolean whiteTurn) {
            return new GameState(GameStateType.CHECK, whiteTurn);
        }

        /**
         * Creates a checkmate-state result.
         *
         * @param whiteTurn side to move
         * @return checkmate result
         */
        public static GameState checkmate(boolean whiteTurn) {
            return new GameState(GameStateType.CHECKMATE, whiteTurn);
        }

        /**
         * Creates a stalemate-state result.
         *
         * @param whiteTurn side to move
         * @return stalemate result
         */
        public static GameState stalemate(boolean whiteTurn) {
            return new GameState(GameStateType.STALEMATE, whiteTurn);
        }

        /**
         * Gets the state type.
         *
         * @return state type
         */
        public GameStateType getType() {
            return type;
        }

        /**
         * Checks whether white is the analyzed side.
         *
         * @return {@code true} if white was analyzed
         */
        public boolean isWhiteTurn() {
            return whiteTurn;
        }

        /**
         * Gets the analyzed side name.
         *
         * @return "White" or "Black"
         */
        public String getPlayerName() {
            return whiteTurn ? "White" : "Black";
        }

        /**
         * Gets legal move list for states that do not store move detail.
         *
         * @return empty list
         */
        public List<Move> getLegalMoves() {
            return Collections.emptyList();
        }
    }
}
