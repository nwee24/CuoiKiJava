# Hệ Thống Đấu Giá Trực Tuyến bằng Java Socket (SSL)

Ứng dụng Đấu giá trực tuyến Client-Server được xây dựng bằng Java Swing và Socket bảo mật SSL. Dưới đây là hướng dẫn cài đặt và chạy dự án hoàn toàn thông qua giao diện đồ họa (GUI), không cần dùng dòng lệnh (Terminal).

---

## 1. Thiết Lập Cơ Sở Dữ Liệu (Bằng pgAdmin 4)
Thay vì dùng dòng lệnh `psql`, bạn hãy sử dụng công cụ quản lý đồ họa của PostgreSQL là **pgAdmin 4** (thường được cài sẵn cùng PostgreSQL).

1. Mở **pgAdmin 4** và đăng nhập vào máy chủ PostgreSQL của bạn (thường là `PostgreSQL 12` hoặc phiên bản bạn đang dùng).
2. Chuột phải vào mục **Databases** -> Chọn **Create** -> **Database...**
3. Ở ô *Database*, nhập tên là: `auction_db` rồi nhấn **Save**.
4. Mở rộng database `auction_db` vừa tạo. Chuột phải vào nó và chọn **Query Tool** (Công cụ truy vấn).
5. Mở file `src/main/resources/schema.sql` trong dự án bằng Notepad hoặc IDE, **copy toàn bộ nội dung** bên trong.
6. Dán nội dung đó vào cửa sổ Query Tool của pgAdmin và bấm nút **Execute** (Biểu tượng nút Play màu trắng/xanh có hình tam giác) hoặc nhấn phím `F5`.
7. Trạng thái báo *Query returned successfully* nghĩa là bạn đã tạo xong bảng.

---

## 2. Tạo Chứng Chỉ Bảo Mật SSL (Bằng KeyStore Explorer)
Để Server chạy được kết nối bảo mật (WSS/SSL), bạn cần 1 file `keystore.jks`. Nếu không muốn dùng dòng lệnh `keytool`, bạn có thể dùng phần mềm đồ họa **KeyStore Explorer** (tải miễn phí tại keystore-explorer.org).

1. Mở phần mềm **KeyStore Explorer**. Chọn **Create a new KeyStore**.
2. Chọn định dạng **JKS** và bấm OK.
3. Bấm nút **Generate Key Pair** (Biểu tượng chìa khóa có dấu cộng). Chọn thuật toán **RSA** -> OK -> OK.
4. Ở bảng *Name*, bấm biểu tượng quyển sổ nhỏ ở mục Name, nhập dòng *Common Name (CN)* là `auction`, sau đó bấm OK.
5. Bấm OK tiếp, phần mềm sẽ hỏi bạn đặt Mật khẩu (Password). **Bắt buộc nhập**: `changeit` (cả 2 ô).
6. Bấm nút **Save** (Lưu file), phần mềm sẽ hỏi mật khẩu của toàn bộ KeyStore một lần nữa. Lại nhập: `changeit`.
7. Lưu file với tên `keystore.jks` vào thư mục: `<Thư mục Project>/src/main/resources/`.

*(Lưu ý: Nếu việc này quá phức tạp, bạn vẫn có thể mở một cửa sổ terminal nhỏ ngay bên trong IntelliJ/Eclipse và dán lệnh sau để tự động tạo file mà không cần bấm nhiều: `keytool -genkey -alias auction -keyalg RSA -keystore src/main/resources/keystore.jks -storepass changeit -validity 365`)*

---

## 3. Cấu Hình Thông Số Mạng & Database
Mở file `src/main/resources/config.xml` trực tiếp trong trình soạn thảo code (IDE) của bạn. 
Hãy kiểm tra và sửa lại mật khẩu ở thẻ `<password>` thành mật khẩu PostgreSQL mà bạn đã đặt trên máy.

```xml
<database>
    <url>jdbc:postgresql://localhost:5432/auction_db</url>
    <user>postgres</user>
    <!-- THAY ĐỔI MẬT KHẨU CỦA BẠN VÀO ĐÂY -->
    <password>123456</password>
</database>
```

---

## 4. Chạy Ứng Dụng Bằng IDE (IntelliJ IDEA / Eclipse)
Mọi thao tác biên dịch và chạy giờ đây chỉ cần click chuột bằng nút Run (Play) màu xanh lá cây trong IDE của bạn.

**Bước 1: Khởi Động Máy Chủ (Server)**
- Mở file `src/main/java/server/AuctionServer.java`.
- Bấm nút **Run** (biểu tượng ▶️ màu xanh lá) nằm cạnh hàm `public static void main` (Hoặc chuột phải -> chọn *Run 'AuctionServer.main()'*).
- Khi Console dưới đáy màn hình hiện thông báo *Máy chủ đang lắng nghe...*, tức là Server đã hoạt động.

**Bước 2: Mở Các Khách Hàng (Client)**
- Mở file `src/main/java/client/MainFrame.java`.
- Bấm nút **Run** tương tự như trên để mở giao diện cửa sổ phần mềm Đấu Giá.
- Bạn có thể **bấm Run MainFrame nhiều lần** để mở 2, 3 cửa sổ khác nhau (Tượng trưng cho việc mở máy tính của người dùng 1, người dùng 2, Mod,...).

Chúc bạn thành công chạy thử nghiệm dự án!
