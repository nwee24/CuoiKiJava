package client;

import shared.MessageType;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.swing.FontIcon;

/**
 * ProductSubmitPanel - Quản lý sản phẩm với tabs: Gửi Yêu Cầu | Đã Duyệt | Đã Bán | Từ Chối
 */
public class ProductSubmitPanel extends JPanel implements NetworkClient.MessageListener {
    
    private JTabbedPane tabbedPane;
    private DefaultTableModel modelPending, modelApproved, modelAuctioning, modelSold, modelRejected;
    private ProductSubmitForm submitForm;

    public ProductSubmitPanel() {
        setLayout(new BorderLayout());
        setBackground(UITheme.BG_CARD);
        
        NetworkClient.getInstance().addListener(this);
        buildUI();
        loadProducts();
    }

    // ... inside buildUI ...
    private void buildUI() {
        tabbedPane = new JTabbedPane();
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabbedPane.setBackground(UITheme.BG_CARD);
        
        submitForm = new ProductSubmitForm();
        tabbedPane.addTab("Gửi Yêu Cầu", FontIcon.of(Feather.EDIT_2, 16, UITheme.TEXT_MUTED), submitForm);
        
        tabbedPane.addTab("Chờ Duyệt", FontIcon.of(Feather.CLOCK, 16, UITheme.TEXT_MUTED), buildProductListTab("PENDING"));
        tabbedPane.addTab("Đã Duyệt", FontIcon.of(Feather.CHECK_CIRCLE, 16, UITheme.TEXT_MUTED), buildProductListTab("APPROVED"));
        tabbedPane.addTab("Đang Đấu Giá", FontIcon.of(Feather.ACTIVITY, 16, UITheme.TEXT_MUTED), buildProductListTab("AUCTIONING"));
        tabbedPane.addTab("Đã Bán", FontIcon.of(Feather.DOLLAR_SIGN, 16, UITheme.TEXT_MUTED), buildProductListTab("SOLD"));
        tabbedPane.addTab("Từ Chối", FontIcon.of(Feather.X_CIRCLE, 16, UITheme.TEXT_MUTED), buildProductListTab("REJECTED"));
        
        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel buildProductListTab(String status) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UITheme.BG_CARD);
        
        String[] columns = {"ID", "Tên Sản Phẩm", "Giá Khởi Điểm", "Trạng Thái"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        
        JTable table = UITheme.styledTable(model);
        panel.add(UITheme.styledScrollPane(table), BorderLayout.CENTER);
        
        if ("PENDING".equals(status)) {
            modelPending = model;
        } else if ("APPROVED".equals(status)) {
            modelApproved = model;
        } else if ("AUCTIONING".equals(status)) {
            modelAuctioning = model;
        } else if ("SOLD".equals(status)) {
            modelSold = model;
            JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            actionPanel.setBackground(UITheme.BG_CARD);
            JButton btnResale = UITheme.primaryBtn("Yêu cầu bán lại");
            btnResale.addActionListener(e -> {
                int row = table.getSelectedRow();
                if (row >= 0) {
                    String id = (String) model.getValueAt(row, 0);
                    int confirm = JOptionPane.showConfirmDialog(panel, "Bạn muốn yêu cầu bán lại sản phẩm này?", "Xác nhận", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        Map<String, String> req = new HashMap<>();
                        req.put("productId", id);
                        NetworkClient.getInstance().sendMessage(MessageType.REQUEST_RESALE, req);
                    }
                } else {
                    JOptionPane.showMessageDialog(panel, "Vui lòng chọn sản phẩm trong danh sách!");
                }
            });
            actionPanel.add(btnResale);
            panel.add(actionPanel, BorderLayout.SOUTH);
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
        modelPending.setRowCount(0);
        modelApproved.setRowCount(0);
        modelAuctioning.setRowCount(0);
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
                    
                    if ("PENDING".equals(status)) {
                        modelPending.addRow(row);
                    } else if ("APPROVED".equals(status)) {
                        modelApproved.addRow(row);
                    } else if ("AUCTIONING".equals(status)) {
                        modelAuctioning.addRow(row);
                    } else if ("SOLD".equals(status) || "COMPLETED".equals(status)) {
                        modelSold.addRow(row);
                    } else if ("REJECTED".equals(status)) {
                        modelRejected.addRow(row);
                    }
                }
            }
        }
    }
}
