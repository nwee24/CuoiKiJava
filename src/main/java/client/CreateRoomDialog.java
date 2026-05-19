package client;

import shared.MessageType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.Timer;

/**
 * CreateRoomDialog - Dialog tạo phòng đấu giá mới.
 * Cho phép Moderator:
 *  - Đặt tên phòng và mô tả
 *  - Thêm nhiều Seller vào phòng (bằng cách chọn từ danh sách user online)
 *  - Xác nhận tạo phòng và gửi lời mời đến các Seller được chọn
 */
public class CreateRoomDialog extends JDialog implements NetworkClient.MessageListener {

    // ─── Theme ─────────────────────────────────────────────────────────────────
    private static final Color BG_DARK      = new Color(15, 23, 42);
    private static final Color BG_CARD      = new Color(30, 41, 59);
    private static final Color BG_INPUT     = new Color(22, 32, 52);
    private static final Color ACCENT       = new Color(99, 102, 241);
    private static final Color ACCENT_LIGHT = new Color(129, 140, 248);
    private static final Color TEXT_PRIMARY = new Color(248, 250, 252);
    private static final Color TEXT_MUTED   = new Color(148, 163, 184);
    private static final Color BORDER       = new Color(51, 65, 85);
    private static final Color SUCCESS      = new Color(52, 211, 153);
    private static final Color DANGER       = new Color(248, 113, 113);
    private static final Color AMBER        = new Color(251, 191, 36);
    private static final Color TAG_BG       = new Color(49, 46, 129);

    // ─── State ─────────────────────────────────────────────────────────────────
    private final List<String> selectedSellers = new ArrayList<>();
    private boolean confirmed = false;

    // ─── UI ────────────────────────────────────────────────────────────────────
    private JTextField txtRoomId;
    private JTextField txtRoomTitle;
    private JTextArea txtDescription;
    private JTextField txtSearchUser;
    private DefaultListModel<String> availableUsersModel;
    private JList<String> listAvailable;
    private JPanel tagsPanel;
    private JLabel lblSellerCount;
    private JButton btnCreate;

