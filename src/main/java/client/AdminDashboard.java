package client;

import shared.MessageType;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import net.miginfocom.swing.MigLayout;
import org.kordamp.ikonli.feather.Feather;

public class AdminDashboard extends JPanel implements NetworkClient.MessageListener {

    private String activeTab = "USERS";
    private JPanel contentArea;
    private MainFrame mainFrame;

    private JTable tblUsers, tblRooms, tblPenalties, tblMods;
    private DefaultTableModel userModel;
    private DefaultTableModel roomModel;
    private DefaultTableModel penaltyModel;
    private DefaultTableModel modModel;
    
    private JComboBox<String> cbFilter;
    private JLabel lblSessions, lblCommission;
    private JButton btnNavDashboard, btnNavUsers, btnNavMods, btnNavRooms, btnNavPenalties;
    private RevenueChartPanel chartPanel;

    public AdminDashboard(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());

        add(buildHeader(),  BorderLayout.NORTH);
        add(buildSidebar(), BorderLayout.WEST);

        contentArea = new JPanel(new CardLayout());
        contentArea.setBackground(UITheme.BG_DARK); // Ensure background is main theme color
        contentArea.setBorder(new EmptyBorder(24, 24, 24, 24));
        
        contentArea.add(buildDashboardPanel(), "DASHBOARD");
        contentArea.add(buildUsersPanel(),     "USERS");
        contentArea.add(buildModsPanel(),      "MODS");
        contentArea.add(buildRoomsPanel(),     "ROOMS");
        contentArea.add(buildPenaltiesPanel(), "PENALTIES");
        add(contentArea, BorderLayout.CENTER);

