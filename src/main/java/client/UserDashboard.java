package client;

import shared.MessageType;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;

public class UserDashboard extends JPanel implements NetworkClient.MessageListener {
    private MainFrame mainFrame;
    private String activeTab = "ROOMS";
    private JPanel contentArea;

    private JTable tblRooms;
    private DefaultTableModel roomModel;
    private Timer refreshTimer;
    private JLabel lblRoomCount;

    private JList<String> listMods;
    private DefaultListModel<String> modModel;
    private Map<String, Boolean> modStatusMap = new HashMap<>();
    private ProductSubmitPanel submitPanel;
    private ChatPanel chatPanel;

    private JPanel historyContainer;

    private JButton btnNavRooms, btnNavContact, btnNavHistory;

    public UserDashboard(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());

        add(buildHeader(), BorderLayout.NORTH);
        add(buildSidebar(), BorderLayout.WEST);

        contentArea = new JPanel(new CardLayout());
        contentArea.setBorder(new EmptyBorder(20, 20, 20, 20));
        contentArea.add(buildRoomsPanel(),   "ROOMS");
        contentArea.add(buildContactPanel(), "CONTACT");
        contentArea.add(buildHistoryPanel(), "HISTORY");
        add(contentArea, BorderLayout.CENTER);

        NetworkClient.getInstance().addListener(this);
        refreshTimer = new Timer(5000, e -> {
            NetworkClient.getInstance().sendMessage(MessageType.GET_ROOM_LIST, null);
            NetworkClient.getInstance().sendMessage(MessageType.GET_MOD_LIST, null);
        });
        refreshTimer.start();

        NetworkClient.getInstance().sendMessage(MessageType.GET_ROOM_LIST, null);
        NetworkClient.getInstance().sendMessage(MessageType.GET_MOD_LIST, null);
        NetworkClient.getInstance().sendMessage(MessageType.GET_HISTORY, null);
        showTab("ROOMS");
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
        logoIcon.setForeground(UIManager.getColor("Actions.Blue"));
        JLabel logoText = new JLabel("AuctionPro");
        logoText.setFont(new Font("Segoe UI", Font.BOLD, 18));
        JLabel sep = new JLabel(" | User");
        sep.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        sep.setForeground(UIManager.getColor("Label.disabledForeground"));
        
        left.add(logoIcon); left.add(logoText); left.add(sep);
        h.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        String username = NetworkClient.getInstance().getCurrentUsername();
        JLabel userLabel = new JLabel("👤 " + (username != null ? username : "User"));
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

        JLabel navLabel = new JLabel("NGƯỜI DÙNG");
        navLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        navLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        sidebar.add(navLabel);
        sidebar.add(Box.createVerticalStrut(10));

        btnNavRooms    = makeNavItem("🏠 Phòng Đấu Giá");
        btnNavContact  = makeNavItem("💬 Liên Hệ Trung Gian");
        btnNavHistory  = makeNavItem("📋 Lịch Sử Giao Dịch");

        btnNavRooms.addActionListener(e -> showTab("ROOMS"));
        btnNavContact.addActionListener(e -> showTab("CONTACT"));
        btnNavHistory.addActionListener(e -> showTab("HISTORY"));

        sidebar.add(btnNavRooms);   sidebar.add(Box.createVerticalStrut(5));
        sidebar.add(btnNavContact); sidebar.add(Box.createVerticalStrut(5));
        sidebar.add(btnNavHistory);
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

    private void showTab(String key) {
        activeTab = key;
        ((CardLayout) contentArea.getLayout()).show(contentArea, key);
        
        Font boldFont = new Font("Segoe UI", Font.BOLD, 14);
        Font plainFont = new Font("Segoe UI", Font.PLAIN, 14);
        
        btnNavRooms.setFont(plainFont);
        btnNavContact.setFont(plainFont);
        btnNavHistory.setFont(plainFont);
        
        if ("ROOMS".equals(key)) btnNavRooms.setFont(boldFont);
        else if ("CONTACT".equals(key)) btnNavContact.setFont(boldFont);
        else btnNavHistory.setFont(boldFont);
    }

    private JPanel buildRoomsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Phòng Đấu Giá Đang Mở");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblRoomCount = new JLabel("Đang tải...");
        header.add(title); header.add(lblRoomCount);
        panel.add(header, BorderLayout.NORTH);

