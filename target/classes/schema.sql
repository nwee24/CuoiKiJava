CREATE TABLE users (
  id SERIAL PRIMARY KEY,
  username VARCHAR(50) UNIQUE NOT NULL,
  password_hash TEXT NOT NULL,
  role VARCHAR(10) NOT NULL CHECK (role IN ('USER','MODERATOR','ADMIN')),
  rating DECIMAL(3,2) DEFAULT 0,
  rating_count INT DEFAULT 0,
  balance DECIMAL(15,2) DEFAULT 0,       -- số dư (dùng cho phạt, hoa hồng)
  is_banned BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE products (
  id SERIAL PRIMARY KEY,
  seller_id INT REFERENCES users(id),
  name VARCHAR(200) NOT NULL,
  description TEXT,
  image_data TEXT,                        -- Base64 ảnh
  starting_price DECIMAL(15,2) NOT NULL,
  status VARCHAR(20) DEFAULT 'PENDING'    -- PENDING, APPROVED, REJECTED, SOLD
);

CREATE TABLE auction_sessions (
  id SERIAL PRIMARY KEY,
  room_id VARCHAR(50) UNIQUE NOT NULL,
  moderator_id INT REFERENCES users(id),
  title VARCHAR(200),
  status VARCHAR(20) DEFAULT 'PREPARING', -- PREPARING, ACTIVE, ENDED, CANCELLED
  start_time TIMESTAMP,
  end_time TIMESTAMP,
  duration_seconds INT,
  extension_count INT DEFAULT 0,
  fixed_fee DECIMAL(15,2),
  commission_percent DECIMAL(5,2)
);

-- Nhiều sản phẩm trong 1 phiên, đấu giá lần lượt từng sp
CREATE TABLE session_products (
  id SERIAL PRIMARY KEY,
  session_id INT REFERENCES auction_sessions(id),
  product_id INT REFERENCES products(id),
  order_index INT,                        -- thứ tự đấu giá
  current_highest_bid DECIMAL(15,2),
  winner_id INT REFERENCES users(id),
  final_price DECIMAL(15,2),
  status VARCHAR(20) DEFAULT 'WAITING'    -- WAITING, ACTIVE, SOLD, PASSED
);

CREATE TABLE bids (
  id SERIAL PRIMARY KEY,
  session_product_id INT REFERENCES session_products(id),
  bidder_id INT REFERENCES users(id),
  amount DECIMAL(15,2),
  bid_time TIMESTAMP DEFAULT NOW()
);

CREATE TABLE chat_messages (
  id SERIAL PRIMARY KEY,
  room_id VARCHAR(50),                    -- NULL nếu là chat riêng
  sender_id INT REFERENCES users(id),
  receiver_id INT REFERENCES users(id),  -- NULL nếu là chat phòng
  content TEXT,
  sent_at TIMESTAMP DEFAULT NOW(),
  message_type VARCHAR(20)               -- ROOM, PRIVATE, SYSTEM
);

CREATE TABLE penalties (
  id SERIAL PRIMARY KEY,
  user_id INT REFERENCES users(id),
  session_product_id INT REFERENCES session_products(id),
  amount DECIMAL(15,2),
  reason TEXT,
  created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE ratings (
  id SERIAL PRIMARY KEY,
  rater_id INT REFERENCES users(id),
  rated_id INT REFERENCES users(id),
  session_id INT REFERENCES auction_sessions(id),
  score INT CHECK (score BETWEEN 1 AND 5),
  comment TEXT
);
