# TCG Shop

Website marketplace thẻ sưu tầm TCG, xây dựng bằng Java 21, Spring Boot, Thymeleaf và Bootstrap.

## Giao diện và nguồn template

Trang chủ hiện tại được thiết kế dựa trên **Shop Homepage** của **Start Bootstrap** (template do David Miller tạo và duy trì).

- Trang template chính thức: <https://startbootstrap.com/template/shop-homepage/>
- Mã nguồn và bản phân phối: <https://github.com/StartBootstrap/startbootstrap-shop-homepage>
- Giấy phép template: **MIT**, Copyright 2013–2023 Start Bootstrap LLC; xem <https://github.com/StartBootstrap/startbootstrap-shop-homepage/blob/master/LICENSE>.
- Bản tham khảo dùng cho thiết kế: **Start Bootstrap Shop Homepage v5.0.6**, dựa trên Bootstrap **5.2.3**.
- Bootstrap CSS và JavaScript bundle được tải từ bản phát hành v5.2.3 chính thức và lưu cục bộ; giấy phép Bootstrap: **MIT**, thông tin tại <https://github.com/twbs/bootstrap/blob/v5.2.3/LICENSE>.

Template cho phép sử dụng, chỉnh sửa và phân phối theo MIT; khi phân phối các bản sao hoặc phần đáng kể của template cần giữ thông báo bản quyền và giấy phép theo điều khoản MIT. Giao diện trong `src/main/resources/templates/index.html` đã được Việt hóa và điều chỉnh theo TCG Shop; phần minh họa thẻ là CSS tự tạo, không dùng ảnh sản phẩm bên ngoài.

**Cần hỏi giảng viên xem dashboard có được dùng chung template Bootstrap này hay không.** Chưa giả định đã được chấp thuận.

## Tài nguyên giao diện

- `src/main/resources/templates/index.html`: trang chủ Thymeleaf, dùng đường dẫn static tương thích Spring Boot và bố cục responsive.
- `src/main/resources/static/css/bootstrap.min.css` và `bootstrap.min.css.map`: Bootstrap 5.2.3 CSS và source map được stylesheet tham chiếu.
- `src/main/resources/static/css/custom.css`: CSS dùng chung, biến màu/font/nền/chữ và các kiểu tùy chỉnh; được nạp sau Bootstrap.
- `src/main/resources/static/js/bootstrap.bundle.min.js` và `bootstrap.bundle.min.js.map`: Bootstrap 5.2.3 bundle (bao gồm Popper), cùng source map được bundle tham chiếu; JavaScript dùng cho menu điều hướng responsive.
- `src/main/resources/static/vendor/licenses/`: nguyên văn giấy phép MIT của Bootstrap và template Start Bootstrap.
- `src/main/resources/static/img/` hiện không cần ảnh vì hình minh họa được dựng bằng CSS; không sao chép thêm hình ảnh không sử dụng. `static/js/` chỉ chứa bundle cần thiết; chưa có JavaScript nghiệp vụ riêng.
- Bootstrap được phục vụ trực tiếp từ Spring Boot, không cần truy cập CDN để tải CSS hoặc JavaScript giao diện.

Các mặt hàng trên trang chủ hiện là nội dung minh họa giao diện, chưa được nối với danh mục hoặc giá từ database.

## Chạy project

Yêu cầu Java 21 và Maven Wrapper có sẵn trong repository. Cấu hình profile local trước khi chạy:

1. Tạo `src/main/resources/application-local.properties` trên máy của bạn; file này không được commit. Cấu hình datasource và các thông tin môi trường cần thiết theo cấu hình local.
2. Tại thư mục gốc project, chạy trên Windows PowerShell:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

3. Mở <http://localhost:8080/>.

Kiểm tra build/test bằng Maven Wrapper:

```powershell
.\mvnw.cmd test
```

> `src/main/resources/application.properties` bật profile `local`; ứng dụng có thể không khởi động nếu chưa tạo và điền cấu hình local cần thiết. Kiểm tra cấu hình datasource trong `application-local.properties` trước khi chạy.
