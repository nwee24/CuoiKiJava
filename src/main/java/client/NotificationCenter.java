package client;

import shared.MessageType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * NotificationCenter - Trung tâm thông báo cho Moderator.
 * Hiển thị một nút chuông ở header với badge đếm tin chưa đọc.
 * Khi bấm, mở drawer/popup danh sách thông báo.
 */
public class NotificationCenter extends JPanel implements NetworkClient.MessageListener {

    // ─── Theme ─────────────────────────────────────────────────────────────────
    private static final Color BG_DARK      = new Color(15, 23, 42);
    private static final Color BG_CARD      = new Color(30, 41, 59);
    private static final Color BG_DRAWER    = new Color(22, 32, 52);
    private static final Color ACCENT       = new Color(99, 102, 241);
    private static final Color ACCENT_LIGHT = new Color(129, 140, 248);
    private static final Color TEXT_PRIMARY = new Color(248, 250, 252);
    private static final Color TEXT_MUTED   = new Color(148, 163, 184);
    private static final Color BORDER       = new Color(51, 65, 85);
    private static final Color AMBER        = new Color(251, 191, 36);
    private static final Color SUCCESS      = new Color(52, 211, 153);
    private static final Color DANGER       = new Color(248, 113, 113);
    private static final Color BADGE_RED    = new Color(239, 68, 68);
    private static final Color UNREAD_BG    = new Color(30, 41, 70);

    // ─── State ─────────────────────────────────────────────────────────────────
    private final List<NotifItem> notifications = new ArrayList<>();
    private int unreadCount = 0;

    // ─── UI ────────────────────────────────────────────────────────────────────
    private JButton btnBell;
    private JLabel badgeLabel;
    private JPanel drawerPanel;
    private JPanel notifListPanel;
    private JWindow drawerWindow;
    private boolean drawerOpen = false;

    // callback để ModeratorDashboard phản ứng khi click item
    public interface NotifClickListener {
        void onNotifClicked(NotifItem item);
    }
    private NotifClickListener clickListener;

    public NotificationCenter() {
        setLayout(new BorderLayout());
        setOpaque(false);
        buildBellButton();
        NetworkClient.getInstance().addListener(this);
    }

    public void setClickListener(NotifClickListener l) {
        this.clickListener = l;
    }

