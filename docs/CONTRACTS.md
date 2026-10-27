# CONTRACTS: HỢP ĐỒNG GIỮA CÁC THÀNH VIÊN (đặt tại `docs/CONTRACTS.md`)

## Cấu hình thương hiệu dùng chung

`SystemSettingService.getSettings()` trả `SystemSettingResponse(siteName, logoUrl, contactPhone, contactEmail)` cho mọi role/Guest, không lộ Entity/publicId. `save(SystemSettingRequest)` chỉ Admin, lấy actor từ CurrentUser; POST multipart `/admin/settings` có CSRF, validation, PRG và flash thành công. Không nhận id/URL/publicId ảnh từ client. CloudinaryService kiểm tra nội dung ảnh và upload; lưu url/publicId vào DB; xóa ảnh mới nếu rollback, ảnh cũ chỉ sau commit. `GlobalSettingsAdvice` đưa DTO `globalSettings` vào model các web controller; Header, Footer và sidebar dashboard dùng fragment `site-brand`. Không cache: request trang tiếp theo đọc cấu hình DB mới nhất, trang đã mở cần tải lại. Hoa hồng vẫn quản lý qua CommissionService/trang Hoa hồng sàn, không ghép vào SystemSetting.

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

Đường dẫn người mua `/orders/{orderId}` đã hoạt động từ PU-18b; service chỉ trả chi tiết khi đơn thuộc người mua đang đăng nhập.

Luồng đổi trả của người mua nằm dưới cùng đường dẫn: `GET /orders/{orderId}/return` mở form, `POST /orders/{orderId}/return` tạo yêu cầu với ảnh bằng chứng bắt buộc và video tùy chọn, `POST /orders/{orderId}/return/escalate` gửi khiếu nại. Hai thao tác ghi chỉ nhận POST; controller lấy đơn qua `UserOrderService` và gọi `ReturnRequestService`, không nhận `userId` từ client.

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

### 3.5 Đã làm ở PU-16
Tác dụng phụ khi đổi trạng thái và phép tính doanh thu được thực hiện trong cùng transaction bởi `OrderStatusSideEffectHandler`; chi tiết tại mục 9.

---

## 4. SỰ KIỆN (package `com.tcgshop.event`)

```java
// Phát một lần cho MỖI đơn con, sau khi đặt hàng thành công (Phú, PU-15).
public record OrderCreatedEvent(Long orderId, String orderCode, Long orderGroupId,
                                Long buyerId, Long shopId, Long shopOwnerId) {}

// Phát một lần cho mỗi lần thanh toán để gửi email xác nhận đơn hàng (Phú, PU-15a).
public record OrderGroupPlacedEvent(Long orderGroupId, String groupCode, Long buyerId,
                                    String buyerEmail, String buyerName, PaymentMethod paymentMethod,
                                    BigDecimal grandTotal, List<String> orderCodes) {}

// Phát sau mỗi lần đổi trạng thái (Phú, OrderService).
public record OrderStatusChangedEvent(Long orderId, String orderCode, Long buyerId,
                                      Long shopId, Long shopOwnerId, Long shipperId,
                                      OrderStatus oldStatus, OrderStatus newStatus,
                                      Long actorId, // null nếu do hệ thống
                                      String note, LocalDateTime occurredAt) {}

// Phát sau khi phân công/đổi shipper (Phú, OrderService.assignShipper).
public record OrderAssignedEvent(Long orderId, String orderCode, Long shipperId,
                                 Long buyerId, Long assignedById) {}

// Phát bởi Phú (ReturnRequestService) khi buyer khiếu nại sau khi Vendor từ chối.
public record ReturnRequestEscalatedEvent(Long orderId, String orderCode, Long buyerId,
                                          Long shopId, Long shopOwnerId) {}

// Phát bởi Phú (ReturnRequestService) khi Manager từ chối khiếu nại; note là lý do từ chối.
public record ReturnEscalationRejectedEvent(Long orderId, String orderCode, Long buyerId,
                                            String note) {}

// Phát sau khi lưu giá thị trường mới (Phát: nhập tay PA-16 hoặc job PA-39).
public record PriceUpdatedEvent(Long cardId, String cardName, BigDecimal oldPrice,
                                BigDecimal newPrice, LocalDate recordDate, String source) {}
```

