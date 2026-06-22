package client;

import shared.MessageType;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.util.Base64;

/**
 * ChatPanel — Chat realtime theo phong cách sáng, đồng nhất với Dashboard.
 * Hỗ trợ chat phòng đấu giá và chat riêng User ↔ Mod.
 */
public class ChatPanel extends JPanel implements NetworkClient.MessageListener {

    // ── Palette (khớp Dashboard) ──────────────────────────────────────────────
    private static final Color BG_MAIN      = UITheme.BG_DARK;
    private static final Color BG_HEADER    = UITheme.BG_DEEP;
    private static final Color BG_MSG_AREA  = UITheme.BG_DARK;
    private static final Color BG_INPUT     = UITheme.BG_CARD;
    private static final Color BORDER_CLR   = UITheme.BORDER;
    private static final Color BUBBLE_MINE  = UITheme.ACCENT;
    private static final Color BUBBLE_OTHER = UITheme.BG_CARD;
    private static final Color BUBBLE_SYS_BG = UITheme.BG_ELEVATED;
    private static final Color TEXT_DARK    = UITheme.TEXT_PRIMARY;
    private static final Color TEXT_MED     = UITheme.TEXT_MUTED;
    private static final Color TEXT_SOFT    = UITheme.TEXT_HINT;
    private static final Color ONLINE_DOT   = UITheme.SUCCESS;
    private static final Color SEND_BTN     = UITheme.ACCENT;
    private static final Color SEND_HOVER   = UITheme.ACCENT_LIGHT;

    // ── State ─────────────────────────────────────────────────────────────────
    private String currentRoomId;
    private String currentReceiverUsername;
    private boolean isPrivateChat = false;
    private final String myUsername;
    private final List<ChatMessage> messages = new ArrayList<>();
    
    // Buffer for offline/pre-select messages: senderName -> list of pending msgs
    private final Map<String, java.util.LinkedList<String[]>> pendingMessages = new HashMap<>();

    // ── UI ────────────────────────────────────────────────────────────────────
    private JLabel lblChatTarget;
    private JLabel lblStatus;
    private JPanel messagesPanel;
    private JScrollPane scrollPane;
    private JTextArea txtInput;
    private JButton btnSend;
    private JButton btnEmoji;
    private JButton btnImage;

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

