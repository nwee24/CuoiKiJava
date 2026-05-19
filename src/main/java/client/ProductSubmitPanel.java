package client;

import shared.MessageType;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class ProductSubmitPanel extends JPanel {
    private JTextField txtName;
    private JTextArea txtDesc;
    private JTextField txtPrice;
    private JLabel lblImagePreview;
    private JButton btnSelectImage;
    private JButton btnSubmit;
    
    private String base64Image = "";
    private String targetModUsername = "";

    public ProductSubmitPanel() {
        setLayout(new BorderLayout());
        
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0; gbc.gridy = 0; formPanel.add(new JLabel("Tên sản phẩm:"), gbc);
        gbc.gridx = 1; txtName = new JTextField(20); formPanel.add(txtName, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1; formPanel.add(new JLabel("Mô tả chi tiết:"), gbc);
        gbc.gridx = 1; 
        txtDesc = new JTextArea(4, 20);
        txtDesc.setLineWrap(true);
        formPanel.add(new JScrollPane(txtDesc), gbc);
        
        gbc.gridx = 0; gbc.gridy = 2; formPanel.add(new JLabel("Giá khởi điểm:"), gbc);
        gbc.gridx = 1; txtPrice = new JTextField(20); formPanel.add(txtPrice, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3; formPanel.add(new JLabel("Hình ảnh:"), gbc);
        gbc.gridx = 1; 
        JPanel imgPanel = new JPanel(new BorderLayout());
        btnSelectImage = new JButton("Chọn tệp...");
        lblImagePreview = new JLabel("Chưa chọn ảnh");
        lblImagePreview.setPreferredSize(new Dimension(150, 150));
        lblImagePreview.setHorizontalAlignment(SwingConstants.CENTER);
        lblImagePreview.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        
        imgPanel.add(btnSelectImage, BorderLayout.NORTH);
        imgPanel.add(lblImagePreview, BorderLayout.CENTER);
        formPanel.add(imgPanel, gbc);
        
        add(formPanel, BorderLayout.CENTER);
        
        btnSubmit = new JButton("Gửi sản phẩm cho Mod");
        btnSubmit.setFont(new Font("Arial", Font.BOLD, 14));
        btnSubmit.setPreferredSize(new Dimension(0, 40));
        add(btnSubmit, BorderLayout.SOUTH);
        
        btnSelectImage.addActionListener(e -> selectImage());
        btnSubmit.addActionListener(e -> submitProduct());
    }
    
    public void setTargetMod(String modUsername) {
        this.targetModUsername = modUsername;
        btnSubmit.setText("Gửi sản phẩm cho " + modUsername);
    }

    private void selectImage() {
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Hình ảnh (JPG, PNG)", "jpg", "jpeg", "png");
        chooser.setFileFilter(filter);
        int returnVal = chooser.showOpenDialog(this);
        if(returnVal == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (file.length() > 5 * 1024 * 1024) {
                JOptionPane.showMessageDialog(this, "Ảnh quá lớn. Vui lòng chọn tệp nhỏ hơn 5MB.");
                return;
            }
            try {
                BufferedImage originalImage = ImageIO.read(file);
                // Tạo ảnh thu nhỏ 300x300 với Graphics2D Antialiasing
                BufferedImage resizedImg = new BufferedImage(300, 300, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2 = resizedImg.createGraphics();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.drawImage(originalImage, 0, 0, 300, 300, null);
                g2.dispose();
                
                // Mã hoá thành Base64
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(resizedImg, "jpg", baos);
                byte[] imageBytes = baos.toByteArray();
                base64Image = Base64.getEncoder().encodeToString(imageBytes);
                
                // Hiển thị preview thu nhỏ hơn (150x150) cho vừa form
                Image preview = resizedImg.getScaledInstance(150, 150, Image.SCALE_SMOOTH);
                lblImagePreview.setIcon(new ImageIcon(preview));
                lblImagePreview.setText("");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi đọc ảnh: " + ex.getMessage());
            }
        }
    }

    private void submitProduct() {
        if (targetModUsername == null || targetModUsername.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một Moderator từ danh sách.");
            return;
        }
        String name = txtName.getText().trim();
        String desc = txtDesc.getText().trim();
        String price = txtPrice.getText().trim();
        
        if (name.isEmpty() || price.isEmpty() || base64Image.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập tên, giá và chọn ảnh.");
            return;
        }
        
        try {
            Long.parseLong(price);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Giá tiền không hợp lệ.");
            return;
        }

        Map<String, String> data = new HashMap<>();
        data.put("modUsername", targetModUsername);
        data.put("productName", name);
        data.put("description", desc);
        data.put("startingPrice", price);
        data.put("imageData", base64Image);
        
        NetworkClient.getInstance().sendMessage(MessageType.CONTACT_MOD, data);
        JOptionPane.showMessageDialog(this, "Đã gửi thông tin sản phẩm thành công!");
        
        // Reset form
        txtName.setText("");
        txtDesc.setText("");
        txtPrice.setText("");
        base64Image = "";
        lblImagePreview.setIcon(null);
        lblImagePreview.setText("Chưa chọn ảnh");
    }
}
