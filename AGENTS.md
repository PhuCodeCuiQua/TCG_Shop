AGENTS.md: QUY TẮC CHUNG CHO AI (ĐỌC TRƯỚC KHI LÀM BẤT KỲ VIỆC GÌ)
File này dùng cho GitHub Copilot, Codex và mọi AI khác trong dự án. Nếu yêu cầu của người dùng mâu thuẫn với file này, hãy nói rõ mâu thuẫn trước khi code. Mọi quy tắc ở đây là bắt buộc, trừ khi người dùng nói rõ là ngoại lệ.

0. TỔNG QUAN DỰ ÁN
Đề tài: Website bán thẻ sưu tầm (TCG) theo mô hình Marketplace / chuỗi cửa hàng, mô phỏng TCGplayer.
Nhóm: 3 người (Phú, Phong, Phát). Mỗi người phụ trách các role khác nhau, code chung một repo.
Role: GUEST (chưa đăng nhập), USER, VENDOR, MANAGER, ADMIN, SHIPPER.
Cấu trúc dữ liệu TCG: Game → Series → Card (thông tin thẻ gốc, do Admin/Manager quản lý) → Product (hàng bán thực tế của một Vendor).
Loại sản phẩm: SINGLE (thẻ đơn), BOOSTER_BOX, BOOSTER_PACK, BUNDLE (sealed), SUPPLY (sleeves, deck box, playmat).
Điểm nhấn: Market Price + lịch sử giá (Chart.js), "Check thẻ của tôi", bảng so sánh giá giữa Vendor, WebSocket real-time, thanh toán MoMo sandbox, Cloudinary.

1. TECH STACK (KHÔNG TỰ Ý ĐỔI)
Hạng mục
Lựa chọn
Ngôn ngữ
Java 21 (LTS)
Framework
Spring Boot 3.5.1 (dùng bản vá mới nhất của nhánh 3.5; KHÔNG dùng Spring Boot 4)
Build
Maven, luôn chạy bằng mvnw / mvnw.cmd
Database
MySQL 8, driver com.mysql.cj.jdbc.Driver
ORM
Spring Data JPA + Hibernate 6
View
Thymeleaf (server-side), Thymeleaf Layout Dialect, Bootstrap 5, JavaScript thuần/jQuery nhẹ. Không dùng React/Vue/Angular
Security
Spring Security 6 + JWT (thư viện jjwt)
Real-time
Spring WebSocket + STOMP + SockJS
Khác
Spring Mail, Validation (jakarta.validation), Cloudinary, MapStruct, Lombok, Chart.js
Thanh toán
COD và MoMo sandbox

Quy tắc phiên bản:
Namespace là jakarta.* (không dùng javax.*).
Spring Security 6: dùng bean SecurityFilterChain với lambda DSL. Không dùng WebSecurityConfigurerAdapter, antMatchers, authorizeRequests (đã bị loại bỏ).
Dùng requestMatchers(...), authorizeHttpRequests(...).
Không tự thêm hoặc nâng/hạ phiên bản dependency trong pom.xml. Cần thêm thì nói rõ lý do và hỏi trước.
Không chắc về một API thì nói "không chắc" và chỉ ra chỗ cần kiểm tra, không đoán.
Có thể dùng cú pháp Java 21 (record, switch expression, pattern matching, text block) khi phù hợp. Entity JPA không được là record.
Lombok + MapStruct: thứ tự annotationProcessorPaths trong maven-compiler-plugin phải là: lombok, lombok-mapstruct-binding, mapstruct-processor. Sai thứ tự thì mapper không được sinh.

