# Báo cáo đồ án — Phần Phong

> Tài liệu này là khung báo cáo dựa trên mã nguồn và tài liệu dự án tại thời điểm rà soát. Các vị trí `[Chèn ảnh ...]` là chỗ để bổ sung ảnh chụp thật khi chạy ứng dụng. Trích đoạn code được rút gọn từ lớp hiện có để đưa vào báo cáo; có thể cần bỏ import hoặc phần bao quanh khi trình bày.

## 1. Template và Layout (PG-01 đến PG-05)

### 1.1. Mô tả chức năng

Ứng dụng dùng Thymeleaf để kết xuất giao diện phía máy chủ, Bootstrap 5 để bố cục responsive, và Thymeleaf Layout Dialect để tái sử dụng khung trang. `layout/main.html` phục vụ trang công khai/người mua; `layout/dashboard.html` cung cấp sidebar theo role cho khu vực quản trị. Header, footer, toast, phân trang và empty state được tách thành fragment. Các trang đăng nhập, đăng ký, xác thực OTP, quên/đổi mật khẩu và trang lỗi dùng chung hệ thống layout và thông báo.

### 1.2. Sơ đồ hoạt động

1. Trình duyệt gửi GET tới route trang (ví dụ `/auth/login`).
2. Web Controller chuẩn bị DTO và dữ liệu cần hiển thị, rồi trả tên template.
3. Thymeleaf ghép nội dung trang với layout tương ứng và các fragment dùng chung.
4. Trình duyệt tải Bootstrap, CSS/JS dùng chung; khi gửi form, CSRF token được gửi cùng request.
5. Khi có lỗi validation, Controller trả lại form cùng BindingResult; khi thành công, Post/Redirect/Get chuyển sang trang tiếp theo và có thể hiển thị flash toast.

### 1.3. Ảnh giao diện

- [Chèn ảnh chụp màn hình trang đăng nhập tại đây]
- [Chèn ảnh chụp màn hình trang đăng ký/xác thực OTP tại đây]
- [Chèn ảnh giao diện dashboard có sidebar tại đây]

### 1.4. Ảnh code có chú thích

```html
<main class="container-fluid flex-grow-1 py-4" layout:fragment="content"></main>
<div th:replace="~{fragments/header :: header}"></div>
<div th:replace="~{fragments/toast :: toast}"></div>
```

`layout/main.html` dành vị trí cho nội dung riêng của trang, đồng thời lắp header và toast ở một chỗ. Trang con chỉ cần khai báo fragment `content`, tránh lặp cấu trúc khung.

### 1.5. Bảng validation và xử lý lỗi

| Trường hợp | Xử lý/thông báo |
|---|---|
| Bỏ trống trường bắt buộc của form | Thông báo gắn với field qua Jakarta Validation và `th:errors` (ví dụ đăng nhập: “Vui lòng nhập email”, “Vui lòng nhập mật khẩu”). |
| Số điện thoại sai định dạng | “Số điện thoại phải có dạng 0xxxxxxxxx hoặc +84xxxxxxxxx”. |
| Mật khẩu/OTP không đạt quy tắc | Form giữ lại dữ liệu an toàn; trường mật khẩu được xóa khỏi model khi lỗi. OTP phải gồm đúng 6 chữ số. |
| CSRF token thiếu/sai khi gọi mutation AJAX | `common.js` dừng request và hiển thị “Không tìm thấy CSRF token. Hãy tải lại trang hoặc kiểm tra cấu hình CSRF.” nếu trang không có token. |

## 2. CloudinaryService và tiện ích giao diện (PG-06, PG-07)

### 2.1. Mô tả chức năng

`CloudinaryService` là cổng upload/xóa media duy nhất. Ảnh được giới hạn JPG/JPEG/PNG, nhỏ hơn 5 MB; video bình luận được giới hạn MP4/WebM, tối đa 50 MB. Service kiểm tra chữ ký nội dung thực của file, trả về URL bảo mật cùng `publicId` để lưu trữ và xóa đúng tài nguyên sau này. `common.js` tập trung các tiện ích giao diện: `fetchWithCsrf`, toast, loading state, xác nhận hành động và preview.

### 2.2. Sơ đồ hoạt động

