# CONTRACTS: HỢP ĐỒNG GIỮA CÁC THÀNH VIÊN (đặt tại `docs/CONTRACTS.md`)

> Chữ ký hàm, sự kiện và API dùng chung giữa Phú, Phong, Phát. Mọi người code **theo file này** để làm song song.
> Ai muốn đổi chữ ký phải **sửa file này trong cùng PR và nhắn nhóm**. AI phải đọc file này khi làm việc có gọi code của người khác.

---

## 1. NGUYÊN TẮC CHUNG

1. **Người phụ trách** viết bản thật. Trong giai đoạn đầu Phú tạo sẵn **interface + bản triển khai tạm (stub)** để các nhánh biên dịch song song; người phụ trách **thay nội dung stub, giữ nguyên chữ ký**.
2. Module này không sửa dữ liệu của module khác bằng repository; chỉ **gọi service** hoặc **phát sự kiện**.
3. **Người thao tác (actor) luôn lấy từ `CurrentUser` bên trong service**, không nhận `userId` từ tham số do client gửi. Việc hệ thống tự làm (job, MoMo quá hạn) dùng phương thức riêng `...BySystem`.
4. **Sự kiện** là `record` bất biến trong package `com.tcgshop.event`, chỉ chứa id và dữ liệu nhỏ, **không chứa Entity**.
5. **Listener** dùng `@TransactionalEventListener(phase = AFTER_COMMIT)`. **Nếu listener ghi DB** (ví dụ lưu thông báo) thì phương thức ghi phải là `@Transactional(propagation = REQUIRES_NEW)`, nếu không dữ liệu sẽ không được lưu (transaction gốc đã commit). Lỗi trong listener chỉ **log**, không ném ra ngoài, để không làm hỏng nghiệp vụ chính.
6. Link trong thông báo dùng đúng mẫu ở mục 2.

---

## 2. URL CHI TIẾT ĐƠN (dùng cho link thông báo)

| Người nhận | Link |
|---|---|
| Người mua (User) | `/orders/{orderId}` |
| Chủ shop (Vendor) | `/vendor/orders/{orderId}` |
| Shipper | `/shipper/orders/{orderId}` |
| Manager/Admin | `/manager/orders/{orderId}` |

`orderId` là `id` (số) của đơn con theo shop trong bảng `orders`.

---

## 3. OrderService VÀ BẢNG CHUYỂN TRẠNG THÁI

### 3.1 Chữ ký (Phú viết; stub chạy được ngay, đầy đủ ở PU-16)

```java
public interface OrderService {
    /** Đổi trạng thái đơn con. Người thao tác lấy từ CurrentUser. */
    void changeStatus(Long orderId, OrderStatus newStatus, String note);

    /** Dành cho job/hệ thống (ví dụ MoMo quá hạn tự hủy). Người thao tác = hệ thống. */
    void changeStatusBySystem(Long orderId, OrderStatus newStatus, String note);

    /** Manager/Admin phân công hoặc đổi shipper cho đơn đang CONFIRMED. */
    void assignShipper(Long orderId, Long shipperId);
}
```

- **Không ai được gọi `order.setStatus(...)` ở nơi khác.** Mọi thay đổi trạng thái đi qua `OrderService`.
- Lỗi: đơn không tồn tại → `ResourceNotFoundException`; chuyển trạng thái không hợp lệ → `BusinessException`; sai người thao tác → `ForbiddenException`; thiếu ghi chú bắt buộc → `BadRequestException`; đổi đồng thời → `ObjectOptimisticLockingFailureException` (đã được `GlobalExceptionHandler` xử lý thành 409).
- Mỗi lần đổi: kiểm tra chuyển hợp lệ → kiểm tra người thao tác → cập nhật → ghi `order_status_histories` → phát `OrderStatusChangedEvent`.
- Truy vấn danh sách/chi tiết đơn **do chủ từng màn hình tự viết** (Phong: `VendorOrderService`, `ShipperOrderService`; Phú: đơn của User; Phát: `ManagerOrderService`), không dồn vào `OrderService`.

### 3.2 Loại người thao tác (`ActorType`)

