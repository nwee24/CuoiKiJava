package server;

import dao.ProductDAO;
import dao.UserDAO;
import model.Product;
import model.SessionProduct;
import model.User;
import shared.AppConfig;
import shared.MessageType;
import shared.XmlMessageParser;
import dao.AuctionSessionDAO;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class AuctionRoom {
    private String roomId;
    private String title;
    private String description;
    private final java.util.Set<String> invitedSellers = new java.util.LinkedHashSet<>();
    private ClientHandler moderatorHandler;
    private final CopyOnWriteArrayList<ClientHandler> sellerHandlers = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<ClientHandler> buyerHandlers = new CopyOnWriteArrayList<>();

    private final List<SessionProduct> products = new ArrayList<>();
    private int currentProductIndex = -1;

    // Dữ liệu sản phẩm hiện tại (cache để không query liên tục)
    private Product currentProduct = null;
    private String currentSellerName = null;

    private BigDecimal currentHighestBid = BigDecimal.ZERO;
    private Integer currentWinnerId = null;
    private String currentWinnerName = null;
    private int remainingSeconds = 0;
    private int extensionCount = 0;
    private final AtomicBoolean productEnding = new AtomicBoolean(false);
    private boolean waitingForProducts = false;

    private ScheduledExecutorService timerService;
    private ScheduledFuture<?> countdownTask;
    private AuctionSessionDAO auctionSessionDAO = new AuctionSessionDAO();
    private ProductDAO productDAO = new ProductDAO();
    private UserDAO userDAO = new UserDAO();

    public AuctionRoom(String roomId, ClientHandler moderatorHandler) {
        this.roomId = roomId;
        this.moderatorHandler = moderatorHandler;
    }

    public synchronized void joinRoom(ClientHandler client, String roleInRoom) {
        if ("SELLER".equals(roleInRoom)) sellerHandlers.add(client);
        else buyerHandlers.add(client);
    }

    public synchronized void leaveRoom(ClientHandler client) {
        sellerHandlers.remove(client);
        buyerHandlers.remove(client);
    }

    public void addProduct(SessionProduct sp) {
        products.add(sp);
    }

    public synchronized void startAuction() {
        System.out.println("[Phòng " + roomId + "] Bắt đầu phiên đấu giá.");
        auctionSessionDAO.startSession(roomId);
        SessionManager.getInstance().notifyAdminRoomChanged();
        nextProduct();
    }

    public synchronized void nextProduct() {
        productEnding.set(false);
        if (currentProductIndex + 1 < products.size()) {
            waitingForProducts = false;
            currentProductIndex++;
            SessionProduct sp = products.get(currentProductIndex);
            currentHighestBid = sp.getCurrentHighestBid() != null ? sp.getCurrentHighestBid() : BigDecimal.ZERO;
            currentWinnerId = null;
            currentWinnerName = null;
            remainingSeconds = AppConfig.getInstance().getDefaultDurationSeconds();
            extensionCount = 0;

            // Load thông tin sản phẩm từ DB
            currentProduct = productDAO.findById(sp.getProductId());
            if (currentProduct != null) {
                // Load tên seller
                User seller = userDAO.findById(currentProduct.getSellerId());
                currentSellerName = seller != null ? seller.getUsername() : "Không rõ";
            }

            System.out.println("[Phòng " + roomId + "] Chuyển sang sản phẩm: "
                + (currentProduct != null ? currentProduct.getName() : "ID " + sp.getProductId()));
            broadcastProductChange();
            startTimer();
        } else {
            waitingForProducts = true;
            System.out.println("[Phòng " + roomId + "] Đã đấu giá hết sản phẩm. Đợi thêm sản phẩm hoặc đóng phòng.");
            Map<String, String> msg = new HashMap<>();
            msg.put("roomId", roomId);
            msg.put("status", "WAITING_FOR_PRODUCTS");
            broadcast(MessageType.ROOM_STATUS_UPDATE, msg);
        }
    }

    public boolean isWaitingForProducts() {
        return waitingForProducts;
    }

    /** Moderator gia hạn thủ công */
    public synchronized void extendTime(int seconds) {
        remainingSeconds += seconds;
        extensionCount++;
        System.out.println("[Phòng " + roomId + "] Mod gia hạn thêm " + seconds + " giây.");
        broadcastBidUpdate();
    }

    /** Moderator bỏ qua sản phẩm hiện tại */
    public synchronized void forceNextProduct() {
        if (countdownTask != null) countdownTask.cancel(true);
        System.out.println("[Phòng " + roomId + "] Mod bỏ qua sản phẩm hiện tại.");
        endCurrentProduct();
    }

    /** Moderator đóng phòng */
    public void forceClose() {
        System.out.println("[Phòng " + roomId + "] Mod đóng phòng.");
        doCloseRoom();
    }

    private void startTimer() {
        if (timerService != null && !timerService.isShutdown()) {
            timerService.shutdownNow();
        }
        timerService = Executors.newSingleThreadScheduledExecutor();
        countdownTask = timerService.scheduleAtFixedRate(() -> {
            synchronized (this) {
                if (remainingSeconds > 0) {
                    remainingSeconds--;
                    // Broadcast đếm ngược mỗi 5 giây, hoặc 10 giây cuối thì mỗi giây
                    if (remainingSeconds % 5 == 0 || remainingSeconds <= 10) {
                        broadcastBidUpdate();
                    }
                } else {
                    if (!productEnding.getAndSet(true)) {
                        endCurrentProduct();
                    }
                }
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    public synchronized boolean placeBid(User user, BigDecimal amount, int productSellerId) {
        // Lấy sellerId từ sản phẩm hiện tại nếu client gửi 0
        int effectiveSellerId = productSellerId;
        if (effectiveSellerId == 0 && currentProduct != null) {
            effectiveSellerId = currentProduct.getSellerId();
        }
        if (effectiveSellerId != 0 && user.getId() == effectiveSellerId) {
            return false; // Không được đặt giá sản phẩm của chính mình
        }

        AppConfig config = AppConfig.getInstance();
        BigDecimal minIncrement = BigDecimal.valueOf(config.getMinBidIncrement());

        if (amount.compareTo(currentHighestBid.add(minIncrement)) >= 0) {
            currentHighestBid = amount;
            currentWinnerId = user.getId();
            currentWinnerName = user.getUsername();

            System.out.println("[Phòng " + roomId + "] " + user.getUsername() + " đặt giá " + amount);

            // Luật: Gia hạn nếu bid phút chót
            if (remainingSeconds <= config.getExtensionThresholdSeconds()) {
                remainingSeconds += config.getExtensionSeconds();
                extensionCount++;
                System.out.println("[Phòng " + roomId + "] Gia hạn thêm " + config.getExtensionSeconds() + " giây.");
            }
            broadcastBidUpdate();
            return true;
        }
        return false;
    }

    private void endCurrentProduct() {
        if (countdownTask != null) countdownTask.cancel(true);
        SessionProduct sp = products.get(currentProductIndex);

        System.out.println("[Phòng " + roomId + "] Chốt sản phẩm " + sp.getProductId() + " với giá " + currentHighestBid);

        String status = currentWinnerId != null ? "SOLD" : "PASSED";
        auctionSessionDAO.updateWinner(sp.getId(), currentWinnerId != null ? currentWinnerId : 0, currentHighestBid, status);

        Map<String, String> msg = new HashMap<>();
        msg.put("roomId", roomId);
        msg.put("productId", String.valueOf(sp.getProductId()));
        msg.put("sessionProductId", String.valueOf(sp.getId()));
        msg.put("productName", currentProduct != null ? currentProduct.getName() : "Sản phẩm");
        msg.put("finalPrice", currentHighestBid.toString());
        msg.put("winnerName", currentWinnerName != null ? currentWinnerName : "Không có người mua");
        broadcast(MessageType.AUCTION_END, msg);

        // Đợi 4 giây rồi chuyển sản phẩm tiếp theo
        Executors.newSingleThreadScheduledExecutor().schedule(this::nextProduct, 4, TimeUnit.SECONDS);
    }

    private void doCloseRoom() {
        if (timerService != null) timerService.shutdownNow();
        auctionSessionDAO.endSession(roomId, "ENDED");
        SessionManager.getInstance().notifyAdminRoomChanged();

        // Tính toán hoa hồng cho Moderator
        try (java.sql.Connection conn = dao.DBConnection.getConnection()) {
            java.math.BigDecimal total = currentHighestBid;
            java.math.BigDecimal commPercent = new java.math.BigDecimal("0.05"); // 5%
            java.math.BigDecimal modFee = total.multiply(commPercent);

            try (java.sql.PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO transactions (user_id, amount, type) VALUES (?, ?, 'COMMISSION')")) {
                ps.setInt(1, moderatorHandler != null ? 1 : 1); // TODO: lấy id Mod thực tế
                ps.setBigDecimal(2, modFee);
                ps.executeUpdate();
            }
            Map<String, String> modMsg = new HashMap<>();
            modMsg.put("message", "Phiên kết thúc. Hoa hồng của bạn: " + modFee + " VND");
            if (moderatorHandler != null) {
                moderatorHandler.sendMessage(XmlMessageParser.serialize(MessageType.SUCCESS, modMsg));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Map<String, String> msg = new HashMap<>();
        msg.put("roomId", roomId);
        msg.put("status", "CLOSED");
        broadcast(MessageType.ROOM_STATUS_UPDATE, msg);
        AuctionManager.getInstance().removeRoom(roomId);
    }

    private void broadcastBidUpdate() {
        Map<String, String> msg = new HashMap<>();
        msg.put("roomId", roomId);
        msg.put("highestBid", currentHighestBid.toString());
        msg.put("bidderName", currentWinnerName != null ? currentWinnerName : "");
        msg.put("remainingSeconds", String.valueOf(remainingSeconds));
        msg.put("extensionCount", String.valueOf(extensionCount));
        broadcast(MessageType.BID_UPDATE, msg);
    }

    private String getProductListForClient() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < products.size(); i++) {
            SessionProduct sp = products.get(i);
            Product p = productDAO.findById(sp.getProductId());
            String pName = p != null ? p.getName() : "Sản phẩm #" + sp.getProductId();
            String status = "WAITING";
            if (i < currentProductIndex) status = "ENDED";
            else if (i == currentProductIndex) status = "CURRENT";
            
            if (sb.length() > 0) sb.append("|");
            sb.append(sp.getProductId()).append(",")
              .append(pName.replace(",", ";").replace("|", "/")).append(",")
              .append(status);
        }
        return sb.toString();
    }

    public void broadcastProductListUpdate() {
        Map<String, String> msg = new HashMap<>();
        msg.put("roomId", roomId);
        msg.put("roomProducts", getProductListForClient());
        broadcast(MessageType.ROOM_PRODUCT_LIST_UPDATE, msg);
    }

    private void broadcastProductChange() {
        SessionProduct sp = products.get(currentProductIndex);
        Map<String, String> msg = new HashMap<>();
        msg.put("roomId", roomId);
        msg.put("productId", String.valueOf(sp.getProductId()));
        msg.put("startingPrice", currentHighestBid.toString());

        // Gửi đầy đủ thông tin sản phẩm
        if (currentProduct != null) {
            msg.put("productName",  currentProduct.getName()        != null ? currentProduct.getName() : "");
            msg.put("description",  currentProduct.getDescription() != null ? currentProduct.getDescription() : "");
            msg.put("imageData",    currentProduct.getImageData()   != null ? currentProduct.getImageData() : "");
            msg.put("sellerName",   currentSellerName != null ? currentSellerName : "");
        } else {
            msg.put("productName", "Sản phẩm #" + sp.getProductId());
            msg.put("description", "");
            msg.put("imageData", "");
            msg.put("sellerName", "");
        }

        // Index thứ tự
        msg.put("productIndex", String.valueOf(currentProductIndex + 1));
        msg.put("totalProducts", String.valueOf(products.size()));
        msg.put("roomProducts", getProductListForClient());
        broadcast(MessageType.PRODUCT_CHANGE, msg);
    }

    public synchronized void sendCurrentStateToClient(ClientHandler client) {
        if (currentProductIndex >= 0 && currentProductIndex < products.size() && !waitingForProducts) {
            SessionProduct sp = products.get(currentProductIndex);
            Map<String, String> msg = new HashMap<>();
            msg.put("roomId", roomId);
            msg.put("productId", String.valueOf(sp.getProductId()));
            msg.put("startingPrice", currentHighestBid.toString());

            if (currentProduct != null) {
                msg.put("productName",  currentProduct.getName()        != null ? currentProduct.getName() : "");
                msg.put("description",  currentProduct.getDescription() != null ? currentProduct.getDescription() : "");
                msg.put("imageData",    currentProduct.getImageData()   != null ? currentProduct.getImageData() : "");
                msg.put("sellerName",   currentSellerName != null ? currentSellerName : "");
            } else {
                msg.put("productName", "Sản phẩm #" + sp.getProductId());
                msg.put("description", "");
                msg.put("imageData", "");
                msg.put("sellerName", "");
            }

            msg.put("productIndex", String.valueOf(currentProductIndex + 1));
            msg.put("totalProducts", String.valueOf(products.size()));
            msg.put("roomProducts", getProductListForClient());
            client.sendMessage(XmlMessageParser.serialize(MessageType.PRODUCT_CHANGE, msg));
            
            // Sync bid state
            Map<String, String> bidMsg = new HashMap<>();
            bidMsg.put("roomId", roomId);
            bidMsg.put("highestBid", currentHighestBid.toString());
            bidMsg.put("bidderName", currentWinnerName != null ? currentWinnerName : "");
            bidMsg.put("remainingSeconds", String.valueOf(remainingSeconds));
            bidMsg.put("extensionCount", String.valueOf(extensionCount));
            client.sendMessage(XmlMessageParser.serialize(MessageType.BID_UPDATE, bidMsg));
        } else if (waitingForProducts) {
            Map<String, String> msg = new HashMap<>();
            msg.put("roomId", roomId);
            msg.put("status", "WAITING_FOR_PRODUCTS");
            client.sendMessage(XmlMessageParser.serialize(MessageType.ROOM_STATUS_UPDATE, msg));
        }
    }

    public void broadcast(MessageType type, Map<String, String> fields) {
        String xml = XmlMessageParser.serialize(type, fields);
        if (moderatorHandler != null) moderatorHandler.sendMessage(xml);
        for (ClientHandler ch : sellerHandlers) ch.sendMessage(xml);
        for (ClientHandler ch : buyerHandlers) ch.sendMessage(xml);
    }
    public String getRoomId() { return roomId; }
    public ClientHandler getModeratorHandler() { return moderatorHandler; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public void addInvitedSeller(String username) { invitedSellers.add(username); }
    public String getSellersDisplay() {
        return invitedSellers.isEmpty() ? "(Chưa có)" : String.join(" & ", invitedSellers);
    }
    public List<String> getInvitedSellersList() {
        return new ArrayList<>(invitedSellers);
    }
    public int getProductCount() {
        return products.size();
    }
    public List<SessionProduct> getProducts() {
        return products;
    }
    public Product getCurrentProduct() {
        return currentProduct;
    }
}