### Ai phát, ai nghe

| Sự kiện | Người phát | Người nghe | Việc của người nghe |
|---|---|---|---|
| `OrderCreatedEvent` | Phú | **Phong** (PG-19) | Gửi `ORDER_NEW` cho chủ shop |
| `OrderGroupPlacedEvent` | Phú | **Phú** | Gửi email xác nhận cho người mua; phát một lần cho mỗi lần thanh toán |
| `OrderStatusChangedEvent` | Phú | **Phát** (PA-40) | Gửi thông báo theo mục 4.1 |
| `OrderAssignedEvent` | Phú | **Phát** (PA-40) | Gửi `ORDER_ASSIGNED` cho shipper và thông báo cho người mua |
| `ReturnRequestEscalatedEvent` | Phú (`ReturnRequestService`) | **Phát** | Báo Manager có khiếu nại mới, link `/manager/orders/{orderId}` |
| `ReturnEscalationRejectedEvent` | Phú (`ReturnRequestService`) | **Phát** | Báo người mua khi khiếu nại bị từ chối, link `/orders/{orderId}` |
| `PriceUpdatedEvent` | Phát | **Phát** (PA-37, PA-53) | PA-16 phát khi Admin cập nhật bằng tay; PA-39 chỉ phát khi giá mới thay đổi tuyệt đối từ 5% so với giá hiện tại. Sau commit, PA-37 phát WebSocket `/topic/price-update/{cardId}` với `cardId`, `cardName`, `oldPrice`, `newPrice`, `changePercent`, `recordDate`, `source`, `updatedAt`; trang chi tiết cập nhật giá/biểu đồ trực tiếp. PA-53 kiểm tra cảnh báo giá |

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

### Chuông thông báo (PA-41)

- Chuông dùng chung trong header cho mọi tài khoản đã đăng nhập; tải 5 thông báo gần nhất từ `GET /api/notifications?page=0&size=5` và số chưa đọc từ `GET /api/notifications/unread-count`.
- Nhận thông báo mới qua `/user/queue/notifications`; số chưa đọc tăng ngay và danh sách dropdown được cập nhật không cần tải lại trang.
- Chọn thông báo chưa đọc gọi `PATCH /api/notifications/{id}/read` trước khi điều hướng tới `link`; các URL điều hướng phải cùng origin.
- `/notifications` hiển thị danh sách phân trang, tái sử dụng API notification và giữ nguyên kiểm tra ownership theo người đăng nhập.

**Quy ước `CouponService.validateAndCalculate`:**

- Gợi ý mã shop khi checkout: `GET /api/checkout/coupons` (ROLE_USER) trả mảng `CouponSuggestionResponse` gồm `id`, `shopId`, `code`, `discountType`, `discountValue`, `minOrderValue`, `maxDiscountAmount`, `endDate`, `discountAmount`; không có mã trả `[]`. Không nhận userId/shopId/subtotal từ client. `CouponSuggestionService.getSuggestions()` lấy giỏ của CurrentUser qua CartService, chỉ xét dòng được chọn, khả dụng, đủ tồn kho và tính subtotal bằng giá làm tròn VND trước khi nhân số lượng. Chỉ lấy mã shop ACTIVE, trong hạn, còn tổng lượt; dùng lại `validateAndCalculate` để kiểm tra đơn tối thiểu, phạm vi và lượt dùng của user. Không ghi lượt sử dụng khi gợi ý. Thẻ mã được ẩn khi danh sách rỗng; nút Áp dụng gọi lại preview hiện có và đặt hàng vẫn kiểm tra lại toàn bộ điều kiện. API không gợi ý mã sàn, ô nhập mã sàn/thủ công được giữ nguyên.
- Mã của **shop** (`coupons.shop_id` khác null): truyền `shopId` của shop đó và `subtotal` của đơn shop đó.
- Mã của **sàn** (`shop_id` null): truyền `shopId = null` và `subtotal` là tổng toàn giỏ. Phú chia mức giảm cho các đơn con theo tỉ lệ `subtotal`.
- Kết quả `CouponApplyResponse(boolean valid, Long couponId, BigDecimal discountAmount, String message)`: không hợp lệ thì `valid = false` kèm lý do tiếng Việt (hết hạn, hết lượt, chưa đủ đơn tối thiểu, sai phạm vi, đã dùng quá số lần).
- `recordUsage` tăng `used_count` và ghi `coupon_usages`, phải an toàn khi nhiều người cùng dùng (kiểm tra lại giới hạn lượt trong lúc ghi).
- Khi đặt một nhóm đơn có mã sàn, gọi `recordUsage` đúng **một lần cho mỗi lần thanh toán**; truyền `orderId` của đơn con đầu tiên và `discountAmount` bằng tổng `platformCouponShare` đã phân bổ cho các shop.

