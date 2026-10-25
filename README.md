# TCG_Shop

TCG_Shop là website marketplace mua bán thẻ bài sưu tầm (Trading Card Game). Người mua có thể tìm và so sánh sản phẩm từ nhiều cửa hàng; Vendor quản lý shop, sản phẩm và đơn hàng; các vai trò Manager, Admin và Shipper có khu vực thao tác riêng.

## Công nghệ và yêu cầu môi trường

- Java JDK 21.
- MySQL 8.0 trở lên.
- Maven Wrapper có sẵn trong repository (`mvnw` / `mvnw.cmd`); không bắt buộc cài Maven riêng.
- Git để lấy mã nguồn; IDE Java như IntelliJ IDEA hoặc VS Code và trình duyệt hiện đại được khuyến nghị.
- Kết nối mạng khi build lần đầu để Maven tải dependency. Các trang dùng CDN (Bootstrap Icons, Chart.js, SockJS/STOMP) cũng cần mạng khi tải những thư viện đó.

Dự án dùng Spring Boot 3.5.x, Spring Data JPA/Hibernate, Thymeleaf, Spring Security 6, JWT, Spring WebSocket/STOMP, Spring Mail, MapStruct, Lombok và Cloudinary.

## Cài đặt và cấu hình

### 1. Tạo database

Tạo database MySQL rỗng. Tên mặc định được hướng dẫn là `tcg_shop`; URL JDBC trong cấu hình local có thể đổi nếu bạn dùng tên hoặc cổng khác.

```sql
CREATE DATABASE tcg_shop
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

### 2. Tạo cấu hình local

Tạo file `src/main/resources/application-local.properties` trên máy cá nhân. File này đã được `.gitignore` loại trừ, không commit thông tin bí mật.

Ví dụ cấu hình phát triển:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/tcg_shop?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh
spring.datasource.username=root
spring.datasource.password=THAY_BANG_MAT_KHAU_MYSQL
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# Bắt buộc: Base64 của ít nhất 32 byte ngẫu nhiên; không dùng giá trị mẫu.
app.jwt.secret=DIEN_SECRET_BASE64_DA_TAO

# SMTP dùng để gửi OTP/email. Có thể thay host và port theo nhà cung cấp.
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=EMAIL_GUI
spring.mail.password=MAT_KHAU_UNG_DUNG_SMTP
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# Nhập thông tin Cloudinary riêng qua biến môi trường hoặc điền local.
cloudinary.cloud-name=${CLOUDINARY_CLOUD_NAME:}
cloudinary.api-key=${CLOUDINARY_API_KEY:}
cloudinary.api-secret=${CLOUDINARY_API_SECRET:}

# Chỉ dùng false khi phát triển local qua HTTP.
app.cookie.secure=false

# Seeder bật mặc định; thay mật khẩu tài khoản mẫu nếu cần.
app.seed.enabled=true
app.seed.default-password=Tcg@12345
```

Tạo JWT secret Base64 bằng PowerShell:

```powershell
$bytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
[Convert]::ToBase64String($bytes)
```

Đưa chuỗi sinh được vào `app.jwt.secret`. Ứng dụng sẽ không khởi động nếu secret thiếu, chưa phải Base64 hợp lệ, ngắn hơn 32 byte sau giải mã hoặc còn chứa giá trị `CHANGE_ME`.

Các thông tin Cloudinary có thể khai báo bằng biến môi trường `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`. Không dùng credential mẫu/fallback trong môi trường chia sẻ hoặc production; hãy cấp credential riêng và chỉ lưu ở cấu hình local hoặc secret manager.

`application.properties` đang bật profile `local`, đặt `spring.jpa.hibernate.ddl-auto=update` và `spring.jpa.open-in-view=false`. `update` phù hợp cho môi trường phát triển của dự án; không dùng cấu hình này thay cho quy trình migration/backup trên dữ liệu production.

### 3. Chạy ứng dụng

Từ thư mục gốc:

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

macOS, Linux hoặc Git Bash:

```bash
./mvnw spring-boot:run
```

Hoặc mở project trong IDE, đặt JDK 21 và chạy class Spring Boot entry point sau khi đã tạo database và file cấu hình local. Mặc định ứng dụng phục vụ tại <http://localhost:8080/>.

Chạy test bằng wrapper:

```powershell
.\mvnw.cmd test
```

### MoMo

MoMo sandbox chưa được tích hợp hoàn chỉnh để tạo đơn: hiện luồng đặt hàng từ chối phương thức MOMO và yêu cầu chọn COD. Vì vậy chưa có bộ biến cấu hình MoMo đang được ứng dụng sử dụng; không điền key MoMo theo hướng dẫn chung nếu chưa triển khai phần tích hợp.

## Tài khoản mẫu (chỉ dùng phát triển)

`DataSeeder` tự tạo dữ liệu mẫu khi khởi động, nếu chưa có dữ liệu tương ứng. Seeder được bật mặc định; có thể tắt bằng `app.seed.enabled=false`. Tài khoản được tạo với mật khẩu chung mặc định `Tcg@12345`; thay mật khẩu bằng `app.seed.default-password` trong local config trước khi chạy nếu muốn dùng giá trị khác.