2. CẤU TRÚC PACKAGE (BẮT BUỘC)
Base package: com.tcgshop. Mọi class mới phải nằm đúng chỗ.
com.tcgshop
 ├── config/            # SecurityConfig, WebSocketConfig, CloudinaryConfig, MailConfig, AsyncConfig, MomoConfig...
 ├── controller/
 │    ├── web/          # Trả về View Thymeleaf. Tên kết thúc bằng `Controller`
 │    └── rest/         # Trả về JSON. Tên kết thúc bằng `RestController`
 ├── dto/
 │    ├── request/      # Kết thúc bằng `Request`
 │    └── response/     # Kết thúc bằng `Response`
 ├── entity/            # Danh từ số ít (User, Product...)
 │    └── enums/        # OrderStatus, ProductType, Condition, Rarity, RoleName...
 ├── exception/         # Custom exception + GlobalExceptionHandler
 ├── mapper/            # MapStruct, kết thúc bằng `Mapper`
 ├── repository/        # Kết thúc bằng `Repository`
 ├── security/          # JwtProvider, JwtAuthenticationFilter, UserDetailsServiceImpl, CurrentUser helper...
 ├── service/           # Interface
 │    └── impl/         # Kết thúc bằng `ServiceImpl`
 ├── scheduler/         # Các job @Scheduled (tính Market Price, dọn OTP hết hạn...)
 ├── event/             # Sự kiện ứng dụng (record bất biến), dùng ApplicationEventPublisher
 ├── listener/          # Các @TransactionalEventListener (AFTER_COMMIT)
 ├── validation/        # Custom validator (StrongPassword, MinCommentLength...)
 └── util/              # Hàm tiện ích dùng chung

Template Thymeleaf đặt tại src/main/resources/templates/, chia theo role: templates/{layout, fragments, auth, guest, user, vendor, manager, admin, shipper, error}/. File tĩnh: static/{css, js, img, lib}/. JavaScript tách riêng vào static/js/, không viết logic dài trong HTML.

3. LUỒNG DỮ LIỆU & PHÂN TẦNG
Luồng duy nhất: Controller → Service → Repository.
Controller chỉ: nhận request, validate, gọi Service, trả View/JSON. Không chứa nghiệp vụ, không gọi Repository trực tiếp.
Service chứa toàn bộ nghiệp vụ, đặt @Transactional ở đây (readOnly = true cho hàm chỉ đọc).
Repository chỉ truy vấn dữ liệu, không chứa nghiệp vụ.
TUYỆT ĐỐI KHÔNG trả Entity ra View hoặc JSON. Luôn map sang DTO bằng MapStruct.
Service giao tiếp với Controller bằng DTO (hoặc kiểu đơn giản), không để Entity lọt ra ngoài tầng Service.
Inject dependency bằng constructor: @RequiredArgsConstructor + private final. Không dùng @Autowired trên field.
Service gọi Service khác được; tránh vòng phụ thuộc (A gọi B, B gọi A). Gặp vòng thì tách logic chung ra service thứ ba.

4. QUY ƯỚC ĐẶT TÊN
Đối tượng
Quy tắc
Ví dụ
Class, interface, enum
PascalCase
ProductServiceImpl
Biến, phương thức
camelCase
findByShopId
Hằng số
UPPER_SNAKE_CASE
MAX_OTP_ATTEMPTS
Bảng DB
snake_case, số nhiều
order_items
Cột DB
snake_case
created_at
URL web
kebab-case
/vendor/my-products
REST
/api/..., danh từ số nhiều
/api/products/{id}

Tên biến, class, comment kỹ thuật bằng tiếng Anh; giải thích và comment nghiệp vụ phức tạp bằng tiếng Việt.

5. ENTITY & JPA
Dùng @Getter @Setter @NoArgsConstructor (+ @Builder nếu cần). Không dùng @Data trên Entity (gây lỗi equals/hashCode/toString với quan hệ lazy).
Mọi Entity kế thừa BaseEntity có id, createdAt, updatedAt (dùng @MappedSuperclass + auditing).
Khóa chính Long, @GeneratedValue(strategy = GenerationType.IDENTITY).
Quan hệ @ManyToOne / @OneToMany luôn fetch = LAZY. Tránh N+1: dùng @EntityGraph hoặc JOIN FETCH.
Trạng thái, loại: dùng enum + @Enumerated(EnumType.STRING). Không dùng số hoặc chuỗi tự do.
Tiền tệ dùng BigDecimal (cột DECIMAL(15,2)), không dùng double/float.
Tồn kho và đơn hàng dùng @Version (optimistic lock) để tránh bán vượt tồn kho khi mua đồng thời.
Xóa mềm (soft delete) cho dữ liệu có liên quan đơn hàng (product, shop, user): dùng cờ deleted hoặc status, không xóa cứng.
spring.jpa.open-in-view=false. Không bật ddl-auto=create trên dữ liệu thật; dùng update khi phát triển, ghi rõ trong README.
Thay đổi Entity dùng chung (User, Product, Order, ...) chỉ do Phú thực hiện. Người khác cần thêm cột/bảng thì báo Phú, hoặc ghi rõ trong PR để Phú duyệt.