1. Người dùng chọn ảnh/video trong form; giao diện hiển thị preview.
2. Controller nhận multipart và gọi service nghiệp vụ; service nghiệp vụ gọi `CloudinaryService`.
3. CloudinaryService kiểm tra file rỗng, kích thước, phần mở rộng và magic bytes.
4. File hợp lệ được tải lên Cloudinary; service trả URL và `publicId`.
5. Nghiệp vụ lưu metadata. Nếu transaction rollback, các luồng hỗ trợ đăng ký sản phẩm/đánh giá lên lịch xóa file mới; khi xóa đánh giá, file hiện có được dọn sau commit.
6. Frontend dùng `fetchWithCsrf` cho request thay đổi dữ liệu, bật/tắt loading và hiển thị toast kết quả.

### 2.3. Ảnh giao diện

- [Chèn ảnh preview ảnh trong form sản phẩm tại đây]
- [Chèn ảnh preview media trong form đánh giá tại đây]
- [Chèn ảnh toast/loading state tại đây]

### 2.4. Ảnh code có chú thích

```java
if (file.getSize() >= MAX_IMAGE_SIZE_BYTES) {
    throw new BadRequestException("Ảnh phải có dung lượng nhỏ hơn 5MB");
}
validateImageContent(file);
return upload(file, folder, "image");
```

Giới hạn được quyết định ở server. `validateImageContent` đọc header nhị phân và đối chiếu chữ ký JPEG/PNG, nên chỉ đổi tên `.exe` thành `.jpg` không đủ để qua kiểm tra.

```javascript
if (isMutation && isSameOrigin) {
    const csrf = getCsrf();
    headers.set(csrf.header, csrf.token);
}
if (button) setLoading(button, true, { text: loadingText });
```

Helper dùng token của trang cho request cùng origin và quản lý trạng thái loading cho nút được truyền vào.

### 2.5. Bảng validation

| Trường hợp | Thông báo hiện có |
|---|---|
| File không được chọn/rỗng | “File không được để trống”. |
| Ảnh từ 5 MB trở lên | “Ảnh phải có dung lượng nhỏ hơn 5MB”. |
| Định dạng ảnh hoặc magic bytes không hợp lệ | “Ảnh chỉ chấp nhận định dạng JPG, JPEG hoặc PNG hợp lệ”. |
| Video lớn hơn 50 MB | “Video không được vượt quá 50MB”. |
| Video không phải MP4/WebM hợp lệ | “Video chỉ chấp nhận định dạng MP4 hoặc WebM hợp lệ”. |
| Không đọc được nội dung file | “Không thể đọc nội dung tệp tải lên”. |

## 3. Quản lý Vendor và Shop (PG-10 đến PG-15)

### 3.1. Mô tả chức năng

Vendor đăng ký shop, theo dõi trạng thái xét duyệt, quản lý thông tin shop, đăng sản phẩm SINGLE hoặc sealed/SUPPLY, xem danh sách sản phẩm và chỉnh sửa/ẩn sản phẩm. Shop dùng trạng thái nghiệp vụ hiện có: `PENDING`, `APPROVED`, `REJECTED`, `SUSPENDED`; trang trạng thái diễn giải `SUSPENDED` là shop bị khóa. Trang công khai chỉ cung cấp thông tin shop được phê duyệt. Các thao tác quản lý sản phẩm truy vấn theo shop của tài khoản đăng nhập và kiểm tra ownership trong service.

### 3.2. Sơ đồ hoạt động

