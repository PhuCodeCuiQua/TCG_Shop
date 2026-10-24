# SECURITY SPEC: ĐẶC TẢ BẢO MẬT VÀ QUY ƯỚC URL (đặt tại `docs/SECURITY_SPEC.md`)

> Cả nhóm phải đặt URL theo bản đồ ở mục 2. Nếu cần URL mới ngoài bản đồ, nhắn Phú để cập nhật `SecurityConfig` và file này.
> AI phải đọc file này cùng `AGENTS.md` và `docs/ENTITY_SPEC.md` khi làm việc liên quan đến bảo mật hoặc đặt URL.

---

## 1. CƠ CHẾ XÁC THỰC

**Stateless, JWT lưu trong HttpOnly cookie** (không dùng HttpSession, không lưu token ở localStorage).

| | Access token | Refresh token |
|---|---|---|
| Dạng | JWT ký HS256 | Chuỗi ngẫu nhiên không rõ nghĩa (opaque), 32 byte từ `SecureRandom`, mã hóa base64url |
| Tên cookie | `ACCESS_TOKEN` | `REFRESH_TOKEN` |
| Thời hạn | 30 phút | 7 ngày |
| Lưu ở server | Không | Lưu **SHA-256 hex** trong `refresh_tokens.token_hash` |
| Cờ cookie | `HttpOnly`, `SameSite=Lax`, `Secure` theo cấu hình, `Path=/` | giống access token |

- **Claims của access token:** `sub` = email, `uid` = id user, `roles` = danh sách tên role (ví dụ `["ROLE_USER","ROLE_VENDOR"]`), `iat`, `exp`. Không đưa dữ liệu nhạy cảm vào token.
- **Xoay vòng refresh token:** mỗi lần dùng refresh token thì thu hồi token cũ và cấp token mới. Dùng lại token đã thu hồi là dấu hiệu bị đánh cắp: thu hồi toàn bộ refresh token của user đó.
- **Mỗi request** `JwtAuthenticationFilter` đọc token, kiểm tra chữ ký/hạn, rồi **nạp lại user từ DB** để trạng thái khóa/chặn có hiệu lực ngay (đánh đổi: thêm một truy vấn mỗi request, chấp nhận được với đồ án).
- Cho phép REST/AJAX gửi `Authorization: Bearer <token>` thay cho cookie.
- JWT secret lấy từ cấu hình/biến môi trường, tối thiểu 32 byte (base64). Ứng dụng **không khởi động** nếu secret thiếu hoặc còn giá trị mẫu `CHANGE_ME`.

---

## 2. BẢN ĐỒ URL VÀ QUYỀN

Quy tắc khớp: các đường dẫn công khai chỉ áp dụng cho **GET** (trừ `/auth/**` và MoMo). Mọi request còn lại phải xác thực (`anyRequest().authenticated()`).

| Nhóm | URL web | URL API | Quyền |
|---|---|---|---|
| **Công khai** | `/`, `/products/**`, `/cards/**`, `/shops/**`, `/games/**`, `/series/**`, `/search` (chỉ GET) | `GET /api/products/**`, `GET /api/cards/**` (gồm biểu đồ giá), `GET /api/shops/**`, `GET /api/games/**`, `GET /api/series/**` | Ai cũng truy cập |
| **Xác thực** | `/auth/**` (đăng ký, đăng nhập, OTP, quên mật khẩu, đăng xuất, refresh) | | Công khai, mọi method |
| **Thanh toán MoMo** | `/payment/momo/return` (GET), `/payment/momo/ipn` (POST) | | Công khai; IPN bỏ CSRF và kiểm tra chữ ký ở service |
| **Hạ tầng** | `/error`, `/css/**`, `/js/**`, `/img/**`, `/lib/**`, `/favicon.ico`, `/ws/**` (chỉ bước handshake HTTP) | | Công khai; WebSocket được xác thực ở interceptor STOMP |
| **Mọi tài khoản đã đăng nhập** | `/profile/**`, `/notifications/**` | `/api/profile/**`, `/api/notifications/**` | `authenticated()` |
| **User** | `/cart/**`, `/checkout/**`, `/orders/**`, `/wishlist/**`, `/viewed/**`, `/check-my-card/**`, `/seller-registration/**` | `/api/cart/**`, `/api/checkout/**`, `/api/wishlist/**`, `/api/reviews/**`, `/api/price-alerts/**` | `ROLE_USER` |
| **Vendor** | `/vendor/**` | `/api/vendor/**` | `ROLE_VENDOR` |
| **Shipper** | `/shipper/**` | `/api/shipper/**` | `ROLE_SHIPPER` |
| **Manager (và Admin)** | `/manager/**` | `/api/manager/**` | `ROLE_MANAGER` hoặc `ROLE_ADMIN` |
| **Admin** | `/admin/**` | `/api/admin/**` | `ROLE_ADMIN` |

