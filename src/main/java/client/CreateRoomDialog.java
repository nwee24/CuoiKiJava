package client;

import shared.MessageType;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * CreateRoomDialog — Dialog tạo phòng đấu giá.
 * Phong cách sáng đồng nhất với Dashboard.
 */
public class CreateRoomDialog extends JDialog implements NetworkClient.MessageListener {

    // ── Palette ───────────────────────────────────────────────────────────────
    private static final Color BG_DIALOG    = UITheme.BG_DARK;
    private static final Color BG_CARD      = UITheme.BG_CARD;
    private static final Color BG_INPUT     = UITheme.BG_DEEP;
    private static final Color BORDER_CLR   = UITheme.BORDER;
    private static final Color ORANGE       = UITheme.ACCENT;
    private static final Color ORANGE_DARK  = UITheme.ACCENT_DARK;
    private static final Color ORANGE_SOFT  = UITheme.BG_ROW_ALT;
    private static final Color ORANGE_RING  = UITheme.BORDER_LIGHT;
    private static final Color TEXT_DARK    = UITheme.TEXT_PRIMARY;
    private static final Color TEXT_MED     = UITheme.TEXT_MUTED;
    private static final Color TEXT_SOFT    = UITheme.TEXT_HINT;
    private static final Color SUCCESS      = UITheme.SUCCESS;
    private static final Color SUCCESS_SOFT = UITheme.BG_ROW_ALT;
    private static final Color DANGER       = UITheme.DANGER;
    private static final Color TAG_BG       = UITheme.BG_ROW_ALT;

    // ── State ─────────────────────────────────────────────────────────────────
    private final List<String> selectedSellers = new ArrayList<>();
    private final Map<String, List<ProductInfo>> sellerProductsMap = new HashMap<>();
    private final List<Integer> selectedProductIds = new ArrayList<>();
    private boolean confirmed = false;
    
    // ProductInfo helper class
    private static class ProductInfo {
        int id;
        String name;
        String description;
        String startingPrice;
        ProductInfo(int id, String name, String desc, String price) {
            this.id = id; this.name = name; this.description = desc; this.startingPrice = price;
        }
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    private JTextField txtRoomId;
    private JTextField txtRoomTitle;
    private JTextArea  txtDescription;
    private JTextField txtSearchUser;
    private DefaultListModel<String> availableUsersModel;
    private JList<String> listAvailable;
    private Map<String, Boolean> userStatusMap = new HashMap<>();
    private JPanel tagsPanel;
    private JLabel lblSellerCount;
    private JButton btnCreate;

    public CreateRoomDialog(Window owner) {
        super(owner, "Tạo Phòng Đấu Giá Mới", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBackground(BG_DIALOG);

        buildUI();
        pack();
        setMinimumSize(new Dimension(720, 580));
        setPreferredSize(new Dimension(760, 600));
        setLocationRelativeTo(owner);

        NetworkClient.getInstance().addListener(this);
        NetworkClient.getInstance().sendMessage(MessageType.GET_USER_LIST, null);
    }

    // ─── Build full UI ────────────────────────────────────────────────────────
    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_DIALOG);

        root.add(buildTitleBar(), BorderLayout.NORTH);

        JPanel body = new JPanel(new GridLayout(1, 2, 16, 0));
        body.setBackground(BG_DIALOG);
        body.setBorder(new EmptyBorder(20, 20, 16, 20));
        body.add(buildLeftPanel());
        body.add(buildRightPanel());
        root.add(body, BorderLayout.CENTER);

        root.add(buildFooter(), BorderLayout.SOUTH);
        setContentPane(root);
    }

