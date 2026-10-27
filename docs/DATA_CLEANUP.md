# Dọn sản phẩm thiếu ảnh thật

`DataCleanupRunner` chỉ chạy khi bật `app.data-cleanup.enabled=true`. Runner tải URL và public ID từ toàn bộ catalog `seed/*.json`; nếu không đọc và xác thực được danh sách asset thì sẽ dừng mà không ẩn sản phẩm.

Ứng viên là sản phẩm chưa soft-delete mà cả ảnh sản phẩm lẫn ảnh Card (nếu có) đều thiếu URL hoặc trỏ tới tên ảnh placeholder. Mọi sản phẩm tham chiếu URL/public ID trong catalog được giữ lại. Script chỉ đổi `deleted=true` và `status=INACTIVE`; không xóa cứng Product, ảnh Cloudinary, hoặc dữ liệu liên quan đơn hàng.

## Xem trước

Chạy ứng dụng với:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--app.data-cleanup.enabled=true"
```

Mặc định `app.data-cleanup.apply=false`. Xem log `Product cleanup dry-run` để biết số lượng và ID ứng viên. Không cần thêm thuộc tính vào `application-local.properties`.

## Áp dụng

Sau khi đã xem trước, chạy:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--app.data-cleanup.enabled=true,--app.data-cleanup.apply=true"
```

Chỉ chạy một lần; bỏ hai program arguments sau khi hoàn tất. Các sản phẩm bị ẩn có thể khôi phục bằng cách đặt `deleted=false` và trạng thái phù hợp qua công cụ quản trị/DB sau khi kiểm tra.

## Biểu đồ Manager

`system-dashboard.js` chỉ áp màu chữ sáng, lưới mờ và tooltip tối khi dashboard Manager được render; màu sắc Admin không đổi. Các đường doanh thu dùng tím/vàng/xanh lá để tương phản với nền Midnight Rare.
