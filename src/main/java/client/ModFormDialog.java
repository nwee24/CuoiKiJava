package client;

import shared.MessageType;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import net.miginfocom.swing.MigLayout;

public class ModFormDialog extends JDialog {
    private JTextField txtUsername;
    private JTextField txtEmail;
    private JTextField txtPhone;
    private JPasswordField txtPassword;

    private final boolean isEditMode;
    private final String modId;

    public ModFormDialog(Frame parent, boolean isEditMode, String modId,
                         String currentUsername, String currentEmail, String currentPhone) {
        super(parent, isEditMode ? "Sửa thông tin Moderator" : "Thêm Moderator mới", true);
        this.isEditMode = isEditMode;
        this.modId = modId;

        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UITheme.BG_DARK);
        setContentPane(root);

        // ── Header ──────────────────────────────────────────────────────────────
        JPanel header = new JPanel(new MigLayout("insets 20 24 16 24"));
        header.setBackground(UITheme.BG_ELEVATED);

        JLabel lblTitle = new JLabel(isEditMode ? "✏️ Sửa Moderator" : "➕ Thêm Moderator Mới");
        lblTitle.setFont(UITheme.fontBold(18));
        lblTitle.setForeground(UITheme.TEXT_PRIMARY);
        header.add(lblTitle);

        JLabel lblSub = new JLabel(isEditMode
                ? "Cập nhật thông tin tài khoản Moderator"
                : "Tạo tài khoản Moderator mới, tự động được duyệt");
        lblSub.setFont(UITheme.fontBody(13));
        lblSub.setForeground(UITheme.TEXT_MUTED);
        header.add(lblSub, "newline");

        root.add(header, BorderLayout.NORTH);

        // ── Form Body ────────────────────────────────────────────────────────────
        // Use a 2-col grid: [label width][input fixed width]
        JPanel form = new JPanel(new MigLayout(
                "wrap 2, insets 24, gap 8 14",
                "[100px,right][280px,fill]"
        ));
        form.setBackground(UITheme.BG_DARK);

        // Username
        form.add(fieldLabel("Tên đăng nhập:"));
        txtUsername = UITheme.darkTextField(isEditMode ? "" : "Nhập tên đăng nhập...");
        txtUsername.setPreferredSize(new Dimension(280, 38));
        if (isEditMode) {
            txtUsername.setText(currentUsername != null ? currentUsername : "");
            txtUsername.setEditable(false);
            txtUsername.setToolTipText("Không thể thay đổi tên đăng nhập");
            txtUsername.setForeground(UITheme.TEXT_MUTED);
        }
        form.add(txtUsername);

        // Email
        form.add(fieldLabel("Email:"));
        txtEmail = UITheme.darkTextField("Nhập địa chỉ email...");
        txtEmail.setPreferredSize(new Dimension(280, 38));
        if (currentEmail != null) txtEmail.setText(currentEmail);
        form.add(txtEmail);

        // Phone
        form.add(fieldLabel("Số điện thoại:"));
        txtPhone = UITheme.darkTextField("Nhập số điện thoại...");
        txtPhone.setPreferredSize(new Dimension(280, 38));
        if (currentPhone != null) txtPhone.setText(currentPhone);
        form.add(txtPhone);

        // Password
        form.add(fieldLabel(isEditMode ? "Mật khẩu mới:" : "Mật khẩu:"));
        txtPassword = UITheme.darkPasswordField(
                isEditMode ? "(Bỏ trống = không đổi)" : "Nhập mật khẩu...");
        txtPassword.setPreferredSize(new Dimension(280, 38));
        form.add(txtPassword);

        root.add(form, BorderLayout.CENTER);

        // ── Buttons ──────────────────────────────────────────────────────────────
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        bottom.setBackground(UITheme.BG_DARK);

        JButton btnCancel = new JButton("Hủy bỏ");
        btnCancel.setFocusPainted(false);
        btnCancel.setPreferredSize(new Dimension(100, 36));
        btnCancel.addActionListener(e -> dispose());

        JButton btnSubmit = UITheme.primaryBtn(isEditMode ? "Lưu thay đổi" : "Tạo tài khoản");
        btnSubmit.setPreferredSize(new Dimension(140, 36));
        btnSubmit.addActionListener(e -> handleSubmit());

        bottom.add(btnCancel);
        bottom.add(btnSubmit);
        root.add(bottom, BorderLayout.SOUTH);

        pack();
        setMinimumSize(new Dimension(460, 350));
        setLocationRelativeTo(parent);
    }

    private JLabel fieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(UITheme.fontBold(13));
        lbl.setForeground(UITheme.TEXT_MUTED);
        return lbl;
    }

    private void handleSubmit() {
        String username = txtUsername.getText().trim();
        String email    = txtEmail.getText().trim();
        String phone    = txtPhone.getText().trim();
        String pass     = new String(txtPassword.getPassword()).trim();

        // Validation
        if (username.isEmpty()) {
            showError("Tên đăng nhập không được để trống!");
            return;
        }
        if (email.isEmpty()) {
            showError("Email không được để trống!");
            return;
        }
        if (phone.isEmpty()) {
            showError("Số điện thoại không được để trống!");
            return;
        }
        if (!isEditMode && pass.isEmpty()) {
            showError("Mật khẩu không được để trống khi tạo tài khoản mới!");
            return;
        }

        Map<String, String> data = new HashMap<>();
        if (isEditMode) {
            data.put("userId",   modId);
            data.put("email",    email);
            data.put("phone",    phone);
            data.put("password", pass); // empty = no change
            NetworkClient.getInstance().sendMessage(MessageType.UPDATE_MOD_INFO, data);
        } else {
            data.put("username", username);
            data.put("password", pass);
            data.put("email",    email);
            data.put("phone",    phone);
            NetworkClient.getInstance().sendMessage(MessageType.CREATE_MOD_BY_ADMIN, data);
        }

        JOptionPane.showMessageDialog(this,
                isEditMode ? "Đã gửi yêu cầu cập nhật!" : "Đã gửi yêu cầu tạo Moderator!",
                "Thành công", JOptionPane.INFORMATION_MESSAGE);
        dispose();
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Lỗi nhập liệu", JOptionPane.ERROR_MESSAGE);
    }
}
