package server;

import model.Role;
import model.User;
import shared.MessageType;
import shared.XmlMessageParser;
import dao.UserDAO;

import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientHandler implements Runnable {
    private Socket socket;
    private InputStreamReader reader;
    private PrintWriter writer;
    private User currentUser;
    private long lastHeartbeatTime;
    
    private UserDAO userDAO = new UserDAO();

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.lastHeartbeatTime = System.currentTimeMillis();
        
        // Nền check timeout 90s
        new Thread(() -> {
            while (!socket.isClosed()) {
                try {
                    Thread.sleep(10000);
                    if (System.currentTimeMillis() - lastHeartbeatTime > 90000) {
                        System.out.println("[ClientHandler] Timeout, đóng kết nối.");
                        socket.close();
                        break;
                    }
                } catch (Exception e) { break; }
            }
        }).start();
    }

    @Override
    public void run() {
        try {
            reader = new InputStreamReader(socket.getInputStream(), "UTF-8");
            writer = new PrintWriter(socket.getOutputStream(), true, java.nio.charset.StandardCharsets.UTF_8);

            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[1024];
            int read;
            
            while ((read = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, read);
                int idx;
                while ((idx = sb.indexOf(XmlMessageParser.DELIMITER)) != -1) {
                    String xml = sb.substring(0, idx);
                    sb.delete(0, idx + XmlMessageParser.DELIMITER.length());
                    processMessage(xml);
                }
            }
        } catch (Exception e) {
            System.out.println("[ClientHandler] Ngắt kết nối: " + e.getMessage());
        } finally {
            // Hủy đăng ký khỏi danh sách online
            if (currentUser != null) {
                SessionManager.getInstance().unregisterHandler(currentUser.getUsername());
            }
            try { socket.close(); } catch (Exception e) {}
        }
    }

    private void processMessage(String xml) {
        Map<String, String> data = XmlMessageParser.deserialize(xml);
        String typeStr = data.get("type");
        if (typeStr == null) return;

        MessageType type;
        try { type = MessageType.valueOf(typeStr); } 
        catch (Exception e) { return; }

        if (type == MessageType.HEARTBEAT) {
            lastHeartbeatTime = System.currentTimeMillis();
            return;
        }

        if (type == MessageType.LOGIN) {
            handleLogin(data);
            return;
        } else if (type == MessageType.REGISTER) {
            handleRegister(data);
            return;
        }

        String token = data.get("sessionToken");
        currentUser = SessionManager.getInstance().validateToken(token);

        if (currentUser == null) {
            sendError("Token không hợp lệ hoặc đã hết hạn!");
            return;
        }

        try {
            switch (type) {
                case CREATE_ROOM: handleCreateRoom(data); break;
                case CREATE_ROOM_WITH_SELLERS: handleCreateRoomWithSellers(data); break;
                case JOIN_ROOM: handleJoinRoom(data); break;
                case LEAVE_ROOM: handleLeaveRoom(data); break;
                case PLACE_BID: handlePlaceBid(data); break;
                case OPEN_AUCTION: handleOpenAuction(data); break;
                case EXTEND_TIME: handleExtendTime(data); break;
                case NEXT_PRODUCT: handleNextProduct(data); break;
                case CLOSE_ROOM: handleCloseRoom(data); break;
                case GET_ROOM_LIST: handleGetRoomList(); break;
                case GET_MOD_LIST: handleGetModList(); break;
                case GET_USER_LIST: handleGetUserList(); break;
                case GET_HISTORY: handleGetHistory(); break;
                case GET_ROOM_DETAIL: handleGetRoomDetail(data); break;
                case GET_ADMIN_STATS: handleGetAdminStats(); break;
                case GET_PENALTY_LIST: handleGetPenaltyList(); break;
                case CONTACT_MOD: handleContactMod(data); break;
                case CHAT_PRIVATE: handleChatPrivate(data); break;
                case CHAT_ROOM: handleChatRoom(data); break;
                case CONFIRM_BUY: handleConfirmBuy(data); break;
                case EXPORT_HISTORY: handleExportHistory(); break;
                default:
                    System.out.println("[ClientHandler] Lệnh bỏ qua hoặc chưa cài đặt chi tiết: " + type);
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendError("Lỗi máy chủ khi xử lý: " + e.getMessage());
        }
    }

    private void handleLogin(Map<String, String> data) {
        User user = userDAO.login(data.get("username"), data.get("password"));
        if (user != null) {
            if (user.isBanned()) {
                sendError("Tài khoản của bạn đã bị khóa.");
                return;
            }
            String token = SessionManager.getInstance().generateToken(user);
            // Đăng ký ClientHandler này vào SessionManager để có thể tìm thấy qua username
            SessionManager.getInstance().registerHandler(user.getUsername(), this);
            Map<String, String> res = new HashMap<>();
            res.put("sessionToken", token);
            res.put("role", user.getRole().name());
            sendMessage(XmlMessageParser.serialize(MessageType.SUCCESS, res));
        } else {
            sendError("Sai tên đăng nhập hoặc mật khẩu.");
        }
    }

    private void handleRegister(Map<String, String> data) {
        Role role = Role.valueOf(data.get("role"));
        String result = userDAO.register(data.get("username"), data.get("password"), role);
        if ("SUCCESS".equals(result)) {
            sendMessage(XmlMessageParser.serialize(MessageType.SUCCESS, null));
        } else {
            sendError("Đăng ký lỗi: " + result);
        }
    }

    private void handleCreateRoom(Map<String, String> data) {
        if (currentUser.getRole() != Role.MODERATOR) {
            sendError("Chỉ Moderator mới có thể tạo phòng.");
            return;
        }
        String roomId = data.get("roomId");
        AuctionRoom room = AuctionManager.getInstance().createRoom(roomId, this);
        if (room != null) {
            System.out.println("[Server] Phòng '" + roomId + "' đã được tạo bởi " + currentUser.getUsername());
            Map<String, String> res = new HashMap<>();
            res.put("roomId", roomId);
            sendMessage(XmlMessageParser.serialize(MessageType.SUCCESS, res));
            SessionManager.getInstance().broadcastRoomListUpdate();
        } else {
            sendError("Phòng '" + roomId + "' đã tồn tại trên hệ thống.");
        }
    }

    private void handleCreateRoomWithSellers(Map<String, String> data) {
        if (currentUser.getRole() != Role.MODERATOR) {
            sendError("Chỉ Moderator mới có thể tạo phòng.");
            return;
        }
        String roomId    = data.get("roomId");
        String title     = data.getOrDefault("title", "Phiên Đấu Giá");
        String sellersStr = data.getOrDefault("sellers", "");
        String description = data.getOrDefault("description", "");

        AuctionRoom room = AuctionManager.getInstance().createRoom(roomId, this);
        if (room == null) {
            sendError("Phòng '" + roomId + "' đã tồn tại.");
            return;
        }
        room.setTitle(title);
        room.setDescription(description);

        System.out.println("[Server] Phòng '" + roomId + "' tạo bởi " + currentUser.getUsername()
            + ", sellers: " + sellersStr);

        // Gửi lời mời đến từng Seller được chọn
        if (!sellersStr.isBlank()) {
            for (String sellerName : sellersStr.split(",")) {
                sellerName = sellerName.trim();
                if (sellerName.isEmpty()) continue;
                ClientHandler sellerHandler = SessionManager.getInstance().getHandlerByUsername(sellerName);
                if (sellerHandler != null) {
                    Map<String, String> invite = new HashMap<>();
                    invite.put("roomId", roomId);
                    invite.put("title", title);
                    invite.put("modName", currentUser.getUsername());
                    invite.put("icon", "📨");
                    invite.put("notifTitle", "Mời tham gia phòng đấu giá");
                    invite.put("body", "Moderator " + currentUser.getUsername()
                        + " mời bạn tham gia phòng '" + title + "' (" + roomId + ")");
                    sellerHandler.sendMessage(XmlMessageParser.serialize(MessageType.INVITE_SELLER, invite));
                } else {
                    System.out.println("[Server] Seller '" + sellerName + "' không online, bỏ qua.");
                }
            }
        }

        Map<String, String> res = new HashMap<>();
        res.put("roomId", roomId);
        res.put("sellers", sellersStr);
        sendMessage(XmlMessageParser.serialize(MessageType.SUCCESS, res));
        SessionManager.getInstance().broadcastRoomListUpdate();
    }

    private void handleGetUserList() {
        // Trả về danh sách user role=USER không bị banned
        List<model.User> allUsers = userDAO.findAll();
        StringBuilder sb = new StringBuilder();
        for (model.User u : allUsers) {
            if (u.getRole() == model.Role.USER && !u.isBanned()) {
                if (sb.length() > 0) sb.append(",");
                sb.append(u.getUsername());
            }
        }
        Map<String, String> res = new HashMap<>();
        res.put("userList", sb.toString());
        sendMessage(XmlMessageParser.serialize(MessageType.GET_USER_LIST, res));
    }

    private void handleChatRoom(Map<String, String> data) {
        String roomId = data.get("roomId");
        AuctionRoom room = AuctionManager.getInstance().getRoom(roomId);
        if (room != null) {
            Map<String, String> broadcast = new HashMap<>(data);
            broadcast.put("senderName", currentUser.getUsername());
            room.broadcast(MessageType.CHAT_ROOM, broadcast);
        } else {
            sendError("Phòng '" + roomId + "' không tồn tại.");
        }
    }

    private void handleOpenAuction(Map<String, String> data) {
        if (currentUser.getRole() != Role.MODERATOR) return;
        AuctionRoom room = AuctionManager.getInstance().getRoom(data.get("roomId"));
        if (room != null && room.getModeratorHandler() == this) {
            // Load products from invited sellers
            int order = 1;
            List<String> sellers = room.getInvitedSellersList();
            dao.ProductDAO productDAO = new dao.ProductDAO();
            
            for (String sellerName : sellers) {
                model.User seller = userDAO.findByUsername(sellerName);
                if (seller != null) {
                    List<model.Product> prods = productDAO.findBySeller(seller.getId());
                    for (model.Product p : prods) {
                        if ("APPROVED".equals(p.getStatus())) {
                            model.SessionProduct sp = new model.SessionProduct();
                            sp.setProductId(p.getId());
                            sp.setOrderIndex(order++);
                            sp.setCurrentHighestBid(p.getStartingPrice());
                            room.addProduct(sp);
                        }
                    }
                }
            }
            if (order == 1) {
                sendError("Không có sản phẩm nào hợp lệ (APPROVED) từ các seller này để đấu giá.");
                return;
            }
            room.startAuction();
        }
    }

    private void handleExtendTime(Map<String, String> data) {
        if (currentUser.getRole() != Role.MODERATOR) return;
        AuctionRoom room = AuctionManager.getInstance().getRoom(data.get("roomId"));
        if (room != null && room.getModeratorHandler() == this) {
            room.extendTime(30); // Gia hạn thêm 30 giây
        }
    }

    private void handleNextProduct(Map<String, String> data) {
        if (currentUser.getRole() != Role.MODERATOR) return;
        AuctionRoom room = AuctionManager.getInstance().getRoom(data.get("roomId"));
        if (room != null && room.getModeratorHandler() == this) {
            room.forceNextProduct();
        }
    }

    private void handleCloseRoom(Map<String, String> data) {
        if (currentUser.getRole() != Role.MODERATOR) return;
        AuctionRoom room = AuctionManager.getInstance().getRoom(data.get("roomId"));
        if (room != null && room.getModeratorHandler() == this) {
            room.forceClose();
        }
    }

    private void handleLeaveRoom(Map<String, String> data) {
        String roomId = data.get("roomId");
        AuctionRoom room = AuctionManager.getInstance().getRoom(roomId);
        if (room != null) room.leaveRoom(this);
    }

    private void handleJoinRoom(Map<String, String> data) {
        String roomId = data.get("roomId");
        AuctionRoom room = AuctionManager.getInstance().getRoom(roomId);
        if (room != null) {
            room.joinRoom(this, "BUYER");
            Map<String, String> res = new HashMap<>();
            res.put("roomId", roomId);
            res.put("status", "JOINED");
            sendMessage(XmlMessageParser.serialize(MessageType.SUCCESS, res));
        } else sendError("Phòng không tồn tại.");
    }

    private void handlePlaceBid(Map<String, String> data) {
        String roomId = data.get("roomId");
        java.math.BigDecimal amount = new java.math.BigDecimal(data.get("amount"));
        // productSellerId: client có thể gửi 0 → server lấy từ room
        int productSellerId = 0;
        try { productSellerId = Integer.parseInt(data.getOrDefault("productSellerId", "0")); } catch (Exception ignored) {}

        AuctionRoom room = AuctionManager.getInstance().getRoom(roomId);
        if (room != null) {
            boolean success = room.placeBid(currentUser, amount, productSellerId);
            if (!success) sendError("Giá thầu không hợp lệ. Phải cao hơn giá hiện tại ít nhất 1 bước.");
        } else sendError("Phòng đã đóng.");
    }
    
    private void handleContactMod(Map<String, String> data) {
        String modName = data.get("modUsername"); // username cụ thể
        Map<String, String> msg = new HashMap<>(data);
        msg.put("senderName", currentUser.getUsername());

        if (modName != null && !modName.trim().isEmpty()) {
            // Gửi đến đúng Mod
            ClientHandler modHandler = SessionManager.getInstance().getHandlerByUsername(modName);
            if (modHandler != null) {
                modHandler.sendMessage(XmlMessageParser.serialize(MessageType.CONTACT_MOD, msg));
                Map<String, String> ack = new HashMap<>();
                ack.put("message", "Đã gửi thông tin đến " + modName + " thành công!");
                sendMessage(XmlMessageParser.serialize(MessageType.SUCCESS, ack));
            } else {
                sendError("Moderator '" + modName + "' hiện không trực tuyến.");
            }
        } else {
            // Broadcast đến tất cả Mod đang online
            boolean found = false;
            for (ClientHandler handler : SessionManager.getInstance().getAllOnlineHandlers()) {
                if (handler.currentUser != null && handler.currentUser.getRole() == Role.MODERATOR) {
                    handler.sendMessage(XmlMessageParser.serialize(MessageType.CONTACT_MOD, msg));
                    found = true;
                }
            }
            if (!found) sendError("Hiện không có Moderator nào trực tuyến.");
        }
    }

    private void handleChatPrivate(Map<String, String> data) {
        String receiverUsername = data.get("targetUser");
        if (receiverUsername == null) receiverUsername = data.get("receiverUsername");
        if (receiverUsername == null || receiverUsername.trim().isEmpty()) return;

        Map<String, String> msg = new HashMap<>();
        msg.put("senderName", currentUser.getUsername());
        msg.put("targetUser", receiverUsername);
        msg.put("message", data.getOrDefault("message", ""));

        // Gửi đến người nhận
        ClientHandler targetHandler = SessionManager.getInstance().getHandlerByUsername(receiverUsername);
        if (targetHandler != null) {
            targetHandler.sendMessage(XmlMessageParser.serialize(MessageType.CHAT_PRIVATE, msg));
        } else {
            sendError("Người dùng '" + receiverUsername + "' hiện không trực tuyến.");
        }
        // Gửi lại cho người gửi (để hiển thị bứt tin nhắn của mình)
        this.sendMessage(XmlMessageParser.serialize(MessageType.CHAT_PRIVATE, msg));
    }
    
    private void handleConfirmBuy(Map<String, String> data) {
        String status = data.get("status");
        if ("REJECTED".equals(status)) {
            try (java.sql.Connection conn = dao.DBConnection.getConnection()) {
                java.math.BigDecimal finalPrice = new java.math.BigDecimal(data.get("finalPrice"));
                java.math.BigDecimal penalty = finalPrice.multiply(new java.math.BigDecimal("0.10"));
                
                // Trừ balance bằng PreparedStatement
                try (java.sql.PreparedStatement ps = conn.prepareStatement(
                    "UPDATE users SET balance = balance - ? WHERE id = ?")) {
                    ps.setBigDecimal(1, penalty);
                    ps.setInt(2, currentUser.getId());
                    ps.executeUpdate();
                }
                
                // Lưu vào transactions
                try (java.sql.PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO transactions (user_id, amount, type) VALUES (?, ?, 'PENALTY')")) {
                    ps.setInt(1, currentUser.getId());
                    ps.setBigDecimal(2, penalty);
                    ps.executeUpdate();
                }
                sendError("Bạn đã từ chối mua và bị truy thu phạt " + penalty + " VND.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    private void handleExportHistory() {
        try {
            javax.xml.parsers.DocumentBuilderFactory dbFactory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            javax.xml.parsers.DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            org.w3c.dom.Document doc = dBuilder.newDocument();
            
            org.w3c.dom.Element rootElement = doc.createElement("auctionSession");
            doc.appendChild(rootElement);
            
            org.w3c.dom.Element roomId = doc.createElement("roomId");
            roomId.appendChild(doc.createTextNode("FakeRoom_123"));
            rootElement.appendChild(roomId);
            
            javax.xml.transform.TransformerFactory transformerFactory = javax.xml.transform.TransformerFactory.newInstance();
            javax.xml.transform.Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.ENCODING, "UTF-8");
            
            java.io.StringWriter xmlWriter = new java.io.StringWriter();
            javax.xml.transform.dom.DOMSource source = new javax.xml.transform.dom.DOMSource(doc);
            javax.xml.transform.stream.StreamResult result = new javax.xml.transform.stream.StreamResult(xmlWriter);
            transformer.transform(source, result);
            
            Map<String, String> res = new HashMap<>();
            res.put("xmlData", xmlWriter.toString());
            sendMessage(XmlMessageParser.serialize(MessageType.EXPORT_HISTORY, res));
        } catch(Exception e) {
            sendError("Lỗi khởi tạo XML Export trên máy chủ.");
        }
    }

    private void handleGetRoomList() {
        StringBuilder sbAll  = new StringBuilder();
        StringBuilder sbMine = new StringBuilder();
        for (AuctionRoom room : AuctionManager.getInstance().listActiveRooms()) {
            String title   = room.getTitle() != null ? room.getTitle() : "Phiên Đấu Giá";
            String sellers = room.getSellersDisplay();
            String info    = room.getRoomId() + "," + title + "," + sellers + ",ACTIVE";
            if (sbAll.length() > 0) sbAll.append("|");
            sbAll.append(info);
            if (room.getModeratorHandler() == this) {
                if (sbMine.length() > 0) sbMine.append("|");
                sbMine.append(info);
            }
        }
        Map<String, String> res = new HashMap<>();
        res.put("roomList",   sbAll.toString());
        res.put("myRoomList", sbMine.toString());
        sendMessage(XmlMessageParser.serialize(MessageType.ROOM_INFO, res));
    }

    private void handleGetModList() {
        // Lấy danh sách Moderator thực tế từ database
        List<model.User> allUsers = userDAO.findAll();
        StringBuilder sb = new StringBuilder();
        for (model.User u : allUsers) {
            if (u.getRole() == model.Role.MODERATOR && !u.isBanned()) {
                if (sb.length() > 0) sb.append(",");
                sb.append(u.getUsername());
            }
        }
        Map<String, String> res = new HashMap<>();
        res.put("modList", sb.toString());
        sendMessage(XmlMessageParser.serialize(MessageType.GET_MOD_LIST, res));
    }

    private void handleGetHistory() {
        Map<String, String> res = new HashMap<>();
        res.put("historyData", "- Đấu giá room1: Thất bại\n- Đấu giá roomVIP: Thắng (15.000.000 VND)\n");
        sendMessage(XmlMessageParser.serialize(MessageType.GET_HISTORY, res));
    }

    private void handleGetRoomDetail(Map<String, String> data) {
        Map<String, String> res = new HashMap<>();
        res.put("roomId", data.get("roomId"));
        res.put("details", "Loaded");
        sendMessage(XmlMessageParser.serialize(MessageType.GET_ROOM_DETAIL, res));
    }

    private void handleGetAdminStats() {
        if (currentUser.getRole() != Role.ADMIN) return;
        Map<String, String> res = new HashMap<>();
        res.put("totalSessions", "125");
        res.put("totalCommission", "18500000");
        res.put("users", "1,admin,ADMIN,0,false|2,user1,USER,500000,false");
        res.put("rooms", "room1,mod01,ACTIVE,2023-10-10,null|room2,mod02,ENDED,2023-10-09,2023-10-09");
        sendMessage(XmlMessageParser.serialize(MessageType.GET_ADMIN_STATS, res));
    }

    private void handleGetPenaltyList() {
        if (currentUser.getRole() != Role.ADMIN) return;
        Map<String, String> res = new HashMap<>();
        res.put("penalties", "1,user1,Từ chối mua SP 1,500000,2023-10-10|2,user2,Từ chối mua SP 3,200000,2023-10-09");
        sendMessage(XmlMessageParser.serialize(MessageType.GET_PENALTY_LIST, res));
    }

    public void sendError(String message) {
        Map<String, String> err = new HashMap<>();
        err.put("message", message);
        sendMessage(XmlMessageParser.serialize(MessageType.ERROR, err));
    }

    public void sendMessage(String xml) {
        if (writer != null) {
            writer.print(xml);
            writer.flush();
        }
    }
}
