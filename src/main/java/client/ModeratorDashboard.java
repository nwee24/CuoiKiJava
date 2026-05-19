package client;

import shared.MessageType;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.Map;

public class ModeratorDashboard extends JPanel implements NetworkClient.MessageListener {
    private MainFrame mainFrame;

    // Colors (shared with UserDashboard theme)
    private static final Color BG_DARK       = new Color(15, 23, 42);
    private static final Color BG_CARD       = new Color(30, 41, 59);
    private static final Color BG_SIDEBAR    = new Color(15, 23, 42);
    private static final Color ACCENT        = new Color(99, 102, 241);
    private static final Color ACCENT_LIGHT  = new Color(129, 140, 248);
    private static final Color TEXT_PRIMARY  = new Color(248, 250, 252);
    private static final Color TEXT_MUTED    = new Color(148, 163, 184);
    private static final Color SUCCESS       = new Color(52, 211, 153);
    private static final Color DANGER        = new Color(248, 113, 113);
    private static final Color BORDER        = new Color(51, 65, 85);
    private static final Color AMBER         = new Color(251, 191, 36);

    private String activeTab = "REQUESTS";
    private JPanel contentArea;
    private Timer refreshTimer;

    // Tab 1 – Product Requests
    private DefaultListModel<String> requestModel;
    private JList<String> listRequests;

    // Tab 2 – Room Management
    private DefaultTableModel roomModel;
    private JTable tblMyRooms;

    // Tab 3 – Chat
    private ChatPanel modChatPanel;
    private DefaultListModel<String> userListModel;
    private JList<String> listUsers;

    // Nav buttons
    private JButton btnNavRequests, btnNavRooms, btnNavChat;

    // Notification Center
    private NotificationCenter notifCenter;

    public ModeratorDashboard(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());
        setBackground(BG_DARK);

        add(buildHeader(),  BorderLayout.NORTH);
        add(buildSidebar(), BorderLayout.WEST);

        contentArea = new JPanel(new CardLayout());
        contentArea.setBackground(BG_DARK);
        contentArea.setBorder(new EmptyBorder(20, 20, 20, 20));
        contentArea.add(buildRequestsPanel(), "REQUESTS");
        contentArea.add(buildRoomsPanel(),    "ROOMS");
        contentArea.add(buildChatPanel(),     "CHAT");
        add(contentArea, BorderLayout.CENTER);

        NetworkClient.getInstance().addListener(this);
        NetworkClient.getInstance().sendMessage(MessageType.GET_ROOM_LIST, null);
        NetworkClient.getInstance().sendMessage(MessageType.GET_USER_LIST, null);

        // Auto-refresh mọi 5 giây
        refreshTimer = new Timer(5000, e -> {
            NetworkClient.getInstance().sendMessage(MessageType.GET_ROOM_LIST, null);
            NetworkClient.getInstance().sendMessage(MessageType.GET_USER_LIST, null);
        });
        refreshTimer.start();

        // NotificationCenter lắng nghe riêng (đã addListener trong constructor của nó)
        // Khi seller gửi CONTACT_MOD, notifCenter sẽ tự nhận và hiện badge