1. USER mở `/seller-registration`, nhập thông tin và gửi multipart form.
2. Controller validate DTO; service lấy user từ `CurrentUser`, kiểm tra đăng ký hiện có/tên shop, upload logo nếu có và lưu shop ở trạng thái `PENDING`.
3. Shop bị `REJECTED` có thể nộp lại qua `/seller-registration/resubmit`; dữ liệu được cập nhật, trạng thái trở lại `PENDING`, lý do từ chối được xóa.
4. Sau khi được duyệt, Vendor vào `/vendor/shop/manage` để cập nhật nội dung, logo/banner và thông tin liên hệ.
5. Khi đăng SINGLE, Vendor tìm thẻ trong catalog, chọn thẻ rồi gửi thông tin tình trạng/ngôn ngữ/phiên bản/giá/tồn kho/media. Với sản phẩm sealed/SUPPLY, chọn loại và điền nhóm trường phù hợp.
6. Trang `/vendor/products` lấy dữ liệu theo shop của `CurrentUser`, hỗ trợ từ khóa, loại, trạng thái và phân trang.
7. Sửa/ẩn/xóa mềm gọi service. Service xác minh product thuộc shop hiện tại; cập nhật dùng optimistic locking; “xóa” chuyển trạng thái sang `INACTIVE` và bị chặn nếu còn đơn thuộc nhóm trạng thái đang xử lý.
8. Khách mở `/shops/{id}`; chỉ shop APPROVED mới được hiển thị cùng sản phẩm công khai.

### 3.3. Ảnh giao diện

- [Chèn ảnh form đăng ký shop và trang trạng thái PENDING tại đây]
- [Chèn ảnh form quản lý shop tại đây]
- [Chèn ảnh trang shop công khai đã duyệt tại đây]
- [Chèn ảnh tìm/chọn SINGLE và form đăng bán tại đây]
- [Chèn ảnh form sealed/SUPPLY tại đây]
- [Chèn ảnh danh sách sản phẩm Vendor, bộ lọc và empty state tại đây]
- [Chèn ảnh form sửa sản phẩm và thao tác ẩn tại đây]

### 3.4. Ảnh code có chú thích

```java
Shop shop = shopRepository.findByOwnerId(currentUser.getId()).orElse(null);
if (shop != null && shop.getStatus() != ShopStatus.REJECTED) {
    throw new BadRequestException("Shop của bạn đã được gửi đăng ký và không thể nộp lại ở trạng thái hiện tại");
}
shop.setStatus(ShopStatus.PENDING);
shop.setRejectReason(null);
```

`CurrentUser` quyết định chủ shop; người dùng chỉ tạo shop lần đầu hoặc nộp lại hồ sơ bị từ chối. Không nhận `ownerId` từ form.

```java
if (product.getShop() == null || product.getShop().getOwner() == null
        || !currentUser.getId().equals(product.getShop().getOwner().getId())) {
    throw new ForbiddenException("Bạn không có quyền quản lý sản phẩm của shop khác");
}
```

Đoạn kiểm tra ownership được thực hiện trong service trước khi sửa/xóa sản phẩm, không dựa vào việc ẩn ID ở giao diện.

### 3.5. Bảng validation

| Trường hợp | Thông báo/đáp ứng |
|---|---|
| Tên shop trống hoặc quá 150 ký tự | Validation form: “Vui lòng nhập tên shop”; “Tên shop không được vượt quá 150 ký tự”. |
| Tên shop đã được dùng | “Tên shop này đã được sử dụng. Vui lòng chọn tên khác.” |
| Số điện thoại không đúng mẫu | “Số điện thoại phải có dạng 0xxxxxxxxx hoặc +84xxxxxxxxx”. |
| Shop không ở trạng thái cho phép đăng ký/nộp lại | “Shop của bạn đã được gửi đăng ký và không thể nộp lại ở trạng thái hiện tại”. |
| Shop chưa APPROVED đăng bán sản phẩm | “Chỉ shop đã được duyệt mới có thể đăng bán sản phẩm”. |
| Thẻ/series không còn khả dụng | Lỗi không tìm thấy thẻ hoặc “Series đã bị ẩn hoặc không còn khả dụng”. |
| Trùng thẻ + tình trạng + ngôn ngữ + phiên bản | Hướng dẫn cập nhật tồn kho sản phẩm hiện có thay vì tạo sản phẩm mới. |
| Giá <= 0 hoặc tồn kho đăng bán <= 0 | “Giá bán phải lớn hơn 0”; “Số lượng phải lớn hơn 0”. |
| Giá khi sửa <= 0 hoặc tồn kho âm | “Giá phải lớn hơn 0”; “Tồn kho không được âm”. |
| Product thuộc shop khác | HTTP 403 qua `ForbiddenException`. |
| Còn đơn hàng đang xử lý khi ẩn/xóa | “Không thể xóa sản phẩm đang có đơn hàng được xử lý”. |
| Có cập nhật đồng thời | “Sản phẩm vừa được cập nhật bởi người khác, vui lòng thử lại”. |

