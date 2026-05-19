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
    private int currentProductSellerId = 0;

    // ─── Product info (left) ──────────────────────────────────────────────────
    private JLabel lblImage;
    private JLabel lblName;
    private JLabel lblDesc;
    private JLabel lblSeller;
    private JLabel lblStartPrice;
    private JLabel lblProgress; // "Sản phẩm 1/3"

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
            buildProductPanel(), buildBidPanel());
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

        // Flash timer
        flashTimer = new javax.swing.Timer(500, e -> {
            flashState = !flashState;
            lblCountdown.setForeground(flashState ? DANGER : AMBER);
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
    private JPanel buildProductPanel() {
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
        txtBidAmount = new JTextField();
        txtBidAmount.setBackground(BG_CARD);
        txtBidAmount.setForeground(FG);
        txtBidAmount.setCaretColor(ACCENT_L);
        txtBidAmount.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        txtBidAmount.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1, true),
            new EmptyBorder(10, 12, 10, 12)));
        txtBidAmount.addActionListener(e -> placeBid()); // Enter to bid
        p.add(txtBidAmount, g);

        g.gridx = 1; g.weightx = 0;
        btnPlaceBid = new JButton("ĐẶT GIÁ");
        btnPlaceBid.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnPlaceBid.setForeground(Color.WHITE);
        btnPlaceBid.setBackground(ACCENT);
        btnPlaceBid.setFocusPainted(false);
        btnPlaceBid.setBorderPainted(false);
        btnPlaceBid.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnPlaceBid.setBorder(new EmptyBorder(10, 20, 10, 20));
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

        btnExtend = new JButton("+30s Gia Han");
        styleBtn(btnExtend, new Color(20,60,40), SUCCESS);
        btnExtend.addActionListener(e ->
            NetworkClient.getInstance().sendMessage(MessageType.EXTEND_TIME, Map.of("roomId", roomId)));

        btnNextProduct = new JButton(">> Chuyen SP");
        styleBtn(btnNextProduct, new Color(40,40,20), AMBER);
        btnNextProduct.addActionListener(e -> {
            int c = JOptionPane.showConfirmDialog(this, "Bỏ qua sản phẩm hiện tại?", "Xác nhận", JOptionPane.YES_NO_OPTION);
            if (c == JOptionPane.YES_OPTION)
                NetworkClient.getInstance().sendMessage(MessageType.NEXT_PRODUCT, Map.of("roomId", roomId));
        });

        btnCloseRoom = new JButton("[ ] Dong Phong");
        styleBtn(btnCloseRoom, new Color(60,15,15), DANGER);
        btnCloseRoom.addActionListener(e -> {
            int c = JOptionPane.showConfirmDialog(this, "Đóng phòng đấu giá?", "Xác nhận", JOptionPane.YES_NO_OPTION);
            if (c == JOptionPane.YES_OPTION)
                NetworkClient.getInstance().sendMessage(MessageType.CLOSE_ROOM, Map.of("roomId", roomId));
        });

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
                case BID_UPDATE:       handleBidUpdate(data); break;
                case AUCTION_END:      handleAuctionEnd(data); break;
                case ROOM_STATUS_UPDATE:
                    if ("CLOSED".equals(data.get("status"))) handleRoomClosed();
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
        flashTimer.stop();
        lblCountdown.setForeground(AMBER);

        // Product info
        String name   = data.getOrDefault("productName", "Sản phẩm #" + data.get("productId"));
        String desc   = data.getOrDefault("description", "");
        String seller = data.getOrDefault("sellerName", "—");
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

        txtBidHistory.append("\n--- San pham moi: " + name + " ---\n");
        txtBidHistory.append("Gia khoi diem: " + formatVnd(price) + "\n");

        // Load image
        String img64 = data.get("imageData");
        if (img64 != null && !img64.isEmpty()) {
            try {
                byte[] b = Base64.getDecoder().decode(img64);
                BufferedImage bi = ImageIO.read(new ByteArrayInputStream(b));
                if (bi != null) {
                    Image scaled = bi.getScaledInstance(240, 200, Image.SCALE_SMOOTH);
                    lblImage.setIcon(new ImageIcon(scaled));
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
    }

    private void handleBidUpdate(Map<String, String> data) {
        String bid    = data.getOrDefault("highestBid", "0");
        String bidder = data.getOrDefault("bidderName", "");
        String secStr = data.getOrDefault("remainingSeconds", "0");
        int sec;
        try { sec = Integer.parseInt(secStr); } catch (Exception e) { sec = 0; }

        lblHighestBid.setText(formatVnd(bid));
        if (!bidder.isEmpty()) {
            lblBidder.setText("Người đặt cao nhất: " + bidder);
            if (!bidder.equals(myUsername) && myHighestBid.compareTo(java.math.BigDecimal.ZERO) > 0) {
                txtBidHistory.append("[!] Gia ban bi vuot boi " + bidder + "\n");
            }
            txtBidHistory.append(bidder + " đặt: " + formatVnd(bid) + "\n");
            scrollHistory();
        }

        lblCountdown.setText("Con lai: " + sec + "s");
        if (sec <= 10 && sec > 0) {
            if (!flashTimer.isRunning()) flashTimer.start();
        } else {
            flashTimer.stop();
            lblCountdown.setForeground(sec <= 30 ? AMBER : SUCCESS);
        }
        if (sec == 0) lblCountdown.setText("⏱ Đang chốt...");
    }

    private void handleAuctionEnd(Map<String, String> data) {
        flashTimer.stop();
        String winner = data.getOrDefault("winnerName", "Không có người mua");
        String price  = data.getOrDefault("finalPrice", "0");
        String pname  = data.getOrDefault("productName", "");

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
            NetworkClient.getInstance().sendMessage(MessageType.CONFIRM_BUY, res);
            if (c == JOptionPane.YES_OPTION)
                JOptionPane.showMessageDialog(this, "✅ Cảm ơn bạn đã mua hàng!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
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
        b.setBorderPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
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
}
