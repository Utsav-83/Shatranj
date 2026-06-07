package chess.gui;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import chess.board.ChessBoard;

/**
 * Application menu bar for board actions.
 * <p>
 * This class acts as a simple view/controller boundary for menu commands while
 * delegating board state changes to the model.
 */
public class MenuPanel extends JMenuBar {

    private static final long serialVersionUID = 1L;

    private final transient ChessBoard chessBoard;
    private final transient BoardPanel boardPanel;
    private final transient SidePanel sidePanel;

    /**
     * Creates the menu bar.
     *
     * @param chessBoard board model
     * @param boardPanel board view
     * @param sidePanel side panel view
     */
    @SuppressWarnings("this-escape")
    public MenuPanel(ChessBoard chessBoard, BoardPanel boardPanel, SidePanel sidePanel) {
        if (chessBoard == null || boardPanel == null || sidePanel == null) {
            throw new IllegalArgumentException("Menu dependencies cannot be null.");
        }

        this.chessBoard = chessBoard;
        this.boardPanel = boardPanel;
        this.sidePanel = sidePanel;

        add(createGameMenu());
        add(createEditMenu());
        add(createViewMenu());
        add(createHelpMenu());
    }

    /**
     * Creates the edit menu.
     *
     * @return edit menu
     */
    private JMenu createEditMenu() {
        JMenu menu = new JMenu("Edit");

        JMenuItem undoItem = new JMenuItem("Undo Move");
        undoItem.addActionListener(event -> boardPanel.undoMove());

        JMenuItem redoItem = new JMenuItem("Redo Move");
        redoItem.addActionListener(event -> boardPanel.redoMove());

        menu.add(undoItem);
        menu.add(redoItem);
        return menu;
    }

    /**
     * Creates the game menu.
     *
     * @return game menu
     */
    private JMenu createGameMenu() {
        JMenu menu = new JMenu("Game");

        JMenuItem newGameItem = new JMenuItem("New Game");
        newGameItem.addActionListener(event -> startNewGame());

        JMenuItem saveItem = new JMenuItem("Save Game");
        saveItem.addActionListener(event -> saveGame());

        JMenuItem loadItem = new JMenuItem("Load Game");
        loadItem.addActionListener(event -> loadGame());

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(event -> SwingUtilities.getWindowAncestor(this).dispose());

        menu.add(newGameItem);
        menu.addSeparator();
        menu.add(saveItem);
        menu.add(loadItem);
        menu.addSeparator();
        menu.add(exitItem);

        return menu;
    }

    /**
     * Creates the view menu.
     *
     * @return view menu
     */
    private JMenu createViewMenu() {
        JMenu menu = new JMenu("View");

        JMenuItem refreshItem = new JMenuItem("Refresh Board");
        refreshItem.addActionListener(event -> boardPanel.refreshBoard());
        menu.add(refreshItem);

        return menu;
    }

    /**
     * Creates the help menu.
     *
     * @return help menu
     */
    private JMenu createHelpMenu() {
        JMenu menu = new JMenu("Help");

        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(event -> JOptionPane.showMessageDialog(getParentComponent(),
                "Java Chess\nMVC Swing interface ready for move logic.",
                "About Java Chess",
                JOptionPane.INFORMATION_MESSAGE));
        menu.add(aboutItem);

        return menu;
    }

    /**
     * Starts a new game from the standard starting position.
     */
    private void startNewGame() {
        chessBoard.setupStartingPosition();
        sidePanel.reset();
        boardPanel.resetInteraction();
        boardPanel.refreshBoard();
    }

    /**
     * Saves the board to a text file.
     */
    private void saveGame() {
        JFileChooser chooser = createFileChooser();

        if (chooser.showSaveDialog(getParentComponent()) == JFileChooser.APPROVE_OPTION) {
            File file = ensureChessExtension(chooser.getSelectedFile());

            try {
                Files.writeString(file.toPath(), chessBoard.serializeGame(), StandardCharsets.UTF_8);
            } catch (IOException exception) {
                showError("Unable to save game: " + exception.getMessage());
            }
        }
    }

    /**
     * Loads the board from a text file.
     */
    private void loadGame() {
        JFileChooser chooser = createFileChooser();

        if (chooser.showOpenDialog(getParentComponent()) == JFileChooser.APPROVE_OPTION) {
            try {
                String data = Files.readString(chooser.getSelectedFile().toPath(), StandardCharsets.UTF_8);
                chessBoard.deserializeGame(data);
                sidePanel.reset();
                boardPanel.resetInteraction();
                boardPanel.refreshBoard();
            } catch (IOException | IllegalArgumentException exception) {
                showError("Unable to load game: " + exception.getMessage());
            }
        }
    }

    /**
     * Creates a file chooser for game files.
     *
     * @return configured file chooser
     */
    private JFileChooser createFileChooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Chess save files (*.chess)", "chess"));
        return chooser;
    }

    /**
     * Adds the {@code .chess} extension when needed.
     *
     * @param file selected file
     * @return file with extension
     */
    private File ensureChessExtension(File file) {
        if (file.getName().toLowerCase().endsWith(".chess")) {
            return file;
        }

        return new File(file.getParentFile(), file.getName() + ".chess");
    }

    /**
     * Shows an error dialog.
     *
     * @param message message to show
     */
    private void showError(String message) {
        JOptionPane.showMessageDialog(getParentComponent(), message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Finds the owning window/component for dialogs.
     *
     * @return parent component
     */
    private Component getParentComponent() {
        return SwingUtilities.getWindowAncestor(this);
    }
}
