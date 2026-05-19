package client;

import shared.MessageType;
import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class MainFrame extends JFrame implements NetworkClient.MessageListener {
    private CardLayout cardLayout;
    private JPanel mainPanel;

    private LoginPanel loginPanel;
    private RegisterPanel registerPanel;git
    
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
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {}
        
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