## 4. Quản lý Shipper (PG-23 đến PG-25)

### 4.1. Mô tả chức năng

Shipper xem các đơn được phân công, mở trang chi tiết, cập nhật tuần tự trạng thái giao hàng và xem thống kê theo ngày. Các lần chuyển trạng thái đi qua `OrderService.changeStatus`, nhờ đó state machine dùng chung có thể ghi timeline và áp dụng quy tắc chuyển trạng thái. Khi giao đơn COD thành công, Shipper phải xác nhận đã thu đủ tiền; khi giao thất bại phải nhập lý do.

### 4.2. Sơ đồ hoạt động

1. Shipper mở `/shipper/orders`; Controller/Service lấy danh sách gắn với shipper hiện tại và hiển thị theo trạng thái.
2. Shipper mở `/shipper/orders/{id}`; Service kiểm tra đơn được phân công cho tài khoản đang đăng nhập.
3. Frontend hiển thị hành động phù hợp với trạng thái hiện tại và gửi AJAX POST có CSRF.
4. Backend kiểm tra quyền sở hữu, bắt buộc lý do cho `DELIVERY_FAILED`, bắt buộc xác nhận COD cho đơn COD chuyển sang `DELIVERED`.
5. Backend gọi `OrderService.changeStatus(orderId, newStatus, note)`; kết quả JSON dùng để toast và tải lại/cập nhật giao diện.
6. Trang `/shipper/statistics` nạp thống kê theo khoảng ngày; API trả dữ liệu chuỗi ngày để Chart.js vẽ cột. Thẻ “Thu nhập của bạn” cộng `shipping_fee` của các đơn `DELIVERED` theo `delivered_at`; thẻ “COD đã thu” vẫn chỉ cộng tiền COD đã xác nhận.

### 4.3. Ảnh giao diện

- [Chèn ảnh danh sách đơn Shipper và bộ lọc trạng thái tại đây]
- [Chèn ảnh chi tiết đơn/timeline tại đây]
- [Chèn ảnh modal xác nhận COD và modal giao thất bại tại đây]
- [Chèn ảnh dashboard thống kê và biểu đồ tại đây]

### 4.4. Ảnh code có chú thích

```java
if (newStatus == OrderStatus.DELIVERY_FAILED
        && (request.note() == null || request.note().isBlank())) {
    throw new BadRequestException("Vui lòng nhập lý do giao hàng thất bại");
}
if (order.status() == OrderStatus.SHIPPING && newStatus == OrderStatus.DELIVERED
        && order.codAmount() != null && !request.codCollectedConfirmed()) {
    throw new BadRequestException("Vui lòng xác nhận đã thu đủ tiền COD trước khi giao thành công");
}
orderService.changeStatus(orderId, newStatus, request.note());
```

Service dùng dữ liệu đơn sau khi kiểm tra ownership, xác thực điều kiện giao hàng/COD ở server rồi gọi state machine chung, không tự gán trạng thái.

```java
long finished = delivered + failed;
BigDecimal successRate = finished == 0
        ? BigDecimal.ZERO.setScale(2)
        : BigDecimal.valueOf(delivered).multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(finished), 2, RoundingMode.HALF_UP);
```

Tỷ lệ giao thành công tránh chia cho 0; các số liệu và biểu đồ lấy theo `CurrentUser.getId()` thay vì nhận shipper ID từ query client.

### 4.5. Bảng validation

| Trường hợp | Thông báo/đáp ứng |
|---|---|
| Giao thất bại nhưng không nhập lý do | “Vui lòng nhập lý do giao hàng thất bại”. |
| Đơn COD chuyển sang DELIVERED nhưng chưa xác nhận thu tiền | “Vui lòng xác nhận đã thu đủ tiền COD trước khi giao thành công”. |
| Chuyển trạng thái không hợp lệ/nhảy cóc | State machine `OrderService` từ chối; API trả lỗi để giao diện báo toast. |
| Xem/cập nhật đơn không được phân công | Service kiểm tra assignment và từ chối truy cập (403). |
| Không có đơn kết thúc trong khoảng thời gian | Tỷ lệ giao thành công trả 0%. |
| Khoảng ngày đảo ngược | “Ngày bắt đầu phải trước hoặc bằng ngày kết thúc”. |
| Khoảng thống kê vượt 366 ngày | “Khoảng thời gian thống kê không được vượt quá 366 ngày”. |