| ActorType | Điều kiện |
|---|---|
| `BUYER` | `order.user.id` = id người đang đăng nhập |
| `SHOP_OWNER` | `order.shop.owner.id` = id người đang đăng nhập |
| `ASSIGNED_SHIPPER` | `order.shipper` khác null và id = id người đang đăng nhập |
| `MANAGER` | có `ROLE_MANAGER` |
| `ADMIN` | có `ROLE_ADMIN` |
| `SYSTEM` | gọi qua `changeStatusBySystem` |

### 3.3 Bảng chuyển trạng thái (đề xuất, cả nhóm chốt)

| Từ | Sang | Ai được làm | Ghi chú (`note`) bắt buộc |
|---|---|---|---|
| `NEW` | `CONFIRMED` | `SHOP_OWNER` | không |
| `NEW` | `CANCELLED` | `BUYER`, `SHOP_OWNER`, `MANAGER`, `ADMIN`, `SYSTEM` | có (lý do) |
| `CONFIRMED` | `CANCELLED` | `BUYER`, `SHOP_OWNER`, `MANAGER`, `ADMIN` | có (lý do) |
| `CONFIRMED` | `PICKED_UP` | `ASSIGNED_SHIPPER` | không |
| `PICKED_UP` | `SHIPPING` | `ASSIGNED_SHIPPER` | không |
| `SHIPPING` | `DELIVERED` | `ASSIGNED_SHIPPER` | không |
| `SHIPPING` | `DELIVERY_FAILED` | `ASSIGNED_SHIPPER` | có (lý do) |
| `DELIVERY_FAILED` | `SHIPPING` | `ASSIGNED_SHIPPER` | không |
| `DELIVERY_FAILED` | `CANCELLED` | `MANAGER`, `ADMIN` | có |
| `DELIVERED` | `RETURN_REQUESTED` | `BUYER` (trong thời hạn đổi trả, kiểm tra ở PU-16/20) | có (lý do) |
| `RETURN_REQUESTED` | `RETURN_APPROVED` | `SHOP_OWNER` | không |
| `RETURN_REQUESTED` | `RETURN_REJECTED` | `SHOP_OWNER` | có |
| `RETURN_REJECTED` | `RETURN_APPROVED` | `MANAGER`, `ADMIN` (khiếu nại) | có |
| `RETURN_APPROVED` | `RETURNED` | `ASSIGNED_SHIPPER` | không |
| `RETURNED` | `REFUNDED` | `MANAGER`, `ADMIN` | có (ghi chú hoàn tiền) |

Mọi cặp không có trong bảng đều **không hợp lệ**. `CONFIRMED → PICKED_UP` còn yêu cầu đơn **đã có shipper**.

### 3.4 `assignShipper`
- Chỉ `MANAGER`/`ADMIN`; chỉ đơn đang `CONFIRMED` (được đổi shipper khi chưa `PICKED_UP`).
- Shipper phải có `ROLE_SHIPPER` và `status = ACTIVE`, nếu không thì `BadRequestException`.
- Ghi một dòng `order_status_histories` với `previous_status = new_status = CONFIRMED` và ghi chú "Phân công shipper: {họ tên}"; phát `OrderAssignedEvent`.

### 3.5 Việc còn để PU-16 (stub chưa làm)
Hoàn tồn kho khi hủy, tăng `sold_count` khi `DELIVERED`, snapshot hoa hồng (`CommissionService.getRate`) và `shop_revenue`, đánh dấu COD đã thu, `REFUND_PENDING` cho đơn MoMo bị hủy, kiểm tra thời hạn trả hàng, `delivered_at`.

---

## 4. SỰ KIỆN (package `com.tcgshop.event`)