**`ShippingCarrierResponse`** = `(Long id, String name, String code, String logoUrl, BigDecimal baseFee, BigDecimal freeShipThreshold)`.
**`CardListingResponse`** = `(Long id, String name, String cardNumber, String seriesName, String gameName, Rarity rarity, CardLanguage language, String imageUrl)`.

---

## 6. API VÀ FRAGMENT GIỮA CÁC MODULE

| Hợp đồng | Người cung cấp | Người dùng | Nội dung |
|---|---|---|---|
| **Thêm vào giỏ** | Phú (PU-12) | Phát (nút Add to Cart) | `POST /api/cart/items` body `{ "productId": 1, "quantity": 2 }` → `200 { "success": true, "message": "Đã thêm vào giỏ hàng", "cartCount": 3 }`. `GET /api/cart/count` → `{ "cartCount": 3 }`. `cartCount` = số dòng sản phẩm khác nhau trong giỏ. Lỗi theo `ApiErrorResponse`: 400 (vượt tồn kho, số lượng sai, sản phẩm của chính shop mình, sản phẩm không còn bán), 404 (không có sản phẩm), 401 (chưa đăng nhập: JS chuyển tới `/auth/login?returnUrl=<trang hiện tại>`), 403 (tài khoản không có `ROLE_USER`, ví dụ Manager). Sau mỗi thao tác giỏ, JavaScript phát sự kiện `window.dispatchEvent(new CustomEvent('cart:updated', { detail: { cartCount } }))`; header của Phong lắng nghe sự kiện này để cập nhật huy hiệu, **chỉ gọi `GET /api/cart/count` khi `sec:authorize="hasRole('USER')"`**. |
| **Yêu thích** | Phong (PG-26) | Phát (thẻ sản phẩm, trang chi tiết) | `POST /api/wishlist/toggle/{productId}` → `200 { "wishlisted": true, "favoriteCount": 12 }` |
| **Biểu đồ giá** | Phát (PA-36) | Phát | `GET /api/cards/{id}/price-history?range=7|30|90` (công khai) |
| **Market Price job** | Phát (PA-39) | Admin, PA-37 | `MarketPriceJobService.runNow()` tổng hợp giá snapshot có trọng số số lượng từ `OrderItem.price` của đơn `DELIVERED` trong 30 ngày gần nhất; khi không có giao dịch dùng giá Admin cập nhật gần nhất. Job ghi tối đa một `price_histories` mỗi thẻ/ngày với `source=JOB`; chạy tay qua `POST /admin/market-price/run` (ROLE_ADMIN), cron 02:00 giờ Việt Nam |
| **Bình luận** | Phong (PG-29) | Phát (trang chi tiết) | `ReviewService.getReviewsByProduct(Long productId, Pageable)` trả `PageResponse<ReviewResponse>`; overload nhận `Integer rating` lọc 1–5 sao. `POST /api/reviews/{reviewId}/reply` nhận `{reply}`; chỉ chủ shop của sản phẩm mới được phản hồi một lần. `ReviewResponse` trả `vendorReply`, `repliedAt`. `ReviewCreatedEvent` gửi `REVIEW_NEW` cho chủ shop sau commit, link `/products/{productId}#reviews`; fragment `fragments/reviews :: reviewSection(product, reviews, canReview)` |
| **Duyệt shop** | Phát (PA-14) | Phong (PG-10 chỉ đọc trạng thái) | Khi duyệt: **thêm** `ROLE_VENDOR` cho chủ shop, **giữ** `ROLE_USER`; từ chối ghi `reject_reason` |
| **Meta CSRF + toast flash** | Phong (PG-02, PG-07) | Cả nhóm | Layout có hai thẻ meta CSRF; toast đọc `successMessage` / `errorMessage` |
| **Tên và avatar ở header** | Phú (PU-10) | Phong (PG-02) | `sec:authentication="principal.fullName"` và `sec:authentication="principal.avatarUrl"` (null thì hiện chữ cái đầu của tên) |