    // ─── Title Bar ────────────────────────────────────────────────────────────
    private JPanel buildTitleBar() {
        JPanel bar = new JPanel(new BorderLayout(0, 2));
        bar.setBackground(BG_CARD);
        bar.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, BORDER_CLR),
            new EmptyBorder(16, 24, 16, 24)
        ));

        // Icon + title
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);

        JPanel iconBox = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ORANGE_SOFT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(ORANGE_RING);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
                g2.dispose(); super.paintComponent(g);
            }
        };
        iconBox.setOpaque(false);
        iconBox.setPreferredSize(new Dimension(40, 40));
        JLabel iconLbl = new JLabel("🏛");
        iconLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        iconBox.add(iconLbl);
        left.add(iconBox);

        JPanel titleStack = new JPanel();
        titleStack.setLayout(new BoxLayout(titleStack, BoxLayout.Y_AXIS));
        titleStack.setOpaque(false);
        JLabel title = new JLabel("Tao Phong Dau Gia Moi");
        title.setFont(new Font("Segoe UI", Font.BOLD, 17));
        title.setForeground(TEXT_DARK);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel sub = new JLabel("Cau hinh phong va moi Seller tham gia dau gia");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(TEXT_SOFT);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleStack.add(title); titleStack.add(Box.createVerticalStrut(2)); titleStack.add(sub);
        left.add(titleStack);
        bar.add(left, BorderLayout.WEST);

        // Close hint
        JLabel hint = new JLabel("Nhan Huy hoac dong de thoat  ");
        hint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        hint.setForeground(TEXT_SOFT);
        bar.add(hint, BorderLayout.EAST);
        return bar;
    }

    // ─── Left: Room Info ──────────────────────────────────────────────────────
    private JPanel buildLeftPanel() {
        JPanel card = buildCard("Thong Tin Phong");

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill  = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 12, 0);
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        // Mã phòng
        gbc.gridy = 0; form.add(fieldLabel("Ma Phong *"), gbc);
        gbc.gridy = 1;
        txtRoomId = makeInput("Vi du: ROOM_001");
        txtRoomId.setText("ROOM_" + String.format("%04d", new Random().nextInt(9999) + 1));
        form.add(txtRoomId, gbc);

        // Tiêu đề
        gbc.gridy = 2; form.add(fieldLabel("Tieu De Phong *"), gbc);
        gbc.gridy = 3;
        txtRoomTitle = makeInput("Vi du: Phien Dau Gia Do Co Thang 6");
        form.add(txtRoomTitle, gbc);

        // Mô tả
        gbc.gridy = 4; form.add(fieldLabel("Mo Ta"), gbc);
        gbc.gridy = 5; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.BOTH;
        txtDescription = new JTextArea(4, 1);
        txtDescription.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtDescription.setForeground(TEXT_DARK);
        txtDescription.setBackground(BG_INPUT);
        txtDescription.setCaretColor(ORANGE);
        txtDescription.setLineWrap(true);
        txtDescription.setWrapStyleWord(true);
        txtDescription.putClientProperty("JTextField.placeholderText", "Mô tả chi tiết về phiên đấu giá...");
        txtDescription.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_CLR, 1, true), new EmptyBorder(8, 12, 8, 12)));
        JScrollPane descScroll = new JScrollPane(txtDescription);
        descScroll.setBorder(null);
        descScroll.setBackground(BG_INPUT);
        descScroll.getViewport().setBackground(BG_INPUT);
        form.add(descScroll, gbc);

        // Info box
        gbc.gridy = 6; gbc.weighty = 0; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 0, 0, 0);
        form.add(buildInfoBox(), gbc);

        card.add(form, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildInfoBox() {
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBackground(new Color(239, 246, 255));
        box.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(191, 219, 254), 1, true),
            new EmptyBorder(10, 14, 10, 14)
        ));

        String[] infos = {
            "Ma phong phai duy nhat trong he thong",
            "Seller nhan loi moi ngay khi phong duoc tao",
            "Phien dau gia bat dau khi ban kich hoat"
        };
        for (String info : infos) {
            JLabel lbl = new JLabel(info);
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            lbl.setForeground(new Color(59, 130, 246));
            lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            lbl.setBorder(new EmptyBorder(1, 0, 1, 0));
            box.add(lbl);
        }
        return box;
    }

    // ─── Right: Seller Selection ──────────────────────────────────────────────
    private JPanel buildRightPanel() {
        JPanel card = buildCard("Moi Seller Tham Gia");
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setOpaque(false);

        // Search bar
        JPanel searchBar = new JPanel(new BorderLayout(8, 0));
        searchBar.setOpaque(false);
        txtSearchUser = new JTextField();
        txtSearchUser.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtSearchUser.setForeground(TEXT_DARK);
        txtSearchUser.setBackground(BG_INPUT);
        txtSearchUser.setCaretColor(ORANGE);
        txtSearchUser.putClientProperty("JTextField.placeholderText", "Tim kiem nguoi dung...");
        txtSearchUser.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_CLR, 1, true), new EmptyBorder(8, 12, 8, 12)));
        txtSearchUser.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) { /* filter handled in onMessage */ }
        });
        searchBar.add(txtSearchUser, BorderLayout.CENTER);

        JButton btnRefresh = buildIconBtn("⟳");
        btnRefresh.setToolTipText("Làm mới danh sách");
        btnRefresh.addActionListener(e -> NetworkClient.getInstance().sendMessage(MessageType.GET_USER_LIST, null));
        searchBar.add(btnRefresh, BorderLayout.EAST);
        content.add(searchBar, BorderLayout.NORTH);

        // Available user list
        JPanel listSection = new JPanel(new BorderLayout(0, 8));
        listSection.setOpaque(false);
        JLabel lblAvail = fieldLabel("Danh Sach Nguoi Dung:");
        listSection.add(lblAvail, BorderLayout.NORTH);

        availableUsersModel = new DefaultListModel<>();
        listAvailable = new JList<>(availableUsersModel);
        listAvailable.setBackground(BG_CARD);
        listAvailable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        listAvailable.setFixedCellHeight(42);
        listAvailable.setSelectionBackground(ORANGE_SOFT);
        listAvailable.setSelectionForeground(ORANGE_DARK);
        listAvailable.setBorder(new EmptyBorder(4, 6, 4, 6));
        listAvailable.setCellRenderer(new UserListRenderer());
        listAvailable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String sel = listAvailable.getSelectedValue();
                    if (sel != null) addSeller(sel);
                }
            }
        });
        JScrollPane listScroll = new JScrollPane(listAvailable);
        listScroll.setBorder(new LineBorder(BORDER_CLR, 1, true));
        listScroll.getViewport().setBackground(BG_CARD);
        listSection.add(listScroll, BorderLayout.CENTER);

        JButton btnAdd = buildSuccessBtn("+ Them Seller Da Chon");
        btnAdd.addActionListener(e -> {
            for (String v : listAvailable.getSelectedValuesList()) addSeller(v);
        });
        listSection.add(btnAdd, BorderLayout.SOUTH);
        content.add(listSection, BorderLayout.CENTER);

        // Selected sellers tags
        JPanel bottomSec = new JPanel(new BorderLayout(0, 8));
        bottomSec.setOpaque(false);
        bottomSec.add(fieldLabel("Seller Da Chon:"), BorderLayout.NORTH);

        tagsPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 6, 4));
        tagsPanel.setBackground(BG_INPUT);
        tagsPanel.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_CLR, 1, true), new EmptyBorder(8, 8, 8, 8)));
        tagsPanel.setPreferredSize(new Dimension(0, 84));
        bottomSec.add(tagsPanel, BorderLayout.CENTER);
        content.add(bottomSec, BorderLayout.SOUTH);

        card.add(content, BorderLayout.CENTER);
        return card;
    }

    // ─── Footer ───────────────────────────────────────────────────────────────
    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(12, 0));
        footer.setBackground(BG_CARD);
        footer.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(1, 0, 0, 0, BORDER_CLR),
            new EmptyBorder(14, 24, 14, 24)
        ));

        lblSellerCount = new JLabel("Đã chọn: 0 seller");
        lblSellerCount.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSellerCount.setForeground(TEXT_SOFT);
        footer.add(lblSellerCount, BorderLayout.WEST);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnRow.setOpaque(false);

        JButton btnCancel = buildOutlineBtn("Hủy");
        btnCancel.addActionListener(e -> dispose());

        btnCreate = buildPrimaryBtn("Tạo Phòng & Gửi Lời Mời");
        btnCreate.addActionListener(e -> onSelectProducts());

        btnRow.add(btnCancel);
        btnRow.add(btnCreate);
        footer.add(btnRow, BorderLayout.EAST);
        return footer;
    }

    // ─── Seller management ────────────────────────────────────────────────────
    private void addSeller(String username) {
        if (!selectedSellers.contains(username)) {
            selectedSellers.add(username);
            refreshTags();
        }
    }

    private void removeSeller(String username) {
        selectedSellers.remove(username);
        refreshTags();
    }

    private void refreshTags() {
        tagsPanel.removeAll();
        for (String seller : selectedSellers) tagsPanel.add(buildTag(seller));
        tagsPanel.revalidate(); tagsPanel.repaint();
        int n = selectedSellers.size();
        lblSellerCount.setText("Đã chọn: " + n + " seller" +
            (n == 0 ? "" : ": " + String.join(", ", selectedSellers)));
        lblSellerCount.setForeground(n > 0 ? ORANGE : TEXT_SOFT);
    }

    private JPanel buildTag(String name) {
        JPanel tag = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        tag.setBackground(ORANGE_SOFT);
        tag.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(ORANGE_RING, 1, true), new EmptyBorder(1, 6, 1, 4)));

        JLabel lbl = new JLabel("👤 " + name);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(ORANGE_DARK);

        JButton btnX = new JButton("✕");
        btnX.setFont(new Font("Segoe UI", Font.BOLD, 10));
        btnX.setForeground(new Color(180, 80, 0));
        btnX.setBackground(ORANGE_SOFT);
        btnX.setBorderPainted(false);
        btnX.setFocusPainted(false);
        btnX.setContentAreaFilled(false);
        btnX.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnX.setMargin(new Insets(0, 2, 0, 2));
        btnX.addActionListener(e -> removeSeller(name));

        tag.add(lbl); tag.add(btnX);
        return tag;
    }

    // ─── Confirm trực tiếp (không cần chọn seller/SP bắt buộc) ─────────────────
    private void onSelectProducts() {
        String roomId = txtRoomId.getText().trim();
        String title  = txtRoomTitle.getText().trim();

        if (roomId.isEmpty()) { shake(txtRoomId); showError("Mã phòng không được để trống!"); return; }
        if (!roomId.matches("[A-Za-z0-9_\\-]+")) { shake(txtRoomId); showError("Mã phòng chỉ được chứa chữ, số, '_' và '-'."); return; }
        if (title.isEmpty())  { shake(txtRoomTitle); showError("Tiêu đề phòng không được để trống!"); return; }
        // Không bắt buộc chọn seller — tạo phòng thẳng
        onConfirm();
    }

    // ─── Confirm ─────────────────────────────────────────────────────────────
    private void onConfirm() {
        Map<String, String> data = new HashMap<>();
        data.put("roomId",      txtRoomId.getText().trim());
        data.put("title",       txtRoomTitle.getText().trim());
        data.put("description", txtDescription.getText().trim());
        data.put("sellers",     String.join(",", selectedSellers));
        // productIds để trống — mod sẽ chọn sản phẩm sau khi vào phòng
        data.put("productIds", "");

        NetworkClient.getInstance().sendMessage(MessageType.CREATE_ROOM_WITH_SELLERS, data);
        confirmed = true;
        dispose();
    }

    private void shake(JComponent comp) {
        Point original = comp.getLocation();
        javax.swing.Timer timer = new javax.swing.Timer(30, null);
        int[] step = {0};
        int[] offsets = {-8, 8, -6, 6, -4, 4, -2, 2, 0};
        timer.addActionListener(e -> {
            if (step[0] < offsets.length) comp.setLocation(original.x + offsets[step[0]++], original.y);
            else { comp.setLocation(original); timer.stop(); }
        });
        timer.start();
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    // ─── NetworkClient ────────────────────────────────────────────────────────
    @Override
    public void onMessage(MessageType type, Map<String, String> data) {
        SwingUtilities.invokeLater(() -> {
            if (type == MessageType.GET_USER_LIST) {
                String list = data.get("userList");
                availableUsersModel.clear();
                userStatusMap.clear();
                if (list != null && !list.isEmpty()) {
                    for (String u : list.split(",")) {
                        if (!u.isBlank()) {
                            String[] parts = u.split(":");
                            String name = parts[0].trim();
                            userStatusMap.put(name, parts.length > 1 ? "1".equals(parts[1]) : false);
                            availableUsersModel.addElement(name);
                        }
                    }
                }
            } else if (type == MessageType.GET_SELLER_PRODUCTS) {
                // Format: sellerUsername|productId,name,price|productId,name,price|...
                String sellerUsername = data.get("sellerUsername");
                String productsStr = data.get("products");
                
                List<ProductInfo> products = new ArrayList<>();
                if (productsStr != null && !productsStr.isEmpty()) {
                    for (String prodStr : productsStr.split("\\|")) {
                        String[] parts = prodStr.split(",", 4); // id,name,desc,price
                        if (parts.length >= 4) {
                            products.add(new ProductInfo(
                                Integer.parseInt(parts[0]),
                                parts[1],
                                parts[2],
                                parts[3]
                            ));
                        }
                    }
                }
                sellerProductsMap.put(sellerUsername, products);
            }
        });
    }

    public boolean isConfirmed() { return confirmed; }
    public String getRoomId() { return txtRoomId != null ? txtRoomId.getText().trim() : ""; }
    public List<String> getSelectedSellers() { return Collections.unmodifiableList(selectedSellers); }

    // ─── UI Helpers ───────────────────────────────────────────────────────────
    private JPanel buildCard(String title) {
        // Returns a wrapper that routes add() to inner content panel
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(BG_CARD);
        card.setBorder(new LineBorder(BORDER_CLR, 1, true));

        JPanel cardHeader = new JPanel(new BorderLayout());
        cardHeader.setBackground(new Color(249, 250, 251));
        cardHeader.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, BORDER_CLR), new EmptyBorder(11, 14, 11, 14)));
        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(TEXT_MED);
        cardHeader.add(lbl);
        card.add(cardHeader, BorderLayout.NORTH);

        JPanel inner = new JPanel(new BorderLayout());
        inner.setOpaque(false);
        inner.setBorder(new EmptyBorder(14, 16, 14, 16));
        card.add(inner, BorderLayout.CENTER);

        return new CardWrapper(card, inner);
    }

    private JLabel fieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl.setForeground(TEXT_MED);
        lbl.setBorder(new EmptyBorder(0, 0, 4, 0));
        return lbl;
    }

    private JTextField makeInput(String placeholder) {
        JTextField tf = new JTextField();
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tf.setForeground(TEXT_DARK);
        tf.setBackground(BG_INPUT);
        tf.setCaretColor(ORANGE);
        tf.putClientProperty("JTextField.placeholderText", placeholder);
        tf.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_CLR, 1, true), new EmptyBorder(8, 12, 8, 12)));
        tf.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                tf.setBorder(BorderFactory.createCompoundBorder(new LineBorder(ORANGE_RING, 1, true), new EmptyBorder(8, 12, 8, 12)));
            }
            public void focusLost(FocusEvent e) {
                tf.setBorder(BorderFactory.createCompoundBorder(new LineBorder(BORDER_CLR, 1, true), new EmptyBorder(8, 12, 8, 12)));
            }
        });
        return tf;
    }

    private JButton buildPrimaryBtn(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? ORANGE_DARK : ORANGE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                super.paintComponent(g); g2.dispose();
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13)); btn.setForeground(Color.WHITE);
        btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR)); btn.setBorder(new EmptyBorder(11, 22, 11, 22));
        return btn;
    }

    private JButton buildOutlineBtn(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 13)); btn.setForeground(TEXT_MED);
        btn.setBackground(BG_CARD); btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_CLR, 1, true), new EmptyBorder(10, 20, 10, 20)));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(ORANGE_SOFT); }
            public void mouseExited(MouseEvent e)  { btn.setBackground(BG_CARD); }
        });
        return btn;
    }

    private JButton buildSuccessBtn(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? SUCCESS.darker() : SUCCESS);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                super.paintComponent(g); g2.dispose();
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12)); btn.setForeground(Color.WHITE);
        btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR)); btn.setBorder(new EmptyBorder(9, 16, 9, 16));
        return btn;
    }

    private JButton buildIconBtn(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 15)); btn.setForeground(TEXT_MED);
        btn.setBackground(BG_CARD); btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(38, 38));
        btn.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_CLR, 1, true), new EmptyBorder(6, 8, 6, 8)));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(ORANGE_SOFT); }
            public void mouseExited(MouseEvent e)  { btn.setBackground(BG_CARD); }
        });
        return btn;
    }

    // ─── Renderers ────────────────────────────────────────────────────────────
    private class UserListRenderer extends DefaultListCellRenderer {
        @Override public Component getListCellRendererComponent(JList<?> list, Object value,
                int idx, boolean sel, boolean foc) {
            JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, idx, sel, foc);
            boolean online = userStatusMap.getOrDefault(value.toString(), false);
            lbl.setText((online ? "  ●  " : "  ○  ") + value);
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            lbl.setForeground(sel ? ORANGE : (online ? TEXT_DARK : TEXT_SOFT));
            lbl.setBackground(sel ? ORANGE_SOFT : BG_CARD);
            lbl.setBorder(new EmptyBorder(6, 6, 6, 6));
            return lbl;
        }
    }

    // ─── CardWrapper ──────────────────────────────────────────────────────────
    private static class CardWrapper extends JPanel {
        private final JPanel inner;
        CardWrapper(JPanel card, JPanel inner) {
            super(new BorderLayout());
            this.inner = inner;
            setOpaque(false);
            super.add(card, BorderLayout.CENTER);
        }
        @Override public Component add(Component comp) { inner.add(comp, BorderLayout.CENTER); return comp; }
        @Override public void add(Component comp, Object constraints) { inner.add(comp, constraints); }
    }

    // ─── WrapLayout ───────────────────────────────────────────────────────────
    static class WrapLayout extends FlowLayout {
        public WrapLayout(int align, int hgap, int vgap) { super(align, hgap, vgap); }

        @Override public Dimension preferredLayoutSize(Container target) { return layoutSize(target, true); }
        @Override public Dimension minimumLayoutSize(Container target)   { return layoutSize(target, false); }

        private Dimension layoutSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int targetWidth = target.getSize().width;
                if (targetWidth == 0) targetWidth = Integer.MAX_VALUE;
                int hgap = getHgap(), vgap = getVgap();
                Insets insets = target.getInsets();
                int maxWidth = targetWidth - insets.left - insets.right - hgap * 2;
                Dimension dim = new Dimension(0, 0);
                int rowWidth = 0, rowHeight = 0;
                for (int i = 0; i < target.getComponentCount(); i++) {
                    Component m = target.getComponent(i);
                    if (m.isVisible()) {
                        Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                        if (rowWidth + d.width > maxWidth) {
                            dim.width = Math.max(dim.width, rowWidth);
                            dim.height += rowHeight + vgap;
                            rowWidth = 0; rowHeight = 0;
                        }
                        rowWidth += d.width + hgap;
                        rowHeight = Math.max(rowHeight, d.height);
                    }
                }
                dim.width = Math.max(dim.width, rowWidth);
                dim.height += rowHeight + insets.top + insets.bottom + vgap * 2;
                return dim;
            }
        }
    }
}
