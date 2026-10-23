# ENTITY SPEC: ĐẶC TẢ ENTITY CHỐT (đặt tại `docs/ENTITY_SPEC.md`)

> Nguồn: ERD của Phát + các điều chỉnh của Phú để khớp với task đã phân công.
> Đây là **nguồn sự thật duy nhất** cho Entity. AI phải đọc file này cùng `AGENTS.md` khi tạo Entity.
> Ký hiệu: **[+]** = cột/bảng mới so với ERD gốc; **[sửa]** = cột đã đổi so với ERD gốc. Phát cập nhật lại ERD theo các dấu này để báo cáo khớp với database thật.

---

## QUY TẮC CHUNG CHO MỌI ENTITY

- Kế thừa `BaseEntity` (id `Long` IDENTITY, `createdAt`, `updatedAt` dùng JPA auditing). Riêng bảng nối `user_roles` và `promotion_products` không có `id`/timestamp: dùng `@ManyToMany` + `@JoinTable`.
- `@Getter @Setter @NoArgsConstructor` (+ `@Builder @AllArgsConstructor` nếu cần). **Không dùng `@Data`/`@ToString`.**
- Mọi `@ManyToOne` / `@OneToOne` khai báo `fetch = FetchType.LAZY` rõ ràng.
- Enum: `@Enumerated(EnumType.STRING)`. Tiền: `BigDecimal` cột `DECIMAL(15,2)`; phần trăm: `DECIMAL(5,2)`.
- Tên bảng số nhiều snake_case, tên cột đúng như bảng dưới. **Tránh từ khóa dành riêng của MySQL** (đã đổi `condition` thành `card_condition`).
- Chỉ khai báo `@OneToMany` khi được nêu rõ trong spec, còn lại chỉ dùng `@ManyToOne` một chiều.
- Mật khẩu/OTP/token chỉ lưu dạng **hash**.

---

# PHẦN 0: ENUM VÀ HẠ TẦNG

Package `com.tcgshop.entity.enums`. Mỗi enum có thuộc tính `displayName` + getter.

| Enum | Giá trị |
|---|---|
| `RoleName` | `ROLE_USER`, `ROLE_VENDOR`, `ROLE_MANAGER`, `ROLE_ADMIN`, `ROLE_SHIPPER` (**không lưu `ROLE_GUEST`**: Guest là người chưa đăng nhập) |
| `UserStatus` | `ACTIVE`, `INACTIVE` (chưa kích hoạt OTP), `BLOCKED` |
| `OtpPurpose` | `REGISTER`, `FORGOT_PASSWORD` |
| `ShopStatus` | `PENDING`, `APPROVED`, `REJECTED`, `SUSPENDED` |
| `ProductType` | `SINGLE`, `BOOSTER_BOX`, `BOOSTER_PACK`, `BUNDLE`, `SUPPLY` |
| `ProductStatus` | `ACTIVE`, `INACTIVE`, `OUT_OF_STOCK`, `BANNED` **[+ BANNED: Manager gỡ vi phạm]** |
| `CardCondition` | `MINT`, `NEAR_MINT`, `LIGHTLY_PLAYED`, `MODERATELY_PLAYED`, `HEAVILY_PLAYED`, `DAMAGED` (thêm thuộc tính `abbreviation`: M, NM, LP, MP, HP, DMG) |
| `Rarity` | `COMMON`, `UNCOMMON`, `RARE`, `HOLO_RARE`, `ULTRA_RARE`, `SECRET_RARE`, `PROMO` |
| `CardLanguage` | `ENGLISH`, `JAPANESE`, `OTHER` |
| `OrderStatus` | `NEW`, `CONFIRMED`, `PICKED_UP`, `SHIPPING`, `DELIVERED`, `DELIVERY_FAILED`, `CANCELLED`, `RETURN_REQUESTED`, `RETURN_APPROVED`, `RETURN_REJECTED`, `RETURNED`, `REFUNDED` |
| `PaymentMethod` | `COD`, `MOMO` |
| `PaymentStatus` | `PENDING`, `PAID`, `FAILED`, `REFUND_PENDING`, `REFUNDED` **[+ REFUND_PENDING]** |
| `TransactionStatus` | `PENDING`, `SUCCESS`, `FAILED` (cho bảng `payments`) |
| `ReturnStatus` | `PENDING`, `APPROVED`, `REJECTED`, `ESCALATED`, `COMPLETED` **[+ ESCALATED: khiếu nại lên Manager]** |
| `DiscountType` | `FIXED_AMOUNT`, `PERCENTAGE` |
| `ActiveStatus` | `ACTIVE`, `INACTIVE` (dùng cho coupons, promotions) |
| `PromotionType` | `PRODUCT_DISCOUNT`, `SHIPPING_DISCOUNT` |
| `MediaType` | `IMAGE`, `VIDEO` |
| `NotificationType` | `ORDER_NEW`, `ORDER_STATUS`, `ORDER_ASSIGNED`, `PRICE_UPDATE`, `PRICE_ALERT`, `SHOP_APPROVAL`, `SYSTEM` |
| `AlertCondition` | `BELOW`, `ABOVE` |