```java
// Phát một lần cho MỖI đơn con, sau khi đặt hàng thành công (Phú, PU-15).
public record OrderCreatedEvent(Long orderId, String orderCode, Long orderGroupId,
                                Long buyerId, Long shopId, Long shopOwnerId) {}

// Phát sau mỗi lần đổi trạng thái (Phú, OrderService).
public record OrderStatusChangedEvent(Long orderId, String orderCode, Long buyerId,
                                      Long shopId, Long shopOwnerId, Long shipperId,
                                      OrderStatus oldStatus, OrderStatus newStatus,
                                      Long actorId, // null nếu do hệ thống
                                      String note, LocalDateTime occurredAt) {}

// Phát sau khi phân công/đổi shipper (Phú, OrderService.assignShipper).
public record OrderAssignedEvent(Long orderId, String orderCode, Long shipperId,
                                 Long buyerId, Long assignedById) {}

// Phát sau khi lưu giá thị trường mới (Phát: nhập tay PA-16 hoặc job PA-39).
public record PriceUpdatedEvent(Long cardId, String cardName, BigDecimal oldPrice,
                                BigDecimal newPrice, LocalDate recordDate, String source) {}
```

### Ai phát, ai nghe

| Sự kiện | Người phát | Người nghe | Việc của người nghe |
|---|---|---|---|
| `OrderCreatedEvent` | Phú | **Phong** (PG-19) | Gửi `ORDER_NEW` cho chủ shop |
| `OrderStatusChangedEvent` | Phú | **Phát** (PA-40) | Gửi thông báo theo mục 4.1 |
| `OrderAssignedEvent` | Phú | **Phát** (PA-40) | Gửi `ORDER_ASSIGNED` cho shipper và thông báo cho người mua |
| `PriceUpdatedEvent` | Phát | **Phát** (PA-37, PA-53) | Đẩy WebSocket `/topic/price-update/{cardId}`; kiểm tra cảnh báo giá |

### 4.1 Ai nhận thông báo khi đổi trạng thái (không báo cho chính người vừa thao tác)

| Trạng thái mới | Người mua | Chủ shop | Shipper |
|---|---|---|---|
| `CONFIRMED` | Có | | |
| `PICKED_UP`, `SHIPPING` | Có | | |
| `DELIVERED` | Có | Có | |
| `DELIVERY_FAILED` | Có | Có | |
| `CANCELLED` | Có | Có | Có (nếu đã có shipper) |
| `RETURN_REQUESTED` | | Có | |
| `RETURN_APPROVED`, `RETURN_REJECTED` | Có | | `RETURN_APPROVED`: Có (cần lấy hàng trả về) |
| `RETURNED` | Có | Có | |
| `REFUNDED` | Có | | |

Mẫu nội dung: tiêu đề `Đơn {orderCode}`, nội dung dùng `OrderStatus.displayName`, có kèm `note` khi là `DELIVERY_FAILED`, `CANCELLED`, `RETURN_REJECTED`.

---

## 5. SERVICE DÙNG CHUNG

| Service | Người viết bản thật | Người gọi | Chữ ký |
|---|---|---|---|
| `NotificationService` | Phát (PA-04, **đã có**) | Phong, Phát, Phú | `NotificationResponse send(Long userId, NotificationType type, String title, String content, String link)` |
| `PromotionService` | Phong (PG-16) | Phú (giỏ hàng, checkout), Phát (trang sản phẩm) | `BigDecimal getEffectivePrice(Long productId)`; `Map<Long, BigDecimal> getEffectivePrices(Collection<Long> productIds)` (dùng cho danh sách để tránh N+1). Trả giá **sau khuyến mãi** (shop và sàn), nếu không có khuyến mãi trả `price` gốc |
| `CouponService` | Phong (PG-17) | Phú (checkout), Phát (giao diện quản trị mã sàn) | `CouponApplyResponse validateAndCalculate(String code, Long userId, Long shopId, BigDecimal subtotal)`; `void recordUsage(Long couponId, Long userId, Long orderId, BigDecimal discountAmount)` |
| `ShippingService` | Phát (PA-18) | Phú (checkout) | `List<ShippingCarrierResponse> getActiveCarriers()`; `BigDecimal calculateFee(Long carrierId, BigDecimal orderSubtotal)` |
| `CommissionService` | Phát (PA-17) | Phú (`OrderService` khi `DELIVERED`) | `BigDecimal getRate(Long shopId)`: ưu tiên `shops.commission_rate`, nếu null thì dòng mới nhất của `commission_settings`, nếu chưa có thì 5.00 |
| `ViewedProductService` | Phong (PG-27) | Phát (trang chi tiết) | `void record(Long userId, Long productId)` |
| `CardService` | Phát (PA-11) | Phong (PG-13) | `Page<CardListingResponse> searchForListing(String keyword, Long seriesId, Pageable pageable)` |