### JavaScript tích hợp nút “Thêm vào giỏ” (Phát)

Dùng `App.fetchWithCsrf` trong `static/js/common.js` để gửi JSON và tự gắn CSRF; nhận message để báo thành công, phát `cart:updated` cho header. Khi API trả 401, chuyển về trang đăng nhập với URL hiện tại làm `returnUrl`:

```javascript
window.App.fetchWithCsrf("/api/cart/items", {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ productId, quantity: 1 })
}).then((data) => {
  window.App.showToast(data.message, "success");
  window.dispatchEvent(new CustomEvent("cart:updated", {
    detail: { cartCount: data.cartCount }
  }));
}).catch((error) => {
  if (/Authentication is required|HTTP 401/.test(error.message)) {
    const returnUrl = `${location.pathname}${location.search}`;
    location.assign(`/auth/login?returnUrl=${encodeURIComponent(returnUrl)}`);
  }
});
```

---

## 7. QUY TẮC ĐỂ KHÔNG CHẶN NHAU

- Cần thêm cột/bảng/enum: nhắn Phú (Phú là người duy nhất sửa Entity dùng chung), PR riêng và merge nhanh.
- Cần đổi chữ ký trong file này: sửa file, nêu trong PR, nhắn nhóm.
- Stub còn tồn tại nghĩa là bản thật **chưa xong**: người phụ trách phải xóa dòng chú thích "STUB" khi thay bản thật.

## 8. QUY TẮC TÍNH TIỀN CHECKOUT

- Đơn giá hiệu lực được làm tròn về đồng gần nhất bằng `MoneyUtils.round` (`HALF_UP`) **trước khi** nhân với số lượng; `subtotal` shop là tổng `unitPrice × quantity` sau bước này.
- Mã giảm giá theo phần trăm làm tròn xuống ở bước tính coupon. Thứ tự áp dụng: giảm giá shop → giảm giá sàn chia theo tỉ lệ phần còn phải trả của từng shop → phí vận chuyển.
- Giảm giá sàn được phân bổ bằng `MoneyAllocator.allocateProportionally` theo phương pháp phần dư lớn nhất; tổng phần chia phải đúng bằng mức giảm sàn đã áp dụng. Không shop nào có tổng đơn âm.
- `orders.discount_amount` bằng giảm giá shop cộng phần giảm sàn được chia cho shop; `orders.total_amount = subtotal - discount_amount + shipping_fee`.
- `CheckoutService.preview` là điểm tính tiền dùng chung cho xem trước và đặt hàng. PU-15 phải gọi lại preview ở server và từ chối đặt hàng nếu `canPlaceOrder = false`, khách gửi mã giảm giá không hợp lệ, hoặc `expectedGrandTotal` khác `preview.grandTotal()`; `expectedGrandTotal` chỉ dùng để so sánh, không dùng làm số tiền lưu.

## 9. TÁC DỤNG PHỤ THEO TRẠNG THÁI VÀ QUY TẮC DOANH THU

| Trạng thái mới | Tác dụng phụ |
|---|---|
| `CANCELLED` | Ghi `cancel_reason`; hoàn tồn kho cho từng sản phẩm theo thứ tự `productId` tăng dần. Nếu đơn MoMo đã `PAID`, ghi `refund_amount = total_amount`, để `refunded_at = null` chờ quy trình hoàn tiền. Không hoàn lại lượt dùng coupon và không tự đổi `order_groups.payment_status` (xử lý ở PU-17). |
| `DELIVERED` | Ghi `delivered_at`; tăng `sold_count`; chốt commission rate, commission amount và shop revenue. Với COD, đặt `cod_collected = true`; phương thức khác không đổi trường này. |
| `REFUNDED` | Giảm `sold_count` theo số lượng đơn; ghi `refund_amount = total_amount` và `refunded_at`. Giữ nguyên snapshot hoa hồng/doanh thu đã chốt. |
| Trạng thái khác | Không phát sinh tác dụng phụ tồn kho, sold count hay hoa hồng. Riêng `RETURN_REQUESTED` phải còn trong thời hạn đổi trả tính từ `delivered_at` (nếu null thì dùng `updated_at`); đúng thời điểm hết hạn vẫn được yêu cầu trả. |

