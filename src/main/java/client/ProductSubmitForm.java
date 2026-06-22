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
import net.miginfocom.swing.MigLayout;

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
        JPanel form = new JPanel(new MigLayout("wrap 1, insets 16, gap 8, fillx", "[grow]"));
        form.setBackground(UITheme.BG_CARD);

        form.add(label("Tên sản phẩm *"), "growx");
        txtName = UITheme.customTextField("Ví dụ: Bình gốm cổ Thế kỷ XVIII", UITheme.BG_DARK, UITheme.BORDER);
        form.add(txtName, "growx, h 40!");

        form.add(label("Mô tả chi tiết"), "growx");
        
        txtDesc = new JTextArea(3, 1);
        txtDesc.setFont(UITheme.fontBody(13));
        txtDesc.setForeground(UITheme.TEXT_PRIMARY);
        txtDesc.setBackground(UITheme.BG_DARK);
        txtDesc.setCaretColor(UITheme.ACCENT);
        txtDesc.setLineWrap(true);
        txtDesc.setWrapStyleWord(true);
        txtDesc.setBorder(new EmptyBorder(8, 12, 8, 12));
        JScrollPane descScroll = UITheme.styledScrollPane(txtDesc);
        form.add(descScroll, "growx, h 80!");

        form.add(label("Giá khởi điểm (VND) *"), "growx");
        txtPrice = UITheme.customTextField("Ví dụ: 5000000", UITheme.BG_DARK, UITheme.BORDER);
        form.add(txtPrice, "growx, h 40!");

        form.add(label("Hình ảnh sản phẩm *"), "growx");
        form.add(buildImageRow(), "growx");

        add(UITheme.styledScrollPane(form), BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new MigLayout("insets 16", "push[]"));
        footer.setBackground(UITheme.BG_CARD);
        footer.setBorder(new MatteBorder(1, 0, 0, 0, UITheme.BORDER));
        btnSubmit = UITheme.primaryBtn("Gửi yêu cầu đến Moderator");
        footer.add(btnSubmit, "growx");
        add(footer, BorderLayout.SOUTH);

        btnSelectImage.addActionListener(e -> selectImage());
        btnSubmit.addActionListener(e -> submitProduct());
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UITheme.fontBold(12));
        l.setForeground(UITheme.TEXT_MUTED);
        return l;
    }

    private JPanel buildImageRow() {
        JPanel imageCard = new JPanel(new MigLayout("insets 8, gap 12", "[][grow]", "[]"));
        imageCard.setBackground(UITheme.BG_DARK);
        imageCard.setBorder(new LineBorder(UITheme.BORDER, 1, true));
        imageCard.putClientProperty("FlatLaf.style", "arc: 10");

        lblImagePreview = new JLabel("Chưa chọn ảnh", SwingConstants.CENTER);
        lblImagePreview.setFont(UITheme.fontBody(11));
        lblImagePreview.setForeground(UITheme.TEXT_HINT);
        lblImagePreview.setPreferredSize(new Dimension(84, 84));
        imageCard.add(lblImagePreview, "cell 0 0");

        btnSelectImage = UITheme.ghostBtn("Chọn ảnh...", UITheme.ACCENT);
        imageCard.add(btnSelectImage, "cell 1 0, left");

        imageCard.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { selectImage(); }
        });

        return imageCard;
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
