package client;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

/**
 * UITheme — Design System trung tâm cho AuctionPro.
 * Tất cả màu, font, và factory methods đều ở đây.
 */
public final class UITheme {

    private UITheme() {}

    // ══════════════════════════════════════════════════════════════════════════
    //  PALETTE
    // ══════════════════════════════════════════════════════════════════════════

    /** Nền sâu nhất (Sidebar, outer frame) */
    public static final Color BG_DEEP    = new Color(9, 13, 22);
    /** Nền trang chính (Page main background) */
    public static final Color BG_DARK    = new Color(15, 23, 42);
    /** Nền card, sidebar items, panels */
    public static final Color BG_CARD    = new Color(30, 41, 59);
    /** Nền header, panel phụ nổi lên */
    public static final Color BG_ELEVATED= new Color(51, 65, 85);
    /** Hàng xen kẽ trong bảng */
    public static final Color BG_ROW_ALT = new Color(22, 32, 51);

    /** Accent chính – Blue 500 */
    public static final Color ACCENT       = new Color(59, 130, 246);
    /** Accent nhạt – Blue 400 */
    public static final Color ACCENT_LIGHT = new Color(96, 165, 250);
    /** Accent đậm – Blue 700 */
    public static final Color ACCENT_DARK  = new Color(29, 78, 216);

    /** Text trắng chính */
    public static final Color TEXT_PRIMARY = new Color(248, 250, 252);
    /** Text phụ xám */
    public static final Color TEXT_MUTED   = new Color(148, 163, 184);
    /** Text hint nhạt */
    public static final Color TEXT_HINT    = new Color(100, 116, 139);

    /** Xanh lá – thành công */
    public static final Color SUCCESS      = new Color(16, 185, 129);
    /** Đỏ hồng – nguy hiểm */
    public static final Color DANGER       = new Color(239, 68, 68);
    /** Vàng amber – cảnh báo / moderator */
    public static final Color AMBER        = new Color(245, 158, 11);
    /** Xanh dương – thông tin */
    public static final Color INFO         = new Color(14, 165, 233);

    public static final Color ORANGE       = new Color(249, 115, 22);
    public static final Color ORANGE_HOVER = new Color(234, 88, 12);
    public static final Color LIGHT_BG     = new Color(15, 23, 42);

    /** Đường viền subtle */
    public static final Color BORDER       = new Color(30, 41, 59);
    /** Đường viền sáng hơn */
    public static final Color BORDER_LIGHT = new Color(51, 65, 85);

    // ══════════════════════════════════════════════════════════════════════════
    //  BACKGROUND CHO TỪNG ROLE
    // ══════════════════════════════════════════════════════════════════════════
    /** Màu avatar User */
    public static final Color USER_AVATAR   = ACCENT;
    /** Màu avatar Moderator */
    public static final Color MOD_AVATAR    = AMBER;
    /** Màu avatar Admin */
    public static final Color ADMIN_AVATAR  = DANGER;

    // ══════════════════════════════════════════════════════════════════════════
    //  FONTS
    // ══════════════════════════════════════════════════════════════════════════
    public static Font fontTitle(int size)  { return new Font("Segoe UI", Font.BOLD,  size); }
    public static Font fontBody(int size)   { return new Font("Segoe UI", Font.PLAIN, size); }
    public static Font fontBold(int size)   { return new Font("Segoe UI", Font.BOLD,  size); }
    public static Font fontMono(int size)   { return new Font("Cascadia Code", Font.PLAIN, size); }
    public static Font fontLabel()          { return new Font("Segoe UI", Font.BOLD,  11); }