Quy tắc doanh thu:

- `OrderStatus.countsAsRevenue()` trả `true` cho `DELIVERED`, `RETURN_REQUESTED`, `RETURN_REJECTED`; các trạng thái còn lại trả `false`.
- Cơ sở hoa hồng = `max(0, subtotal - phần giảm giá do shop chịu)`, trong đó phần shop chịu = `max(0, discount_amount - platform_discount_amount)`. Mã giảm giá sàn do sàn chịu, không giảm cơ sở của shop; phí vận chuyển không tính hoa hồng.
- `commission_amount = cơ sở × commission_rate / 100`, làm tròn đến đồng gần nhất bằng `HALF_UP`; `shop_revenue = cơ sở - commission_amount`.
- `sold_count` và tồn kho chỉ được cập nhật qua `OrderService` cùng các repository counter của luồng này; module khác không tự cập nhật hai giá trị đó.
- Hủy đơn không hoàn lại lượt sử dụng mã giảm giá. Tác dụng phụ không tự cập nhật `order_groups.payment_status`; luồng trạng thái thanh toán/hoàn tiền cấp nhóm thuộc PU-17.
- Ví dụ: `subtotal = 1.000.000`, tổng giảm `100.000` gồm `40.000` do sàn chịu, commission rate `5%`. Cơ sở = `1.000.000 - (100.000 - 40.000) = 940.000`; commission = `47.000`; shop nhận `893.000` (phí ship không tính vào các phép tính này).

### Tập trạng thái phục vụ doanh thu và đối soát (PU-21a)

| Tập `OrderStatus` | Trạng thái | Ý nghĩa |
|---|---|---|
| `REVENUE_STATUSES` | `DELIVERED`, `RETURN_REQUESTED`, `RETURN_REJECTED` | Đơn được tính vào doanh thu theo `countsAsRevenue()`. |
| `SOLD_COUNT_STATUSES` | `DELIVERED`, `RETURN_REQUESTED`, `RETURN_REJECTED`, `RETURN_APPROVED`, `RETURNED` | Sản phẩm vẫn tính là đã bán trong quá trình trả hàng; chỉ trừ `sold_count` khi đơn sang `REFUNDED`. |
| `COMMISSION_FINALIZED_STATUSES` | `DELIVERED`, `RETURN_REQUESTED`, `RETURN_REJECTED`, `RETURN_APPROVED`, `RETURNED`, `REFUNDED` | Hoa hồng được chốt khi `DELIVERED` và giữ nguyên snapshot qua mọi trạng thái sau đó, kể cả `REFUNDED`. |

Các tập lồng nhau theo thứ tự trên: `REVENUE_STATUSES ⊆ SOLD_COUNT_STATUSES ⊆ COMMISSION_FINALIZED_STATUSES`.

### Đối soát đơn hàng (PU-21a)

`OrderReconciliationService` chạy các câu SQL chỉ đọc, độc lập với service ghi dữ liệu để phép kiểm tra không lặp lại cùng logic/cùng lỗi tính tiền. Tiền dùng `DECIMAL`; công thức hoa hồng dùng `ROUND(..., 0)` của MySQL để làm tròn nửa lên như `HALF_UP`, không chuyển qua `DOUBLE`/`FLOAT`.