    // ─── Header ───────────────────────────────────────────────────────────────
    private void buildHeader() {
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setBackground(BG_HEADER);
        header.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, BORDER_CLR),
            new EmptyBorder(10, 16, 10, 16)
        ));

        // Avatar circle + name/status
        JPanel leftSide = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftSide.setOpaque(false);

        // Chat icon circle
        JPanel iconCircle = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 247, 237));
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(254, 215, 170));
                g2.setStroke(new BasicStroke(1f));
                g2.drawOval(0, 0, getWidth()-1, getHeight()-1);
                g2.setColor(SEND_BTN);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                FontMetrics fm = g2.getFontMetrics();
                String ic = "C";
                g2.drawString(ic, (getWidth()-fm.stringWidth(ic))/2, (getHeight()+fm.getAscent()-fm.getDescent())/2);
                g2.dispose();
            }
        };
        iconCircle.setOpaque(false);
        iconCircle.setPreferredSize(new Dimension(38, 38));
        leftSide.add(iconCircle);

        JPanel nameStatus = new JPanel();
        nameStatus.setLayout(new BoxLayout(nameStatus, BoxLayout.Y_AXIS));
        nameStatus.setOpaque(false);

        lblChatTarget = new JLabel("Chọn cuộc trò chuyện");
        lblChatTarget.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblChatTarget.setForeground(TEXT_DARK);
        lblChatTarget.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblChatTarget.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (isPrivateChat && currentReceiverUsername != null) {
                    UserDashboard ud = (UserDashboard) SwingUtilities.getAncestorOfClass(UserDashboard.class, ChatPanel.this);
                    if (ud != null) {
                        ud.openRateUserDialogByUsername(currentReceiverUsername);
                    }
                }
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                if (isPrivateChat) lblChatTarget.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                lblChatTarget.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        });

        lblStatus = new JLabel("Đang chờ...");
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblStatus.setForeground(TEXT_SOFT);
        lblStatus.setAlignmentX(Component.LEFT_ALIGNMENT);

        nameStatus.add(lblChatTarget);
        nameStatus.add(Box.createVerticalStrut(1));
        nameStatus.add(lblStatus);

        leftSide.add(nameStatus);
        header.add(leftSide, BorderLayout.WEST);

        // Online indicator badge (right side)
        JLabel onlineBadge = new JLabel("● Trực tuyến");
        onlineBadge.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        onlineBadge.setForeground(ONLINE_DOT);
        header.add(onlineBadge, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);
    }

    // ─── Messages Area ────────────────────────────────────────────────────────
    private void buildMessagesArea() {
        messagesPanel = new JPanel();
        messagesPanel.setLayout(new BoxLayout(messagesPanel, BoxLayout.Y_AXIS));
        messagesPanel.setBackground(BG_MSG_AREA);
        messagesPanel.setBorder(new EmptyBorder(16, 12, 16, 12));

        addSystemMessage("Bắt đầu cuộc trò chuyện...");

        scrollPane = new JScrollPane(messagesPanel);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(BG_MSG_AREA);
        scrollPane.setBackground(BG_MSG_AREA);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // Clean light scrollbar
        scrollPane.getVerticalScrollBar().setBackground(BG_MSG_AREA);
        scrollPane.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            protected void configureScrollBarColors() {
                this.thumbColor = new Color(210, 214, 220);
                this.trackColor = BG_MSG_AREA;
            }
            protected JButton createDecreaseButton(int o) { return zeroBtn(); }
            protected JButton createIncreaseButton(int o) { return zeroBtn(); }
            private JButton zeroBtn() { JButton b = new JButton(); b.setPreferredSize(new Dimension(0,0)); return b; }
        });

        add(scrollPane, BorderLayout.CENTER);
    }

    // ─── Input Area ───────────────────────────────────────────────────────────
    private void buildInputArea() {
        JPanel inputArea = new JPanel(new BorderLayout(8, 0));
        inputArea.setBackground(BG_INPUT);
        inputArea.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(1, 0, 0, 0, BORDER_CLR),
            new EmptyBorder(10, 12, 10, 12)
        ));

        // Emoji button
        btnEmoji = new JButton("😊");
        btnEmoji.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        btnEmoji.setBackground(BG_INPUT);
        btnEmoji.setForeground(TEXT_SOFT);
        btnEmoji.setBorderPainted(false);
        btnEmoji.setFocusPainted(false);
        btnEmoji.setContentAreaFilled(false);
        btnEmoji.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnEmoji.setPreferredSize(new Dimension(36, 36));
        btnEmoji.addActionListener(e -> showEmojiPicker());
        btnEmoji.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btnEmoji.setOpaque(true); btnEmoji.setBackground(UITheme.BG_ROW_ALT); }
            public void mouseExited(MouseEvent e)  { btnEmoji.setOpaque(false); }
        });

        // Text input styled like a modern pill
        txtInput = new JTextArea(2, 1);
        txtInput.setBackground(BG_INPUT);
        txtInput.setForeground(TEXT_DARK);
        txtInput.setCaretColor(SEND_BTN);
        txtInput.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        txtInput.setLineWrap(true);
        txtInput.setWrapStyleWord(true);
        txtInput.setBorder(new EmptyBorder(8, 16, 8, 16));

        JScrollPane inputScroll = new JScrollPane(txtInput) {
            @Override
            protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BORDER_CLR);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
                g2.dispose();
            }
        };
        inputScroll.setBorder(new EmptyBorder(1, 1, 1, 1));
        inputScroll.setOpaque(false);
        inputScroll.getViewport().setOpaque(false);

        // Enter = send, Shift+Enter = new line
        txtInput.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && !e.isShiftDown()) {
                    e.consume();
                    sendMessage();
                }
            }
        });



        // Send button — filled orange circle
        btnSend = new JButton(">") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? SEND_HOVER : SEND_BTN);
                g2.fillOval(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
                g2.dispose();
            }
        };
        btnSend.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnSend.setForeground(Color.WHITE);
        btnSend.setContentAreaFilled(false);
        btnSend.setBorderPainted(false);
        btnSend.setFocusPainted(false);
        btnSend.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSend.setPreferredSize(new Dimension(40, 40));
        btnSend.addActionListener(e -> sendMessage());

        btnImage = new JButton("🖼");
        btnImage.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        btnImage.setBackground(BG_INPUT);
        btnImage.setForeground(TEXT_SOFT);
        btnImage.setBorderPainted(false);
        btnImage.setFocusPainted(false);
        btnImage.setContentAreaFilled(false);
        btnImage.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnImage.setPreferredSize(new Dimension(36, 36));
        btnImage.addActionListener(e -> showImagePicker());
        btnImage.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btnImage.setOpaque(true); btnImage.setBackground(UITheme.BG_ROW_ALT); }
            public void mouseExited(MouseEvent e)  { btnImage.setOpaque(false); }
        });

        JPanel leftTools = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        leftTools.setOpaque(false);
        leftTools.add(btnEmoji);
        leftTools.add(btnImage);

        inputArea.add(leftTools, BorderLayout.WEST);
        inputArea.add(inputScroll, BorderLayout.CENTER);
        inputArea.add(btnSend, BorderLayout.EAST);

        add(inputArea, BorderLayout.SOUTH);
    }

    // ─── Public API ───────────────────────────────────────────────────────────
    public void setRoomMode(String roomId) {
        this.currentRoomId = roomId;
        this.isPrivateChat = false;
        messages.clear();
        SwingUtilities.invokeLater(() -> {
            if (btnImage != null) btnImage.setVisible(false);
            lblChatTarget.setIcon(org.kordamp.ikonli.swing.FontIcon.of(org.kordamp.ikonli.feather.Feather.HOME, 18, UITheme.AMBER));
            lblChatTarget.setText("Phòng: " + roomId);
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
            if (btnImage != null) btnImage.setVisible(true);
            lblChatTarget.setIcon(org.kordamp.ikonli.swing.FontIcon.of(org.kordamp.ikonli.feather.Feather.MESSAGE_SQUARE, 18, UITheme.INFO));
            lblChatTarget.setText(targetUsername);
            lblStatus.setText("● Trực tuyến");
            lblStatus.setForeground(ONLINE_DOT);
            messagesPanel.removeAll();
            addSystemMessage("Đã bắt đầu chat riêng với " + targetUsername);
            // Flush buffered offline messages for this user
            java.util.List<String[]> buffered = pendingMessages.remove(targetUsername);
            if (buffered != null) {
                for (String[] m : buffered) {
                    appendBubble(m[0], m[1], m.length > 2 ? m[2] : null, false);
                }
            }
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

        appendBubble(myUsername != null ? myUsername : "Tôi", msg, null, true);
        txtInput.setText("");
    }

    private void showImagePicker() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Image Files", "jpg", "png", "jpeg"));
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                BufferedImage img = ImageIO.read(selectedFile);
                if (img == null) return;
                
                // Giới hạn kích thước tối đa 300px để tránh packet quá lớn
                int w = img.getWidth();
                int h = img.getHeight();
                int maxDim = 300;
                if (w > maxDim || h > maxDim) {
                    float ratio = Math.min((float) maxDim / w, (float) maxDim / h);
                    int newW = Math.round(w * ratio);
                    int newH = Math.round(h * ratio);
                    Image tmp = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
                    BufferedImage resized = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2d = resized.createGraphics();
                    g2d.drawImage(tmp, 0, 0, null);
                    g2d.dispose();
                    img = resized;
                }
                
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(img, "png", baos);
                String base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());
                
                sendImage(base64Image);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Không thể tải ảnh", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void sendImage(String base64Image) {
        if (!isPrivateChat || currentReceiverUsername == null) return;
        Map<String, String> data = new HashMap<>();
        data.put("content", "[Hình ảnh]");
        data.put("image", base64Image);
        data.put("receiverUsername", currentReceiverUsername);
        NetworkClient.getInstance().sendMessage(MessageType.CHAT_PRIVATE, data);
        appendBubble(myUsername != null ? myUsername : "Tôi", "[Hình ảnh]", base64Image, true);
    }

    // ─── Emoji Picker ─────────────────────────────────────────────────────────
    private void showEmojiPicker() {
        String[] emojis = {"😀","😂","🥰","😎","🤔","👍","👏","🔥","💯","🎉","❤️","😢","🙏","😅","🤣","💪","✨","🏆","⭐","💬"};

        JPopupMenu popup = new JPopupMenu();
        popup.setBackground(BG_INPUT);
        popup.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_CLR, 1, true),
            new EmptyBorder(4, 4, 4, 4)
        ));

        JPanel grid = new JPanel(new GridLayout(4, 5, 4, 4));
        grid.setBackground(BG_INPUT);
        grid.setBorder(new EmptyBorder(4, 4, 4, 4));

        for (String emoji : emojis) {
            JButton btn = new JButton(emoji);
            btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
            btn.setBackground(BG_INPUT);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btn.setPreferredSize(new Dimension(40, 40));
            btn.addActionListener(e -> {
                txtInput.insert(emoji, txtInput.getCaretPosition());
                popup.setVisible(false);
            });
            btn.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { btn.setBackground(UITheme.BG_ROW_ALT); btn.setOpaque(true); }
                public void mouseExited(MouseEvent e)  { btn.setBackground(BG_INPUT); }
            });
            grid.add(btn);
        }
        popup.add(grid);
        popup.show(btnEmoji, 0, -popup.getPreferredSize().height - 6);
    }

    // ─── Bubble Rendering ─────────────────────────────────────────────────────
    private void appendBubble(String sender, String content, String imageBase64, boolean isMe) {
        SwingUtilities.invokeLater(() -> {
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            JPanel bubble = createBubble(sender, content, time, imageBase64, isMe);

            JPanel wrapper = new JPanel(new FlowLayout(isMe ? FlowLayout.RIGHT : FlowLayout.LEFT, 4, 2));
            wrapper.setOpaque(false);
            wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
            wrapper.add(bubble);

            messagesPanel.add(wrapper);
            messagesPanel.add(Box.createVerticalStrut(6));
            messagesPanel.revalidate();
            scrollToBottom();
        });
    }

    private JPanel createBubble(String sender, String content, String time, String imageBase64, boolean isMe) {
        JPanel outer = new JPanel();
        outer.setLayout(new BoxLayout(outer, BoxLayout.Y_AXIS));
        outer.setOpaque(false);
        outer.setMaximumSize(new Dimension(360, Integer.MAX_VALUE));

        // Sender label (only for others)
        if (!isMe) {
            JLabel lblSender = new JLabel(sender);
            lblSender.setFont(new Font("Segoe UI", Font.BOLD, 11));
            lblSender.setForeground(new Color(249, 115, 22));
            lblSender.setBorder(new EmptyBorder(0, 14, 2, 14));
            outer.add(lblSender);
        }

        Color bubbleColor = isMe ? BUBBLE_MINE : BUBBLE_OTHER;
        Color textColor   = isMe ? Color.WHITE  : TEXT_DARK;
        Color timeFg      = isMe ? new Color(255, 237, 213) : TEXT_SOFT;

        JPanel bubble = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (isMe) {
                    // Gradient for my bubbles
                    GradientPaint gp = new GradientPaint(0, 0, UITheme.ACCENT_LIGHT, getWidth(), getHeight(), UITheme.ACCENT_DARK);
                    g2.setPaint(gp);
                    // Asymmetrical border for iMessage feel
                    g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 24, 24));
                    g2.fillRect(getWidth() - 12, getHeight() - 12, 12, 12);
                    g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 24, 24));
                } else {
                    g2.setColor(bubbleColor);
                    g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 24, 24));
                    g2.fillRect(0, getHeight() - 12, 12, 12);
                    g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 24, 24));
                    
                    g2.setColor(BORDER_CLR);
                    g2.setStroke(new BasicStroke(1f));
                    g2.draw(new RoundRectangle2D.Double(0, 0, getWidth()-1, getHeight()-1, 24, 24));
                }
                g2.dispose();
            }
        };
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setOpaque(false);
        bubble.setBorder(new EmptyBorder(10, 16, 10, 16));

        JLabel lblContent = new JLabel();
        if (content.length() < 30 && !content.contains("\n")) {
            lblContent.setText(content);
        } else {
            lblContent.setText("<html><p style='width:260px;word-wrap:break-word;'>"
                + content.replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>") + "</p></html>");
        }
        lblContent.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblContent.setForeground(textColor);
        bubble.add(lblContent);

        if (imageBase64 != null && !imageBase64.isEmpty()) {
            try {
                byte[] b = Base64.getDecoder().decode(imageBase64);
                BufferedImage bi = ImageIO.read(new ByteArrayInputStream(b));
                if (bi != null) {
                    JLabel lblImg = new JLabel(new ImageIcon(bi));
                    lblImg.setBorder(new EmptyBorder(5, 0, 5, 0));
                    bubble.add(lblImg);
                }
            } catch (Exception ignored) {}
        }

        JLabel lblTime = new JLabel(time);
        lblTime.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblTime.setForeground(timeFg);
        lblTime.setAlignmentX(isMe ? Component.RIGHT_ALIGNMENT : Component.LEFT_ALIGNMENT);
        lblTime.setBorder(new EmptyBorder(3, 0, 0, 0));
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
            lbl.setForeground(TEXT_SOFT);
            lbl.setBackground(BUBBLE_SYS_BG);
            lbl.setOpaque(true);
            lbl.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_CLR, 1, true),
                new EmptyBorder(4, 14, 4, 14)
            ));

            wrapper.add(lbl);
            messagesPanel.add(wrapper);
            messagesPanel.add(Box.createVerticalStrut(8));
            messagesPanel.revalidate();
            scrollToBottom();
        });
    }

    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            if (scrollPane != null) {
                JScrollBar bar = scrollPane.getVerticalScrollBar();
                if (bar != null) {
                    bar.setValue(bar.getMaximum());
                }
            }
        });
    }

    // ─── MessageListener ──────────────────────────────────────────────────────
    @Override
    public void onMessage(MessageType type, Map<String, String> data) {
        if (type == MessageType.CHAT_ROOM && !isPrivateChat) {
            String roomId = data.get("roomId");
            if (roomId != null && roomId.equals(currentRoomId)) {
                String sender  = data.getOrDefault("senderName", "Hệ thống");
                String content = data.getOrDefault("content", "");
                if (!sender.equals(myUsername)) appendBubble(sender, content, null, false);
            }
        } else if (type == MessageType.CHAT_PRIVATE) {
            String sender  = data.get("senderName");
            String content = data.getOrDefault("content", "");
            String imageBase64 = data.get("image");
            if (sender == null || sender.equals(myUsername)) return;
            
            if (isPrivateChat && sender.equals(currentReceiverUsername)) {
                // Active conversation - show immediately
                appendBubble(sender, content, imageBase64, false);
            } else {
                // Buffer the message until the user is selected
                pendingMessages
                    .computeIfAbsent(sender, k -> new java.util.LinkedList<>())
                    .add(new String[]{sender, content, imageBase64});
            }
        }
    }

    /** Returns true if there are buffered offline messages from this sender */
    public boolean hasPendingMessages(String sender) {
        java.util.LinkedList<String[]> buf = pendingMessages.get(sender);
        return buf != null && !buf.isEmpty();
    }

    private static class ChatMessage {
        String sender, content, time;
        boolean isMe;
    }
}
