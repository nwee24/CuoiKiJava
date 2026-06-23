package client;

import shared.MessageType;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.table.DefaultTableCellRenderer;

public class AuctionRoomPanel extends JPanel implements NetworkClient.MessageListener {

    // ─── Theme ────────────────────────────────────────────────────────────────
    private static final Color BG       = new Color(13, 20, 36);
    private static final Color BG_CARD  = new Color(22, 33, 55);
    private static final Color BG_PANEL = new Color(18, 27, 46);
    private static final Color ACCENT   = new Color(99, 102, 241);
    private static final Color ACCENT_L = new Color(129, 140, 248);
    private static final Color SUCCESS  = new Color(52, 211, 153);
    private static final Color DANGER   = new Color(248, 113, 113);
    private static final Color AMBER    = new Color(251, 191, 36);
    private static final Color FG       = new Color(248, 250, 252);
    private static final Color MUTED    = new Color(148, 163, 184);
    private static final Color BORDER   = new Color(51, 65, 85);
    private static final Font  FONT_UI  = new Font("Segoe UI", Font.PLAIN, 13);

    // ─── State ────────────────────────────────────────────────────────────────
    private String roomId;
    private final String myUsername;
    private java.math.BigDecimal myHighestBid = java.math.BigDecimal.ZERO;
    private boolean isModerator = false;
    private javax.swing.Timer flashTimer;
    private boolean flashState = false;

    private String lastBidAmount = "";
    private String lastBidder = "";
    private int currentProductSellerId = 0;

    // ─── Local countdown (chạy mỗi giây, sync với server) ────────────────────
    private int localCountdown = 0;
    private javax.swing.Timer countdownTimer;

    // ─── Product info (left) ──────────────────────────────────────────────────
    private JLabel lblImage;
    private JLabel lblName;
    private JLabel lblDesc;
    private JLabel lblSeller;
    private JLabel lblStartPrice;
    private JLabel lblProgress; // "Sản phẩm 1/3"
    
    private JTable tblProducts;
    private javax.swing.table.DefaultTableModel productListModel;
    private String currentSellerName = "";

    // ─── Bid area (center) ────────────────────────────────────────────────────
    private JLabel lblHighestBid;
    private JLabel lblCountdown;
    private JLabel lblBidder;
    private JTextField txtBidAmount;
    private JButton btnPlaceBid;
    private JButton btnBack;

    // ─── Mod controls ─────────────────────────────────────────────────────────
    private JPanel modControlPanel;
    private JButton btnExtend, btnNextProduct, btnCloseRoom;

    // ─── Bid history (bottom-left) ─────────────────────────────────────────────
    private JTextArea txtBidHistory;

    // ─── Chat (bottom-right) ──────────────────────────────────────────────────
    private ChatPanel chatPanel;

    public AuctionRoomPanel() {
        myUsername = NetworkClient.getInstance().getCurrentUsername();
        setBackground(BG);
        setLayout(new BorderLayout(0, 0));
        buildUI();
    }

    private void buildUI() {
        // ── Top Bar ──────────────────────────────────────────────────────────
        add(buildTopBar(), BorderLayout.NORTH);

        // ── Main split: Left (product) | Center (bid) ────────────────────────
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            buildLeftTabs(), buildBidPanel());
        mainSplit.setDividerLocation(280);
        mainSplit.setDividerSize(4);
        mainSplit.setBorder(null);
        mainSplit.setBackground(BG);