6. DTO & MAPSTRUCT
DTO nhập: kết thúc bằng Request; DTO xuất: kết thúc bằng Response. Có thể dùng record cho DTO.
DTO dùng cho form Thymeleaf (`th:field`) phải là class có `@Getter @Setter @NoArgsConstructor`, không dùng `record`, vì `th:field` cần getter/setter kiểu JavaBean. `record` chỉ dùng cho DTO Response và Request của REST.
Mapper: @Mapper(componentModel = "spring"). Mapping tên khác nhau phải khai báo @Mapping.
Không đưa mật khẩu, OTP, token, hash vào bất kỳ DTO Response nào.
Form Thymeleaf dùng DTO Request riêng, không bind thẳng vào Entity.

7. VALIDATION & XỬ LÝ LỖI
Validate bằng annotation jakarta.validation trên DTO Request: @NotNull, @NotBlank, @Size, @Min, @DecimalMin, @Email, @Pattern...
Bắt buộc có validate: giá > 0, số lượng > 0, email hợp lệ, mật khẩu mạnh (≥ 8 ký tự, có chữ hoa, chữ thường, số), bình luận ≥ 50 ký tự, số điện thoại VN hợp lệ.
Controller Web: @Valid + BindingResult; lỗi trả lại form và hiển thị bằng th:errors; giữ lại dữ liệu người dùng đã nhập.
Controller Rest: @Valid, lỗi do GlobalExceptionHandler xử lý.
Validate ở cả hai tầng: HTML5/Bootstrap phía client và server. Server luôn là nơi quyết định.
Không dùng try-catch tràn lan trong Controller. Ném exception tùy chỉnh và xử lý tập trung ở @ControllerAdvice: ResourceNotFoundException (404), BadRequestException (400), ForbiddenException (403), ConflictException (409), BusinessException (400/422, lỗi nghiệp vụ).
Lỗi REST trả về cấu trúc thống nhất: { timestamp, status, error, message, path, fieldErrors? }.
Trang lỗi riêng cho 403 / 404 / 500 trong templates/error/.
Không để lộ stack trace hoặc thông tin nội bộ ra người dùng. Ghi log bằng SLF4J (@Slf4j), không dùng System.out.println.

8. BẢO MẬT (SECURITY)
Xác thực
Mật khẩu mã hóa bằng BCryptPasswordEncoder. Không lưu, không log mật khẩu thô.
JWT lưu trong HttpOnly cookie (HttpOnly, SameSite=Lax, Secure khi chạy HTTPS). Không lưu token trong localStorage.
JwtAuthenticationFilter đọc token từ cookie, hoặc từ header Authorization: Bearer ... cho REST/AJAX.
Access token ngắn hạn (15-30 phút) + refresh token; đăng xuất phải vô hiệu hóa refresh token (xóa/blacklist) và xóa cookie.
Bật CSRF cho form Thymeleaf (Thymeleaf tự thêm token vào th:action). Chỉ tắt CSRF cho endpoint IPN của MoMo và chỉ khi đã kiểm tra chữ ký.
JWT secret, key API lấy từ biến môi trường, không hard-code, không commit.
OTP (đăng ký, quên mật khẩu)
6 chữ số, sinh bằng SecureRandom, hết hạn sau 5 phút.
Lưu dạng hash, không lưu OTP thô. Tối đa 5 lần nhập sai (sau đó phải xin OTP mới). Gửi lại OTP cách nhau tối thiểu 60 giây.
OTP dùng một lần, dùng xong phải vô hiệu hóa.
Phân quyền hai lớp (cả hai đều bắt buộc)
Theo role: @PreAuthorize("hasRole('VENDOR')") hoặc trong SecurityFilterChain (@EnableMethodSecurity). Role lưu dạng ROLE_ADMIN, ROLE_MANAGER, ROLE_VENDOR, ROLE_USER, ROLE_SHIPPER.
Theo quyền sở hữu dữ liệu (ownership): kiểm tra trong Service, không chỉ dựa vào role.
Vendor chỉ xem/sửa sản phẩm, đơn, khuyến mãi của shop mình.
User chỉ xem/sửa đơn, địa chỉ, giỏ hàng, wishlist của chính mình.
Shipper chỉ xem/cập nhật đơn được phân công cho mình.
Không lấy userId / shopId từ request client; luôn lấy từ người đang đăng nhập (CurrentUser/SecurityContext). Vi phạm thì ném ForbiddenException.
Khác: chống SQL injection (chỉ dùng JPA/tham số hóa), chống XSS (dùng th:text, hạn chế th:utext), giới hạn số lần đăng nhập sai nếu có thể.

