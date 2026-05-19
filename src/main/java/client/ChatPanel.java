package client;

import shared.MessageType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ChatPanel - Khung chat đẹp kiểu Messenger.
 * Dùng JavaSocket (thông qua NetworkClient) để gửi/nhận tin nhắn realtime.
 * Hỗ trợ: Chat phòng đấu giá & Chat riêng User ↔ Mod.
 */
public class ChatPanel extends JPanel implements NetworkClient.MessageListener {

    // ─── Theme ─────────────────────────────────────────────────────────────────
    private static final Color BG_MAIN      = new Color(15, 23, 42);
    private static final Color BG_HEADER    = new Color(22, 32, 52);
    private static final Color BG_MSG_AREA  = new Color(13, 20, 36);
    private static final Color BG_INPUT     = new Color(22, 32, 52);
    private static final Color BUBBLE_MINE  = new Color(79, 70, 229);  // Indigo
    private static final Color BUBBLE_OTHER = new Color(30, 41, 59);   // Slate
    private static final Color BUBBLE_SYS   = new Color(20, 40, 60);
    private static final Color ACCENT       = new Color(99, 102, 241);
    private static final Color ACCENT_LIGHT = new Color(129, 140, 248);
    private static final Color TEXT_PRIMARY = new Color(248, 250, 252);
    private static final Color TEXT_MUTED   = new Color(148, 163, 184);
    private static final Color BORDER       = new Color(51, 65, 85);
    private static final Color ONLINE_DOT   = new Color(52, 211, 153);
    private static final Color INPUT_BORDER = new Color(71, 85, 105);

    // ─── State ─────────────────────────────────────────────────────────────────
    private String currentRoomId;
    private String currentReceiverUsername;
    private boolean isPrivateChat = false;
    private final String myUsername;
    private final List<ChatMessage> messages = new ArrayList<>();

    // ─── UI ────────────────────────────────────────────────────────────────────
    private JPanel headerPanel;
    private JLabel lblChatTarget;
    private JLabel lblStatus;
    private JPanel messagesPanel;
    private JScrollPane scrollPane;
    private JTextArea txtInput;
    private JButton btnSend;
    private JButton btnEmoji;

    public ChatPanel() {
        this.myUsername = NetworkClient.getInstance().getCurrentUsername();
        setLayout(new BorderLayout());
        setBackground(BG_MAIN);
        setBorder(null);

        buildHeader();
        buildMessagesArea();
        buildInputArea();

        NetworkClient.getInstance().addListener(this);
    }

