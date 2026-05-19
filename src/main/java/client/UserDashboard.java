package client;

import shared.MessageType;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class UserDashboard extends JPanel implements NetworkClient.MessageListener {
    private MainFrame mainFrame;

    // Colors
    private static final Color BG_DARK       = new Color(15, 23, 42);   // Slate 900
    private static final Color BG_CARD       = new Color(30, 41, 59);   // Slate 800
    private static final Color BG_SIDEBAR    = new Color(15, 23, 42);   // Slate 900
    private static final Color ACCENT        = new Color(99, 102, 241); // Indigo 500
    private static final Color ACCENT_LIGHT  = new Color(129, 140, 248);// Indigo 400
    private static final Color TEXT_PRIMARY  = new Color(248, 250, 252);// Slate 50
    private static final Color TEXT_MUTED    = new Color(148, 163, 184);// Slate 400
    private static final Color SUCCESS       = new Color(52, 211, 153); // Emerald 400
    private static final Color DANGER        = new Color(248, 113, 113);// Red 400
    private static final Color BORDER        = new Color(51, 65, 85);   // Slate 700

    // State
    private String activeTab = "ROOMS";
    private JPanel contentArea;

    // Rooms Tab
    private JTable tblRooms;
    private DefaultTableModel roomModel;
    private JButton btnJoinRoom;
    private Timer refreshTimer;

    // Contact Tab
    private JList<String> listMods;
    private DefaultListModel<String> modModel;
    private ProductSubmitPanel submitPanel;
    private ChatPanel chatPanel;

    // History Tab
    private JTextArea txtHistory;

    // Nav buttons
    private JButton btnNavRooms, btnNavContact, btnNavHistory;

    public UserDashboard(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());
        setBackground(BG_DARK);

        // === TOP HEADER ===
        JPanel header = buildHeader();
        add(header, BorderLayout.NORTH);

        // === SIDEBAR ===
        JPanel sidebar = buildSidebar();
        add(sidebar, BorderLayout.WEST);

        // === CONTENT AREA ===
        contentArea = new JPanel(new CardLayout());
        contentArea.setBackground(BG_DARK);
        contentArea.setBorder(new EmptyBorder(20, 20, 20, 20));
        contentArea.add(buildRoomsPanel(), "ROOMS");
        contentArea.add(buildContactPanel(), "CONTACT");
        contentArea.add(buildHistoryPanel(), "HISTORY");
        add(contentArea, BorderLayout.CENTER);

        NetworkClient.getInstance().addListener(this);
        refreshTimer = new Timer(5000, e -> fetchRooms());
        refreshTimer.start();

        fetchRooms();
        fetchMods();
        fetchHistory();

        showTab("ROOMS");
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_CARD);
        header.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, BORDER),
            new EmptyBorder(14, 24, 14, 24)
        ));

        JLabel lblApp = new JLabel("AuctionPro");
        lblApp.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblApp.setForeground(ACCENT_LIGHT);
        header.add(lblApp, BorderLayout.WEST);

        String username = NetworkClient.getInstance().getCurrentUsername();
        JLabel lblUser = new JLabel((username != null ? username : "User") + "  |  Nguoi Dung");
        lblUser.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblUser.setForeground(TEXT_MUTED);
        header.add(lblUser, BorderLayout.EAST);

        return header;
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(BG_SIDEBAR);
        sidebar.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 0, 1, BORDER),
            new EmptyBorder(20, 10, 20, 10)
        ));
        sidebar.setPreferredSize(new Dimension(200, 0));

        JLabel lblMenu = new JLabel("  MENU");
        lblMenu.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblMenu.setForeground(TEXT_MUTED);
        lblMenu.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblMenu.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        sidebar.add(lblMenu);
        sidebar.add(Box.createVerticalStrut(8));

        btnNavRooms   = createNavBtn("Phong Dau Gia",       "ROOMS");
        btnNavContact = createNavBtn("Lien He Trung Gian",  "CONTACT");
        btnNavHistory = createNavBtn("Lich Su Giao Dich",   "HISTORY");

        sidebar.add(btnNavRooms);
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(btnNavContact);
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(btnNavHistory);
        sidebar.add(Box.createVerticalGlue());

        return sidebar;
    }

    private JButton createNavBtn(String text, String tabKey) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btn.setForeground(TEXT_MUTED);
        btn.setBackground(BG_SIDEBAR);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 12, 8, 12));

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (!activeTab.equals(tabKey)) {
                    btn.setBackground(new Color(30, 41, 59));
                    btn.setForeground(TEXT_PRIMARY);
                }
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (!activeTab.equals(tabKey)) {
                    btn.setBackground(BG_SIDEBAR);
                    btn.setForeground(TEXT_MUTED);
                }
            }
        });

        btn.addActionListener(e -> showTab(tabKey));
        return btn;
    }

    private void showTab(String tabKey) {
        activeTab = tabKey;
        CardLayout cl = (CardLayout) contentArea.getLayout();
        cl.show(contentArea, tabKey);

        // Reset all nav buttons
        for (JButton b : new JButton[]{btnNavRooms, btnNavContact, btnNavHistory}) {
            b.setBackground(BG_SIDEBAR);
            b.setForeground(TEXT_MUTED);
            b.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        }
        // Highlight active
        JButton active = tabKey.equals("ROOMS") ? btnNavRooms :
                         tabKey.equals("CONTACT") ? btnNavContact : btnNavHistory;
        active.setBackground(new Color(49, 46, 129)); // Indigo 900
        active.setForeground(ACCENT_LIGHT);
        active.setFont(new Font("Segoe UI", Font.BOLD, 14));
    }

    // ============================================================
    //  ROOMS PANEL
    // ============================================================
    private JPanel buildRoomsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(BG_DARK);

        // Title row
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setBackground(BG_DARK);
        JLabel lbl = new JLabel("Phòng Đấu Giá Đang Mở");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lbl.setForeground(TEXT_PRIMARY);
        titleRow.add(lbl, BorderLayout.WEST);

        JButton btnRefresh = createSecondaryBtn("⟳ Làm mới");
        btnRefresh.addActionListener(e -> fetchRooms());
        titleRow.add(btnRefresh, BorderLayout.EAST);
        panel.add(titleRow, BorderLayout.NORTH);

        // Table
        roomModel = new DefaultTableModel(new Object[]{"Mã Phòng", "Tiêu đề", "Moderator", "Trạng thái"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblRooms = buildStyledTable(roomModel);
        JScrollPane sp = buildScrollPane(tblRooms);
        panel.add(sp, BorderLayout.CENTER);

        // Bottom bar
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        bottom.setBackground(BG_DARK);
        btnJoinRoom = createPrimaryBtn("Tham Gia Phong");
        btnJoinRoom.addActionListener(e -> {
            int row = tblRooms.getSelectedRow();
            if (row >= 0) {
                String roomId = (String) roomModel.getValueAt(row, 0);
                Map<String, String> data = new HashMap<>();
                data.put("roomId", roomId);
                NetworkClient.getInstance().sendMessage(MessageType.JOIN_ROOM, data);
            } else {
                showToast("Vui lòng chọn một phòng để tham gia.");
            }
        });
        bottom.add(btnJoinRoom);
        panel.add(bottom, BorderLayout.SOUTH);

        return panel;
    }

    // ============================================================
    //  CONTACT PANEL
    // ============================================================
    private JPanel buildContactPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(BG_DARK);

        JLabel lbl = new JLabel("Liên Hệ Trung Gian");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lbl.setForeground(TEXT_PRIMARY);
        panel.add(lbl, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setBackground(BG_DARK);
        split.setBorder(null);
        split.setDividerSize(6);

        // LEFT: Mod list
        JPanel leftCard = buildCard("🟢 Moderator Online");
        modModel = new DefaultListModel<>();
        listMods = new JList<>(modModel);
        listMods.setBackground(BG_CARD);
        listMods.setForeground(TEXT_PRIMARY);
        listMods.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        listMods.setFixedCellHeight(36);
        listMods.setBorder(new EmptyBorder(4, 8, 4, 8));
        listMods.setSelectionBackground(new Color(49, 46, 129));
        listMods.setSelectionForeground(TEXT_PRIMARY);
        leftCard.add(new JScrollPane(listMods) {{
            setBorder(null);
            getViewport().setBackground(BG_CARD);
        }}, BorderLayout.CENTER);
        split.setLeftComponent(leftCard);

        // RIGHT: Submit + Chat
        JPanel rightPanel = new JPanel(new GridLayout(2, 1, 0, 12));
        rightPanel.setBackground(BG_DARK);

        submitPanel = new ProductSubmitPanel();
        styleInnerPanel(submitPanel, "📦 Gửi Thông Tin Sản Phẩm");
        rightPanel.add(submitPanel);

        chatPanel = new ChatPanel();
        styleInnerPanel(chatPanel, "💬 Trò Chuyện Trực Tiếp");
        rightPanel.add(chatPanel);

        split.setRightComponent(rightPanel);
        split.setDividerLocation(200);
        panel.add(split, BorderLayout.CENTER);

        listMods.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && listMods.getSelectedValue() != null) {
                String mod = listMods.getSelectedValue();
                submitPanel.setTargetMod(mod);
                chatPanel.setPrivateMode(mod);
            }
        });

        return panel;
    }

    // ============================================================
    //  HISTORY PANEL
    // ============================================================
    private JPanel buildHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(BG_DARK);

        JLabel lbl = new JLabel("Lịch Sử Giao Dịch");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lbl.setForeground(TEXT_PRIMARY);
        panel.add(lbl, BorderLayout.NORTH);

        txtHistory = new JTextArea("Đang tải dữ liệu lịch sử...\n");
        txtHistory.setEditable(false);
        txtHistory.setBackground(BG_CARD);
        txtHistory.setForeground(TEXT_PRIMARY);
        txtHistory.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtHistory.setMargin(new Insets(12, 16, 12, 16));
        txtHistory.setCaretColor(TEXT_PRIMARY);
        JScrollPane sp = buildScrollPane(txtHistory);
        panel.add(sp, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        bottom.setBackground(BG_DARK);
        JButton btnExport = createSecondaryBtn("⬇ Xuất XML Lịch Sử");
        btnExport.addActionListener(e -> NetworkClient.getInstance().sendMessage(MessageType.EXPORT_HISTORY, null));
        bottom.add(btnExport);
        panel.add(bottom, BorderLayout.SOUTH);

        return panel;
    }

    // ============================================================
    //  UI Helpers
    // ============================================================
    private JPanel buildCard(String title) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1, true),
            new EmptyBorder(0, 0, 0, 0)
        ));

        JLabel lblTitle = new JLabel("  " + title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTitle.setForeground(TEXT_MUTED);
        lblTitle.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, BORDER),
            new EmptyBorder(10, 8, 10, 8)
        ));
        lblTitle.setBackground(new Color(22, 32, 48));
        lblTitle.setOpaque(true);
        card.add(lblTitle, BorderLayout.NORTH);
        return card;
    }

    private void styleInnerPanel(JPanel panel, String title) {
        panel.setBackground(BG_CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1, true),
            new EmptyBorder(8, 8, 8, 8)
        ));
        // Add title label if possible
        if (panel.getLayout() instanceof BorderLayout) {
            JLabel lbl = new JLabel("  " + title);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
            lbl.setForeground(TEXT_MUTED);
            lbl.setBorder(new EmptyBorder(2, 0, 8, 0));
            panel.add(lbl, BorderLayout.NORTH);
        }
    }

    private JTable buildStyledTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setBackground(BG_CARD);
        table.setForeground(TEXT_PRIMARY);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setRowHeight(40);
        table.setGridColor(BORDER);
        table.setSelectionBackground(new Color(49, 46, 129));
        table.setSelectionForeground(TEXT_PRIMARY);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));

        JTableHeader header = table.getTableHeader();
        header.setBackground(new Color(22, 32, 48));
        header.setForeground(TEXT_MUTED);
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBorder(new MatteBorder(0, 0, 1, 0, BORDER));
        header.setReorderingAllowed(false);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.LEFT);
        centerRenderer.setBackground(BG_CARD);
        centerRenderer.setForeground(TEXT_PRIMARY);
        for (int i = 0; i < model.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        return table;
    }

    private JScrollPane buildScrollPane(JComponent comp) {
        JScrollPane sp = new JScrollPane(comp);
        sp.setBorder(BorderFactory.createLineBorder(BORDER, 1, true));
        sp.getViewport().setBackground(BG_CARD);
        sp.setBackground(BG_CARD);
        return sp;
    }

    private JButton createPrimaryBtn(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setForeground(Color.WHITE);
        btn.setBackground(ACCENT);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(10, 22, 10, 22));
        return btn;
    }

    private JButton createSecondaryBtn(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btn.setForeground(TEXT_MUTED);
        btn.setBackground(BG_CARD);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1, true),
            new EmptyBorder(7, 16, 7, 16)
        ));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void showToast(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Thông báo", JOptionPane.INFORMATION_MESSAGE);
    }

    // ============================================================
    //  Data Fetching
    // ============================================================
    private void fetchRooms()   { NetworkClient.getInstance().sendMessage(MessageType.GET_ROOM_LIST, null); }
    private void fetchMods()    { NetworkClient.getInstance().sendMessage(MessageType.GET_MOD_LIST, null); }
    private void fetchHistory() { NetworkClient.getInstance().sendMessage(MessageType.GET_HISTORY, null); }

    @Override
    public void onMessage(MessageType type, Map<String, String> data) {
        SwingUtilities.invokeLater(() -> {
            if (type == MessageType.SUCCESS && data.containsKey("roomId") && "JOINED".equals(data.get("status"))) {
                AuctionRoomPanel roomPanel = new AuctionRoomPanel();
                roomPanel.initRoom(data.get("roomId"), false);
                mainFrame.addPanel(roomPanel, "ROOM_" + data.get("roomId"));
                mainFrame.switchPanel("ROOM_" + data.get("roomId"));

            } else if (type == MessageType.INVITE_SELLER) {
                // Được mời vào phòng bởi Moderator
                String roomId  = data.getOrDefault("roomId", "?");
                String title   = data.getOrDefault("title", roomId);
                String modName = data.getOrDefault("modName", "Moderator");
                int choice = JOptionPane.showConfirmDialog(
                    UserDashboard.this,
                    "📨 Moderator " + modName + " mời bạn tham gia phòng đấu giá:\n"
                        + "  Phòng: " + title + " (" + roomId + ")\n\n"
                        + "Bạn có muốn vào phòng ngay không?",
                    "Lời Mời Tham Gia Phòng",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE
                );
                if (choice == JOptionPane.YES_OPTION) {
                    java.util.Map<String, String> req = new java.util.HashMap<>();
                    req.put("roomId", roomId);
                    NetworkClient.getInstance().sendMessage(MessageType.JOIN_ROOM, req);
                }

            } else if (type == MessageType.ROOM_INFO && data.containsKey("roomList")) {
                String roomsStr = data.get("roomList");
                roomModel.setRowCount(0);
                if (roomsStr != null && !roomsStr.isEmpty()) {
                    for (String r : roomsStr.split("\\|")) {
                        String[] parts = r.split(",", -1);
                        if (parts.length >= 4) roomModel.addRow(new Object[]{parts[0], parts[1], parts[2], parts[3]});
                    }
                }
            } else if (type == MessageType.GET_MOD_LIST) {
                String modsStr = data.get("modList");
                modModel.clear();
                if (modsStr != null && !modsStr.isEmpty()) {
                    for (String mod : modsStr.split(",")) modModel.addElement(mod);
                }
            } else if (type == MessageType.GET_HISTORY) {
                txtHistory.setText("--- LỊCH SỬ GIAO DỊCH CỦA BẠN ---\n\n");
                txtHistory.append(data.get("historyData"));
            } else if (type == MessageType.EXPORT_HISTORY) {
                String xmlData = data.get("xmlData");
                if (xmlData != null) {
                    try (java.io.PrintWriter out = new java.io.PrintWriter("history_export.xml")) {
                        out.println(xmlData);
                        showToast("Đã xuất file history_export.xml thành công!");
                    } catch (Exception ex) {
                        showToast("Lỗi lưu file: " + ex.getMessage());
                    }
                }
            }
        });
    }
}