9. QUY TẮC NGHIỆP VỤ CỐT LÕI
Tính tiền (luôn tính ở Service, không tin số liệu từ client) Tổng = Σ(giá hiện tại của sản phẩm × số lượng) − khuyến mãi shop − mã giảm giá + phí vận chuyển (đã trừ giảm phí ship). Giá lấy từ DB tại thời điểm đặt hàng và lưu snapshot giá vào order_items.
Đơn hàng
Giỏ hàng có sản phẩm của nhiều shop: khi thanh toán tách thành đơn con theo từng shop, cùng một lần thanh toán.
Trạng thái (OrderStatus): NEW → CONFIRMED → PICKED_UP → SHIPPING → DELIVERED; nhánh hủy: NEW/CONFIRMED → CANCELLED; giao thất bại: SHIPPING → DELIVERY_FAILED; trả hàng: DELIVERED → RETURN_REQUESTED → RETURN_APPROVED/RETURN_REJECTED → RETURNED → REFUNDED.
Chỉ chuyển trạng thái qua một phương thức duy nhất trong OrderService (state machine). Không setStatus(...) rải rác. Mỗi lần chuyển phải: kiểm tra chuyển hợp lệ, kiểm tra ai được phép chuyển, ghi bản ghi vào order_status_histories, và gửi thông báo.
Chống đặt đơn trùng khi bấm hai lần; trừ tồn kho an toàn khi đặt, hoàn tồn kho khi hủy/trả.
Chỉ cho hủy ở trạng thái hợp lệ (NEW, CONFIRMED).
Chiết khấu & doanh thu: khi đơn DELIVERED, tự tính hoa hồng sàn theo tỉ lệ cấu hình (theo shop, mặc định toàn sàn) và doanh thu thực nhận của shop.
Mã giảm giá: có điều kiện đơn tối thiểu, mức giảm tối đa, hạn dùng, tổng số lượt, số lần mỗi user được dùng; có loại của sàn (Admin) và của shop (Vendor). Kiểm tra lại toàn bộ điều kiện ở bước đặt hàng.
Đánh giá & bình luận: chỉ cho người đã mua và đơn đã giao; mỗi sản phẩm mỗi đơn đánh giá một lần; bình luận ≥ 50 ký tự; kèm ảnh/video tùy chọn.
Market Price & lịch sử giá: price_histories(card_id, date, market_price, avg_price). Job @Scheduled hằng đêm tính trung bình giá bán thực tế 30 ngày; Admin/Manager có thể nhập tay. Khi giá cập nhật, đẩy thông báo WebSocket.