| Mã | Mức | Nội dung |
|---|---|---|
| `ORD_TOTAL_FORMULA` | ERROR | `total_amount` khác subtotal − discount + shipping. |
| `ORD_NEGATIVE` | ERROR | Số tiền âm, discount vượt subtotal hoặc platform discount vượt discount. |
| `ORD_NO_ITEMS` | ERROR | Đơn không có dòng hàng. |
| `ORD_ITEMS_SUM` | ERROR | Subtotal khác tổng `order_items.total_price`. |
| `ITEM_LINE` | ERROR | Quantity không dương hoặc `total_price` khác price × quantity. |
| `GROUP_TOTAL` | ERROR | Tổng nhóm khác tổng đơn con, bao gồm nhóm không có đơn. |
| `COMM_FORMULA` | ERROR | Snapshot hoa hồng thiếu hoặc sai công thức/cơ sở/doanh thu shop. |
| `COMM_PREMATURE` | ERROR | Snapshot hoa hồng tồn tại trước trạng thái chốt. |
| `REFUND_FORMULA` | ERROR | Số tiền/thời điểm hoàn không khớp trạng thái. |
| `MONEY_SCALE` | WARNING | Giá trị VND có phần thập phân. |
| `HIST_MISSING` | WARNING | Đơn không có lịch sử trạng thái. |
| `HIST_MISMATCH` | WARNING | `new_status` của history có id lớn nhất khác trạng thái đơn. |
| `SOLD_COUNT_LOW` | ERROR | `sold_count` thấp hơn số lượng từ đơn thuộc các trạng thái đang tính bán. |
| `SOLD_COUNT_HIGH` | WARNING | `sold_count` cao hơn số lượng đơn đang biết; có thể gồm seed hoặc lịch sử trước khi theo dõi đơn hàng chạy. |
| `STOCK_NEGATIVE` | ERROR | Tồn kho sản phẩm âm. |
| `COUPON_USED_LOW` | ERROR | `used_quantity` thấp hơn số dòng `coupon_usages`. |
| `COUPON_USED_HIGH` | WARNING | `used_quantity` cao hơn số dòng `coupon_usages`; có thể gồm seed hoặc lịch sử cũ. |
| `COUPON_USAGE_AMOUNT` | ERROR | Mức giảm trong usage khác phần giảm được phân bổ cho đơn/nhóm. |

`ERROR` là sai lệch cần xử lý; `WARNING` là bất thường cần xem xét nhưng có thể hợp lệ do dữ liệu seed/lịch sử. Báo cáo chỉ trả mã đơn (nếu có), ID, số liệu và chi tiết kiểm tra; không trả thông tin cá nhân. Mỗi check đếm toàn bộ vi phạm nhưng chỉ trả tối đa `app.reconcile.max-findings-per-check` finding (mặc định 50). `GET /api/admin/reconciliation` chỉ dành cho `ROLE_ADMIN`. Có thể bật đối soát khi khởi động bằng `app.reconcile.run-on-startup=true` (mặc định false); runner chỉ ghi tổng kết, số lượng mỗi check và tối đa 10 finding mẫu mỗi check, lỗi đối soát không dừng ứng dụng. Chạy IT bằng `mvnw test -DrunIT=true`; `ReconciliationIT` chỉ xác nhận finding thuộc fixture do test tạo, không yêu cầu DB dev không có finding khác.

## 10. LUỒNG TRẢ HÀNG VÀ KHIẾU NẠI (PU-20)

| Trạng thái đơn mới | Đồng bộ `return_requests` | Ghi chú |
|---|---|---|
| `RETURN_REQUESTED` | Tạo yêu cầu `PENDING` | Chỉ người mua sở hữu đơn đã giao và còn hạn; khóa đơn trước khi kiểm tra trùng và tải bằng chứng. |
| `RETURN_APPROVED` | `APPROVED`, ghi `decided_at`; nếu đơn đang khiếu nại thì lưu ghi chú Manager vào `admin_note` | Manager duyệt khiếu nại thông qua `OrderService.changeStatus`. |
| `RETURN_REJECTED` | `REJECTED`, ghi ghi chú Vendor vào `vendor_note` và `decided_at` | Đây là điều kiện để người mua gửi khiếu nại một lần. |
| `RETURNED` | Giữ `APPROVED` | Shipper xác nhận hàng đã hoàn về. |
| `REFUNDED` | `COMPLETED`, ghi thông tin hoàn tiền | Hoàn tiền thủ công COD chỉ chuyển sang `REFUNDED` sau khi đã hoàn tiền ngoài hệ thống; `refund_amount` là số tiền cần hoàn. |

Người mua chỉ được khiếu nại sau khi Vendor từ chối yêu cầu trả hàng, trong vòng 3 ngày từ `decided_at`, và mỗi yêu cầu chỉ khiếu nại một lần. Khiếu nại không tự đổi trạng thái đơn: Manager chấp nhận bằng `OrderService.changeStatus(..., RETURN_APPROVED, ...)`, hoặc kết luận từ chối bằng `ReturnRequestService.rejectEscalation(orderId, note)` với ghi chú bắt buộc. Sau kết luận của Manager, `admin_note` ngăn gửi khiếu nại lần nữa.

