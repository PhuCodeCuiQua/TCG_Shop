
# Prompt Log

## Ẩn chatbot khi đăng xuất và với Guest

Chỉ render widget cho tài khoản đã xác thực, ẩn widget mặc định và không khôi phục lịch sử cho khóa Guest. Khi submit form đăng xuất, ẩn widget và xóa nội dung chat đang hiển thị trước khi tiếp tục gửi form; giữ lịch sử localStorage riêng theo tài khoản để có thể khôi phục khi chính tài khoản đó đăng nhập lại. Ẩn thêm tại trang đăng nhập/đăng ký; không sửa Entity, SecurityConfig hoặc pom.xml; không chạy test/build, không commit/push.

## Sửa lỗi thuộc tính phân trang PageResponse

Khắc phục lỗi trang Series khi fragment phân trang gọi `page.number` trên `PageResponse`, vốn lưu số trang ở trường `page`. Bổ sung accessor `getNumber()` tương thích với giao diện phân trang của Spring và dùng accessor này thống nhất trong fragment để hỗ trợ cả `PageResponse` lẫn `Page`. Không chạy test/build, không commit/push.

## Đồng bộ giao diện Admin và Manager theo Midnight Rare

Đọc AGENTS.md và toàn bộ docs/. Gắn marker giao diện chỉ cho ROLE_ADMIN và ROLE_MANAGER trong layout dashboard dùng chung, giữ nguyên Vendor/Shipper. Trong stylesheet Admin/Manager áp dụng panel/form/control/bảng tối, tiêu đề và nội dung tương phản, bảng cuộn responsive theo cấu trúc sẵn có, viền và bóng cho stat/chart cards. Giữ nguyên nghiệp vụ, binding, route, SecurityConfig và pom.xml; tái sử dụng bảng màu Midnight Rare vốn có trong system-dashboard.js. Không commit/push.

## Đồng bộ giao diện Vendor Khuyến mãi và Mã giảm giá theo Midnight Rare

Đọc AGENTS.md và toàn bộ docs/. Cập nhật bốn template Vendor promotion/coupon: form dùng hai panel Bootstrap tỷ lệ 7/5, navy và input tối, unit addon, nút cuối form; danh sách dùng panel navy có tổng số/thời gian địa phương, bảng cuộn ngang, chữ sáng, badge đang chạy xanh mờ và action tròn có icon. Giữ nguyên các field/binding, route, xử lý trạng thái và POST/CSRF. Form khuyến mãi hiện không có code, discount cap, quota hay field status; không tự thêm nghiệp vụ, thay bằng phần chọn sản phẩm và hướng dẫn bật/tắt qua danh sách. Không sửa Controller/Service/Entity/SecurityConfig/pom.xml; không commit/push.

## Căn chỉnh form khuyến mãi và mã hệ thống

Đọc AGENTS.md và toàn bộ docs/. Căn lại hai form Bootstrap với `row g-3 mb-4`, nút cuối form căn phải; thêm placeholder cho giá trị nhập, suffix đơn vị tiền/phần trăm, và CSS giới hạn riêng cho viền xám cùng focus tím. Select nhiều sản phẩm giữ nguyên dữ liệu và binding, tăng chiều cao/độ dễ đọc, dùng nhãn văn bản thuần không có ký tự phân cách kiểu trang trí. Không thay đổi logic, Entity, SecurityConfig, pom.xml; không commit/push.

## Sửa form và áp dụng mã giảm giá hệ thống

Đọc AGENTS.md và toàn bộ docs/. Form mã hệ thống đã có lưới Bootstrap hai cột; làm rõ hướng dẫn giá cố định/phần trăm, giới hạn, điều kiện subtotal và ngày hiệu lực, căn nút thao tác về cuối form. Kiểm tra luồng CouponService → CheckoutService → OrderPlacement: mã sàn được tra với shopId=null, min_order_value so với tổng tiền hàng trước mã chi nhánh, mức giảm được giới hạn vào số tiền còn lại và chia theo tỷ lệ. Không phát hiện sai logic trong luồng này; thêm log debug SLF4J cho lý do từ chối và test hồi quy scope/minimum. Không dùng System.out.println vì quy tắc dự án cấm; không sửa Entity, SecurityConfig, pom.xml, không commit/push.

## Sửa tương phản danh sách thông báo

Đọc AGENTS.md và toàn bộ docs/. `/notifications` dựng item bằng notifications-page.js; rule nền chung đặt màu chữ trắng trực tiếp lên liên kết và đoạn văn. Thêm CSS override có phạm vi `#allNotificationsList` để card trắng có chữ tối cho tiêu đề/nội dung/thời gian, viền và bóng nhẹ; giữ badge “Đã đọc” xám/chữ tối và “Chưa đọc” chữ trắng. Không thay đổi logic thông báo, Entity, SecurityConfig hoặc pom.xml; không commit/push.

## Tạo khoảng cách giữa lưới sản phẩm và phân trang

Thêm khoảng nghỉ 40px phía trên phân trang dùng chung và phân trang catalog, căn giữa điều hướng; thêm 30px padding dưới lưới sản phẩm catalog và trang chi nhánh. Trên mobile giảm padding lưới còn 14px và margin phân trang còn 24px để tránh khoảng trống quá lớn. Không thêm padding vào trang chủ vì hiện trang này không có phân trang; không sửa Entity, SecurityConfig, pom.xml và không commit/push.

## Khắc phục badge loại sản phẩm bị chìm trên ảnh

Đọc AGENTS.md và toàn bộ docs/. Xác định `.catalog-product-type` và `.shop-product-type` là nhãn phủ ảnh; CSS toàn cục đặt lại màu `span` bên trong card nên badge bị chữ tối. Thêm override ưu tiên cao có `!important` trong `global-background.css`, giới hạn vào card sản phẩm công khai: nền navy đặc, chữ trắng đậm, viền vàng, blur, opacity đầy đủ và text-shadow. Không áp dụng vào cột loại sản phẩm của bảng Vendor. Không sửa Entity, SecurityConfig, pom.xml; không commit/push.

## Tối ưu phân trang tìm kiếm và làm nổi bật nhãn sản phẩm

Giới hạn phân trang riêng trên trang tìm kiếm còn tối đa 5 trang lân cận, thêm trang đầu/cuối và dấu ba chấm; giữ đầy đủ bộ lọc trên URL. Dùng Bootstrap pagination và trên mobile chỉ giữ Trước, trang hiện tại, Sau. Đồng bộ badge loại sản phẩm ở trang tìm kiếm/chi nhánh thành nền tối trong suốt, chữ trắng, viền sáng mờ và blur; làm rõ tiêu đề sản phẩm đang bán trên nền tối. Badge tiếp thị trong product-card không đổi vì không biểu thị loại sản phẩm. Không sửa Entity, SecurityConfig, pom.xml; không commit/push.

## Cấu hình hệ thống lưu DB và thương hiệu toàn hệ thống

Theo yêu cầu rõ của người dùng, tạo entity SystemSetting mới và bảng system_settings, giữ nguyên entity khác, SecurityConfig/pom.xml. Triển khai DTO form validation, MapStruct, repository singleton có unique/check và khởi tạo nguyên tử/khóa ghi; Service đọc công khai và chỉ Admin lưu. Controller mới thay route settings trong AdminPlaceholderController, POST multipart + CSRF + PRG. Upload logo qua CloudinaryService, bảo toàn ảnh cũ khi rollback và dọn ảnh đúng thời điểm giao dịch. GlobalSettingsAdvice cung cấp DTO cho Header/Footer/sidebar và tiêu đề layout; mặc định TCGSHOP + SVG khi chưa cấu hình. Form điền sẵn DB, JS chỉ preview/validation/loading, bỏ toast demo và các tùy chọn demo không được lưu; hoa hồng dẫn tới chức năng riêng hiện có. 23 kiểm thử liên quan đạt: H2 chế độ MySQL xác nhận DB một dòng/unique/check; form Admin/CSRF/validation, Header Guest/fallback, lỗi upload giữ dữ liệu, giữ logo cũ khi không upload và dọn Cloudinary theo commit/rollback. Mô phỏng JS xác nhận submit thật, chặn form lỗi và reset về logo đã lưu. Chưa gọi Cloudinary thật. Không commit/push.

## Hoàn thiện giao diện ba trang quản trị Admin

Sau khi người dùng xác nhận danh sách file, thay placeholder settings/roles/logs bằng giao diện Midnight Rare dùng layout/dashboard. Settings hai cột có tên web, logo preview JPG/PNG dưới 5 MB, điện thoại VN, email, tỷ lệ hoa hồng demo 0–100%, tùy chọn hiển thị và nút vàng Lưu thay đổi; chỉ toast "Đã lưu cấu hình thành công (Bản demo)", không gửi/lưu DB. Link hoa hồng dẫn tới trang quản lý thực tế. Roles tĩnh gồm năm role và badge theo phạm vi quyền hiện hành. Logs dùng dữ liệu mẫu ghi rõ demo, tìm kiếm không dấu, phân trang và trạng thái xanh/đỏ/vàng; bảng đều bọc table-responsive. Thêm admin-tools.css, admin-settings.js, admin-logs.js; cập nhật test render/phân quyền cũ. 3 tests render/phân quyền đạt; mô phỏng JS xác nhận toast form hợp lệ, chặn form lỗi, tìm kiếm không dấu, phân trang, empty state và giữ q/page trên URL. Giữ nguyên Controller đã trả đúng view, Entity, SecurityConfig, pom.xml; không commit/push.

## Sửa link đơn hàng cho Shipper