## 5. Tương tác người dùng (PG-26 đến PG-29)

### 5.1. Wishlist — PG-26

**Mô tả chức năng.** Người dùng đã đăng nhập có thể thích/bỏ thích sản phẩm công khai ngay trên thẻ sản phẩm và xem wishlist có phân trang. Toggle trả `wishlisted` cùng `favoriteCount`; backend khóa sản phẩm, cập nhật quan hệ wishlist và đồng bộ số lượt yêu thích.

**Sơ đồ hoạt động.** Bấm tim → `wishlist.js` gửi `POST /api/wishlist/toggle/{productId}` bằng `fetchWithCsrf` → service xác minh sản phẩm còn công khai → tạo/xóa quan hệ → cập nhật số lượt → trả JSON → frontend đổi màu tim/số đếm. Guest được giao diện nhắc đăng nhập. API batch trạng thái nhận nhiều product ID và giới hạn 100 ID.

**Ảnh giao diện.** [Chèn ảnh nút tim chưa thích/đã thích và trang `/wishlist` tại đây]

**Ảnh code có chú thích.**

```java
wishlistRepository.flush();
int favoriteCount = Math.toIntExact(wishlistRepository.countByProductId(productId));
product.setFavoriteCount(favoriteCount);
productRepository.saveAndFlush(product);
```

Đếm lại từ quan hệ lưu trữ sau khi toggle để phản hồi số yêu thích hiện hành.

**Bảng validation.**

| Trường hợp | Đáp ứng |
|---|---|
| Guest gọi API toggle | Endpoint yêu cầu role USER; không cho thao tác ẩn danh. |
| Sản phẩm không tồn tại/không được mở bán | “Sản phẩm không tồn tại hoặc hiện không được mở bán”. |
| Batch trạng thái vượt 100 sản phẩm | “Không thể kiểm tra quá 100 sản phẩm trong một lần”. |
| Bấm lại sản phẩm đã yêu thích | Xóa quan hệ, trả `wishlisted=false`; unique user/product chống bản ghi trùng. |

### 5.2. Sản phẩm đã xem — PG-27

**Mô tả chức năng.** Người mua đăng nhập có thể xem trang lịch sử gần đây; trang chi tiết gọi endpoint ghi nhận lượt xem. Bản ghi cũ được cập nhật `viewedAt`, và lịch sử được giữ tối đa 20 sản phẩm. Sản phẩm/shop không còn công khai sẽ không được ghi nhận/hiển thị. Guest vào trang lịch sử được chuyển đến đăng nhập.

**Sơ đồ hoạt động.** Mở chi tiết sản phẩm → nếu USER đã đăng nhập, JS gửi `POST /api/viewed-products/record/{productId}` → service xác thực user hiện tại và tính công khai sản phẩm → insert hoặc cập nhật thời gian xem → giữ 20 bản ghi mới nhất, xóa phần cũ → trang `/viewed-products` truy vấn và phân trang.

**Ảnh giao diện.** [Chèn ảnh trang chi tiết có gọi ghi nhận và trang “Sản phẩm đã xem” tại đây]

**Ảnh code có chú thích.**

```java
ViewedProduct viewedProduct = viewedProductRepository.findByUserIdAndProductId(userId, productId)
        .orElseGet(() -> createViewedProduct(user, product));
viewedProduct.setViewedAt(LocalDateTime.now());
viewedProductRepository.saveAndFlush(viewedProduct);
trimHistory(userId);
```

Lượt xem lặp lại cập nhật cùng bản ghi; `trimHistory` giữ tối đa 20 bản ghi xếp theo thời gian mới nhất.

**Bảng validation.**

| Trường hợp | Đáp ứng |
|---|---|
| Chưa đăng nhập gọi API | Yêu cầu xác thực; không ghi lịch sử cho guest. |
| User truyền ID của user khác | “Không thể ghi lịch sử xem cho tài khoản khác” (403). |
| Sản phẩm ẩn/xóa, shop chưa duyệt/đã xóa, catalog không hoạt động | Bỏ qua lượt xem và không đưa vào danh sách công khai. |
| Vượt quá 20 lịch sử | Tự xóa các bản ghi cũ, giữ 20 sản phẩm gần nhất. |