        showTab("REQUESTS");
    }

    // ============================================================
    //  HEADER
    // ============================================================
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_CARD);
        header.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, BORDER),
            new EmptyBorder(12, 24, 12, 24)
        ));

        JLabel lblApp = new JLabel("AuctionPro");
        lblApp.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblApp.setForeground(ACCENT_LIGHT);
        header.add(lblApp, BorderLayout.WEST);

        // Right side: NotifCenter + Badge + Username
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightPanel.setBackground(BG_CARD);

        // Notification Center bell button
        notifCenter = new NotificationCenter();
        notifCenter.setOpaque(false);
        // Khi click vào item thông báo → chuyển sang tab Requests
        notifCenter.setClickListener(item -> showTab("REQUESTS"));
        rightPanel.add(notifCenter);

        JLabel badge = new JLabel("  MODERATOR  ");
        badge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        badge.setForeground(AMBER);
        badge.setBackground(new Color(92, 64, 10));
        badge.setOpaque(true);
        badge.setBorder(new EmptyBorder(3, 8, 3, 8));
        rightPanel.add(badge);

        String username = NetworkClient.getInstance().getCurrentUsername();
        JLabel lblUser = new JLabel(username != null ? username : "Moderator");
        lblUser.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblUser.setForeground(TEXT_MUTED);
        rightPanel.add(lblUser);

        header.add(rightPanel, BorderLayout.EAST);
        return header;
    }

    // ============================================================
    //  SIDEBAR
    // ============================================================
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(BG_SIDEBAR);
        sidebar.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 0, 1, BORDER),
            new EmptyBorder(20, 10, 20, 10)
        ));
        sidebar.setPreferredSize(new Dimension(210, 0));

        JLabel lblMenu = new JLabel("  QUẢN LÝ");
        lblMenu.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblMenu.setForeground(TEXT_MUTED);
        lblMenu.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblMenu.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        sidebar.add(lblMenu);
        sidebar.add(Box.createVerticalStrut(8));

        btnNavRequests = createNavBtn(">> Yeu Cau Tu Seller", "REQUESTS");
        btnNavRooms    = createNavBtn(">> Quan Ly Phong",     "ROOMS");
        btnNavChat     = createNavBtn(">> Chat Voi Seller",   "CHAT");

        sidebar.add(btnNavRequests);
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(btnNavRooms);
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(btnNavChat);
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
        ((CardLayout) contentArea.getLayout()).show(contentArea, tabKey);
        for (JButton b : new JButton[]{btnNavRequests, btnNavRooms, btnNavChat}) {
            b.setBackground(BG_SIDEBAR);
            b.setForeground(TEXT_MUTED);
            b.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        }
        JButton active = tabKey.equals("REQUESTS") ? btnNavRequests
                       : tabKey.equals("ROOMS")    ? btnNavRooms
                       : btnNavChat;
        active.setBackground(new Color(49, 46, 129));
        active.setForeground(ACCENT_LIGHT);
        active.setFont(new Font("Segoe UI", Font.BOLD, 14));
    }

    // ============================================================
    //  REQUESTS PANEL
    // ============================================================
    private JPanel buildRequestsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(BG_DARK);

        // Title
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setBackground(BG_DARK);
        JLabel lbl = new JLabel("Yêu Cầu Từ Seller");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lbl.setForeground(TEXT_PRIMARY);
        titleRow.add(lbl, BorderLayout.WEST);

        JLabel hint = new JLabel("Double-click để xem chi tiết");
        hint.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        hint.setForeground(TEXT_MUTED);
        titleRow.add(hint, BorderLayout.EAST);
        panel.add(titleRow, BorderLayout.NORTH);

        // List
        requestModel = new DefaultListModel<>();
        listRequests = new JList<>(requestModel);
        listRequests.setBackground(BG_CARD);
        listRequests.setForeground(TEXT_PRIMARY);
        listRequests.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        listRequests.setFixedCellHeight(52);
        listRequests.setBorder(new EmptyBorder(4, 10, 4, 10));
        listRequests.setSelectionBackground(new Color(49, 46, 129));
        listRequests.setSelectionForeground(TEXT_PRIMARY);
        listRequests.setCellRenderer(new RequestCellRenderer());

        requestModel.addElement("(Chưa có yêu cầu nào)");

        JScrollPane sp = buildScrollPane(listRequests);
        panel.add(sp, BorderLayout.CENTER);

        // Action buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        btnPanel.setBackground(BG_DARK);

        JButton btnReject = new JButton("Tu Choi");
        btnReject.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnReject.setForeground(DANGER);
        btnReject.setBackground(new Color(60, 20, 20));
        btnReject.setBorderPainted(false);
        btnReject.setFocusPainted(false);
        btnReject.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnReject.setBorder(new EmptyBorder(9, 20, 9, 20));
        btnReject.addActionListener(e -> {
            int idx = listRequests.getSelectedIndex();
            if (idx >= 0 && requestModel.getSize() > 0 && !requestModel.get(0).startsWith("(")) {
                requestModel.remove(idx);
                if (requestModel.isEmpty()) requestModel.addElement("(Chưa có yêu cầu nào)");
            }
        });

        JButton btnApprove = new JButton("Duyet Yeu Cau");
        btnApprove.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnApprove.setForeground(new Color(15, 23, 42));
        btnApprove.setBackground(SUCCESS);
        btnApprove.setBorderPainted(false);
        btnApprove.setFocusPainted(false);
        btnApprove.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnApprove.setBorder(new EmptyBorder(9, 20, 9, 20));
        btnApprove.addActionListener(e -> {
            int idx = listRequests.getSelectedIndex();
            if (idx >= 0 && requestModel.getSize() > 0 && !requestModel.get(0).startsWith("(")) {
                String item = requestModel.get(idx);
                showToast("Da duyet: " + item);
                requestModel.remove(idx);
                if (requestModel.isEmpty()) requestModel.addElement("(Chưa có yêu cầu nào)");
            } else {
                showToast("Vui lòng chọn một yêu cầu để duyệt.");
            }
        });

        btnPanel.add(btnReject);
        btnPanel.add(btnApprove);
        panel.add(btnPanel, BorderLayout.SOUTH);

        return panel;
    }

    // ============================================================
    //  ROOMS PANEL (updated: uses CreateRoomDialog)
    // ============================================================
    private JPanel buildRoomsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(BG_DARK);

        JLabel lbl = new JLabel("Quản Lý Phòng Đấu Giá");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lbl.setForeground(TEXT_PRIMARY);
        panel.add(lbl, BorderLayout.NORTH);

        // Table
        roomModel = new DefaultTableModel(new Object[]{"Mã Phòng", "Tiêu Đề", "Sellers", "Trạng Thái"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblMyRooms = buildStyledTable(roomModel);
        panel.add(buildScrollPane(tblMyRooms), BorderLayout.CENTER);

        // Bottom control bar
        JPanel bottomBar = new JPanel(new BorderLayout(12, 0));
        bottomBar.setBackground(new Color(22, 32, 48));
        bottomBar.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(1, 0, 0, 0, BORDER),
            new EmptyBorder(12, 0, 4, 0)
        ));

        // LEFT: Tạo phòng mới (opens dialog)
        JPanel createRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        createRow.setBackground(new Color(22, 32, 48));

        JButton btnCreate = createPrimaryBtn("＋ Tạo Phòng Mới...");
        btnCreate.addActionListener(e -> openCreateRoomDialog());

        createRow.add(btnCreate);
        bottomBar.add(createRow, BorderLayout.WEST);

        // RIGHT: Vào phòng, bắt đầu đấu giá
        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionRow.setBackground(new Color(22, 32, 48));

        JButton btnOpenAuction = createAccentBtn("▶ Bắt Đầu Đấu Giá", AMBER, new Color(15, 23, 42));
        btnOpenAuction.addActionListener(e -> {
            int row = tblMyRooms.getSelectedRow();
            if (row >= 0) {
                String rid = (String) roomModel.getValueAt(row, 0);
                NetworkClient.getInstance().sendMessage(MessageType.OPEN_AUCTION, Map.of("roomId", rid));
            } else {
                showToast("Vui lòng chọn phòng để bắt đầu.");
            }
        });

        JButton btnViewRoom = createPrimaryBtn("🚪 Vào Phòng Điều Hành");
        btnViewRoom.addActionListener(e -> {
            int row = tblMyRooms.getSelectedRow();
            if (row >= 0) {
                String rid = (String) roomModel.getValueAt(row, 0);
                AuctionRoomPanel roomPanel = new AuctionRoomPanel();
                roomPanel.initRoom(rid, true);
                mainFrame.addPanel(roomPanel, "MOD_ROOM_" + rid);
                mainFrame.switchPanel("MOD_ROOM_" + rid);
            } else {
                showToast("Vui lòng chọn phòng để vào.");
            }
        });

        actionRow.add(btnOpenAuction);
        actionRow.add(btnViewRoom);
        bottomBar.add(actionRow, BorderLayout.EAST);

        panel.add(bottomBar, BorderLayout.SOUTH);
        return panel;
    }

    /** Mở CreateRoomDialog */
    private void openCreateRoomDialog() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        CreateRoomDialog dialog = new CreateRoomDialog(owner);
        dialog.setVisible(true);
        // Dialog là modal, khi đóng kiểm tra kết quả
        if (dialog.isConfirmed()) {
            // Đã gửi CREATE_ROOM_WITH_SELLERS trong dialog.onConfirm()
            // Refresh danh sách phòng
            NetworkClient.getInstance().sendMessage(MessageType.GET_ROOM_LIST, null);
        }
    }

    // ============================================================
    //  CHAT PANEL (Mod side)
    // ============================================================
    private JPanel buildChatPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(BG_DARK);

        JLabel lbl = new JLabel("Lien He Nguoi Dung");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lbl.setForeground(TEXT_PRIMARY);
        panel.add(lbl, BorderLayout.NORTH);

        // SplitPane: Trái = Danh sách user, Phải = Chat
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(250);
        splitPane.setDividerSize(4);
        splitPane.setBorder(null);
        splitPane.setBackground(BG_DARK);

        // --- Left: User List ---
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBackground(BG_CARD);
        leftPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER));

        JLabel lblList = new JLabel(" Danh bạ người dùng");
        lblList.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblList.setForeground(TEXT_MUTED);
        lblList.setBorder(new EmptyBorder(10, 10, 10, 10));
        leftPanel.add(lblList, BorderLayout.NORTH);

        userListModel = new DefaultListModel<>();
        listUsers = new JList<>(userListModel);
        listUsers.setBackground(BG_CARD);
        listUsers.setSelectionBackground(new Color(49, 46, 129));
        listUsers.setSelectionForeground(TEXT_PRIMARY);
        listUsers.setCellRenderer(new RequestCellRenderer()); // Reuse renderer
        listUsers.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = listUsers.getSelectedValue();
                if (selected != null) {
                    modChatPanel.setPrivateMode(selected);
                }
            }
        });

        JScrollPane scrollUsers = new JScrollPane(listUsers);
        scrollUsers.setBorder(null);
        scrollUsers.getViewport().setBackground(BG_CARD);
        leftPanel.add(scrollUsers, BorderLayout.CENTER);
        splitPane.setLeftComponent(leftPanel);

        // --- Right: Chat Panel ---
        modChatPanel = new ChatPanel();
        splitPane.setRightComponent(modChatPanel);

        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }

    // ============================================================
    //  UI Helpers
    // ============================================================
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

        DefaultTableCellRenderer r = new DefaultTableCellRenderer();
        r.setBackground(BG_CARD);
        r.setForeground(TEXT_PRIMARY);
        for (int i = 0; i < model.getColumnCount(); i++)
            table.getColumnModel().getColumn(i).setCellRenderer(r);

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
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setBackground(ACCENT);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(9, 20, 9, 20));
        return btn;
    }

    private JButton createAccentBtn(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(fg);
        btn.setBackground(bg);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(9, 20, 9, 20));
        return btn;
    }

    private void showToast(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Thông báo", JOptionPane.INFORMATION_MESSAGE);
    }

    // ============================================================
    //  MESSAGE HANDLER
    // ============================================================
    @Override
    public void onMessage(MessageType type, Map<String, String> data) {
        SwingUtilities.invokeLater(() -> {
            if (type == MessageType.SUCCESS && data.containsKey("roomId") && !data.containsKey("status")) {
                showToast("✅ Phòng '" + data.get("roomId") + "' đã được tạo thành công!");
                NetworkClient.getInstance().sendMessage(MessageType.GET_ROOM_LIST, null);

            } else if (type == MessageType.ROOM_INFO) {
                String roomsStr = data.get("myRoomList");
                if (roomsStr != null) {
                    roomModel.setRowCount(0);
                    if (!roomsStr.isEmpty()) {
                        for (String r : roomsStr.split("\\|")) {
                            String[] parts = r.split(",", -1);
                            String id = parts.length > 0 ? parts[0] : "";
                            String title = parts.length > 1 ? parts[1] : "Phiên Đấu Giá";
                            String sellers = parts.length > 2 ? parts[2] : "";
                            String status = parts.length > 3 ? parts[3] : "ACTIVE";
                            roomModel.addRow(new Object[]{id, title, sellers, status});
                        }
                    }
                }

            } else if (type == MessageType.GET_USER_LIST) {
                String usersStr = data.get("userList");
                if (usersStr != null && userListModel != null) {
                    // Cập nhật danh sách nếu có thay đổi để tránh flicker
                    String[] newUsers = usersStr.isEmpty() ? new String[0] : usersStr.split(",");
                    if (userListModel.getSize() != newUsers.length) {
                        userListModel.clear();
                        for (String u : newUsers) userListModel.addElement(u);
                    } else {
                        boolean changed = false;
                        for (int i = 0; i < newUsers.length; i++) {
                            if (!newUsers[i].equals(userListModel.get(i))) {
                                changed = true;
                                break;
                            }
                        }
                        if (changed) {
                            userListModel.clear();
                            for (String u : newUsers) userListModel.addElement(u);
                        }
                    }
                }

            } else if (type == MessageType.CONTACT_MOD) {
                String product = "[YC] " + data.getOrDefault("productName", "?")
                    + "  |  Gia: " + data.getOrDefault("startingPrice", "?")
                    + "  |  Tu: " + data.getOrDefault("senderName", "?");
                if (requestModel.size() == 1 && requestModel.get(0).startsWith("(")) {
                    requestModel.clear();
                }
                requestModel.addElement(product);

            } else if (type == MessageType.ERROR && data.containsKey("message")) {
                JOptionPane.showMessageDialog(
                    ModeratorDashboard.this,
                    "❌ " + data.get("message"),
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        });
    }

    // ─── Request Cell Renderer ────────────────────────────────────────────────
    private static class RequestCellRenderer extends DefaultListCellRenderer {
        private static final Color BG   = new Color(30, 41, 59);
        private static final Color SEL  = new Color(49, 46, 129);
        private static final Color FG   = new Color(248, 250, 252);
        private static final Color MUTED= new Color(148, 163, 184);

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean hasFocus) {
            JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, hasFocus);
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            lbl.setForeground(isSelected ? FG : (value.toString().startsWith("(") ? MUTED : FG));
            lbl.setBackground(isSelected ? SEL : BG);
            lbl.setBorder(new EmptyBorder(8, 12, 8, 12));
            return lbl;
        }
    }
}
