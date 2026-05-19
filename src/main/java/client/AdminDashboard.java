package client;

import shared.MessageType;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Map;

public class AdminDashboard extends JPanel implements NetworkClient.MessageListener {
    private JTabbedPane tabbedPane;
    private JTable tblUsers, tblRooms, tblPenalties;
    private DefaultTableModel userModel, roomModel, penaltyModel;
    private JLabel lblStats;

    public AdminDashboard(MainFrame mainFrame) {
        setLayout(new BorderLayout());
        
        tabbedPane = new JTabbedPane();
        
        userModel = new DefaultTableModel(new Object[]{"ID", "Username", "Vai trò", "Số dư", "Banned"}, 0);
        tblUsers = new JTable(userModel);
        tabbedPane.addTab("Quản lý Người Dùng", new JScrollPane(tblUsers));
        
        roomModel = new DefaultTableModel(new Object[]{"Mã phòng", "Moderator", "Trạng thái", "Bắt đầu", "Kết thúc"}, 0);
        tblRooms = new JTable(roomModel);
        tabbedPane.addTab("Toàn bộ Phòng Đấu Giá", new JScrollPane(tblRooms));
        
        penaltyModel = new DefaultTableModel(new Object[]{"ID Phạt", "User", "Lý do", "Số tiền", "Ngày phạt"}, 0);
        tblPenalties = new JTable(penaltyModel);
        tabbedPane.addTab("Lịch sử Phạt", new JScrollPane(tblPenalties));
        
        lblStats = new JLabel("Đang tải dữ liệu hệ thống...", SwingConstants.CENTER);
        lblStats.setFont(new Font("Arial", Font.BOLD, 16));
        lblStats.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        add(lblStats, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
        
        NetworkClient.getInstance().addListener(this);
        
        JButton btnRefresh = new JButton("Làm Mới Dữ Liệu Máy Chủ");
        btnRefresh.addActionListener(e -> fetchData());
        add(btnRefresh, BorderLayout.SOUTH);
        
        fetchData();
    }
    
    private void fetchData() {
        NetworkClient.getInstance().sendMessage(MessageType.GET_ADMIN_STATS, null);
        NetworkClient.getInstance().sendMessage(MessageType.GET_PENALTY_LIST, null);
    }

    @Override
    public void onMessage(MessageType type, Map<String, String> data) {
        SwingUtilities.invokeLater(() -> {
            if (type == MessageType.GET_ADMIN_STATS) {
                String sessions = data.get("totalSessions");
                String comm = data.get("totalCommission");
                lblStats.setText("<html>Thống kê: Tổng phiên đấu giá: <font color='red'><b>" + 
                                 (sessions!=null?sessions:"0") + 
                                 "</b></font> | Tổng doanh thu hoa hồng: <font color='red'><b>" + 
                                 (comm!=null?comm:"0") + " VND</b></font></html>");
                                 
                // Load Users
                String usersStr = data.get("users");
                userModel.setRowCount(0);
                if (usersStr != null && !usersStr.isEmpty()) {
                    for (String u : usersStr.split("\\|")) {
                        userModel.addRow(u.split(","));
                    }
                }
                
                // Load Rooms
                String roomsStr = data.get("rooms");
                roomModel.setRowCount(0);
                if (roomsStr != null && !roomsStr.isEmpty()) {
                    for (String r : roomsStr.split("\\|")) {
                        roomModel.addRow(r.split(","));
                    }
                }
                
            } else if (type == MessageType.GET_PENALTY_LIST) {
                String penStr = data.get("penalties");
                penaltyModel.setRowCount(0);
                if (penStr != null && !penStr.isEmpty()) {
                    for (String p : penStr.split("\\|")) {
                        penaltyModel.addRow(p.split(","));
                    }
                }
            }
        });
    }
}