| Vai trò | Email |
|---|---|
| Admin | `admin@tcgshop.local` |
| Manager | `manager@tcgshop.local` |
| Vendor | `vendor1@tcgshop.local`, `vendor2@tcgshop.local` |
| User | `user1@tcgshop.local` |
| Shipper | `shipper1@tcgshop.local` |

Các tài khoản mẫu được seed ở trạng thái hoạt động và email đã xác minh. Dữ liệu catalog mẫu gồm một số Game, Series, Card, sản phẩm, shop, hãng vận chuyển và lịch sử giá. Không dùng tài khoản/mật khẩu seed trên môi trường thật.

## Cấu trúc chính

```text
src/main/java/com/tcgshop/
├── controller/web/       # Controller trả trang Thymeleaf
├── controller/rest/      # Controller REST trả JSON
├── dto/                  # Request/Response dùng giữa các tầng
├── entity/               # Entity JPA và enums
├── exception/            # Exception nghiệp vụ và xử lý lỗi chung
├── mapper/               # MapStruct mapper
├── repository/           # Truy vấn Spring Data JPA
├── security/             # JWT, CurrentUser và xác thực
├── service/              # Interface nghiệp vụ
│   └── impl/             # Các implementation
└── config/               # Cấu hình Spring và DataSeeder

src/main/resources/
├── templates/            # Thymeleaf: layout, fragments và trang theo vai trò
└── static/
    ├── css/              # Bootstrap và CSS giao diện
    ├── js/               # JavaScript giao diện
    ├── img/              # Hình ảnh tĩnh
    └── lib/              # Thư viện frontend lưu cục bộ

docs/                     # Đặc tả Entity, bảo mật, hợp đồng và prompt log
```

## Giao diện, template và thư viện frontend

- **Bootstrap 5.2.3** được phục vụ cục bộ. Trang chủ được Việt hóa và phát triển dựa trên template **Start Bootstrap Shop Homepage v5.0.6** (David Miller / Start Bootstrap): [trang template](https://startbootstrap.com/template/shop-homepage/), [repository](https://github.com/StartBootstrap/startbootstrap-shop-homepage), giấy phép **MIT**. Bản quyền và giấy phép được lưu trong `src/main/resources/static/vendor/licenses/`.
- Bootstrap CSS/JS: giấy phép **MIT**, dùng bản lưu tại `src/main/resources/static/css/` và `src/main/resources/static/js/`. [Bootstrap v5.2.3](https://github.com/twbs/bootstrap/tree/v5.2.3).
- [Bootstrap Icons 1.11.3](https://github.com/twbs/icons): MIT, nạp từ jsDelivr.
- [Chart.js 4.4.8](https://github.com/chartjs/Chart.js): MIT, dùng cho biểu đồ doanh thu, thống kê và lịch sử giá.
- [SockJS Client 1.6.1](https://github.com/sockjs/sockjs-client) và [STOMP.js 2.3.3](https://github.com/jmesnil/stomp-websocket): dùng cho WebSocket/STOMP.
- JavaScript giao diện dùng JavaScript thuần và các tiện ích chung trong `static/js/common.js`; không dùng React, Vue hoặc Angular.

Các trang có thư viện CDN cần kết nối Internet khi mở trang. Bootstrap cốt lõi được đóng gói trong project nên không phụ thuộc CDN để hiển thị layout cơ bản.

## Chức năng chính

- Đăng ký, đăng nhập, xác minh OTP, quên/đặt lại mật khẩu và quản lý hồ sơ.
- Trang chủ marketplace, duyệt/tìm kiếm Game, Series, Card và sản phẩm; so sánh sản phẩm cùng thẻ và xem lịch sử giá.
- Đăng ký shop, xem trạng thái xét duyệt, quản lý thông tin shop và trang shop công khai.
- Vendor quản lý sản phẩm, khuyến mãi, mã giảm giá, đơn hàng, yêu cầu trả hàng và doanh thu.
- Shipper xem đơn được phân công, cập nhật trạng thái giao hàng và xem thống kê giao nhận.
- Giỏ hàng, xem trước checkout, đặt đơn COD, theo dõi đơn và yêu cầu trả hàng.
- Wishlist, lịch sử sản phẩm đã xem, đánh giá có ảnh/video và thông báo trong ứng dụng.
- Khu vực quản trị catalog, shop, tài khoản, hãng vận chuyển, hoa hồng, khuyến mãi/mã giảm giá sàn, phân công Shipper và đối soát dữ liệu.

Một số phần được mô tả là placeholder hoặc còn phụ thuộc công việc tích hợp của nhóm. Xem `docs/BLOCKERS-PA.md`, `docs/CONTRACTS.md` và `docs/SECURITY_SPEC.md` để biết các giới hạn, hợp đồng và quyền truy cập hiện tại.

## Tài liệu dự án

- `AGENTS.md`: quy tắc phát triển và cấu trúc code.
- `docs/ENTITY_SPEC.md`: schema và quan hệ Entity thống nhất.
- `docs/SECURITY_SPEC.md`: cơ chế xác thực, quyền và URL.
- `docs/CONTRACTS.md`: hợp đồng service, event và API giữa các module.
- `docs/BLOCKERS-PA.md`: các phần đang chờ schema hoặc tích hợp.