10. UPLOAD FILE (CLOUDINARY)
Chỉ upload qua CloudinaryService duy nhất; không gọi Cloudinary trực tiếp từ Controller.
Ảnh: chỉ jpg, jpeg, png, dung lượng < 5MB. Video (bình luận): chỉ mp4, webm, dung lượng ≤ 50MB, tối đa số file cho mỗi bình luận theo cấu hình.
Kiểm tra MIME type thật (đọc nội dung file), không chỉ dựa vào đuôi file hay Content-Type do client gửi.
Kết quả trả về: url và public_id; lưu cả hai vào DB. Khi sửa/xóa phải xóa ảnh cũ trên Cloudinary theo public_id.
Lỗi upload ném exception tùy chỉnh, không để lộ lỗi nội bộ.

11. EMAIL (SPRING MAIL)
Gửi mail (OTP, xác nhận đơn, thông báo giá) bất đồng bộ bằng @Async. Cần @EnableAsync và ThreadPoolTaskExecutor trong AsyncConfig.
@Async không có tác dụng nếu gọi method trong cùng class; đặt trong bean riêng (EmailService).
Mail gửi sau khi transaction commit (@TransactionalEventListener(phase = AFTER_COMMIT) hoặc tương đương), lỗi gửi mail chỉ log, không làm hỏng giao dịch chính.

12. WEBSOCKET (STOMP)
Endpoint: /ws (có SockJS). Prefix gửi từ client: /app. Broker: /topic, /queue; prefix user: /user.
Topic công khai: /topic/price-update (hoặc /topic/price-update/{cardId}), dùng cho biến động giá.
Kênh riêng theo user: /user/queue/notifications, /user/queue/order-status (dùng SimpMessagingTemplate.convertAndSendToUser). Không phát thông tin đơn hàng qua topic chung.
Mỗi thông báo ghi vào bảng notifications (có cờ read) rồi mới đẩy real-time.
Xác thực WebSocket bằng JWT khi handshake/CONNECT; không cho client chưa đăng nhập đăng ký kênh riêng.
Comment rõ trong WebSocketConfig và interceptor xác thực.