        roomModel = new DefaultTableModel(new Object[]{"Mã Phòng", "Tiêu Đề", "Moderator", "Trạng Thái"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblRooms = new JTable(roomModel);
        tblRooms.setRowHeight(35);
        panel.add(new JScrollPane(tblRooms), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnJoinRoom = new JButton("Tham Gia Phòng");
        btnJoinRoom.putClientProperty("JButton.buttonType", "roundRect");
        btnJoinRoom.addActionListener(e -> {
            int row = tblRooms.getSelectedRow();
            if (row >= 0) {
                Map<String, String> data = new HashMap<>();
                data.put("roomId", (String) roomModel.getValueAt(row, 0));
                NetworkClient.getInstance().sendMessage(MessageType.JOIN_ROOM, data);
            } else {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn một phòng.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        bottom.add(btnJoinRoom);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildContactPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        
        JLabel title = new JLabel("Liên Hệ Trung Gian");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        panel.add(title, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerLocation(200);

        modModel = new DefaultListModel<>();
        listMods = new JList<>(modModel);
        listMods.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                String mod = value.toString();
                setText((modStatusMap.getOrDefault(mod, false) ? "● " : "○ ") + mod);
                return this;
            }
        });
        listMods.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && listMods.getSelectedValue() != null) {
                String mod = listMods.getSelectedValue();
                submitPanel.setTargetMod(mod);
                chatPanel.setPrivateMode(mod);
            }
        });
        split.setLeftComponent(new JScrollPane(listMods));

        JSplitPane rightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        rightSplit.setDividerLocation(300);
        submitPanel = new ProductSubmitPanel();
        chatPanel = new ChatPanel();
        
        JPanel pnlSubmit = new JPanel(new BorderLayout());
        pnlSubmit.setBorder(new TitledBorder("Gửi Sản Phẩm"));
        pnlSubmit.add(submitPanel, BorderLayout.CENTER);
        
        JPanel pnlChat = new JPanel(new BorderLayout());
        pnlChat.setBorder(new TitledBorder("Chat"));
        pnlChat.add(chatPanel, BorderLayout.CENTER);
        
        rightSplit.setLeftComponent(pnlSubmit);
        rightSplit.setRightComponent(pnlChat);

        split.setRightComponent(rightSplit);
        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        
        JLabel title = new JLabel("Lịch Sử Giao Dịch");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        panel.add(title, BorderLayout.NORTH);

        historyContainer = new JPanel();
        historyContainer.setLayout(new BoxLayout(historyContainer, BoxLayout.Y_AXIS));
        
        JScrollPane sp = new JScrollPane(historyContainer);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        panel.add(sp, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnExport = new JButton("Xuất XML");
        btnExport.addActionListener(e -> NetworkClient.getInstance().sendMessage(MessageType.EXPORT_HISTORY, null));
        bottom.add(btnExport);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    private void addTransactionCard(String productName, String finalPrice, String status, String winner) {
        JPanel card = new JPanel(new BorderLayout(15, 0));
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(UIManager.getColor("Component.borderColor"), 1, true),
            new EmptyBorder(10, 15, 10, 15)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        
        JPanel info = new JPanel(new GridLayout(2, 1));
        JLabel nameLbl = new JLabel(productName != null ? productName : "Sản phẩm");
        nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        info.add(nameLbl);
        
        String details = "";
        if (finalPrice != null && !finalPrice.isEmpty()) details += "Giá: " + finalPrice + "   ";
        if (winner != null && !winner.isEmpty()) details += "Người thắng: " + winner;
        info.add(new JLabel(details));
        card.add(info, BorderLayout.CENTER);
        
        if (status != null && !status.isEmpty()) {
            JLabel badge = new JLabel(" " + status.toUpperCase() + " ");
            badge.setFont(new Font("Segoe UI", Font.BOLD, 12));
            String sLower = status.toLowerCase();
            if (sLower.contains("thắng") || sLower.contains("won") || sLower.contains("mua")) badge.setForeground(UIManager.getColor("Actions.Green"));
            else if (sLower.contains("thua") || sLower.contains("lost") || sLower.contains("từ chối")) badge.setForeground(UIManager.getColor("Actions.Red"));
            else badge.setForeground(UIManager.getColor("Actions.Blue"));
            card.add(badge, BorderLayout.EAST);
        }

        historyContainer.add(card);
        historyContainer.add(Box.createVerticalStrut(10));
    }

    private void addSimpleHistoryCard(String text) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(UIManager.getColor("Component.borderColor"), 1, true),
            new EmptyBorder(10, 15, 10, 15)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        card.add(new JLabel(text), BorderLayout.CENTER);
        historyContainer.add(card);
        historyContainer.add(Box.createVerticalStrut(10));
    }

    @Override
    public void onMessage(MessageType type, Map<String, String> data) {
        SwingUtilities.invokeLater(() -> {
            if (type == MessageType.SUCCESS && data.containsKey("roomId") && "JOINED".equals(data.get("status"))) {
                AuctionRoomPanel rp = new AuctionRoomPanel();
                rp.initRoom(data.get("roomId"), false);
                mainFrame.addPanel(rp, "ROOM_" + data.get("roomId"));
                mainFrame.switchPanel("ROOM_" + data.get("roomId"));

            } else if (type == MessageType.SUCCESS && data.containsKey("message")) {
                JOptionPane.showMessageDialog(this, "✅ " + data.get("message"), "Thành công", JOptionPane.INFORMATION_MESSAGE);

            } else if (type == MessageType.ERROR && data.containsKey("message")) {
                JOptionPane.showMessageDialog(this, "❌ " + data.get("message"), "Lỗi", JOptionPane.ERROR_MESSAGE);

            } else if (type == MessageType.INVITE_SELLER) {
                String roomId = data.getOrDefault("roomId", "?");
                String roomTitle = data.getOrDefault("title", roomId);
                String mod = data.getOrDefault("modName", "Moderator");
                int choice = JOptionPane.showConfirmDialog(this,
                        "Moderator " + mod + " mời bạn vào:\n" + roomTitle + " (" + roomId + ")\n\nTham gia ngay?",
                        "Lời Mời", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
                if (choice == JOptionPane.YES_OPTION) {
                    Map<String, String> req = new HashMap<>();
                    req.put("roomId", roomId);
                    NetworkClient.getInstance().sendMessage(MessageType.JOIN_ROOM, req);
                }

            } else if (type == MessageType.ROOM_INFO && data.containsKey("roomList")) {
                roomModel.setRowCount(0);
                String roomsStr = data.get("roomList");
                if (roomsStr != null && !roomsStr.isEmpty()) {
                    for (String r : roomsStr.split("\\|")) {
                        String[] p = r.split(",", -1);
                        if (p.length >= 4) roomModel.addRow(new Object[]{p[0], p[1], p[2], p[3]});
                    }
                }
                if (lblRoomCount != null) {
                    int c = roomModel.getRowCount();
                    lblRoomCount.setText(c == 0 ? "Không có phòng nào" : c + " phòng");
                }

            } else if (type == MessageType.GET_MOD_LIST) {
                modModel.clear(); modStatusMap.clear();
                String s = data.get("modList");
                if (s != null && !s.isEmpty()) {
                    for (String m : s.split(",")) {
                        String[] parts = m.split(":");
                        String name = parts[0].trim();
                        modStatusMap.put(name, parts.length > 1 && "1".equals(parts[1]));
                        modModel.addElement(name);
                    }
                }

            } else if (type == MessageType.GET_HISTORY) {
                historyContainer.removeAll();
                String h = data.get("historyData");

                if (h == null || h.trim().isEmpty()) {
                    historyContainer.add(new JLabel("Chưa có giao dịch"));
                } else {
                    for (String line : h.split("\n")) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("─") || line.startsWith("===") || line.contains("LỊCH SỬ")) continue;

                        String productName = null, price = null, status = null, winner = null;
                        String lowerLine = line.toLowerCase();

                        if (line.contains(":")) {
                            String[] parts = line.split(":", 2);
                            if (parts.length == 2) {
                                String key = parts[0].trim().toLowerCase();
                                String value = parts[1].trim();
                                if (key.contains("sản phẩm") || key.contains("product")) productName = value;
                                else if (key.contains("giá") || key.contains("price")) price = value;
                                else if (key.contains("người thắng") || key.contains("winner")) winner = value;
                                else if (key.contains("trạng thái") || key.contains("status")) status = value;
                            }
                        }

                        if (lowerLine.contains("thắng") || lowerLine.contains("won")) status = status == null ? "THẮNG" : status;
                        else if (lowerLine.contains("thua") || lowerLine.contains("lost")) status = status == null ? "THUA" : status;
                        else if (lowerLine.contains("chấp nhận") || lowerLine.contains("mua")) status = status == null ? "ĐÃ MUA" : status;
                        else if (lowerLine.contains("từ chối")) status = status == null ? "TỪ CHỐI" : status;

                        if (productName != null || status != null) {
                            if (productName == null) productName = line.length() > 60 ? line.substring(0, 57) + "..." : line;
                            addTransactionCard(productName, price, status, winner);
                        } else {
                            addSimpleHistoryCard(line);
                        }
                    }
                }
                historyContainer.revalidate();
                historyContainer.repaint();

            } else if (type == MessageType.EXPORT_HISTORY) {
                String xmlData = data.get("xmlData");
                if (xmlData != null) {
                    try (java.io.PrintWriter out = new java.io.PrintWriter("history_export.xml")) {
                        out.println(xmlData);
                        JOptionPane.showMessageDialog(this, "Đã xuất!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
    }
}