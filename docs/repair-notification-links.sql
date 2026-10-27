-- Chạy một lần trên MySQL 8 để sửa các thông báo cũ đang trỏ về /profile.
-- Đầu tiên kiểm tra trước các bản ghi sẽ được cập nhật.
SELECT id, type, title, content, link
FROM notifications
WHERE (link = '/profile' OR link IS NULL OR TRIM(link) = '')
  AND (
    type = 'SHOP_APPROVAL'
    OR (type = 'SYSTEM' AND title IN ('Sản phẩm đã bị gỡ khỏi hệ thống', 'Sản phẩm đã được khôi phục'))
    OR (type = 'ORDER_NEW' AND title = 'Đơn hàng mới' AND content LIKE 'Bạn có đơn hàng mới #%')
  );

START TRANSACTION;

UPDATE notifications
SET link = '/vendor/shop/manage'
WHERE type = 'SHOP_APPROVAL'
  AND (link = '/profile' OR link IS NULL OR TRIM(link) = '');

UPDATE notifications
SET link = '/vendor/products'
WHERE type = 'SYSTEM'
  AND title IN ('Sản phẩm đã bị gỡ khỏi hệ thống', 'Sản phẩm đã được khôi phục')
  AND (link = '/profile' OR link IS NULL OR TRIM(link) = '');

-- NotificationService cũ không lưu orderId vào reference_id; lấy order_code từ nội dung để tra đúng đơn.
UPDATE notifications AS n
JOIN orders AS o
  ON o.order_code = TRIM(SUBSTRING_INDEX(n.content, '#', -1))
SET n.link = CONCAT('/vendor/orders/', o.id)
WHERE n.type = 'ORDER_NEW'
  AND n.title = 'Đơn hàng mới'
  AND (n.link = '/profile' OR n.link IS NULL OR TRIM(n.link) = '')
  AND n.content LIKE 'Bạn có đơn hàng mới #%';

COMMIT;

-- Các thông báo ORDER_NEW không khớp order_code được giữ nguyên để tránh gán nhầm đơn.
SELECT id, type, title, content, link
FROM notifications
WHERE (link = '/profile' OR link IS NULL OR TRIM(link) = '')
  AND type IN ('SHOP_APPROVAL', 'SYSTEM', 'ORDER_NEW');
