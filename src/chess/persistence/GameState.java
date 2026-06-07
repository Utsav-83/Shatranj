package chess.persistence;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import chess.board.ChessBoard;
import chess.board.Position;
import chess.move.Move;
import chess.pieces.Piece;

/**
 * Serializable snapshot of a chess game.
 * <p>
 * The board is stored using {@link ChessBoard#serializeGame()} so piece
 * positions, moved flags, and en passant state are preserved. Move history is
 * stored as serializable value objects rather than command objects.
 */
public class GameState implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String boardState;
    private final ArrayList<SavedMove> moveHistory;
    private final boolean whiteTurn;
    private final long whiteRemainingMillis;
    private final long blackRemainingMillis;
    private final long savedAtMillis;

    /**
     * Creates a serializable game state.
     *
     * @param boardState serialized board data
     * @param moveHistory move history records
     * @param whiteTurn {@code true} if white is to move
     * @param whiteRemainingMillis white clock time
     * @param blackRemainingMillis black clock time
     */
    public GameState(String boardState, List<SavedMove> moveHistory, boolean whiteTurn, long whiteRemainingMillis,
            long blackRemainingMillis) {
        if (boardState == null || boardState.trim().isEmpty()) {
            throw new IllegalArgumentException("Board state cannot be empty.");
        }

        if (whiteRemainingMillis < 0 || blackRemainingMillis < 0) {
            throw new IllegalArgumentException("Timer values cannot be negative.");
        }

        this.boardState = boardState;
        this.moveHistory = new ArrayList<>(moveHistory == null ? Collections.emptyList() : moveHistory);
        this.whiteTurn = whiteTurn;
        this.whiteRemainingMillis = whiteRemainingMillis;
        this.blackRemainingMillis = blackRemainingMillis;
        this.savedAtMillis = System.currentTimeMillis();
    }

    /**
     * Creates a snapshot from live board data.
     *
     * @param chessBoard board to serialize
     * @param moves moves to save
     * @param whiteTurn {@code true} if white is to move
     * @param whiteRemainingMillis white clock time
     * @param blackRemainingMillis black clock time
     * @return serializable state
     */
    public static GameState fromBoard(ChessBoard chessBoard, List<Move> moves, boolean whiteTurn,
            long whiteRemainingMillis, long blackRemainingMillis) {
        if (chessBoard == null) {
            throw new IllegalArgumentException("Chess board cannot be null.");
        }

        ArrayList<SavedMove> savedMoves = new ArrayList<>();
        if (moves != null) {
            for (Move move : moves) {
                savedMoves.add(SavedMove.fromMove(move));
            }
        }

        return new GameState(chessBoard.serializeGame(), savedMoves, whiteTurn, whiteRemainingMillis,
                blackRemainingMillis);
    }

    /**
     * Restores this state's board data into the supplied board.
     *
     * @param chessBoard board to update
     */
    public void restoreBoard(ChessBoard chessBoard) {
        if (chessBoard == null) {
            throw new IllegalArgumentException("Chess board cannot be null.");
        }

        chessBoard.deserializeGame(boardState);
    }

    public String getBoardState() {
        return boardState;
    }

    public List<SavedMove> getMoveHistory() {
        return Collections.unmodifiableList(new ArrayList<>(moveHistory));
    }

    public boolean isWhiteTurn() {
        return whiteTurn;
    }

    public long getWhiteRemainingMillis() {
        return whiteRemainingMillis;
    }

    public long getBlackRemainingMillis() {
        return blackRemainingMillis;
    }

    public long getSavedAtMillis() {
        return savedAtMillis;
    }

    /**
     * Serializable move-history entry.
     */
    public static final class SavedMove implements Serializable {

        private static final long serialVersionUID = 1L;

        private final int sourceRow;
        private final int sourceColumn;
        private final int destinationRow;
        private final int destinationColumn;
        private final String promotionSymbol;
        private final String movedPieceSymbol;
        private final String capturedPieceSymbol;
        private final long timestampMillis;

        /**
         * Creates a saved move.
         *
         * @param sourceRow source row
         * @param sourceColumn source column
         * @param destinationRow destination row
         * @param destinationColumn destination column
         * @param promotionSymbol promotion symbol, or {@code null}
         * @param movedPieceSymbol moved piece symbol, or {@code null}
         * @param capturedPieceSymbol captured piece symbol, or {@code null}
         * @param timestampMillis save timestamp
         */
        public SavedMove(int sourceRow, int sourceColumn, int destinationRow, int destinationColumn,
                String promotionSymbol, String movedPieceSymbol, String capturedPieceSymbol, long timestampMillis) {
            this.sourceRow = sourceRow;
            this.sourceColumn = sourceColumn;
            this.destinationRow = destinationRow;
            this.destinationColumn = destinationColumn;
            this.promotionSymbol = promotionSymbol;
            this.movedPieceSymbol = movedPieceSymbol;
            this.capturedPieceSymbol = capturedPieceSymbol;
            this.timestampMillis = timestampMillis;
        }

        /**
         * Converts a live move to a saved move.
         *
         * @param move move to convert
         * @return saved move
         */
        public static SavedMove fromMove(Move move) {
            if (move == null) {
                throw new IllegalArgumentException("Move cannot be null.");
            }

            Piece movedPiece = move.getMovedPiece();
            Piece capturedPiece = move.getCapturedPiece();

            return new SavedMove(move.getSource().getRow(), move.getSource().getColumn(),
                    move.getDestination().getRow(), move.getDestination().getColumn(), move.getPromotionSymbol(),
                    movedPiece == null ? null : movedPiece.getSymbol(),
                    capturedPiece == null ? null : capturedPiece.getSymbol(), System.currentTimeMillis());
        }

        /**
         * Converts this saved entry back to a move request.
         *
         * @return move
         */
        public Move toMove() {
            Move move = new Move(new Position(sourceRow, sourceColumn), new Position(destinationRow,
                    destinationColumn));
            move.setPromotionSymbol(promotionSymbol);
            return move;
        }

        public int getSourceRow() {
            return sourceRow;
        }

        public int getSourceColumn() {
            return sourceColumn;
        }

        public int getDestinationRow() {
            return destinationRow;
        }

        public int getDestinationColumn() {
            return destinationColumn;
        }

        public String getPromotionSymbol() {
            return promotionSymbol;
        }

        public String getMovedPieceSymbol() {
            return movedPieceSymbol;
        }

        public String getCapturedPieceSymbol() {
            return capturedPieceSymbol;
        }

        public long getTimestampMillis() {
            return timestampMillis;
        }
    }
}
