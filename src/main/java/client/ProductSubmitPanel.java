package client;

import shared.MessageType;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * ProductSubmitPanel - Quản lý sản phẩm với tabs: Gửi Yêu Cầu | Đã Duyệt | Đã Bán | Từ Chối
 */
public class ProductSubmitPanel extends JPanel implements NetworkClient.MessageListener {
    
    private JTabbedPane tabbedPane;
    private DefaultTableModel modelApproved, modelSold, modelRejected;
    private ProductSubmitForm submitForm;

    public ProductSubmitPanel() {
        setLayout(new BorderLayout());
        setBackground(UITheme.BG_CARD);
        
        NetworkClient.getInstance().addListener(this);
        buildUI();
        loadProducts();
    }

    private void buildUI() {
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabbedPane.setBackground(UITheme.BG_CARD);
        
        // Tab 1: Gửi yêu cầu mới
        submitForm = new ProductSubmitForm();
        tabbedPane.addTab("📝 Gửi Yêu Cầu", submitForm);
        
        // Tab 2, 3, 4: Danh sách sản phẩm
        tabbedPane.addTab("✅ Đã Duyệt", buildProductListTab("APPROVED"));
        tabbedPane.addTab("💰 Đã Bán", buildProductListTab("SOLD"));
        tabbedPane.addTab("❌ Từ Chối", buildProductListTab("REJECTED"));
        
        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel buildProductListTab(String status) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UITheme.BG_CARD);
        
        String[] columns = {"ID", "Tên Sản Phẩm", "Giá Khởi Điểm", "Trạng Thái"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        
        JTable table = new JTable(model);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setRowHeight(36);
        table.setBackground(UITheme.BG_CARD);
        table.setSelectionBackground(new Color(255, 247, 237));
        table.setSelectionForeground(UITheme.ACCENT_DARK);
        table.setGridColor(UITheme.BORDER);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        table.getTableHeader().setBackground(new Color(249, 250, 251));
        
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new LineBorder(UITheme.BORDER, 1));
        panel.add(scroll, BorderLayout.CENTER);
        
        if ("APPROVED".equals(status)) {
            modelApproved = model;
        } else if ("SOLD".equals(status)) {
            modelSold = model;
        } else if ("REJECTED".equals(status)) {
            modelRejected = model;
        }
        
        return panel;
    }

    private void loadProducts() {
        NetworkClient.getInstance().sendMessage(MessageType.GET_MY_PRODUCTS, new HashMap<>());
    }

    public void setTargetMod(String modUsername) {
        submitForm.setTargetMod(modUsername);
    }

    @Override
    public void onMessage(MessageType type, Map<String, String> data) {
        SwingUtilities.invokeLater(() -> {
            if (type == MessageType.GET_MY_PRODUCTS) {
                updateProductTables(data.get("products"));
            } else if (type == MessageType.SUCCESS) {
                String msg = data.get("message");
                if (msg != null && msg.contains("sản phẩm")) {
                    loadProducts(); // Refresh
                }
            }
        });
    }

    private void updateProductTables(String productsStr) {
        modelApproved.setRowCount(0);
        modelSold.setRowCount(0);
        modelRejected.setRowCount(0);
        
        if (productsStr != null && !productsStr.isEmpty()) {
            for (String prodStr : productsStr.split("\\|")) {
                String[] parts = prodStr.split(",", 5);
                if (parts.length >= 4) {
                    String id = parts[0];
                    String name = parts[1];
                    String price = parts[2];
                    String status = parts[3];
                    
                    Object[] row = {id, name, price + " VND", status};
                    
                    if ("APPROVED".equals(status)) {
                        modelApproved.addRow(row);
                    } else if ("SOLD".equals(status)) {
                        modelSold.addRow(row);
                    } else if ("REJECTED".equals(status)) {
                        modelRejected.addRow(row);
                    }
                }
            }
        }
    }
}