### 5.3. Đánh giá sản phẩm — PG-28

**Mô tả chức năng.** Trang chi tiết công khai hiển thị điểm trung bình, số lượt và danh sách review phân trang. Người mua đủ điều kiện gửi đánh giá 1–5 sao sau khi có đơn DELIVERED chứa sản phẩm; mỗi cặp đơn hàng/sản phẩm chỉ được đánh giá một lần. Điểm trung bình và số lượt được tính lại từ review hợp lệ.

**Sơ đồ hoạt động.** Khách xem trang chi tiết và danh sách review → USER gọi API eligibility → service tìm đơn đã giao chưa đánh giá → giao diện mở form nếu đủ điều kiện → submit rating/comment/media → service khóa sản phẩm, kiểm tra lại đơn đủ điều kiện và unique → lưu review → cập nhật rating aggregate → trả response để UI cập nhật.

**Ảnh giao diện.** [Chèn ảnh sao trung bình, form chọn sao và danh sách review tại đây]

**Ảnh code có chú thích.**

```java
return productRepository.findPublicProduct(productId).isPresent()
        && !eligibilityRepository.findEligibleOrderIds(userId, productId, PageRequest.of(0, 1)).isEmpty();
```

Điều kiện gắn với user hiện tại, sản phẩm công khai và đơn hàng đã giao chưa có review của sản phẩm đó.

**Bảng validation.**

| Trường hợp | Thông báo/đáp ứng |
|---|---|
| Chưa mua hoặc đơn chưa giao | “Bạn cần mua sản phẩm này và đơn hàng phải giao thành công để đánh giá.” |
| Đã đánh giá sản phẩm trong các đơn đã giao | “Bạn đã đánh giá sản phẩm trong tất cả đơn hàng đã giao.” |
| Rating ngoài 1–5 | “Đánh giá phải từ 1 đến 5 sao”. |
| Comment trống/null | Hợp lệ vì comment không bắt buộc. |
| Comment ngắn hoặc bỏ trống | Hợp lệ; không giới hạn độ dài tối thiểu. |
| Comment dài hơn 2.000 ký tự | “Nhận xét không được vượt quá 2.000 ký tự”. |
| Lặp đánh giá cùng order/product | “Bạn đã đánh giá sản phẩm trong đơn hàng này”. |

### 5.4. Bình luận kèm ảnh/video — PG-29

**Mô tả chức năng.** Mở rộng review để đính kèm tối đa 5 ảnh và 1 video; người tạo review được sửa hoặc xóa review của mình. Media được lưu URL/public ID, upload qua CloudinaryService. Khi xóa review, hệ thống soft-delete review, gỡ metadata media và lên lịch xóa tài nguyên cloud sau khi transaction commit.

**Sơ đồ hoạt động.** Chọn sao, comment và file → JS hiển thị preview/đếm ký tự, gửi multipart AJAX → backend validate quyền mua, giới hạn file và media → CloudinaryService kiểm tra magic bytes rồi upload → lưu review/media → frontend hiển thị comment card. Sửa/xóa trước hết kiểm tra owner; khi xóa, cập nhật DB trong transaction rồi xóa file Cloudinary sau commit. Nếu upload mới mà transaction rollback, đăng ký cleanup để không để file mồ côi.

**Ảnh giao diện.** [Chèn ảnh preview media, review card, modal xem ảnh và nút sửa/xóa của chủ review tại đây]

**Ảnh code có chú thích.**

```java
if (review.getUser() == null || !userId.equals(review.getUser().getId())) {
    throw new ForbiddenException("Bạn không có quyền chỉnh sửa đánh giá của người khác");
}
```

Kiểm tra owner phía service áp dụng cho sửa và xóa, dù client tự thay `reviewId`.

```java
if (existingImages + newImageCount > MAX_IMAGES) {
    throw new BadRequestException("Mỗi đánh giá được đính kèm tối đa 5 ảnh");
}
if (existingVideos + (newVideo ? 1 : 0) > 1) {
    throw new BadRequestException("Mỗi đánh giá chỉ được đính kèm 1 video");
}
```

