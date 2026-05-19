package server;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Collection;

public class AuctionManager {
    private static AuctionManager instance;
    private final ConcurrentHashMap<String, AuctionRoom> rooms = new ConcurrentHashMap<>();

    private AuctionManager() {}

    public static synchronized AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }

    // Quản lý phòng đấu giá
    public AuctionRoom createRoom(String roomId, ClientHandler moderatorHandler) {
        if (rooms.containsKey(roomId)) {
            return null; // Phòng đã tồn tại
        }
        AuctionRoom room = new AuctionRoom(roomId, moderatorHandler);
        rooms.put(roomId, room);
        System.out.println("[AuctionManager] Đã tạo phòng đấu giá: " + roomId);
        return room;
    }

    public AuctionRoom getRoom(String roomId) {
        return rooms.get(roomId);
    }

    public void removeRoom(String roomId) {
        rooms.remove(roomId);
        System.out.println("[AuctionManager] Đã đóng và xóa phòng: " + roomId);
    }

    public Collection<AuctionRoom> listActiveRooms() {
        return rooms.values();
    }
}
