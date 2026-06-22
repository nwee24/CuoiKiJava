package client;

import shared.MessageType;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.HashMap;
import java.util.Map;
import net.miginfocom.swing.MigLayout;

public class RegisterPanel extends JPanel implements NetworkClient.MessageListener {
    private MainFrame mainFrame;
    private JTextField txtUsername;
    private JTextField txtEmail;
    private JTextField txtPhone;
    private JPasswordField txtPassword;
    private JPasswordField txtConfirmPassword;
    private JButton btnUser, btnModerator;
    private boolean isUserSelected = true;
    private JButton btnRegister;
    private JLabel lblGoLogin;
    private JLabel lblStatus;

    public RegisterPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new GridBagLayout());
        setOpaque(true);
        setBackground(UITheme.LIGHT_BG);

        // --- Card Đăng ký (Phần màu trắng) ---
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
        cardShadowPanel.setBorder(new EmptyBorder(10, 10, 18, 18));
        cardShadowPanel.setPreferredSize(new Dimension(1000, 660));
        cardShadowPanel.add(cardPanel, BorderLayout.CENTER);

        // ======== CỘT TRÁI (ẢNH VÀ CHỮ OVERLAY CẦU KỲ) ========
        JPanel leftPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Clip để góc trái bo tròn dính theo card
                Shape roundedRect = new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 40, 40);
                g2d.clip(new Rectangle(0, 0, getWidth() + 40, getHeight()));
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
                g2d.drawString("Tạo tài khoản", 40, getHeight() / 2 - 20);
                
                // Dòng phụ màu cam nổi bật
                g2d.setColor(UITheme.ORANGE);
                g2d.drawString("nhanh chóng & dễ dàng", 40, getHeight() / 2 + 25);

                // Mô tả
                g2d.setColor(UITheme.TEXT_MUTED);
                g2d.setFont(UITheme.fontBody(15));
                g2d.drawString("Bắt đầu đăng sản phẩm và đấu giá chuyên nghiệp,", 40, getHeight() / 2 + 75);
                g2d.drawString("gia nhập cộng đồng đấu giá hàng đầu.", 40, getHeight() / 2 + 100);

                g2d.dispose();
            }
        };
        leftPanel.setOpaque(false);
        cardPanel.add(leftPanel);

        // ======== CỘT PHẢI (FORM NHẬP LIỆU) ========
        JPanel rightPanel = new JPanel(new MigLayout("wrap 1, insets 15 40 15 40, gapy 2, fillx", "[grow]"));
        rightPanel.setOpaque(false);

        // Header Title
        JLabel title = new JLabel("Đăng ký", SwingConstants.CENTER);
        title.setFont(UITheme.fontBold(26));
        title.setForeground(UITheme.TEXT_PRIMARY);
        rightPanel.add(title, "growx, align center, gapTop 5");

        JLabel subTitle = new JLabel("Tạo tài khoản để tham gia đấu giá", SwingConstants.CENTER);
        subTitle.setFont(UITheme.fontBody(13));
        subTitle.setForeground(UITheme.TEXT_MUTED);
        rightPanel.add(subTitle, "growx, align center, gapBottom 10");

        // Username
        rightPanel.add(fieldLabel("Tên đăng nhập"), "growx");
        txtUsername = UITheme.customTextField("Nhập tên đăng nhập...", UITheme.BG_DARK, UITheme.BORDER);
        rightPanel.add(txtUsername, "growx, h 38!");

        // Email
        rightPanel.add(fieldLabel("Email"), "growx, gapTop 8");
        txtEmail = UITheme.customTextField("Nhập email...", UITheme.BG_DARK, UITheme.BORDER);
        rightPanel.add(txtEmail, "growx, h 38!");

        // Phone
        rightPanel.add(fieldLabel("Số điện thoại"), "growx, gapTop 8");
        txtPhone = UITheme.customTextField("Nhập số điện thoại...", UITheme.BG_DARK, UITheme.BORDER);
        rightPanel.add(txtPhone, "growx, h 38!");

        // Password
        rightPanel.add(fieldLabel("Mật khẩu"), "growx, gapTop 8");
        txtPassword = UITheme.customPasswordField("Nhập mật khẩu...", UITheme.BG_DARK, UITheme.BORDER);
        rightPanel.add(txtPassword, "growx, h 38!");

        // Confirm Password
        rightPanel.add(fieldLabel("Xác nhận mật khẩu"), "growx, gapTop 8");
        txtConfirmPassword = UITheme.customPasswordField("Nhập lại mật khẩu...", UITheme.BG_DARK, UITheme.BORDER);
        rightPanel.add(txtConfirmPassword, "growx, h 38!");

        // Role Toggle
        rightPanel.add(fieldLabel("Vai trò"), "growx, gapTop 8");
        rightPanel.add(buildRoleToggle(), "growx, h 38!");

        // Status
        lblStatus = new JLabel(" ");
        lblStatus.setFont(UITheme.fontBody(11));
        lblStatus.setForeground(UITheme.DANGER);
        lblStatus.setHorizontalAlignment(SwingConstants.CENTER);
        rightPanel.add(lblStatus, "growx, gapTop 4, gapBottom 4");

        // Button Register
        btnRegister = UITheme.customBtn("Đăng ký tài khoản  →", UITheme.ACCENT, UITheme.ACCENT_DARK, Color.WHITE, 35);
        rightPanel.add(btnRegister, "growx, h 40!, gapTop 8");

        // Login link
        lblGoLogin = new JLabel("<html>Đã có tài khoản? <font color='#FF8C5A'>Đăng nhập ngay</font></html>", SwingConstants.CENTER);
        lblGoLogin.setFont(UITheme.fontBody(13));
        lblGoLogin.setForeground(UITheme.TEXT_PRIMARY);
        lblGoLogin.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblGoLogin.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { mainFrame.switchPanel("LOGIN"); }
            public void mouseEntered(MouseEvent e) {
                lblGoLogin.setText("<html><u>Đã có tài khoản? <font color='#FF6B35'>Đăng nhập ngay</font></u></html>");
            }
            public void mouseExited(MouseEvent e) {
                lblGoLogin.setText("<html>Đã có tài khoản? <font color='#FF8C5A'>Đăng nhập ngay</font></html>");
            }
        });
        rightPanel.add(lblGoLogin, "growx, gapTop 10");

        cardPanel.add(rightPanel);

        // Add card to center
        GridBagConstraints centerGbc = new GridBagConstraints();
        centerGbc.gridx = 0;
        centerGbc.gridy = 0;
        add(cardShadowPanel, centerGbc);

        btnRegister.addActionListener(e -> doRegister());
        txtConfirmPassword.addActionListener(e -> doRegister());
    }

    private JPanel buildRoleToggle() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 8, 0));
        panel.setOpaque(false);

        btnUser = new JButton("👤  Người Dùng");
        btnModerator = new JButton("🎯  Moderator");

        styleToggle(btnUser, true);
        styleToggle(btnModerator, false);

        btnUser.addActionListener(e -> { isUserSelected = true;  styleToggle(btnUser, true);  styleToggle(btnModerator, false); });
        btnModerator.addActionListener(e -> { isUserSelected = false; styleToggle(btnUser, false); styleToggle(btnModerator, true);  });

        panel.add(btnUser);
        panel.add(btnModerator);
        return panel;
    }

    private void styleToggle(JButton btn, boolean active) {
        btn.setFont(UITheme.fontBody(13));
        btn.setFocusPainted(false);
        btn.setBorderPainted(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.putClientProperty("JButton.buttonType", "roundRect");
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(active ? UITheme.ACCENT : UITheme.BORDER_LIGHT, 1),
            new EmptyBorder(10, 0, 10, 0)
        ));
        btn.setBackground(active ? new Color(255, 107, 53, 50) : UITheme.BG_ELEVATED);
        btn.setForeground(active ? UITheme.ACCENT : UITheme.TEXT_MUTED);
    }

    private JLabel fieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(UITheme.fontBold(12));
        lbl.setForeground(UITheme.TEXT_MUTED);
        return lbl;
    }

    private void doRegister() {
        String user    = txtUsername.getText().trim();
        String pass    = new String(txtPassword.getPassword());
        String confirm = new String(txtConfirmPassword.getPassword());
        String email   = txtEmail.getText().trim();
        String phone   = txtPhone.getText().trim();

        if (user.isEmpty() || pass.isEmpty() || confirm.isEmpty() || email.isEmpty() || phone.isEmpty()) {
            lblStatus.setText("Vui lòng nhập đầy đủ thông tin!");
            return;
        }
        
        // Validate Email
        String emailRegex = "^[A-Za-z0-9+_.-]+@gmail\\.com$";
        if (!email.matches(emailRegex)) {
            lblStatus.setText("Email phải có đuôi @gmail.com!");
            return;
        }
        
        // Validate Phone (Đủ 10 chữ số)
        String phoneRegex = "^\\d{10}$";
        if (!phone.matches(phoneRegex)) {
            lblStatus.setText("Số điện thoại phải bao gồm đúng 10 số!");
            return;
        }
        
        if (!pass.equals(confirm)) {
            lblStatus.setText("Mật khẩu xác nhận không khớp!");
            return;
        }

        lblStatus.setText("Đang xử lý...");
        lblStatus.setForeground(UITheme.ACCENT_LIGHT);
        btnRegister.setEnabled(false);
        btnRegister.setText("Đang đăng ký...");

        if (!NetworkClient.getInstance().connect()) {
            lblStatus.setText("Không thể kết nối máy chủ!");
            lblStatus.setForeground(UITheme.DANGER);
            btnRegister.setEnabled(true);
            btnRegister.setText("Đăng ký tài khoản");
            return;
        }

        NetworkClient.getInstance().addListener(this);
        Map<String, String> data = new HashMap<>();
        data.put("username", user);
        data.put("password", pass);
        data.put("role", isUserSelected ? "USER" : "MODERATOR");
        data.put("email", email);
        data.put("phone", phone);
        NetworkClient.getInstance().sendMessage(MessageType.REGISTER, data);
    }

    @Override
    public void onMessage(MessageType type, Map<String, String> data) {
        SwingUtilities.invokeLater(() -> {
            if (!btnRegister.isEnabled()) {
                if (type == MessageType.SUCCESS) {
                    NetworkClient.getInstance().removeListener(this);
                    JOptionPane.showMessageDialog(this,
                        "Đăng ký thành công! Vui lòng đăng nhập.",
                        "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    mainFrame.switchPanel("LOGIN");
                    btnRegister.setEnabled(true);
                    btnRegister.setText("Đăng ký tài khoản");
                    lblStatus.setText(" ");
                    txtUsername.setText("");
                    txtEmail.setText("");
                    txtPhone.setText("");
                    txtPassword.setText("");
                    txtConfirmPassword.setText("");
                } else if (type == MessageType.ERROR) {
                    lblStatus.setText(data.get("message"));
                    lblStatus.setForeground(UITheme.DANGER);
                    btnRegister.setEnabled(true);
                    btnRegister.setText("Đăng ký tài khoản");
                    NetworkClient.getInstance().removeListener(this);
                }
            }
        });
    }
}
