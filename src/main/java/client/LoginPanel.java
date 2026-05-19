package client;

import shared.MessageType;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.HashMap;
import java.util.Map;

public class LoginPanel extends JPanel implements NetworkClient.MessageListener {
    private MainFrame mainFrame;
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnLogin;
    private JLabel lblGoRegister;
    private JLabel lblStatus;

    // Colors
    private final Color PRIMARY_COLOR = new Color(79, 70, 229); // Indigo 600
    private final Color PRIMARY_HOVER = new Color(67, 56, 202); // Indigo 700
    private final Color BACKGROUND_COLOR = new Color(243, 244, 246); // Gray 100
    private final Color CARD_COLOR = Color.WHITE;
    private final Color TEXT_MAIN = new Color(17, 24, 39); // Gray 900
    private final Color TEXT_MUTED = new Color(107, 114, 128); // Gray 500

    public LoginPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());
        setBackground(BACKGROUND_COLOR);

        // Main Wrapper to center the login card
        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setBackground(BACKGROUND_COLOR);

        // The Login Card
        RoundedPanel cardPanel = new RoundedPanel(20, CARD_COLOR);
        cardPanel.setLayout(new GridBagLayout());
        cardPanel.setPreferredSize(new Dimension(450, 500));
        cardPanel.setBorder(new EmptyBorder(40, 40, 40, 40));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 20, 0);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;

        // Title
        JLabel lblTitle = new JLabel("Chào mừng trở lại");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTitle.setForeground(TEXT_MAIN);
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        cardPanel.add(lblTitle, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 30, 0);
        JLabel lblSub = new JLabel("Đăng nhập vào tài khoản của bạn");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        lblSub.setForeground(TEXT_MUTED);
        lblSub.setHorizontalAlignment(SwingConstants.CENTER);
        cardPanel.add(lblSub, gbc);

        // Username Field
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 8, 0);
        JLabel lblUser = new JLabel("Tên đăng nhập");
        lblUser.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblUser.setForeground(TEXT_MAIN);
        cardPanel.add(lblUser, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 20, 0);
        txtUsername = createTextField();
        cardPanel.add(txtUsername, gbc);

        // Password Field
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 8, 0);
        JLabel lblPass = new JLabel("Mật khẩu");
        lblPass.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblPass.setForeground(TEXT_MAIN);
        cardPanel.add(lblPass, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 15, 0);
        txtPassword = createPasswordField();
        cardPanel.add(txtPassword, gbc);

        // Status Label
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 15, 0);
        lblStatus = new JLabel(" ");
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblStatus.setForeground(new Color(220, 38, 38)); // Red 600
        lblStatus.setHorizontalAlignment(SwingConstants.CENTER);
        cardPanel.add(lblStatus, gbc);

        // Login Button
        gbc.gridy++;
        gbc.insets = new Insets(10, 0, 20, 0);
        btnLogin = createPrimaryButton("Đăng nhập");
        cardPanel.add(btnLogin, gbc);

        // Register Link
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 0, 0);
        lblGoRegister = new JLabel("Chưa có tài khoản? Đăng ký ngay");
        lblGoRegister.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblGoRegister.setForeground(PRIMARY_COLOR);
        lblGoRegister.setHorizontalAlignment(SwingConstants.CENTER);
        lblGoRegister.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        lblGoRegister.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                mainFrame.switchPanel("REGISTER");
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                lblGoRegister.setText("<html><u>Chưa có tài khoản? Đăng ký ngay</u></html>");
            }
            @Override
            public void mouseExited(MouseEvent e) {
                lblGoRegister.setText("Chưa có tài khoản? Đăng ký ngay");
            }
        });
        cardPanel.add(lblGoRegister, gbc);

        centerWrapper.add(cardPanel);
        add(centerWrapper, BorderLayout.CENTER);

        // Actions
        btnLogin.addActionListener(e -> doLogin());
        txtPassword.addActionListener(e -> doLogin()); // Press enter to login
    }

    private JTextField createTextField() {
        JTextField tf = new JTextField();
        tf.setPreferredSize(new Dimension(0, 45));
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        tf.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(209, 213, 219), 1, true), // Gray 300
            new EmptyBorder(5, 15, 5, 15)
        ));
        return tf;
    }

    private JPasswordField createPasswordField() {
        JPasswordField pf = new JPasswordField();
        pf.setPreferredSize(new Dimension(0, 45));
        pf.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        pf.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(209, 213, 219), 1, true),
            new EmptyBorder(5, 15, 5, 15)
        ));
        return pf;
    }

    private JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) {
                    g2.setColor(PRIMARY_HOVER.darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(PRIMARY_HOVER);
                } else {
                    g2.setColor(PRIMARY_COLOR);
                }
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setPreferredSize(new Dimension(0, 45));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btn.setForeground(Color.WHITE);
        btn.setBackground(PRIMARY_COLOR);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void doLogin() {
        String user = txtUsername.getText().trim();
        String pass = new String(txtPassword.getPassword());
        if (user.isEmpty() || pass.isEmpty()) {
            lblStatus.setText("Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        lblStatus.setText("Đang kết nối...");
        lblStatus.setForeground(PRIMARY_COLOR);
        btnLogin.setEnabled(false);
        btnLogin.setText("Đang xử lý...");

        // Ensure Network is connected
        if (!NetworkClient.getInstance().connect()) {
            lblStatus.setText("Không thể kết nối máy chủ!");
            lblStatus.setForeground(new Color(220, 38, 38));
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
            if (type == MessageType.SUCCESS && data.containsKey("sessionToken")) {
                NetworkClient.getInstance().removeListener(this);
                String token = data.get("sessionToken");
                String role = data.get("role");
                NetworkClient.getInstance().setSessionInfo(token, role, txtUsername.getText());
                
                lblStatus.setText("Đăng nhập thành công!");
                lblStatus.setForeground(new Color(16, 185, 129)); // Emerald 500
                btnLogin.setEnabled(true);
                btnLogin.setText("Đăng nhập");
                
                mainFrame.onLoginSuccess(role);
            } else if (type == MessageType.ERROR) {
                lblStatus.setText(data.get("message"));
                lblStatus.setForeground(new Color(220, 38, 38));
                btnLogin.setEnabled(true);
                btnLogin.setText("Đăng nhập");
                NetworkClient.getInstance().removeListener(this);
            }
        }
    }

    // Custom Rounded Panel for the Card
    class RoundedPanel extends JPanel {
        private int cornerRadius;
        private Color bgColor;

        public RoundedPanel(int radius, Color bgColor) {
            super();
            this.cornerRadius = radius;
            this.bgColor = bgColor;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Draw shadow
            g2.setColor(new Color(0, 0, 0, 15));
            g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, cornerRadius, cornerRadius);
            g2.setColor(new Color(0, 0, 0, 10));
            g2.fillRoundRect(4, 4, getWidth() - 8, getHeight() - 8, cornerRadius, cornerRadius);
            
            // Draw background
            g2.setColor(bgColor);
            g2.fillRoundRect(0, 0, getWidth() - 6, getHeight() - 6, cornerRadius, cornerRadius);
            
            g2.dispose();
        }
    }
}