Đọc AGENTS.md và docs/, xác nhận SecurityConfig đã giới hạn /shipper/** và /api/shipper/** cho ROLE_SHIPPER, /orders/** dành cho ROLE_USER. Sửa Header, profile/_nav và link theo dõi đơn hàng Footer: Shipper tới /shipper/orders, User giữ /orders; ưu tiên Shipper nếu có cả hai role. Bổ sung link Đơn hàng/Hồ sơ trên Header cho Shipper không có USER; hồ sơ vẫn dùng /profile chung theo SECURITY_SPEC. Không mở quyền /orders cho Shipper, không sửa Entity, pom.xml hoặc SecurityConfig, không commit/push.

## Đồng bộ topbar dashboard cho các role

Đọc AGENTS.md và tái sử dụng fragment notification-bell cùng API unread-count/danh sách và WebSocket đã được dashboard nạp sẵn; không dùng badge giả. Đổi avatar chấm tròn/email sang ảnh avatar hoặc chữ cái đầu và fullName, fallback username nếu tên rỗng; hỗ trợ principal mặc định khi kiểm thử. Bổ sung dropdown Hồ sơ cá nhân, Đổi mật khẩu và Cài đặt hệ thống chỉ Admin theo route có sẵn; giữ POST logout, hidden CSRF và helper js-logout-form. CSS Midnight Rare sáng/tím trong admin-dashboard.css áp dụng chung, tên dài được rút gọn trên mobile. Sửa fixture ShipperDashboardMvcTest bổ sung mock user và CSRF do test tắt filter; 6 kiểm thử render hiện có đạt trên Admin/Manager/Shipper và fallback username. Không sửa Entity, SecurityConfig, pom.xml; không commit/push.

## Vendor Midnight Rare và gợi ý mã shop ở checkout

Triển khai sau khi người dùng xác nhận danh sách file/cấu trúc. Chỉ chỉnh UI promotions/coupons: chữ sáng trên bảng navy, ngày Từ/Đến, badge xanh/xám/vàng, nút tròn Bootstrap Icons và tooltip; CSS bổ sung có phạm vi trong vendor-shop.css. Thêm GET /api/checkout/coupons theo Controller → CouponSuggestionService → repository riêng, mapper MapStruct và DTO; CurrentUser/CartService cung cấp giỏ, không nhận total/user/shop từ client; CouponService.validateAndCalculate giữ nguyên hợp đồng và kiểm tra mã đủ điều kiện. Checkout có thẻ ticket navy viền nét đứt và nút vàng, danh sách rỗng tự ẩn, giữ nhập mã thủ công và preview server khi áp dụng. Dùng checkout.css riêng, không thêm inline style. Bổ sung test service/API và kiểm tra JS bằng mô phỏng DOM (danh sách rỗng ẩn, render ticket, áp dụng gửi đúng mã tới preview). 34 tests liên quan đạt; build kiểm thử dùng output tạm riêng trong target để tránh xung đột biên dịch IDE. Điều chỉnh stub không dùng trong CouponServiceImplTest và thêm kiểm tra vượt lượt user. Không sửa Entity, SecurityConfig, pom.xml, không commit/push.

## Bỏ giới hạn nhận xét tối thiểu theo yêu cầu mới

Quy tắc hiện hành: nhận xét đánh giá tùy chọn, cho phép 1–2 ký tự hoặc bỏ trống; giữ tối đa 2.000 ký tự và số sao 1–5. Quy tắc này thay thế các mô tả tối thiểu 50 ký tự trong prompt lịch sử và là ngoại lệ được người dùng chỉ định đối với AGENTS.md. Xóa MinCommentLength/Validator và các test quy tắc cũ, bỏ annotation trên cả ReviewSubmitRequest và ReviewMediaRequest, bỏ kiểm tra tại ReviewServiceImpl và JavaScript của cả tạo/sửa; xóa bộ đếm /50. Cập nhật báo cáo và kiểm thử DTO/Service cho nhận xét ngắn, null, rỗng, khoảng trắng.


## Sửa hiển thị rating Shop/người bán và lỗi tải review

Đọc AGENTS.md và toàn bộ docs/, giữ nguyên Entity, SecurityConfig, pom.xml; không commit/push. Sửa PublicShopController mặc định 0 sao và public-shop.html hiển thị Chưa có đánh giá khi count bằng 0/null hoặc thiếu điểm trung bình. Xác nhận CardOfferService đã lấy Shop.avgRating/ratingCount; vendor-offers.js kiểm tra count/điểm hợp lệ và hiển thị điểm trên 5 cùng số lượt. Tái hiện lỗi review bằng MockMvc: Thymeleaf từ chối view specification có tham số vị trí `fragments/reviews :: reviewSection(product, reviews, canReview)`; sửa thành chọn fragment `reviewSection` dùng model hiện có. Giữ endpoint HTML GET /products/{id}/reviews mà reviews.js đang gọi, empty state Chưa có đánh giá nào và điều kiện form chỉ người mua đủ điều kiện DELIVERED. Bổ sung PublicRatingControllerMvcTest, CardOfferServiceImplTest; hoàn thiện assertion thông báo trong ReviewEligibilityScenariosTest để các stub hiện có được dùng. 16 tests liên quan đạt; kiểm tra cú pháp JavaScript đạt. Rating shop đọc từ trường Shop hiện có, không tự suy ra từ rating Product và không bổ sung nghiệp vụ tổng hợp rating shop.

## Kiểm tra GET tạo mới và trạng thái đăng nhập trên trang lỗi

Đọc AGENTS.md và toàn bộ docs/, kiểm tra link GET /admin/platform-coupons/new, form POST, common.js và cấu hình CSRF. GET tạo mới với JWT Admin hợp lệ trả 200; form th:action tự chèn hidden CSRF; helper đã có cookie/meta fallback. Sửa SecurityConfig và JwtAuthenticationFilter để lưu SecurityContext trong request attributes, giữ principal qua ERROR dispatch mà không tạo session hoặc xoay refresh token lần nữa. Thêm hồi quy ERROR dispatch và kiểm tra hidden CSRF trong HTML thực. Không bỏ CSRF cho Admin, không sửa Entity/dependency. Chưa xác định được nguyên nhân 403 ban đầu trên trình duyệt vì chưa có log request lỗi thực tế; không đồng nhất Header anonymous trên trang lỗi với cookie đã bị xóa.

## Tổng rà soát quyền Admin/Manager và kiểm tra Admin đang chạy

Đọc AGENTS.md và toàn bộ docs/. Khôi phục /admin/** và /api/admin/** chỉ ROLE_ADMIN, /manager/** cho cả Admin/Manager; bỏ ngoại lệ marketing trong namespace Admin. Giữ GET/POST tạo promotions/coupons chỉ Admin, URL tạo ở giao diện trỏ tường minh /admin/.../new và sec:authorize Admin; giữ quyền sửa/ngừng Manager qua namespace Manager theo yêu cầu trước. Kiểm tra MySQL chỉ đọc xác nhận admin@tcgshop.local ACTIVE, ROLE_ADMIN, không must-change-password. GET thực tế bằng JWT Admin hợp lệ vào /admin/promotions, /admin/promotions/new, /admin/platform-coupons/new, /manager/promotions/new đều 200; chưa tái hiện 403 bằng token Admin hợp lệ. Thêm hồi quy JWT/filter và ranh giới namespace. Chạy PlatformPromotionControllerMvcTest, PlatformMarketingAuthorizationTest, SecurityAuthorizationTest bằng bản Maven Wrapper tạm xử lý lỗi Target[0] của PowerShell: 20 tests, 0 failures/errors, BUILD SUCCESS. Không thay Entity/pom.xml/wrapper dự án, không commit/push.

## Chốt quyền tạo mới chỉ Admin cho khuyến mãi và mã giảm giá

Thay yêu cầu trước: Admin tạo mới và quản lý toàn bộ; Manager xem/sửa/ngừng hoặc kích hoạt, không được tạo mới. Giới hạn GET/POST /new ở cả hai alias cho Admin; ẩn nút Tạo bằng sec:authorize. POST danh sách chỉ cho Manager nếu có ID cập nhật, Service kiểm tra ID null để chặn tạo lách Controller; giữ kiểm tra đối tượng là chương trình toàn sàn. Cập nhật Controller/Service của cả promotions và platform-coupons, form action /new khi tạo, SecurityConfig và SECURITY_SPEC; giữ cơ chế ngừng/kích hoạt hiện có thay vì xóa cứng dữ liệu giao dịch. Không sửa Entity/dependency, không commit/push.

## Mở quyền quản lý khuyến mãi toàn sàn cho Admin và Manager

Theo yêu cầu mới của người dùng, áp dụng ngoại lệ promotions cho cả Admin/Manager ở URL matcher, Controller GET/POST và Service (annotation cùng kiểm tra CurrentUser). Hiển thị nút tạo cho hai role, giữ basePath theo role và sửa breadcrumb Manager tránh /admin/dashboard. Chỉ mở /admin/promotions/**, không mở các module Admin khác; giữ CSRF. Cập nhật kiểm thử tạo form/lưu qua cả hai alias và SECURITY_SPEC. Chưa xác nhận được nguyên nhân 403 của Admin ở môi trường chạy: source trước thay đổi vốn đã cho ROLE_ADMIN mở form. Không sửa Entity/dependency, không commit/push.

## Rà soát 403 của Khuyến mãi toàn sàn

Đối chiếu /admin/promotions và /manager/promotions với Sidebar, SecurityConfig, Controller, Service và class đã biên dịch. Source hiện map cả hai route; Admin được phép ở cả hai, Manager chỉ xem qua /manager/promotions; tạo/sửa vẫn chỉ Admin. Không đổi link hoặc mở rộng /admin/** khi chưa có bằng chứng lỗi. Thêm PlatformPromotionControllerMvcTest kiểm tra Admin/Manager/User và model canManage/basePath. Chưa xác định nguyên nhân 403 thực tế vì chưa có log/trạng thái role của tiến trình đang chạy; Maven Wrapper hiện lỗi Cannot index into a null array. Không sửa Entity/dependency, không commit/push.

## Sửa tên và ảnh sản phẩm Xem gần đây

Kiểm tra HomeController và dữ liệu DTO: xác định HomePageMapperImpl đã áp dụng helper mark(String) cho mọi chuỗi, làm tên/series/game/URL ảnh bị cắt còn một ký tự. Đánh dấu helper bằng MapStruct @Named để chỉ dùng tường minh cho chữ cái đầu Game. Giữ danh sách recentlyViewed và CurrentUser; ưu tiên ảnh Card rồi ảnh chính Product trong truy vấn lịch sử. Tách homeCard vào fragment product-card, dùng CSS catalog giống /products, link đúng /products/{id}, ảnh dự phòng khi null/rỗng. Thêm kiểm thử mapper giữ đầy đủ chuỗi/URL và template dùng fragment; không sửa Entity/dependency, không commit/push.

## Sửa đăng xuất 403 khi trang giữ CSRF token cũ

Kiểm tra form Header/dashboard, SecurityConfig và luồng JWT logout hiện có. Giữ POST /auth/logout, hidden token, thu hồi refresh token, xóa cookie và redirect /auth/login trong AuthController; không bật thêm LogoutFilter hoặc bỏ bảo vệ CSRF. Đánh dấu form đăng xuất và dùng common.js cập nhật tất cả hidden CSRF token từ cookie hiện tại ngay trước native submit, fallback meta; từ chối gửi khi thiếu token hoặc sai origin/method, báo thân thiện. Thêm kiểm thử controller thu hồi token/xóa cookie/chuyển trang và mô phỏng submit với token cũ, trùng field, thiếu field, thiếu token. Không sửa Entity/dependency, không commit/push.

## Sửa truy cập và upload ảnh đại diện

Kiểm tra /profile/avatar: giữ quyền authenticated cho mọi role và ownership qua CurrentUser trong ProfileService. Khai báo tường minh /profile, thêm GET /profile/avatar chuyển về phần avatar; upload multipart bằng App.fetchWithCsrf và FormData, gửi token bằng header để kiểm tra trước khi MVC đọc body. Trả ProfileResponse JSON cho AJAX, giữ POST/Redirect/Get cho form thường. Thêm loading, thông báo lỗi thân thiện, xử lý 401 bằng luồng đăng nhập chung và lời mời trên trang đăng nhập khi đến từ /profile. Giữ CSRF, luồng refresh JWT và bắt đổi mật khẩu; kiểm thử mọi role, Guest, raw CSRF header và từ chối thiếu token. Không sửa Entity, pom.xml; không commit/push.

## Sửa wishlist đã lưu nhưng không hiển thị

Kiểm tra Controller → Service → Repository → MapStruct → template /wishlist. Giữ CurrentUser trong Service và tên Model wishlistPage đã đồng bộ; sửa các đường dẫn JPQL qua Card/Series/Game có thể tạo inner join ngầm bằng LEFT JOIN tường minh cho truy vấn danh sách, count và trạng thái. Giữ bộ lọc sản phẩm/shop/catalog công khai, không loại sản phẩm chỉ vì thiếu Card, Series hoặc ảnh chính. Hiển thị Số lượng từ wishlistPage.totalElements và cập nhật khi bỏ thích. Thêm kiểm thử hồi quy cho SINGLE, sealed, supply, ảnh thiếu, phân trang và ownership; không sửa Entity/dependency, không commit/push.

## Trải nghiệm Guest khi dùng chức năng cần đăng nhập

Đọc AGENTS.md và toàn bộ docs/, giữ nguyên phân quyền và bảo vệ CSRF. Dùng helper chung để báo “Vui lòng đăng nhập để thực hiện chức năng này.” và chuyển /auth/login sau 1,5 giây, giữ trang hiện tại làm returnUrl; xử lý 401 và 403 khi giao diện đang ở trạng thái Guest, không nhầm 403 của tài khoản đã đăng nhập với lỗi xác thực. Đồng bộ cart, wishlist, checkout và các nút thêm giỏ ở chi tiết/bảng so sánh giá; Header hiển thị Đăng nhập/Đăng ký và lời mời cho Guest. Kiểm thử API Guest trả 401 khi CSRF hợp lệ và các trang riêng chuyển sang đăng nhập. Không sửa Entity, pom.xml hoặc rule phân quyền; không commit/push.

## Sửa ID sản phẩm cho nút yêu thích

Kiểm tra wishlist.js, fragment nút tim, trang /wishlist và API toggle. Sửa kết quả tìm kiếm SINGLE gộp theo Card đang trả productId null: dùng ID Product nhỏ nhất trong tập kết quả hợp lệ làm đại diện cho yêu thích sản phẩm. JS đọc ID từ nút hoặc card cha, kiểm tra ID dương và chặn gửi trùng; trang yêu thích dùng fragment product-card và wishlist-button chung, giữ link Khám phá về /products. Thêm test ID đại diện, render nút tim và truyền đúng path variable; không sửa Entity/dependency/security và không commit/push.

## Sửa CSRF raw cookie token trên Spring Security 6

Thống nhất CookieCsrfTokenRepository.withHttpOnlyFalse(), CsrfTokenRequestAttributeHandler và token từ cookie/meta để khắc phục 403 do XOR token. Tải deferred token ngay trên mỗi request để cấp cookie cho GET không render view, ưu tiên cookie hiện tại trong fetchWithCsrf, giữ credentials include cùng origin, khôi phục bảo vệ CSRF cho logout. Bổ sung kiểm thử GET → cookie → POST raw token, form và từ chối token sai/thiếu; không đổi dependency hoặc Entity.

## PA-12 - Sửa thông tin và khóa tài khoản từ danh sách

Đọc AGENTS.md và toàn bộ docs/, bổ sung nút Sửa và Khóa/Mở khóa tại /manager/users; form dashboard Midnight Rare chỉ sửa họ tên và số điện thoại, validation phía client/server, spinner khi lưu và toast. Toggle trạng thái qua fetchWithCsrf không tải lại trang, giữ lý do khóa và phân quyền Manager/Admin hiện có, chặn tự khóa ở Service. Dùng ACTIVE/BLOCKED theo Entity và SECURITY_SPEC; INACTIVE vẫn cần xác thực OTP. Tái sử dụng UpdateProfileRequest và MapStruct, thêm test hồi quy, không sửa Entity, SecurityConfig, pom.xml; không commit/push.

## Admin sidebar and placeholder pages

Audit Admin sidebar destinations against existing web controllers and templates. Connect implemented pages, add role-protected dashboard placeholders for missing order, permission, system configuration, and system log screens, and finish the shared Admin/Manager dashboard presentation without changing business services, entities, security rules, or dependencies.

## Wishlist role repair

Repair idempotent seed-role assignment for existing demo users, ensure registration/resubmission assigns ROLE_USER, preserve existing roles and required USER+VENDOR membership, and stop presenting every Wishlist 403 (including CSRF failures) as a missing USER role. Add focused regression tests without weakening the existing authorization rules.

## Static information pages

Create eight Vietnamese Midnight Rare static information pages (about, contact, FAQ, terms, privacy, buying guide, shipping, returns) under the shared Thymeleaf main layout. Add one web controller, connect footer links, make the pages public without changing any existing authorization rules, and implement the contact form as a client-only toast/reset demo. Do not add a backend contact submission flow or dependencies. Use the provided student names and IDs; clearly label the email as a demo address and do not invent phone/address details.

## PA-12 - Quản lý người dùng

Implement a paginated Vietnamese Manager/Admin user-management page with search by name/email, role and status filters, a user detail page, and account block/unblock actions with a required reason. Enforce management rules in the service: managers cannot view or manage Admin/Manager accounts, users cannot block themselves, and only Admin can manage Manager accounts. Reuse existing User status, block reason, CurrentUser, and authentication behavior. Do not modify User, SecurityConfig, UserDetailsServiceImpl, OrderService, or dependencies.

## PG-03 - Dashboard layout and role sidebars

Create a responsive Thymeleaf dashboard layout with role-specific sidebars for Vendor, Shipper, Manager, and Admin; add role demo pages and endpoints, reusing the existing breadcrumb and toast fragments. Do not change dependencies, security configuration, WebSocket code, or other unrelated files.

## PG-05 - Error pages

Build friendly, responsive Vietnamese 403, 404, and 500 error pages using the shared Thymeleaf layout, inline SVG illustrations, and a dedicated Midnight Rare stylesheet. Render only fixed HTTP codes and safe copy; never expose model-provided exception details or trace IDs.

## PG-06 - Cloudinary upload service and dev test page

Add the Cloudinary SDK and environment-backed configuration, implement validated image/video uploads with magic-byte checks and safe failures, and provide a temporary `/dev/upload` page/controller for manual testing. Do not alter security configuration, WebSocket code, or unrelated backend modules.

## PG-07 - Shared frontend JavaScript utilities

Create a vanilla JavaScript utility module for CSRF-aware JSON fetch, Bootstrap toast and confirmation modal, safe image preview validation, and button loading states. Add a temporary local-profile demo page/controller and the CSRF meta tags required by the shared layout contract.

## PU-12b - Cart controllers and page shell

Read AGENTS.md, SECURITY_SPEC.md, and CONTRACTS.md section 6. Add only the REST cart controller delegating to CartService, the web cart page controller without loading cart data into the model, a minimal cart template using the existing main layout, and MockMvc tests for authorization, validation, CSRF, response shape, and not-found errors. Keep existing SecurityConfig and mock CartService in controller tests.

## PU-12c - Cart page interactions

Reuse common.js fetch/CSRF, toast, and confirmation helpers; build the cart view in cart.js with safe DOM APIs, shop grouping, selection, quantity updates, removal, summaries, loading/empty/error states, and cart:updated events. Add a short add-to-cart JavaScript integration example to CONTRACTS.md. Keep the markup Bootstrap-based and do not add API endpoints or long custom CSS.

## PG-05 - Error pages

Build friendly, responsive Vietnamese 403, 404, and 500 error pages using the shared Thymeleaf layout, inline SVG illustrations, and a dedicated Midnight Rare stylesheet. Render only fixed HTTP codes and safe copy; never expose model-provided exception details or trace IDs.

## PG-06 - Cloudinary upload service and dev test page

Add the Cloudinary SDK and environment-backed configuration, implement validated image/video uploads with magic-byte checks and safe failures, and provide a temporary `/dev/upload` page/controller for manual testing. Do not alter security configuration, WebSocket code, or unrelated backend modules.

## PG-07 - Shared frontend JavaScript utilities

Create a vanilla JavaScript utility module for CSRF-aware JSON fetch, Bootstrap toast and confirmation modal, safe image preview validation, and button loading states. Add a temporary local-profile demo page/controller and the CSRF meta tags required by the shared layout contract.

## PA-11 - Card catalog REST API

Expose the existing Card Catalog service through REST endpoints: allow Vendors to search active cards for listings, and allow Managers/Admins to search, view, create, update, and hide/show cards. Reuse the service's Cloudinary upload and replacement cleanup behavior; do not add a frontend or change shared entities.


## PA-18 - Quản lý nhà vận chuyển

Implement Admin-only shipping carrier CRUD under `/admin/shipping-carriers`, preserving the checkout `ShippingService` contract. Validate and check duplicate carrier name/code, retain fee-threshold calculation, hide carriers referenced by orders and delete unused ones, add Thymeleaf list/form pages and focused service tests. Do not modify shared entities, SecurityConfig, OrderService, dependencies, or unrelated tests. Since `ShippingCarrier` has no `logoPublicId`, document the blocker and do not upload Cloudinary logos until the schema can safely persist the public ID.

## PA-17 - Quản lý hoa hồng

Implement Admin-only commission management under `/admin/commissions`: preserve `CommissionService.getRate(Long shopId)`, prefer a shop override, otherwise use the latest platform rate and fall back to 5.00%; append a new `CommissionSetting` row for each platform-rate change; allow Admins to set or clear a shop-specific override. Validate rates from 0% to 100%, enforce Admin authorization in controller and service, add paginated Vietnamese Thymeleaf pages, and write focused service tests. Do not modify shared entities, SecurityConfig, OrderService, dependencies, or unrelated tests. `CommissionSetting` has no `effective_from`, so document the blocker and make platform changes immediate until the schema is approved.

## PA-13 - Tạo tài khoản Manager và Shipper

Implement Admin-only Manager account creation and Manager/Admin Shipper account creation/listing under `/admin/**` and `/manager/**`. Generate a 12-character SecureRandom temporary password, store only its encoded form, send it synchronously by email so a send failure rolls back the transaction, and require a password change at first login. Show Shipper status and count orders in `SHIPPING`; do not modify shared entities, SecurityConfig, security package, OrderService, dependencies, or unrelated tests. `User` has no region field, so document the blocker and do not accept/discard region input.
## PA-14 - Duyệt và tạm ngừng cửa hàng

Implement Manager/Admin shop review under `/manager/shops`: search/filter/paginate shop applications, view shop/owner details, approve or reject pending applications, suspend approved shops, and reactivate suspended shops. Approval adds `ROLE_VENDOR` without removing `ROLE_USER`; rejection stores a required reason; send the owner a `SHOP_APPROVAL` notification. Enforce Manager/Admin authorization in the controller and service, validate form input, map entities to DTOs, and add focused service and MVC tests. Do not modify shared entities, SecurityConfig, OrderService, dependencies, or unrelated tests. No public product listing service/controller exists yet; preserve approval-status checks in cart and document that public listing filtering must be wired when that module is added.

## PU-11a - Address service and DTO

Implement authenticated-user address listing, CRUD, default selection and checkout lookup. Extend the existing repositories with user-row locking and ownership-scoped queries, normalize validated address fields, enforce the per-user limit and a single default address, and add DTO mapping plus Mockito and validation tests. Do not create a duplicate controller or overwrite existing address code.

## PU-11b - Address controllers and pages

Implement web and read-only REST controllers for authenticated address management, reuse the existing profile navigation and safe return URL resolver, add Bootstrap Thymeleaf address list/form pages and delete confirmation, preserve the existing security rules, and add MockMvc coverage for validation, redirects, CSRF, authorization, 404 ownership handling, and must-change-password behavior.

## PU-13a - Checkout DTOs and money allocation

Add largest-remainder allocation for whole-VND proportional splits, checkout preview request/response DTOs with validation and non-null response lists, and deterministic unit tests including 2,000 seeded property-style cases. Reuse existing checkout-related types and utilities without creating duplicate services or DTOs.

## PU-13b - Checkout preview calculation

Implement a read-only checkout preview service that uses the existing cart, address, coupon and shipping service contracts, rounds unit prices before multiplication, applies shop and platform discounts in order, allocates platform discounts proportionally, and reports blocking issues. Add focused Mockito tests for calculation, service-call counts, validation scenarios and money invariants, then document the shared checkout money rules in CONTRACTS.md.

## PU-14a - Checkout preview endpoint and page shell

Add the checkout preview REST endpoint and checkout page shell without replacing existing code. Restrict `/api/checkout/**` to `ROLE_USER`, document the URL rule, reject duplicate shop ids in the preview service, and add MockMvc/service tests for authorization, CSRF, validation, idempotency token generation, and error handling.

## PU-14b - Checkout preview interface

Complete the checkout page with address selection, per-shop carrier and coupon controls, payment selection, summary totals, loading and retry states, stale-request cancellation, safe DOM rendering, and the shared CSRF/API/toast helpers. Keep order submission as a PU-15 placeholder and do not add or call a write API.

## PA-15 - Quản lý sản phẩm theo shop

Implement Manager/Admin product moderation under `/manager/products`: filter products by shop, type and status, search by product/shop name, and paginate results. Allow removal of a product only with a required reason, persist `ProductStatus.BANNED` and `banReason`, restore a banned product to `ACTIVE`, and notify the shop owner through the existing `NotificationService`. Enforce role authorization in both controller and service, map entities to DTOs, and add focused service and MVC tests. Do not modify shared entities, SecurityConfig, OrderService, security package, dependencies, or unrelated tests. No public product catalog exists yet; retain cart availability checks and document that catalog filtering for banned products must be wired when that module is added.

## PG-10 - Đăng ký shop

Implement the seller shop registration flow for regular users at `/seller-registration` using the existing `Shop` entity, `ShopStatus`, repository, `CloudinaryService`, shared main layout, and `common.js`. Support new applications and resubmission after `REJECTED`, display `SUSPENDED` as a locked shop, and do not modify shared entities, enums, `SecurityConfig`, approval workflows, WebSocket code, dependencies, or unrelated files.

## PG-11 - Quản lý hồ sơ và trang công khai của shop

Add Vendor shop profile editing at `/vendor/shop/manage` for logo, banner, description, phone, and pickup address, reusing the existing Shop schema and CloudinaryService with safe image replacement cleanup. Add a public approved shop page at `/shops/{id}` with paginated active products and ProductType filtering. Keep ProductRepository, ShopRepository, shared entities, SecurityConfig, PA-14, WebSocket code, and dependencies unchanged.

## PG-12 - Danh sách sản phẩm Vendor

Implement the Vendor product list at `/vendor/products` with name search, ProductType/status filters, and pagination. Resolve the authenticated user with CurrentUser, find their Shop, and query only that shop through a dedicated repository projection. Map query results to VendorProductResponse and render a responsive Midnight Rare dashboard table with status badges, empty state, shared pagination, and coming-soon toasts for create/edit/delete placeholders. Keep Product, ProductRepository, ShopRepository, SecurityConfig, dependencies, and unrelated files unchanged.

## UI - Connect implemented navigation routes

Review the shared main/dashboard layouts and role sidebars against the implemented web controllers and SECURITY_SPEC URL map. Route implemented links to their real pages, mark unavailable destinations as coming soon with a shared toast, and leave unrelated controllers, entities, and security configuration unchanged.

## PG-13 - Đăng bán Single Card

Implement the Vendor single-card listing flow at `/vendor/products/new/single`. Reuse the existing CardService catalog search, approved Vendor shop, shared Product/Card entities and enums, and CloudinaryService. Provide a responsive Midnight Rare card search and listing form with validation, image preview/loading, and a shop-scoped duplicate variant check that directs the Vendor to update existing stock. Keep shared entities, repositories, SecurityConfig, dependencies, and unrelated files unchanged.

## PG-14 - Đăng bán hàng niêm phong và phụ kiện

Implement Vendor product creation at `/vendor/products/new/other` for BOOSTER_BOX, BOOSTER_PACK, BUNDLE, and SUPPLY. Reuse the existing Product, Shop, ProductType, CardLanguage, repositories, SeriesMapper, CurrentUser, and CloudinaryService; enforce Vendor ownership and APPROVED shop status, validate fields, and render a responsive Midnight Rare conditional form with image preview/loading. Since Product has no accessory-category column, persist the selected supply category as a line in the product description. Do not modify shared entities, SecurityConfig, dependencies, or unrelated files.

## PG-15 - Chá»‰nh sá»­a, áº©n vÃ  xÃ³a má»m sáº£n pháº©m Vendor

Add an ownership-checked Vendor product edit form at `/vendor/products/{id}/edit`, supporting price, non-negative stock, description, and optional Cloudinary image replacement with old image cleanup after commit. Add CSRF-protected AJAX status toggle and soft-delete endpoints; soft delete changes status to INACTIVE and blocks when the product has orders in processing states. Use the existing Product `@Version` for optimistic locking, return a retry message on conflicts, use a dedicated repository for order checks, and leave Product, shared repositories, SecurityConfig, and dependencies unchanged.

## PG-16 - Khuyến mãi của shop

Implement Vendor shop-promotion list/create/edit/toggle/soft-delete under `/vendor/promotions`, with 1–90% validation, date-order validation, product selection scoped to the authenticated approved shop, ownership checks, and rejection of overlapping active promotion periods for the same product. Implement the existing shared `PromotionService` signatures (`getEffectivePrice(Long)` and `getEffectivePrices(Collection<Long>)`) to combine the best applicable shop discount and platform discount; use bulk effective prices on the Vendor product list. Preserve shared entities, `PromotionRepository`, SecurityConfig, dependencies, and other members' files.

## PG-17 - Mã giảm giá của shop và CouponService

Add Vendor shop-coupon list/create/edit/toggle/deactivate under `/vendor/coupons`, with uppercase unique codes, discount/date/usage validation, ownership checks, and Midnight Rare Thymeleaf pages. Implement the existing shared `CouponService` signatures without changing the contract: invalid coupons return `CouponApplyResponse(valid=false, message=...)`, percentage discounts round down and respect the maximum, and successful order placement records coupon usage and increments usage under a coupon row lock. Preserve shared entities, enums, repositories, SecurityConfig, dependencies, and other members' files; infer coupon scope from `Coupon.shop` as defined in `ENTITY_SPEC.md`.

## UI - TCGplayer-style home page

Replace the current role-action landing page with a responsive TCG marketplace homepage using `layout/main.html`, mock game/product/set data in `HomeController`, local original SVG artwork, Best Sellers and Latest Sets sections, and a detailed shared footer. Preserve role-based quick links and existing homepage shortcut search; do not add dependencies, modify entities/security, or rely on external image URLs.

## UI - Shop registration routing and Midnight Rare pages

Route users without a shop to the seller registration form and all existing shop applications to their status page; provide a rejected-only resubmission URL. Show the correct seller action in the shared header based on shop registration state, redirect authenticated roles to their selected dashboards while preserving safe return URLs and mandatory password changes, and provide a Shipper dashboard using its role sidebar. Restyle the registration and status pages with the shared main layout and Midnight Rare theme. Keep SecurityConfig, entities, dependencies, and unrelated worktree changes untouched; synchronize the redirect table in SECURITY_SPEC.md and add focused tests.

## PA-30 - Trang chủ Guest và User

Wire the TCG homepage to active games and public products from the database. Show at most 12 active products from approved, non-deleted shops with more than 10 sales, ordered by sales descending; add authenticated User sections for their recent viewed products and highly-rated product recommendations. Reuse the existing ViewedProduct data without changing the PG-27 recording contract, preserve the banner and existing layout, and add focused repository, service, and MVC tests.

## PA-31 - Trang theo danh mục

Add a public catalog navigation flow from active Games to their active Series and from each Series to a paginated list of active marketplace products, including singles linked through Cards and sealed products linked directly to Series. Include breadcrumbs, pagination, empty states, approved-shop and soft-delete/status filtering, and a visible Game & Series menu link. Reuse existing Game/Series catalog services and DTOs, do not modify entities or security rules, and add repository, service, and MVC tests.

## PA-32 + PA-33 - Tìm kiếm nâng cao và Top 20

Add a public `/products` discovery page combining validated keyword, Game, Series, rarity, product type, language, condition, and non-negative price range filters with stable URL-preserving pagination and sorting. Use JPA Specification for public product eligibility and dynamic filters. Group SINGLE offers by Card and show the lowest matching price and distinct approved sellers. Add four ranked Top 20 tabs (newest, best-selling, top-rated, favorites), with top-rated entries requiring at least five reviews. Keep public product/shop/card visibility rules, reuse existing catalog services, do not change shared entities, dependencies, or security rules, and cover combined filtering, card aggregation, review threshold, Top 20 caps, URL pagination, and validation in focused tests.

## PG-18 - Vendor orders

Add a paginated Vendor order list with status tabs and order-code search, plus an ownership-checked order detail page containing recipient, payment, snapshot item, total, and status timeline data. Add a CSRF-protected AJAX action endpoint that allows a Vendor to confirm or reject only NEW orders, requires a rejection note, and delegates every state transition to the shared `OrderService.changeStatus(Long, OrderStatus, String)` contract. Keep shared entities, `OrderRepository`, `OrderService`, SecurityConfig, and common.js unchanged; scope every order query to the shop owned by `CurrentUser`.

## PG-19 - Thông báo đơn mới cho Vendor

Listen for each committed `OrderCreatedEvent` and send an `ORDER_NEW` notification to its `shopOwnerId` through the existing `NotificationService` contract. Add a dashboard bell with unread count, recent notification dropdown, and SockJS/STOMP subscription to `/user/queue/notifications`; preserve shared entities, order flow, security configuration, and dependencies.

## PG-20 - Xử lý yêu cầu trả hàng của Vendor

Add a paginated Vendor return-request list scoped to the authenticated shop, with proof-image preview and AJAX accept/reject actions. Delegate all order transitions to `OrderService.changeStatus`, validate the rejection reason, and preserve the shared return entities, repositories, security configuration, and order workflow.

## PG-21 - Báo cáo doanh thu Vendor

Build a date-filtered Vendor revenue dashboard with paginated delivered-order details. Scope all queries to the shop resolved from `CurrentUser`, use only `DELIVERED` orders filtered by `deliveredAt`, and aggregate the persisted `shopRevenue` and `commissionAmount` snapshots without recalculating commission. Keep shared entities, repositories, security configuration, dependencies, and unrelated worktree changes untouched.

## PG-22 - Biểu đồ doanh thu Vendor

Thêm API `/api/vendor/revenue` và ba biểu đồ Chart.js cho trang doanh thu Vendor. Chuỗi doanh thu và top sản phẩm chỉ tính đơn `DELIVERED` theo `deliveredAt`, dùng snapshot `shopRevenue`; biểu đồ phân bố trạng thái tính mọi đơn theo `createdAt`. Mọi truy vấn được giới hạn theo shop của `CurrentUser`. Thêm bộ chuyển Day/Month, lọc ngày AJAX, tooltip tiền VND và trạng thái rỗng; không sửa Entity, repository dùng chung, SecurityConfig, dependency hay OrderService.

## PA-34 - Trang chi tiết sản phẩm


## PA-35 - Bảng so sánh giá giữa các Vendor

Add a public JSON API GET /api/cards/{cardId}/offers listing all in-stock ACTIVE products of approved, non-deleted shops that sell the same card, with shop rating from Shop.avgRating/ratingCount, condition, language, promotion-adjusted price via PromotionService.getEffectivePrices and stock. Support filter by condition and sort by price, condition or shop rating with stable tie-breaks and nulls last. Render the comparison table in the product detail page with safe DOM APIs, stale-request cancellation, and AJAX add-to-cart through POST /api/cart/items. Do not modify Entities, security configuration, or OrderService.

Implement the public product detail page at /products/{id}: single-card layout (image, series, number, rarity, market price, lowest price, condition/language/seller selectors, quantity, AJAX add to cart via POST /api/cart/items) and a reduced layout for sealed/supply items. Use PromotionService.getEffectivePrices for discounted prices, return 404 for banned/inactive/deleted products or non-approved shops, keep OUT_OF_STOCK products visible with add-to-cart disabled, and record ViewedProductService for authenticated ROLE_USER only. Leave placeholders for the price chart (PA-36), vendor comparison table (PA-35) and Phong's review section (PG-29). Do not modify Entities, security configuration, or OrderService.

## PA-38 - Check thẻ của tôi

Thêm trang `/check-my-card` chỉ cho ROLE_USER, gợi ý thẻ qua REST theo tên/số thẻ, lọc tình trạng và ngôn ngữ; hiển thị giá thị trường, giá bán hiệu lực thấp nhất, số shop bán, phần trăm chênh lệch, biểu đồ giá 30 ngày và liên kết tới sản phẩm rẻ nhất. Tái sử dụng dịch vụ CardOfferService và PriceChartService, không sửa Entity hoặc cấu hình bảo mật; hiển thị rõ khi thiếu dữ liệu giá.

## PA-39 - Job tính Market Price

Thêm job chạy hằng ngày lúc 02:00 giờ Việt Nam và thao tác chạy tay chỉ cho Admin. Tính trung bình có trọng số theo số lượng từ giá snapshot của OrderItem thuộc đơn DELIVERED trong 30 ngày; khi không có giao dịch dùng giá ADMIN cập nhật gần nhất. Ghi/cập nhật mỗi thẻ tối đa một dòng mỗi ngày, giữ nguyên bản ghi ADMIN trong ngày, cập nhật Card.marketPrice, chỉ phát PriceUpdatedEvent khi thay đổi tuyệt đối đạt ít nhất 5%, và ghi log số thẻ xử lý. Không sửa Entity hay dependency.

## PA-41 - Chuông thông báo

Tạo fragment chuông dùng chung cho header của mọi tài khoản đăng nhập: hiển thị 5 thông báo mới nhất, số chưa đọc và cập nhật ngay khi nhận `/user/queue/notifications`; bấm thông báo chưa đọc phải PATCH đánh dấu đã đọc rồi chuyển tới liên kết nội bộ. Tạo trang `/notifications` có phân trang dùng API hiện có. Tái sử dụng NotificationService, REST API, WebSocket/STOMP và CSRF hiện có; không sửa Entity, dependency hoặc SecurityConfig.

## PA-40 - Thông báo trạng thái đơn hàng

Lắng nghe OrderStatusChangedEvent và OrderAssignedEvent sau commit, gửi thông báo qua NotificationService để được lưu DB rồi đẩy real-time. Áp dụng đúng người nhận theo từng trạng thái trong CONTRACTS.md, gửi thông báo phân công cho Shipper và User, liên kết tới trang đơn hàng phù hợp, thêm lý do cho các trạng thái cần thiết và không gửi cho người vừa thao tác. Không sửa luồng OrderService, Entity, SecurityConfig hoặc dependency.

## PG-23 - Danh sách và chi tiết đơn được phân công cho Shipper

Implement Shipper-only paginated order list and ownership-checked detail pages under `/shipper/orders`. Group assigned orders into pickup (CONFIRMED), in-transit (PICKED_UP/SHIPPING), completed (DELIVERED), failed (DELIVERY_FAILED), and return pickup (RETURN_APPROVED); scope all queries to CurrentUser, return 403 for an existing order assigned to another Shipper and 404 for a missing order. Render the pickup shop contact, recipient delivery details, unpaid COD amount, and order-item snapshots in the Midnight Rare dashboard layout. Keep entities, SecurityConfig, pom.xml, OrderRepository, and unrelated files unchanged; use disabled placeholders for PG-24 actions.

## PG-24 - Cập nhật trạng thái giao hàng Shipper

Add a Shipper-only AJAX status endpoint and order-detail actions for the contract-approved transitions. Verify assignment through ShipperOrderService before delegating all state changes to OrderService.changeStatus(Long, OrderStatus, String); require a delivery-failure note and server-side COD collection confirmation, while leaving COD persistence to the shared order side-effect handler. Preserve shared entities, OrderService, security configuration, dependencies, and unrelated worktree changes.

## PG-25 - Thống kê giao hàng Shipper

Add a Shipper-only date-filtered statistics page and JSON daily delivered-order chart. Scope every query to the authenticated Shipper from CurrentUser; calculate the success rate from delivered and failed orders, sum collected COD only for delivered COD orders, and keep the current picked-up/shipping workload visible. Preserve shared entities, OrderService, shared repositories, SecurityConfig, dependencies, and unrelated PG-23/24 worktree changes.

## PG-26 - Wishlist sản phẩm yêu thích

Implement an authenticated User wishlist page and AJAX heart controls across the homepage, public product lists, shop listings, and product detail page. Follow `CONTRACTS.md`: `POST /api/wishlist/toggle/{productId}` returns `{ wishlisted, favoriteCount }`. Reuse Wishlist, WishlistRepository, Product.favoriteCount and CurrentUser; serialize toggles with a product row lock, recalculate the counter from wishlist rows, expose batch per-user wishlist states, and only include products from approved, non-deleted shops that remain publicly visible. Keep shared entities, ProductRepository, SecurityConfig, dependencies, and unrelated files unchanged.

## PG-27 - Sản phẩm đã xem

Implement the existing `ViewedProductService.record(Long userId, Long productId)` contract with idempotent recency updates and a 20-entry per-user limit. Ignore products that are no longer public, provide a User-only paginated `/viewed-products` page and CSRF-protected POST `/api/viewed-products/record/{productId}`, and call that endpoint from the product detail page for `ROLE_USER`. Reuse the existing ViewedProduct entity and detail-page direct service call; preserve `ProductDetailController`, SecurityConfig, Product, dependencies, and unrelated worktree changes. The new URL patterns require Phú to synchronize `SECURITY_SPEC.md` / SecurityConfig separately.

## PG-28 - Product ratings and reviews (PG-29 contract takeover)

Implement review eligibility for authenticated buyers with at least one delivered order containing the product and no review for that order/product pair. Add a public paginated review fragment, authenticated eligibility and submit APIs, accessible interactive 1–5 star controls, optional comments (minimum 50 characters when present), and transactional Product rating aggregates. Reuse the shared Review/Product/Order entities and existing order-product uniqueness constraint; do not modify entities, SecurityConfig, dependencies, ProductDetailController, or unrelated PG-26/27 worktree changes.

## PG-29 - Review photos and video

Extend product reviews with up to five Cloudinary-validated images and one video, attachment previews, public media thumbnails/lightbox, and owner-only edit/soft-delete endpoints. Enforce optional comment minimum length, retain transactional cleanup of uploaded assets on rollback and deleted media after commit, and preserve the shared review entities, SecurityConfig, dependencies, ProductDetailController, and unrelated worktree changes.

### 2026-10-26 — Mua lại và đánh giá từ đơn đã giao
- Yêu cầu: thêm Mua lại/Đánh giá trên /orders cho DELIVERED; bỏ qua hàng hết, chuyển giỏ, liên kết #review; không sửa Entity/SecurityConfig/pom.xml, không commit/push.
- Thực hiện: POST có CSRF, service tái sử dụng kiểm tra ownership và CartService, thông báo các dòng bỏ qua; cập nhật anchor AJAX review và liên kết chi tiết đơn. Kiểm thử service và MVC cho trạng thái, ownership, tồn kho, role, CSRF và render.
## Trang chủ — Phụ kiện & Hàng niêm phong

Theo phương án đã được người dùng xác nhận: thêm section ngay dưới danh mục Game, tối đa 8 sản phẩm SUPPLY/BOOSTER_BOX/BOOSTER_PACK/BUNDLE mới nhất, ACTIVE, stock > 0, shop APPROVED và không bị xóa. HomeProductRepository trả projection, HomePageServiceImpl dùng HomePageMapper sang DTO, HomeController đưa danh sách vào model. Tách thẻ Best Sellers thành fragment marketplaceCard để dùng chung; grid 2 cột mobile/4 cột desktop, pill dẫn tới /products và /products?type=... có sẵn, empty state khi rỗng. DataSeeder thêm Sleeves 100 nấm, Deck Box gỗ, Playmat TCG và Bundle Pokémon Card 151 theo tên, không ghi đè mẫu đã có và vẫn bổ sung khi bảng products không rỗng. Dùng ảnh dự phòng hiện có khi mẫu chưa có ảnh. Bổ sung kiểm thử repository/service/MVC/seeder. Không sửa Entity, SecurityConfig, pom.xml; không commit/push.
## CatalogDataImporter — đồng bộ sản phẩm/ảnh từ JSON

Yêu cầu đọc catalog.json, tạo Product/ProductImage thiếu hoặc cập nhật ảnh có sẵn, bật riêng bằng app.catalog-import.enabled=true, shop mặc định ID 1, stock mới 10, lỗi item không làm dừng cả đợt. File thực tế ở resources/seed/catalog.json; hỗ trợ ưu tiên resources/catalog.json rồi fallback. Runner dùng Jackson và DTO gọi service REQUIRES_NEW; repository riêng khóa shop, đối chiếu tên và identity thẻ theo bộ/số/ngôn ngữ do file có tên trùng. MapStruct map Product mới; tạo Game/Series/Card còn thiếu theo metadata để SINGLE hợp lệ; cập nhật ảnh chính tại chỗ, giữ giá/tồn kho sản phẩm có sẵn và không xóa asset Cloudinary dùng chung. DataSeeder có Order(0) để chạy trước importer. Thêm hướng dẫn docs/CATALOG_IMPORT.md, kiểm thử unit và H2 import đủ 223 item hai lần. Không sửa Entity, SecurityConfig, pom.xml; không commit/push.
Kiểm thử: 10 tests đạt, gồm rollback Game/Series của item lỗi, import đủ 223 sản phẩm/223 ảnh/200 Card và chạy lại không tăng số bản ghi; chỉ dùng H2 thử nghiệm, chưa chạy import vào DB phát triển.

## Catalog importer — bổ sung lịch sử giá 14 ngày

Theo yêu cầu mới, CatalogImportServiceImpl sinh lịch sử mẫu cho SINGLE có Card chưa có bất kỳ dòng price_histories nào, gồm cả sản phẩm đã được import. Dùng giá Product hiện tại nhân hệ số ngẫu nhiên 0,95–1,05 bằng BigDecimal, làm tròn HALF_UP về đồng; tạo 14 ngày liên tiếp tới hôm nay, market_price=avg_price, source=ADMIN giống DataSeeder/quy ước hiện hành. Inject Clock có sẵn để kiểm tra ngày. Không ghi đè lịch sử cũ, không sinh cho sealed/SUPPLY, không đổi Card.marketPrice. Unit test xác nhận giới hạn ±5%, ngày, giá gốc Product và bỏ qua lịch sử cũ; integration kiểm tra 2.800 dòng cho 200 thẻ, chạy lại giữ nguyên giá và backfill được sản phẩm cũ. Không sửa Entity, SecurityConfig, pom.xml; không commit/push.
## Thumbnail trong danh sách quản lý sản phẩm

Bổ sung cột Ảnh trước Sản phẩm ở /manager/products dùng chung Admin/Manager, ảnh chính 48x48 rounded/object-fit-cover, lazy loading, căn giữa/padding gọn và icon khi thiếu ảnh; giữ table-responsive. ProductModerationResponse thêm imageUrl và constructor tương thích, ProductModerationMapper nhận URL ảnh, ProductModerationServiceImpl truy vấn ảnh chính theo cả trang qua ProductImageRepository để tránh N+1; không đổi nghiệp vụ kiểm duyệt. Bổ sung kiểm thử Service/MVC cho batch ảnh, thumbnail và placeholder. Không sửa Entity, SecurityConfig, pom.xml; không commit/push.
## Latest Sets lấy Series từ Database

## Form khuyến mãi toàn sàn và phạm vi động

## Chỉnh sửa thông tin Chi nhánh Admin/Manager

Thêm thao tác sửa hồ sơ cơ bản cho Admin/Manager tại `/manager/shops/{shopId}/edit`: cho phép cập nhật tên chi nhánh, số điện thoại, địa chỉ lấy hàng và mô tả; không cho phép sửa chủ sở hữu, mã số thuế hoặc trạng thái. Dùng DTO Request riêng có validation, Controller GET/POST theo POST/Redirect/GET và Service kiểm tra quyền, tên trùng trước khi lưu. Bổ sung nút Sửa tại danh sách và hồ sơ, form Thymeleaf hiển thị owner/trạng thái chỉ đọc. Không sửa Entity, SecurityConfig, pom.xml; không chạy test, không commit/push.

Thiết kế lại form Admin/Manager thành các panel thông tin, điều kiện-thời gian và phạm vi; thêm tùy chọn `applyToAll`. Bổ sung GET `/api/series?gameId=...` và `/api/products?seriesId=...` trả DTO, lọc danh sách bằng service/repository và cập nhật chọn Game → Series → sản phẩm bằng JavaScript. Khuyến mãi PRODUCT_DISCOUNT không có phạm vi được lưu dưới dạng không có target, hiển thị rõ toàn sản phẩm đang hoạt động và được PromotionService áp dụng chỉ cho Product ACTIVE; khi edit nhận diện lại trạng thái này mà không sửa Entity. Server bỏ qua target IDs khi applyToAll bật, mặc định toàn sàn khi không có target, xác thực Series/Product theo phạm vi và giữ nguyên luồng SHIPPING_DISCOUNT. Bổ sung test service, giá hiệu lực và MVC/API. Không sửa Entity, SecurityConfig, pom.xml; không commit/push.

Bỏ danh sách Map mock trong HomeController. HomePageServiceImpl lấy tối đa 4 Series active thuộc Game active qua SeriesRepository, ưu tiên release_date giảm dần, ngày null xếp cuối, created_at/id giảm dần để ổn định. HomePageMapper map sang HomeLatestSetResponse (id/name/gameName/releaseDate/logoUrl), đưa vào HomePageResponse và model latestSets. Series Entity hiện không có trường logo; giữ ràng buộc không sửa Entity/schema, logoUrl mặc định null và hiển thị placeholder trung tính bằng icon/text, không dùng hình bộ sưu tập tự vẽ trước đây hoặc giả logo từ ảnh khác. Template hỗ trợ URL logo khi có dữ liệu, ảnh và nút Shop set trỏ /series/{id}; ngày phát hành thật thay số thẻ giả. Kiểm thử repository/service/MVC cho thứ tự, giới hạn, lọc active, link, logo/placeholder. Không sửa Entity, SecurityConfig, pom.xml; không commit/push.
## Đồng bộ nguồn thumbnail Vendor/Manager và ảnh chi tiết

Rà soát cho thấy Vendor đã map imageUrl nhưng SQL chọn dòng ảnh URL rỗng được ưu tiên; Manager chỉ lấy is_main=true nên dữ liệu thiếu cờ main không có ảnh, mapper một tham số dùng ở chi tiết luôn truyền null. Bổ sung batch projection ProductThumbnailProjection/SQL vào ProductImageRepository và đồng bộ SQL Vendor: bỏ URL null/rỗng, ưu tiên main rồi display_order/id, fallback Card.imageUrl. Manager giữ batch theo trang không N+1; DTO hiện có đã đủ trường. ProductImageUtil thống nhất chọn ảnh cho mapper chi tiết Manager và ProductDetailServiceImpl: ưu tiên ProductImage thay vì ảnh Card/ảnh đầu tiên tùy ý. Vendor thumbnail 48x48 bo góc, Manager list kiểm tra blank URL, chi tiết Manager hiển thị ảnh đã map. Không sửa dữ liệu ảnh DB hay thay đổi ownership. Kiểm thử H2 dùng ảnh main/gallery/URL rỗng/Card fallback/thiếu ảnh, kiểm tra cả mapper thực và hai truy vấn. Không sửa Entity, SecurityConfig, pom.xml; không commit/push.
Đồng bộ cả VendorProductEditMapper với helper chọn ảnh. Kết quả: 14 tests đạt, gồm native SQL Vendor/Manager và mapper thực; chưa xác minh ảnh của từng sản phẩm trên Database đang chạy hoặc thử trực tiếp trình duyệt.

## Nền trắng và panel Midnight Rare cho Admin/Manager

Đặt nền body và vùng nội dung Admin/Manager màu trắng, giữ sidebar/topbar hiện có; chuyển card, form, panel quản lý và bảng về Midnight Rare với chữ sáng, input/select tối và focus tím. Ghi đè có phạm vi các utility `bg-white`/`table-light` đang dùng trong trang quản trị, giữ select nhiều sản phẩm dễ đọc và giữ nút tím/vàng. Không tác động Vendor/Shipper, nghiệp vụ, Entity, SecurityConfig hay pom.xml; không commit/push.

## Tự động hết hạn khuyến mãi và mã giảm giá

Hiển thị badge “Đã hết hạn” cho promotion/coupon có endDate trước hiện tại, kể cả khi ActiveStatus lưu ACTIVE; trạng thái đang chạy xanh, tạm dừng xám. Do ActiveStatus/đặc tả schema chỉ có ACTIVE và INACTIVE, không thêm EXPIRED vào enum/entity: job mỗi giờ cập nhật bản ghi ACTIVE quá hạn thành INACTIVE, còn giao diện suy ra nhãn hết hạn từ endDate. Áp dụng nhãn hết hạn cho cả danh sách Vendor sau khi job chạy. Thêm test cho controller và service job; không sửa SecurityConfig, pom.xml; không commit/push.
## Bỏ nền màu khung ảnh và ảnh đại diện Series theo giá bán cao nhất

Theo yêu cầu mới, bỏ nền màu của các khung thumbnail/thẻ sản phẩm trong Vendor, catalog, trang chủ, shop, wishlist và lịch sử; dùng contain/bo góc để không cắt ảnh, giữ layout/các badge. HomeLatestSetResponse bổ sung coverImageUrl, HomePageServiceImpl lấy ảnh bìa cho cả 4 Series trong một query xếp hạng theo Product.price giảm dần, ID giảm dần khi đồng giá. Chỉ xét SINGLE ACTIVE, shop APPROVED, Card active và dữ liệu chưa xóa; ảnh ưu tiên ảnh chính hợp lệ, gallery rồi Card giống thumbnail. Không dùng Market Price làm thước đo. Giữ 4 Series mới nhất và link /series/{id}; thẻ đắt nhất không có nguồn ảnh thì placeholder, không dùng ảnh thẻ rẻ hơn. Template dùng coverImageUrl thay logoUrl. Kiểm thử native SQL với sản phẩm đắt nhất, sản phẩm BANNED giá cao hơn và fallback Card; kiểm thử MVC render ảnh bìa. Không sửa Entity, SecurityConfig, pom.xml; không commit/push.

## Catalog importer đa file và phân bổ shop

Mở rộng `CatalogDataImporter` quét tất cả `seed/*.json` trong classpath, xử lý theo tên file và tiếp tục qua lỗi file/item; tên file không ảnh hưởng parser. `CatalogImportServiceImpl` chọn ngẫu nhiên shop `APPROVED` chưa xóa và khóa/kiểm tra lại trước khi gán Product mới. Nhận diện sản phẩm đã import bằng asset URL/public ID để chạy lại không nhân bản; không ghi đè listing khác shop khi có nhiều match không rõ nguồn. Đồng bộ hướng dẫn `docs/CATALOG_IMPORT.md` và fixture test từ một catalog/Shop ID 1 sang hai catalog và nhiều shop. Dọn thumbnail chung Vendor/Manager: CSS không dùng các `!important` thừa, căn ảnh 48×48 contain; đưa ảnh và tên về cùng flex cell, padding và vertical alignment đồng bộ. Không sửa Entity, SecurityConfig, pom.xml; không commit/push.

## 2026-10-26 - MoMo giả lập tại Checkout
- Yêu cầu: bật COD/MoMo ở Checkout, mô phỏng xác nhận/hủy giao dịch; xác nhận đặt PAID và chuyển success, hủy đặt FAILED và quay lại Checkout. Không gọi API MoMo thật, không sửa Entity/SecurityConfig/pom.xml, không commit/push.
- Thực hiện: tái sử dụng payment_method/payment_status trên OrderGroup; thêm endpoints giả lập có kiểm tra ownership, cập nhật state, hủy đơn qua OrderService và khôi phục hàng vào giỏ; bổ sung modal MoMo responsive.


### 2026-10-26 - Bổ sung QR cho MoMo giả lập
- Yêu cầu: hiển thị QR thanh toán tạo trên trình duyệt từ mã nhóm đơn và số tiền trong modal MoMo; giữ nguyên xác nhận/hủy, không gọi MoMo thật.


## 2026-10-26 - Sửa dashboard Shipper và đồng bộ dashboard quản lý
- Yêu cầu: bỏ số liệu demo Shipper, truy vấn số đơn/đã giao/COD theo shipper đăng nhập; sửa dữ liệu biểu đồ giao theo ngày; áp dụng giao diện Midnight Rare cho dashboard Shipper và Manager.
- Thực hiện: thêm DTO/service thống kê Shipper dựa trên database, bảo đảm chart trả dải ngày và backend/frontend thống nhất dữ liệu; tạo dashboard Shipper mới và giới hạn dark theme cho dashboard Manager.

## 2026-10-26 - Dọn sản phẩm thiếu ảnh và tăng tương phản biểu đồ Manager
- Yêu cầu: dọn sản phẩm test không có ảnh thật, giữ nguyên listing dùng asset Cloudinary từ catalog; sửa độ tương phản chữ/trục/lưới/tooltip biểu đồ Manager.
- Thực hiện: thêm DataCleanupRunner bật riêng, mặc định dry-run, nạp URL/public ID từ `seed/*.json`, chỉ soft-delete ứng viên không có ảnh thật sau khi bật cờ apply. Không xóa cứng bản ghi hay tài nguyên Cloudinary. Sửa màu Chart.js theo phạm vi dashboard Manager và thêm `docs/DATA_CLEANUP.md` hướng dẫn xem trước/chạy một lần.

## 2026-10-27 - Điều hướng mua sắm cho Vendor
- Yêu cầu: thêm lối từ sidebar Vendor về trang mua sắm và từ dropdown tài khoản User sang Kênh người bán; giữ quyền mua hàng và không đổi quy tắc redirect login.
- Thực hiện: thêm liên kết trang chủ vào sidebar Vendor và mục Kênh người bán trong dropdown cho role Vendor có role User. Giữ link header riêng cho Vendor không có role User; không sửa SecurityConfig hoặc RoleRedirectResolver.

## 2026-10-27 - Ảnh và liên kết sản phẩm trên trang Series
- Yêu cầu: khắc phục ảnh placeholder trên danh sách Series và chuyển CTA sang chi tiết sản phẩm.
- Thực hiện: ưu tiên thumbnail Product (ảnh chính trước, rồi gallery/Card), fallback sang ảnh Card có giá thị trường cao nhất trong cùng Series; sửa nút hành động về `/products/{id}`. Không sửa Entity, SecurityConfig hoặc dependency.

## 2026-10-27 - Số liệu Dashboard Vendor và Header mua sắm
- Yêu cầu: lấy số liệu Vendor theo shop đăng nhập và dọn Header thành avatar/tên cùng dropdown tài khoản.
- Thực hiện: thêm service/repository thống kê sản phẩm ACTIVE, đơn NEW và shopRevenue của đơn DELIVERED trong tháng; thay dashboard demo, chuyển route demo cũ sang dashboard thật. Header lấy tên/avatar từ CustomUserDetails, dùng chữ cái đầu nếu thiếu ảnh, chuyển quản lý shop vào dropdown và gỡ các nút shop lặp trên thanh điều hướng.


### 2026-10-27 - Link thông báo và giao diện Dashboard Vendor
- Yêu cầu: điều hướng chuông thông báo tới đúng trang theo link được lưu và chuyển Dashboard Vendor sang giao diện navy tối.
- Thực hiện: sửa link thông báo shop về `/vendor/shop/manage`, thông báo sản phẩm được xử lý về `/vendor/products`; giữ nguyên link từ API/DB ở frontend và vẫn điều hướng nếu đánh dấu đã đọc gặp lỗi. Thêm theme navy riêng cho Dashboard Vendor với thẻ thống kê viền tím, chữ sáng và số liệu vàng.
### 2026-10-27 - Sửa link thông báo cũ
- Yêu cầu: đảm bảo chuông điều hướng theo `notification.link`, kiểm tra nơi tạo thông báo và sửa các link `/profile` đã lưu sai.
- Thực hiện: xác nhận fragment header dùng `notification-bell.js`, JS lấy `notification.link`, các service/listener tạo shop, product và ORDER_NEW hiện gán đúng route. Thêm SQL MySQL để sửa dữ liệu cũ theo type/title; thông báo đơn hàng được đối chiếu order_code trong nội dung với bảng orders để dựng link `/vendor/orders/{id}`, bản ghi không khớp được giữ nguyên để tránh điều hướng sai.

### 2026-10-27 - Tự sửa link legacy của thông báo khi tải
- Yêu cầu: khắc phục các thông báo cụ thể vẫn mở `/profile` sau khi đã sửa logic tạo mới.
- Thực hiện: NotificationService sửa link `/profile` hoặc NULL của thông báo shop/sản phẩm/đơn mới ngay khi tải danh sách hay mở thông báo; đơn mới được đối chiếu order_code trước khi gán route. Bản ghi không xác định chắc chắn giữ nguyên để tránh điều hướng nhầm; giữ thêm SQL sửa toàn bộ dữ liệu cũ.

### 2026-10-27 - Vendor đề xuất thẻ mới chờ duyệt
- Yêu cầu: cho Vendor gửi thẻ chưa có trong catalog và tiếp tục lập tin đăng nhưng không đưa thẻ mới ra công khai trước khi duyệt.
- Thực hiện: thêm form/DTO validated, upload ảnh qua CloudinaryService và tạo Card với `active=false`; kiểm tra trùng series+số thẻ+ngôn ngữ trước upload. Dùng session-bound Card ID để Vendor hiện tại tiếp tục bước giá/tình trạng; public catalog đã lọc Card inactive nên tin đăng chưa hiển thị công khai. Manager/Admin duyệt bằng thao tác bật Card hiện có; không sửa Entity, SecurityConfig, pom.xml.

### 2026-10-27 - Lọc đánh giá và phản hồi từ Vendor
- Yêu cầu: lọc đánh giá theo 1–5 sao bằng AJAX và cho đúng Vendor phản hồi đánh giá của sản phẩm thuộc shop mình.
- Thực hiện: thêm cột nullable `vendor_reply`/`replied_at`, Request/Response DTO, lọc rating vào truy vấn phân trang; ReviewService kiểm tra ROLE_VENDOR, ownership qua chủ Shop và chỉ nhận phản hồi lần đầu. Fragment reviews và reviews.js cập nhật lọc, pagination giữ filter, textarea/khung phản hồi; không đổi SecurityConfig hay dependency.

### 2026-10-27 - Thông báo Vendor khi có đánh giá mới
- Yêu cầu: gửi thông báo cho chủ shop sau khi khách đánh giá, đưa Vendor tới phần đánh giá trên chi tiết sản phẩm.
- Thực hiện: phát `ReviewCreatedEvent` sau khi lưu và cập nhật rating; listener AFTER_COMMIT/REQUIRES_NEW gửi `REVIEW_NEW` tới chủ shop, link `/products/{id}#reviews`. Giữ nguyên link qua notification bell, đặt anchor ổn định trên vùng reviews trước khi fragment AJAX tải và cuộn lại sau khi nội dung sẵn sàng.


### 2026-10-27 - Hội thoại hai chiều trên đánh giá
- Yêu cầu: cho phép Vendor và tác giả đánh giá gửi nhiều tin nhắn qua lại, thông báo cho bên còn lại và hiển thị dạng chat.
- Thực hiện: tạo entity/repository `ReviewReply`, bổ sung DTO danh sách tin nhắn; API hiện tại thêm tin mới sau khi kiểm tra ownership, gửi `REVIEW_REPLY` tới người còn lại và giữ link `#reviews`. Fragment hiển thị bong bóng trái/phải; phản hồi Vendor legacy vẫn được hiển thị.

### 2026-10-27 - Chuyển notifications.type sang VARCHAR
- Yêu cầu: xử lý lỗi Data truncated do cột MySQL ENUM thiếu loại thông báo mới.
- Thực hiện: giữ `NotificationType` làm enum Java, khai báo rõ cột JPA `VARCHAR(50)` và thêm SQL migration đổi cột `notifications.type`, giữ nullable hiện tại và bảo toàn tên giá trị đã lưu.

### 2026-10-27 - Import catalog Yu-Gi-Oh! vào shop riêng
- Yêu cầu: import `yugioh_catalog.json` vào shop có tên khớp Yugi/Yu-Gi-Oh!, tạo shop YuGiOhh APPROVED nếu chưa có; bảo đảm ảnh/link và Latest Sets LOB/MRD/SRL đúng.
- Thực hiện: importer chọn một shop đích cho toàn bộ file và chuyển listing khớp asset về shop đó; fallback tạo YuGiOhh. Ưu tiên ba series YGO trong Latest Sets, ảnh bìa lấy theo market price Card cao nhất. Sửa link ảnh/tên sản phẩm ở gợi ý trang chủ sang chi tiết sản phẩm. Tìm kiếm và Series đã dùng link chi tiết và fallback ảnh Cloudinary.

### 2026-10-27 - Bổ sung lịch sử giá chart khi import và nút Latest Sets
- Yêu cầu: biểu đồ của card Yu-Gi-Oh! mới phải có dữ liệu; Latest Sets cần nút rõ ràng vào trang bộ sưu tập.
- Thực hiện: importer đảm bảo đủ ngày lịch sử từ hôm nay lùi 13 ngày, chỉ thêm ngày thiếu và dùng Card.marketPrice (fallback Product.price); template chi tiết có trạng thái tải chart cho SINGLE có cardId. Thay link chữ Shop set bằng nút vàng “Xem bộ sưu tập”.

### 2026-10-27 - Ảnh bìa Series trên trang Game
- Yêu cầu: thêm cover cho từng Series trong trang danh sách theo Game, lấy từ ảnh thẻ giá cao nhất.
- Thực hiện: thêm DTO công khai `GameSeriesResponse.coverImageUrl`; ProductImageRepository truy vấn cover theo Card.marketPrice, ưu tiên ảnh main của listing Cloudinary rồi fallback Card.imageUrl. Tạo layout ảnh và thông tin trong card Midnight Rare, placeholder nếu thiếu ảnh, responsive grid và hover.

### 2026-10-27 - Hero Trang chủ dùng ảnh thẻ bài thật
- Yêu cầu: thay ảnh AI và tiêu đề Riftbound trên Hero bằng nhóm ảnh thẻ thật lấy từ Database, có hiệu ứng xếp chồng và responsive.
- Thực hiện: HomeProductRepository chọn tối đa bốn ảnh từ Card có market price cao, ưu tiên ảnh main của listing Single thuộc Shop đã duyệt và fallback Card.imageUrl. Truyền danh sách `heroImages` qua HomePageResponse; thay minh họa tĩnh bằng các thẻ nghiêng, đổ bóng, giảm còn hai thẻ ở mobile; đổi tiêu đề chung và giữ CTA hiện có.

### 2026-10-27 - Tăng kích thước thẻ trong Hero Trang chủ
- Yêu cầu: làm các ảnh thẻ Hero lớn và nổi bật hơn, xếp chồng cân đối ở bên phải banner, vẫn gọn trên mobile.
- Thực hiện: tăng kích thước thẻ desktop lên tối đa 350px, mở rộng vùng xếp chồng, dùng góc nghiêng -16/0/16 độ và bóng đậm hơn. Giảm kích thước ở tablet/mobile, ẩn thẻ thứ ba và thứ tư trên mobile.

### 2026-10-27 - Tăng nhẹ Header và Footer giao diện mua sắm
- Yêu cầu: làm Header/Footer chắc chắn, dễ đọc hơn, giữ responsive và không ảnh hưởng Dashboard.
- Thực hiện: thêm class riêng cho fragment Header/Footer và scope CSS ở `custom.css`; tăng nhẹ chiều cao, font, logo, khoảng cách link và line-height; giảm lại padding/logo ở mobile.

### 2026-10-27 - Điểm đánh giá tổng hợp trên trang Shop công khai
- Yêu cầu: hiển thị điểm trung bình và tổng lượt đánh giá của shop dựa trên review sản phẩm.
- Thực hiện: thêm truy vấn tổng hợp theo shop vào `ReviewAggregateRepository`; `PublicShopServiceImpl` truyền điểm trung bình và số review chưa xóa qua `PublicShopResponse`. Tái sử dụng giao diện sao vàng và trạng thái “Chưa có đánh giá”, không thêm Entity mới.

### 2026-10-27 - Chatbot FAQ theo từ khóa và quyền tài khoản
- Yêu cầu: thêm widget chat Midnight Rare dùng chung cho Guest và dashboard; tạo FAQ theo keyword cho khách mua hàng, Vendor và Shipper, kèm tra cứu giá thị trường thẻ.
- Thực hiện: thêm fragment/CSS/JS, API `POST /api/chatbot/ask` có CSRF, `ChatbotService` chuẩn hóa dấu và chọn keyword cụ thể nhất, lọc kịch bản theo authority; thêm truy vấn giá cho thẻ/Game/Series đang công khai. Cập nhật route map bảo mật, không thêm dependency hoặc LLM API.
- Bổ sung: mở rộng chỉ đường tới giỏ hàng, hồ sơ, đơn hàng, wishlist, đăng xuất, tìm kiếm; chi tiết hóa thanh toán, hỗ trợ, lỗi đăng nhập, Vendor và Shipper. Khi chỉ hỏi giá mà chưa nêu thẻ, chatbot yêu cầu tên thẻ.

### 2026-10-27 - Nâng chatbot thành trợ lý mua sắm
- Yêu cầu: hỗ trợ FAQ có sai dấu/lỗi gõ nhỏ và tìm thẻ, hiển thị ảnh, tối đa ba shop rẻ nhất cùng phân tích giá.
- Thực hiện: bổ sung tìm ứng viên thẻ công khai và so khớp Levenshtein, tái sử dụng CardOfferService để lấy giá bán hiệu lực, thêm HTML phản hồi đã escape và frontend sanitizer theo allowlist; mở rộng kích thước khung chat và bố cục ảnh/nội dung.

### 2026-10-27 - Đồng bộ hiển thị trang chủ, Header/Footer và ngữ cảnh đánh giá
- Yêu cầu: chỉnh nhẹ kích thước Header/Footer/chat, sửa ảnh mục phụ kiện/hàng niêm phong để lấy đúng ảnh Product và hiển thị thumbnail/giá trong khung đánh giá.
- Thực hiện: HomeProductRepository truy vấn ảnh ProductImage riêng theo product_id (ưu tiên ảnh chính và thứ tự hiển thị), không dùng ảnh Card cho sealed/SUPPLY; ReviewProductResponse cung cấp ảnh và giá sản phẩm để khung đánh giá hiển thị ngữ cảnh. Header/Footer đã có kích thước và CSS responsive phù hợp từ tinh chỉnh trước; tăng nhẹ nút chatbot và cân chỉnh khoảng cách panel trên mobile. Không sửa Entity, SecurityConfig hoặc pom.xml.

### 2026-10-27 - Sửa tìm kiếm mờ thẻ trong chatbot
- Yêu cầu: cho phép nhập riêng tên thẻ không phân biệt hoa thường, fuzzy match tên gõ gần đúng, hiển thị tối đa ba shop và mở chi tiết sản phẩm khi bấm ảnh.
- Thực hiện: tách FAQ khỏi truy vấn tên thẻ; tìm LIKE theo từng token rồi fallback sang so khớp Levenshtein trên danh mục công khai. Bọc ảnh bằng link cùng origin tới sản phẩm rẻ nhất và mở rộng sanitizer phía trình duyệt chỉ cho phép URL `/products/{id}`.

### 2026-10-27 - Đồng bộ dữ liệu lịch sử và API biểu đồ giá
- Yêu cầu: đảm bảo catalog importer backfill lịch sử giá 14 ngày không ghi trùng, API trả dữ liệu phù hợp Chart.js và chi tiết hiển thị empty state thân thiện.
- Thực hiện: giữ cơ chế importer hiện có chỉ thêm ngày thiếu; API giữ `points` tương thích và bổ sung `labels`, `data`, `averageData`; biểu đồ dùng các mảng mới và hiện “Chưa có dữ liệu giá cho sản phẩm này.” khi chưa có điểm. Bổ sung kiểm thử backfill ngày thiếu và JSON API.

### 2026-10-27 - Cho Admin thay đổi vai trò người dùng
- Yêu cầu: thêm dropdown role vào trang sửa người dùng cho Admin, giữ email bất biến, chặn Admin tự đổi role và cảnh báo khi hạ Vendor còn sở hữu Shop.
- Thực hiện: thêm UserUpdateRequest riêng cho màn hình quản lý; Service thay bộ role qua RoleRepository, chỉ Admin được đổi và Admin không thể đổi role chính mình. Vendor luôn nhận cả ROLE_USER + ROLE_VENDOR theo SECURITY_SPEC; không bỏ role Vendor khi tài khoản còn Shop chưa xóa để tránh mất quyền quản lý shop. Email không có trong DTO cập nhật.

### 2026-10-27 - Trang quản lý đơn hàng toàn hệ thống
- Yêu cầu: thay trang placeholder `/manager/orders` bằng danh sách đơn toàn hệ thống có tìm kiếm, lọc trạng thái, phân trang và trang chi tiết chỉ đọc.

### 2026-10-27 - Phân phối catalog cho mọi chi nhánh và so sánh giá chatbot
- Yêu cầu: mỗi item JSON được tạo ở mọi Shop `APPROVED` chưa xóa nếu Shop chưa có listing trùng; chatbot hiển thị giá sau Promotion tự động theo chi nhánh và chỉ nơi rẻ nhất. Không tính coupon; không sửa Entity/SecurityConfig/pom.xml, không chạy test/build hoặc commit/push.
- Thực hiện: importer khóa và duyệt danh sách Shop đã duyệt cho từng item, so khớp listing theo Shop, bỏ qua listing có sẵn mà không cập nhật dữ liệu, dùng chung Card và lịch sử giá giữa các Shop; bỏ đích Shop riêng cho Yu-Gi-Oh!. Chatbot nhóm offer theo Shop, giữ giá hiệu lực thấp nhất và thông báo coupon không được tính. Cập nhật test source và `docs/CATALOG_IMPORT.md`; chưa chạy kiểm thử hay importer.
- Thực hiện: thêm truy vấn tìm đơn không giới hạn shop, service/controller chỉ đọc cho Manager/Admin, DTO riêng cho danh sách/chi tiết/timeline và giao diện responsive; giữ route và logic thay đổi trạng thái hiện có của Vendor/Shipper.

### 2026-10-27 - Lưu lịch sử trò chuyện chatbot
- Yêu cầu: giữ lịch sử chat qua điều hướng/tải lại trang và thêm nút xóa lịch sử.
- Thực hiện: lưu tin nhắn user/bot trong `localStorage` bằng khóa `tcg_chat_history`, khôi phục và sanitize nội dung rich HTML khi `DOMContentLoaded`; nút thùng rác xóa khóa lịch sử và khôi phục lời chào mặc định.

### 2026-10-27 - Ưu tiên tìm thẻ trong chatbot
- Yêu cầu: không nhầm yêu cầu tìm thẻ gõ sai với FAQ về coupon; hỗ trợ tên đầy đủ, tên rút gọn và lỗi gõ nhẹ.
- Thực hiện: đưa nhánh tìm sản phẩm lên trước FAQ khi câu có tín hiệu tìm/mua hoặc từ chỉ thẻ; trả thông báo không tìm thấy thay vì rơi sang intent không liên quan. Tìm ứng viên bằng LIKE với tiền tố ngắn để fuzzy scorer xếp hạng tên gần đúng.

### 2026-10-27 - Tìm thẻ theo ngân sách trong chatbot
- Yêu cầu: nhận diện số tiền trong câu tự nhiên, tìm tối đa 5 sản phẩm phù hợp theo khoảng giá và hiển thị ảnh, tên thẻ, giá cùng Shop.
- Thực hiện: parse các dạng `200k`, `200 ngàn/nghìn`, `500.000đ`, quy đổi về VND và tạo khoảng giá theo từ khóa giới hạn hoặc xấp xỉ; thêm repository/service/DTO/mapper chỉ đọc cho các sản phẩm SINGLE đang bán, áp dụng giá khuyến mãi và dựng danh sách card đã sanitize.

### 2026-10-27 - Tách lịch sử chatbot theo tài khoản
- Yêu cầu: giữ lịch sử riêng cho từng tài khoản qua đăng xuất/đăng nhập, Guest dùng vùng lưu trữ riêng; không sửa Entity, SecurityConfig, pom.xml, không commit/push.
- Thực hiện: widget Thymeleaf cung cấp email tài khoản hiện tại hoặc `guest_chat`; chatbot dùng key localStorage riêng theo danh tính và giữ nguyên dữ liệu khi đăng xuất. Key cũ dùng chung không được tự di chuyển vì không xác định được thuộc tài khoản nào. Thêm kiểm thử MVC cho Guest và tài khoản đã đăng nhập.

### 2026-10-27 - Sửa nhận diện intent hỗ trợ và fallback chatbot
- Yêu cầu: không nhầm câu hỏi về trung tâm hỗ trợ/FAQ với quên mật khẩu hoặc tạo mã giảm giá; câu không hiểu phải trả lời fallback rõ ràng có link FAQ.
- Thực hiện: chuyển intent sang so khớp cụm từ chính xác, tách hướng dẫn quên mật khẩu khỏi đăng ký tài khoản, thêm liên kết an toàn tới `/faq` và `/contact`, đồng thời hạn chế tìm thẻ tự do để câu hỏi thông thường không bị gán kết quả ngẫu nhiên. Bổ sung kiểm thử hồi quy cho FAQ, hỗ trợ, quên mật khẩu, đăng ký và fallback.

### 2026-10-27 - Đồng bộ nền Lavender nhạt toàn hệ thống
- Yêu cầu: dùng `#f8f6fc` làm nền chung cho toàn bộ trang mua sắm và dashboard, giữ card/panel trắng hoặc navy theo khu vực.
- Thực hiện: đổi nền body trong CSS dùng chung và bỏ `bg-light` cùng các lớp nền tối phủ toàn bộ dashboard Manager/Vendor/Shipper; giữ sidebar/topbar và các panel navy, card trắng hiện có. Đã rà soát trang chủ, chi tiết sản phẩm, giỏ hàng và dashboard; không đổi logic hoặc dependency.

### 2026-10-27 - Thêm nền sao lấp lánh và khung kính mờ
- Yêu cầu: thêm nền Lavender chuyển sắc với các đốm sáng chuyển động cho trang người mua, không áp dụng lên Dashboard; tạo hiệu ứng glassmorphism cho card sản phẩm, form xác thực và giỏ hàng.
- Thực hiện: thêm lớp nền CSS cố định, pointer-events không chặn thao tác, animation nhẹ và tôn trọng `prefers-reduced-motion`; gắn class riêng vào layout chính, giữ layout Dashboard tách biệt. Làm khung auth thành kính trắng mờ, áp dụng viền tím nhạt/blur cho các card và panel liên quan; không thêm JavaScript hay đổi logic biểu mẫu.

### 2026-10-27 - Nền rừng Pixel Art và thẻ sản phẩm 3D
- Yêu cầu: thay nền Lavender của trang người mua bằng ảnh rừng Pixel Art, phủ overlay tối mờ, tạo thẻ sản phẩm viền vàng và hiệu ứng 3D; Dashboard giữ nguyên.
- Thực hiện: dùng ảnh dự kiến `static/img/bg-forest.png` qua URL Spring Static `/img/bg-forest.png`, thêm overlay blur, làm Header/Footer sáng dạng kính và thẻ sản phẩm nền trắng với bóng đổ/hover xoay nhẹ. Chỉ áp dụng trong `.public-site-body`; không sửa layout Dashboard hoặc logic.

### 2026-10-27 - Tăng tương phản chữ và bộ lọc trên nền rừng
- Yêu cầu: làm tối và blur nhẹ overlay nền, chuyển tiêu đề trang chủ sang trắng và làm nổi bật các nút lọc; giữ thẻ sản phẩm sáng, viền vàng.
- Thực hiện: tăng overlay lên 70%, blur 4px; thêm bóng chữ cho tiêu đề/nhãn trên nền và đổi tab lọc sang nền tối chữ trắng, trạng thái active/hover vàng chữ đen. Giữ nguyên nền, viền và bóng 3D của card, scope trong `.public-site-body`.

### 2026-10-27 - Khôi phục tương phản nội dung trên nền rừng
- Yêu cầu: hiển thị chữ sáng trên nền tối ở chi tiết sản phẩm và đánh giá, giữ chữ tối trên card/form sáng, cải thiện bộ lọc đánh giá và rà soát giỏ hàng, thanh toán, đăng nhập/đăng ký.
- Thực hiện: đặt màu chữ mặc định sáng cho trang người mua; làm rõ màu chữ chi tiết sản phẩm, tiêu đề/tóm tắt đánh giá và viền bộ lọc; giữ màu tối cho form đánh giá, ngữ cảnh đánh giá, card giỏ hàng/checkout và auth. Link đăng nhập trong ghi chú nền sáng vẫn dùng tím đậm để đảm bảo tương phản.

### 2026-10-27 - Tăng tương phản cho Games, Shop và biểu mẫu xác thực
- Yêu cầu: dùng màu chữ sáng trên nền Pixel Art tối, giữ chữ tối trên các bề mặt sáng và làm form đăng nhập/đăng ký sáng rõ hơn.
- Thực hiện: đặt biến màu chữ riêng trong `.public-site-body`, áp màu sáng cho tiêu đề/đoạn văn/nhãn/link trên nền tối, khôi phục chữ tối cho card, bảng, Header/Footer và panel sáng. Chuyển panel auth sang kính mờ navy, giữ input tối với viền sáng và tăng tương phản nhãn, placeholder, link, thông báo lỗi. CSS chỉ tải ở layout mua sắm nên không thay đổi Dashboard.

### 2026-10-27 - Giảm overlay và sửa tương phản nút Game
- Yêu cầu: làm ảnh nền rừng Pixel Art sáng hơn và khôi phục chữ tối trên các lựa chọn Game có nền trắng, giữ chữ sáng ở các tiêu đề trên nền tối.
- Thực hiện: giảm overlay trong `global-background.css` xuống 45%, giữ nguyên blur; đặt màu chữ tối, đậm cho các nút Game ở trang chủ và danh mục, gồm cả trạng thái hover/focus. Không áp dụng override lên các thẻ Series nền navy hoặc Dashboard.

### 2026-10-27 - Sửa tương phản trang tĩnh và phân trang
- Yêu cầu: giữ chữ tối trên các panel sáng của trang tĩnh và form liên hệ, đồng thời chuẩn hóa phân trang trên các trang người mua.
- Thực hiện: thêm override giới hạn trong `.public-site-body` cho nội dung, liên kết và trường nhập của các panel trang tĩnh; giữ tiêu đề Hero sáng trên nền tối. Đặt nền trắng/chữ tối cho nút phân trang thường và tím/chữ trắng cho trạng thái đang chọn; không ảnh hưởng giao diện Dashboard.

### 2026-10-27 - Làm sáng nền và sửa tìm kiếm phụ kiện
- Yêu cầu: giảm độ tối overlay, sửa màu các tab xếp hạng phụ và đảm bảo ảnh SUPPLY dùng ảnh chính của sản phẩm hoặc placeholder phù hợp.
- Thực hiện: giảm overlay nền Pixel xuống 40%; giới hạn màu tối/nền trắng/viền nhạt cho tab trong trang tìm kiếm. Luồng tìm kiếm SUPPLY chỉ dùng ảnh `product_images.is_main=true`, không lấy ảnh Card/Series thay thế; template có SVG phụ kiện và fallback khi URL ảnh lỗi.

### 2026-10-27 - Tối ưu giao diện trang chủ trên mobile
- Yêu cầu: ngăn Hero tràn, giữ nút Game/danh mục dễ cuộn và không rớt dòng, tăng khoảng đệm mobile, giữ lưới sản phẩm hai cột, đồng thời làm gọn Header/Footer.
- Thực hiện: thêm breakpoint mobile trong `global-background.css` để thu nhỏ Hero/thẻ ảnh, tối ưu menu tìm kiếm và điều hướng, biến danh mục thành hàng cuộn ngang, tăng padding nội dung và xếp các cột Footer dọc. Các luật được giới hạn trong breakpoint mobile, không đổi bố cục desktop.

### 2026-10-27 - Tăng tương phản biểu đồ giá thị trường
- Yêu cầu: tách biểu đồ khỏi nền ảnh bằng panel tối và làm rõ trục, lưới, đường dữ liệu cùng tooltip Chart.js.
- Thực hiện: thêm panel glassmorphism bao quanh canvas và khung chứa biểu đồ, thu gọn padding trên mobile; đặt ticks/legend sáng, grid trắng mờ, đường giá xanh dương/vàng và tooltip navy chữ sáng. Không thay đổi API hay dữ liệu biểu đồ.

### 2026-10-27 - Sửa tương phản Latest Sets
- Yêu cầu: làm rõ tiêu đề/mô tả Latest Sets trên panel sáng và đổi nhãn NEW SET sang vàng, chữ tối.
- Thực hiện: giới hạn màu chữ tối, in đậm cho phần đầu đề và mô tả trong `#latest-sets`; đổi nhãn NEW SET sang nền vàng và chữ tối, không ảnh hưởng tiêu đề ở các khu vực nền tối hay nội dung thẻ sản phẩm.

### 2026-10-27 - Sửa tương phản hồ sơ, đơn hàng và yêu thích
- Yêu cầu: khôi phục chữ tối trên card/form nền sáng ở hồ sơ, địa chỉ, đơn hàng và wishlist; làm rõ Sidebar Profile, giữ tiêu đề ngoài panel sáng trên nền tối.
- Thực hiện: giới hạn override trong các trang `.profile-page`, `.user-orders-page`, `.wishlist-page`; đặt chữ/input/table tối cho panel trắng, thêm trạng thái hover/active cho Sidebar Profile và trạng thái `aria-current` theo trang. Không đổi logic nghiệp vụ, Entity, SecurityConfig hoặc dependency.

### 2026-10-27 - Đồng bộ nền và khung hồ sơ
- Yêu cầu: đảm bảo Hồ sơ, Đổi mật khẩu và Sổ địa chỉ dùng nền phong cảnh của `layout/main.html`, làm nổi bật form và chuyển Sidebar Profile sang nền trắng mờ.
- Thực hiện: các trang đã kế thừa layout chính; bỏ class `bg-light` ghi đè nền ở trang đổi mật khẩu, bọc form mật khẩu/địa chỉ bằng panel dùng chung. Tăng glassmorphism cho card profile và làm Sidebar bán trong suốt, vẫn giữ chữ tương phản và trạng thái active. Không đổi logic, Entity, SecurityConfig hay dependency.

### 2026-10-27 - Sửa tương phản dropdown Header
- Yêu cầu: khôi phục chữ dễ đọc trên menu tài khoản và các dropdown sáng của layout người mua, giữ nút Đăng xuất màu đỏ.
- Thực hiện: đặt nền sáng và chữ tối cho dropdown trong `.public-site-body`, thêm nền tím nhạt khi hover/focus và giữ màu đỏ riêng cho Đăng xuất. Không thay đổi markup, dropdown Dashboard, phân quyền hoặc logic đăng xuất.

### 2026-10-27 - Tối ưu responsive mobile cho trang mua sắm và Dashboard
- Yêu cầu: tránh tràn nội dung trên mobile ở trang chủ/người mua và Dashboard; giữ lưới sản phẩm hai cột, menu Sidebar dạng hamburger, bảng cuộn ngang, thẻ thống kê rớt dòng và biểu đồ co giãn.
- Thực hiện: giữ các breakpoint sẵn có của Hero, Header, danh mục và Footer; chuyển lưới sản phẩm danh mục/tìm kiếm sang hai cột trên màn hình nhỏ. Bổ sung giới hạn chiều rộng offcanvas, ngăn co tràn ở layout Dashboard, giữ bảng cuộn ngang và giới hạn canvas trong khung; không đổi cấu trúc nghiệp vụ hoặc bố cục desktop.

### 2026-10-27 - Thu gọn phân trang và tối ưu trên mobile
- Yêu cầu: giới hạn các số trang hiển thị quanh trang hiện tại, thêm dấu ba chấm khi lược bớt trang và trên mobile chỉ giữ Trước/trang hiện tại/Sau để tránh tràn.
- Thực hiện: cập nhật fragment phân trang dùng chung để hiển thị tối đa 5 trang liên tiếp cùng trang đầu/cuối khi cần; giữ các nút Trước/Sau, ẩn số phụ và dấu ba chấm dưới breakpoint 768px. Thêm CSS tăng vùng bấm và khoảng cách; bổ sung kiểm thử MVC cho trang giữa và đầu danh sách.

### 2026-10-27 - Thu gọn Hero và cuộn danh mục Game trên mobile
- Yêu cầu: bỏ khoảng trống thừa ở Hero khi ẩn ảnh thẻ trên điện thoại, tránh tràn hàng Game và bảo đảm mô tả được xuống dòng đầy đủ.
- Thực hiện: trong breakpoint tối đa 768px, ẩn vùng ảnh Hero và bỏ chiều cao tối thiểu của hàng, giảm nhẹ tiêu đề, cho mô tả wrap tự nhiên; giới hạn track Game trong chiều rộng container, giữ thẻ không co và ẩn scrollbar nhưng vẫn cuộn ngang. Desktop không đổi.

### 2026-10-27 - Chuẩn hóa thuật ngữ mô hình chuỗi cửa hàng
- Yêu cầu: đổi nội dung giao diện từ thuật ngữ marketplace sang cách gọi chi nhánh/nhượng quyền; cập nhật tiêu đề Dashboard, đăng ký nhượng quyền, quản lý chi nhánh và thông báo.
- Thực hiện: cập nhật nhãn, mô tả, tiêu đề, thông báo và breadcrumb trong template, Java, JavaScript theo thuật ngữ mới; giữ nguyên thương hiệu TCG Shop, route, tên biến/field, định danh kỹ thuật và logic nghiệp vụ. Cập nhật các assertion kiểm thử giao diện tương ứng; không sửa Entity, SecurityConfig hay pom.xml.

### 2026-10-27 - Khắc phục tràn Hero, danh mục và phân trang trên mobile
- Yêu cầu: tránh cắt mô tả/chồng nút trong Hero, tràn danh mục Game và phân trang nhiều dòng trên màn hình dưới 768px.
- Thực hiện: thu nhỏ chữ Hero, xếp dọc CTA, buộc danh mục cuộn ngang trong khung và giới hạn phân trang mobile còn Trước/trang hiện tại/Sau trên một hàng.

### 2026-10-27 - Hiển thị giá khuyến mãi và tìm kiếm tại trang chi nhánh
- Yêu cầu: đồng bộ giá gốc/giá khuyến mãi trên trang chủ và chi nhánh, thêm tìm sản phẩm theo tên trong chi nhánh, và sửa fallback ảnh SUPPLY.
- Thực hiện: Home và PublicShop bulk lấy giá hiệu lực qua PromotionService, giữ giá gốc để gạch ngang khi được giảm; thêm keyword GET vào truy vấn phân trang công khai; chỉ chọn ảnh ProductImage is_main cho danh sách phụ kiện và dùng placeholder-supply khi thiếu ảnh. Bổ sung kiểm thử service, controller, repository; không sửa Entity, SecurityConfig hoặc pom.xml.

### 2026-10-27 - Khôi phục nền sáng cho form và bảng Admin/Manager
- Yêu cầu: đưa các panel nhập liệu và bảng quản trị về nền sáng, giữ màu thương hiệu ở nút và trạng thái focus, cải thiện select nhiều sản phẩm.
- Thực hiện: CSS chung chỉ chuyển panel chứa form/bảng và các vùng form quản trị sang nền trắng, chữ tối, viền trung tính; giữ nền Midnight Rare cho topbar, sidebar và dashboard thống kê/biểu đồ. Input/select/date dùng nền sáng, focus tím, nút lưu tím/nút vàng và nút quay lại xám; multi-select tăng chiều cao và khoảng cách option. Không sửa logic, Entity, SecurityConfig hoặc pom.xml.

## Đồng bộ phân trang trang Series

Thay vòng lặp phân trang riêng trên trang sản phẩm Series bằng fragment `fragments/pagination` dùng chung. Giữ nguyên nội dung và giá sản phẩm; tái sử dụng cửa sổ trang gọn, dấu ba chấm và cơ chế giữ query parameters của fragment. Không sửa Entity, SecurityConfig, pom.xml; không chạy test/build, không commit/push.

## Khôi phục bố cục Footer trang Series

Kiểm tra template trang Series: thẻ HTML cân bằng, dùng `layout/main.html`, fragment phân trang nằm trong main; CSS không có min-height lớn và padding lưới 30px chỉ tạo khoảng cách nhỏ. Bổ sung `flex-grow-1` cho vùng nội dung để khớp layout flex chung, bỏ margin Bootstrap thừa bọc phân trang vì fragment đã tự đặt khoảng cách. Không sửa Entity, SecurityConfig, pom.xml; không chạy test/build, không commit/push.
## Bộ lọc kết hợp trên trang Chi nhánh công khai

Đọc AGENTS.md và toàn bộ docs/. Mở rộng trang `/shops/{id}` với panel responsive lọc tên, Game, Series, độ hiếm, giá từ/đến và loại sản phẩm; giữ tương thích tham số `type` cũ. Kết hợp điều kiện lọc trong query phân trang và count, giữ chỉ hiển thị Shop đã duyệt, Product đang bán/chưa xóa và Catalog active; khi đổi Game, gọi API Series hiện có. Giá lọc theo giá niêm yết, còn giá trên card tiếp tục dùng giá hiệu lực. Thêm xử lý lỗi khoảng giá, cập nhật kiểm thử nguồn. Không sửa Entity, SecurityConfig, pom.xml; không chạy test/build, không commit/push.

### Đồng bộ giá khuyến mãi và lọc chi nhánh trên Series, Tìm kiếm và Trang chủ
- Yêu cầu: hiển thị giá gốc/giá hiệu lực trên Series và Tìm kiếm; giữ tìm kiếm gần đúng không phân biệt hoa thường; lọc kết quả theo chi nhánh; hiển thị các chi nhánh nổi bật tại Trang chủ.
- Thực hiện: tính giá hiệu lực theo bulk PromotionService cho các sản phẩm trong trang, chọn giá khuyến mãi thấp nhất theo nhóm SINGLE; thêm shopId vào bộ lọc công khai, dropdown và liên kết phân trang/top tabs; truy vấn danh sách chi nhánh đã duyệt, chưa xóa để dùng cho bộ lọc và Trang chủ. Thêm giá trị tương thích cho DTO và cập nhật kiểm thử nguồn. Không sửa Entity, SecurityConfig hoặc pom.xml; không chạy test, không commit/push.

### Ngăn importer chuyển sản phẩm sang shop có chủ và khôi phục dữ liệu Quận 6
- Yêu cầu: bảo đảm cập nhật hồ sơ chỉ sửa shop hiện tại; ngăn sản phẩm bị chuyển sang shop YuGiOh mồ côi và chuẩn bị khôi phục liên kết shop_id.
- Thực hiện: cập nhật hồ sơ trên Shop entity đang được quản lý bằng dirty checking, không gọi save; importer Yu-Gi-Oh chỉ chọn shop khớp tên khi shop chưa xóa và không có chủ sở hữu, nếu không thì tạo shop dữ liệu mẫu riêng. Tạo SQL preview và khôi phục có điều kiện chỉ khi shop địa chỉ Quận 6 và shop YuGiOh mồ côi đều duy nhất; shop mồ côi được soft-delete để giữ lịch sử tham chiếu. Không sửa Entity, SecurityConfig, pom.xml; không chạy test, không commit/push.