    /**
     * Font dùng riêng cho emoji / icon Unicode.
     * Ưu tiên "Segoe UI Emoji" (Windows), fallback sang "Apple Color Emoji" (macOS),
     * cuối cùng là "Dialog" (cross-platform).
     * Kết hợp với deriveFont để giữ đúng size mà không bị □.
     */
    public static Font fontEmoji(int size) {
        String[] candidates = {"Segoe UI Emoji", "Apple Color Emoji", "Noto Color Emoji", "Dialog"};
        for (String name : candidates) {
            Font f = new Font(name, Font.PLAIN, size);
            // Kiểm tra font có thực sự được cài không (tránh fallback sang Dialog ngay)
            if (!f.getFamily().equalsIgnoreCase("Dialog") || name.equals("Dialog")) {
                return f;
            }
        }
        return new Font("Dialog", Font.PLAIN, size);
    }
    
    /**
     * Tạo JLabel hiển thị emoji đúng.
     * Dùng thay cho: new JLabel(emojiText)
     */
    public static JLabel emojiLabel(String emoji, int size) {
        JLabel lbl = new JLabel(emoji, SwingConstants.CENTER);
        lbl.setFont(fontEmoji(size));
        return lbl;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  BUTTON FACTORY
    // ══════════════════════════════════════════════════════════════════════════

    public static JButton primaryBtn(String text) {
        return makeBtn(text, ACCENT, Color.WHITE, ACCENT_DARK);
    }

    public static JButton secondaryBtn(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color currentBg = getModel().isRollover() ? BG_ELEVATED : BG_CARD;
                g2.setColor(currentBg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(BORDER_LIGHT);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        btn.setFont(fontBody(12));
        btn.setForeground(TEXT_MUTED);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(7, 16, 7, 16));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setForeground(TEXT_PRIMARY); }
            public void mouseExited(MouseEvent e)  { btn.setForeground(TEXT_MUTED); }
        });
        return btn;
    }

    public static JButton dangerBtn(String text) {
        return makeBtn(text, new Color(127, 29, 29), DANGER, new Color(153, 27, 27));
    }

    public static JButton successBtn(String text) {
        return makeBtn(text, new Color(6, 78, 59), SUCCESS, new Color(4, 120, 87));
    }

    public static JButton amberBtn(String text) {
        return makeBtn(text, new Color(120, 53, 4), AMBER, new Color(146, 64, 14));
    }

    public static JButton ghostBtn(String text, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(fontBody(12));
        btn.setForeground(fg);
        btn.setBackground(null);
        btn.setOpaque(false);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 14, 8, 14));
        return btn;
    }

    private static JButton makeBtn(String text, Color bg, Color fg, Color hoverBg) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color currentBg = getModel().isRollover() ? hoverBg : bg;
                g2.setColor(currentBg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        btn.setFont(fontBold(13));
        btn.setForeground(fg);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(10, 22, 10, 22));
        return btn;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TABLE FACTORY
    // ══════════════════════════════════════════════════════════════════════════

    public static JTable styledTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setBackground(BG_CARD);
        table.setForeground(TEXT_PRIMARY);
        table.setFont(fontBody(13));
        table.setRowHeight(44);
        table.setGridColor(BORDER);
        table.setSelectionBackground(new Color(30, 41, 59));
        table.setSelectionForeground(TEXT_PRIMARY);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setBackground(BG_DEEP);
        header.setForeground(TEXT_MUTED);
        header.setFont(fontLabel());
        header.setBorder(new MatteBorder(0, 0, 1, 0, BORDER));
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(0, 36));

        // Default striped renderer
        table.setDefaultRenderer(Object.class, new StripedRenderer());

        return table;
    }

    public static JScrollPane styledScrollPane(JComponent comp) {
        JScrollPane sp = new JScrollPane(comp);
        sp.setBorder(BorderFactory.createCompoundBorder(
            UIManager.getBorder("ScrollPane.border"),
            new EmptyBorder(1, 1, 1, 1)
        ));
        sp.getViewport().setBackground(BG_CARD);
        sp.setBackground(BG_CARD);
        sp.getVerticalScrollBar().setBackground(BG_CARD);
        return sp;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  LABEL / HEADER FACTORY
    // ══════════════════════════════════════════════════════════════════════════

    /** Tiêu đề section + subtitle nhỏ bên dưới */
    public static JPanel sectionHeader(String title, String subtitle) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setBackground(null);
        row.setOpaque(false);

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(fontTitle(22));
        lblTitle.setForeground(TEXT_PRIMARY);
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(lblTitle);

        if (subtitle != null && !subtitle.isEmpty()) {
            JLabel lblSub = new JLabel(subtitle);
            lblSub.setFont(fontBody(13));
            lblSub.setForeground(TEXT_MUTED);
            lblSub.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.add(Box.createVerticalStrut(3));
            row.add(lblSub);
        }
        return row;
    }

    /** Badge label nhỏ */
    public static JLabel badge(String text, Color fg, Color bg) {
        JLabel lbl = new JLabel("  " + text + "  ");
        lbl.setFont(fontBold(10));
        lbl.setForeground(fg);
        lbl.setBackground(bg);
        lbl.setOpaque(true);
        lbl.setBorder(new EmptyBorder(3, 6, 3, 6));
        return lbl;
    }

    /** Stat card nhỏ trong sidebar */
    public static StatCard statCard(String label, Color valueColor) {
        return new StatCard(label, valueColor);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CARD PANEL
    // ══════════════════════════════════════════════════════════════════════════

    /** Card panel với viền và tiêu đề */
    public static JPanel card(String headerTitle) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createLineBorder(BORDER, 1, true));

        if (headerTitle != null && !headerTitle.isEmpty()) {
            JPanel cardHeader = new JPanel(new BorderLayout());
            cardHeader.setBackground(BG_DEEP);
            cardHeader.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 1, 0, BORDER),
                new EmptyBorder(10, 14, 10, 14)
            ));
            JLabel lbl = new JLabel(headerTitle);
            lbl.setFont(fontBold(12));
            lbl.setForeground(TEXT_MUTED);
            cardHeader.add(lbl);
            card.add(cardHeader, BorderLayout.NORTH);
        }
        return card;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  AVATAR
    // ══════════════════════════════════════════════════════════════════════════

    /** Panel avatar tròn với chữ initials */
    public static JPanel avatarCircle(String initial, Color bgColor, int size) {
        JPanel p = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Subtle glow
                g2.setColor(new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), 40));
                g2.fillOval(-3, -3, getWidth() + 6, getHeight() + 6);
                g2.setColor(bgColor);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(size, size));
        p.setMaximumSize(new Dimension(size, size));
        p.setMinimumSize(new Dimension(size, size));

        JLabel lbl = new JLabel(initial, SwingConstants.CENTER);
        lbl.setFont(fontBold((int)(size * 0.42)));
        lbl.setForeground(Color.WHITE);
        p.add(lbl);
        return p;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  NAV BUTTON (sidebar)
    // ══════════════════════════════════════════════════════════════════════════

    public static JButton navBtn(String text) {
        JButton btn = new JButton(text);
        btn.setFont(fontBody(13));
        btn.setForeground(TEXT_MUTED);
        btn.setBackground(null);
        btn.setOpaque(true);
        btn.setBackground(BG_DARK);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 14, 8, 14));
        return btn;
    }

    /** Kích hoạt trạng thái active cho nav button */
    public static void setNavActive(JButton btn, boolean active) {
        if (active) {
            btn.setBackground(new Color(30, 27, 75));
            btn.setForeground(ACCENT_LIGHT);
            btn.setFont(fontBold(13));
        } else {
            btn.setBackground(BG_DARK);
            btn.setForeground(TEXT_MUTED);
            btn.setFont(fontBody(13));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  INPUT FIELD
    // ══════════════════════════════════════════════════════════════════════════

    public static JTextField darkTextField(String placeholder) {
        JTextField tf = new JTextField();
        tf.setFont(fontBody(14));
        tf.setForeground(TEXT_PRIMARY);
        tf.setBackground(BG_ELEVATED);
        tf.setCaretColor(ACCENT_LIGHT);
        if (placeholder != null && !placeholder.isEmpty()) {
            tf.putClientProperty("JTextField.placeholderText", placeholder);
        }
        tf.setBorder(BorderFactory.createCompoundBorder(
            UIManager.getBorder("TextField.border"),
            new EmptyBorder(8, 14, 8, 14)
        ));
        tf.setPreferredSize(new Dimension(0, 46));
        return tf;
    }

    public static JPasswordField darkPasswordField() {
        return darkPasswordField("");
    }

    public static JPasswordField darkPasswordField(String placeholder) {
        JPasswordField pf = new JPasswordField();
        pf.setFont(fontBody(14));
        pf.setForeground(TEXT_PRIMARY);
        pf.setBackground(BG_ELEVATED);
        pf.setCaretColor(ACCENT_LIGHT);
        if (placeholder != null && !placeholder.isEmpty()) {
            pf.putClientProperty("JTextField.placeholderText", placeholder);
        }
        pf.setBorder(BorderFactory.createCompoundBorder(
            UIManager.getBorder("PasswordField.border"),
            new EmptyBorder(8, 14, 8, 14)
        ));
        pf.setPreferredSize(new Dimension(0, 46));
        return pf;
    }

    public static JTextField customTextField(String placeholder, Color bg, Color borderColor) {
        JTextField tf = new JTextField() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
                super.paintComponent(g);
                g2.dispose();
            }
            @Override protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(borderColor);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
                g2.dispose();
            }
        };
        tf.setOpaque(false);
        tf.setFont(fontBody(14));
        tf.setForeground(bg == Color.WHITE ? new Color(40, 40, 40) : TEXT_PRIMARY);
        tf.setCaretColor(bg == Color.WHITE ? new Color(40, 40, 40) : ACCENT_LIGHT);
        if (placeholder != null && !placeholder.isEmpty()) {
            tf.putClientProperty("JTextField.placeholderText", placeholder);
        }
        tf.setBorder(new EmptyBorder(5, 15, 5, 15));
        tf.setPreferredSize(new Dimension(350, 45));
        return tf;
    }

    public static JPasswordField customPasswordField(String placeholder, Color bg, Color borderColor) {
        JPasswordField pf = new JPasswordField() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
                super.paintComponent(g);
                g2.dispose();
            }
            @Override protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(borderColor);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
                g2.dispose();
            }
        };
        pf.setOpaque(false);
        pf.setFont(fontBody(14));
        pf.setForeground(bg == Color.WHITE ? new Color(40, 40, 40) : TEXT_PRIMARY);
        pf.setCaretColor(bg == Color.WHITE ? new Color(40, 40, 40) : ACCENT_LIGHT);
        if (placeholder != null && !placeholder.isEmpty()) {
            pf.putClientProperty("JTextField.placeholderText", placeholder);
        }
        pf.setBorder(new EmptyBorder(5, 15, 5, 15));
        pf.setPreferredSize(new Dimension(350, 45));
        return pf;
    }

    public static JButton customBtn(String text, Color baseColor, Color hoverColor, Color fgColor, int radius) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover()) {
                    g2.setColor(hoverColor);
                } else {
                    g2.setColor(baseColor);
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        btn.setFont(fontBold(15));
        btn.setForeground(fgColor);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(350, 45));
        return btn;
    }

    public static JButton logoutBtn(Color sidebarColor, Runnable logoutAction) {
        JButton btn = new JButton("Đăng xuất") {
            private boolean isHovered = false;
            {
                setOpaque(false);
                setContentAreaFilled(false);
                setBorderPainted(false);
                setFocusPainted(false);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                setMaximumSize(new Dimension(220, 36));
                setHorizontalAlignment(SwingConstants.LEFT);
                setBorder(new EmptyBorder(0, 38, 0, 10));
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { isHovered = true; repaint(); }
                    public void mouseExited(MouseEvent e) { isHovered = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color c = isHovered ? new Color(248, 113, 113) : new Color(239, 68, 68);
                g2.setColor(c);
                
                // Draw exit icon
                g2.setStroke(new BasicStroke(1.5f));
                int ix = 12;
                int iy = (getHeight() - 16) / 2;
                g2.drawRoundRect(ix, iy, 10, 16, 2, 2);
                g2.drawLine(ix + 6, iy + 8, ix + 14, iy + 8);
                g2.drawLine(ix + 11, iy + 5, ix + 14, iy + 8);
                g2.drawLine(ix + 11, iy + 11, ix + 14, iy + 8);
                
                // Cover right bracket line to open it
                g2.setColor(sidebarColor);
                g2.fillRect(ix + 8, iy + 3, 3, 10);
                
                // Text
                g2.setColor(c);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), 38, (getHeight() + fm.getAscent() - fm.getDescent()) / 2 - 1);
                g2.dispose();
            }
        };
        btn.addActionListener(e -> logoutAction.run());
        return btn;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  INNER CLASSES
    // ══════════════════════════════════════════════════════════════════════════

    /** Stat card nhỏ trong sidebar */
    public static class StatCard extends RoundedPanel {
        private final JLabel valueLabel;

        public StatCard(String label, Color valueColor) {
            super(12, BG_ELEVATED, BORDER);
            setLayout(new BorderLayout(0, 4));
            // Add inner margins inside the rounded card border
            setBorder(BorderFactory.createCompoundBorder(
                getBorder(),
                new EmptyBorder(8, 12, 8, 12)
            ));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));
            setAlignmentX(Component.LEFT_ALIGNMENT);

            valueLabel = new JLabel("—");
            valueLabel.setFont(fontBold(22));
            valueLabel.setForeground(valueColor);
            add(valueLabel, BorderLayout.CENTER);

            JLabel lblLbl = new JLabel(label);
            lblLbl.setFont(fontBody(11));
            lblLbl.setForeground(TEXT_MUTED);
            add(lblLbl, BorderLayout.SOUTH);
        }

        public void setValue(String v) { valueLabel.setText(v); }
    }

    /** Striped table cell renderer */
    public static class StripedRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v,
                boolean sel, boolean foc, int row, int col) {
            super.getTableCellRendererComponent(t, v, sel, foc, row, col);
            setBackground(sel ? new Color(30, 27, 75) : (row % 2 == 0 ? BG_CARD : BG_ROW_ALT));
            setForeground(TEXT_PRIMARY);
            setBorder(new EmptyBorder(0, 12, 0, 12));
            return this;
        }
    }

    /** Rounded panel với custom background */
    public static class RoundedPanel extends JPanel {
        private final int radius;
        private final Color bg;
        private final Color borderColor;

        public RoundedPanel(int radius, Color bg, Color borderColor) {
            this.radius = radius;
            this.bg = bg;
            this.borderColor = borderColor;
            setOpaque(false);
            // Default internal border to prevent inner elements from touching the round border or shadow
            setBorder(new EmptyBorder(12, 12, 12, 12));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            int shadowSize = 4;
            // Draw soft shadows outside
            for (int i = 0; i < shadowSize; i++) {
                g2.setColor(new Color(0, 0, 0, 10 - i * 2));
                g2.fillRoundRect(shadowSize - i, shadowSize - i + 1, getWidth() - (shadowSize - i) * 2, getHeight() - (shadowSize - i) * 2, radius, radius);
            }
            
            // Draw actual card background inside the shadow boundaries
            g2.setColor(bg);
            g2.fillRoundRect(shadowSize, shadowSize, getWidth() - shadowSize * 2, getHeight() - shadowSize * 2, radius, radius);
            
            // Draw thin elegant border
            if (borderColor != null) {
                g2.setColor(borderColor);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(shadowSize, shadowSize, getWidth() - shadowSize * 2 - 1, getHeight() - shadowSize * 2 - 1, radius, radius);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Panel login/register với gradient background */
    public static class GradientPanel extends JPanel {
        private final Color c1, c2;
        public GradientPanel(Color c1, Color c2) {
            this.c1 = c1; this.c2 = c2;
            setOpaque(false);
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            GradientPaint gp = new GradientPaint(0, 0, c1, getWidth(), getHeight(), c2);
            g2.setPaint(gp);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }
}
