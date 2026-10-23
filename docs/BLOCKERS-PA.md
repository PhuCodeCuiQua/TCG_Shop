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