**Quy ước `CouponService.validateAndCalculate`:**
- Mã của **shop** (`coupons.shop_id` khác null): truyền `shopId` của shop đó và `subtotal` của đơn shop đó.
- Mã của **sàn** (`shop_id` null): truyền `shopId = null` và `subtotal` là tổng toàn giỏ. Phú chia mức giảm cho các đơn con theo tỉ lệ `subtotal`.
- Kết quả `CouponApplyResponse(boolean valid, Long couponId, BigDecimal discountAmount, String message)`: không hợp lệ thì `valid = false` kèm lý do tiếng Việt (hết hạn, hết lượt, chưa đủ đơn tối thiểu, sai phạm vi, đã dùng quá số lần).
- `recordUsage` tăng `used_count` và ghi `coupon_usages`, phải an toàn khi nhiều người cùng dùng (kiểm tra lại giới hạn lượt trong lúc ghi).

**`ShippingCarrierResponse`** = `(Long id, String name, String code, String logoUrl, BigDecimal baseFee, BigDecimal freeShipThreshold)`.
**`CardListingResponse`** = `(Long id, String name, String cardNumber, String seriesName, String gameName, Rarity rarity, CardLanguage language, String imageUrl)`.

---

## 6. API VÀ FRAGMENT GIỮA CÁC MODULE

| Hợp đồng | Người cung cấp | Người dùng | Nội dung |
|---|---|---|---|
| **Thêm vào giỏ** | Phú (PU-12) | Phát (nút Add to Cart) | `POST /api/cart/items` body `{ "productId": 1, "quantity": 2 }` → `200 { "success": true, "message": "...", "cartCount": 3 }`; lỗi theo `ApiErrorResponse` (400 vượt tồn kho/số lượng sai, 404 không có sản phẩm, 401 chưa đăng nhập, JS chuyển tới đăng nhập). `GET /api/cart/count` → `{ "cartCount": 3 }` |
| **Yêu thích** | Phong (PG-26) | Phát (thẻ sản phẩm, trang chi tiết) | `POST /api/wishlist/toggle/{productId}` → `200 { "wishlisted": true, "favoriteCount": 12 }` |
| **Biểu đồ giá** | Phát (PA-36) | Phát | `GET /api/cards/{id}/price-history?range=7|30|90` (công khai) |
| **Bình luận** | Phong (PG-29) | Phát (trang chi tiết) | Phong cung cấp `ReviewService.getReviewsByProduct(Long productId, Pageable)` trả `PageResponse<ReviewResponse>` và `ReviewService.canReview(Long userId, Long productId)`; fragment `fragments/reviews :: reviewSection(product, reviews, canReview)`. Phong và Phát chốt chi tiết khi tới PG-29/PA-34 rồi cập nhật file này |
| **Duyệt shop** | Phát (PA-14) | Phong (PG-10 chỉ đọc trạng thái) | Khi duyệt: **thêm** `ROLE_VENDOR` cho chủ shop, **giữ** `ROLE_USER`; từ chối ghi `reject_reason` |
| **Meta CSRF + toast flash** | Phong (PG-02, PG-07) | Cả nhóm | Layout có hai thẻ meta CSRF; toast đọc `successMessage` / `errorMessage` |

---

## 7. QUY TẮC ĐỂ KHÔNG CHẶN NHAU

- Cần thêm cột/bảng/enum: nhắn Phú (Phú là người duy nhất sửa Entity dùng chung), PR riêng và merge nhanh.
- Cần đổi chữ ký trong file này: sửa file, nêu trong PR, nhắn nhóm.
- Stub còn tồn tại nghĩa là bản thật **chưa xong**: người phụ trách phải xóa dòng chú thích "STUB" khi thay bản thật.