        // ── Bottom split: History | Chat ─────────────────────────────────────
        JSplitPane bottomSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            buildHistoryPanel(), buildChatPanel());
        bottomSplit.setResizeWeight(0.4);
        bottomSplit.setDividerSize(4);
        bottomSplit.setBorder(null);
        bottomSplit.setBackground(BG);

        JSplitPane vertSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, mainSplit, bottomSplit);
        vertSplit.setResizeWeight(0.6);
        vertSplit.setDividerSize(4);
        vertSplit.setBorder(null);
        vertSplit.setBackground(BG);

        add(vertSplit, BorderLayout.CENTER);

        // Flash timer (nhấp nháy màu khi ≤10 giây)
        flashTimer = new javax.swing.Timer(500, e -> {
            flashState = !flashState;
            lblCountdown.setForeground(flashState ? DANGER : AMBER);
            lblHighestBid.setForeground(flashState ? Color.WHITE : SUCCESS);
        });

        // Countdown timer: đếm ngược cục bộ mỗi giây, sync với server qua BID_UPDATE
        countdownTimer = new javax.swing.Timer(1000, e -> {
            if (localCountdown > 0) {
                localCountdown--;
                updateCountdownDisplay(localCountdown);
            }
        });
    }

    // ─── Top Bar ─────────────────────────────────────────────────────────────
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setBackground(BG_CARD);
        bar.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0,0,1,0,BORDER), new EmptyBorder(10,16,10,16)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        JLabel ico = new JLabel("[A]");
        ico.setFont(new Font("Segoe UI", Font.BOLD, 16));
        ico.setForeground(ACCENT_L);
        lblProgress = new JLabel("Phòng đấu giá");
        lblProgress.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblProgress.setForeground(FG);
        left.add(ico);
        left.add(lblProgress);

        btnBack = new JButton("< Quay lai");
        styleBtn(btnBack, BG_PANEL, MUTED);
        btnBack.addActionListener(e -> {
            leaveRoom();
            // Tìm MainFrame cha và quay về dashboard
            Window w = SwingUtilities.getWindowAncestor(this);
            if (w instanceof JFrame) {
                JPanel parent = (JPanel) getParent();
                if (parent != null) {
                    CardLayout cl = (CardLayout) parent.getLayout();
                    cl.show(parent, myUsername != null && isModerator ? "MOD_DASHBOARD" : "USER_DASHBOARD");
                }
            }
        });

        bar.add(left, BorderLayout.WEST);
        bar.add(btnBack, BorderLayout.EAST);
        return bar;
    }

    // ─── Product Panel (left) ─────────────────────────────────────────────────
    private JTabbedPane buildLeftTabs() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(BG_PANEL);
        tabbedPane.setForeground(FG);
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabbedPane.setBorder(new MatteBorder(0,0,0,1,BORDER));
        
        tabbedPane.addTab("Chi tiết", buildProductDetailPanel());
        tabbedPane.addTab("Danh sách", buildProductListPanel());
        
        return tabbedPane;
    }

    private JPanel buildProductDetailPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(BG_PANEL);
        p.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0,0,0,1,BORDER), new EmptyBorder(14,12,12,12)));
        p.setPreferredSize(new Dimension(268, 0));

        // Image
        lblImage = new JLabel("Chưa có sản phẩm", SwingConstants.CENTER);
        lblImage.setForeground(MUTED);
        lblImage.setFont(FONT_UI);
        lblImage.setOpaque(true);
        lblImage.setBackground(BG_CARD);
        lblImage.setBorder(BorderFactory.createLineBorder(BORDER, 1, true));
        lblImage.setPreferredSize(new Dimension(240, 200));
        lblImage.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        lblImage.setMinimumSize(new Dimension(0, 200));
        lblImage.setAlignmentX(CENTER_ALIGNMENT);
        p.add(lblImage);
        p.add(Box.createVerticalStrut(10));

        lblName = mkLabel("Chờ bắt đầu...", Font.BOLD, 16, FG);
        lblName.setAlignmentX(LEFT_ALIGNMENT);
        p.add(lblName);
        p.add(Box.createVerticalStrut(6));

        lblDesc = new JLabel("<html><p style='width:220px'>—</p></html>");
        lblDesc.setFont(FONT_UI);
        lblDesc.setForeground(MUTED);
        lblDesc.setAlignmentX(LEFT_ALIGNMENT);
        p.add(lblDesc);
        p.add(Box.createVerticalStrut(6));

        lblSeller = mkLabel("Người bán: —", Font.PLAIN, 13, MUTED);
        lblSeller.setAlignmentX(LEFT_ALIGNMENT);
        p.add(lblSeller);
        p.add(Box.createVerticalStrut(4));

        lblStartPrice = mkLabel("Giá khởi điểm: —", Font.PLAIN, 13, MUTED);
        lblStartPrice.setAlignmentX(LEFT_ALIGNMENT);
        p.add(lblStartPrice);
        p.add(Box.createVerticalGlue());
        return p;
    }

    private JPanel buildProductListPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG_PANEL);
        p.setBorder(new EmptyBorder(10, 10, 10, 10));

        String[] cols = {"ID", "Tên Sản Phẩm", "Trạng Thái"};
        productListModel = new javax.swing.table.DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tblProducts = new JTable(productListModel);
        tblProducts.setBackground(BG_CARD);
        tblProducts.setForeground(FG);
        tblProducts.setFont(FONT_UI);
        tblProducts.setRowHeight(25);
        tblProducts.getTableHeader().setBackground(BG_PANEL);
        tblProducts.getTableHeader().setForeground(FG);
        tblProducts.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        tblProducts.getColumnModel().getColumn(0).setMaxWidth(40);
        
        // Custom renderer for Status column
        tblProducts.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String status = (String) value;
                if ("CURRENT".equals(status)) {
                    c.setForeground(AMBER);
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                } else if ("ENDED".equals(status)) {
                    c.setForeground(MUTED);
                } else {
                    c.setForeground(FG);
                }
                return c;
            }
        });

        JScrollPane sp = new JScrollPane(tblProducts);
        sp.setBorder(BorderFactory.createLineBorder(BORDER));
        sp.getViewport().setBackground(BG_CARD);
        p.add(sp, BorderLayout.CENTER);
        
        return p;
    }

    // ─── Bid Panel (center) ───────────────────────────────────────────────────
    private JPanel buildBidPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(BG_PANEL);
        p.setBorder(new EmptyBorder(20, 24, 20, 24));

        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.gridwidth = 2; g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(4, 4, 4, 4);

        // Price
        lblHighestBid = new JLabel("0 VND", SwingConstants.CENTER);
        lblHighestBid.setFont(new Font("Segoe UI", Font.BOLD, 36));
        lblHighestBid.setForeground(SUCCESS);
        g.gridy = 0;
        p.add(lblHighestBid, g);

        // Bidder
        lblBidder = new JLabel("Chưa có người đặt giá", SwingConstants.CENTER);
        lblBidder.setFont(FONT_UI);
        lblBidder.setForeground(MUTED);
        g.gridy = 1;
        p.add(lblBidder, g);

        // Countdown
        lblCountdown = new JLabel("Cho bat dau...", SwingConstants.CENTER);
        lblCountdown.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblCountdown.setForeground(AMBER);
        g.gridy = 2; g.insets = new Insets(14, 4, 14, 4);
        p.add(lblCountdown, g);

        // Separator
        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER);
        g.gridy = 3; g.insets = new Insets(4, 4, 16, 4);
        p.add(sep, g);

        // Bid input label
        JLabel lblInput = new JLabel("Nhập giá của bạn (VND):");
        lblInput.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblInput.setForeground(MUTED);
        g.gridy = 4; g.insets = new Insets(4, 4, 4, 4);
        p.add(lblInput, g);

        // Input + Button row
        g.gridy = 5; g.gridwidth = 1; g.weightx = 1.0;
        txtBidAmount = UITheme.darkTextField("Nhập số tiền...");
        txtBidAmount.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        txtBidAmount.addActionListener(e -> placeBid()); // Enter to bid
        p.add(txtBidAmount, g);

        g.gridx = 1; g.weightx = 0;
        btnPlaceBid = UITheme.primaryBtn("ĐẶT GIÁ");
        btnPlaceBid.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnPlaceBid.addActionListener(e -> placeBid());
        p.add(btnPlaceBid, g);

        // Mod controls
        g.gridx = 0; g.gridy = 6; g.gridwidth = 2; g.insets = new Insets(20, 4, 4, 4);
        modControlPanel = buildModControls();
        modControlPanel.setVisible(false);
        p.add(modControlPanel, g);

        return p;
    }

    private JPanel buildModControls() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        p.setBackground(BG_PANEL);
        p.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(1,0,0,0,BORDER), new EmptyBorder(12, 0, 0, 0)));

        JButton btnStart = new JButton("▶ Bat Dau");
        styleBtn(btnStart, new Color(20,40,80), new Color(100,150,255));
        btnStart.addActionListener(e -> {
            Map<String, String> params = new HashMap<>();
            params.put("roomId", roomId);
            NetworkClient.getInstance().sendMessage(MessageType.OPEN_AUCTION, params);
        });

        btnExtend = new JButton("+30s Gia Han");
        styleBtn(btnExtend, new Color(20,60,40), SUCCESS);
        btnExtend.addActionListener(e -> {
            Map<String, String> params = new HashMap<>();
            params.put("roomId", roomId);
            NetworkClient.getInstance().sendMessage(MessageType.EXTEND_TIME, params);
        });

        btnNextProduct = new JButton(">> Chuyen SP");
        styleBtn(btnNextProduct, new Color(40,40,20), AMBER);
        btnNextProduct.addActionListener(e -> {
            int c = JOptionPane.showConfirmDialog(this, "Bỏ qua sản phẩm hiện tại?", "Xác nhận", JOptionPane.YES_NO_OPTION);
            if (c == JOptionPane.YES_OPTION) {
                Map<String, String> params = new HashMap<>();
                params.put("roomId", roomId);
                NetworkClient.getInstance().sendMessage(MessageType.NEXT_PRODUCT, params);
            }
        });

        btnCloseRoom = new JButton("[ ] Dong Phong");
        styleBtn(btnCloseRoom, new Color(60,15,15), DANGER);
        btnCloseRoom.addActionListener(e -> {
            int c = JOptionPane.showConfirmDialog(this, "Đóng phòng đấu giá?", "Xác nhận", JOptionPane.YES_NO_OPTION);
            if (c == JOptionPane.YES_OPTION) {
                Map<String, String> params = new HashMap<>();
                params.put("roomId", roomId);
                NetworkClient.getInstance().sendMessage(MessageType.CLOSE_ROOM, params);
            }
        });

        // Nút thêm sản phẩm (Moderator only)
        JButton btnAddProduct = new JButton("+ Them SP");
        styleBtn(btnAddProduct, new Color(40,20,60), new Color(167,139,250));
        btnAddProduct.addActionListener(e -> openAddProductDialog());
        
        p.add(btnStart);
        p.add(btnAddProduct);
        p.add(btnExtend);
        p.add(btnNextProduct);
        p.add(btnCloseRoom);
        return p;
    }

    // ─── History Panel ────────────────────────────────────────────────────────
    private JPanel buildHistoryPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG_PANEL);
        p.setBorder(new MatteBorder(1,0,0,0,BORDER));

        JLabel title = new JLabel("  Lich Su Dat Gia");
        title.setFont(new Font("Segoe UI", Font.BOLD, 12));
        title.setForeground(MUTED);
        title.setBackground(BG_CARD);
        title.setOpaque(true);
        title.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0,0,1,0,BORDER), new EmptyBorder(8,10,8,10)));
        p.add(title, BorderLayout.NORTH);

        txtBidHistory = new JTextArea();
        txtBidHistory.setEditable(false);
        txtBidHistory.setBackground(BG_PANEL);
        txtBidHistory.setForeground(FG);
        txtBidHistory.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtBidHistory.setMargin(new Insets(8, 10, 8, 10));
        txtBidHistory.setLineWrap(true);
        txtBidHistory.setWrapStyleWord(true);
        JScrollPane sp = new JScrollPane(txtBidHistory);
        sp.setBorder(null);
        sp.getViewport().setBackground(BG_PANEL);
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    // ─── Chat Panel ───────────────────────────────────────────────────────────
    private JPanel buildChatPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG_PANEL);
        p.setBorder(new MatteBorder(1,1,0,0,BORDER));

        JLabel title = new JLabel("  Chat Phong");
        title.setFont(new Font("Segoe UI", Font.BOLD, 12));
        title.setForeground(MUTED);
        title.setBackground(BG_CARD);
        title.setOpaque(true);
        title.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0,0,1,0,BORDER), new EmptyBorder(8,10,8,10)));
        p.add(title, BorderLayout.NORTH);

        chatPanel = new ChatPanel();
        p.add(chatPanel, BorderLayout.CENTER);
        return p;
    }

    // ─── Init ─────────────────────────────────────────────────────────────────
    public void initRoom(String roomId, boolean isModerator) {
        this.roomId = roomId;
        this.isModerator = isModerator;
        chatPanel.setRoomMode(roomId);
        modControlPanel.setVisible(isModerator);
        lblProgress.setText("Phòng: " + roomId);
        NetworkClient.getInstance().addListener(this);
        Map<String, String> d = new HashMap<>();
        d.put("roomId", roomId);
        NetworkClient.getInstance().sendMessage(MessageType.GET_ROOM_DETAIL, d);
    }

    public void leaveRoom() {
        if (flashTimer != null) flashTimer.stop();
        if (countdownTimer != null) countdownTimer.stop();
        NetworkClient.getInstance().removeListener(this);
        Map<String, String> d = new HashMap<>();
        d.put("roomId", roomId);
        NetworkClient.getInstance().sendMessage(MessageType.LEAVE_ROOM, d);
    }

    // ─── Place Bid ────────────────────────────────────────────────────────────
    private void placeBid() {
        String raw = txtBidAmount.getText().trim().replace(",", "").replace(".", "");
        if (raw.isEmpty()) return;
        try {
            java.math.BigDecimal amt = new java.math.BigDecimal(raw);
            Map<String, String> data = new HashMap<>();
            data.put("roomId", roomId);
            data.put("amount", amt.toString());
            data.put("productSellerId", String.valueOf(currentProductSellerId));
            NetworkClient.getInstance().sendMessage(MessageType.PLACE_BID, data);
            myHighestBid = amt;
            txtBidAmount.setText("");
            txtBidAmount.requestFocus();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập số tiền hợp lệ (VD: 1500000)", "Lỗi", JOptionPane.WARNING_MESSAGE);
        }
    }

    // ─── Message Handler ─────────────────────────────────────────────────────
    @Override
    public void onMessage(MessageType type, Map<String, String> data) {
        // Filter by roomId (except non-room messages)
        String msgRoom = data.get("roomId");
        if (msgRoom != null && !msgRoom.equals(roomId)) return;

        SwingUtilities.invokeLater(() -> {
            switch (type) {
                case PRODUCT_CHANGE:   handleProductChange(data); break;
                case ROOM_PRODUCT_LIST_UPDATE: handleRoomProductListUpdate(data); break;
                case BID_UPDATE:       handleBidUpdate(data); break;
                case AUCTION_END:      handleAuctionEnd(data); break;
                case ROOM_STATUS_UPDATE:
                    if ("CLOSED".equals(data.get("status"))) {
                        handleRoomClosed();
                    } else if ("WAITING_FOR_PRODUCTS".equals(data.get("status"))) {
                        handleWaitingForProducts();
                    }
                    break;
                case ERROR:
                    JOptionPane.showMessageDialog(this, "Loi: " + data.get("message"),
                        "Loi", JOptionPane.ERROR_MESSAGE);
                    break;
                default: break;
            }
        });
    }

    private void handleProductChange(Map<String, String> data) {
        // Dừng hết timer cũ, reset trạng thái
        flashTimer.stop();
        countdownTimer.stop();
        lblCountdown.setForeground(AMBER);

        // Product info
        String name   = data.getOrDefault("productName", "Sản phẩm #" + data.get("productId"));
        String desc   = data.getOrDefault("description", "");
        String seller = data.getOrDefault("sellerName", "—");
        currentSellerName = seller;
        String price  = data.getOrDefault("startingPrice", "0");
        String idx    = data.getOrDefault("productIndex", "?");
        String total  = data.getOrDefault("totalProducts", "?");

        lblName.setText(name);
        lblDesc.setText("<html><p style='width:210px'>" + (desc.isEmpty() ? "—" : desc) + "</p></html>");
        lblSeller.setText("Người bán: " + seller);
        lblStartPrice.setText("Giá khởi điểm: " + formatVnd(price));
        lblProgress.setText("Phòng: " + roomId + "  |  SP " + idx + "/" + total);
        lblHighestBid.setText(formatVnd(price));
        lblBidder.setText("Chưa có người đặt giá");

        // Disable bid if own product
        boolean isOwn = seller.equals(myUsername);
        btnPlaceBid.setEnabled(!isOwn);
        btnPlaceBid.setText(isOwn ? "Sản phẩm của bạn" : "ĐẶT GIÁ");
        btnPlaceBid.setBackground(isOwn ? new Color(50, 50, 70) : ACCENT);
        myHighestBid = java.math.BigDecimal.ZERO;
        lastBidAmount = price;
        lastBidder = "";

        txtBidHistory.append("\n--- San pham moi: " + name + " ---\n");
        txtBidHistory.append("Gia khoi diem: " + formatVnd(price) + "\n");

        // Khởi động đếm ngược từ giá trị mặc định (server sẽ sync ngay qua BID_UPDATE)
        localCountdown = 30;
        lblCountdown.setText("30");
        lblCountdown.setForeground(AMBER);
        countdownTimer.start();

        String img64 = data.get("imageData");
        if (img64 != null && !img64.isEmpty()) {
            try {
                String firstImage = img64.split(";;;")[0];
                byte[] b = Base64.getDecoder().decode(firstImage);
                BufferedImage bi = ImageIO.read(new ByteArrayInputStream(b));
                if (bi != null) {
                    BufferedImage rounded = new BufferedImage(240, 200, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2 = rounded.createGraphics();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setClip(new java.awt.geom.RoundRectangle2D.Float(0, 0, 240, 200, 16, 16));
                    g2.drawImage(bi, 0, 0, 240, 200, null);
                    g2.dispose();
                    lblImage.setIcon(new ImageIcon(rounded));
                    lblImage.setText("");
                }
            } catch (Exception ignored) {
                lblImage.setIcon(null);
                lblImage.setText("Không tải được ảnh");
            }
        } else {
            lblImage.setIcon(null);
            lblImage.setText("Không có ảnh");
        }
        
        // Update product list
        String rpStr = data.get("roomProducts");
        updateProductListModel(rpStr);
    }

    private void handleRoomProductListUpdate(Map<String, String> data) {
        String rpStr = data.get("roomProducts");
        updateProductListModel(rpStr);
    }
    
    private void updateProductListModel(String rpStr) {
        if (rpStr != null && !rpStr.isEmpty() && productListModel != null) {
            productListModel.setRowCount(0);
            for (String pStr : rpStr.split("\\|")) {
                String[] pParts = pStr.split(",", -1);
                if (pParts.length >= 3) {
                    productListModel.addRow(new Object[]{pParts[0], pParts[1].replace(";", ",").replace("/", "|"), pParts[2]});
                }
            }
        }
    }

    private void handleBidUpdate(Map<String, String> data) {
        String bid    = data.getOrDefault("highestBid", "0");
        String bidder = data.getOrDefault("bidderName", "");
        String secStr = data.getOrDefault("remainingSeconds", "0");
        int sec;
        try { sec = Integer.parseInt(secStr); } catch (Exception e) { sec = 0; }

        lblHighestBid.setText(formatVnd(bid));
        boolean isNewBid = !bid.equals(lastBidAmount) || !bidder.equals(lastBidder);
        if (!bidder.isEmpty()) {
            lblBidder.setText("Người đặt cao nhất: " + bidder);
            if (isNewBid) {
                if (!bidder.equals(myUsername) && myHighestBid.compareTo(java.math.BigDecimal.ZERO) > 0) {
                    txtBidHistory.append("[!] Gia ban bi vuot boi " + bidder + "\n");
                }
                txtBidHistory.append(bidder + " đặt: " + formatVnd(bid) + "\n");
                scrollHistory();
                lastBidAmount = bid;
                lastBidder = bidder;
            }
        }

        // Sync countdown từ server (tránh lệch do network delay)
        localCountdown = sec;
        if (sec > 0 && !countdownTimer.isRunning()) {
            countdownTimer.start();
        }
        updateCountdownDisplay(sec);
    }

    /** Cập nhật label đếm ngược + hiệu ứng nhấp nháy */
    private void updateCountdownDisplay(int sec) {
        if (sec > 10) {
            flashTimer.stop();
            lblCountdown.setForeground(AMBER);
            lblCountdown.setText(String.valueOf(sec));
        } else if (sec > 0) {
            lblCountdown.setText(String.valueOf(sec));
            if (!flashTimer.isRunning()) flashTimer.start();
        } else {
            countdownTimer.stop();
            flashTimer.stop();
            lblCountdown.setText("0");
            lblCountdown.setForeground(DANGER);
        }
    }

    private void handleAuctionEnd(Map<String, String> data) {
        flashTimer.stop();
        countdownTimer.stop();
        String winner = data.getOrDefault("winnerName", "Không có người mua");
        String price  = data.getOrDefault("finalPrice", "0");
        String pname  = data.getOrDefault("productName", "");
        String sessionProductId = data.get("sessionProductId");

        lblCountdown.setText("Da chot gia");
        lblCountdown.setForeground(SUCCESS);
        txtBidHistory.append("\n[OK] Chot: " + formatVnd(price) + " --- " + winner + "\n");
        scrollHistory();
        btnPlaceBid.setEnabled(false);

        if (myUsername != null && myUsername.equals(winner)) {
            int c = JOptionPane.showConfirmDialog(this,
                "🎉 Bạn đã thắng đấu giá!\n\n"
                + "Sản phẩm: " + pname + "\n"
                + "Giá: " + formatVnd(price) + " VND\n\n"
                + "Xác nhận mua hàng? (Từ chối sẽ bị phạt 10%)",
                "Xác nhận mua hàng", JOptionPane.YES_NO_OPTION);
            Map<String, String> res = new HashMap<>();
            res.put("roomId", roomId);
            res.put("status", c == JOptionPane.YES_OPTION ? "ACCEPTED" : "REJECTED");
            res.put("finalPrice", price);
            if (sessionProductId != null) {
                res.put("sessionProductId", sessionProductId);
            }
            NetworkClient.getInstance().sendMessage(MessageType.CONFIRM_BUY, res);
            if (c == JOptionPane.YES_OPTION)
                JOptionPane.showMessageDialog(this, "✅ Cảm ơn bạn đã mua hàng!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
        } else if (myUsername != null && myUsername.equals(currentSellerName)) {
            if ("Không có người mua".equals(winner)) {
                JOptionPane.showMessageDialog(this, 
                    "Sản phẩm \"" + pname + "\" của bạn không có ai đặt giá và đã bị bỏ qua.", 
                    "Kết quả đấu giá", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, 
                    "🎉 Sản phẩm \"" + pname + "\" của bạn đã được bán!\n\n"
                    + "Người mua: " + winner + "\n"
                    + "Giá chốt: " + formatVnd(price) + " VND", 
                    "Bán thành công", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private void handleRoomClosed() {
        flashTimer.stop();
        JOptionPane.showMessageDialog(this, "Phòng đấu giá đã kết thúc.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        leaveRoom();
        // Quay về dashboard
        Container parent = getParent();
        if (parent != null && parent.getLayout() instanceof CardLayout) {
            ((CardLayout) parent.getLayout()).show(parent, isModerator ? "MOD_DASHBOARD" : "USER_DASHBOARD");
        }
    }

    private void handleWaitingForProducts() {
        flashTimer.stop();
        countdownTimer.stop();
        lblCountdown.setText("Chờ thêm sản phẩm");
        lblCountdown.setForeground(AMBER);
        
        lblName.setText("Đang chờ...");
        lblDesc.setText("<html><p style='width:210px'>Phòng đấu giá hiện tại đã hết sản phẩm. Vui lòng đợi Moderator thêm sản phẩm mới hoặc kết thúc phòng.</p></html>");
        lblSeller.setText("Người bán: —");
        lblStartPrice.setText("Giá khởi điểm: —");
        lblProgress.setText("Phòng: " + roomId + "  |  Chờ SP");
        lblHighestBid.setText("0 VND");
        lblBidder.setText("Chưa có người đặt giá");
        
        btnPlaceBid.setEnabled(false);
        txtBidHistory.append("\n[!] Hệ thống: Đang chờ sản phẩm mới...\n");
        scrollHistory();
        
        lblImage.setIcon(null);
        lblImage.setText("Đang chờ sản phẩm");
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────
    private JLabel mkLabel(String t, int style, int sz, Color c) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("Segoe UI", style, sz));
        l.setForeground(c);
        return l;
    }

    private void styleBtn(JButton b, Color bg, Color fg) {
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setForeground(fg);
        b.setBackground(bg);
        b.setFocusPainted(false);
        b.setBorderPainted(true);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.putClientProperty("JButton.buttonType", "roundRect");
        b.setBorder(new EmptyBorder(8, 16, 8, 16));
    }

    private String formatVnd(String raw) {
        try {
            long v = new java.math.BigDecimal(raw).longValue();
            return String.format("%,d VND", v).replace(',', '.');
        } catch (Exception e) { return raw + " VND"; }
    }

    private void scrollHistory() {
        txtBidHistory.setCaretPosition(txtBidHistory.getDocument().getLength());
    }

    // ─── Add Product Dialog (Mod Only) ────────────────────────────────────────
    private void openAddProductDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), 
            "Chọn Sản Phẩm Để Thêm Vào Phòng", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(750, 500);
        dialog.setLocationRelativeTo(this);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(BG_PANEL);

        JLabel lblTitle = new JLabel("Chọn sản phẩm và chỉnh sửa giá khởi điểm (nếu cần)");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTitle.setForeground(FG);
        lblTitle.setBorder(new EmptyBorder(0, 0, 10, 0));
        mainPanel.add(lblTitle, BorderLayout.NORTH);

        // Product table
        String[] columnNames = {"Chọn", "ID", "Tên Sản Phẩm", "Người Bán", "Giá Khởi Điểm (VND)"};
        javax.swing.table.DefaultTableModel tableModel = new javax.swing.table.DefaultTableModel(columnNames, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) return Boolean.class;
                return String.class;
            }
            @Override
            public boolean isCellEditable(int row, int column) {
                if (column == 0 || column == 4) {
                    String name = (String) getValueAt(row, 2);
                    return !name.endsWith("(Đã thêm)");
                }
                return false;
            }
        };
        
        JTable productTable = new JTable(tableModel);
        productTable.setBackground(BG_CARD);
        productTable.setForeground(FG);
        productTable.setFont(FONT_UI);
        productTable.setRowHeight(30);
        productTable.getTableHeader().setBackground(BG_PANEL);
        productTable.getTableHeader().setForeground(FG);
        productTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        
        productTable.getColumnModel().getColumn(0).setMaxWidth(50);
        productTable.getColumnModel().getColumn(1).setMaxWidth(50);

        JScrollPane scrollPane = new JScrollPane(productTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        JButton btnCancel = UITheme.secondaryBtn("Hủy");
        btnCancel.addActionListener(ev -> dialog.dispose());

        JButton btnAdd = UITheme.primaryBtn("Thêm Sản Phẩm");
        btnAdd.addActionListener(ev -> {
            if (productTable.isEditing()) productTable.getCellEditor().stopCellEditing();
            
            StringBuilder productIds = new StringBuilder();
            StringBuilder customPrices = new StringBuilder();
            int count = 0;
            
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                Boolean isSelected = (Boolean) tableModel.getValueAt(i, 0);
                String name = (String) tableModel.getValueAt(i, 2);
                if (isSelected != null && isSelected && !name.endsWith("(Đã thêm)")) {
                    if (count > 0) {
                        productIds.append(",");
                        customPrices.append(",");
                    }
                    productIds.append(tableModel.getValueAt(i, 1).toString());
                    String priceInput = tableModel.getValueAt(i, 4).toString().replace(".", "").replace(",", "");
                    customPrices.append(priceInput);
                    count++;
                }
            }
            
            if (count == 0) {
                JOptionPane.showMessageDialog(dialog, "Vui lòng chọn ít nhất một sản phẩm!", "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Map<String, String> data = new HashMap<>();
            data.put("roomId", roomId);
            data.put("productIds", productIds.toString());
            data.put("customPrices", customPrices.toString());
            NetworkClient.getInstance().sendMessage(MessageType.ADD_PRODUCT_TO_SESSION, data);
            
            JOptionPane.showMessageDialog(dialog, "✅ Đã thêm " + count + " sản phẩm!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            dialog.dispose();
        });

        btnPanel.add(btnCancel);
        btnPanel.add(btnAdd);
        mainPanel.add(btnPanel, BorderLayout.SOUTH);

        dialog.add(mainPanel);

        // Fetch products
        Map<String, String> request = new HashMap<>();
        request.put("roomId", roomId);
        
        NetworkClient.MessageListener tempListener = new NetworkClient.MessageListener() {
            @Override
            public void onMessage(MessageType type, Map<String, String> data) {
                if (type == MessageType.GET_APPROVED_PRODUCTS_IN_ROOM && roomId.equals(data.get("roomId"))) {
                    SwingUtilities.invokeLater(() -> {
                        String productsStr = data.get("productList");
                        tableModel.setRowCount(0);
                        
                        if (productsStr != null && !productsStr.isEmpty()) {
                            String[] products = productsStr.split("\\|");
                            for (String productStr : products) {
                                String[] parts = productStr.split(",", -1);
                                if (parts.length >= 4) {
                                    String id = parts[0];
                                    String name = parts[1].replace(";", ",");
                                    String price = parts[2];
                                    String seller = parts[3];
                                    // By default we only fetch APPROVED, so isAdded is false
                                    tableModel.addRow(new Object[]{false, id, name, seller, price});
                                }
                            }
                        } else {
                            JOptionPane.showMessageDialog(dialog, "Chưa có sản phẩm nào được duyệt. Vui lòng duyệt sản phẩm trước khi thêm vào phòng.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                        }
                        NetworkClient.getInstance().removeListener(this);
                    });
                }
            }
        };
        
        NetworkClient.getInstance().addListener(tempListener);
        NetworkClient.getInstance().sendMessage(MessageType.GET_APPROVED_PRODUCTS_IN_ROOM, request);
        
        dialog.setVisible(true);
    }

}