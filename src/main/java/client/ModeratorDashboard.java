package client;

import shared.MessageType;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import net.miginfocom.swing.MigLayout;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.swing.FontIcon;

public class ModeratorDashboard extends JPanel implements NetworkClient.MessageListener {
    private MainFrame mainFrame;
    private String activeTab = "REQUESTS";
    private JPanel contentArea;
    private Timer refreshTimer;

    private JPanel pendingContainer, approvedContainer, auctioningContainer, rejectedContainer, soldContainer;
    private int pendingCount = 0;
    private JLabel lblRequestCount;

    private DefaultTableModel roomModel;
    private JTable tblMyRooms;

    private ChatPanel modChatPanel;
    private DefaultListModel<String> userListModel;
    private JList<String> listUsers;
    private Map<String, Boolean> userStatusMap = new HashMap<>();

    private JButton btnNavRequests, btnNavRooms, btnNavChat;

    public ModeratorDashboard(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());

        add(buildHeader(),  BorderLayout.NORTH);
        add(buildSidebar(), BorderLayout.WEST);

        contentArea = new JPanel(new CardLayout());
        contentArea.setBackground(UITheme.BG_DARK);
        contentArea.setBorder(new EmptyBorder(24, 24, 24, 24));
        contentArea.add(buildRequestsPanel(), "REQUESTS");
        contentArea.add(buildRoomsPanel(),    "ROOMS");
        contentArea.add(buildChatPanel(),     "CHAT");
        add(contentArea, BorderLayout.CENTER);

        NetworkClient.getInstance().addListener(this);
        NetworkClient.getInstance().sendMessage(MessageType.GET_ROOM_LIST, null);
        NetworkClient.getInstance().sendMessage(MessageType.GET_USER_LIST, null);
        NetworkClient.getInstance().sendMessage(MessageType.GET_ALL_PRODUCTS, null);

