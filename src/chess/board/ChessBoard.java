package chess.board;

import java.util.ArrayList;
import java.util.List;

import chess.interfaces.ObservableGame;
import chess.interfaces.SerializableGame;
import chess.exceptions.IllegalMoveException;
import chess.exceptions.InvalidPositionException;
import chess.move.Move;
import chess.pieces.Bishop;
import chess.pieces.King;
import chess.pieces.Knight;
import chess.pieces.Pawn;
import chess.pieces.Piece;
import chess.pieces.Queen;
import chess.pieces.Rook;

/**
 * Model class for an 8x8 chess board.
 * <p>
 * This class contains board state only and does not render any user interface.
 * That keeps it compatible with MVC designs where a separate view observes or
 * reads this model and a controller handles user actions.
 */
public class ChessBoard implements SerializableGame, ObservableGame {

    /** Number of rows and columns on a standard chess board. */
    public static final int BOARD_SIZE = Position.BOARD_SIZE;

    private final Square[][] squares;
    private final List<Runnable> observers;
    private Move lastMove;
    private Position enPassantTarget;

    /**
     * Creates a board and fills it with empty squares.
     */
    public ChessBoard() {
        squares = new Square[BOARD_SIZE][BOARD_SIZE];
        observers = new ArrayList<>();
        initializeBoard();
    }

