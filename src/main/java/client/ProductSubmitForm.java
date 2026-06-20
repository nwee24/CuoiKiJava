package client;

import shared.MessageType;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * ProductSubmitForm - Form gửi yêu cầu sản phẩm mới
 */
public class ProductSubmitForm extends JPanel {
    
    private JTextField txtName;
    private JTextArea txtDesc;
    private JTextField txtPrice;
    private JLabel lblImagePreview;
    private JButton btnSelectImage, btnSubmit;
    private String base64Image = "";

    public ProductSubmitForm() {
        setLayout(new BorderLayout());
        setBackground(UITheme.BG_CARD);
        buildUI();
    }

    private void buildUI() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UITheme.BG_CARD);
        form.setBorder(new EmptyBorder(12, 14, 8, 14));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 10, 0);
        gbc.gridx = 0;

        gbc.gridy = 0; form.add(label("Tên sản phẩm *"), gbc);
        gbc.gridy = 1; txtName = textField("Ví dụ: Bình gốm cổ Thế kỷ XVIII"); form.add(txtName, gbc);

        gbc.gridy = 2; form.add(label("Mô tả chi tiết"), gbc);
        gbc.gridy = 3;
        txtDesc = new JTextArea(3, 1);
        txtDesc.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtDesc.setForeground(UITheme.TEXT_PRIMARY);
        txtDesc.setBackground(UITheme.BG_DARK);
        txtDesc.setCaretColor(UITheme.ACCENT);
        txtDesc.setLineWrap(true);
        txtDesc.setWrapStyleWord(true);
        txtDesc.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(UITheme.BORDER, 1, true), new EmptyBorder(8, 10, 8, 10)));
        JScrollPane descScroll = new JScrollPane(txtDesc);
        descScroll.setBorder(null);
        form.add(descScroll, gbc);

        gbc.gridy = 4; form.add(label("Giá khởi điểm (VND) *"), gbc);
        gbc.gridy = 5; txtPrice = textField("Ví dụ: 5000000"); form.add(txtPrice, gbc);

        gbc.gridy = 6; form.add(label("Hình ảnh sản phẩm *"), gbc);
        gbc.gridy = 7; form.add(buildImageRow(), gbc);

        add(form, BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(UITheme.BG_CARD);
        footer.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(1, 0, 0, 0, UITheme.BORDER), new EmptyBorder(10, 14, 12, 14)));
        btnSubmit = createButton("Gửi yêu cầu đến Moderator");
        footer.add(btnSubmit, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);

        btnSelectImage.addActionListener(e -> selectImage());
        btnSubmit.addActionListener(e -> submitProduct());
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        l.setForeground(UITheme.TEXT_MUTED);
        return l;
    }

    private JTextField textField(String placeholder) {
        JTextField tf = new JTextField();
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tf.setForeground(UITheme.TEXT_PRIMARY);
        tf.setBackground(UITheme.BG_DARK);
        tf.setCaretColor(UITheme.ACCENT);
        tf.putClientProperty("JTextField.placeholderText", placeholder);
        tf.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(UITheme.BORDER, 1, true), new EmptyBorder(8, 12, 8, 12)));
        return tf;
    }

    private JPanel buildImageRow() {
        JPanel imageCard = new JPanel(new BorderLayout(12, 0));
        imageCard.setBackground(UITheme.BG_DARK);
        imageCard.setBorder(new LineBorder(UITheme.BORDER, 1, true));
        imageCard.setCursor(new Cursor(Cursor.HAND_CURSOR));

        lblImagePreview = new JLabel("Chưa chọn ảnh", SwingConstants.CENTER);
        lblImagePreview.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblImagePreview.setForeground(UITheme.TEXT_HINT);
        lblImagePreview.setBackground(UITheme.BG_DARK);
        lblImagePreview.setOpaque(true);
        lblImagePreview.setPreferredSize(new Dimension(84, 84));
        imageCard.add(lblImagePreview, BorderLayout.WEST);

        btnSelectImage = new JButton("Chọn ảnh...");
        btnSelectImage.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnSelectImage.setForeground(UITheme.ACCENT);
        btnSelectImage.setBackground(new Color(255, 247, 237));
        btnSelectImage.setFocusPainted(false);
        btnSelectImage.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSelectImage.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(UITheme.BORDER_LIGHT, 1, true), new EmptyBorder(5, 10, 5, 10)));
        imageCard.add(btnSelectImage, BorderLayout.CENTER);

        imageCard.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { selectImage(); }
        });

        return imageCard;
    }

    private JButton createButton(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? UITheme.ACCENT_DARK : UITheme.ACCENT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(12, 20, 12, 20));
        return btn;
    }

    public void setTargetMod(String modUsername) {
        btnSubmit.setText("Gửi yêu cầu đến " + modUsername);
    }

    private void selectImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Hình ảnh (JPG, PNG)", "jpg", "jpeg", "png"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (file.length() > 5 * 1024 * 1024) {
                JOptionPane.showMessageDialog(this, "Ảnh quá lớn. Vui lòng chọn < 5MB.", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                BufferedImage orig = ImageIO.read(file);
                BufferedImage resized = new BufferedImage(300, 300, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2 = resized.createGraphics();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.setColor(Color.WHITE);
                g2.fillRect(0, 0, 300, 300);
                double scale = Math.min(300.0 / orig.getWidth(), 300.0 / orig.getHeight());
                int w = (int)(orig.getWidth() * scale);
                int h = (int)(orig.getHeight() * scale);
                g2.drawImage(orig, (300 - w) / 2, (300 - h) / 2, w, h, null);
                g2.dispose();

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(resized, "jpg", baos);
                base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());

                Image preview = resized.getScaledInstance(76, 76, Image.SCALE_SMOOTH);
                lblImagePreview.setIcon(new ImageIcon(preview));
                lblImagePreview.setText("");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi đọc ảnh: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void submitProduct() {
        String name = txtName.getText().trim();
        String desc = txtDesc.getText().trim();
        String price = txtPrice.getText().trim();

        if (name.isEmpty() || price.isEmpty() || base64Image.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập tên, giá và chọn ảnh.", "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try { 
            Long.parseLong(price); 
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Giá tiền phải là số nguyên.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Map<String, String> data = new HashMap<>();
        data.put("productName", name);
        data.put("description", desc);
        data.put("startingPrice", price);
        data.put("imageData", base64Image);

        NetworkClient.getInstance().sendMessage(MessageType.SUBMIT_PRODUCT, data);

        txtName.setText("");
        txtDesc.setText("");
        txtPrice.setText("");
        base64Image = "";
        lblImagePreview.setIcon(null);
        lblImagePreview.setText("Chưa chọn ảnh");
        
        JOptionPane.showMessageDialog(this, "Đã gửi yêu cầu! Chờ Moderator duyệt.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
    }
}