Giới hạn được tính trên tổng media cũ và mới khi cập nhật; không chỉ dựa vào kiểm tra input HTML.

**Bảng validation.**

| Trường hợp | Thông báo/đáp ứng |
|---|---|
| Hơn 5 ảnh trên một review | “Mỗi đánh giá được đính kèm tối đa 5 ảnh”. |
| Hơn 1 video | “Mỗi đánh giá chỉ được đính kèm 1 video”. |
| Loại hoặc chữ ký file không đúng | CloudinaryService trả lỗi định dạng ảnh/video hợp lệ. |
| Người khác sửa/xóa review | “Bạn không có quyền chỉnh sửa đánh giá của người khác” (403). |
| Xóa review | Soft-delete review, gỡ metadata và xóa file cloud sau commit. |
| Upload thất bại giữa transaction | File đã upload được lên lịch dọn nếu transaction rollback. |

## 6. Danh sách Prompt đã sử dụng

Phần này tóm tắt các mục được ghi trong `docs/prompts-log.md`; đây là nhóm prompt chính, không phải bản chép nguyên văn từng prompt.

| Nhóm task | Nội dung prompt chính trong nhật ký |
|---|---|
| PG-01 đến PG-05 — Giao diện và layout | Nhật ký có các mục PG-03, PG-05 và các prompt bổ sung về layout/template. Nội dung tập trung dựng giao diện Thymeleaf/Bootstrap, trang xác thực và khung giao diện dùng chung. Nhật ký không có mục PG-01/PG-02 riêng biệt để trích dẫn nguyên văn. |
| PG-06, PG-07 — Cloudinary/common UI | Xây CloudinaryService upload/xóa media, quản lý URL/public ID; tạo tiện ích JavaScript dùng chung cho toast, loading, CSRF và thao tác giao diện. |
| PG-10 đến PG-15 — Vendor/shop | Đăng ký và nộp lại shop; form quản lý và trang công khai shop; danh sách sản phẩm Vendor; đăng SINGLE và sealed/SUPPLY; sửa, ẩn/xóa mềm sản phẩm, kiểm tra ownership và giữ giao diện Midnight Rare. |
| PG-23 đến PG-25 — Shipper | Danh sách/chi tiết đơn được giao; cập nhật trạng thái qua OrderService, bắt buộc lý do giao thất bại và xác nhận COD; dashboard thống kê và biểu đồ theo khoảng ngày. |
| PG-26 — Wishlist | Toggle wishlist bằng AJAX theo hợp đồng API, chống trùng, đồng bộ `favoriteCount`, trạng thái tim theo batch và trang wishlist phân trang. |
| PG-27 — Viewed Products | Ghi nhận sản phẩm đã xem, cập nhật thời gian xem, giới hạn lịch sử 20, kiểm tra khả năng hiển thị công khai và trang lịch sử. |
| PG-28 — Review | Kiểm tra điều kiện dựa trên đơn DELIVERED, unique theo order/product, rating 1–5, comment tùy chọn, không giới hạn độ dài tối thiểu, cập nhật rating aggregate và kiểm thử. |
| PG-29 — Review media | Mở rộng review bằng tối đa 5 ảnh/1 video, upload multipart qua Cloudinary, preview, ownership khi sửa/xóa, dọn media khi rollback/xóa và cập nhật giao diện comment. |

Nhật ký còn có prompt về các task khác (PA/PU/PG) ngoài phạm vi phần Phong; chúng không được gán vào các chức năng trong báo cáo này.

## 7. Tài liệu tham khảo nội bộ

- `AGENTS.md` — kiến trúc, bảo mật, validation, upload và quy tắc giao diện.
- `docs/ENTITY_SPEC.md` — nguồn quy ước entity/trạng thái/quan hệ.
- `docs/SECURITY_SPEC.md` — hợp đồng xác thực, quyền và ownership.
- `docs/CONTRACTS.md` — chữ ký/hành vi service dùng chung, đặc biệt state machine đơn hàng.
- `docs/prompts-log.md` — nhật ký prompt được tóm tắt ở Mục 6.
