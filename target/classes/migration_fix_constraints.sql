-- Migration: Fix transaction_status constraint
-- Chạy script này trên PostgreSQL DB nếu constraint cũ không có ACCEPTED
-- Script an toàn (IF EXISTS / IDEMPOTENT):

-- 1. Xóa constraint cũ
ALTER TABLE session_products 
    DROP CONSTRAINT IF EXISTS session_products_transaction_status_check;

-- 2. Thêm constraint mới bao gồm PENDING, PAID, SHIPPED, COMPLETED (đủ dùng)
ALTER TABLE session_products
    ADD CONSTRAINT session_products_transaction_status_check
    CHECK (transaction_status IN ('PENDING', 'PAID', 'SHIPPED', 'COMPLETED'));

-- 3. Cập nhật dữ liệu cũ nếu có giá trị không hợp lệ
UPDATE session_products SET transaction_status = 'PENDING' 
WHERE transaction_status NOT IN ('PENDING', 'PAID', 'SHIPPED', 'COMPLETED');

-- 4. Đảm bảo products.status không bị UPDATE thành COMPLETED (không có trong constraint)
-- products.status constraint: PENDING, APPROVED, REJECTED, SOLD (giữ nguyên, không sửa)