    /**
     * Creates all 64 squares on the board.
     */
    private void initializeBoard() {
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int column = 0; column < BOARD_SIZE; column++) {
                squares[row][column] = new Square(new Position(row, column));
            }
        }
    }

    /**
     * Removes all pieces and places the standard starting chess position.
     */
    public void setupStartingPosition() {
        clearBoard();
        lastMove = null;
        enPassantTarget = null;

        for (int column = 0; column < BOARD_SIZE; column++) {
            setPieceWithoutNotification(new Pawn(false), new Position(1, column));
            setPieceWithoutNotification(new Pawn(true), new Position(6, column));
        }

        setPieceWithoutNotification(new Rook(false), new Position(0, 0));
        setPieceWithoutNotification(new Knight(false), new Position(0, 1));
        setPieceWithoutNotification(new Bishop(false), new Position(0, 2));
        setPieceWithoutNotification(new Queen(false), new Position(0, 3));
        setPieceWithoutNotification(new King(false), new Position(0, 4));
        setPieceWithoutNotification(new Bishop(false), new Position(0, 5));
        setPieceWithoutNotification(new Knight(false), new Position(0, 6));
        setPieceWithoutNotification(new Rook(false), new Position(0, 7));

        setPieceWithoutNotification(new Rook(true), new Position(7, 0));
        setPieceWithoutNotification(new Knight(true), new Position(7, 1));
        setPieceWithoutNotification(new Bishop(true), new Position(7, 2));
        setPieceWithoutNotification(new Queen(true), new Position(7, 3));
        setPieceWithoutNotification(new King(true), new Position(7, 4));
        setPieceWithoutNotification(new Bishop(true), new Position(7, 5));
        setPieceWithoutNotification(new Knight(true), new Position(7, 6));
        setPieceWithoutNotification(new Rook(true), new Position(7, 7));

        notifyObservers();
    }

    /**
     * Removes every piece from the board.
     */
    public void clearBoard() {
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int column = 0; column < BOARD_SIZE; column++) {
                squares[row][column].removePiece();
            }
        }

        lastMove = null;
        enPassantTarget = null;
        notifyObservers();
    }

    /**
     * Places a piece at the given position.
     *
     * @param piece the piece to place
     * @param position the target position
     * @throws IllegalArgumentException if piece or position is {@code null}
     */
    public void placePiece(Piece piece, Position position) {
        setPieceWithoutNotification(piece, position);
        notifyObservers();
    }

    /**
     * Gets the square at the given position.
     *
     * @param position the position to read
     * @return the square at the position
     * @throws IllegalArgumentException if position is {@code null}
     */
    public Square getSquare(Position position) {
        if (position == null) {
            throw new IllegalArgumentException("Position cannot be null.");
        }

        return squares[position.getRow()][position.getColumn()];
    }

    /**
     * Gets the piece at the given position.
     *
     * @param position the position to read
     * @return the piece at the position, or {@code null} if the square is empty
     */
    public Piece getPiece(Position position) {
        return getSquare(position).getPiece();
    }

    /**
     * Removes and returns the piece at the given position.
     *
     * @param position the position to clear
     * @return the removed piece, or {@code null} if the square was empty
     */
    public Piece removePiece(Position position) {
        Piece removedPiece = getSquare(position).removePiece();
        notifyObservers();
        return removedPiece;
    }

    /**
     * Moves a piece from one square to another and returns any captured piece.
     *
     * @param source source position
     * @param destination destination position
     * @return captured piece, or {@code null} if the destination was empty
     * @throws IllegalArgumentException if the source or destination is invalid
     * @throws IllegalStateException if the source square is empty
     */
    public Piece movePiece(Position source, Position destination) {
        return movePiece(new Move(source, destination));
    }

    /**
     * Moves a piece using a move object and stores move metadata.
     *
     * @param move move to execute
     * @return captured piece, or {@code null} if no capture occurred
     */
    public Piece movePiece(Move move) {
        if (move == null) {
            throw new IllegalMoveException("Move cannot be null.");
        }

        Position source = move.getSource();
        Position destination = move.getDestination();

        if (source == null || destination == null) {
            throw new InvalidPositionException("Source and destination cannot be null.", null);
        }

        Piece movingPiece = getPiece(source);
        if (movingPiece == null) {
            throw new IllegalMoveException("Source square does not contain a piece.");
        }

        if (movingPiece instanceof King && move.isCastling() && !canCastle(movingPiece.isWhite(),
                destination.getColumn() > source.getColumn())) {
            throw new IllegalMoveException("Castling is not allowed without an unmoved king and rook.");
        }

        Piece capturedPiece = getCapturedPieceForMove(move, movingPiece);
        if (capturedPiece != null && capturedPiece.isWhite() == movingPiece.isWhite()) {
            throw new IllegalMoveException("Cannot capture your own piece.");
        }

        if (capturedPiece instanceof King) {
            throw new IllegalMoveException("The king cannot be captured. This position is check.");
        }

        executeMove(move, movingPiece, capturedPiece);
        notifyObservers();

        return capturedPiece;
    }

    /**
     * Gets the last executed move.
     *
     * @return last move, or {@code null}
     */
    public Move getLastMove() {
        return lastMove;
    }

    /**
     * Restores the last executed move.
     *
     * @param lastMove move to store, or {@code null}
     */
    public void setLastMove(Move lastMove) {
        this.lastMove = lastMove;
    }

    /**
     * Gets the current en passant target square.
     *
     * @return en passant target, or {@code null}
     */
    public Position getEnPassantTarget() {
        return enPassantTarget;
    }

    /**
     * Restores the current en passant target.
     *
     * @param enPassantTarget target square, or {@code null}
     */
    public void setEnPassantTarget(Position enPassantTarget) {
        this.enPassantTarget = enPassantTarget;
    }

    /**
     * Checks whether the given side still has castling rights on one side.
     *
     * @param white {@code true} for white
     * @param kingSide {@code true} for kingside, {@code false} for queenside
     * @return {@code true} if the unmoved king and rook are on their original squares
     */
    public boolean canCastle(boolean white, boolean kingSide) {
        int row = white ? 7 : 0;
        Position kingPosition = new Position(row, 4);
        Position rookPosition = new Position(row, kingSide ? 7 : 0);
        Piece king = getPiece(kingPosition);
        Piece rook = getPiece(rookPosition);

        return king instanceof King && rook instanceof Rook
                && king.isWhite() == white && rook.isWhite() == white
                && !king.hasMoved() && !rook.hasMoved();
    }

    /**
     * Gets a snapshot of the board squares.
     * <p>
     * The returned outer and inner arrays are copies so callers cannot replace
     * squares inside this board.
     *
     * @return a copied 8x8 array of square references
     */
    public Square[][] getSquares() {
        Square[][] copy = new Square[BOARD_SIZE][BOARD_SIZE];

        for (int row = 0; row < BOARD_SIZE; row++) {
            System.arraycopy(squares[row], 0, copy[row], 0, BOARD_SIZE);
        }

        return copy;
    }

    /**
     * Prints a simple console view of the current board.
     */
    public void printBoard() {
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int column = 0; column < BOARD_SIZE; column++) {
                Piece piece = squares[row][column].getPiece();
                System.out.print(piece == null ? ". " : piece.getSymbol() + " ");
            }

            System.out.println();
        }
    }

    /**
     * Converts the board into a simple 8-line text format.
     *
     * @return serialized board state
     */
    @Override
    public String serializeGame() {
        StringBuilder builder = new StringBuilder();
        builder.append("#STATE ");
        builder.append(enPassantTarget == null ? "EP=-" : "EP=" + enPassantTarget.getRow() + ","
                + enPassantTarget.getColumn());
        builder.append(System.lineSeparator());

        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int column = 0; column < BOARD_SIZE; column++) {
                Piece piece = squares[row][column].getPiece();
                builder.append(piece == null ? "--" : piece.getSymbol() + (piece.hasMoved() ? "*" : ""));

                if (column < BOARD_SIZE - 1) {
                    builder.append(' ');
                }
            }

            if (row < BOARD_SIZE - 1) {
                builder.append(System.lineSeparator());
            }
        }

        return builder.toString();
    }

    /**
     * Loads the board from the format produced by {@link #serializeGame()}.
     *
     * @param data serialized board data
     * @throws IllegalArgumentException if the data cannot be loaded
     */
    @Override
    public void deserializeGame(String data) {
        if (data == null) {
            throw new IllegalArgumentException("Serialized data cannot be null.");
        }

        String[] rows = data.strip().split("\\R");
        int boardStartIndex = 0;
        Position loadedEnPassantTarget = null;

        if (rows.length > 0 && rows[0].startsWith("#STATE")) {
            loadedEnPassantTarget = parseEnPassantTarget(rows[0]);
            boardStartIndex = 1;
        }

        if (rows.length - boardStartIndex != BOARD_SIZE) {
            throw new IllegalArgumentException("Serialized board must contain 8 rows.");
        }

        clearBoard();

        for (int row = 0; row < BOARD_SIZE; row++) {
            String[] symbols = rows[row + boardStartIndex].trim().split("\\s+");

            if (symbols.length != BOARD_SIZE) {
                throw new IllegalArgumentException("Each serialized row must contain 8 squares.");
            }

            for (int column = 0; column < BOARD_SIZE; column++) {
                Piece piece = createPieceFromSymbol(symbols[column]);

                if (piece != null) {
                    setPieceWithoutNotification(piece, new Position(row, column));
                }
            }
        }

        enPassantTarget = loadedEnPassantTarget;
        lastMove = null;
        notifyObservers();
    }

    /**
     * Registers an observer callback.
     *
     * @param observer callback to register
     */
    @Override
    public void addObserver(Runnable observer) {
        if (observer == null) {
            throw new IllegalArgumentException("Observer cannot be null.");
        }

        observers.add(observer);
    }

    /**
     * Removes an observer callback.
     *
     * @param observer callback to remove
     */
    @Override
    public void removeObserver(Runnable observer) {
        observers.remove(observer);
    }

    /**
     * Notifies all registered observers.
     */
    @Override
    public void notifyObservers() {
        List<Runnable> observerSnapshot = new ArrayList<>(observers);

        for (Runnable observer : observerSnapshot) {
            observer.run();
        }
    }

    /**
     * Places a piece without notifying observers.
     *
     * @param piece the piece to place
     * @param position the target position
     */
    private void setPieceWithoutNotification(Piece piece, Position position) {
        if (piece == null) {
            throw new IllegalArgumentException("Piece cannot be null.");
        }

        getSquare(position).setPiece(piece);
    }

    /**
     * Executes regular moves, castling, en passant, and promotion.
     *
     * @param move move to execute
     * @param movingPiece piece being moved
     * @param capturedPiece captured piece, or {@code null}
     */
    private void executeMove(Move move, Piece movingPiece, Piece capturedPiece) {
        Position source = move.getSource();
        Position destination = move.getDestination();

        move.setMovedPiece(movingPiece);
        move.setCapturedPiece(capturedPiece);
        markEnPassantCapture(move, movingPiece);

        getSquare(source).removePiece();

        if (move.isEnPassant()) {
            getSquare(move.getEnPassantCapturedPosition()).removePiece();
        }

        getSquare(destination).setPiece(createPromotionPiece(move, movingPiece));
        movingPiece.setMoved(true);

        if (movingPiece instanceof King && move.isCastling()) {
            moveCastlingRook(source, destination);
        }

        updateEnPassantTarget(move, movingPiece);
        lastMove = move;
    }

    /**
     * Gets the captured piece, including en passant captured pawns.
     *
     * @param move move to inspect
     * @param movingPiece moving piece
     * @return captured piece, or {@code null}
     */
    private Piece getCapturedPieceForMove(Move move, Piece movingPiece) {
        if (isEnPassantMove(move, movingPiece)) {
            int captureRow = movingPiece.isWhite() ? move.getDestination().getRow() + 1
                    : move.getDestination().getRow() - 1;
            return getPiece(new Position(captureRow, move.getDestination().getColumn()));
        }

        return getPiece(move.getDestination());
    }

    /**
     * Marks en passant capture metadata on the move.
     *
     * @param move move to mark
     * @param movingPiece moving piece
     */
    private void markEnPassantCapture(Move move, Piece movingPiece) {
        if (isEnPassantMove(move, movingPiece)) {
            int captureRow = movingPiece.isWhite() ? move.getDestination().getRow() + 1
                    : move.getDestination().getRow() - 1;
            move.setEnPassantCapturedPosition(new Position(captureRow, move.getDestination().getColumn()));
        }
    }

    /**
     * Checks whether the move is an en passant capture.
     *
     * @param move move to inspect
     * @param movingPiece moving piece
     * @return {@code true} if en passant
     */
    private boolean isEnPassantMove(Move move, Piece movingPiece) {
        if (!(movingPiece instanceof Pawn) || enPassantTarget == null
                || !move.getDestination().equals(enPassantTarget)
                || getPiece(move.getDestination()) != null
                || Math.abs(move.getDestination().getColumn() - move.getSource().getColumn()) != 1) {
            return false;
        }

        int captureRow = movingPiece.isWhite() ? move.getDestination().getRow() + 1
                : move.getDestination().getRow() - 1;
        if (!Position.isValid(captureRow, move.getDestination().getColumn())) {
            return false;
        }

        Piece capturedPawn = getPiece(new Position(captureRow, move.getDestination().getColumn()));
        return capturedPawn instanceof Pawn && capturedPawn.isWhite() != movingPiece.isWhite();
    }

    /**
     * Moves the rook during castling.
     *
     * @param source king source
     * @param destination king destination
     */
    private void moveCastlingRook(Position source, Position destination) {
        int row = source.getRow();
        boolean kingSide = destination.getColumn() > source.getColumn();
        Position rookSource = new Position(row, kingSide ? 7 : 0);
        Position rookDestination = new Position(row, kingSide ? 5 : 3);
        Piece rook = getPiece(rookSource);

        if (!(rook instanceof Rook)) {
            throw new IllegalMoveException("Castling rook is missing.");
        }

        getSquare(rookSource).removePiece();
        getSquare(rookDestination).setPiece(rook);
        rook.setMoved(true);
    }

    /**
     * Creates the promoted piece when a pawn reaches the last rank.
     *
     * @param move move being executed
     * @param movingPiece moving piece
     * @return original piece or promoted piece
     */
    private Piece createPromotionPiece(Move move, Piece movingPiece) {
        if (!(movingPiece instanceof Pawn) || !isPromotionRank(move.getDestination(), movingPiece.isWhite())) {
            return movingPiece;
        }

        String promotionSymbol = move.getPromotionSymbol() == null ? "Q" : move.getPromotionSymbol();
        Piece promotedPiece;

        switch (promotionSymbol) {
            case "R":
                promotedPiece = new Rook(movingPiece.isWhite());
                break;
            case "B":
                promotedPiece = new Bishop(movingPiece.isWhite());
                break;
            case "N":
                promotedPiece = new Knight(movingPiece.isWhite());
                break;
            case "Q":
            default:
                promotedPiece = new Queen(movingPiece.isWhite());
                break;
        }

        promotedPiece.setMoved(true);
        return promotedPiece;
    }

    /**
     * Checks whether a destination is a pawn promotion rank.
     *
     * @param destination destination square
     * @param whitePawn {@code true} for white pawn
     * @return {@code true} if the pawn promotes
     */
    private boolean isPromotionRank(Position destination, boolean whitePawn) {
        return whitePawn ? destination.getRow() == 0 : destination.getRow() == BOARD_SIZE - 1;
    }

    /**
     * Updates en passant target after a move.
     *
     * @param move executed move
     * @param movingPiece moved piece
     */
    private void updateEnPassantTarget(Move move, Piece movingPiece) {
        enPassantTarget = null;

        if (movingPiece instanceof Pawn
                && Math.abs(move.getDestination().getRow() - move.getSource().getRow()) == 2) {
            int targetRow = (move.getSource().getRow() + move.getDestination().getRow()) / 2;
            enPassantTarget = new Position(targetRow, move.getSource().getColumn());
        }
    }

    /**
     * Creates a piece instance from a serialized piece symbol.
     *
     * @param symbol serialized piece symbol
     * @return the matching piece, or {@code null} for an empty square
     */
    private Piece createPieceFromSymbol(String symbol) {
        boolean moved = symbol.endsWith("*");
        String pieceSymbol = moved ? symbol.substring(0, symbol.length() - 1) : symbol;
        Piece piece;

        switch (pieceSymbol) {
            case "--":
                return null;
            case "WP":
                piece = new Pawn(true);
                break;
            case "BP":
                piece = new Pawn(false);
                break;
            case "WR":
                piece = new Rook(true);
                break;
            case "BR":
                piece = new Rook(false);
                break;
            case "WN":
                piece = new Knight(true);
                break;
            case "BN":
                piece = new Knight(false);
                break;
            case "WB":
                piece = new Bishop(true);
                break;
            case "BB":
                piece = new Bishop(false);
                break;
            case "WQ":
                piece = new Queen(true);
                break;
            case "BQ":
                piece = new Queen(false);
                break;
            case "WK":
                piece = new King(true);
                break;
            case "BK":
                piece = new King(false);
                break;
            default:
                throw new IllegalArgumentException("Unknown piece symbol: " + symbol);
        }

        piece.setMoved(moved);
        return piece;
    }

    /**
     * Parses en passant metadata from a serialized state line.
     *
     * @param stateLine serialized metadata line
     * @return en passant target, or {@code null}
     */
    private Position parseEnPassantTarget(String stateLine) {
        int markerIndex = stateLine.indexOf("EP=");
        if (markerIndex < 0) {
            return null;
        }

        String value = stateLine.substring(markerIndex + 3).trim();
        if ("-".equals(value)) {
            return null;
        }

        String[] parts = value.split(",");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid en passant metadata.");
        }

        return new Position(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }
}
