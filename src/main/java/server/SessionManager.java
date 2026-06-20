package server;

import model.User;
import shared.MessageType;
import shared.XmlMessageParser;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SessionManager {
    private static SessionManager instance;
    
    private static class Session {
        User user;
        long lastAccessTime;
        Session(User user) {
            this.user = user;
            this.lastAccessTime = System.currentTimeMillis();
        }
    }

    private final ConcurrentHashMap<String, Session> activeSessions = new ConcurrentHashMap<>();
    // Map username → ClientHandler để routing tin nhắn trực tiếp
    private final ConcurrentHashMap<String, ClientHandler> onlineHandlers = new ConcurrentHashMap<>();
    
    // Map offline messages: username -> List of Xml strings
    private final ConcurrentHashMap<String, java.util.List<String>> offlineMessages = new ConcurrentHashMap<>();
    
    public void addOfflineMessage(String username, String xmlMessage) {
        offlineMessages.computeIfAbsent(username, k -> new java.util.ArrayList<>()).add(xmlMessage);
    }
    
    public void sendOfflineMessages(String username, ClientHandler handler) {
        java.util.List<String> msgs = offlineMessages.remove(username);
        if (msgs != null) {
            for (String msg : msgs) {
                handler.sendMessage(msg);
            }
        }
    }

    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();

    private SessionManager() {
        // Tự động dọn dẹp token hết hạn sau 2 giờ
        cleaner.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            int removed = 0;
            for (String key : activeSessions.keySet()) {
                if (now - activeSessions.get(key).lastAccessTime > 7200000) {
                    activeSessions.remove(key);
                    removed++;
                }
            }
            if (removed > 0) {
                System.out.println("[Hệ thống] Đã dọn dẹp " + removed + " token hết hạn.");
            }
        }, 1, 1, TimeUnit.HOURS);
    }

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public String generateToken(User user) {
        String token = UUID.randomUUID().toString();
        activeSessions.put(token, new Session(user));
        return token;
    }

    public User validateToken(String token) {
        if (token == null) return null;
        Session session = activeSessions.get(token);
        if (session != null) {
            session.lastAccessTime = System.currentTimeMillis();
            return session.user;
        }
        return null;
    }

    public void invalidateToken(String token) {
        if (token != null) activeSessions.remove(token);
    }

    // ===== Handler Registry (để routing tin nhắn) =====

    /** Đăng ký ClientHandler online khi user đăng nhập thành công */
    public void registerHandler(String username, ClientHandler handler) {
        onlineHandlers.put(username, handler);
        System.out.println("[SessionManager] User '" + username + "' đã kết nối. Online: " + onlineHandlers.size());
        // Lệnh gửi tin nhắn offline được dời sang khi Client yêu cầu (SYNC_OFFLINE)
    }

    /** Hủy đăng ký khi user ngắt kết nối */
    public void unregisterHandler(String username) {
        if (username != null) {
            onlineHandlers.remove(username);
            System.out.println("[SessionManager] User '" + username + "' đã ngắt kết nối. Online: " + onlineHandlers.size());
        }
    }

    /** Tìm ClientHandler đang online theo username */
    public ClientHandler getHandlerByUsername(String username) {
        return onlineHandlers.get(username);
    }

    /** Lấy tất cả handlers đang online */
    public Collection<ClientHandler> getAllOnlineHandlers() {
        return onlineHandlers.values();
    }

    /**
     * Phát sóng cập nhật danh sách phòng đến TẤT CẢ clients đang online.
     * Được gọi sau khi tạo phòng mới.
     */
    public void broadcastRoomListUpdate() {
        StringBuilder sbAll = new StringBuilder();
        for (AuctionRoom room : AuctionManager.getInstance().listActiveRooms()) {
            if (sbAll.length() > 0) sbAll.append("|");
            sbAll.append(room.getRoomId()).append(",Phiên Đấu Giá,HệThống,ACTIVE");
        }
        Map<String, String> res = new HashMap<>();
        res.put("roomList", sbAll.toString());
        res.put("myRoomList", ""); // Clients sẽ tự lọc
        String xml = XmlMessageParser.serialize(MessageType.ROOM_INFO, res);

        for (ClientHandler handler : onlineHandlers.values()) {
            handler.sendMessage(xml);
        }
        System.out.println("[SessionManager] Đã broadcast room list tới " + onlineHandlers.size() + " clients.");
    }
}

