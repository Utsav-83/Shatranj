package chess.gui;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import chess.board.ChessBoard;

/**
 * Main Swing window for the chess application.
 * <p>
 * The frame composes view components around the {@link ChessBoard} model. Move
 * execution can later be added through a controller without placing chess rules
 * inside the view.
 */
public class ChessGUI extends JFrame {

    private static final long serialVersionUID = 1L;

    private final transient ChessBoard chessBoard;
    private final transient BoardPanel boardPanel;
    private final transient SidePanel sidePanel;

    /**
     * Creates the chess GUI.
     */
    @SuppressWarnings("this-escape")
    public ChessGUI() {
        super("Java Chess");

        chessBoard = new ChessBoard();
        chessBoard.setupStartingPosition();

        sidePanel = new SidePanel();
        boardPanel = new BoardPanel(chessBoard, sidePanel);
        chessBoard.addObserver(boardPanel::refreshBoard);

        configureFrame();
        composeLayout();
    }

    /**
     * Opens the GUI on the Swing event dispatch thread.
     */
    public static void launch() {
        SwingUtilities.invokeLater(() -> {
            ChessGUI gui = new ChessGUI();
            gui.setVisible(true);
        });
    }

    /**
     * Configures base frame behavior.
     */
    private void configureFrame() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setJMenuBar(new MenuPanel(chessBoard, boardPanel, sidePanel));
        setResizable(false);
    }

    /**
     * Builds the professional two-column application layout.
     */
    private void composeLayout() {
        JPanel rootPanel = new JPanel(new BorderLayout(18, 0));
        rootPanel.setBackground(new Color(230, 233, 237));
        rootPanel.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        rootPanel.add(boardPanel, BorderLayout.CENTER);
        rootPanel.add(sidePanel, BorderLayout.EAST);

        setContentPane(rootPanel);
        pack();
        setLocationRelativeTo(null);
    }
}
