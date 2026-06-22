-- ============================================================
--  SCHEMA ĐẤU GIÁ TRỰC TUYẾN  –  PostgreSQL
--  Đồng bộ với: model/*, dao/*, server/ClientHandler.java
-- ============================================================

-- ─────────────────────────────────────────────────────────────
--  1. USERS
--     DAO : UserDAO.extractUser() đọc:
--           id, username, password_hash, role, rating,
--           rating_count, balance, is_banned, created_at
--     INSERT: chỉ (username, password_hash, role) → các cột
--             còn lại có DEFAULT
-- ─────────────────────────────────────────────────────────────
CREATE TABLE users (
    id            SERIAL        PRIMARY KEY,
    username      VARCHAR(50)   UNIQUE NOT NULL,
    password_hash TEXT          NOT NULL,
    role          VARCHAR(10)   NOT NULL
                                CHECK (role IN ('USER', 'MODERATOR', 'ADMIN')),
    phone         VARCHAR(20),
    email         VARCHAR(100),
    rating        DECIMAL(3,2)  NOT NULL DEFAULT 0.00,
    rating_count  INT           NOT NULL DEFAULT 0,
    balance       DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    is_banned     BOOLEAN       NOT NULL DEFAULT FALSE,
    is_approved   BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- ─────────────────────────────────────────────────────────────
--  2. PRODUCTS
--     Model : Product.java KHÔNG có field created_at
--     DAO   : ProductDAO.extractProduct() đọc:
--             id, seller_id, name, description,
--             image_data, starting_price, status
--     INSERT: (seller_id, name, description, image_data,
--              starting_price, status)
--     updateStatus() dùng: PENDING, APPROVED, REJECTED, SOLD
-- ─────────────────────────────────────────────────────────────
CREATE TABLE products (
    id             SERIAL        PRIMARY KEY,
    seller_id      INT           REFERENCES users(id) ON DELETE SET NULL,
    name           VARCHAR(200)  NOT NULL,
    description    TEXT,
    image_data     TEXT,                          -- ảnh mã hoá Base64
    starting_price DECIMAL(15,2) NOT NULL,
    status         VARCHAR(20)   NOT NULL DEFAULT 'PENDING'
                                 CHECK (status IN ('PENDING','APPROVED','REJECTED','SOLD'))
);

-- ─────────────────────────────────────────────────────────────
--  3. AUCTION_SESSIONS
--     DAO   : AuctionSessionDAO.extractSession() đọc:
--             id, room_id, moderator_id, title, status,
--             start_time, end_time, duration_seconds,
--             extension_count, fixed_fee, commission_percent
--     INSERT từ DAO: 9 cột đầy đủ
--     INSERT từ ClientHandler.handleCreateRoomWithSellers():
--             chỉ (room_id, moderator_id, title, status)
--             → fixed_fee, commission_percent phải có DEFAULT
-- ─────────────────────────────────────────────────────────────
CREATE TABLE auction_sessions (
    id                 SERIAL        PRIMARY KEY,
    room_id            VARCHAR(50)   UNIQUE NOT NULL,
    moderator_id       INT           REFERENCES users(id) ON DELETE SET NULL,
    title              VARCHAR(200),
    status             VARCHAR(20)   NOT NULL DEFAULT 'PREPARING'
                                     CHECK (status IN ('PREPARING','ACTIVE','ENDED','CANCELLED')),
    start_time         TIMESTAMP,
    end_time           TIMESTAMP,
    duration_seconds   INT           NOT NULL DEFAULT 60,
    extension_count    INT           NOT NULL DEFAULT 0,
    fixed_fee          DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    commission_percent DECIMAL(5,2)  NOT NULL DEFAULT 0.00
);

-- ─────────────────────────────────────────────────────────────
--  4. SESSION_PRODUCTS
--     DAO   : AuctionSessionDAO.extractSessionProduct() đọc:
--             id, session_id, product_id, order_index,
--             current_highest_bid, winner_id, final_price, status
--     INSERT từ DAO.addProduct():
--             chỉ (session_id, product_id, order_index, status)
--             → current_highest_bid PHẢI có DEFAULT
--     INSERT từ ClientHandler.handleCreateRoomWithSellers():
--             (session_id, product_id, order_index,
--              current_highest_bid, status) — 5 cột, có giá trị
--     Trạng thái: WAITING → ACTIVE → SOLD / PASSED
-- ─────────────────────────────────────────────────────────────
CREATE TABLE session_products (
    id                  SERIAL        PRIMARY KEY,
    session_id          INT           REFERENCES auction_sessions(id) ON DELETE CASCADE,
    product_id          INT           REFERENCES products(id) ON DELETE SET NULL,
    order_index         INT           NOT NULL DEFAULT 1,
    current_highest_bid DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    winner_id           INT           REFERENCES users(id) ON DELETE SET NULL,
    final_price         DECIMAL(15,2),
    status              VARCHAR(20)   NOT NULL DEFAULT 'WAITING'
                                      CHECK (status IN ('WAITING','ACTIVE','SOLD','PASSED')),
    transaction_status  VARCHAR(20)   NOT NULL DEFAULT 'PENDING'
                                      CHECK (transaction_status IN ('PENDING', 'PAID', 'SHIPPED', 'COMPLETED'))
);

-- ─────────────────────────────────────────────────────────────
--  5. BIDS
--     DAO   : BidDAO.extractBid() đọc:
--             id, session_product_id, bidder_id, amount, bid_time
--     INSERT: (session_product_id, bidder_id, amount)
--             → bid_time tự sinh qua DEFAULT
-- ─────────────────────────────────────────────────────────────
CREATE TABLE bids (
    id                 SERIAL        PRIMARY KEY,
    session_product_id INT           REFERENCES session_products(id) ON DELETE CASCADE,
    bidder_id          INT           REFERENCES users(id) ON DELETE SET NULL,
    amount             DECIMAL(15,2) NOT NULL,
    bid_time           TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- ─────────────────────────────────────────────────────────────
--  6. CHAT_MESSAGES
--     DAO   : ChatDAO.extractMsg() đọc:
--             id, room_id, sender_id, receiver_id,
--             content, sent_at, message_type
--     INSERT: (room_id, sender_id, receiver_id, content,
--              message_type)  → sent_at tự sinh qua DEFAULT
--     sender_id / receiver_id có thể NULL (message hệ thống)
--     Loại: ROOM | PRIVATE | SYSTEM
-- ─────────────────────────────────────────────────────────────
CREATE TABLE chat_messages (
    id           SERIAL      PRIMARY KEY,
    room_id      VARCHAR(50),                       -- NULL nếu là chat riêng
    sender_id    INT         REFERENCES users(id) ON DELETE SET NULL,
    receiver_id  INT         REFERENCES users(id) ON DELETE SET NULL,
    content      TEXT        NOT NULL,
    sent_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    message_type VARCHAR(20) NOT NULL
                             CHECK (message_type IN ('ROOM','PRIVATE','SYSTEM'))
);

-- ─────────────────────────────────────────────────────────────
--  7. PENALTIES
--     DAO   : PenaltyDAO.findByUser() đọc:
--             id, user_id, session_product_id,
--             amount, reason, created_at
--     INSERT từ DAO: (user_id, session_product_id, amount, reason)
--             → created_at tự sinh qua DEFAULT
--     session_product_id có thể NULL
-- ─────────────────────────────────────────────────────────────
CREATE TABLE penalties (
    id                 SERIAL        PRIMARY KEY,
    user_id            INT           REFERENCES users(id) ON DELETE CASCADE,
    session_product_id INT           REFERENCES session_products(id) ON DELETE SET NULL,
    amount             DECIMAL(15,2) NOT NULL,
    reason             TEXT,
    created_at         TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- ─────────────────────────────────────────────────────────────
--  8. TRANSACTIONS
--     Dùng bởi ClientHandler.handleConfirmBuy() khi buyer
--     từ chối mua → ghi phạt vào đây.
--     INSERT: (user_id, amount, type)
--     type: 'PENALTY' | 'COMMISSION' | 'PAYMENT'
-- ─────────────────────────────────────────────────────────────
CREATE TABLE transactions (
    id         SERIAL        PRIMARY KEY,
    user_id    INT           REFERENCES users(id) ON DELETE CASCADE,
    amount     DECIMAL(15,2) NOT NULL,
    type       VARCHAR(20)   NOT NULL
                             CHECK (type IN ('PENALTY','COMMISSION','PAYMENT')),
    note       TEXT,
    created_at TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- ─────────────────────────────────────────────────────────────
--  9. RATINGS
--     Model : Rating.java — KHÔNG có field created_at
--     Các field: id, rater_id, rated_id, session_id,
--                score, comment
-- ─────────────────────────────────────────────────────────────
CREATE TABLE ratings (
    id         SERIAL PRIMARY KEY,
    rater_id   INT    REFERENCES users(id) ON DELETE CASCADE,
    rated_id   INT    REFERENCES users(id) ON DELETE CASCADE,
    session_id INT    REFERENCES auction_sessions(id) ON DELETE SET NULL,
    score      INT    NOT NULL CHECK (score BETWEEN 1 AND 5),
    comment    TEXT
);

-- ─────────────────────────────────────────────────────────────
--  INDEX — tăng hiệu năng các truy vấn thường dùng trong DAO
-- ─────────────────────────────────────────────────────────────
CREATE INDEX idx_products_seller    ON products(seller_id);
CREATE INDEX idx_products_status    ON products(status);
CREATE INDEX idx_sessions_room      ON auction_sessions(room_id);
CREATE INDEX idx_sp_session         ON session_products(session_id);
CREATE INDEX idx_sp_status          ON session_products(status, order_index);
CREATE INDEX idx_bids_sp            ON bids(session_product_id);
CREATE INDEX idx_bids_amount        ON bids(session_product_id, amount DESC);
CREATE INDEX idx_chat_room          ON chat_messages(room_id);
CREATE INDEX idx_chat_private       ON chat_messages(sender_id, receiver_id);
CREATE INDEX idx_penalties_user     ON penalties(user_id);
CREATE INDEX idx_transactions_user  ON transactions(user_id);