13. GIAO DIỆN THYMELEAF + BOOTSTRAP
Dùng một template Bootstrap 5 đã chọn cho toàn dự án; không trộn nhiều template/CSS framework.
Layout chung bằng Thymeleaf Layout Dialect hoặc fragment: layout/main, layout/dashboard (sidebar theo role), fragments/header|footer|toast|pagination.
Mọi trang phải responsive (mobile, tablet, desktop), có trạng thái rỗng (empty state), thông báo thành công/lỗi (toast/alert) và trạng thái loading khi gọi AJAX.
Dùng Post/Redirect/Get; thông báo flash qua RedirectAttributes.
Form có th:action, th:object, th:field, th:errors. Có phân trang cho mọi danh sách dài; giữ tham số lọc/sắp xếp trên URL.
Trang chi tiết Single Card: ảnh thẻ bên trái; thông tin (giá, tình trạng, nút Add to Cart) bên phải; bên dưới là biểu đồ giá (Chart.js, đường) và bảng so sánh giá giữa các Vendor.
Dữ liệu cho Chart.js lấy từ API JSON (/api/cards/{id}/price-history?range=30) hoặc truyền JSON từ Controller.
Văn bản hiển thị cho người dùng bằng tiếng Việt, định dạng tiền VND (#numbers.formatDecimal).

14. REST API
Trả ResponseEntity<...> với HTTP status đúng: 200, 201 (tạo mới), 204 (xóa), 400, 401, 403, 404, 409.
Đường dẫn dạng /api/...; phân trang bằng Pageable, trả PageResponse thống nhất.
Không có logic trong RestController, chỉ gọi Service.

15. CẤU HÌNH & BÍ MẬT
Thông tin nhạy cảm (DB password, JWT secret, Cloudinary, MoMo, mail) đọc từ biến môi trường hoặc application-local.properties. File application-local.properties phải nằm trong .gitignore.
Commit kèm application-example.properties (giá trị giả) để người khác biết cần điền gì.
MoMo: kiểm tra chữ ký HMAC-SHA256 ở cả return URL và IPN; chỉ cập nhật trạng thái thanh toán sau khi chữ ký hợp lệ và số tiền khớp đơn hàng; xử lý IPN idempotent (gọi lặp không gây lỗi).
Dữ liệu mẫu (seed) đặt trong data.sql hoặc DataSeeder; có tài khoản mẫu cho mỗi role.

16. GIT & CÁCH PHỐI HỢP
Nhánh: main ← develop ← feature/<ten-chuc-nang>. Không commit trực tiếp vào main/develop.
Commit message: feat:, fix:, docs:, refactor:, test:, chore: + mô tả ngắn. Commit nhỏ, thường xuyên.
Mọi merge vào develop qua Pull Request và có người review.
Không sửa file/chức năng của thành viên khác nếu chưa báo; không sửa Entity dùng chung (xem mục 5).
Sau mỗi chức năng quan trọng: ghi prompt đã dùng vào docs/prompts-log.md.

17. QUY TẮC KHI AI LÀM VIỆC
Phạm vi: chỉ tạo hoặc sửa các file liên quan trực tiếp đến yêu cầu. Không tự sửa Entity dùng chung, không thêm dependency, không xóa hoặc viết lại code có sẵn khi chưa được yêu cầu.
Kiểm tra trước khi tạo: trước khi tạo class mới, tìm trong project xem đã có class/method tương tự chưa để tái sử dụng.
Hỏi lại có giới hạn: chỉ hỏi khi thật sự không đoán được (ví dụ chưa rõ trả View hay JSON); tối đa 3 câu một lần. Với chi tiết nhỏ, tự chọn phương án hợp lý theo file này và ghi rõ giả định.
Code đầy đủ: luôn có đủ import; code phải biên dịch được; không để TODO rỗng thay cho logic chính.
Giải thích ngắn: mỗi đoạn code kèm 1-2 câu lý do; comment tiếng Việt cho logic phức tạp (JWT Filter, WebSocket, Cloudinary, state machine đơn hàng, tính tiền, OTP).
Đủ bộ khi làm một chức năng: DTO (Request/Response) + validation, Mapper, Repository, Service interface + Impl, Controller (web hoặc rest), template (nếu có giao diện), kiểm tra phân quyền và ownership. Chỉ sinh phần được yêu cầu, không sinh lan sang chức năng khác.
Test: viết unit test cho logic quan trọng (tính tiền, state machine, OTP, kiểm tra ownership) khi được yêu cầu hoặc khi thêm logic mới loại này.
Tổng kết cuối mỗi lần: liệt kê (a) các file đã tạo/sửa, (b) cách chạy thử, (c) giả định đã đặt ra hoặc việc còn dang dở.
Ngôn ngữ: trả lời bằng tiếng Việt; tên class, biến, method bằng tiếng Anh.
Trung thực: nếu không chắc một API/phiên bản/cấu hình, nói rõ là không chắc, không bịa. Nếu code không chạy được, nói rõ thay vì giả vờ đã chạy.

18. DANH SÁCH "TUYỆT ĐỐI KHÔNG" (KIỂM TRA LẠI TRƯỚC KHI TRẢ KẾT QUẢ)
[ ] Không gọi Repository trong Controller.
[ ] Không trả Entity ra View hoặc JSON.
[ ] Không dùng @Data trên Entity; không dùng double cho tiền.
[ ] Không lưu mật khẩu/OTP thô; không hard-code key hay secret.
[ ] Không tin userId, shopId, price, total do client gửi lên.
[ ] Không bỏ qua kiểm tra ownership khi sửa/xóa/xem dữ liệu riêng.
[ ] Không setStatus đơn hàng ngoài state machine.
[ ] Không dùng javax.*, WebSecurityConfigurerAdapter, antMatchers.
[ ] Không dùng localStorage để lưu token.
[ ] Không thêm dependency hoặc đổi phiên bản khi chưa hỏi.
[ ] Không try-catch nuốt lỗi trong Controller.

19. CẬP NHẬT FILE NÀY
Ai muốn đổi quy tắc phải báo cả nhóm, sửa trong một PR riêng và nhắn thông báo. Sau khi sửa, chép lại nội dung sang .github/copilot-instructions.md để Copilot và Codex luôn dùng cùng một phiên bản.
