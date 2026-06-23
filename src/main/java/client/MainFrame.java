package client;

import shared.MessageType;
import javax.swing.*;
import java.awt.*;
import java.util.Map;
import com.formdev.flatlaf.FlatDarkLaf;

public class    MainFrame extends JFrame implements NetworkClient.MessageListener {
    private CardLayout cardLayout;
    private JPanel mainPanel;

    private LoginPanel loginPanel;
    private RegisterPanel registerPanel;
    
    // Khai báo trước các Dashboard
    private UserDashboard userDashboard;
    private ModeratorDashboard moderatorDashboard;
    private AdminDashboard adminDashboard;

    public MainFrame() {
        setTitle("AuctionPro - Hệ Thống Đấu Giá Trực Tuyến");
        setSize(1200, 780);
        setMinimumSize(new Dimension(900, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        if (!NetworkClient.getInstance().connect()) {
            JOptionPane.showMessageDialog(this, "Không thể kết nối đến máy chủ!", "Lỗi Kết Nối", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
        NetworkClient.getInstance().addListener(this);

        loginPanel = new LoginPanel(this);
        registerPanel = new RegisterPanel(this);

        mainPanel.add(loginPanel, "LOGIN");
        mainPanel.add(registerPanel, "REGISTER");

        add(mainPanel);
        switchPanel("LOGIN");
    }

    public void switchPanel(String name) {
        cardLayout.show(mainPanel, name);
    }

    public void addPanel(JPanel panel, String name) {
        mainPanel.add(panel, name);
    }

    public void onLoginSuccess(String role) {
        String username = NetworkClient.getInstance().getCurrentUsername();
        System.out.println("[MainFrame] Login success - User: " + username + ", Role: " + role);
        setTitle("AuctionPro - " + username + " (" + role + ")");

        if ("USER".equals(role)) {
            if (userDashboard == null) {
                userDashboard = new UserDashboard(this);
                mainPanel.add(userDashboard, "USER_DASHBOARD");
            }
            switchPanel("USER_DASHBOARD");
        } else if ("MODERATOR".equals(role)) {
            if (moderatorDashboard == null) {
                moderatorDashboard = new ModeratorDashboard(this);
                mainPanel.add(moderatorDashboard, "MOD_DASHBOARD");
            }
            switchPanel("MOD_DASHBOARD");
        } else if ("ADMIN".equals(role)) {
            if (adminDashboard == null) {
                adminDashboard = new AdminDashboard(this);
                mainPanel.add(adminDashboard, "ADMIN_DASHBOARD");
            }
            switchPanel("ADMIN_DASHBOARD");
        } else {
            JOptionPane.showMessageDialog(this, "Role không xác định: " + role, "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void onMessage(MessageType type, Map<String, String> data) {
        // Global listener cho các event quan trọng như KICK_USER hoặc ERROR hệ thống
        if (type == MessageType.KICK_USER) {
            JOptionPane.showMessageDialog(this, "Bạn đã bị quản trị viên ngắt kết nối.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            System.exit(0);
        }
        
        // Mời Seller vào phòng
        if (type == MessageType.INVITE_SELLER) {
            String roomId = data.get("roomId");
            int confirm = JOptionPane.showConfirmDialog(this, 
                "Bạn nhận được lời mời tham gia bán sản phẩm ở phòng: " + roomId + "\nBạn có đồng ý không?", 
                "Lời mời tham gia đấu giá", 
                JOptionPane.YES_NO_OPTION);
                
            java.util.Map<String, String> res = new java.util.HashMap<>();
            res.put("roomId", roomId);
            if (confirm == JOptionPane.YES_OPTION) {
                res.put("status", "ACCEPTED");
                NetworkClient.getInstance().sendMessage(MessageType.SELLER_CONFIRM, res);
            } else {
                res.put("status", "REJECTED");
                NetworkClient.getInstance().sendMessage(MessageType.SELLER_CONFIRM, res);
            }
        }
    }

    public static void main(String[] args) {
        try {
            // Khử răng cưa font chữ hệ thống
            System.setProperty("awt.useSystemAAFontSettings", "on");
            System.setProperty("swing.aatext", "true");

            // ── BƯỚC 1: Thiết lập FlatDarkLaf ──────────────────────────────────
            FlatDarkLaf.setup();

            // ── BƯỚC 2: Custom theme màu cam (#FF6B35) làm accent color ─────────
            Color accentOrange   = new Color(0xFF6B35);
            Color accentOrangeDk = new Color(0xE5521A);

            // Focus & selection highlight
            UIManager.put("Component.focusColor",         accentOrange);
            UIManager.put("Component.focusedBorderColor", accentOrange);

            // Default button (primary)
            UIManager.put("Button.default.background",       accentOrange);
            UIManager.put("Button.default.foreground",       Color.WHITE);
            UIManager.put("Button.default.hoverBackground",  accentOrangeDk);
            UIManager.put("Button.default.pressedBackground",accentOrangeDk);
            UIManager.put("Button.default.focusedBackground",accentOrange);

            // ── Bo góc button & input ─────────────────────────────────────────
            UIManager.put("Button.arc",           12);
            UIManager.put("Component.arc",        10);
            UIManager.put("TextComponent.arc",    10);

            // ── Scrollbar bo tròn hoàn toàn ───────────────────────────────────
            UIManager.put("ScrollBar.thumbArc",   999);
            UIManager.put("ScrollBar.trackArc",   999);
            UIManager.put("ScrollBar.width",      10);
            UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));

            // ── Background tổng thể tông tối ─────────────────────────────────
            UIManager.put("Panel.background",          new Color(0x0F0F1A));
            UIManager.put("RootPane.background",       new Color(0x0F0F1A));
            UIManager.put("OptionPane.background",     new Color(0x1A1A2E));
            UIManager.put("Table.background",          new Color(0x1A1A2E));
            UIManager.put("Table.alternateRowColor",   new Color(0x16213E));
            UIManager.put("Table.selectionBackground", new Color(0xFF6B35, false));
            UIManager.put("Table.selectionForeground", Color.WHITE);

            // ── Font mặc định ─────────────────────────────────────────────────
            UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 13));

            // ── Thanh tiêu đề bảng ────────────────────────────────────────────
            UIManager.put("TableHeader.background",      new Color(0x0F0F1A));
            UIManager.put("TableHeader.font",
                new Font("Segoe UI", Font.BOLD, 12));

            // ── TabbedPane accent ─────────────────────────────────────────────
            UIManager.put("TabbedPane.selectedForeground",  accentOrange);
            UIManager.put("TabbedPane.underlineColor",      accentOrange);
            UIManager.put("TabbedPane.focusColor",          new Color(0xFF6B35, false));

            // ── TextField height (min 36px via insets) ────────────────────────
            UIManager.put("TextField.minimumWidth", 80);

            // ── ProgressBar / Slider accent ───────────────────────────────────
            UIManager.put("ProgressBar.foreground", accentOrange);
            UIManager.put("Slider.foreground",      accentOrange);
            UIManager.put("CheckBox.icon.selectedBackground",    accentOrange);
            UIManager.put("RadioButton.icon.selectedBackground", accentOrange);

        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {}
        }
        
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