Tệp bằng chứng chỉ được tải qua `CloudinaryService`. Mỗi lần tải thành công phải đăng ký xóa theo `publicId` khi transaction rollback để không lưu tệp mồ côi. Controller không nhận `userId`; service lấy người mua từ `CurrentUser`, và đơn không thuộc người đó trả 404.

**Phối hợp PG-20 và PA-21:** Phong có thể dùng `OrderDetailResponse.canRequestReturn`, `returnDeadline`, `returnRequest`, `canEscalate` để hiển thị nút/form và trạng thái; gửi các form `CreateReturnRequest` hoặc `EscalateReturnRequest` cùng ảnh bắt buộc và video tùy chọn. Phát dùng các endpoint manager hiện có để duyệt qua `OrderService.changeStatus`; từ chối khiếu nại cuối cùng gọi `ReturnRequestService.rejectEscalation` kèm ghi chú. Không cập nhật trạng thái đơn trực tiếp hoặc sửa `return_requests` bằng repository ở controller/module khác.

## 11. Check thẻ của tôi (PA-38)

- Trang `/check-my-card` chỉ dành cho `ROLE_USER`; `SecurityConfig` đã khai báo quyền này.
- `GET /api/cards/suggestions?keyword={keyword}` là API công khai, tìm theo tên hoặc số thẻ, chỉ trả thẻ/Game/Series đang hiển thị và giới hạn tối đa 8 gợi ý.
- Trang gửi `cardId`, `condition` và `language` qua query string. Service chỉ tổng hợp sản phẩm SINGLE đang ACTIVE, còn hàng, thuộc Shop APPROVED và chưa bị xóa; giá bán dùng giá hiệu lực sau khuyến mãi.
- Số người bán là số shop phân biệt sau khi lọc tình trạng/ngôn ngữ. Chênh lệch phần trăm được tính `(giá thấp nhất - giá thị trường) / giá thị trường × 100`; số âm nghĩa là giá bán thấp hơn giá thị trường.
- Mini chart tái sử dụng dữ liệu lịch sử giá 30 ngày. Thiếu giá thị trường hoặc lịch sử giá phải hiển thị rõ trạng thái không có dữ liệu.

### Mua lại đơn đã giao (2026-10-26)
- `POST /orders/{id}/reorder`: ROLE_USER, CSRF bắt buộc; lấy người mua từ CurrentUser qua UserOrderService, đơn người khác trả 404. Chỉ nhận DELIVERED.
- OrderReorderService gọi CartService cho toàn bộ dòng sản phẩm theo số lượng cũ; kiểm tra giá, trạng thái và tồn kho hiện tại (gồm lượng đã có trong giỏ). Dòng thiếu hàng/không còn bán bị bỏ qua và thông báo, các dòng hợp lệ vẫn được thêm; chưa trừ tồn kho ở bước này.
- Mỗi lần thêm có transaction riêng để lỗi nghiệp vụ của một dòng không rollback các dòng thành công. Redirect /cart với flash thành công/lỗi.
- Nút Đánh giá trên danh sách dẫn tới sản phẩm đầu tiên với #review; trong chi tiết đơn có liên kết đánh giá theo từng sản phẩm. Điều kiện gửi đánh giá vẫn do ReviewService quyết định.

### Hội thoại phản hồi đánh giá (2026-10-27)
- Lưu mỗi tin nhắn trong `review_replies` với review, người gửi, nội dung và timestamp; không ghi đè các tin nhắn trước. Cột `reviews.vendor_reply` cũ vẫn được đưa vào đầu hội thoại để giữ dữ liệu legacy.
- Chỉ chủ shop của sản phẩm hoặc chính người viết review được gửi tin nhắn; danh tính người gửi và shop lấy từ CurrentUser/DB, không nhận ID người dùng từ request.
- Mỗi tin nhắn gửi thông báo tới phía còn lại với link `/products/{productId}#reviews`. Danh sách trả về kèm `senderRole` để giao diện phân biệt bong bóng khách hàng và Vendor.