    // ─── Header ────────────────────────────────────────────────────────────────
    private void buildHeader() {
        headerPanel = new JPanel(new BorderLayout(10, 0));
        headerPanel.setBackground(BG_HEADER);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, BORDER),
            new EmptyBorder(10, 14, 10, 14)
        ));

        // Avatar + name
        JPanel leftSide = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftSide.setOpaque(false);

        JLabel avatar = new JLabel("💬");
        avatar.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));

        JPanel nameStatus = new JPanel();
        nameStatus.setLayout(new BoxLayout(nameStatus, BoxLayout.Y_AXIS));
        nameStatus.setOpaque(false);

        lblChatTarget = new JLabel("Chọn cuộc trò chuyện");
        lblChatTarget.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblChatTarget.setForeground(TEXT_PRIMARY);

        lblStatus = new JLabel("● Đang chờ...");
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblStatus.setForeground(TEXT_MUTED);

        nameStatus.add(lblChatTarget);
        nameStatus.add(lblStatus);

        leftSide.add(avatar);
        leftSide.add(nameStatus);
        headerPanel.add(leftSide, BorderLayout.WEST);

        add(headerPanel, BorderLayout.NORTH);
    }

    // ─── Messages Area ─────────────────────────────────────────────────────────
    private void buildMessagesArea() {
        messagesPanel = new JPanel();
        messagesPanel.setLayout(new BoxLayout(messagesPanel, BoxLayout.Y_AXIS));
        messagesPanel.setBackground(BG_MSG_AREA);
        messagesPanel.setBorder(new EmptyBorder(12, 8, 12, 8));

        // Welcome message
        addSystemMessage("Bắt đầu cuộc trò chuyện...");

        scrollPane = new JScrollPane(messagesPanel);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(BG_MSG_AREA);
        scrollPane.setBackground(BG_MSG_AREA);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // Custom scrollbar
        scrollPane.getVerticalScrollBar().setBackground(BG_MSG_AREA);
        scrollPane.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            protected void configureScrollBarColors() {
                this.thumbColor = new Color(51, 65, 85);
                this.trackColor = BG_MSG_AREA;
            }
            protected JButton createDecreaseButton(int orientation) { return makeZeroButton(); }
            protected JButton createIncreaseButton(int orientation) { return makeZeroButton(); }
            private JButton makeZeroButton() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                return b;
            }
        });

        add(scrollPane, BorderLayout.CENTER);
    }

    // ─── Input Area ────────────────────────────────────────────────────────────
    private void buildInputArea() {
        JPanel inputArea = new JPanel(new BorderLayout(8, 0));
        inputArea.setBackground(BG_INPUT);
        inputArea.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(1, 0, 0, 0, BORDER),
            new EmptyBorder(10, 12, 10, 12)
        ));

        // Emoji button
        btnEmoji = new JButton("😊");
        btnEmoji.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        btnEmoji.setBackground(new Color(0, 0, 0, 0));
        btnEmoji.setForeground(TEXT_MUTED);
        btnEmoji.setBorderPainted(false);
        btnEmoji.setFocusPainted(false);
        btnEmoji.setContentAreaFilled(false);
        btnEmoji.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnEmoji.setPreferredSize(new Dimension(36, 36));
        btnEmoji.addActionListener(e -> showEmojiPicker());

        // Text input
        txtInput = new JTextArea(2, 1);
        txtInput.setBackground(new Color(30, 41, 59));
        txtInput.setForeground(TEXT_PRIMARY);
        txtInput.setCaretColor(ACCENT_LIGHT);
        txtInput.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtInput.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(INPUT_BORDER, 1, true),
            new EmptyBorder(8, 12, 8, 12)
        ));
        txtInput.setLineWrap(true);
        txtInput.setWrapStyleWord(true);

        // Enter to send, Shift+Enter for newline
        txtInput.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && !e.isShiftDown()) {
                    e.consume();
                    sendMessage();
                }
            }
        });

        JScrollPane inputScroll = new JScrollPane(txtInput);
        inputScroll.setBorder(null);
        inputScroll.setBackground(new Color(30, 41, 59));

        // Send button
        btnSend = new JButton("➤");
        btnSend.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnSend.setForeground(Color.WHITE);
        btnSend.setBackground(ACCENT);
        btnSend.setBorderPainted(false);
        btnSend.setFocusPainted(false);
        btnSend.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSend.setPreferredSize(new Dimension(42, 42));
        btnSend.addActionListener(e -> sendMessage());
        btnSend.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btnSend.setBackground(ACCENT_LIGHT); }
            public void mouseExited(MouseEvent e) { btnSend.setBackground(ACCENT); }
        });

        JPanel leftTools = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        leftTools.setOpaque(false);
        leftTools.add(btnEmoji);

        inputArea.add(leftTools, BorderLayout.WEST);
        inputArea.add(inputScroll, BorderLayout.CENTER);
        inputArea.add(btnSend, BorderLayout.EAST);

        add(inputArea, BorderLayout.SOUTH);
    }

    // ─── Public API ────────────────────────────────────────────────────────────
    public void setRoomMode(String roomId) {
        this.currentRoomId = roomId;
        this.isPrivateChat = false;
        messages.clear();
        SwingUtilities.invokeLater(() -> {
            lblChatTarget.setText("🏛 Phòng: " + roomId);
            lblStatus.setText("● Chat chung phòng đấu giá");
            lblStatus.setForeground(ONLINE_DOT);
            messagesPanel.removeAll();
            addSystemMessage("Đã vào phòng đấu giá " + roomId);
        });
    }

    public void setPrivateMode(String targetUsername) {
        this.currentReceiverUsername = targetUsername;
        this.isPrivateChat = true;
        messages.clear();
        SwingUtilities.invokeLater(() -> {
            lblChatTarget.setText("👤 " + targetUsername);
            lblStatus.setText("● Trực tuyến");
            lblStatus.setForeground(ONLINE_DOT);
            messagesPanel.removeAll();
            addSystemMessage("Đã bắt đầu chat riêng với " + targetUsername);
        });
    }

    // ─── Send ─────────────────────────────────────────────────────────────────
    private void sendMessage() {
        String msg = txtInput.getText().trim();
        if (msg.isEmpty()) return;
        if (msg.length() > 500) {
            JOptionPane.showMessageDialog(this, "Tin nhắn quá dài (tối đa 500 ký tự).", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Map<String, String> data = new HashMap<>();
        data.put("content", msg);

        if (isPrivateChat) {
            if (currentReceiverUsername == null) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn người dùng để chat.", "Lỗi", JOptionPane.WARNING_MESSAGE);
                return;
            }
            data.put("receiverUsername", currentReceiverUsername);
            NetworkClient.getInstance().sendMessage(MessageType.CHAT_PRIVATE, data);
        } else {
            if (currentRoomId == null) {
                JOptionPane.showMessageDialog(this, "Chưa vào phòng đấu giá.", "Lỗi", JOptionPane.WARNING_MESSAGE);
                return;
            }
            data.put("roomId", currentRoomId);
            NetworkClient.getInstance().sendMessage(MessageType.CHAT_ROOM, data);
        }

        // Hiển thị tin nhắn của mình ngay
        appendBubble(myUsername != null ? myUsername : "Tôi", msg, true);
        txtInput.setText("");
    }

    // ─── Emoji Picker ─────────────────────────────────────────────────────────
    private void showEmojiPicker() {
        String[] emojis = {"😀","😂","🥰","😎","🤔","👍","👏","🔥","💯","🎉","❤️","😢","🙏","😅","🤣","💪","✨","🏆","⭐","💬"};

        JPopupMenu popup = new JPopupMenu();
        popup.setBackground(BG_HEADER);
        popup.setBorder(BorderFactory.createLineBorder(BORDER));

        JPanel grid = new JPanel(new GridLayout(4, 5, 2, 2));
        grid.setBackground(BG_HEADER);
        grid.setBorder(new EmptyBorder(6, 6, 6, 6));

        for (String emoji : emojis) {
            JButton btn = new JButton(emoji);
            btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
            btn.setBackground(BG_HEADER);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btn.addActionListener(e -> {
                txtInput.insert(emoji, txtInput.getCaretPosition());
                popup.setVisible(false);
            });
            btn.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { btn.setBackground(BORDER); }
                public void mouseExited(MouseEvent e) { btn.setBackground(BG_HEADER); }
            });
            grid.add(btn);
        }
        popup.add(grid);
        popup.show(btnEmoji, 0, -popup.getPreferredSize().height - 6);
    }

    // ─── Bubble Rendering ─────────────────────────────────────────────────────
    private void appendBubble(String sender, String content, boolean isMe) {
        SwingUtilities.invokeLater(() -> {
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            JPanel bubble = createBubble(sender, content, time, isMe);

            JPanel wrapper = new JPanel(new FlowLayout(isMe ? FlowLayout.RIGHT : FlowLayout.LEFT, 4, 2));
            wrapper.setOpaque(false);
            wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
            wrapper.add(bubble);

            messagesPanel.add(wrapper);
            messagesPanel.add(Box.createVerticalStrut(4));
            messagesPanel.revalidate();
            scrollToBottom();
        });
    }

    private JPanel createBubble(String sender, String content, String time, boolean isMe) {
        JPanel outer = new JPanel();
        outer.setLayout(new BoxLayout(outer, BoxLayout.Y_AXIS));
        outer.setOpaque(false);
        outer.setMaximumSize(new Dimension(320, Integer.MAX_VALUE));

        // Sender name (only for others)
        if (!isMe) {
            JLabel lblSender = new JLabel(sender);
            lblSender.setFont(new Font("Segoe UI", Font.BOLD, 11));
            lblSender.setForeground(ACCENT_LIGHT);
            lblSender.setBorder(new EmptyBorder(0, 12, 2, 12));
            outer.add(lblSender);
        }

        // Bubble background
        Color bubbleColor = isMe ? BUBBLE_MINE : BUBBLE_OTHER;
        JPanel bubble = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bubbleColor);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 18, 18));
                g2.dispose();
            }
        };
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setOpaque(false);
        bubble.setBorder(new EmptyBorder(8, 12, 8, 12));

        // Content
        JLabel lblContent = new JLabel("<html><p style='width:220px;word-wrap:break-word;'>" 
            + content.replace("<", "&lt;").replace(">", "&gt;") + "</p></html>");
        lblContent.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblContent.setForeground(TEXT_PRIMARY);
        bubble.add(lblContent);

        // Time
        JLabel lblTime = new JLabel(time);
        lblTime.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblTime.setForeground(isMe ? new Color(200, 200, 230, 180) : TEXT_MUTED);
        lblTime.setAlignmentX(isMe ? Component.RIGHT_ALIGNMENT : Component.LEFT_ALIGNMENT);
        lblTime.setBorder(new EmptyBorder(2, 0, 0, 0));
        bubble.add(lblTime);

        outer.add(bubble);
        return outer;
    }

    private void addSystemMessage(String text) {
        SwingUtilities.invokeLater(() -> {
            JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
            wrapper.setOpaque(false);

            JLabel lbl = new JLabel(text);
            lbl.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            lbl.setForeground(TEXT_MUTED);
            lbl.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                new EmptyBorder(4, 12, 4, 12)
            ));
            lbl.setBackground(BUBBLE_SYS);
            lbl.setOpaque(true);

            wrapper.add(lbl);
            messagesPanel.add(wrapper);
            messagesPanel.add(Box.createVerticalStrut(8));
            messagesPanel.revalidate();
            scrollToBottom();
        });
    }

    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            JScrollBar bar = scrollPane.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }

    // ─── MessageListener ──────────────────────────────────────────────────────
    @Override
    public void onMessage(MessageType type, Map<String, String> data) {
        if (type == MessageType.CHAT_ROOM && !isPrivateChat) {
            String roomId = data.get("roomId");
            if (roomId != null && roomId.equals(currentRoomId)) {
                String sender = data.getOrDefault("senderName", "Hệ thống");
                String content = data.getOrDefault("content", "");
                // Không lặp lại tin của mình
                if (!sender.equals(myUsername)) {
                    appendBubble(sender, content, false);
                }
            }
        } else if (type == MessageType.CHAT_PRIVATE && isPrivateChat) {
            String sender = data.get("senderName");
            String content = data.getOrDefault("content", "");
            if (sender != null && sender.equals(currentReceiverUsername)) {
                appendBubble(sender, content, false);
            }
        }
    }

    // ─── Data class ───────────────────────────────────────────────────────────
    private static class ChatMessage {
        String sender, content, time;
        boolean isMe;
    }
}