        NetworkClient.getInstance().addListener(this);
        fetchData();
        showTab("DASHBOARD");
    }

    private JPanel buildHeader() {
        JPanel h = new JPanel(new MigLayout("insets 0 24 0 24, fill", "[left]push[right]"));
        h.setPreferredSize(new Dimension(0, 64)); // 64px height
        h.setBackground(UITheme.BG_ELEVATED);
        h.setBorder(new MatteBorder(0, 0, 1, 0, UITheme.BORDER));

        // Left – Logo
        JPanel left = new JPanel(new MigLayout("insets 0, gap 12"));
        left.setOpaque(false);
        JLabel logoIcon = new JLabel("⚡");
        logoIcon.setFont(new Font("Segoe UI", Font.BOLD, 24));
        logoIcon.setForeground(UIManager.getColor("Actions.Red"));
        
        JLabel logoText = new JLabel("AuctionPro");
        logoText.setFont(new Font("Segoe UI", Font.BOLD, 18));
        
        JLabel adminSep = new JLabel(" | Admin");
        adminSep.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        adminSep.setForeground(UIManager.getColor("Label.disabledForeground"));
        
        left.add(logoIcon); 
        left.add(logoText); 
        left.add(adminSep);
        h.add(left, "cell 0 0");

        // Right – user chip + refresh
        JPanel right = new JPanel(new MigLayout("insets 0, gap 16"));
        right.setOpaque(false);

        JButton btnRefresh = UITheme.ghostBtn("Làm Mới", UITheme.TEXT_MUTED);
        btnRefresh.setIcon(org.kordamp.ikonli.swing.FontIcon.of(Feather.REFRESH_CW, 16, UITheme.TEXT_MUTED));
        btnRefresh.addActionListener(e -> fetchData());
        right.add(btnRefresh);

        String username = NetworkClient.getInstance().getCurrentUsername();
        JLabel userLabel = new JLabel("👤 " + (username != null ? username : "Admin"));
        userLabel.setFont(UITheme.fontBold(14));
        userLabel.setForeground(UITheme.TEXT_PRIMARY);
        right.add(userLabel);
        
        h.add(right, "cell 1 0");
        return h;
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new MigLayout("wrap 1, insets 16, gap 6, fillx"));
        sidebar.setBackground(UITheme.BG_DEEP);
        sidebar.setPreferredSize(new Dimension(280, 0));
        sidebar.setBorder(new MatteBorder(0, 0, 0, 1, UITheme.BORDER));

        JLabel navLabel = new JLabel("QUẢN TRỊ");
        navLabel.setFont(UITheme.fontBold(11));
        navLabel.setForeground(UITheme.TEXT_HINT);
        sidebar.add(navLabel, "wrap 8");

        btnNavDashboard = UITheme.navBtn("Tổng Quan", Feather.PIE_CHART);
        btnNavUsers     = UITheme.navBtn("Người Dùng", Feather.USERS);
        btnNavMods      = UITheme.navBtn("Người Trung Gian", Feather.SHIELD);
        btnNavRooms     = UITheme.navBtn("Phòng Đấu Giá", Feather.HOME);
        btnNavPenalties = UITheme.navBtn("Lịch Sử Phạt", Feather.ALERT_TRIANGLE);

        btnNavDashboard.addActionListener(e -> showTab("DASHBOARD"));
        btnNavUsers.addActionListener(e -> showTab("USERS"));
        btnNavMods.addActionListener(e -> showTab("MODS"));
        btnNavRooms.addActionListener(e -> showTab("ROOMS"));
        btnNavPenalties.addActionListener(e -> showTab("PENALTIES"));

        sidebar.add(btnNavDashboard, "growx");
        sidebar.add(btnNavUsers, "growx");
        sidebar.add(btnNavMods, "growx");
        sidebar.add(btnNavRooms, "growx");
        sidebar.add(btnNavPenalties, "growx");

        JButton btnLogout = UITheme.logoutBtn(UITheme.BG_DEEP, () -> mainFrame.switchPanel("LOGIN"));
        sidebar.add(btnLogout, "growx, pushy, aligny bottom");
        
        return sidebar;
    }

    private void showTab(String tabKey) {
        activeTab = tabKey;
        ((CardLayout) contentArea.getLayout()).show(contentArea, tabKey);

        UITheme.setNavActive(btnNavDashboard, "DASHBOARD".equals(tabKey));
        UITheme.setNavActive(btnNavUsers,     "USERS".equals(tabKey));
        UITheme.setNavActive(btnNavMods,      "MODS".equals(tabKey));
        UITheme.setNavActive(btnNavRooms,     "ROOMS".equals(tabKey));
        UITheme.setNavActive(btnNavPenalties, "PENALTIES".equals(tabKey));
    }

    private JPanel buildDashboardPanel() {
        JPanel panel = UITheme.createCardPanel("wrap 1, insets 24, gap 16, fill");
        panel.add(buildPageHeader("Tổng Quan", "Thống kê hoạt động của hệ thống"), "growx");

        JPanel statsGrid = new JPanel(new MigLayout("insets 0, gap 16, fillx", "[grow][grow]"));
        statsGrid.setOpaque(false);
        
        lblSessions = new JLabel("—");
        lblCommission = new JLabel("—");
        
        StatCardPanel card1 = new StatCardPanel("wrap 1, insets 16 24", UITheme.ACCENT);
        card1.add(new JLabel("Tổng Phiên Đấu Giá"), "wrap");
        lblSessions.setFont(UITheme.fontBold(28));
        lblSessions.setForeground(UITheme.ACCENT);
        card1.add(lblSessions);
        
        StatCardPanel card2 = new StatCardPanel("wrap 1, insets 16 24", UITheme.SUCCESS);
        card2.add(new JLabel("Doanh Thu (VND)"), "wrap");
        lblCommission.setFont(UITheme.fontBold(28));
        lblCommission.setForeground(UITheme.SUCCESS);
        card2.add(lblCommission);
        
        statsGrid.add(card1, "growx");
        statsGrid.add(card2, "growx");
        
        panel.add(statsGrid, "growx");
        
        JPanel chartTop = new JPanel(new MigLayout("insets 0", "[grow][]"));
        chartTop.setOpaque(false);
        chartTop.add(new JLabel("Biểu Đồ Doanh Thu"), "growx");
        
        cbFilter = new JComboBox<>(new String[]{"Theo Ngày", "Theo Tuần", "Theo Tháng", "Theo Năm"});
        cbFilter.addActionListener(e -> fetchFilteredData());
        chartTop.add(cbFilter);
        
        panel.add(chartTop, "gapy 16, growx");
        chartPanel = new RevenueChartPanel();
        panel.add(chartPanel, "grow, push");
        
        return panel;
    }

    private JPanel buildModsPanel() {
        JPanel panel = UITheme.createCardPanel("wrap 1, insets 24, gap 16, fill");
        panel.add(buildPageHeader("Quản Lý Moderator", "Danh sách quản trị viên trung gian"), "growx");
        
        JPanel top = new JPanel(new MigLayout("insets 0", "[grow][]"));
        top.setOpaque(false);
        top.add(new JLabel("Danh sách:"), "growx");

        JButton btnAddMod = UITheme.primaryBtn("Thêm Moderator");
        btnAddMod.addActionListener(e -> {
            new ModFormDialog(mainFrame, false, null, null, null, null).setVisible(true);
        });
        top.add(btnAddMod);
        panel.add(top, "growx");

        modModel = new DefaultTableModel(
            new Object[]{"ID", "Tên Đăng Nhập", "Email", "SĐT", "Trạng Thái", "Thao Tác"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblMods = buildTable(modModel);

        // Cột Trạng Thái (col 4) – StatusBadgeCellRenderer
        tblMods.getColumnModel().getColumn(4).setCellRenderer(new StatusBadgeCellRenderer());
        tblMods.getColumnModel().getColumn(4).setPreferredWidth(110);
        tblMods.getColumnModel().getColumn(4).setMaxWidth(140);

        // Cột Thao Tác (col 5) – ActionButtonsCellRenderer
        tblMods.getColumnModel().getColumn(5).setCellRenderer(new ActionButtonsCellRenderer());
        tblMods.getColumnModel().getColumn(5).setPreferredWidth(220);
        tblMods.getColumnModel().getColumn(5).setMinWidth(200);

        tblMods.setRowHeight(44);

        // MouseListener: detect click vào cột 5 – Thao Tác
        tblMods.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = tblMods.rowAtPoint(e.getPoint());
                int col = tblMods.columnAtPoint(e.getPoint());
                if (row < 0 || col != 5) return;

                Rectangle rect = tblMods.getCellRect(row, col, false);
                int mouseX = e.getX() - rect.x;

                ActionButtonsCellRenderer renderer = (ActionButtonsCellRenderer) tblMods.getCellRenderer(row, col);
                ActionButtonsPanel actionPanel = renderer.panel;
                String status = tblMods.getValueAt(row, 4).toString();
                actionPanel.refresh(status);
                actionPanel.setBounds(0, 0, rect.width, rect.height);
                actionPanel.doLayout();

                Component hit = actionPanel.getComponentAt(mouseX, e.getY() - rect.y);
                String id = tblMods.getValueAt(row, 0).toString();

                if (hit == actionPanel.btnApprove && actionPanel.btnApprove.isVisible()) {
                    approveMod(id);
                } else if (hit == actionPanel.btnEdit) {
                    editMod(id,
                        tblMods.getValueAt(row, 1).toString(),
                        tblMods.getValueAt(row, 2).toString(),
                        tblMods.getValueAt(row, 3).toString());
                } else if (hit == actionPanel.btnToggle) {
                    boolean isBanned = "ĐÃ KHÓA".equals(status);
                    toggleBanMod(id, isBanned);
                }
            }
        });

        panel.add(UITheme.styledScrollPane(tblMods), "grow, push");
        return panel;
    }

    private JPanel buildUsersPanel() {
        JPanel panel = UITheme.createCardPanel("wrap 1, insets 24, gap 16, fill");
        panel.add(buildPageHeader("Quản Lý Người Dùng", "Toàn bộ tài khoản trong hệ thống"), "growx");
        
        userModel = new DefaultTableModel(new Object[]{"ID", "Tên Đăng Nhập", "Vai Trò", "Số Dư (VND)", "Trạng Thái"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblUsers = buildTable(userModel);
        panel.add(UITheme.styledScrollPane(tblUsers), "grow, push");
        return panel;
    }

    private JPanel buildRoomsPanel() {
        JPanel panel = UITheme.createCardPanel("wrap 1, insets 24, gap 16, fill");
        panel.add(buildPageHeader("Phòng Đấu Giá", "Theo dõi tất cả các phòng"), "growx");
        
        roomModel = new DefaultTableModel(new Object[]{"Mã Phòng", "Moderator", "Trạng Thái", "Bắt Đầu", "Kết Thúc"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblRooms = buildTable(roomModel);
        
        tblRooms.getColumnModel().getColumn(3).setPreferredWidth(160);
        tblRooms.getColumnModel().getColumn(4).setPreferredWidth(160);
        
        panel.add(UITheme.styledScrollPane(tblRooms), "grow, push");
        return panel;
    }

    private JPanel buildPenaltiesPanel() {
        JPanel panel = UITheme.createCardPanel("wrap 1, insets 24, gap 16, fill");
        panel.add(buildPageHeader("Lịch Sử Phạt", "Các khoản phạt đã được ghi nhận"), "growx");
        
        penaltyModel = new DefaultTableModel(new Object[]{"ID Phạt", "Người Dùng", "Lý Do", "Số Tiền (VND)", "Ngày Phạt"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblPenalties = buildTable(penaltyModel);
        panel.add(UITheme.styledScrollPane(tblPenalties), "grow, push");
        return panel;
    }

    private JPanel buildPageHeader(String title, String sub) {
        return UITheme.sectionHeader(title, sub);
    }

    private JTable buildTable(DefaultTableModel model) {
        return UITheme.styledTable(model);
    }

    private void fetchData() {
        fetchFilteredData();
        NetworkClient.getInstance().sendMessage(MessageType.GET_MOD_LIST, null);
        NetworkClient.getInstance().sendMessage(MessageType.GET_USER_LIST, null);
        NetworkClient.getInstance().sendMessage(MessageType.GET_ROOM_LIST, null);
        NetworkClient.getInstance().sendMessage(MessageType.GET_PENALTY_LIST, null);
    }

    private void approveMod(String userId) {
        Map<String, String> data = new HashMap<>();
        data.put("userId", userId);
        NetworkClient.getInstance().sendMessage(MessageType.APPROVE_MOD, data);
    }

    private void toggleBanMod(String userId, boolean currentBanStatus) {
        Map<String, String> data = new HashMap<>();
        data.put("userId", userId);
        data.put("isBanned", String.valueOf(!currentBanStatus));
        NetworkClient.getInstance().sendMessage(MessageType.TOGGLE_BAN_USER, data);
    }

    private void editMod(String idStr, String username, String email, String phone) {
        new ModFormDialog(mainFrame, true, idStr, username, email, phone).setVisible(true);
    }
    
    private void fetchFilteredData() {
        String filter = "DAY";
        if (cbFilter != null) {
            String sel = (String) cbFilter.getSelectedItem();
            if ("Theo Tuần".equals(sel)) filter = "WEEK";
            else if ("Theo Tháng".equals(sel)) filter = "MONTH";
            else if ("Theo Năm".equals(sel)) filter = "YEAR";
        }
        Map<String, String> data = new HashMap<>();
        data.put("filter", filter);
        NetworkClient.getInstance().sendMessage(MessageType.GET_ADMIN_STATS, data);
    }

    @Override
    public void onMessage(MessageType type, Map<String, String> data) {
        SwingUtilities.invokeLater(() -> {
            if (type == MessageType.GET_ADMIN_STATS) {
                if (lblSessions != null) lblSessions.setText(data.getOrDefault("totalSessions", "0"));
                if (lblCommission != null) lblCommission.setText(formatMoney(data.getOrDefault("totalCommission", "0")));
                
                if (chartPanel != null) {
                    chartPanel.setData(data.get("revenueData"));
                }
                
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
            } else if (type == MessageType.GET_MOD_LIST) {
                handleModListData(data.get("mods"));
            } else if (type == MessageType.GET_PENALTY_LIST) {
                penaltyModel.setRowCount(0);
                String p = data.get("penalties");
                if (p != null && !p.isEmpty()) {
                    for (String row : p.split("\\|")) penaltyModel.addRow(row.split(",", -1));
                }
            } else if (type == MessageType.ROOM_STATUS_UPDATE) {
                // Nhận tín hiệu server báo trạng thái phòng thay đổi → tải lại dữ liệu tự động
                if ("ROOM_STATUS_CHANGED".equals(data.get("trigger"))) {
                    fetchFilteredData();
                }
            }
        });
    }

    private void handleModListData(String mods) {
        modModel.setRowCount(0);
        if (mods == null || mods.isEmpty()) return;
        for (String row : mods.split("\\|")) {
            String[] parts = row.split(",", -1);
            // parts: id, username, email, phone, status
            // We add a 6th dummy column for the action buttons renderer
            String[] rowData = new String[6];
            for (int i = 0; i < 5 && i < parts.length; i++) rowData[i] = parts[i];
            rowData[5] = parts.length > 4 ? parts[4] : ""; // duplicate status into actions col
            modModel.addRow(rowData);
        }
    }

    private String formatMoney(String raw) {
        try { 
            return String.format("%,d", Long.parseLong(raw.trim())).replace(',', '.'); 
        } catch (Exception e) { 
            return raw; 
        }
    }

    class RevenueChartPanel extends JPanel {
        private String[] labels = new String[0];
        private int[] values = new int[0];
        private int maxValue = 1;

        public RevenueChartPanel() {
            setOpaque(false);
        }

        public void setData(String dataString) {
            if (dataString == null || dataString.isEmpty()) return;
            String[] parts = dataString.split("\\|");
            labels = new String[parts.length];
            values = new int[parts.length];
            maxValue = 1;
            for (int i = 0; i < parts.length; i++) {
                String[] kv = parts[i].split(":");
                if (kv.length == 2) {
                    labels[i] = kv[0];
                    try {
                        values[i] = Integer.parseInt(kv[1]);
                        if (values[i] > maxValue) maxValue = values[i];
                    } catch (Exception e) {
                        values[i] = 0;
                    }
                }
            }
            if (maxValue == 0) maxValue = 1; // Prevent division by zero
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (labels.length == 0) return;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int padding = 40;
            int chartWidth = width - 2 * padding;
            int chartHeight = height - 2 * padding;

            // Vẽ trục
            g2.setColor(UITheme.BORDER);
            g2.drawLine(padding, padding, padding, height - padding);
            g2.drawLine(padding, height - padding, width - padding, height - padding);

            int barCount = labels.length;
            int maxBarWidth = 50;
            int spacing = chartWidth / barCount;
            int barWidth = Math.min(maxBarWidth, spacing - 10);
            if (barWidth < 5) barWidth = 5;

            for (int i = 0; i < barCount; i++) {
                int barHeight = (int) (((double) values[i] / maxValue) * chartHeight);
                int x = padding + i * spacing + (spacing - barWidth) / 2;
                int y = height - padding - barHeight;

                // Vẽ bar
                g2.setColor(UITheme.ACCENT);
                g2.fillRoundRect(x, y, barWidth, barHeight, 8, 8);
                if (barHeight > 4) {
                    g2.fillRect(x, y + barHeight - 4, barWidth, 4);
                }

                // Vẽ label trục X
                g2.setColor(UITheme.TEXT_MUTED);
                g2.setFont(UITheme.fontBody(12));
                FontMetrics fm = g2.getFontMetrics();
                int labelWidth = fm.stringWidth(labels[i]);
                g2.drawString(labels[i], x + (barWidth - labelWidth) / 2, height - padding + 20);

                // Vẽ giá trị trên cột
                String valStr = String.valueOf(values[i]);
                int valWidth = fm.stringWidth(valStr);
                g2.setColor(UITheme.TEXT_PRIMARY);
                g2.drawString(valStr, x + (barWidth - valWidth) / 2, y - 5);
            }
            g2.dispose();
        }
    }

    class StatCardPanel extends JPanel {
        private Color accentColor;

        public StatCardPanel(String constraints, Color accentColor) {
            super(new MigLayout(constraints));
            this.accentColor = accentColor;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Nền card
            g2.setColor(UITheme.BG_CARD);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);

            // Viền ngoài
            g2.setColor(UITheme.BORDER_LIGHT);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 16, 16);

            // Dải màu nhấn bên trái
            g2.setColor(accentColor);
            g2.fillRoundRect(1, 1, 6, getHeight() - 2, 16, 16);
            g2.fillRect(6, 1, 4, getHeight() - 2); // che bớt góc bo bên phải của dải màu

            super.paintComponent(g);
            g2.dispose();
        }
    }

    // ====================================================================
    //  STATUS BADGE CELL RENDERER (col 4)
    // ====================================================================
    class StatusBadgeCellRenderer extends javax.swing.table.DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            String status = value != null ? value.toString() : "";
            JLabel lbl = new JLabel(status, SwingConstants.CENTER);
            lbl.setFont(UITheme.fontBold(12));
            lbl.setOpaque(true);

            Color bg, fg;
            if ("CHỜ DUYỆT".equals(status)) {
                bg = new Color(245, 158, 11, 30);
                fg = UITheme.AMBER;
            } else if ("ĐÃ DUYỆT".equals(status)) {
                bg = new Color(16, 185, 129, 30);
                fg = UITheme.SUCCESS;
            } else {
                bg = new Color(239, 68, 68, 30);
                fg = UITheme.DANGER;
            }

            if (isSelected) {
                lbl.setBackground(table.getSelectionBackground());
                lbl.setForeground(table.getSelectionForeground());
            } else {
                lbl.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
                lbl.setForeground(fg);

                // Pill badge effect
                lbl = new JLabel(status, SwingConstants.CENTER) {
                    @Override protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(bg);
                        g2.fillRoundRect(4, 6, getWidth()-8, getHeight()-12, 20, 20);
                        g2.setColor(fg);
                        g2.setStroke(new BasicStroke(1f));
                        g2.drawRoundRect(4, 6, getWidth()-9, getHeight()-13, 20, 20);
                        super.paintComponent(g);
                        g2.dispose();
                    }
                };
                lbl.setFont(UITheme.fontBold(12));
                lbl.setForeground(fg);
                lbl.setOpaque(false);
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                lbl.setBackground(table.getBackground());
            }

            return lbl;
        }
    }

    // ====================================================================
    //  ACTION BUTTONS PANEL & RENDERER (col 5)
    // ====================================================================
    class ActionButtonsPanel extends JPanel {
        JButton btnApprove;
        JButton btnEdit;
        JButton btnToggle;

        // Colors
        private static final Color BTN_APPROVE_BG  = new Color(16,  185, 129);
        private static final Color BTN_APPROVE_FG  = Color.WHITE;
        private static final Color BTN_EDIT_BG     = new Color(59,  130, 246); // Blue
        private static final Color BTN_EDIT_FG     = Color.WHITE;
        private static final Color BTN_LOCK_BG     = new Color(239, 68,  68);  // Red
        private static final Color BTN_LOCK_FG     = Color.WHITE;
        private static final Color BTN_UNLOCK_BG   = new Color(107, 114, 128); // Gray
        private static final Color BTN_UNLOCK_FG   = Color.WHITE;

        public ActionButtonsPanel() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 4, 6));
            setOpaque(true);

            btnApprove = makeColorBtn("✅ Duyệt",  BTN_APPROVE_BG, BTN_APPROVE_FG);
            btnEdit    = makeColorBtn("📝 Sửa",    BTN_EDIT_BG,    BTN_EDIT_FG);
            btnToggle  = makeColorBtn("❌ Khóa",    BTN_LOCK_BG,    BTN_LOCK_FG);

            add(btnApprove);
            add(btnEdit);
            add(btnToggle);
        }

        private JButton makeColorBtn(String text, Color bg, Color fg) {
            JButton btn = new JButton(text) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    Color c = getModel().isRollover() ? bg.darker() : bg;
                    g2.setColor(c);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    super.paintComponent(g);
                    g2.dispose();
                }
            };
            btn.setFont(UITheme.fontBold(11));
            btn.setForeground(fg);
            btn.setOpaque(false);
            btn.setContentAreaFilled(false);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setPreferredSize(new Dimension(70, 28));
            return btn;
        }

        public void refresh(String status) {
            setBackground(UITheme.BG_CARD);
            btnApprove.setVisible("CHỜ DUYỆT".equals(status));
            if ("ĐÃ KHÓA".equals(status)) {
                btnToggle.setText("↺ Mở");
                recolorBtn(btnToggle, BTN_UNLOCK_BG, BTN_UNLOCK_FG);
            } else {
                btnToggle.setText("■ Khóa");
                recolorBtn(btnToggle, BTN_LOCK_BG, BTN_LOCK_FG);
            }
        }

        private void recolorBtn(JButton btn, Color bg, Color fg) {
            btn.putClientProperty("__bg", bg);
            btn.setForeground(fg);
        }
    }

    class ActionButtonsCellRenderer extends javax.swing.table.DefaultTableCellRenderer {
        ActionButtonsPanel panel = new ActionButtonsPanel();

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            // value in col 5 is the status string (duplicated for convenience)
            String status = value != null ? value.toString() : "";
            panel.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            panel.refresh(status);
            return panel;
        }
    }

    // Legacy – kept to avoid compile errors if referenced elsewhere
    class ModActionPanel extends JPanel {
        JLabel lblStatus = new JLabel();
        JButton btnApprove = new JButton("✔ Duyệt");
        JButton btnEdit    = new JButton("✎ Sửa");
        JButton btnToggle  = new JButton("■ Khóa");
        public ModActionPanel() { setOpaque(true); }
        public void updateData(String s, boolean sel, JTable t) {}
    }
    class ModActionCellRenderer extends javax.swing.table.DefaultTableCellRenderer {
        ModActionPanel panel = new ModActionPanel();
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            return panel;
        }
    }
}
