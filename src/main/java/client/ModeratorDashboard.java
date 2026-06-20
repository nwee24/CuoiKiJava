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

public class ModeratorDashboard extends JPanel implements NetworkClient.MessageListener {
    private MainFrame mainFrame;
    private String activeTab = "REQUESTS";
    private JPanel contentArea;
    private Timer refreshTimer;

    private JPanel pendingContainer, approvedContainer, rejectedContainer;
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
        contentArea.setBorder(new EmptyBorder(20, 20, 20, 20));
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
        JPanel h = new JPanel(new BorderLayout());
        h.setPreferredSize(new Dimension(0, 60));
        h.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, UIManager.getColor("Component.borderColor")),
            new EmptyBorder(0, 20, 0, 20)
        ));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JLabel logoIcon = new JLabel("⚡");
        logoIcon.setFont(new Font("Segoe UI", Font.BOLD, 24));
        logoIcon.setForeground(UIManager.getColor("Actions.Yellow"));
        JLabel logoText = new JLabel("AuctionPro");
        logoText.setFont(new Font("Segoe UI", Font.BOLD, 18));
        JLabel sep = new JLabel(" | Moderator");
        sep.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        sep.setForeground(UIManager.getColor("Label.disabledForeground"));
        
        left.add(logoIcon); left.add(logoText); left.add(sep);
        h.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        NotificationCenter notifCenter = new NotificationCenter();
        notifCenter.setClickListener(item -> showTab("REQUESTS"));
        right.add(notifCenter);

        String username = NetworkClient.getInstance().getCurrentUsername();
        JLabel userLabel = new JLabel("👤 " + (username != null ? username : "Moderator"));
        userLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        right.add(userLabel);
        
        h.add(right, BorderLayout.EAST);
        return h;
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 0, 1, UIManager.getColor("Component.borderColor")),
            new EmptyBorder(20, 15, 20, 15)
        ));

        JLabel navLabel = new JLabel("ĐIỀU PHỐI VIÊN");
        navLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        navLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        sidebar.add(navLabel);
        sidebar.add(Box.createVerticalStrut(10));

        btnNavRequests = makeNavItem("📥 Yêu Cầu Seller");
        btnNavRooms    = makeNavItem("🏠 Quản Lý Phòng");
        btnNavChat     = makeNavItem("💬 Chat Người Dùng");

        btnNavRequests.addActionListener(e -> showTab("REQUESTS"));
        btnNavRooms.addActionListener(e -> showTab("ROOMS"));
        btnNavChat.addActionListener(e -> showTab("CHAT"));

        sidebar.add(btnNavRequests); sidebar.add(Box.createVerticalStrut(5));
        sidebar.add(btnNavRooms);    sidebar.add(Box.createVerticalStrut(5));
        sidebar.add(btnNavChat);
        sidebar.add(Box.createVerticalGlue());

        JButton btnLogout = new JButton("Đăng xuất");
        btnLogout.setForeground(UIManager.getColor("Actions.Red"));
        btnLogout.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btnLogout.addActionListener(e -> {
            NetworkClient.getInstance().disconnect();
            if (refreshTimer != null) refreshTimer.stop();
            mainFrame.switchPanel("LOGIN");
        });
        sidebar.add(btnLogout);
        return sidebar;
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
        
        btnNavRequests.setFont(plainFont);
        btnNavRooms.setFont(plainFont);
        btnNavChat.setFont(plainFont);
        
        if ("REQUESTS".equals(tabKey)) btnNavRequests.setFont(boldFont);
        else if ("ROOMS".equals(tabKey)) btnNavRooms.setFont(boldFont);
        else btnNavChat.setFont(boldFont);
    }

    private JPanel buildRequestsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Yêu Cầu Từ Seller");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblRequestCount = new JLabel("Đang tải...");
        lblRequestCount.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblRequestCount.setForeground(UIManager.getColor("Label.disabledForeground"));
        header.add(title); header.add(lblRequestCount);
        panel.add(header, BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();
        pendingContainer = createScrollableBox();
        approvedContainer = createScrollableBox();
        rejectedContainer = createScrollableBox();
        
        tabbedPane.addTab("⏳ Chờ duyệt", new JScrollPane(pendingContainer));
        tabbedPane.addTab("✅ Đã duyệt", new JScrollPane(approvedContainer));
        tabbedPane.addTab("❌ Từ chối", new JScrollPane(rejectedContainer));
        
        panel.add(tabbedPane, BorderLayout.CENTER);
        return panel;
    }
    
    private JPanel createScrollableBox() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(10, 10, 10, 10));
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
            JLabel empty = new JLabel("Không có sản phẩm nào");
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            container.add(empty);
        } else {
            for (Map<String,String> d : items) {
                container.add(buildProductCard(d, status));
                container.add(Box.createVerticalStrut(10));
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

        JPanel card = new JPanel(new BorderLayout(15, 0));
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(UIManager.getColor("Component.borderColor"), 1, true),
            new EmptyBorder(10, 10, 10, 10)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

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
        card.add(imgLabel, BorderLayout.WEST);

        JPanel info = new JPanel(new GridLayout(3, 1));
        JLabel nameLbl = new JLabel(productName);
        nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        JLabel sellerLbl = new JLabel("Seller: " + senderName);
        JLabel priceLbl = new JLabel("Giá khởi điểm: " + startingPrice + " VND");
        priceLbl.setForeground(UIManager.getColor("Actions.Green"));
        info.add(nameLbl); info.add(sellerLbl); info.add(priceLbl);
        card.add(info, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnView = new JButton("Xem");
        btnView.addActionListener(e -> showProductDetailsDialog(productId, productName, senderName, description, startingPrice, imageData, card, status));
        actions.add(btnView);

        if ("PENDING".equals(status)) {
            JButton btnApprove = new JButton("Duyệt");
            btnApprove.putClientProperty("JButton.buttonType", "roundRect");
            btnApprove.setForeground(UIManager.getColor("Actions.Green"));
            btnApprove.addActionListener(e -> processProductAction(productId, MessageType.APPROVE_PRODUCT));
            
            JButton btnReject = new JButton("Từ chối");
            btnReject.putClientProperty("JButton.buttonType", "roundRect");
            btnReject.setForeground(UIManager.getColor("Actions.Red"));
            btnReject.addActionListener(e -> processProductAction(productId, MessageType.REJECT_PRODUCT));
            
            actions.add(btnReject); actions.add(btnApprove);
        }
        card.add(actions, BorderLayout.EAST);
        return card;
    }

    private void processProductAction(String productId, MessageType type) {
        Map<String,String> msg = new HashMap<>();
        msg.put("productId", productId);
        NetworkClient.getInstance().sendMessage(type, msg);
        NetworkClient.getInstance().sendMessage(MessageType.GET_ALL_PRODUCTS, null);
    }

    private void showProductDetailsDialog(String productId, String productName, String senderName, String description, String startingPrice, String imageData, JPanel card, String status) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Chi Tiết Sản Phẩm", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);
        
        JPanel p = new JPanel(new BorderLayout(15, 15));
        p.setBorder(new EmptyBorder(15, 15, 15, 15));
        
        JTextArea descArea = new JTextArea(description);
        descArea.setWrapStyleWord(true); descArea.setLineWrap(true); descArea.setEditable(false);
        p.add(new JScrollPane(descArea), BorderLayout.CENTER);
        
        JPanel top = new JPanel(new GridLayout(3, 1));
        top.add(new JLabel("Tên: " + productName));
        top.add(new JLabel("Người bán: " + senderName));
        top.add(new JLabel("Giá: " + startingPrice + " VND"));
        p.add(top, BorderLayout.NORTH);
        
        dialog.add(p);
        dialog.setVisible(true);
    }

    private JPanel buildRoomsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Quản Lý Phòng");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        header.add(title);
        panel.add(header, BorderLayout.NORTH);

        roomModel = new DefaultTableModel(new Object[]{"Mã Phòng", "Tiêu Đề", "Sellers", "Trạng Thái"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblMyRooms = new JTable(roomModel);
        tblMyRooms.setRowHeight(35);
        panel.add(new JScrollPane(tblMyRooms), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnCreate = new JButton("+ Tạo Phòng");
        btnCreate.addActionListener(e -> {
            CreateRoomDialog dialog = new CreateRoomDialog(SwingUtilities.getWindowAncestor(this));
            dialog.setVisible(true);
            if (dialog.isConfirmed()) NetworkClient.getInstance().sendMessage(MessageType.GET_ROOM_LIST, null);
        });
        
        JButton btnStart = new JButton("Bắt Đầu");
        btnStart.addActionListener(e -> {
            int row = tblMyRooms.getSelectedRow();
            if (row >= 0) {
                Map<String, String> params = new HashMap<>();
                params.put("roomId", (String) roomModel.getValueAt(row, 0));
                NetworkClient.getInstance().sendMessage(MessageType.OPEN_AUCTION, params);
            } else JOptionPane.showMessageDialog(this, "Chọn phòng!");
        });
        
        JButton btnEnter = new JButton("Vào Phòng");
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
        
        actions.add(btnCreate); actions.add(btnStart); actions.add(btnEnter);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildChatPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        
        JLabel title = new JLabel("Chat Người Dùng");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        panel.add(title, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        userListModel = new DefaultListModel<>();
        listUsers = new JList<>(userListModel);
        listUsers.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                String user = value.toString();
                setText((userStatusMap.getOrDefault(user, false) ? "● " : "○ ") + user);
                return this;
            }
        });
        listUsers.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && listUsers.getSelectedValue() != null)
                modChatPanel.setPrivateMode(listUsers.getSelectedValue());
        });
        split.setLeftComponent(new JScrollPane(listUsers));
        
        modChatPanel = new ChatPanel();
        split.setRightComponent(modChatPanel);
        split.setDividerLocation(200);
        panel.add(split, BorderLayout.CENTER);
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
                pendingCount = pending.size();
                populateProductContainer(pendingContainer, pending, "PENDING");
                populateProductContainer(approvedContainer, parseProductList(data.get("approved")), "APPROVED");
                populateProductContainer(rejectedContainer, parseProductList(data.get("rejected")), "REJECTED");
                updateRequestCount();
            } else if (type == MessageType.ERROR && data.containsKey("message")) {
                JOptionPane.showMessageDialog(this, "❌ " + data.get("message"), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}