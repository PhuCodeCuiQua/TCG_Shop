# Blockers PA

## PA-18 - Logo nhà vận chuyển

- **Cần:** Bổ sung `logo_public_id` vào `shipping_carriers` và field tương ứng trên entity `ShippingCarrier`.
- **Lý do:** Entity hiện chỉ lưu `logo_url`; không thể thay hoặc xóa ảnh Cloudinary cũ một cách an toàn nếu không có `public_id`. Quy tắc PA hiện tại không cho phép tự sửa Entity dùng chung.
- **Ảnh hưởng:** Chức năng tạo/cập nhật nhà vận chuyển không nhận upload logo. Nhà vận chuyển chưa có logo vẫn tạo/sửa được. Nếu xóa nhà vận chuyển còn logo, hệ thống chỉ ẩn bản ghi để tránh làm mồ côi ảnh.
- **TODO(PA-18):** Sau khi Phú duyệt schema và bổ sung `logo_public_id`, tích hợp upload, thay ảnh và xóa ảnh cũ qua `CloudinaryService`.