    // ─── Bell Button ───────────────────────────────────────────────────────────
    private void buildBellButton() {
        JLayeredPane layered = new JLayeredPane();
        layered.setPreferredSize(new Dimension(44, 44));
        layered.setOpaque(false);

        btnBell = new JButton("[N]");
        btnBell.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnBell.setForeground(TEXT_MUTED);
        btnBell.setBackground(new Color(0,0,0,0));
        btnBell.setBorderPainted(false);
        btnBell.setFocusPainted(false);
        btnBell.setContentAreaFilled(false);
        btnBell.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnBell.setBounds(0, 2, 40, 40);
        btnBell.setToolTipText("Trung tâm thông báo");

        btnBell.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btnBell.setForeground(ACCENT_LIGHT); }
            public void mouseExited(MouseEvent e) { btnBell.setForeground(unreadCount > 0 ? AMBER : TEXT_MUTED); }
        });
        btnBell.addActionListener(e -> toggleDrawer());

        badgeLabel = new JLabel("0");
        badgeLabel.setFont(new Font("Segoe UI", Font.BOLD, 9));
        badgeLabel.setForeground(Color.WHITE);
        badgeLabel.setBackground(BADGE_RED);
        badgeLabel.setOpaque(true);
        badgeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        badgeLabel.setBounds(22, 0, 18, 14);
        badgeLabel.setBorder(BorderFactory.createEmptyBorder(1, 2, 1, 2));
        badgeLabel.setVisible(false);

        layered.add(btnBell, JLayeredPane.DEFAULT_LAYER);
        layered.add(badgeLabel, JLayeredPane.PALETTE_LAYER);

        add(layered, BorderLayout.CENTER);
    }

    // ─── Drawer ────────────────────────────────────────────────────────────────
    private void toggleDrawer() {
        if (drawerOpen) {
            closeDrawer();
        } else {
            openDrawer();
        }
    }

    private void openDrawer() {
        if (drawerWindow != null) drawerWindow.dispose();

        Window owner = SwingUtilities.getWindowAncestor(this);
        drawerWindow = new JWindow(owner);
        drawerWindow.setBackground(new Color(0, 0, 0, 0));

        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(BG_DRAWER);
        container.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1),
            new EmptyBorder(0, 0, 0, 0)
        ));
        container.setPreferredSize(new Dimension(360, 480));

        // Header
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(new Color(22, 32, 52));
        hdr.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, BORDER),
            new EmptyBorder(12, 16, 12, 16)
        ));
        JLabel lblTitle = new JLabel("Thong Bao");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTitle.setForeground(TEXT_PRIMARY);
        hdr.add(lblTitle, BorderLayout.WEST);

        JButton btnMarkAll = new JButton("Đọc tất cả");
        btnMarkAll.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnMarkAll.setForeground(ACCENT_LIGHT);
        btnMarkAll.setBackground(new Color(22, 32, 52));
        btnMarkAll.setBorderPainted(false);
        btnMarkAll.setFocusPainted(false);
        btnMarkAll.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnMarkAll.addActionListener(e -> markAllRead());
        hdr.add(btnMarkAll, BorderLayout.EAST);
        container.add(hdr, BorderLayout.NORTH);

        // List
        notifListPanel = new JPanel();
        notifListPanel.setLayout(new BoxLayout(notifListPanel, BoxLayout.Y_AXIS));
        notifListPanel.setBackground(BG_DRAWER);

        refreshNotifList();

        JScrollPane sp = new JScrollPane(notifListPanel);
        sp.setBorder(null);
        sp.getViewport().setBackground(BG_DRAWER);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        container.add(sp, BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footer.setBackground(new Color(18, 26, 42));
        footer.setBorder(new MatteBorder(1, 0, 0, 0, BORDER));
        JButton btnClear = new JButton("Xoa tat ca thong bao");
        btnClear.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnClear.setForeground(DANGER);
        btnClear.setBackground(new Color(18, 26, 42));
        btnClear.setBorderPainted(false);
        btnClear.setFocusPainted(false);
        btnClear.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClear.addActionListener(e -> { notifications.clear(); unreadCount = 0; updateBadge(); refreshNotifList(); });
        footer.add(btnClear);
        container.add(footer, BorderLayout.SOUTH);

        drawerWindow.add(container);
        drawerWindow.pack();

        // Định vị dưới nút chuông
        Point loc = btnBell.getLocationOnScreen();
        int dx = loc.x + btnBell.getWidth() - 360;
        int dy = loc.y + btnBell.getHeight() + 6;
        drawerWindow.setLocation(dx, dy);

        drawerWindow.setVisible(true);
        drawerOpen = true;

        // Đóng khi click ngoài
        drawerWindow.addWindowFocusListener(new WindowFocusListener() {
            public void windowGainedFocus(WindowEvent e) {}
            public void windowLostFocus(WindowEvent e) { closeDrawer(); }
        });
    }

    private void closeDrawer() {
        if (drawerWindow != null) {
            drawerWindow.dispose();
            drawerWindow = null;
        }
        drawerOpen = false;
    }

    private void refreshNotifList() {
        if (notifListPanel == null) return;
        notifListPanel.removeAll();

        if (notifications.isEmpty()) {
            JLabel empty = new JLabel("Không có thông báo nào");
            empty.setForeground(TEXT_MUTED);
            empty.setFont(new Font("Segoe UI", Font.ITALIC, 13));
            empty.setHorizontalAlignment(SwingConstants.CENTER);
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            empty.setBorder(new EmptyBorder(40, 20, 40, 20));
            notifListPanel.add(empty);
        } else {
            // Hiện theo thứ tự mới nhất trước
            for (int i = notifications.size() - 1; i >= 0; i--) {
                notifListPanel.add(buildNotifRow(notifications.get(i)));
                notifListPanel.add(Box.createVerticalStrut(1));
            }
        }
        notifListPanel.revalidate();
        notifListPanel.repaint();
    }

    private JPanel buildNotifRow(NotifItem item) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(item.read ? BG_DRAWER : UNREAD_BG);
        row.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, BORDER),
            new EmptyBorder(10, 14, 10, 14)
        ));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        row.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Icon
        JLabel icon = new JLabel(item.icon);
        icon.setFont(new Font("Segoe UI", Font.BOLD, 14));
        icon.setForeground(new Color(129, 140, 248));
        icon.setPreferredSize(new Dimension(36, 36));
        icon.setHorizontalAlignment(SwingConstants.CENTER);
        row.add(icon, BorderLayout.WEST);

        // Content
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        JLabel title = new JLabel(item.title);
        title.setFont(new Font("Segoe UI", Font.BOLD, 13));
        title.setForeground(item.read ? TEXT_MUTED : TEXT_PRIMARY);

        JLabel body = new JLabel("<html><p style='width:240px'>" + item.body + "</p></html>");
        body.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        body.setForeground(TEXT_MUTED);

        JLabel time = new JLabel(item.time);
        time.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        time.setForeground(new Color(100, 116, 139));

        content.add(title);
        content.add(Box.createVerticalStrut(2));
        content.add(body);
        content.add(Box.createVerticalStrut(4));
        content.add(time);
        row.add(content, BorderLayout.CENTER);

        // Unread dot
        if (!item.read) {
            JLabel dot = new JLabel("●");
            dot.setFont(new Font("Segoe UI", Font.BOLD, 10));
            dot.setForeground(ACCENT);
            dot.setVerticalAlignment(SwingConstants.TOP);
            row.add(dot, BorderLayout.EAST);
        }

        // Click handler
        row.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (!item.read) {
                    item.read = true;
                    unreadCount = Math.max(0, unreadCount - 1);
                    updateBadge();
                    refreshNotifList();
                }
                if (clickListener != null) clickListener.onNotifClicked(item);
            }
            public void mouseEntered(MouseEvent e) {
                row.setBackground(new Color(40, 55, 80));
            }
            public void mouseExited(MouseEvent e) {
                row.setBackground(item.read ? BG_DRAWER : UNREAD_BG);
            }
        });

        return row;
    }

    // ─── Public API ────────────────────────────────────────────────────────────
    public void addNotification(String icon, String title, String body) {
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        NotifItem item = new NotifItem(icon, title, body, time);
        notifications.add(item);
        unreadCount++;
        updateBadge();
        if (drawerOpen) refreshNotifList();

        // Hiện popup nhỏ
        showPopupToast(title, body);
    }

    private void markAllRead() {
        for (NotifItem n : notifications) n.read = true;
        unreadCount = 0;
        updateBadge();
        refreshNotifList();
    }

    private void updateBadge() {
        SwingUtilities.invokeLater(() -> {
            if (unreadCount > 0) {
                badgeLabel.setText(unreadCount > 99 ? "99+" : String.valueOf(unreadCount));
                badgeLabel.setVisible(true);
                btnBell.setForeground(AMBER);
            } else {
                badgeLabel.setVisible(false);
                btnBell.setForeground(TEXT_MUTED);
            }
        });
    }

    /** Popup toast nhỏ góc phải màn hình */
    private void showPopupToast(String title, String body) {
        SwingUtilities.invokeLater(() -> {
            Window owner = SwingUtilities.getWindowAncestor(this);
            if (owner == null) return;
            JWindow toast = new JWindow(owner);
            toast.setBackground(new Color(0, 0, 0, 0));

            JPanel panel = new JPanel(new BorderLayout(8, 0));
            panel.setBackground(new Color(30, 41, 59, 240));
            panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT, 1, true),
                new EmptyBorder(10, 14, 10, 14)
            ));

            // Left accent bar
            JPanel bar = new JPanel();
            bar.setBackground(ACCENT);
            bar.setPreferredSize(new Dimension(4, 0));
            panel.add(bar, BorderLayout.WEST);

            JPanel text = new JPanel();
            text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
            text.setOpaque(false);

            JLabel lbl1 = new JLabel(title);
            lbl1.setFont(new Font("Segoe UI", Font.BOLD, 13));
            lbl1.setForeground(TEXT_PRIMARY);

            JLabel lbl2 = new JLabel("<html><p style='width:220px'>" + body + "</p></html>");
            lbl2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            lbl2.setForeground(TEXT_MUTED);

            text.add(lbl1);
            text.add(Box.createVerticalStrut(3));
            text.add(lbl2);
            panel.add(text, BorderLayout.CENTER);

            toast.add(panel);
            toast.pack();

            // Góc phải màn hình
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            toast.setLocation(screen.width - toast.getWidth() - 20, screen.height - toast.getHeight() - 60);
            toast.setVisible(true);

            // Tự đóng sau 4 giây
            Timer timer = new Timer(4000, ev -> toast.dispose());
            timer.setRepeats(false);
            timer.start();
        });
    }

    // ─── MessageListener ───────────────────────────────────────────────────────
    @Override
    public void onMessage(MessageType type, Map<String, String> data) {
        SwingUtilities.invokeLater(() -> {
            if (type == MessageType.CONTACT_MOD) {
                String from    = data.getOrDefault("senderName", "Seller");
                String product = data.getOrDefault("productName", "San pham khong ro");
                String price   = data.getOrDefault("startingPrice", "?");
                addNotification("[YC]", "Yeu cau moi tu: " + from,
                    "San pham: " + product + " | Gia: " + price);
            } else if (type == MessageType.NOTIFICATION) {
                String ico   = data.getOrDefault("icon", "[i]");
                String title = data.getOrDefault("title", "Thong bao");
                String body  = data.getOrDefault("body", "");
                addNotification(ico, title, body);
            }
        });
    }

    // ─── Data class ────────────────────────────────────────────────────────────
    public static class NotifItem {
        public final String icon, title, body, time;
        public boolean read = false;
        // Raw data cho clickListener
        public Map<String, String> rawData;

        public NotifItem(String icon, String title, String body, String time) {
            this.icon = icon; this.title = title;
            this.body = body; this.time = time;
        }
    }
}