Hạ tầng: `entity/BaseEntity` (`@MappedSuperclass`, `@EntityListeners(AuditingEntityListener.class)`, `equals/hashCode` chỉ theo id và class, an toàn với proxy Hibernate) và `config/JpaConfig` (`@EnableJpaAuditing`).

---

# PHẦN 1 (PU-02a): USER / AUTH

- **roles**: `name` (RoleName, unique), `description`.
- **users**: `email` (unique, not null), `password` (BCrypt hash), `full_name`, `phone_number`, `status` (UserStatus, mặc định `INACTIVE`), `email_verified` (mặc định false), **[+]** `avatar_url`, **[+]** `avatar_public_id`, **[+]** `must_change_password` (mặc định false, dùng cho Manager/Shipper mới tạo), **[+]** `block_reason` (text), **[+]** `failed_login_attempts` (int, mặc định 0), **[+]** `locked_until` (datetime, null). Quan hệ `roles`: `@ManyToMany(LAZY)` qua `user_roles(user_id, role_id)`, kiểu `Set<Role>`.
- **addresses**: `user_id`, `receiver_name`, `phone_number`, `province`, `district`, `ward`, `detail_address`, `is_default`.
- **otp_tokens**: `user_id`, `otp_hash`, `purpose` (OtpPurpose), `expires_at`, `attempt_count` (mặc định 0), `used` (mặc định false). *Cooldown gửi lại 60 giây tính từ `created_at` của OTP mới nhất, mỗi lần gửi lại tạo token mới.*
- **refresh_tokens**: `user_id`, **[sửa]** `token_hash` (varchar(128), unique, lưu SHA-256 hex), `expires_at`, `revoked`.

Repository: `RoleRepository.findByName`; `UserRepository` (`findByEmail` có `@EntityGraph("roles")`, `existsByEmail`, tìm kiếm phân trang theo keyword/status/role); `AddressRepository` (`findByUserId`, `countByUserId`, `findByIdAndUserId`); `OtpTokenRepository.findTopByUserIdAndPurposeOrderByCreatedAtDesc`; `RefreshTokenRepository` (`findByTokenHash`, `deleteByUserId`).

---

# PHẦN 2 (PU-02b): CATALOG

