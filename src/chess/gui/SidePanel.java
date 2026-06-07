package chess.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

/**
 * Swing side panel that displays game status, captured pieces, and move history.
 * <p>
 * The panel is a view component and can be updated by a controller later when
 * move execution logic is introduced.
 */
public class SidePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JLabel currentTurnLabel;
    private final JPanel whiteCapturedPanel;
    private final JPanel blackCapturedPanel;
    private final DefaultListModel<String> moveHistoryModel;

    /**
     * Creates the side panel.
     */
    @SuppressWarnings("this-escape")
    public SidePanel() {
        setLayout(new BorderLayout(0, 14));
        setPreferredSize(new Dimension(280, 640));
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        setBackground(new Color(247, 248, 250));

        currentTurnLabel = createTurnLabel();
        whiteCapturedPanel = createCapturedPanel();
        blackCapturedPanel = createCapturedPanel();
        moveHistoryModel = new DefaultListModel<>();

        add(currentTurnLabel, BorderLayout.NORTH);
        add(createCenterPanel(), BorderLayout.CENTER);
    }

    /**
     * Updates the current turn text.
     *
     * @param whiteTurn {@code true} when it is white's turn
     */
    public void setCurrentTurn(boolean whiteTurn) {
        currentTurnLabel.setText(whiteTurn ? "White to move" : "Black to move");
        currentTurnLabel.setBackground(new Color(31, 41, 55));
    }

    /**
     * Displays current turn and check status.
     *
     * @param whiteTurn {@code true} when it is white's turn
     * @param inCheck {@code true} when the current player's king is attacked
     */
    public void setTurnStatus(boolean whiteTurn, boolean inCheck) {
        if (inCheck) {
            currentTurnLabel.setText("CHECK");
            currentTurnLabel.setBackground(new Color(185, 28, 28));
            return;
        }

        setCurrentTurn(whiteTurn);
    }

    /**
     * Displays a final game result.
     *
     * @param resultText result text to display
     */
    public void setGameResult(String resultText) {
        currentTurnLabel.setText(resultText);
        currentTurnLabel.setBackground(new Color(88, 28, 135));
    }

    /**
     * Adds a captured piece symbol to the correct captured-piece section.
     *
     * @param pieceSymbol piece symbol such as {@code WP} or {@code BQ}
     * @param whitePiece {@code true} if the captured piece is white
     */
    public void addCapturedPiece(String pieceSymbol, boolean whitePiece) {
        JLabel label = new JLabel(pieceSymbol, SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        label.setOpaque(true);
        label.setBackground(Color.WHITE);
        label.setBorder(BorderFactory.createLineBorder(new Color(218, 222, 226)));

        if (whitePiece) {
            whiteCapturedPanel.add(label);
        } else {
            blackCapturedPanel.add(label);
        }

        revalidate();
        repaint();
    }

    /**
     * Removes one captured piece label from the matching captured-piece section.
     *
     * @param pieceSymbol piece symbol to remove
     * @param whitePiece {@code true} if the captured piece is white
     */
    public void removeCapturedPiece(String pieceSymbol, boolean whitePiece) {
        JPanel panel = whitePiece ? whiteCapturedPanel : blackCapturedPanel;

        for (int index = panel.getComponentCount() - 1; index >= 0; index--) {
            if (panel.getComponent(index) instanceof JLabel) {
                JLabel label = (JLabel) panel.getComponent(index);

                if (pieceSymbol.equals(label.getText())) {
                    panel.remove(index);
                    revalidate();
                    repaint();
                    return;
                }
            }
        }
    }

    /**
     * Adds one move notation row to the move history.
     *
     * @param moveText move notation to display
     */
    public void addMoveHistory(String moveText) {
        moveHistoryModel.addElement(moveText);
    }

    /**
     * Removes the most recent move-history row.
     */
    public void removeLastMoveHistory() {
        if (!moveHistoryModel.isEmpty()) {
            moveHistoryModel.remove(moveHistoryModel.size() - 1);
        }
    }

    /**
     * Clears captured pieces and move history.
     */
    public void reset() {
        whiteCapturedPanel.removeAll();
        blackCapturedPanel.removeAll();
        moveHistoryModel.clear();
        setCurrentTurn(true);
        revalidate();
        repaint();
    }

    /**
     * Creates the current-turn display.
     *
     * @return turn label
     */
    private JLabel createTurnLabel() {
        JLabel label = new JLabel("White to move", SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI", Font.BOLD, 22));
        label.setOpaque(true);
        label.setBackground(new Color(31, 41, 55));
        label.setForeground(Color.WHITE);
        label.setBorder(BorderFactory.createEmptyBorder(14, 12, 14, 12));
        return label;
    }

    /**
     * Creates the center content area.
     *
     * @return center panel
     */
    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 1, 0, 14));
        panel.setOpaque(false);
        panel.add(createSection("Captured White", whiteCapturedPanel));
        panel.add(createSection("Captured Black", blackCapturedPanel));
        panel.add(createMoveHistorySection());
        return panel;
    }

    /**
     * Creates a captured-piece grid.
     *
     * @return captured-piece panel
     */
    private JPanel createCapturedPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 8, 4, 4));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return panel;
    }

    /**
     * Wraps a section with a title.
     *
     * @param title section title
     * @param content section content
     * @return titled section panel
     */
    private JPanel createSection(String title, JPanel content) {
        JPanel wrapper = new JPanel(new BorderLayout(0, 8));
        wrapper.setOpaque(false);
        wrapper.add(createSectionTitle(title), BorderLayout.NORTH);
        wrapper.add(content, BorderLayout.CENTER);
        return wrapper;
    }

    /**
     * Creates the move history section.
     *
     * @return move history panel
     */
    private JPanel createMoveHistorySection() {
        JList<String> moveHistoryList = new JList<>(moveHistoryModel);
        moveHistoryList.setFont(new Font("Consolas", Font.PLAIN, 13));
        moveHistoryList.setBackground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(moveHistoryList);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(218, 222, 226)));

        JPanel wrapper = new JPanel(new BorderLayout(0, 8));
        wrapper.setOpaque(false);
        wrapper.add(createSectionTitle("Move History"), BorderLayout.NORTH);
        wrapper.add(scrollPane, BorderLayout.CENTER);
        return wrapper;
    }

    /**
     * Creates a section heading.
     *
     * @param title heading text
     * @return heading label
     */
    private JLabel createSectionTitle(String title) {
        JLabel label = new JLabel(title);
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        label.setForeground(new Color(55, 65, 81));
        return label;
    }
}