    public CreateRoomDialog(Window owner) {
        super(owner, "Tạo Phòng Đấu Giá Mới", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBackground(BG_DARK);
        getRootPane().putClientProperty("apple.awt.windowTitleVisible", false);

        buildUI();
        pack();
        setMinimumSize(new Dimension(680, 560));
        setLocationRelativeTo(owner);

        // Lắng nghe danh sách user từ server
        NetworkClient.getInstance().addListener(this);
        // Yêu cầu server gửi danh sách User (không phải Mod/Admin)
        NetworkClient.getInstance().sendMessage(MessageType.GET_USER_LIST, null);
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_DARK);

        // ── Title Bar ──────────────────────────────────────────────────────────
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(new Color(22, 32, 52));
        titleBar.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, BORDER),
            new EmptyBorder(16, 24, 16, 24)
        ));

        JLabel lblTitle = new JLabel("🏛 Tạo Phòng Đấu Giá Mới");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(TEXT_PRIMARY);
        titleBar.add(lblTitle, BorderLayout.WEST);

        JLabel lblSub = new JLabel("Cấu hình phòng và mời Seller tham gia");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(TEXT_MUTED);
        titleBar.add(lblSub, BorderLayout.EAST);

        root.add(titleBar, BorderLayout.NORTH);

        // ── Body ──────────────────────────────────────────────────────────────
        JPanel body = new JPanel(new GridLayout(1, 2, 16, 0));
        body.setBackground(BG_DARK);
        body.setBorder(new EmptyBorder(20, 20, 16, 20));

        body.add(buildLeftPanel());
        body.add(buildRightPanel());
        root.add(body, BorderLayout.CENTER);

        // ── Footer ────────────────────────────────────────────────────────────
        JPanel footer = new JPanel(new BorderLayout(12, 0));
        footer.setBackground(new Color(18, 26, 42));
        footer.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(1, 0, 0, 0, BORDER),
            new EmptyBorder(14, 24, 14, 24)
        ));

        lblSellerCount = new JLabel("Đã chọn: 0 seller");
        lblSellerCount.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSellerCount.setForeground(TEXT_MUTED);
        footer.add(lblSellerCount, BorderLayout.WEST);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnRow.setOpaque(false);

        JButton btnCancel = createSecondaryBtn("Hủy");
        btnCancel.addActionListener(e -> dispose());

        btnCreate = createPrimaryBtn("✓ Tạo Phòng & Gửi Lời Mời");
        btnCreate.addActionListener(e -> onConfirm());

        btnRow.add(btnCancel);
        btnRow.add(btnCreate);
        footer.add(btnRow, BorderLayout.EAST);
        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);
    }

    // ─── Left Panel: Room Info ─────────────────────────────────────────────────
    private JPanel buildLeftPanel() {
        JPanel panel = buildCard("📋 Thông Tin Phòng");

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 14, 0);
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        // Room ID
        gbc.gridy = 0;
        form.add(makeLabel("Mã Phòng *"), gbc);
        gbc.gridy = 1;
        txtRoomId = makeTextField("VD: ROOM_2024_VIP");
        // Auto-generate
        txtRoomId.setText("ROOM_" + String.format("%04d", new Random().nextInt(9999) + 1));
        form.add(txtRoomId, gbc);

        // Room Title
        gbc.gridy = 2;
        form.add(makeLabel("Tiêu Đề Phòng *"), gbc);
        gbc.gridy = 3;
        txtRoomTitle = makeTextField("VD: Phiên Đấu Giá Đồ Cổ Tháng 5");
        form.add(txtRoomTitle, gbc);

        // Description
        gbc.gridy = 4;
        form.add(makeLabel("Mô Tả"), gbc);
        gbc.gridy = 5;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        txtDescription = new JTextArea(4, 1);
        txtDescription.setBackground(BG_INPUT);
        txtDescription.setForeground(TEXT_PRIMARY);
        txtDescription.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtDescription.setCaretColor(ACCENT_LIGHT);
        txtDescription.setLineWrap(true);
        txtDescription.setWrapStyleWord(true);
        txtDescription.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1, true),
            new EmptyBorder(8, 12, 8, 12)
        ));
        JScrollPane descScroll = new JScrollPane(txtDescription);
        descScroll.setBorder(null);
        descScroll.setBackground(BG_INPUT);
        form.add(descScroll, gbc);

        // Info boxes
        gbc.gridy = 6;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 0, 0, 0);
        JPanel infoBox = buildInfoBox();
        form.add(infoBox, gbc);

        panel.add(form, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildInfoBox() {
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBackground(new Color(17, 26, 44));
        box.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(37, 99, 235, 100), 1, true),
            new EmptyBorder(10, 12, 10, 12)
        ));

        String[] infos = {
            "ℹ️  Mã phòng phải duy nhất trong hệ thống",
            "📨  Seller sẽ nhận lời mời ngay khi phòng tạo",
            "⏱️  Phiên đấu giá bắt đầu khi bạn kích hoạt"
        };
        for (String info : infos) {
            JLabel lbl = new JLabel(info);
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            lbl.setForeground(new Color(147, 197, 253));
            lbl.setBorder(new EmptyBorder(2, 0, 2, 0));
            box.add(lbl);
        }
        return box;
    }

    // ─── Right Panel: Seller Selection ────────────────────────────────────────
    private JPanel buildRightPanel() {
        JPanel panel = buildCard("👥 Mời Seller Tham Gia");

        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setOpaque(false);

        // Search bar
        JPanel searchBar = new JPanel(new BorderLayout(6, 0));
        searchBar.setOpaque(false);

        txtSearchUser = new JTextField();
        txtSearchUser.setBackground(BG_INPUT);
        txtSearchUser.setForeground(TEXT_PRIMARY);
        txtSearchUser.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtSearchUser.setCaretColor(ACCENT_LIGHT);
        txtSearchUser.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1, true),
            new EmptyBorder(7, 12, 7, 12)
        ));
        txtSearchUser.putClientProperty("Placeholder", "🔍 Tìm kiếm người dùng...");

        txtSearchUser.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) { filterUsers(txtSearchUser.getText()); }
        });

        searchBar.add(txtSearchUser, BorderLayout.CENTER);
        content.add(searchBar, BorderLayout.NORTH);

        // Available users list
        JPanel listSection = new JPanel(new BorderLayout(0, 6));
        listSection.setOpaque(false);

        JLabel lblAvail = makeLabel("Danh Sách User Online:");
        listSection.add(lblAvail, BorderLayout.NORTH);

        availableUsersModel = new DefaultListModel<>();
        listAvailable = new JList<>(availableUsersModel);
        listAvailable.setBackground(BG_CARD);
        listAvailable.setForeground(TEXT_PRIMARY);
        listAvailable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        listAvailable.setFixedCellHeight(38);
        listAvailable.setSelectionBackground(new Color(49, 46, 129));
        listAvailable.setSelectionForeground(TEXT_PRIMARY);
        listAvailable.setBorder(new EmptyBorder(4, 8, 4, 8));
        listAvailable.setCellRenderer(new UserListRenderer());

        listAvailable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String selected = listAvailable.getSelectedValue();
                    if (selected != null) addSeller(selected);
                }
            }
        });

        JScrollPane listScroll = new JScrollPane(listAvailable);
        listScroll.setBorder(BorderFactory.createLineBorder(BORDER, 1, true));
        listScroll.getViewport().setBackground(BG_CARD);

        JButton btnAdd = createSuccessBtn("＋ Thêm Seller Đã Chọn");
        btnAdd.addActionListener(e -> {
            List<String> vals = listAvailable.getSelectedValuesList();
            for (String v : vals) addSeller(v);
        });

        listSection.add(listScroll, BorderLayout.CENTER);
        listSection.add(btnAdd, BorderLayout.SOUTH);
        content.add(listSection, BorderLayout.CENTER);

        // Selected sellers tags
        JPanel bottomSec = new JPanel(new BorderLayout(0, 6));
        bottomSec.setOpaque(false);

        JLabel lblSelected = makeLabel("Seller Đã Chọn:");
        bottomSec.add(lblSelected, BorderLayout.NORTH);

        tagsPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 6, 4));
        tagsPanel.setBackground(new Color(18, 26, 40));
        tagsPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1, true),
            new EmptyBorder(8, 8, 8, 8)
        ));
        tagsPanel.setPreferredSize(new Dimension(0, 80));

        bottomSec.add(tagsPanel, BorderLayout.CENTER);
        content.add(bottomSec, BorderLayout.SOUTH);

        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    // ─── Seller Management ────────────────────────────────────────────────────
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
        for (String seller : selectedSellers) {
            tagsPanel.add(buildTag(seller));
        }
        tagsPanel.revalidate();
        tagsPanel.repaint();
        lblSellerCount.setText("Đã chọn: " + selectedSellers.size() + " seller"
            + (selectedSellers.isEmpty() ? "" : ": " + String.join(", ", selectedSellers)));
    }

    private JPanel buildTag(String name) {
        JPanel tag = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        tag.setBackground(TAG_BG);
        tag.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT, 1, true),
            new EmptyBorder(1, 6, 1, 2)
        ));

        JLabel lbl = new JLabel("👤 " + name);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(ACCENT_LIGHT);

        JButton btnX = new JButton("✕");
        btnX.setFont(new Font("Segoe UI", Font.BOLD, 10));
        btnX.setForeground(new Color(200, 200, 250));
        btnX.setBackground(TAG_BG);
        btnX.setBorderPainted(false);
        btnX.setFocusPainted(false);
        btnX.setContentAreaFilled(false);
        btnX.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnX.setMargin(new Insets(0, 2, 0, 2));
        btnX.addActionListener(e -> removeSeller(name));

        tag.add(lbl);
        tag.add(btnX);
        return tag;
    }

    private void filterUsers(String query) {
        // Filter locally; server has already provided list
        // Re-apply filter from a cached copy
        String q = query.toLowerCase().trim();
        // We'll simply enable/disable items via selection logic
        // Since DefaultListModel doesn't filter, we rebuild from the cached list
    }

    // ─── Confirm ─────────────────────────────────────────────────────────────
    private void onConfirm() {
        String roomId = txtRoomId.getText().trim();
        String title = txtRoomTitle.getText().trim();

        if (roomId.isEmpty()) {
            shake(txtRoomId);
            showError("Mã phòng không được để trống!");
            return;
        }
        if (!roomId.matches("[A-Za-z0-9_\\-]+")) {
            shake(txtRoomId);
            showError("Mã phòng chỉ được chứa chữ, số, '_' và '-'.");
            return;
        }
        if (title.isEmpty()) {
            shake(txtRoomTitle);
            showError("Tiêu đề phòng không được để trống!");
            return;
        }

        // Build payload
        Map<String, String> data = new HashMap<>();
        data.put("roomId", roomId);
        data.put("title", title);
        data.put("description", txtDescription.getText().trim());
        // Sellers as comma-separated
        data.put("sellers", String.join(",", selectedSellers));

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
            if (step[0] < offsets.length) {
                comp.setLocation(original.x + offsets[step[0]], original.y);
                step[0]++;
            } else {
                comp.setLocation(original);
                timer.stop();
            }
        });
        timer.start();
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    // ─── NetworkClient listener ───────────────────────────────────────────────
    @Override
    public void onMessage(MessageType type, Map<String, String> data) {
        SwingUtilities.invokeLater(() -> {
            if (type == MessageType.GET_USER_LIST) {
                String userList = data.get("userList");
                availableUsersModel.clear();
                if (userList != null && !userList.isEmpty()) {
                    for (String u : userList.split(",")) {
                        if (!u.isBlank()) availableUsersModel.addElement(u.trim());
                    }
                }
            }
        });
    }

    // ─── Getters ─────────────────────────────────────────────────────────────
    public boolean isConfirmed() { return confirmed; }
    public String getRoomId() { return txtRoomId.getText().trim(); }
    public List<String> getSelectedSellers() { return Collections.unmodifiableList(selectedSellers); }

    private static class CardWrapper extends JPanel {
        private final JPanel inner;
        CardWrapper(JPanel card, JPanel inner) {
            super(new BorderLayout());
            this.inner = inner;
            setOpaque(false);
            super.add(card, BorderLayout.CENTER);
        }
        @Override public Component add(Component comp) {
            inner.add(comp, BorderLayout.CENTER); return comp;
        }
        @Override public void add(Component comp, Object constraints) {
            inner.add(comp, constraints);
        }
    }

    private JPanel buildCard(String title) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createLineBorder(BORDER, 1, true));

        JLabel lblTitle = new JLabel("  " + title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTitle.setForeground(TEXT_MUTED);
        lblTitle.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, BORDER),
            new EmptyBorder(10, 12, 10, 12)
        ));
        lblTitle.setBackground(new Color(22, 32, 48));
        lblTitle.setOpaque(true);
        card.add(lblTitle, BorderLayout.NORTH);

        JPanel inner = new JPanel(new BorderLayout());
        inner.setOpaque(false);
        inner.setBorder(new EmptyBorder(14, 16, 16, 16));
        card.add(inner, BorderLayout.CENTER);

        return new CardWrapper(card, inner);
    }


    private JLabel makeLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(TEXT_MUTED);
        lbl.setBorder(new EmptyBorder(0, 0, 4, 0));
        return lbl;
    }

    private JTextField makeTextField(String placeholder) {
        JTextField tf = new JTextField();
        tf.setBackground(BG_INPUT);
        tf.setForeground(TEXT_PRIMARY);
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tf.setCaretColor(ACCENT_LIGHT);
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1, true),
            new EmptyBorder(8, 12, 8, 12)
        ));
        return tf;
    }

    private JButton createPrimaryBtn(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setBackground(ACCENT);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(10, 22, 10, 22));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(ACCENT_LIGHT); }
            public void mouseExited(MouseEvent e) { btn.setBackground(ACCENT); }
        });
        return btn;
    }

    private JButton createSuccessBtn(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(new Color(6, 35, 20));
        btn.setBackground(SUCCESS);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 16, 8, 16));
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
            new EmptyBorder(9, 20, 9, 20)
        ));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ─── Custom cell renderer ─────────────────────────────────────────────────
    private static class UserListRenderer extends DefaultListCellRenderer {
        private static final Color BG = new Color(30, 41, 59);
        private static final Color SEL = new Color(49, 46, 129);
        private static final Color FG = new Color(248, 250, 252);
        private static final Color MUTED = new Color(148, 163, 184);

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean hasFocus) {
            JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, hasFocus);
            lbl.setText("  👤 " + value);
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            lbl.setForeground(isSelected ? FG : MUTED);
            lbl.setBackground(isSelected ? SEL : BG);
            lbl.setBorder(new EmptyBorder(4, 4, 4, 4));
            return lbl;
        }
    }

    /**
     * WrapLayout - FlowLayout variant that wraps to next line
     */
    static class WrapLayout extends FlowLayout {
        public WrapLayout(int align, int hgap, int vgap) { super(align, hgap, vgap); }

        @Override
        public Dimension preferredLayoutSize(Container target) {
            return layoutSize(target, true);
        }
        @Override
        public Dimension minimumLayoutSize(Container target) {
            return layoutSize(target, false);
        }

        private Dimension layoutSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int targetWidth = target.getSize().width;
                if (targetWidth == 0) targetWidth = Integer.MAX_VALUE;

                int hgap = getHgap(), vgap = getVgap();
                Insets insets = target.getInsets();
                int maxWidth = targetWidth - insets.left - insets.right - hgap * 2;

                Dimension dim = new Dimension(0, 0);
                int rowWidth = 0, rowHeight = 0;
                int nmembers = target.getComponentCount();
                for (int i = 0; i < nmembers; i++) {
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
