# Blockers PA

## PA-18 - Logo nhà vận chuyển

- **Cần:** Bổ sung `logo_public_id` vào `shipping_carriers` và field tương ứng trên entity `ShippingCarrier`.
- **Lý do:** Entity hiện chỉ lưu `logo_url`; không thể thay hoặc xóa ảnh Cloudinary cũ một cách an toàn nếu không có `public_id`. Quy tắc PA hiện tại không cho phép tự sửa Entity dùng chung.
- **Ảnh hưởng:** Chức năng tạo/cập nhật nhà vận chuyển không nhận upload logo. Nhà vận chuyển chưa có logo vẫn tạo/sửa được. Nếu xóa nhà vận chuyển còn logo, hệ thống chỉ ẩn bản ghi để tránh làm mồ côi ảnh.
- **TODO(PA-18):** Sau khi Phú duyệt schema và bổ sung `logo_public_id`, tích hợp upload, thay ảnh và xóa ảnh cũ qua `CloudinaryService`.

## PA-17 - Ngày hiệu lực tỷ lệ hoa hồng

- **Cần:** Bổ sung cột `effective_from` vào `commission_settings` và field tương ứng trên entity `CommissionSetting`.
- **Lý do:** Yêu cầu chọn dòng mới nhất có `effective_from <= hôm nay`, nhưng entity/schema hiện chỉ có `default_rate`, `updated_by` và timestamp auditing. Không được tự sửa Entity dùng chung trong task này.
- **Ảnh hưởng:** Tỷ lệ mặc định được thêm qua trang Admin áp dụng ngay; chưa hỗ trợ lên lịch thay đổi trong tương lai. `getRate` hiện chọn dòng mới nhất theo `created_at` vì chưa có ngày hiệu lực để lọc.
- **TODO(PA-17):** Sau khi Phú duyệt và bổ sung `effective_from`, lọc lịch sử hoa hồng theo ngày hiệu lực trong repository/service và hiển thị ngày hiệu lực trên giao diện.

## PA-13 - Khu vực phụ trách của Shipper

- **Cần:** Phê duyệt và bổ sung cột/field lưu khu vực phụ trách trên `users`/entity `User`.
- **Lý do:** Yêu cầu PA-13 cần nhập khu vực khi tạo Shipper, nhưng `User` hiện không có field này và quy tắc task cấm tự sửa Entity dùng chung.
- **Ảnh hưởng:** Tạo tài khoản Manager/Shipper, gửi mật khẩu tạm, bắt buộc đổi mật khẩu, lọc trạng thái và đếm đơn đang giao đã hoạt động; khu vực không được nhận hoặc lưu cho đến khi schema được bổ sung.
- **TODO(PA-13):** Sau khi Phú phê duyệt field khu vực, bổ sung request/validation, lưu khu vực khi tạo Shipper và hiển thị/lọc khu vực trong danh sách.

## PA-14 - Lọc cửa hàng chưa được duyệt khỏi danh sách sản phẩm công khai

- **Cần:** Mọi truy vấn danh sách sản phẩm công khai phải lọc `shop.status = APPROVED` và shop chưa bị xóa.
- **Lý do:** Trang shop công khai và danh sách bestseller trên trang chủ hiện áp dụng điều kiện này.
- **Ảnh hưởng:** Trang catalog tìm kiếm sản phẩm trên toàn sàn vẫn chưa được triển khai; không được bỏ điều kiện khi bổ sung catalog đó.
- **TODO(PA-14):** Khi có catalog toàn sàn, kiểm thử shop PENDING/REJECTED/SUSPENDED và shop đã xóa không trả sản phẩm công khai.

## PA-15 - Lọc sản phẩm bị gỡ khỏi danh sách công khai

- **Cần:** Mọi truy vấn công khai phải chỉ lấy `ProductStatus.ACTIVE`, shop đã duyệt và shop/sản phẩm chưa bị xóa.
- **Lý do:** Trang shop công khai và danh sách bestseller trên trang chủ hiện áp dụng các điều kiện này.
- **Ảnh hưởng:** Catalog tìm kiếm sản phẩm trên toàn sàn vẫn chưa có; cần giữ bộ lọc này khi triển khai.
- **TODO(PA-15):** Khi có catalog toàn sàn, kiểm thử sản phẩm BANNED/INACTIVE và shop chưa được duyệt không xuất hiện.

## PA-30 - Lịch sử xem gần đây phụ thuộc PG-27

- **Cần:** Phần PG-27 của Phong cần ghi lượt xem sản phẩm vào `viewed_products`.
- **Lý do:** Trong nhánh triển khai PA-30 hiện tại, `ViewedProductServiceImpl.record(...)` vẫn là stub và không lưu dữ liệu.
- **Ảnh hưởng:** Trang User đã đọc lịch sử từ `viewed_products` theo đúng user đăng nhập; mục “Xem gần đây” sẽ rỗng cho đến khi PG-27 ghi lượt xem thực tế.
- **TODO(PA-30):** Khi tích hợp với nhánh PG-27, kiểm tra luồng ghi lịch sử và xác nhận mục “Xem gần đây” hiển thị dữ liệu thật.