- **games**: `name` (unique), `code` (unique), `description`, **[+]** `logo_url`, **[+]** `logo_public_id`, **[+]** `active` (mặc định true).
- **series**: `game_id`, `name`, `code`, `release_date`, **[+]** `active`; unique (`game_id`, `code`).
- **cards**: `series_id`, `name`, `card_number`, `rarity` (Rarity), `language` (CardLanguage), `image_url`, `image_public_id`, `market_price` (giá thị trường hiện tại, job/Admin cập nhật), **[+]** `active`; unique (`series_id`, `card_number`, `language`); index `name`, `card_number`.
- **shops**: `owner_id` (**[sửa] unique**: mỗi user một shop), `name` (unique), `slug` (unique), `logo_url`, `logo_public_id`, **[+]** `banner_url`, **[+]** `banner_public_id`, **[+]** `phone_number`, **[+]** `pickup_address`, `description` (text), `status` (ShopStatus, mặc định `PENDING`), `reject_reason`, `commission_rate` (null thì dùng mặc định sàn), `avg_rating`, `rating_count`, **[+]** `deleted`.
- **products**: `shop_id`, `type` (ProductType), `card_id` (null, bắt buộc khi SINGLE), `series_id` (null, dùng cho hàng niêm phong), `name`, **[+]** `description` (text), **[sửa]** `card_condition` (CardCondition, null với hàng không phải thẻ đơn; **đổi tên từ `condition` vì là từ khóa MySQL**), `edition`, `language` (CardLanguage), `price`, `stock`, `sold_count`, `avg_rating`, `rating_count`, `favorite_count`, `status` (ProductStatus, mặc định `ACTIVE`), **[+]** `ban_reason`, `deleted`, `version` (`@Version`). Index: `shop_id`, `card_id`, `series_id`, `type`, `sold_count`. Quan hệ `images`: `@OneToMany(mappedBy="product", cascade=ALL, orphanRemoval=true)`.
- **product_images**: `product_id`, `image_url`, `image_public_id`, `is_main`, `display_order`.
- **price_histories**: `card_id`, `recorded_date`, `market_price`, `avg_price`, **[+]** `source` (varchar(10): `ADMIN` hoặc `JOB`, mặc định `ADMIN`); unique (`card_id`, `recorded_date`).

Repository: `GameRepository`, `SeriesRepository.findByGameId`, `CardRepository` (+ `JpaSpecificationExecutor`), `ShopRepository` (`findByOwnerId`, `existsByName`, `findBySlug`), `ProductRepository` (+ `JpaSpecificationExecutor`), `ProductImageRepository`, `PriceHistoryRepository` (`findByCardIdAndRecordedDateBetweenOrderByRecordedDate`, `findByCardIdAndRecordedDate`).

---

# PHẦN 3 (PU-02c): BÁN HÀNG

> **Quyết định thiết kế [sửa]:** ERD gốc dùng `orders.parent_id` (đơn tổng và đơn con trong cùng một bảng). Phú đổi sang **bảng riêng `order_groups`** (một lần thanh toán) và `orders` luôn là **đơn con theo shop** (mọi dòng đều có `shop_id`). Lý do: đơn tổng có `shop_id`, `shipper_id`, trạng thái không có nghĩa nên sinh ra nhiều giá trị null và dễ sai khi truy vấn; thanh toán MoMo thuộc về cả nhóm đơn chứ không thuộc từng shop.

