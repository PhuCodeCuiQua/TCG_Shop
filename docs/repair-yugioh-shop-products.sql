-- Khôi phục Product từ shop YuGiOh không có chủ sở hữu về shop có địa chỉ nhận hàng Quận 6.
-- Chỉ chạy sau khi xem kết quả preview và xác nhận đúng hai shop. Script không xóa cứng dữ liệu.

-- Preview shop mục tiêu theo địa chỉ.
SELECT id, name, owner_id, pickup_address, status, deleted
FROM shops
WHERE (deleted IS NULL OR deleted = 0)
  AND LOWER(pickup_address) LIKE '%quận 6%';

-- Preview shop YuGiOh không có chủ sở hữu hợp lệ cùng số sản phẩm đang trỏ tới shop.
SELECT s.id, s.name, s.owner_id, s.status, s.deleted, COUNT(p.id) AS product_count
FROM shops s
LEFT JOIN users u ON u.id = s.owner_id
LEFT JOIN products p ON p.shop_id = s.id
WHERE (s.deleted IS NULL OR s.deleted = 0)
  AND u.id IS NULL
  AND (
      LOWER(s.name) LIKE '%yugioh%'
      OR LOWER(s.name) LIKE '%yu-gi-oh%'
      OR LOWER(s.name) LIKE '%yugi%'
  )
GROUP BY s.id, s.name, s.owner_id, s.status, s.deleted;

START TRANSACTION;

-- Mỗi ID chỉ được chọn khi có đúng một bản ghi khớp; nếu mơ hồ, biến nhận NULL và UPDATE là no-op.
SELECT CASE WHEN COUNT(*) = 1 THEN MIN(s.id) ELSE NULL END
INTO @quand6_shop_id
FROM shops s
WHERE (s.deleted IS NULL OR s.deleted = 0)
  AND LOWER(s.pickup_address) LIKE '%quận 6%';

SELECT CASE WHEN COUNT(*) = 1 THEN MIN(s.id) ELSE NULL END
INTO @yugioh_orphan_shop_id
FROM shops s
LEFT JOIN users u ON u.id = s.owner_id
WHERE (s.deleted IS NULL OR s.deleted = 0)
  AND u.id IS NULL
  AND (
      LOWER(s.name) LIKE '%yugioh%'
      OR LOWER(s.name) LIKE '%yu-gi-oh%'
      OR LOWER(s.name) LIKE '%yugi%'
  )
  AND s.id <> COALESCE(@quand6_shop_id, -1);

SELECT @quand6_shop_id AS target_shop_id, @yugioh_orphan_shop_id AS orphan_shop_id;

UPDATE products
SET shop_id = @quand6_shop_id
WHERE shop_id = @yugioh_orphan_shop_id
  AND @quand6_shop_id IS NOT NULL
  AND @yugioh_orphan_shop_id IS NOT NULL;

-- Soft delete shop mồ côi sau khi đã chuyển toàn bộ Product, giữ nguyên các bản ghi giao dịch liên quan.
UPDATE shops
SET deleted = 1
WHERE id = @yugioh_orphan_shop_id
  AND @quand6_shop_id IS NOT NULL
  AND @yugioh_orphan_shop_id IS NOT NULL
  AND owner_id IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM products p
      WHERE p.shop_id = shops.id
  );

COMMIT;

-- Xác nhận dữ liệu sau khôi phục.
SELECT id, name, owner_id, pickup_address, status, deleted
FROM shops
WHERE id IN (@quand6_shop_id, @yugioh_orphan_shop_id);

SELECT shop_id, COUNT(*) AS product_count
FROM products
WHERE shop_id IN (@quand6_shop_id, @yugioh_orphan_shop_id)
GROUP BY shop_id;