**Quy ước kèm theo:**
- **Vendor luôn giữ cả `ROLE_USER` và `ROLE_VENDOR`** (khi duyệt shop thì *thêm* role, không thay role). Nhờ vậy Vendor dùng được giỏ hàng, đơn mua... mà không cần cấu hình role hierarchy.
- Manager, Admin, Shipper chỉ có role của riêng mình.
- Trang dùng chung giữa Manager và Admin (quản lý user, duyệt shop, catalog, giá, sản phẩm theo shop, phân công shipper, khiếu nại) đặt dưới `/manager/**`. Chức năng chỉ Admin (tạo Manager, chiết khấu, mã/khuyến mãi sàn, nhà vận chuyển, dashboard toàn hệ thống) đặt dưới `/admin/**`.
- Ngoài phân quyền URL, mọi Service vẫn phải kiểm tra **quyền sở hữu dữ liệu** (ownership) và dùng `@PreAuthorize("hasRole('...')")` cho các phương thức nhạy cảm.
- Trang thử tạm đặt dưới `/dev/**` chỉ dùng khi phát triển và **phải xóa trước khi merge**.
- **Thư mục tài nguyên tĩnh của thư viện/template là `static/lib/`, không dùng `static/vendor/`** vì `/vendor/**` là khu vực của Vendor.

---

## 3. URL XÁC THỰC

| URL | Method | Mục đích |
|---|---|---|
| `/auth/login` | GET, POST | Đăng nhập |
| `/profile/change-password` | GET, POST | Đổi mật khẩu bắt buộc lần đầu hoặc đổi mật khẩu trong hồ sơ (PU-10); cần đăng nhập |
| `/auth/register` | GET, POST | Đăng ký |
| `/auth/verify-otp` | GET, POST | Nhập OTP kích hoạt |
| `/auth/resend-otp` | POST | Gửi lại OTP |
| `/auth/forgot-password` | GET, POST | Nhập email để nhận OTP |
| `/auth/reset-password` | GET, POST | Nhập OTP và mật khẩu mới |
| `/auth/logout` | POST | Đăng xuất (công khai, vẫn chạy được khi token đã hết hạn) |
| `/auth/refresh` | POST | Làm mới access token bằng refresh token |

Tên trường form: `email`, `password`, `confirmPassword`, `fullName`, `phoneNumber`, `otp`, `newPassword`.

---

## 4. CHUYỂN HƯỚNG SAU ĐĂNG NHẬP

| Role (ưu tiên từ trên xuống) | Trang đích |
|---|---|
| `ROLE_ADMIN`, `ROLE_MANAGER` | `/manager/users` |
| `ROLE_SHIPPER` | `/shipper/dashboard` |
| `ROLE_VENDOR` | `/vendor/dashboard` |
| `ROLE_USER` | `/` |

Nếu có tham số `returnUrl` hợp lệ (chỉ đường dẫn nội bộ bắt đầu bằng `/`, không chứa `//` hay `http`) thì ưu tiên chuyển về đó. Tài khoản `must_change_password = true` bị chuyển tới trang đổi mật khẩu trước.

---

## 5. KHI CHƯA ĐĂNG NHẬP HOẶC KHÔNG ĐỦ QUYỀN

| Tình huống | Request web | Request `/api/**` |
|---|---|---|
| Chưa đăng nhập | Chuyển tới `/auth/login?returnUrl=<đường dẫn đã mã hóa>` | `401` JSON |
| Đăng nhập nhưng không đủ quyền | Trang `error/403` | `403` JSON |
| Token sai chữ ký/hết hạn | Coi như chưa đăng nhập; nếu còn refresh token hợp lệ thì tự làm mới | `401` JSON |

JSON lỗi theo cấu trúc chung: `{ timestamp, status, error, message, path }`.

Trạng thái tài khoản: `ACTIVE` mới đăng nhập được; `INACTIVE` (chưa nhập OTP) thì chuyển tới trang nhập OTP; `BLOCKED` hoặc `locked_until` còn hiệu lực thì báo tài khoản bị khóa.

---

## 6. CSRF

- Bật CSRF cho toàn bộ form và request thay đổi dữ liệu, dùng `CookieCsrfTokenRepository` (vì ứng dụng stateless). Form Thymeleaf dùng `th:action` sẽ tự chèn token.
- **Layout bắt buộc có hai thẻ meta** trong `<head>` để JavaScript lấy token:
  `<meta name="_csrf" th:content="${_csrf.token}">` và `<meta name="_csrf_header" th:content="${_csrf.headerName}">`.
  `static/js/common.js` đọc hai thẻ này và gắn header cho mọi `fetch` POST/PUT/PATCH/DELETE.
- Bỏ CSRF chỉ cho: `/payment/momo/ipn` (kiểm tra chữ ký thay thế) và `/ws/**`.

---

## 7. WEBSOCKET

- `/ws/**` mở ở tầng HTTP để handshake; xác thực thật ở interceptor STOMP (Phát, PA-03): đọc cookie `ACCESS_TOKEN` bằng `JwtProvider`.
- Chưa đăng nhập: chỉ được nghe topic công khai (`/topic/price-update/**`). Kênh `/user/queue/**` yêu cầu đã xác thực.

---

## 8. CẤU HÌNH

`application.properties` (không chứa bí mật):
```properties
app.jwt.access-expiration-minutes=30
app.jwt.refresh-expiration-days=7
app.jwt.access-cookie-name=ACCESS_TOKEN
app.jwt.refresh-cookie-name=REFRESH_TOKEN
app.cookie.secure=false
```

`application-local.properties` (không commit) và `application-example.properties` (giá trị giả):
```properties
app.jwt.secret=CHANGE_ME_BASE64_AT_LEAST_32_BYTES
```

Sinh secret trên PowerShell:
```powershell
$b = New-Object byte[] 32; [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)
```