- **shipping_carriers**: `name` (unique), `code` (unique), `base_fee`, `active`, **[+]** `logo_url`, **[+]** `free_ship_threshold` (null).
- **carts**: `user_id` (unique). Quan hệ `items`: `@OneToMany(mappedBy="cart", cascade=ALL, orphanRemoval=true)`.
- **cart_items**: `cart_id`, `product_id`, `quantity` (mặc định 1), **[+]** `selected` (boolean, mặc định true), **[+]** `price_when_added` (DECIMAL(15,2), null, giá hiệu lực tại thời điểm thêm hoặc đổi số lượng); unique (`cart_id`, `product_id`).
- **order_groups [+ bảng mới]**: `group_code` (unique), `user_id`, `total_amount`, `payment_method` (PaymentMethod), `payment_status` (PaymentStatus, mặc định `PENDING`), `paid_at` (null), `idempotency_token` (unique, chống đặt trùng). Quan hệ `orders`: `@OneToMany(mappedBy="orderGroup")`.
- **orders** (đơn con theo shop): `order_code` (unique), **[sửa]** `order_group_id` (thay `parent_id`, not null), `user_id`, **[sửa]** `shop_id` (not null), `shipper_id` (User, null), `shipping_carrier_id` (null), `status` (OrderStatus, mặc định `NEW`), `subtotal`, `shipping_fee`, `discount_amount`, `total_amount`, **[+]** `commission_rate` (DECIMAL(5,2), null), `commission_amount` (**[sửa]** null cho đến khi `DELIVERED`), `shop_revenue` (**[sửa]** null cho đến khi `DELIVERED`), **[+]** `receiver_name`, **[+]** `receiver_phone`, `shipping_address` (text, snapshot địa chỉ), `note`, **[+]** `cancel_reason`, **[+]** `cod_collected` (mặc định false), **[+]** `delivered_at` (null), `version` (`@Version`). **[sửa] Bỏ `payment_method`, `payment_status`** khỏi `orders` (đọc qua `order_group`). Index: (`user_id`, `status`), (`shop_id`, `status`), (`shipper_id`, `status`), `order_code`. Quan hệ: `items` (`@OneToMany`, cascade ALL, orphanRemoval), `statusHistories` (`@OneToMany(mappedBy="order")`).
  - Quy ước tiền: `subtotal` = Σ(giá snapshot đã gồm khuyến mãi × số lượng); `discount_amount` = giảm từ mã giảm giá (mã sàn được chia cho các đơn con theo tỉ lệ `subtotal`); `total_amount` = `subtotal − discount_amount + shipping_fee`.
- **order_items**: `order_id`, `product_id`, `product_name`, `product_image`, **[+]** `card_condition` (snapshot, null), `price`, `quantity`, `total_price`.
- **order_status_histories**: `order_id`, `previous_status` (null), `new_status`, **[sửa]** `changed_by` (**null được**, hệ thống tự đổi như MoMo quá hạn), `note`.
- **payments**: **[sửa]** `order_group_id` (thay `order_id`), `transaction_id`, `amount`, `payment_method`, `status` (TransactionStatus), `pay_url` (text), `response_data` (text).
- **return_requests**: `order_id` (**[sửa] unique**), `user_id`, `reason` (text), `proof_image_url`, **[+]** `proof_image_public_id`, `proof_video_url`, **[+]** `proof_video_public_id`, `status` (ReturnStatus, mặc định `PENDING`), **[+]** `vendor_note`, `admin_note` (ghi chú Manager/Admin), **[+]** `decided_at`.

Repository: `OrderRepository` (tìm theo `userId`/`shopId`/`shipperId` + `status` có `Pageable`, `findByOrderCode`, `findByOrderGroupId`), `OrderGroupRepository` (`findByGroupCode`, `findByIdempotencyToken`), `CartRepository.findByUserId`, `CartItemRepository.findByCartIdAndProductId`, còn lại `JpaRepository` cơ bản.

---

# PHẦN 4 (PU-02d): CÁC BẢNG CÒN LẠI

