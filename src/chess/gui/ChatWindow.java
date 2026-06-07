package chess.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import chess.network.ChatService;
import chess.network.ChatThread;

/**
 * Swing chat window backed by the RMI chat service.
 */
public class ChatWindow extends JFrame {

    private static final long serialVersionUID = 1L;

    private final transient ChatThread chatThread;
    private final JTextArea messageArea;
    private final JTextField inputField;
    private final JButton sendButton;

    /**
     * Creates a chat window.
     *
     * @param title window title
     * @throws RemoteException if the chat thread cannot be exported
     */
    @SuppressWarnings("this-escape")
    public ChatWindow(String title) throws RemoteException {
        super(title == null || title.trim().isEmpty() ? "Multiplayer Chat" : title);

        messageArea = new JTextArea();
        inputField = new JTextField();
        sendButton = new JButton("Send");
        chatThread = new ChatThread(new ChatThread.ChatListener() {
            @Override
            public void onMessageReceived(ChatService.ChatMessage message) {
                SwingUtilities.invokeLater(() -> appendMessage(message));
            }

            @Override
            public void onDisconnected(String message) {
                SwingUtilities.invokeLater(() -> showDisconnected(message));
            }
        });

        configureWindow();
        composeLayout();
    }

    /**
     * Hosts a new chat service.
     *
     * @param playerName player name
     * @throws RemoteException if hosting fails
     */
    public void hostChat(String playerName) throws RemoteException {
        chatThread.hostChat(playerName);
    }

    /**
     * Joins a remote chat service.
     *
     * @param host host address
     * @param playerName player name
     * @throws RemoteException if joining fails
     */
    public void joinChat(String host, String playerName) throws RemoteException {
        chatThread.joinChat(host, playerName);
    }

    /**
     * Gets the backing chat thread.
     *
     * @return chat thread
     */
    public ChatThread getChatThread() {
        return chatThread;
    }

    private void configureWindow() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setPreferredSize(new Dimension(420, 520));

        messageArea.setEditable(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        messageArea.setBackground(new Color(250, 251, 252));
        messageArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputField.addActionListener(event -> sendCurrentMessage());

        sendButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        sendButton.addActionListener(event -> sendCurrentMessage());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                chatThread.close();
            }
        });
    }

    private void composeLayout() {
        JPanel inputPanel = new JPanel(new BorderLayout(8, 0));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        JScrollPane scrollPane = new JScrollPane(messageArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(218, 222, 226)));

        JPanel rootPanel = new JPanel(new BorderLayout(0, 0));
        rootPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        rootPanel.add(scrollPane, BorderLayout.CENTER);
        rootPanel.add(inputPanel, BorderLayout.SOUTH);

        setContentPane(rootPanel);
        pack();
        setLocationRelativeTo(null);
    }

    private void sendCurrentMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) {
            return;
        }

        try {
            chatThread.sendChatMessage(text);
            inputField.setText("");
        } catch (RemoteException exception) {
            showDisconnected("Could not send message: " + exception.getMessage());
        }
    }

    private void appendMessage(ChatService.ChatMessage message) {
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date(message.getTimestampMillis()));
        if (message.isSystemMessage()) {
            messageArea.append("[" + time + "] " + message.getText() + System.lineSeparator());
        } else {
            messageArea.append("[" + time + "] " + message.getSenderName() + ": " + message.getText()
                    + System.lineSeparator());
        }

        messageArea.setCaretPosition(messageArea.getDocument().getLength());
    }

    private void showDisconnected(String message) {
        sendButton.setEnabled(false);
        inputField.setEnabled(false);
        JOptionPane.showMessageDialog(this, message, "Chat Disconnected", JOptionPane.WARNING_MESSAGE);
    }
}
