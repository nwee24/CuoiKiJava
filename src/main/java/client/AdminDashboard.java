package client;

import shared.MessageType;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Map;

public class AdminDashboard extends JPanel implements NetworkClient.MessageListener {

    private String activeTab = "USERS";
    private JPanel contentArea;
    private MainFrame mainFrame;

    private JTable tblUsers, tblRooms, tblPenalties;
    private DefaultTableModel userModel, roomModel, penaltyModel;
    private JLabel lblSessions, lblCommission;
    private JButton btnNavUsers, btnNavRooms, btnNavPenalties;

    public AdminDashboard(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());

        add(buildHeader(),  BorderLayout.NORTH);
        add(buildSidebar(), BorderLayout.WEST);

        contentArea = new JPanel(new CardLayout());
        contentArea.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        contentArea.add(buildUsersPanel(),     "USERS");
        contentArea.add(buildRoomsPanel(),     "ROOMS");
        contentArea.add(buildPenaltiesPanel(), "PENALTIES");
        add(contentArea, BorderLayout.CENTER);

        NetworkClient.getInstance().addListener(this);
        fetchData();
        showTab("USERS");
    }

    private JPanel buildHeader() {
        JPanel h = new JPanel(new BorderLayout());
        h.setPreferredSize(new Dimension(0, 60));
        h.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, UIManager.getColor("Component.borderColor")),
            new EmptyBorder(0, 20, 0, 20)
        ));

        // Left – Logo
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JLabel logoIcon = new JLabel("⚡");
        logoIcon.setFont(new Font("Segoe UI", Font.BOLD, 24));
        logoIcon.setForeground(UIManager.getColor("Actions.Red"));
        
        JLabel logoText = new JLabel("AuctionPro");
        logoText.setFont(new Font("Segoe UI", Font.BOLD, 18));
        
        JLabel adminSep = new JLabel(" | Admin");
        adminSep.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        adminSep.setForeground(UIManager.getColor("Label.disabledForeground"));
        
        left.add(logoIcon); left.add(logoText); left.add(adminSep);
        h.add(left, BorderLayout.WEST);

        // Right – user chip + refresh
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));

        JButton btnRefresh = new JButton("Làm Mới");
        btnRefresh.setIcon(UIManager.getIcon("Tree.leafIcon")); // Default icon as placeholder
        btnRefresh.addActionListener(e -> fetchData());
        right.add(btnRefresh);

        String username = NetworkClient.getInstance().getCurrentUsername();
        JLabel userLabel = new JLabel("👤 " + (username != null ? username : "Admin"));
        userLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        right.add(userLabel);
        
        h.add(right, BorderLayout.EAST);
        return h;
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(250, 0));
        sidebar.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 0, 1, UIManager.getColor("Component.borderColor")),
            new EmptyBorder(20, 15, 20, 15)
        ));

        // Stat cards
        sidebar.add(buildSideStatCard("Phiên Đấu Giá", "—", true));
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(buildSideStatCard("Doanh Thu (VND)", "—", false));
        sidebar.add(Box.createVerticalStrut(20));

        JLabel navLabel = new JLabel("QUẢN TRỊ");
        navLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        navLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        sidebar.add(navLabel);
        sidebar.add(Box.createVerticalStrut(10));

        btnNavUsers     = makeNavItem("👤 Người Dùng");
        btnNavRooms     = makeNavItem("🏠 Phòng Đấu Giá");
        btnNavPenalties = makeNavItem("⚠️ Lịch Sử Phạt");

        btnNavUsers.addActionListener(e -> showTab("USERS"));
        btnNavRooms.addActionListener(e -> showTab("ROOMS"));
        btnNavPenalties.addActionListener(e -> showTab("PENALTIES"));

        sidebar.add(btnNavUsers);     sidebar.add(Box.createVerticalStrut(5));
        sidebar.add(btnNavRooms);     sidebar.add(Box.createVerticalStrut(5));
        sidebar.add(btnNavPenalties);
        sidebar.add(Box.createVerticalGlue());

        JButton btnLogout = new JButton("Đăng xuất");
        btnLogout.setForeground(UIManager.getColor("Actions.Red"));
        btnLogout.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btnLogout.addActionListener(e -> mainFrame.switchPanel("LOGIN"));
        sidebar.add(btnLogout);
        
        return sidebar;
    }

    private JPanel buildSideStatCard(String label, String value, boolean isSessions) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(UIManager.getColor("Component.borderColor"), 1, true),
            new EmptyBorder(15, 15, 15, 15)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        JLabel valLbl = new JLabel(value);
        valLbl.setFont(new Font("Segoe UI", Font.BOLD, 22));
        card.add(valLbl, BorderLayout.CENTER);

        if (isSessions) lblSessions = valLbl;
        else lblCommission = valLbl;

        JLabel lblLbl = new JLabel(label);
        lblLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblLbl.setForeground(UIManager.getColor("Label.disabledForeground"));
        card.add(lblLbl, BorderLayout.SOUTH);
        
        return card;
    }

    private JButton makeNavItem(String label) {
        JButton btn = new JButton(label);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btn.putClientProperty("JButton.buttonType", "roundRect");
        return btn;
    }

    private void showTab(String tabKey) {
        activeTab = tabKey;
        ((CardLayout) contentArea.getLayout()).show(contentArea, tabKey);
        
        Font boldFont = new Font("Segoe UI", Font.BOLD, 14);
        Font plainFont = new Font("Segoe UI", Font.PLAIN, 14);
        
        btnNavUsers.setFont(plainFont);
        btnNavRooms.setFont(plainFont);
        btnNavPenalties.setFont(plainFont);
        
        if ("USERS".equals(tabKey)) btnNavUsers.setFont(boldFont);
        else if ("ROOMS".equals(tabKey)) btnNavRooms.setFont(boldFont);
        else btnNavPenalties.setFont(boldFont);
    }

    private JPanel buildUsersPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.add(buildPageHeader("Quản Lý Người Dùng", "Toàn bộ tài khoản trong hệ thống"), BorderLayout.NORTH);
        
        userModel = new DefaultTableModel(new Object[]{"ID", "Tên Đăng Nhập", "Vai Trò", "Số Dư (VND)", "Trạng Thái"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblUsers = buildTable(userModel);
        panel.add(new JScrollPane(tblUsers), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildRoomsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.add(buildPageHeader("Phòng Đấu Giá", "Theo dõi tất cả các phòng"), BorderLayout.NORTH);
        
        roomModel = new DefaultTableModel(new Object[]{"Mã Phòng", "Moderator", "Trạng Thái", "Bắt Đầu", "Kết Thúc"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblRooms = buildTable(roomModel);
        panel.add(new JScrollPane(tblRooms), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildPenaltiesPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.add(buildPageHeader("Lịch Sử Phạt", "Các khoản phạt đã được ghi nhận"), BorderLayout.NORTH);
        
        penaltyModel = new DefaultTableModel(new Object[]{"ID Phạt", "Người Dùng", "Lý Do", "Số Tiền (VND)", "Ngày Phạt"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblPenalties = buildTable(penaltyModel);
        panel.add(new JScrollPane(tblPenalties), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildPageHeader(String title, String sub) {
        JPanel p = new JPanel(); 
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS)); 
        
        JLabel t = new JLabel(title); 
        t.setFont(new Font("Segoe UI", Font.BOLD, 24)); 
        
        JLabel s = new JLabel(sub); 
        s.setFont(new Font("Segoe UI", Font.PLAIN, 14)); 
        s.setForeground(UIManager.getColor("Label.disabledForeground")); 
        
        p.add(t); p.add(Box.createVerticalStrut(5)); p.add(s);
        return p;
    }

    private JTable buildTable(DefaultTableModel model) {
        JTable tbl = new JTable(model);
        tbl.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tbl.setRowHeight(35);
        tbl.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        return tbl;
    }

    private void fetchData() {
        NetworkClient.getInstance().sendMessage(MessageType.GET_ADMIN_STATS, null);
        NetworkClient.getInstance().sendMessage(MessageType.GET_PENALTY_LIST, null);
    }

    @Override
    public void onMessage(MessageType type, Map<String, String> data) {
        SwingUtilities.invokeLater(() -> {
            if (type == MessageType.GET_ADMIN_STATS) {
                if (lblSessions != null) lblSessions.setText(data.getOrDefault("totalSessions", "0"));
                if (lblCommission != null) lblCommission.setText(formatMoney(data.getOrDefault("totalCommission", "0")));
                
                userModel.setRowCount(0);
                String u = data.get("users");
                if (u != null && !u.isEmpty()) {
                    for (String row : u.split("\\|")) userModel.addRow(row.split(",", -1));
                }
                
                roomModel.setRowCount(0);
                String r = data.get("rooms");
                if (r != null && !r.isEmpty()) {
                    for (String row : r.split("\\|")) roomModel.addRow(row.split(",", -1));
                }
            } else if (type == MessageType.GET_PENALTY_LIST) {
                penaltyModel.setRowCount(0);
                String p = data.get("penalties");
                if (p != null && !p.isEmpty()) {
                    for (String row : p.split("\\|")) penaltyModel.addRow(row.split(",", -1));
                }
            }
        });
    }

    private String formatMoney(String raw) {
        try { 
            return String.format("%,d", Long.parseLong(raw.trim())).replace(',', '.'); 
        } catch (Exception e) { 
            return raw; 
        }
    }
}
