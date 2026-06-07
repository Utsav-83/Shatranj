package chess.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import chess.board.ChessBoard;
import chess.board.Position;
import chess.exceptions.ChessException;
import chess.exceptions.CheckException;
import chess.exceptions.CheckmateException;
import chess.exceptions.StalemateException;
import chess.move.CheckDetector;
import chess.move.GameStateDetector;
import chess.move.GameStateDetector.GameState;
import chess.move.GameStateDetector.GameStateType;
import chess.move.Move;
import chess.move.MoveCommand;
import chess.move.MoveHistory;
import chess.move.MoveValidator;
import chess.pieces.Pawn;
import chess.pieces.Piece;

/**
 * Swing view component responsible for rendering the chess board.
 * <p>
 * This class reads state from the {@link ChessBoard} model but does not own
 * chess rules. That separation keeps it MVC compatible.
 */
public class BoardPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int BOARD_SIZE = ChessBoard.BOARD_SIZE;
    private static final int SQUARE_SIZE = 76;
    private static final Color LIGHT_SQUARE = new Color(238, 238, 210);
    private static final Color DARK_SQUARE = new Color(118, 150, 86);
    private static final Color BORDER_COLOR = new Color(43, 48, 53);
    private static final Color LEGAL_MOVE_COLOR = new Color(105, 190, 105);
    private static final Color SELECTED_COLOR = new Color(246, 214, 92);
    private static final Logger LOGGER = Logger.getLogger(BoardPanel.class.getName());

    private final transient ChessBoard chessBoard;
    private final transient SidePanel sidePanel;
    private final transient MoveValidator moveValidator;
    private final transient CheckDetector checkDetector;
    private final transient GameStateDetector gameStateDetector;
    private final transient MoveHistory moveHistory;
    private final transient Map<String, ImageIcon> pieceIconCache;
    private final JButton[][] squareButtons;
    private final transient List<Position> legalMoves;
    private transient Position selectedPosition;
    private boolean whiteTurn;
    private boolean gameOver;

    /**
     * Creates a board panel for the supplied board model.
     *
     * @param chessBoard board model to render
     * @param sidePanel side panel to update after moves
     */
    @SuppressWarnings("this-escape")
    public BoardPanel(ChessBoard chessBoard, SidePanel sidePanel) {
        if (chessBoard == null || sidePanel == null) {
            throw new IllegalArgumentException("Board panel dependencies cannot be null.");
        }

        this.chessBoard = chessBoard;
        this.sidePanel = sidePanel;
        this.moveValidator = new MoveValidator();
        this.checkDetector = new CheckDetector();
        this.gameStateDetector = new GameStateDetector(checkDetector, moveValidator);
        this.moveHistory = new MoveHistory();
        this.pieceIconCache = new HashMap<>();
        this.squareButtons = new JButton[BOARD_SIZE][BOARD_SIZE];
        this.legalMoves = new ArrayList<>();
        this.whiteTurn = true;
        this.gameOver = false;

        setLayout(new GridLayout(BOARD_SIZE, BOARD_SIZE));
        setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 3));
        setPreferredSize(new Dimension(SQUARE_SIZE * BOARD_SIZE, SQUARE_SIZE * BOARD_SIZE));
        setBackground(BORDER_COLOR);

        createSquares();
        refreshBoard();
        updateTurnStatus();
    }

    /**
     * Creates all board square buttons.
     */
    private void createSquares() {
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int column = 0; column < BOARD_SIZE; column++) {
                JButton button = new JButton();
                button.setFocusable(false);
                button.setBorderPainted(false);
                button.setContentAreaFilled(true);
                button.setOpaque(true);
                button.setPreferredSize(new Dimension(SQUARE_SIZE, SQUARE_SIZE));
                button.setBorder(BorderFactory.createLineBorder(new Color(0, 0, 0, 0), 2));

                final int selectedRow = row;
                final int selectedColumn = column;
                button.addActionListener(event -> handleSquareClick(new Position(selectedRow, selectedColumn)));

                squareButtons[row][column] = button;
                add(button);
            }
        }
    }

    /**
     * Refreshes every square from the current board model state.
     */
    public final void refreshBoard() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::refreshBoard);
            return;
        }

        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int column = 0; column < BOARD_SIZE; column++) {
                JButton button = squareButtons[row][column];
                Piece piece = chessBoard.getPiece(new Position(row, column));

                Position position = new Position(row, column);
                button.setBackground(getDisplayColor(position));
                button.setIcon(piece == null ? null : createPieceIcon(piece));
                button.setToolTipText(createToolTip(row, column, piece));
            }
        }

        revalidate();
        repaint();
    }

    /**
     * Clears current selection, legal highlights, and restores white to move.
     */
    public void resetInteraction() {
        selectedPosition = null;
        legalMoves.clear();
        whiteTurn = true;
        gameOver = false;
        moveHistory.clear();
        updateTurnStatus();
        refreshBoard();
    }

    /**
     * Handles the click-to-move flow for one board square.
     *
     * @param clickedPosition clicked square position
     */
    private void handleSquareClick(Position clickedPosition) {
        if (gameOver) {
            return;
        }

        Piece clickedPiece = chessBoard.getPiece(clickedPosition);

        if (selectedPosition == null) {
            selectPiece(clickedPosition, clickedPiece);
            return;
        }

        if (legalMoves.contains(clickedPosition)) {
            moveSelectedPiece(clickedPosition);
            return;
        }

        if (clickedPiece != null && clickedPiece.isWhite() == whiteTurn) {
            selectPiece(clickedPosition, clickedPiece);
            return;
        }

        clearSelection();
    }

    /**
     * Selects a piece and highlights its legal moves.
     *
     * @param position piece position
     * @param piece selected piece
     */
    private void selectPiece(Position position, Piece piece) {
        legalMoves.clear();

        if (piece == null || piece.isWhite() != whiteTurn) {
            selectedPosition = null;
            refreshBoard();
            return;
        }

        selectedPosition = position;
        legalMoves.addAll(moveValidator.getLegalMovesForTurn(position, chessBoard, whiteTurn));
        refreshBoard();
    }

    /**
     * Moves the selected piece to a legal destination.
     *
     * @param destination destination position
     */
    private void moveSelectedPiece(Position destination) {
        Move move = new Move(selectedPosition, destination);

        try {
            if (isPromotionMove(move)) {
                String promotionSymbol = choosePromotionPiece();

                if (promotionSymbol == null) {
                    clearSelection();
                    return;
                }

                move.setPromotionSymbol(promotionSymbol);
            }

            moveValidator.validateMove(move, chessBoard, whiteTurn);

            MoveCommand command = new MoveCommand(chessBoard, move);
            Piece movingPiece = chessBoard.getPiece(selectedPosition);
            moveHistory.execute(command);
            Piece capturedPiece = command.getCapturedPiece();

            if (capturedPiece != null) {
                sidePanel.addCapturedPiece(capturedPiece.getSymbol(), capturedPiece.isWhite());
            }

            sidePanel.addMoveHistory(createMoveText(movingPiece, selectedPosition, destination, capturedPiece,
                    move.getPromotionSymbol()));
            whiteTurn = !whiteTurn;
            updateTurnStatus();
            clearSelection();
        } catch (ChessException exception) {
            showChessException(exception, Level.WARNING);
            clearSelection();
        }
    }

    /**
     * Undoes the previous move.
     */
    public void undoMove() {
        if (!moveHistory.canUndo()) {
            return;
        }

        chess.move.Command command = moveHistory.undoCommand();
        if (command instanceof MoveCommand) {
            MoveCommand moveCommand = (MoveCommand) command;
            Piece capturedPiece = moveCommand.getCapturedPiece();

            if (capturedPiece != null) {
                sidePanel.removeCapturedPiece(capturedPiece.getSymbol(), capturedPiece.isWhite());
            }

            sidePanel.removeLastMoveHistory();
        }

        whiteTurn = !whiteTurn;
        gameOver = false;
        clearSelection();
        updateTurnStatus();
    }

    /**
     * Redoes the most recently undone move.
     */
    public void redoMove() {
        if (!moveHistory.canRedo()) {
            return;
        }

        chess.move.Command command = moveHistory.redoCommand();
        if (command instanceof MoveCommand) {
            MoveCommand moveCommand = (MoveCommand) command;
            Move redoneMove = moveCommand.getMove();
            Piece capturedPiece = moveCommand.getCapturedPiece();
            Piece movedPiece = moveCommand.getMovedPiece();

            if (capturedPiece != null) {
                sidePanel.addCapturedPiece(capturedPiece.getSymbol(), capturedPiece.isWhite());
            }

            sidePanel.addMoveHistory(createMoveText(movedPiece, redoneMove.getSource(), redoneMove.getDestination(),
                    capturedPiece, redoneMove.getPromotionSymbol()));
        }

        whiteTurn = !whiteTurn;
        gameOver = false;
        clearSelection();
        updateTurnStatus();
    }

    /**
     * Logs a chess exception and displays it in a GUI dialog.
     *
     * @param exception chess exception to report
     * @param level log severity
     */
    private void showChessException(ChessException exception, Level level) {
        exception.logAndShow(LOGGER, level, this);
    }

    /**
     * Updates the side panel with current turn and check status.
     */
    private void updateTurnStatus() {
        try {
            GameState gameState = gameStateDetector.analyze(chessBoard, whiteTurn);

            if (gameState.getType() == GameStateType.CHECKMATE) {
                handleCheckmate(gameState);
                return;
            }

            if (gameState.getType() == GameStateType.STALEMATE) {
                handleStalemate(gameState);
                return;
            }

            boolean inCheck = gameState.getType() == GameStateType.CHECK;
            List<Position> checkingPieces = checkDetector.findCheckingPieces(chessBoard, whiteTurn);
            sidePanel.setTurnStatus(whiteTurn, inCheck);

            if (inCheck) {
                CheckException exception = new CheckException(gameState.getPlayerName() + " king is in check from "
                        + formatPositions(checkingPieces) + ".");
                exception.log(LOGGER, Level.INFO);
            }
        } catch (ChessException exception) {
            showChessException(exception, Level.WARNING);
        }
    }

    /**
     * Handles a checkmate result.
     *
     * @param gameState analyzed game state
     */
    private void handleCheckmate(GameState gameState) {
        gameOver = true;
        String winner = gameState.isWhiteTurn() ? "Black" : "White";
        sidePanel.setGameResult("CHECKMATE - " + winner + " wins");

        CheckmateException exception = new CheckmateException("Checkmate. " + winner + " wins.");
        showChessException(exception, Level.INFO);
    }

    /**
     * Handles a stalemate result.
     *
     * @param gameState analyzed game state
     */
    private void handleStalemate(GameState gameState) {
        gameOver = true;
        sidePanel.setGameResult("STALEMATE - Draw");

        StalemateException exception = new StalemateException("Stalemate. " + gameState.getPlayerName()
                + " has no legal moves, so the game is a draw.");
        showChessException(exception, Level.INFO);
    }

    /**
     * Checks whether a move promotes a pawn.
     *
     * @param move move to inspect
     * @return {@code true} if the selected pawn reaches the final rank
     */
    private boolean isPromotionMove(Move move) {
        Piece movingPiece = chessBoard.getPiece(move.getSource());
        if (!(movingPiece instanceof Pawn)) {
            return false;
        }

        return movingPiece.isWhite() ? move.getDestination().getRow() == 0
                : move.getDestination().getRow() == BOARD_SIZE - 1;
    }

    /**
     * Shows the promotion choice dialog.
     *
     * @return promotion code, or {@code null} if cancelled
     */
    private String choosePromotionPiece() {
        String[] options = {"Queen", "Rook", "Bishop", "Knight"};
        int choice = JOptionPane.showOptionDialog(this,
                "Choose promotion piece:",
                "Pawn Promotion",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        switch (choice) {
            case 1:
                return "R";
            case 2:
                return "B";
            case 3:
                return "N";
            case 0:
                return "Q";
            default:
                return null;
        }
    }

    /**
     * Formats attacker positions for log messages.
     *
     * @param positions positions to format
     * @return comma-separated algebraic square names
     */
    private String formatPositions(List<Position> positions) {
        StringBuilder builder = new StringBuilder();

        for (int index = 0; index < positions.size(); index++) {
            if (index > 0) {
                builder.append(", ");
            }

            builder.append(toSquareName(positions.get(index)));
        }

        return builder.toString();
    }

    /**
     * Clears current selected square and legal move highlights.
     */
    private void clearSelection() {
        selectedPosition = null;
        legalMoves.clear();
        refreshBoard();
    }

    /**
     * Gets the display color for a square, including selection highlights.
     *
     * @param position square position
     * @return display color
     */
    private Color getDisplayColor(Position position) {
        if (position.equals(selectedPosition)) {
            return SELECTED_COLOR;
        }

        if (legalMoves.contains(position)) {
            return LEGAL_MOVE_COLOR;
        }

        return getSquareColor(position.getRow(), position.getColumn());
    }

    /**
     * Gets the standard alternating square color.
     *
     * @param row square row
     * @param column square column
     * @return square color
     */
    private Color getSquareColor(int row, int column) {
        return (row + column) % 2 == 0 ? LIGHT_SQUARE : DARK_SQUARE;
    }

    /**
     * Creates a simple move-history entry.
     *
     * @param piece moved piece
     * @param source source position
     * @param destination destination position
     * @param capturedPiece captured piece, or {@code null}
     * @return move text
     */
    private String createMoveText(Piece piece, Position source, Position destination, Piece capturedPiece,
            String promotionSymbol) {
        String captureMarker = capturedPiece == null ? "-" : "x";
        String promotionText = promotionSymbol == null ? "" : "=" + promotionSymbol;
        return piece.getSymbol() + " " + toSquareName(source) + captureMarker + toSquareName(destination)
                + promotionText;
    }

    /**
     * Converts a board position to algebraic square text.
     *
     * @param position board position
     * @return square name such as {@code e4}
     */
    private String toSquareName(Position position) {
        char file = (char) ('a' + position.getColumn());
        int rank = BOARD_SIZE - position.getRow();
        return String.valueOf(file) + rank;
    }

    /**
     * Creates an accessible tooltip for a board square.
     *
     * @param row square row
     * @param column square column
     * @param piece piece on the square, or {@code null}
     * @return tooltip text
     */
    private String createToolTip(int row, int column, Piece piece) {
        char file = (char) ('a' + column);
        int rank = BOARD_SIZE - row;
        String squareName = String.valueOf(file) + rank;

        if (piece == null) {
            return squareName + " empty";
        }

        return squareName + " " + getPieceName(piece);
    }

    /**
     * Creates a generated image icon for a piece.
     *
     * @param piece piece to draw
     * @return rendered piece image
     */
    private ImageIcon createPieceIcon(Piece piece) {
        ImageIcon cachedIcon = pieceIconCache.get(piece.getSymbol());
        if (cachedIcon != null) {
            return cachedIcon;
        }

        int imageSize = 64;
        BufferedImage image = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();

        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setColor(new Color(0, 0, 0, 42));
        graphics.fillOval(12, 49, 40, 8);

        String glyph = getPieceGlyph(piece);
        Font font = new Font("Segoe UI Symbol", Font.PLAIN, 52);
        graphics.setFont(font);
        FontMetrics metrics = graphics.getFontMetrics();
        int x = (imageSize - metrics.stringWidth(glyph)) / 2;
        int y = ((imageSize - metrics.getHeight()) / 2) + metrics.getAscent() - 2;

        graphics.setStroke(new BasicStroke(2f));
        graphics.setColor(piece.isWhite() ? new Color(40, 44, 52) : new Color(245, 245, 245));
        graphics.drawString(glyph, x + 1, y + 1);
        graphics.setColor(piece.isWhite() ? Color.WHITE : new Color(32, 36, 40));
        graphics.drawString(glyph, x, y);
        graphics.dispose();

        ImageIcon icon = new ImageIcon(image);
        pieceIconCache.put(piece.getSymbol(), icon);
        return icon;
    }

    /**
     * Converts a piece to its Unicode chess glyph.
     *
     * @param piece piece to convert
     * @return chess glyph
     */
    private String getPieceGlyph(Piece piece) {
        switch (piece.getSymbol()) {
            case "WP":
                return "\u2659";
            case "BP":
                return "\u265F";
            case "WR":
                return "\u2656";
            case "BR":
                return "\u265C";
            case "WN":
                return "\u2658";
            case "BN":
                return "\u265E";
            case "WB":
                return "\u2657";
            case "BB":
                return "\u265D";
            case "WQ":
                return "\u2655";
            case "BQ":
                return "\u265B";
            case "WK":
                return "\u2654";
            case "BK":
                return "\u265A";
            default:
                return "?";
        }
    }

    /**
     * Converts a piece to a readable name.
     *
     * @param piece piece to name
     * @return readable piece name
     */
    private String getPieceName(Piece piece) {
        String color = piece.isWhite() ? "White" : "Black";
        String symbol = piece.getSymbol().substring(1);

        switch (symbol) {
            case "P":
                return color + " Pawn";
            case "R":
                return color + " Rook";
            case "N":
                return color + " Knight";
            case "B":
                return color + " Bishop";
            case "Q":
                return color + " Queen";
            case "K":
                return color + " King";
            default:
                return color + " Piece";
        }
    }
}