        refreshTimer = new Timer(5000, e -> {
            NetworkClient.getInstance().sendMessage(MessageType.GET_ROOM_LIST, null);
            NetworkClient.getInstance().sendMessage(MessageType.GET_USER_LIST, null);
            NetworkClient.getInstance().sendMessage(MessageType.GET_ALL_PRODUCTS, null);
        });
        refreshTimer.start();
        showTab("REQUESTS");
    }

    private JPanel buildHeader() {
        JPanel h = new JPanel(new MigLayout("insets 0 24 0 24, fill", "[left]push[right]"));
        h.setPreferredSize(new Dimension(0, 64));
        h.setBackground(UITheme.BG_ELEVATED);
        h.setBorder(new MatteBorder(0, 0, 1, 0, UITheme.BORDER));

        JPanel left = new JPanel(new MigLayout("insets 0, gap 12"));
        left.setOpaque(false);
        JLabel logoIcon = new JLabel("⚡");
        logoIcon.setFont(new Font("Segoe UI", Font.BOLD, 24));
        logoIcon.setForeground(UIManager.getColor("Actions.Yellow"));
        JLabel logoText = new JLabel("AuctionPro");
        logoText.setFont(new Font("Segoe UI", Font.BOLD, 18));
        JLabel sep = new JLabel(" | Moderator");
        sep.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        sep.setForeground(UIManager.getColor("Label.disabledForeground"));
        
        left.add(logoIcon); left.add(logoText); left.add(sep);
        h.add(left, "cell 0 0");

        JPanel right = new JPanel(new MigLayout("insets 0, gap 16"));
        right.setOpaque(false);
        NotificationCenter notifCenter = new NotificationCenter();
        notifCenter.setClickListener(item -> showTab("REQUESTS"));
        right.add(notifCenter);

        String username = NetworkClient.getInstance().getCurrentUsername();
        JLabel userLabel = new JLabel("👤 " + (username != null ? username : "Moderator"));
        userLabel.setFont(UITheme.fontBold(14));
        userLabel.setForeground(UITheme.TEXT_PRIMARY);
        right.add(userLabel);
        
        h.add(right, "cell 1 0");
        return h;
    }

    private JButton btnNavRatings;

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new MigLayout("wrap 1, insets 16, gap 6, fillx"));
        sidebar.setBackground(UITheme.BG_DEEP);
        sidebar.setPreferredSize(new Dimension(280, 0));
        sidebar.setBorder(new MatteBorder(0, 0, 0, 1, UITheme.BORDER));

        JLabel navLabel = new JLabel("ĐIỀU PHỐI VIÊN");
        navLabel.setFont(UITheme.fontBold(11));
        navLabel.setForeground(UITheme.TEXT_HINT);
        sidebar.add(navLabel, "wrap 8");

        btnNavRequests = UITheme.navBtn("Yêu Cầu Seller", Feather.INBOX);
        btnNavRooms    = UITheme.navBtn("Quản Lý Phòng", Feather.LAYERS);
        btnNavChat     = UITheme.navBtn("Chat Người Dùng", Feather.MESSAGE_SQUARE);
        btnNavRatings  = UITheme.navBtn("Đánh Giá Của Tôi", Feather.STAR);

        btnNavRequests.addActionListener(e -> showTab("REQUESTS"));
        btnNavRooms.addActionListener(e -> showTab("ROOMS"));
        btnNavChat.addActionListener(e -> showTab("CHAT"));
        btnNavRatings.addActionListener(e -> {
            NetworkClient.getInstance().sendMessage(MessageType.GET_RATINGS, new HashMap<>());
        });

        sidebar.add(btnNavRequests, "growx");
        sidebar.add(btnNavRooms, "growx");
        sidebar.add(btnNavChat, "growx");
        sidebar.add(btnNavRatings, "growx");

        JButton btnThemeToggle = UITheme.ghostBtn(UITheme.isDarkMode ? "🌙 Dark Mode" : "☀️ Light Mode", UITheme.TEXT_MUTED);
        btnThemeToggle.addActionListener(e -> {
            UITheme.applyTheme(!UITheme.isDarkMode);
            btnThemeToggle.setText(UITheme.isDarkMode ? "🌙 Dark Mode" : "☀️ Light Mode");
        });
        sidebar.add(btnThemeToggle, "growx, pushy, aligny bottom, wrap 0");

        JButton btnLogout = UITheme.logoutBtn(UITheme.BG_DEEP, () -> {
            NetworkClient.getInstance().disconnect();
            if (refreshTimer != null) refreshTimer.stop();
            mainFrame.switchPanel("LOGIN");
        });
        sidebar.add(btnLogout, "growx");
        return sidebar;
    }

    private void showTab(String tabKey) {
        activeTab = tabKey;
        ((CardLayout) contentArea.getLayout()).show(contentArea, tabKey);

        UITheme.setNavActive(btnNavRequests, "REQUESTS".equals(tabKey));
        UITheme.setNavActive(btnNavRooms,    "ROOMS".equals(tabKey));
        UITheme.setNavActive(btnNavChat,     "CHAT".equals(tabKey));
        if (btnNavRatings != null)
            UITheme.setNavActive(btnNavRatings, false);
    }

    private JPanel buildRequestsPanel() {
        JPanel panel = UITheme.createCardPanel("wrap 1, insets 24, gap 16, fill");
        
        JPanel header = new JPanel(new MigLayout("insets 0, gap 4, wrap 1"));
        header.setOpaque(false);
        JLabel title = new JLabel("Yêu Cầu Từ Seller");
        title.setFont(UITheme.fontTitle(24));
        title.setForeground(UITheme.TEXT_PRIMARY);
        lblRequestCount = new JLabel("Đang tải...");
        lblRequestCount.setFont(UITheme.fontBody(14));
        lblRequestCount.setForeground(UITheme.TEXT_HINT);
        header.add(title); 
        header.add(lblRequestCount);
        panel.add(header, "growx");

        pendingContainer = createScrollableBox();
        approvedContainer = createScrollableBox();
        auctioningContainer = createScrollableBox();
        rejectedContainer = createScrollableBox();
        soldContainer = createScrollableBox();
        
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.putClientProperty("JTabbedPane.style", "tabType: card; tabRouting: scroll");
        tabbedPane.addTab("⏳ Chờ duyệt", UITheme.styledScrollPane(pendingContainer));
        tabbedPane.addTab("✅ Đã duyệt", UITheme.styledScrollPane(approvedContainer));
        tabbedPane.addTab("⚖ Đang đấu giá", UITheme.styledScrollPane(auctioningContainer));
        tabbedPane.addTab("❌ Từ chối", UITheme.styledScrollPane(rejectedContainer));
        tabbedPane.addTab("💰 Đã bán", UITheme.styledScrollPane(soldContainer));
        
        panel.add(tabbedPane, "grow, push");
        return panel;
    }
    
    private JPanel createScrollableBox() {
        JPanel p = new JPanel(new MigLayout("wrap 1, insets 16, gap 12, fillx", "[grow]"));
        p.setBackground(UITheme.BG_CARD);
        return p;
    }

    private void updateRequestCount() {
        if (lblRequestCount != null) {
            lblRequestCount.setText(pendingCount == 0 ? "Chưa có yêu cầu mới" : pendingCount + " sản phẩm chờ duyệt");
        }
    }

    private void populateProductContainer(JPanel container, java.util.List<Map<String,String>> items, String status) {
        container.removeAll();
        if (items.isEmpty()) {
            container.setLayout(new BorderLayout());
            container.add(UITheme.emptyState(Feather.PACKAGE, "Trống", "Không có sản phẩm nào"), BorderLayout.CENTER);
        } else {
            container.setLayout(new MigLayout("wrap 1, insets 16, gap 12, fillx", "[grow]"));
            for (Map<String,String> d : items) {
                container.add(buildProductCard(d, status), "growx");
            }
        }
        container.revalidate();
        container.repaint();
    }

    private JPanel buildProductCard(Map<String,String> data, String status) {
        String productId = data.getOrDefault("productId", "0");
        String productName = data.getOrDefault("productName", "(Không có tên)");
        String senderName = data.getOrDefault("senderName", "?");
        String description = data.getOrDefault("description", "");
        String startingPrice = data.getOrDefault("startingPrice", "0");
        String imageData = data.getOrDefault("imageData", "");

        JPanel card = new JPanel(new MigLayout("insets 16, gap 16, fill", "[80!]16[grow]16[right]", "[center]"));
        card.setBackground(UITheme.BG_ELEVATED);
        card.setBorder(new LineBorder(UITheme.BORDER, 1, true));
        card.putClientProperty("FlatLaf.style", "arc: 12");

        JLabel imgLabel = new JLabel();
        imgLabel.setPreferredSize(new Dimension(80, 80));
        imgLabel.setHorizontalAlignment(SwingConstants.CENTER);
        if (!imageData.isEmpty()) {
            try {
                byte[] bytes = Base64.getDecoder().decode(imageData.replaceAll("[^A-Za-z0-9+/=]", ""));
                BufferedImage bi = ImageIO.read(new ByteArrayInputStream(bytes));
                if (bi != null) {
                    imgLabel.setIcon(new ImageIcon(bi.getScaledInstance(80, 80, Image.SCALE_SMOOTH)));
                } else {
                    imgLabel.setText("No Image");
                }
            } catch (Exception ex) {
                imgLabel.setText("Error");
            }
        } else {
            imgLabel.setText("No Image");
        }
        card.add(imgLabel, "cell 0 0");

        JPanel info = new JPanel(new MigLayout("wrap 1, insets 0, gap 4"));
        info.setOpaque(false);
        JLabel nameLbl = new JLabel(productName);
        nameLbl.setFont(UITheme.fontBold(16));
        nameLbl.setForeground(UITheme.TEXT_PRIMARY);
        JLabel sellerLbl = new JLabel("Seller: " + senderName);
        sellerLbl.setFont(UITheme.fontBody(13));
        sellerLbl.setForeground(UITheme.TEXT_MUTED);
        JLabel priceLbl = new JLabel("Giá khởi điểm: " + startingPrice + " VND");
        priceLbl.setFont(UITheme.fontBold(14));
        priceLbl.setForeground(UITheme.SUCCESS);
        info.add(nameLbl); info.add(sellerLbl); info.add(priceLbl);
        card.add(info, "cell 1 0, grow");

        JPanel actions = new JPanel(new MigLayout("insets 0, gap 8"));
        actions.setOpaque(false);
        JButton btnView = UITheme.ghostBtn("Xem", UITheme.TEXT_MUTED);
        btnView.addActionListener(e -> showProductDetailsDialog(productId, productName, senderName, description, startingPrice, imageData, card, status));
        actions.add(btnView);

        if ("PENDING".equals(status)) {
            JButton btnApprove = UITheme.primaryBtn("Duyệt", UITheme.SUCCESS);
            btnApprove.addActionListener(e -> processProductAction(productId, MessageType.APPROVE_PRODUCT));
            
            JButton btnReject = UITheme.ghostBtn("Từ chối", UITheme.DANGER);
            btnReject.addActionListener(e -> processProductAction(productId, MessageType.REJECT_PRODUCT));
            
            actions.add(btnReject); actions.add(btnApprove);
        } else if ("APPROVED".equals(status)) {
            JButton btnAddToRoom = UITheme.primaryBtn("Đưa vào phòng", UITheme.INFO);
            btnAddToRoom.addActionListener(e -> showAddToRoomDialog(productId, productName));
            actions.add(btnAddToRoom);
        }
        card.add(actions, "cell 2 0");
        return card;
    }
    
    private void showAddToRoomDialog(String productId, String productName) {
        if (roomModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Bạn chưa có phòng nào. Vui lòng tạo phòng trước!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        java.util.List<String> activeRooms = new java.util.ArrayList<>();
        for (int i = 0; i < roomModel.getRowCount(); i++) {
            String status = (String) roomModel.getValueAt(i, 3);
            if ("ACTIVE".equals(status)) {
                activeRooms.add((String) roomModel.getValueAt(i, 0));
            }
        }
        
        if (activeRooms.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Không có phòng ACTIVE nào để thêm sản phẩm.", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Đưa Sản Phẩm Vào Phòng", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new net.miginfocom.swing.MigLayout("fill, insets 24"));
        dialog.setSize(450, 250);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(UITheme.BG_DARK);

        JPanel card = UITheme.createCardPanel("wrap 1, insets 24, gap 16, fillx");

        JLabel lblTitle = new JLabel("Thêm: " + productName);
        lblTitle.setFont(UITheme.fontTitle(16));
        lblTitle.setForeground(UITheme.TEXT_PRIMARY);
        card.add(lblTitle);

        JLabel lblMsg = new JLabel("Chọn phòng đấu giá:");
        lblMsg.setFont(UITheme.fontBold(13));
        lblMsg.setForeground(UITheme.TEXT_MUTED);
        card.add(lblMsg, "gapbottom 4");

        JComboBox<String> comboRoom = new JComboBox<>(activeRooms.toArray(new String[0]));
        comboRoom.setFont(UITheme.fontBody(14));
        card.add(comboRoom, "growx, h 40!");

        JPanel pBtn = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 0));
        pBtn.setOpaque(false);
        JButton btnCancel = UITheme.ghostBtn("Hủy", UITheme.TEXT_MUTED);
        btnCancel.addActionListener(e -> dialog.dispose());
        JButton btnSubmit = UITheme.primaryBtn("Xác Nhận");
        btnSubmit.addActionListener(e -> {
            String selectedRoom = (String) comboRoom.getSelectedItem();
            if (selectedRoom != null && !selectedRoom.trim().isEmpty()) {
                Map<String, String> data = new HashMap<>();
                data.put("roomId", selectedRoom);
                data.put("productId", productId);
                NetworkClient.getInstance().sendMessage(MessageType.ADD_PRODUCT_TO_ROOM, data);
            }
            dialog.dispose();
        });
        pBtn.add(btnCancel);
        pBtn.add(btnSubmit);
        
        card.add(pBtn, "growx, gaptop 16");

        dialog.add(card, "grow, push");
        dialog.setVisible(true);
    }

    private void processProductAction(String productId, MessageType type) {
        Map<String,String> msg = new HashMap<>();
        msg.put("productId", productId);
        NetworkClient.getInstance().sendMessage(type, msg);
        NetworkClient.getInstance().sendMessage(MessageType.GET_ALL_PRODUCTS, null);
    }

    private void showProductDetailsDialog(String productId, String productName, String senderName, String description, String startingPrice, String imageData, JPanel card, String status) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Chi Tiết Sản Phẩm", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(600, 500);
        dialog.setLocationRelativeTo(this);
        
        JPanel p = new JPanel(new BorderLayout(15, 15));
        p.setBorder(new EmptyBorder(15, 15, 15, 15));
        
        JTextArea descArea = new JTextArea(description);
        descArea.setWrapStyleWord(true); descArea.setLineWrap(true); descArea.setEditable(false);
        
        JPanel top = new JPanel(new BorderLayout(10, 10));
        JPanel infoPanel = new JPanel(new GridLayout(3, 1));
        infoPanel.add(new JLabel("Tên: " + productName));
        infoPanel.add(new JLabel("Người bán: " + senderName));
        infoPanel.add(new JLabel("Giá: " + startingPrice + " VND"));
        top.add(infoPanel, BorderLayout.CENTER);
        
        if (imageData != null && !imageData.isEmpty()) {
            JLabel imgLabel = new JLabel();
            imgLabel.setPreferredSize(new Dimension(150, 150));
            imgLabel.setHorizontalAlignment(SwingConstants.CENTER);
            imgLabel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
            try {
                String base64Str = imageData;
                if (base64Str.startsWith("data:image")) {
                    base64Str = base64Str.substring(base64Str.indexOf(",") + 1);
                }
                byte[] bytes = Base64.getDecoder().decode(base64Str.replaceAll("[^A-Za-z0-9+/=]", ""));
                BufferedImage bi = ImageIO.read(new ByteArrayInputStream(bytes));
                if (bi != null) {
                    Image scaledImage = bi.getScaledInstance(150, 150, Image.SCALE_SMOOTH);
                    imgLabel.setIcon(new ImageIcon(scaledImage));
                } else {
                    imgLabel.setText("No Image");
                }
            } catch (Exception ex) {
                imgLabel.setText("Lỗi Ảnh");
            }
            top.add(imgLabel, BorderLayout.EAST);
        }
        
        p.add(top, BorderLayout.NORTH);
        p.add(new JScrollPane(descArea), BorderLayout.CENTER);
        
        dialog.add(p);
        dialog.setVisible(true);
    }

    private JPanel buildRoomsPanel() {
        JPanel panel = UITheme.createCardPanel("wrap 1, insets 24, gap 16, fill");
        panel.add(UITheme.sectionHeader("Quản Lý Phòng", "Tạo và quản lý phòng đấu giá"), "growx");

        roomModel = new DefaultTableModel(new Object[]{"Mã Phòng", "Tiêu Đề", "Sellers", "Trạng Thái"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblMyRooms = UITheme.styledTable(roomModel);
        panel.add(UITheme.styledScrollPane(tblMyRooms), "grow, push");

        JPanel actions = new JPanel(new MigLayout("insets 0, gap 12", "[]push[][][]"));
        actions.setOpaque(false);
        JButton btnCreate = UITheme.primaryBtn("+ Tạo Phòng", UITheme.ACCENT);
        btnCreate.addActionListener(e -> {
            CreateRoomDialog dialog = new CreateRoomDialog(SwingUtilities.getWindowAncestor(this));
            dialog.setVisible(true);
            if (dialog.isConfirmed()) NetworkClient.getInstance().sendMessage(MessageType.GET_ROOM_LIST, null);
        });
        
        JButton btnStart = UITheme.primaryBtn("Bắt Đầu", UITheme.SUCCESS);
        btnStart.addActionListener(e -> {
            int row = tblMyRooms.getSelectedRow();
            if (row >= 0) {
                Map<String, String> params = new HashMap<>();
                params.put("roomId", (String) roomModel.getValueAt(row, 0));
                NetworkClient.getInstance().sendMessage(MessageType.OPEN_AUCTION, params);
            } else JOptionPane.showMessageDialog(this, "Chọn phòng!");
        });
        
        JButton btnEnter = UITheme.ghostBtn("Vào Phòng", UITheme.INFO);
        btnEnter.addActionListener(e -> {
            int row = tblMyRooms.getSelectedRow();
            if (row >= 0) {
                String rid = (String) roomModel.getValueAt(row, 0);
                AuctionRoomPanel roomPanel = new AuctionRoomPanel();
                roomPanel.initRoom(rid, true);
                mainFrame.addPanel(roomPanel, "MOD_ROOM_" + rid);
                mainFrame.switchPanel("MOD_ROOM_" + rid);
            } else JOptionPane.showMessageDialog(this, "Chọn phòng!");
        });
        
        actions.add(btnCreate); 
        actions.add(btnStart, "cell 2 0"); 
        actions.add(btnEnter, "cell 3 0");
        panel.add(actions, "growx");
        return panel;
    }

    private JPanel buildChatPanel() {
        JPanel panel = UITheme.createCardPanel("wrap 1, insets 24, gap 16, fill");
        panel.add(UITheme.sectionHeader("Chat Người Dùng", "Hỗ trợ và trao đổi"), "growx");

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerSize(1);
        split.setBorder(null);
        userListModel = new DefaultListModel<>();
        listUsers = new JList<>(userListModel);
        listUsers.setBackground(UITheme.BG_ELEVATED);
        listUsers.setForeground(UITheme.TEXT_PRIMARY);
        listUsers.setFont(UITheme.fontBody(14));
        listUsers.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                String user = value.toString();
                setText((userStatusMap.getOrDefault(user, false) ? "● " : "○ ") + user);
                if (isSelected) {
                    setBackground(UITheme.SIDEBAR_ACTIVE_BG);
                    setForeground(UITheme.ACCENT);
                } else {
                    setBackground(UITheme.BG_ELEVATED);
                    setForeground(UITheme.TEXT_PRIMARY);
                }
                return this;
            }
        });
        listUsers.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && listUsers.getSelectedValue() != null)
                modChatPanel.setPrivateMode(listUsers.getSelectedValue());
        });
        split.setLeftComponent(UITheme.styledScrollPane(listUsers));
        
        modChatPanel = new ChatPanel();
        split.setRightComponent(modChatPanel);
        split.setDividerLocation(250);
        panel.add(split, "grow, push");
        return panel;
    }

    private java.util.List<Map<String,String>> parseProductList(String raw) {
        java.util.List<Map<String,String>> list = new java.util.ArrayList<>();
        if (raw == null || raw.isEmpty()) return list;
        for (String entry : raw.split("\\|")) {
            String[] p = entry.split(",", 6);
            Map<String,String> m = new HashMap<>();
            m.put("productId", p.length > 0 ? p[0] : "0");
            m.put("senderName", p.length > 1 ? p[1] : "?");
            m.put("productName", p.length > 2 ? p[2] : "");
            m.put("description", p.length > 3 ? p[3] : "");
            m.put("startingPrice", p.length > 4 ? p[4] : "0");
            m.put("imageData", p.length > 5 ? p[5] : "");
            list.add(m);
        }
        return list;
    }

    @Override
    public void onMessage(MessageType type, Map<String, String> data) {
        SwingUtilities.invokeLater(() -> {
            if (type == MessageType.SUCCESS && data.containsKey("roomId")) {
                JOptionPane.showMessageDialog(this, "✅ Phòng tạo thành công!");
                NetworkClient.getInstance().sendMessage(MessageType.GET_ROOM_LIST, null);
            } else if (type == MessageType.ROOM_INFO) {
                String roomsStr = data.get("myRoomList");
                roomModel.setRowCount(0);
                if (roomsStr != null && !roomsStr.isEmpty()) {
                    for (String r : roomsStr.split("\\|")) {
                        String[] parts = r.split(",", -1);
                        roomModel.addRow(new Object[]{
                            parts.length > 0 ? parts[0] : "",
                            parts.length > 1 ? parts[1] : "",
                            parts.length > 2 ? parts[2] : "",
                            parts.length > 3 ? parts[3] : "ACTIVE"
                        });
                    }
                }
            } else if (type == MessageType.GET_USER_LIST) {
                String usersStr = data.get("userList");
                userStatusMap.clear(); userListModel.clear();
                if (usersStr != null && !usersStr.isEmpty()) {
                    for (String u : usersStr.split(",")) {
                        String[] parts = u.split(":");
                        String name = parts[0].trim();
                        userStatusMap.put(name, parts.length > 1 && "1".equals(parts[1]));
                        userListModel.addElement(name);
                    }
                }
            } else if (type == MessageType.GET_ALL_PRODUCTS) {
                java.util.List<Map<String,String>> pending = parseProductList(data.get("pending"));
                java.util.List<Map<String,String>> approved = parseProductList(data.get("approved"));
                java.util.List<Map<String,String>> auctioning = parseProductList(data.get("auctioning"));
                java.util.List<Map<String,String>> rejected = parseProductList(data.get("rejected"));
                java.util.List<Map<String,String>> sold = parseProductList(data.get("sold"));

                pendingCount = pending.size();
                SwingUtilities.invokeLater(() -> {
                    populateProductContainer(pendingContainer, pending, "PENDING");
                    populateProductContainer(approvedContainer, approved, "APPROVED");
                    populateProductContainer(auctioningContainer, auctioning, "AUCTIONING");
                    populateProductContainer(rejectedContainer, rejected, "REJECTED");
                    populateProductContainer(soldContainer, sold, "SOLD");
                    updateRequestCount();
                });
            } else if (type == MessageType.GET_RATINGS) {
                String ratingsStr = data.get("ratings");
                showRatingsDialog(ratingsStr);
            } else if (type == MessageType.ERROR && data.containsKey("message")) {
                JOptionPane.showMessageDialog(this, "❌ " + data.get("message"), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void showRatingsDialog(String ratingsStr) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Đánh Giá Của Bạn", true);
        dialog.setLayout(new net.miginfocom.swing.MigLayout("fill, insets 24"));
        dialog.setSize(500, 450); // Giảm chiều cao
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(UITheme.BG_DARK);

        JPanel card = UITheme.createCardPanel("wrap 1, insets 24, gap 16, fill");

        JLabel lblTitle = new JLabel("Đánh Giá Của Bạn");
        lblTitle.setFont(UITheme.fontTitle(18));
        lblTitle.setForeground(UITheme.TEXT_PRIMARY);
        card.add(lblTitle, "growx");

        if (ratingsStr == null || ratingsStr.isEmpty()) {
            JPanel empty = UITheme.emptyState(org.kordamp.ikonli.feather.Feather.INBOX, 
                "Không có đánh giá", 
                "Chưa có người dùng nào đánh giá bạn.");
            card.add(empty, "grow, pushy");
        } else {
            JPanel listPanel = new JPanel();
            listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
            listPanel.setOpaque(false);

            for (String r : ratingsStr.split("\\|")) {
                String[] parts = r.split(",", 3);
                if (parts.length >= 3) {
                    String user = parts[0];
                    String score = parts[1];
                    String comment = parts[2].replace("/", "|").trim(); 

                    JPanel item = new JPanel(new BorderLayout(8, 8));
                    item.setBorder(BorderFactory.createCompoundBorder(
                        new javax.swing.border.LineBorder(UITheme.BORDER, 1, true),
                        new javax.swing.border.EmptyBorder(12, 16, 12, 16)
                    ));
                    item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
                    item.setOpaque(false);

                    // Header: User + Stars
                    JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
                    headerPanel.setOpaque(false);
                    JLabel lblUser = new JLabel("Từ: " + user);
                    lblUser.setFont(UITheme.fontBold(13));
                    lblUser.setForeground(UITheme.TEXT_PRIMARY);
                    headerPanel.add(lblUser);
                    headerPanel.add(Box.createHorizontalStrut(8));

                    int numScore = 0;
                    try { numScore = Integer.parseInt(score); } catch (Exception ignored) {}
                    for (int i = 0; i < 5; i++) {
                        JLabel star = new JLabel();
                        if (i < numScore) {
                            star.setIcon(org.kordamp.ikonli.swing.FontIcon.of(org.kordamp.ikonli.feather.Feather.STAR, 18, UITheme.AMBER));
                        } else {
                            star.setIcon(org.kordamp.ikonli.swing.FontIcon.of(org.kordamp.ikonli.feather.Feather.STAR, 18, UITheme.BORDER));
                        }
                        headerPanel.add(star);
                    }
                    item.add(headerPanel, BorderLayout.NORTH);

                    // Comment field (Read-only view)
                    if (comment.isEmpty()) {
                        JLabel lblEmptyComment = new JLabel("Không có nhận xét");
                        lblEmptyComment.setFont(new Font(UITheme.fontBody(13).getName(), Font.ITALIC, 13));
                        lblEmptyComment.setForeground(UITheme.TEXT_HINT);
                        item.add(lblEmptyComment, BorderLayout.CENTER);
                    } else {
                        JTextArea txtComment = new JTextArea(comment);
                        txtComment.setLineWrap(true);
                        txtComment.setWrapStyleWord(true);
                        txtComment.setEditable(false);
                        txtComment.setFocusable(false); // No blinking cursor
                        txtComment.setOpaque(false);
                        txtComment.setForeground(UITheme.TEXT_MUTED);
                        txtComment.setFont(UITheme.fontBody(13));
                        item.add(txtComment, BorderLayout.CENTER);
                    }

                    listPanel.add(item);
                    listPanel.add(Box.createVerticalStrut(12));
                }
            }
            JScrollPane sp = UITheme.styledScrollPane(listPanel);
            sp.setBorder(null);
            card.add(sp, "grow, pushy");
        }

        // Action area with divider
        card.add(new JSeparator(), "growx, gaptop 8");

        JPanel pBtn = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        pBtn.setOpaque(false);
        JButton btnClose = UITheme.ghostBtn("Đóng", UITheme.TEXT_MUTED);
        btnClose.addActionListener(e -> dialog.dispose());
        pBtn.add(btnClose);

        card.add(pBtn, "growx, gaptop 8");

        dialog.add(card, "grow, push");
        dialog.setVisible(true);
    }
}