- **coupons**: `shop_id` (null = mã của sàn, khác null = mã của shop; **không có cột scope**, suy ra từ `shop_id`), `code` (unique, viết hoa), `discount_type` (DiscountType), `discount_value`, `min_order_value` (mặc định 0), `max_discount_amount` (null), `total_quantity`, `used_quantity` (mặc định 0), `limit_per_user` (mặc định 1), `start_date`, `end_date`, `status` (ActiveStatus, mặc định `ACTIVE`).
- **coupon_usages**: `coupon_id`, `user_id`, `order_id`, `discount_amount`, `used_at`; index (`coupon_id`, `user_id`).
- **promotions**: **[sửa]** `shop_id` (**null = khuyến mãi của sàn**), `title`, **[+]** `type` (PromotionType), **[sửa]** `discount_percent` (null được, dùng cho giảm %), **[+]** `discount_amount` (null, dùng cho giảm phí ship cố định), **[+]** `min_order_value` (null), **[+]** `game_id` (null), **[+]** `series_id` (null) (áp dụng theo game/series cho khuyến mãi sàn), `start_date`, `end_date`, `status` (ActiveStatus). Quan hệ `products`: `@ManyToMany(LAZY)` qua `promotion_products(promotion_id, product_id)`, kiểu `Set<Product>`.
- **reviews**: `product_id`, `user_id`, `order_id`, `rating` (1-5), `comment` (text), **[+]** `deleted`; **[+]** unique (`order_id`, `product_id`). Quan hệ `media`: `@OneToMany(mappedBy="review", cascade=ALL, orphanRemoval=true)`.
- **review_media**: `review_id`, `media_url`, `media_public_id`, `media_type` (MediaType).
- **wishlists**: `user_id`, `product_id`; unique (`user_id`, `product_id`).
- **viewed_products**: `user_id`, `product_id`, `viewed_at`; **[+]** unique (`user_id`, `product_id`).
- **notifications**: `user_id`, `title`, `content` (text), `type` (NotificationType), `is_read` (mặc định false; **không dùng tên cột `read`**), `reference_id` (varchar(100)), **[+]** `link` (varchar(255)); index (`user_id`, `is_read`).
- **commission_settings**: `default_rate` (mặc định 5.00), `updated_by` (User). *Mỗi lần đổi tỉ lệ tạo dòng mới, dòng có `created_at` mới nhất là tỉ lệ đang áp dụng, các dòng cũ là lịch sử.*
- **price_alerts**: `user_id`, `card_id`, `target_price`, `alert_condition` (AlertCondition), `is_triggered` (mặc định false).

Repository: `CouponRepository.findByCode`, `CouponUsageRepository.countByCouponIdAndUserId`, `WishlistRepository` (`existsByUserIdAndProductId`, `findByUserId` có `Pageable`), `ViewedProductRepository.findByUserIdOrderByViewedAtDesc`, `NotificationRepository` (`findByUserIdOrderByCreatedAtDesc`, `countByUserIdAndReadFalse` theo tên field), `ReviewRepository` (`existsByOrderIdAndProductId`, `findByProductId` có `Pageable`), `CommissionSettingRepository.findTopByOrderByCreatedAtDesc`, còn lại `JpaRepository` cơ bản.

---

# TÓM TẮT ĐIỀU CHỈNH SO VỚI ERD CỦA PHÁT (để Phát cập nhật ERD)

**Bắt buộc sửa (nếu không sẽ lỗi hoặc mâu thuẫn):**
1. `products.condition` → `card_condition` (`condition` là từ khóa dành riêng của MySQL, bảng sẽ không tạo được).
2. `orders.parent_id` → bảng mới `order_groups` + `orders.order_group_id`; `shop_id` not null; bỏ `payment_method`/`payment_status` khỏi `orders`; `payments.order_id` → `order_group_id`.
3. `order_status_histories.changed_by` cho phép null (thay đổi do hệ thống).
4. `promotions.shop_id` cho phép null và thêm cột để hỗ trợ khuyến mãi sàn (PA-19).
5. Đổi `status` shop `SUSPENDED` giữ nguyên, nhưng thống nhất `ProductStatus` thêm `BANNED` + `products.ban_reason`.

**Bổ sung cột mà task cần:** `users` (avatar, `must_change_password`, `block_reason`, đếm đăng nhập sai), `shops` (banner, SĐT, địa chỉ lấy hàng, `deleted`, `owner_id` unique), `games/series/cards` (`active`, logo game), `products.description`, `cart_items.selected`, `orders` (`commission_rate`, `receiver_name/phone`, `cancel_reason`, `cod_collected`, `delivered_at`), `order_items.card_condition`, `return_requests` (public_id, `vendor_note`, `decided_at`, unique `order_id`), `reviews` (unique + `deleted`), `viewed_products` (unique), `notifications.link`, `shipping_carriers` (logo, ngưỡng miễn phí), `price_histories.source`.
