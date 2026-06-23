package client;

import shared.MessageType;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;
import net.miginfocom.swing.MigLayout;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.swing.FontIcon;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.FileOutputStream;

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
    private ChatPanel partnerChatPanel;

    private JPanel historyContainer;
    private JPanel transactionsContainer;
    private String lastHistoryData = "";

    private JButton btnNavRooms, btnNavContact, btnNavHistory, btnNavTransactions;

    public UserDashboard(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());

        add(buildHeader(), BorderLayout.NORTH);
        add(buildSidebar(), BorderLayout.WEST);

        contentArea = new JPanel(new CardLayout());
        contentArea.setBackground(UITheme.BG_DARK);
        contentArea.setBorder(new EmptyBorder(24, 24, 24, 24));
        contentArea.add(buildRoomsPanel(),        "ROOMS");
        contentArea.add(buildContactPanel(),      "CONTACT");
        contentArea.add(buildHistoryPanel(),      "HISTORY");
        contentArea.add(buildTransactionsPanel(), "TRANSACTIONS");
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
        NetworkClient.getInstance().sendMessage(MessageType.GET_SUCCESSFUL_TRANSACTIONS, null);
        showTab("ROOMS");
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
        logoIcon.setForeground(UIManager.getColor("Actions.Blue"));
        JLabel logoText = new JLabel("AuctionPro");
        logoText.setFont(new Font("Segoe UI", Font.BOLD, 18));
        JLabel sep = new JLabel(" | User");
        sep.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        sep.setForeground(UIManager.getColor("Label.disabledForeground"));
        left.add(logoIcon); left.add(logoText); left.add(sep);
        h.add(left, "cell 0 0");

        JPanel right = new JPanel(new MigLayout("insets 0, gap 16"));
        right.setOpaque(false);
        String username = NetworkClient.getInstance().getCurrentUsername();
        JLabel userLabel = new JLabel("👤 " + (username != null ? username : "User"));
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

        JLabel navLabel = new JLabel("NGƯỜI DÙNG");
        navLabel.setFont(UITheme.fontBold(11));
        navLabel.setForeground(UITheme.TEXT_HINT);
        sidebar.add(navLabel, "wrap 8");

        btnNavRooms        = UITheme.navBtn("Phòng Đấu Giá", Feather.HOME);
        btnNavContact      = UITheme.navBtn("Liên Hệ Trung Gian", Feather.MESSAGE_SQUARE);
        btnNavTransactions = UITheme.navBtn("Giao Dịch Của Tôi", Feather.SHOPPING_CART);
        btnNavHistory      = UITheme.navBtn("Lịch Sử Giao Dịch", Feather.CLIPBOARD);

        btnNavRooms.addActionListener(e -> showTab("ROOMS"));
        btnNavContact.addActionListener(e -> showTab("CONTACT"));
        btnNavHistory.addActionListener(e -> {
            showTab("HISTORY");
            NetworkClient.getInstance().sendMessage(MessageType.GET_HISTORY, null);
        });
        btnNavTransactions.addActionListener(e -> {
            showTab("TRANSACTIONS");
            NetworkClient.getInstance().sendMessage(MessageType.GET_SUCCESSFUL_TRANSACTIONS, null);
        });

        sidebar.add(btnNavRooms, "growx");
        sidebar.add(btnNavContact, "growx");
        sidebar.add(btnNavTransactions, "growx");
        sidebar.add(btnNavHistory, "growx");

        // Nút xem đánh giá Mod
        JButton btnViewModRatings = UITheme.navBtn("Xem Đánh Giá Mod", Feather.STAR);
        btnViewModRatings.addActionListener(e -> openViewModRatingsDialog());
        sidebar.add(btnViewModRatings, "growx, wrap 20");

        JButton btnThemeToggle = UITheme.ghostBtn(UITheme.isDarkMode ? "Dark Mode" : "Light Mode", UITheme.TEXT_MUTED);
        btnThemeToggle.setIcon(FontIcon.of(UITheme.isDarkMode ? Feather.MOON : Feather.SUN, 16, UITheme.TEXT_MUTED));
        btnThemeToggle.addActionListener(e -> {
            UITheme.applyTheme(!UITheme.isDarkMode);
            btnThemeToggle.setText(UITheme.isDarkMode ? "Dark Mode" : "Light Mode");
            btnThemeToggle.setIcon(FontIcon.of(UITheme.isDarkMode ? Feather.MOON : Feather.SUN, 16, UITheme.TEXT_MUTED));
            SwingUtilities.updateComponentTreeUI(this);
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

    private void showTab(String key) {
        activeTab = key;
        ((CardLayout) contentArea.getLayout()).show(contentArea, key);

        UITheme.setNavActive(btnNavRooms,        "ROOMS".equals(key));
        UITheme.setNavActive(btnNavContact,      "CONTACT".equals(key));
        UITheme.setNavActive(btnNavHistory,      "HISTORY".equals(key));
        UITheme.setNavActive(btnNavTransactions, "TRANSACTIONS".equals(key));
    }

    // ─────────────────────────────────── PANELS ───────────────────────────────────

    private JPanel buildRoomsPanel() {
        JPanel panel = UITheme.createCardPanel("wrap 1, insets 24, gap 16, fill");
        
        JPanel header = new JPanel(new MigLayout("insets 0, gap 4, wrap 1"));
        header.setOpaque(false);
        JLabel title = new JLabel("Phòng Đấu Giá Đang Mở");
        title.setFont(UITheme.fontTitle(24));
        title.setForeground(UITheme.TEXT_PRIMARY);
        lblRoomCount = new JLabel("Đang tải...");
        lblRoomCount.setFont(UITheme.fontBody(14));
        lblRoomCount.setForeground(UITheme.TEXT_HINT);
        header.add(title); 
        header.add(lblRoomCount);
        panel.add(header, "growx");

        roomModel = new DefaultTableModel(new Object[]{"Mã Phòng", "Tiêu Đề", "Moderator", "Trạng Thái"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblRooms = UITheme.styledTable(roomModel);
        panel.add(UITheme.styledScrollPane(tblRooms), "grow, push");

        JPanel bottom = new JPanel(new MigLayout("insets 0", "push[]"));
        bottom.setOpaque(false);
        JButton btnJoinRoom = UITheme.primaryBtn("Tham Gia Phòng", UITheme.ACCENT);
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
        panel.add(bottom, "growx");
        return panel;
    }

    private JPanel buildContactPanel() {
        JPanel panel = UITheme.createCardPanel("wrap 1, insets 24, gap 16, fill");
        panel.add(UITheme.sectionHeader("Liên Hệ Trung Gian", "Nhắn tin và gửi sản phẩm để kiểm duyệt"), "growx");

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerSize(1);
        split.setBorder(null);

        modModel = new DefaultListModel<>();
        listMods = new JList<>(modModel);
        listMods.setBackground(UITheme.BG_ELEVATED);
        listMods.setForeground(UITheme.TEXT_PRIMARY);
        listMods.setFont(UITheme.fontBody(14));
        listMods.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                String mod = value.toString();
                setText((modStatusMap.getOrDefault(mod, false) ? "● " : "○ ") + mod);
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
        listMods.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && listMods.getSelectedValue() != null) {
                String mod = listMods.getSelectedValue();
                submitPanel.setTargetMod(mod);
                chatPanel.setPrivateMode(mod);
            }
        });
        split.setLeftComponent(UITheme.styledScrollPane(listMods));

        JSplitPane rightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        rightSplit.setDividerSize(1);
        rightSplit.setBorder(null);
        rightSplit.setDividerLocation(350);
        submitPanel = new ProductSubmitPanel();
        chatPanel = new ChatPanel();

        JPanel pnlSubmit = new JPanel(new BorderLayout());
        pnlSubmit.setBorder(new TitledBorder("Gửi Sản Phẩm"));
        pnlSubmit.add(submitPanel, BorderLayout.CENTER);

        JPanel pnlChat = new JPanel(new BorderLayout());
        pnlChat.setBorder(new TitledBorder("Chat Với Moderator"));
        pnlChat.add(chatPanel, BorderLayout.CENTER);

        rightSplit.setLeftComponent(pnlSubmit);
        rightSplit.setRightComponent(pnlChat);
        split.setRightComponent(rightSplit);
        split.setDividerLocation(200);
        
        panel.add(split, "grow, push");
        return panel;
    }

    private JPanel buildHistoryPanel() {
        JPanel panel = UITheme.createCardPanel("wrap 1, insets 24, gap 16, fill");
        panel.add(UITheme.sectionHeader("Lịch Sử Giao Dịch", "Các giao dịch đã hoàn tất (đã xác nhận thanh toán)"), "growx");

        historyContainer = new JPanel(new MigLayout("wrap 1, insets 0, gap 12, fillx", "[grow]"));
        historyContainer.setOpaque(false);

        panel.add(UITheme.styledScrollPane(historyContainer), "grow, push");

        JPanel bottom = new JPanel(new MigLayout("insets 0", "push[]"));
        bottom.setOpaque(false);
        JButton btnExport = UITheme.ghostBtn("Xuất Excel", UITheme.TEXT_MUTED);
        btnExport.addActionListener(e -> exportHistoryToExcel());
        bottom.add(btnExport);
        panel.add(bottom, "growx");
        return panel;
    }

    private JPanel buildTransactionsPanel() {
        JPanel panel = UITheme.createCardPanel("wrap 1, insets 24, gap 16, fill");
        panel.add(UITheme.sectionHeader("Giao Dịch Của Tôi", "Các giao dịch đang chờ xác nhận — chat và thống nhất thanh toán tại đây"), "growx");

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerSize(1);
        split.setBorder(null);
        split.setDividerLocation(500); // Tăng từ 350 lên 500 để thông tin không bị che
        split.setResizeWeight(0.6); // Cho phép khu vực bên trái giãn nhiều hơn khi resize

        transactionsContainer = new JPanel(new MigLayout("wrap 1, insets 0, gap 12, fillx", "[grow]"));
        transactionsContainer.setOpaque(false);
        
        split.setLeftComponent(UITheme.styledScrollPane(transactionsContainer));

        partnerChatPanel = new ChatPanel();
        JPanel pnlPartnerChat = new JPanel(new BorderLayout());
        pnlPartnerChat.setBorder(new TitledBorder("Chat Với Đối Tác"));
        pnlPartnerChat.add(partnerChatPanel, BorderLayout.CENTER);
        
        split.setRightComponent(pnlPartnerChat);

        panel.add(split, "grow, push");
        return panel;
    }

    private void exportHistoryToExcel() {
        if (lastHistoryData.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Không có dữ liệu lịch sử để xuất.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Chọn nơi lưu file Excel");
        fileChooser.setSelectedFile(new java.io.File("LichSuGiaoDich.xlsx"));
        
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            java.io.File fileToSave = fileChooser.getSelectedFile();
            
            // Đảm bảo đuôi file là .xlsx
            if (!fileToSave.getName().toLowerCase().endsWith(".xlsx")) {
                fileToSave = new java.io.File(fileToSave.getParentFile(), fileToSave.getName() + ".xlsx");
            }
            
            try (Workbook workbook = new XSSFWorkbook();
                 FileOutputStream out = new FileOutputStream(fileToSave)) {
                
                Sheet sheet = workbook.createSheet("Lịch Sử Giao Dịch");
                
                // Tạo header
                Row headerRow = sheet.createRow(0);
                String[] columns = {"Phòng Giao Dịch", "Tên Sản Phẩm", "Kết Quả"};
                
                CellStyle headerStyle = workbook.createCellStyle();
                org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
                headerFont.setBold(true);
                headerStyle.setFont(headerFont);
                
                for(int i = 0; i < columns.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(columns[i]);
                    cell.setCellStyle(headerStyle);
                }
                
                // Parse dữ liệu từ lastHistoryData (định dạng: "- {roomId} - {pName}: {displayStatus}")
                int rowNum = 1;
                for (String line : lastHistoryData.split("\\n")) {
                    if (line.trim().isEmpty()) continue;
                    
                    String roomId = "";
                    String pName = "";
                    String result = "";
                    
                    if (line.startsWith("- ") && line.contains(" - ") && line.contains(": ")) {
                        try {
                            String temp = line.substring(2); // Bỏ "- "
                            int firstDash = temp.indexOf(" - ");
                            roomId = temp.substring(0, firstDash).trim();
                            
                            temp = temp.substring(firstDash + 3); // Lấy phần sau "- "
                            int firstColon = temp.indexOf(": ");
                            pName = temp.substring(0, firstColon).trim();
                            result = temp.substring(firstColon + 2).trim();
                        } catch (Exception e) {
                            roomId = line; // Fallback
                        }
                    } else {
                        roomId = line.replace("- ", "").trim();
                    }
                    
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(roomId);
                    row.createCell(1).setCellValue(pName);
                    row.createCell(2).setCellValue(result);
                }
                
                sheet.autoSizeColumn(0);
                sheet.autoSizeColumn(1);
                sheet.autoSizeColumn(2);
                
                workbook.write(out);
                JOptionPane.showMessageDialog(this, "Đã xuất file thành công tại:\n" + fileToSave.getAbsolutePath(), "Thành công", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Lỗi khi lưu file: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ─────────────────────────────────── CARD BUILDERS ────────────────────────────

    private void addHistoryCard(String roomId, String productName, String role, String price) {
        JPanel card = new JPanel(new MigLayout("insets 16, gap 16, fill", "[grow][right]", "[center]"));
        card.setBackground(UITheme.BG_ELEVATED);
        card.setBorder(new LineBorder(UITheme.BORDER, 1, true));
        card.putClientProperty("FlatLaf.style", "arc: 12");

        JPanel info = new JPanel(new MigLayout("wrap 1, insets 0, gap 4"));
        info.setOpaque(false);
        JLabel nameLbl = new JLabel(productName);
        nameLbl.setFont(UITheme.fontBold(16));
        nameLbl.setForeground(UITheme.TEXT_PRIMARY);
        info.add(nameLbl);
        
        JLabel detailLbl = new JLabel("Phòng: " + roomId + "  |  " + role + "  |  Giá: " + price + " VND");
        detailLbl.setFont(UITheme.fontBody(13));
        detailLbl.setForeground(UITheme.TEXT_MUTED);
        info.add(detailLbl);
        card.add(info, "cell 0 0, grow");

        JLabel badge = new JLabel("Hoàn tất");
        badge.setIcon(FontIcon.of(Feather.CHECK_CIRCLE, 16, UITheme.SUCCESS));
        badge.setForeground(UITheme.SUCCESS);
        badge.setFont(UITheme.fontBold(13));
        card.add(badge, "cell 1 0");

        historyContainer.add(card, "growx");
    }

    private void addSimpleHistoryCard(String text) {
        JPanel card = new JPanel(new MigLayout("insets 16", "[grow]"));
        card.setBackground(UITheme.BG_ELEVATED);
        card.setBorder(new LineBorder(UITheme.BORDER, 1, true));
        card.putClientProperty("FlatLaf.style", "arc: 12");
        JLabel lbl = new JLabel(text);
        lbl.setFont(UITheme.fontBody(14));
        lbl.setForeground(UITheme.TEXT_PRIMARY);
        card.add(lbl, "growx");
        historyContainer.add(card, "growx");
    }

    // ─────────────────────────────────── MESSAGE HANDLER ──────────────────────────

    @Override
    public void onMessage(MessageType type, Map<String, String> data) {
        SwingUtilities.invokeLater(() -> {
            if (type == MessageType.SUCCESS && data.containsKey("roomId") && "JOINED".equals(data.get("status"))) {
                AuctionRoomPanel rp = new AuctionRoomPanel();
                rp.initRoom(data.get("roomId"), false);
                mainFrame.addPanel(rp, "ROOM_" + data.get("roomId"));
                mainFrame.switchPanel("ROOM_" + data.get("roomId"));

            } else if (type == MessageType.SUCCESS && data.containsKey("message")) {
                String msg = data.get("message");
                // Sau khi mark completed server tự refresh transactions → không cần popup
                if (!msg.contains("hoàn tất") && !msg.contains("giao dịch")) {
                    JOptionPane.showMessageDialog(this, "✅ " + msg, "Thành công", JOptionPane.INFORMATION_MESSAGE);
                }

            } else if (type == MessageType.ERROR && data.containsKey("message")) {
                JOptionPane.showMessageDialog(this, "❌ " + data.get("message"), "Lỗi", JOptionPane.ERROR_MESSAGE);

            } else if (type == MessageType.INVITE_SELLER) {
                String roomId   = data.getOrDefault("roomId", "?");
                String roomTitle = data.getOrDefault("title", roomId);
                String mod       = data.getOrDefault("modName", "Moderator");
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
                        if (p.length >= 5) {
                            roomModel.addRow(new Object[]{p[0], p[1], p[2], p[4]});
                        } else if (p.length >= 4) {
                            roomModel.addRow(new Object[]{p[0], p[1], p[2], p[3]});
                        }
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
                this.lastHistoryData = (h != null) ? h : "";

                if (h == null || h.trim().isEmpty()) {
                    JLabel empty = new JLabel("Chưa có giao dịch hoàn tất nào.");
                    empty.setForeground(UIManager.getColor("Label.disabledForeground"));
                    empty.setBorder(new EmptyBorder(20, 0, 0, 0));
                    historyContainer.add(empty);
                } else {
                    for (String line : h.split("\n")) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("-") && line.equals("-")) continue;
                        // Format: "- roomId | productName | role | Giá: price VND"
                        if (line.startsWith("- ") && line.contains("|")) {
                            String[] parts = line.substring(2).split("\\|", -1);
                            String roomId = parts.length > 0 ? parts[0].trim() : "";
                            String pName  = parts.length > 1 ? parts[1].trim() : line;
                            String role   = parts.length > 2 ? parts[2].trim() : "";
                            String price  = "";
                            if (parts.length > 3) {
                                String priceStr = parts[3].trim();
                                price = priceStr.replace("Giá: ", "").replace(" VND", "");
                            }
                            addHistoryCard(roomId, pName, role, price);
                        } else if (!line.equals("-")) {
                            addSimpleHistoryCard(line.replaceFirst("^- ", ""));
                        }
                    }
                }
                historyContainer.revalidate();
                historyContainer.repaint();

            } else if (type == MessageType.GET_SUCCESSFUL_TRANSACTIONS) {
                transactionsContainer.removeAll();
                String tData = data.get("transactions");

                if (tData == null || tData.trim().isEmpty()) {
                    JLabel empty = new JLabel("Chưa có giao dịch nào đang chờ xử lý.");
                    empty.setForeground(UIManager.getColor("Label.disabledForeground"));
                    empty.setBorder(new EmptyBorder(20, 0, 0, 0));
                    transactionsContainer.add(empty);
                } else {
                    for (String tLine : tData.split("\\|")) {
                        tLine = tLine.trim();
                        if (tLine.isEmpty()) continue;
                        String[] parts = tLine.split(",", -1);
                        // role(0), productId(1), productName(2), partnerId(3), partnerName(4),
                        // partnerPhone(5), partnerEmail(6), finalPrice(7), txStatus(8), sessionProductId(9)
                        if (parts.length >= 10) {
                            String role        = parts[0];
                            String pName       = parts[2];
                            String partnerName = parts[4];
                            String partnerPhone = parts[5];
                            String partnerEmail = parts[6];
                            String price       = parts[7];
                            String txStatus    = parts[8];
                            String spIdStr     = parts[9];

                            boolean isCompleted = "COMPLETED".equals(txStatus);

                            JPanel card = new JPanel(new MigLayout("wrap 1, insets 16, gap 12, fillx"));
                            card.setBackground(UITheme.BG_ELEVATED);
                            card.setBorder(new LineBorder(isCompleted ? UITheme.SUCCESS : UITheme.BORDER, 1, true));
                            card.putClientProperty("FlatLaf.style", "arc: 12");

                            JLabel lblName = new JLabel("[Bán] " + pName);
                            if ("SELLER".equals(role)) {
                                lblName.setIcon(FontIcon.of(Feather.TAG, 16, UITheme.INFO));
                                lblName.setText("[Bán] " + pName);
                            } else {
                                lblName.setIcon(FontIcon.of(Feather.SHOPPING_CART, 16, UITheme.AMBER));
                                lblName.setText("[Mua] " + pName);
                            }
                            lblName.setFont(UITheme.fontBold(16));
                            lblName.setForeground(UITheme.TEXT_PRIMARY);
                            card.add(lblName, "growx");

                            JLabel lblPartner = new JLabel("Đối tác: " + partnerName
                                + "  |  " + partnerPhone
                                + "  |  " + partnerEmail);
                            lblPartner.setFont(UITheme.fontBody(13));
                            lblPartner.setForeground(UITheme.TEXT_MUTED);
                            card.add(lblPartner, "growx");

                            JLabel lblPrice = new JLabel("Giá chốt: " + price + " VND  |  Trạng thái: " + txStatus);
                            lblPrice.setFont(UITheme.fontBody(13));
                            lblPrice.setForeground(UITheme.SUCCESS);
                            card.add(lblPrice, "growx");

                            // Buttons panel
                            JPanel btnPanel = new JPanel(new MigLayout("insets 0, gap 12", "push[][]"));
                            btnPanel.setOpaque(false);

                            // Nút Chat với đối tác
                            JButton btnChat = UITheme.ghostBtn("Chat", UITheme.INFO);
                            btnChat.setIcon(FontIcon.of(Feather.MESSAGE_SQUARE, 16, UITheme.INFO));
                            btnChat.addActionListener(e -> {
                                partnerChatPanel.setPrivateMode(partnerName);
                            });
                            btnPanel.add(btnChat);

                            // Nút Xác nhận đã giao/thanh toán — chỉ hiện cho SELLER và chưa COMPLETED
                            if ("SELLER".equals(role) && !isCompleted) {
                                JButton btnComplete = UITheme.primaryBtn("Xác nhận", UITheme.SUCCESS);
                                btnComplete.setIcon(FontIcon.of(Feather.CHECK, 16, Color.WHITE));
                                btnComplete.addActionListener(e -> {
                                    int confirm = JOptionPane.showConfirmDialog(this,
                                        "<html><b>Xác nhận đã giao hàng / thanh toán xong?</b><br>"
                                        + "Thao tác này sẽ lưu giao dịch vào Lịch Sử<br>và không thể hoàn tác.</html>",
                                        "Xác nhận", JOptionPane.YES_NO_OPTION);
                                    if (confirm == JOptionPane.YES_OPTION) {
                                        Map<String, String> req = new HashMap<>();
                                        req.put("sessionProductId", spIdStr);
                                        NetworkClient.getInstance().sendMessage(MessageType.MARK_TRANSACTION_COMPLETED, req);
                                    }
                                });
                                btnPanel.add(btnComplete);
                            }

                            if (isCompleted) {
                                JLabel done = new JLabel("Hoàn tất");
                                done.setIcon(FontIcon.of(Feather.CHECK_CIRCLE, 16, UITheme.SUCCESS));
                                done.setForeground(UITheme.SUCCESS);
                                done.setFont(UITheme.fontBold(14));
                                btnPanel.add(done);
                            }

                            card.add(btnPanel, "growx, gaptop 8");
                            transactionsContainer.add(card, "growx");
                        }
                    }
                }
                transactionsContainer.revalidate();
                transactionsContainer.repaint();

            } else if (type == MessageType.GET_RATINGS) {
                // Trả về sau khi user query đánh giá mod → xử lý trong dialog riêng
                String targetName = data.getOrDefault("targetName", "");
                String rData = data.get("ratings");
                showModRatingsResult(targetName, rData);
            }
        });
    }

    // ─────────────────────────────────── DIALOG XEM ĐÁNH GIÁ MOD ─────────────────

    /**
     * Dialog để User tìm kiếm và xem đánh giá của Moderator.
     */
    public void openViewModRatingsDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
            "Xem Đánh Giá Moderator", false);
        dialog.setLayout(new net.miginfocom.swing.MigLayout("fill, insets 24"));
        dialog.setSize(600, 600);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(UITheme.BG_DARK);

        JPanel card = UITheme.createCardPanel("wrap 1, insets 24, gap 16, fill");

        // Header: Tìm kiếm
        JPanel searchPanel = new JPanel(new net.miginfocom.swing.MigLayout("insets 0, gap 8", "[][grow][]"));
        searchPanel.setOpaque(false);
        JLabel lblSearch = new JLabel("Tên Moderator:");
        lblSearch.setFont(UITheme.fontBold(13));
        lblSearch.setForeground(UITheme.TEXT_MUTED);
        JTextField tfSearch = UITheme.darkTextField("Nhập tên mod...");
        JButton btnSearch = UITheme.primaryBtn("Tìm Kiếm");

        searchPanel.add(lblSearch);
        searchPanel.add(tfSearch, "growx, h 40!");
        searchPanel.add(btnSearch, "h 40!");
        card.add(searchPanel, "growx");

        // Content
        JPanel resultContainer = new JPanel();
        resultContainer.setLayout(new BoxLayout(resultContainer, BoxLayout.Y_AXIS));
        resultContainer.setOpaque(false);
        JScrollPane sp = UITheme.styledScrollPane(resultContainer);
        sp.setBorder(null); // Inside card
        card.add(sp, "grow, pushy");
        
        dialog.add(card, "grow, push");

        // Khi user nhấn tìm → gửi GET_RATINGS với targetUsername
        Runnable doSearch = () -> {
            String modName = tfSearch.getText().trim();
            if (modName.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Vui lòng nhập tên moderator.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            resultContainer.removeAll();
            JLabel loading = new JLabel("⏳ Đang tìm...");
            loading.setAlignmentX(Component.LEFT_ALIGNMENT);
            resultContainer.add(loading);
            resultContainer.revalidate();
            resultContainer.repaint();

            Map<String, String> req = new HashMap<>();
            req.put("ratedUsername", modName);
            NetworkClient.getInstance().sendMessage(MessageType.GET_RATINGS, req);
        };

        btnSearch.addActionListener(e -> doSearch.run());
        tfSearch.addActionListener(e -> doSearch.run());

        // Kết quả sẽ được hiển thị qua onMessage → GET_RATINGS
        // Lưu resultContainer để onMessage điền vào
        this._currentRatingResultContainer = resultContainer;

        dialog.setVisible(true);
    }

    // Container tạm dùng để onMessage điền kết quả rating vào dialog đang mở
    private JPanel _currentRatingResultContainer = null;

    private void showModRatingsResult(String targetName, String rData) {
        if (_currentRatingResultContainer == null) return;
        JPanel container = _currentRatingResultContainer;
        container.removeAll();

        if (rData == null || rData.trim().isEmpty()) {
            JPanel empty = UITheme.emptyState(org.kordamp.ikonli.feather.Feather.INBOX, 
                "Không có đánh giá", 
                "Moderator \"" + targetName + "\" chưa có đánh giá nào.");
            container.add(empty);
        } else {
            // Header tên mod
            JLabel header = new JLabel("⭐ Đánh giá của Moderator: " + targetName);
            header.setFont(new Font("Segoe UI", Font.BOLD, 15));
            header.setAlignmentX(Component.LEFT_ALIGNMENT);
            header.setBorder(new EmptyBorder(0, 0, 10, 0));
            container.add(header);

            int totalScore = 0;
            int count = 0;
            java.util.List<String[]> rows = new java.util.ArrayList<>();
            for (String line : rData.split("\\|")) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(",", 3);
                if (parts.length >= 2) {
                    rows.add(parts);
                    try { totalScore += Integer.parseInt(parts[1].trim()); count++; } catch(Exception ignored) {}
                }
            }

            if (count > 0) {
                double avg = (double) totalScore / count;
                JLabel avgLabel = new JLabel(String.format("Điểm trung bình: %.1f ⭐  (%d đánh giá)", avg, count));
                avgLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
                avgLabel.setForeground(UIManager.getColor("Actions.Yellow"));
                avgLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                avgLabel.setBorder(new EmptyBorder(0, 0, 8, 0));
                container.add(avgLabel);
                container.add(Box.createVerticalStrut(4));
            }

            for (String[] parts : rows) {
                String fromName = parts[0].trim();
                String scoreStr = parts.length > 1 ? parts[1].trim() : "?";
                String comment  = parts.length > 2 ? parts[2].trim().replace(";", ",").replace("/", "|").trim() : "";

                JPanel card = new JPanel(new BorderLayout(8, 8));
                card.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(UIManager.getColor("Component.borderColor"), 1, true),
                    new EmptyBorder(12, 16, 12, 16)
                ));
                card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
                card.setAlignmentX(Component.LEFT_ALIGNMENT);
                card.setOpaque(false);

                // Header: User + Stars
                JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
                headerPanel.setOpaque(false);
                JLabel lblUser = new JLabel("Từ: " + fromName);
                lblUser.setFont(UITheme.fontBold(13));
                lblUser.setForeground(UITheme.TEXT_PRIMARY);
                headerPanel.add(lblUser);
                headerPanel.add(Box.createHorizontalStrut(8));

                int numScore = 0;
                try { numScore = Integer.parseInt(scoreStr); } catch (Exception ignored) {}
                for (int i = 0; i < 5; i++) {
                    JLabel star = new JLabel();
                    if (i < numScore) {
                        star.setIcon(org.kordamp.ikonli.swing.FontIcon.of(org.kordamp.ikonli.feather.Feather.STAR, 18, UITheme.AMBER));
                    } else {
                        star.setIcon(org.kordamp.ikonli.swing.FontIcon.of(org.kordamp.ikonli.feather.Feather.STAR, 18, UITheme.BORDER));
                    }
                    headerPanel.add(star);
                }
                card.add(headerPanel, BorderLayout.NORTH);

                // Comment field (Read-only view)
                if (comment.isEmpty()) {
                    JLabel lblEmptyComment = new JLabel("Không có nhận xét");
                    lblEmptyComment.setFont(new Font(UITheme.fontBody(13).getName(), Font.ITALIC, 13));
                    lblEmptyComment.setForeground(UITheme.TEXT_HINT);
                    card.add(lblEmptyComment, BorderLayout.CENTER);
                } else {
                    JTextArea txtComment = new JTextArea(comment);
                    txtComment.setLineWrap(true);
                    txtComment.setWrapStyleWord(true);
                    txtComment.setEditable(false);
                    txtComment.setFocusable(false); // No blinking cursor
                    txtComment.setOpaque(false);
                    txtComment.setForeground(UITheme.TEXT_MUTED);
                    txtComment.setFont(UITheme.fontBody(13));
                    card.add(txtComment, BorderLayout.CENTER);
                }

                container.add(card);
                container.add(Box.createVerticalStrut(12));
            }
        }

        container.revalidate();
        container.repaint();
    }

    // ─────────────────────────────────── RATE DIALOG (chỉ đánh giá MOD) ──────────

    public void openRateUserDialogByUsername(String ratedUsername) {
        openRateModDialog("0", ratedUsername, "0");
    }

    public void openRateModDialog(String ratedId, String ratedName, String sessionId) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
            "Đánh giá Moderator: " + ratedName, true);
        dialog.setLayout(new net.miginfocom.swing.MigLayout("fill, insets 24"));
        dialog.setSize(500, 500);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(UITheme.BG_DARK);

        JPanel card = UITheme.createCardPanel("wrap 1, insets 24, gap 16, fillx");

        JLabel lblTitle = new JLabel("Đánh giá Moderator: " + ratedName);
        lblTitle.setFont(UITheme.fontTitle(18));
        lblTitle.setForeground(UITheme.TEXT_PRIMARY);
        card.add(lblTitle);

        JLabel lblRate = new JLabel("Đánh giá:");
        lblRate.setFont(UITheme.fontBold(13));
        lblRate.setForeground(UITheme.TEXT_MUTED);
        card.add(lblRate, "gapbottom 4");

        StarRatingPanel starRating = new StarRatingPanel();
        card.add(starRating, "gapbottom 16");

        JLabel lblComment = new JLabel("Nhận xét (không bắt buộc):");
        lblComment.setFont(UITheme.fontBold(13));
        lblComment.setForeground(UITheme.TEXT_MUTED);
        card.add(lblComment, "gapbottom 4");

        JTextArea txtComment = new JTextArea(4, 30);
        txtComment.setLineWrap(true);
        txtComment.setWrapStyleWord(true);
        txtComment.setFont(UITheme.fontBody(14));
        txtComment.setForeground(UITheme.TEXT_PRIMARY);
        txtComment.setBackground(UITheme.BG_DEEP);
        txtComment.setCaretColor(UITheme.ACCENT);
        txtComment.setBorder(BorderFactory.createCompoundBorder(
            new javax.swing.border.LineBorder(UITheme.BORDER, 1, true),
            new javax.swing.border.EmptyBorder(8, 12, 8, 12)
        ));
        card.add(new JScrollPane(txtComment), "growx, pushy, h 120!");

        JPanel pBtn = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 0));
        pBtn.setOpaque(false);
        JButton btnCancel = UITheme.ghostBtn("Hủy", UITheme.TEXT_MUTED);
        btnCancel.addActionListener(e -> dialog.dispose());
        JButton btnSubmit = UITheme.primaryBtn("Gửi Đánh Giá");
        btnSubmit.addActionListener(e -> {
            Map<String, String> req = new HashMap<>();
            req.put("ratedId", ratedId);
            req.put("ratedUsername", ratedName);
            req.put("sessionId", sessionId);
            req.put("score", String.valueOf(starRating.getRating()));
            req.put("comment", txtComment.getText().trim());
            NetworkClient.getInstance().sendMessage(MessageType.RATE_USER, req);
            dialog.dispose();
        });
        pBtn.add(btnCancel);
        pBtn.add(btnSubmit);
        
        card.add(pBtn, "growx, gaptop 16");

        dialog.add(card, "grow, push");
        dialog.setVisible(true);
    }

    // Kept for backward compat (ChatPanel might call this)
    public void openRateUserDialog(String ratedId, String ratedName, String sessionId) {
        openRateModDialog(ratedId, ratedName, sessionId);
    }
}