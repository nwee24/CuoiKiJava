package client;

import shared.MessageType;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.HashMap;
import java.util.Map;

public class LoginPanel extends JPanel implements NetworkClient.MessageListener {
    private MainFrame mainFrame;
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnLogin;
    private JLabel lblGoRegister;
    private JLabel lblStatus;

    public LoginPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new GridBagLayout());
        setOpaque(true);
        setBackground(UITheme.LIGHT_BG);

        // --- Card Đăng nhập (Phần màu trắng) ---
        JPanel cardPanel = new JPanel(new GridLayout(1, 2));
        cardPanel.setOpaque(false);

        JPanel cardShadowPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Đổ bóng giả mịn màng
                g2.setColor(new Color(0, 0, 0, 15));
                g2.fillRoundRect(8, 8, getWidth() - 16, getHeight() - 16, 40, 40);
                g2.setColor(new Color(0, 0, 0, 10));
                g2.fillRoundRect(4, 4, getWidth() - 8, getHeight() - 8, 40, 40);
                // Vẽ Card chính màu tối
                g2.setColor(UITheme.BG_CARD);
                g2.fillRoundRect(0, 0, getWidth() - 8, getHeight() - 8, 40, 40);
                g2.dispose();
            }
        };
        cardShadowPanel.setOpaque(false);
        cardShadowPanel.setBorder(new EmptyBorder(10, 10, 18, 18)); // Chừa biên cho đổ bóng
        cardShadowPanel.setPreferredSize(new Dimension(1000, 600));
        cardShadowPanel.add(cardPanel, BorderLayout.CENTER);

        // ======== CỘT TRÁI (ẢNH VÀ CHỮ OVERLAY CẦU KỲ) ========
        JPanel leftPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Clip để góc trái bo tròn dính theo card
                Shape roundedRect = new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 40, 40);
                g2d.clip(new Rectangle(0, 0, getWidth() + 40, getHeight())); // Bỏ clip bên phải
                g2d.clip(roundedRect);

                // Vẽ nền gradient tối hiện đại
                GradientPaint gp = new GradientPaint(
                    0, 0, UITheme.BG_DEEP,
                    0, getHeight(), UITheme.BG_DARK
                );
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());

                // Các vòng tròn phát sáng trừu tượng
                g2d.setColor(new Color(59, 130, 246, 30));
                g2d.fillOval(-100, -100, 300, 300);
                g2d.setColor(new Color(239, 68, 68, 15));
                g2d.fillOval(getWidth() - 150, getHeight() - 200, 250, 250);

                // Vẽ Logo top-left (⚡ dùng Segoe UI Emoji, text dùng Segoe UI Bold)
                g2d.setColor(Color.WHITE);
                g2d.setFont(UITheme.fontEmoji(22));
                g2d.drawString("⚡", 40, 55);
                g2d.setFont(UITheme.fontBold(22));
                int emojiW = g2d.getFontMetrics(UITheme.fontEmoji(22)).stringWidth("⚡");
                g2d.drawString(" AuctionPro", 40 + emojiW, 55);
                g2d.setFont(UITheme.fontBody(11));
                g2d.setColor(UITheme.TEXT_MUTED);
                g2d.drawString("HỆ THỐNG ĐẤU GIÁ TRỰC TUYẾN", 40, 75);

                // Tiêu đề to giữa màn hình
                g2d.setColor(Color.WHITE);
                g2d.setFont(UITheme.fontBold(36));
                g2d.drawString("Đấu giá trực tuyến", 40, getHeight() / 2 - 20);
                
                // Dòng phụ màu cam nổi bật
                g2d.setColor(UITheme.ORANGE);
                g2d.drawString("an toàn & minh bạch", 40, getHeight() / 2 + 25);

                // Mô tả
                g2d.setColor(UITheme.TEXT_MUTED);
                g2d.setFont(UITheme.fontBody(15));
                g2d.drawString("Nền tảng giao dịch trực tuyến thời gian thực,", 40, getHeight() / 2 + 75);
                g2d.drawString("kết nối người mua và người bán trên toàn quốc.", 40, getHeight() / 2 + 100);

                g2d.dispose();
            }
        };
        leftPanel.setOpaque(false);
        cardPanel.add(leftPanel);

        // ======== CỘT PHẢI (FORM NHẬP LIỆU) ========
        JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 40, 8, 40);

        // Header Title
        JLabel title = new JLabel("Đăng nhập", SwingConstants.CENTER);
        title.setFont(UITheme.fontBold(28));
        title.setForeground(UITheme.TEXT_PRIMARY);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 40, 5, 40);
        rightPanel.add(title, gbc);

        JLabel subTitle = new JLabel("Chào mừng bạn trở lại hệ thống", SwingConstants.CENTER);
        subTitle.setFont(UITheme.fontBody(14));
        subTitle.setForeground(UITheme.TEXT_MUTED);
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 40, 30, 40);
        rightPanel.add(subTitle, gbc);

        gbc.gridwidth = 1;
        gbc.insets = new Insets(5, 40, 2, 40);

        // Username
        gbc.gridy = 2;
        JLabel lblUser = new JLabel("Tên đăng nhập / Email");
        lblUser.setFont(UITheme.fontBold(13));
        lblUser.setForeground(UITheme.TEXT_MUTED);
        rightPanel.add(lblUser, gbc);
        
        gbc.gridy = 3;
        txtUsername = UITheme.customTextField("Nhập tên đăng nhập...", UITheme.BG_DARK, UITheme.BORDER);
        rightPanel.add(txtUsername, gbc);

        // Password
        gbc.gridy = 4;
        gbc.insets = new Insets(15, 40, 2, 40);
        JLabel lblPass = new JLabel("Mật khẩu");
        lblPass.setFont(UITheme.fontBold(13));
        lblPass.setForeground(UITheme.TEXT_MUTED);
        rightPanel.add(lblPass, gbc);

        gbc.gridy = 5;
        gbc.insets = new Insets(5, 40, 5, 40);
        txtPassword = UITheme.customPasswordField("Nhập mật khẩu...", UITheme.BG_DARK, UITheme.BORDER);
        rightPanel.add(txtPassword, gbc);

        // Status
        gbc.gridy = 6;
        gbc.insets = new Insets(8, 40, 8, 40);
        lblStatus = new JLabel(" ");
        lblStatus.setFont(UITheme.fontBody(12));
        lblStatus.setForeground(UITheme.DANGER);
        lblStatus.setHorizontalAlignment(SwingConstants.CENTER);
        rightPanel.add(lblStatus, gbc);

        // Button Login
        gbc.gridy = 7;
        gbc.insets = new Insets(10, 40, 20, 40);
        btnLogin = UITheme.customBtn("Đăng nhập  →", UITheme.ACCENT, UITheme.ACCENT_LIGHT, Color.WHITE, 35);
        rightPanel.add(btnLogin, gbc);

        // Register Link
        gbc.gridy = 8;
        gbc.insets = new Insets(20, 40, 20, 40);
        lblGoRegister = new JLabel("<html>Bạn chưa có tài khoản? <font color='#60A5FA'>Đăng ký ngay</font></html>", SwingConstants.CENTER);
        lblGoRegister.setFont(UITheme.fontBody(14));
        lblGoRegister.setForeground(UITheme.TEXT_PRIMARY);
        lblGoRegister.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblGoRegister.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { mainFrame.switchPanel("REGISTER"); }
            public void mouseEntered(MouseEvent e) {
                lblGoRegister.setText("<html><u>Bạn chưa có tài khoản? <font color='#93C5FD'>Đăng ký ngay</font></u></html>");
            }
            public void mouseExited(MouseEvent e) {
                lblGoRegister.setText("<html>Bạn chưa có tài khoản? <font color='#60A5FA'>Đăng ký ngay</font></html>");
            }
        });
        rightPanel.add(lblGoRegister, gbc);

        cardPanel.add(rightPanel);
        
        // Add card to center
        GridBagConstraints centerGbc = new GridBagConstraints();
        centerGbc.gridx = 0;
        centerGbc.gridy = 0;
        add(cardShadowPanel, centerGbc);

        // Actions
        btnLogin.addActionListener(e -> doLogin());
        txtPassword.addActionListener(e -> doLogin());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
    }

    private JLabel fieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(UITheme.fontBold(12));
        lbl.setForeground(UITheme.TEXT_MUTED);
        return lbl;
    }

    private void doLogin() {
        String user = txtUsername.getText().trim();
        String pass = new String(txtPassword.getPassword());
        if (user.isEmpty() || pass.isEmpty()) {
            lblStatus.setText("Vui lòng nhập đầy đủ thông tin!");
            return;
        }
        lblStatus.setText("Đang kết nối...");
        lblStatus.setForeground(UITheme.ACCENT_LIGHT);
        btnLogin.setEnabled(false);
        btnLogin.setText("Đang xử lý...");

        if (!NetworkClient.getInstance().connect()) {
            lblStatus.setText("Không thể kết nối máy chủ!");
            lblStatus.setForeground(UITheme.DANGER);
            btnLogin.setEnabled(true);
            btnLogin.setText("Đăng nhập");
            return;
        }

        NetworkClient.getInstance().addListener(this);
        Map<String, String> data = new HashMap<>();
        data.put("username", user);
        data.put("password", pass);
        NetworkClient.getInstance().sendMessage(MessageType.LOGIN, data);
    }

    @Override
    public void onMessage(MessageType type, Map<String, String> data) {
        if (!btnLogin.isEnabled()) {
            SwingUtilities.invokeLater(() -> {
                if (type == MessageType.SUCCESS && data.containsKey("sessionToken")) {
                    NetworkClient.getInstance().removeListener(this);
                    String role = data.get("role");
                    NetworkClient.getInstance().setSessionInfo(
                        data.get("sessionToken"), role, txtUsername.getText());
                    lblStatus.setText("Đăng nhập thành công!");
                    lblStatus.setForeground(UITheme.SUCCESS);
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Đăng nhập");
                    mainFrame.onLoginSuccess(role);
                } else if (type == MessageType.ERROR) {
                    lblStatus.setText(data.get("message"));
                    lblStatus.setForeground(UITheme.DANGER);
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Đăng nhập");
                    NetworkClient.getInstance().removeListener(this);
                }
            });
        }
    }
}
