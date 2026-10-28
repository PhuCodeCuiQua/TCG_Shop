# Kịch bản quay video demo TCG Shop

Kịch bản chia theo phần việc của Phú (PU), Phong (PG) và Phát (PA). Các thao tác ghi tên nút theo giao diện hiện có; một số màn hình chỉ có nút hành động khi dữ liệu ở đúng trạng thái.

## Chuẩn bị trước khi quay

- Khởi động ứng dụng và kiểm tra trang chủ hoạt động; dùng dữ liệu demo đã được nhóm chuẩn bị.
- Chuẩn bị riêng các tài khoản USER, VENDOR, MANAGER/ADMIN và SHIPPER có quyền phù hợp. Không dùng chung tài khoản giữa các phần; đăng xuất trước khi đổi vai trò.
- Chuẩn bị sản phẩm còn hàng, địa chỉ nhận hàng, coupon còn hiệu lực, đơn hàng ở các trạng thái phù hợp, hồ sơ shop đang chờ duyệt và một shipper đang hoạt động. Chỉ quay các nhánh có dữ liệu thật đã chuẩn bị.
- Đăng ký tài khoản cần email có thể nhận OTP. Dùng tài khoản ADMIN riêng để tạo mã giảm giá hệ thống; không đưa mật khẩu, OTP, token hoặc thông tin cá nhân thật lên video.
- Các thao tác tạo/sửa/duyệt/đặt hàng làm thay đổi dữ liệu. Nếu cần quay nhiều lần, chuẩn bị bản ghi demo riêng hoặc làm mới dữ liệu theo cách nhóm đã thống nhất.
- Mọi thông tin trong ngoặc vuông như `[tài khoản USER demo]`, `[mã coupon hợp lệ]` là chỗ điền dữ liệu đã cấp; không phải tài khoản/mã mặc định của hệ thống.

---

## PHẦN 1: KỊCH BẢN CỦA PHÚ (PU)

### Bước 1: Đăng ký, xác thực OTP và đăng nhập

- **Thao tác trên web:** Mở `/auth/register`, điền thông tin bằng email demo có thể nhận thư rồi gửi biểu mẫu đăng ký. Theo điều hướng của ứng dụng đến màn hình xác thực, nhập OTP nhận được và xác nhận. Sau khi tài khoản được kích hoạt, mở `/auth/login`, nhập tài khoản vừa tạo và đăng nhập.
- **Lời thoại:** “Em bắt đầu từ quy trình tạo tài khoản. Sau khi nhập thông tin, hệ thống yêu cầu xác thực OTP gửi đến email trước khi tài khoản có thể đăng nhập. Tiếp theo, em đăng nhập bằng tài khoản đã xác thực để sử dụng các chức năng dành cho người mua. Thầy/Cô có thể thấy thông báo trạng thái ngay trên giao diện.”
- **File code minh họa:** `src/main/java/com/tcgshop/controller/web/AuthController.java`; `src/main/resources/templates/auth/register.html`; `src/main/resources/templates/auth/verify-otp.html`; `src/main/resources/templates/auth/login.html`.

### Bước 2: Thêm sản phẩm vào giỏ hàng

- **Thao tác trên web:** Đăng nhập bằng `[tài khoản USER demo]`, mở trang sản phẩm đã chuẩn bị, chọn số lượng còn hàng rồi bấm nút thêm vào giỏ. Mở biểu tượng giỏ hàng hoặc `/cart`; chỉ ra sản phẩm, số lượng và tạm tính.
- **Lời thoại:** “Ở phía người mua, em chọn sản phẩm và thêm vào giỏ. Giỏ hàng hiển thị mặt hàng cùng số lượng để người mua rà soát trước khi thanh toán. Số lượng phải phù hợp với tồn kho hiện tại.”
- **File code minh họa:** `src/main/java/com/tcgshop/controller/web/CartController.java`; `src/main/java/com/tcgshop/service/impl/CartServiceImpl.java`; `src/main/resources/templates/user/cart.html`.

### Bước 3: Kiểm tra giỏ và đi đến thanh toán

- **Thao tác trên web:** Tại `/cart`, chọn mặt hàng cần mua (hoặc chọn tất cả nếu muốn demo toàn bộ giỏ), cập nhật số lượng nếu cần rồi tiếp tục đến trang thanh toán. Trên `/checkout`, điền thông tin người nhận, số điện thoại và địa chỉ đã chuẩn bị; nếu dùng coupon thì nhập `[mã coupon hợp lệ]` và bấm nút áp dụng/kiểm tra mã để xem phần tổng kết.
- **Lời thoại:** “Trước khi đặt hàng, em chọn đúng các sản phẩm cần mua và kiểm tra thông tin giao hàng. Nếu có mã giảm giá, em nhập mã để hệ thống kiểm tra điều kiện và cập nhật số tiền dự kiến. Phần xem trước giúp người mua kiểm tra lại đơn trước khi gửi.”
- **File code minh họa:** `src/main/java/com/tcgshop/controller/web/CheckoutController.java`; `src/main/java/com/tcgshop/service/impl/CheckoutServiceImpl.java`; `src/main/resources/static/js/checkout.js`; `src/main/resources/templates/user/cart.html`; `src/main/resources/templates/user/checkout.html`.

### Bước 4: Đặt hàng với COD

- **Thao tác trên web:** Trên trang checkout, chọn phương thức COD, rà soát tổng tiền và bấm nút đặt hàng. Chờ trang kết quả hiện mã nhóm đơn/mã đơn; mở chi tiết nếu có liên kết.
- **Lời thoại:** “Với COD, em xác nhận đặt hàng sau khi kiểm tra thông tin. Hệ thống xử lý đơn theo từng chi nhánh trong trường hợp giỏ có sản phẩm từ nhiều shop, rồi hiển thị kết quả đặt hàng để người mua tiếp tục theo dõi.”
- **File code minh họa:** `src/main/java/com/tcgshop/service/impl/OrderPlacementServiceImpl.java`; `src/main/java/com/tcgshop/service/OrderService.java`; `src/main/resources/templates/user/checkout-success.html`.

### Bước 5: Trình diễn nhánh thanh toán MoMo giả lập

- **Thao tác trên web:** Dùng giỏ/đơn demo đã chuẩn bị để vào `/checkout` lần nữa. Chọn MoMo rồi xác nhận đặt hàng. Trong hộp thoại thanh toán hiện ra, trình bày QR/màn hình mô phỏng; bấm **“Xác nhận thanh toán”** để trình diễn nhánh hoàn tất, hoặc quay riêng một lượt và bấm **“Hủy”** để minh họa nhánh hủy.
- **Lời thoại:** “Ngoài COD, giao diện còn có nhánh MoMo. Trong phiên bản demo này, đây là luồng thanh toán giả lập trong ứng dụng, không phải kết nối giao dịch MoMo thật. Em xác nhận trên hộp thoại để minh họa kết quả mà hệ thống xử lý.”
- **File code minh họa:** `src/main/resources/static/js/checkout.js`; `src/main/java/com/tcgshop/controller/web/CheckoutController.java`; `src/main/java/com/tcgshop/service/impl/OrderPlacementServiceImpl.java`.

### Bước 6: Xem danh sách và chi tiết đơn hàng

- **Thao tác trên web:** Mở `/orders`, chọn đơn vừa tạo và bấm xem chi tiết. Trình bày mã đơn, shop, sản phẩm, tổng tiền và trạng thái đang hiển thị. Nếu có dữ liệu phù hợp, mở một đơn ở trạng thái khác để so sánh.
- **Lời thoại:** “Sau khi đặt hàng, người mua có thể tra cứu danh sách và mở chi tiết để xem nội dung cũng như trạng thái đơn. Trạng thái được cập nhật theo luồng xử lý đơn, thay vì người mua tự sửa trực tiếp.”
- **File code minh họa:** `src/main/java/com/tcgshop/controller/web/UserOrderController.java`; `src/main/java/com/tcgshop/service/OrderService.java`; template danh sách/chi tiết đơn trong `src/main/resources/templates/user/`.

---

## PHẦN 2: KỊCH BẢN CỦA PHONG (PG)

### Bước 1: Giới thiệu giao diện, layout và khả năng responsive

- **Thao tác trên web:** Mở trang chủ ở cửa sổ trình duyệt rộng, giới thiệu header/menu, nội dung, footer và toast nếu đang có thông báo. Thu nhỏ cửa sổ hoặc dùng chế độ responsive của trình duyệt để trình bày cách bố cục chuyển theo kích thước màn hình; không kết luận về thiết bị chưa kiểm tra.
- **Lời thoại:** “Em phụ trách nhiều phần giao diện dùng chung của TCG Shop. Layout thống nhất phần đầu trang, nội dung, chân trang và các thông báo. Khi thu hẹp khung nhìn, các thành phần được bố trí lại để phù hợp với màn hình nhỏ.”
- **File code minh họa:** `src/main/resources/templates/layout/main.html`; `src/main/resources/templates/layout/dashboard.html`; `src/main/resources/templates/fragments/header.html`; `src/main/resources/templates/fragments/footer.html`; `src/main/resources/static/css/custom.css`; `src/main/resources/static/js/common.js`.

### Bước 2: Gửi đăng ký mở shop

- **Thao tác trên web:** Đăng nhập bằng `[tài khoản USER demo chưa có shop]`, mở `/seller-registration`, điền các trường bắt buộc và thông tin shop bằng dữ liệu demo, tải ảnh nếu biểu mẫu có trường ảnh rồi gửi đăng ký. Dừng ở thông báo/kết quả tiếp nhận; việc duyệt được minh họa bằng tài khoản quản trị ở phần PA hoặc dùng tài khoản vendor đã được duyệt để quay các bước kế tiếp.
- **Lời thoại:** “Người dùng có thể gửi hồ sơ để đăng ký trở thành người bán. Hồ sơ cần được xử lý theo trạng thái trước khi tài khoản dùng các chức năng vận hành shop. Để video không phải chờ duyệt, em chuẩn bị sẵn một tài khoản vendor hợp lệ cho các màn hình tiếp theo.”
- **File code minh họa:** `src/main/java/com/tcgshop/controller/web/SellerRegistrationController.java`; `src/main/java/com/tcgshop/service/impl/ShopRegistrationServiceImpl.java`; `src/main/resources/templates/seller-registration/register.html`; `src/main/resources/templates/seller-registration/status.html`; `src/main/resources/static/js/seller-registration.js`.

### Bước 3: Tìm thẻ catalog và quản lý sản phẩm shop

- **Thao tác trên web:** Đăng nhập bằng `[tài khoản VENDOR demo đã được duyệt]`, mở `/vendor/products`, chọn chức năng tạo sản phẩm/thẻ. Ở trang tìm thẻ gốc, nhập tên hoặc số thẻ đã có trong catalog, chọn series nếu cần rồi bấm **“Tìm kiếm”**; chọn một thẻ bằng **“Chọn thẻ này”**. Điền thông tin đăng bán và tải ảnh sản phẩm để giới thiệu phần preview nếu được hiển thị; lưu theo nút trên biểu mẫu.
- **Lời thoại:** “Khi đăng bán, vendor có thể tìm thẻ theo tên hoặc số thẻ, lọc theo series rồi chọn đúng bản ghi catalog. Sau đó em bổ sung thông tin sản phẩm của shop. Ảnh tải lên được dùng để trình bày sản phẩm; dịch vụ lưu ảnh được tích hợp với Cloudinary.”
- **File code minh họa:** `src/main/java/com/tcgshop/controller/web/VendorProductController.java`; `src/main/java/com/tcgshop/controller/web/VendorSingleProductController.java`; `src/main/java/com/tcgshop/service/impl/VendorSingleProductServiceImpl.java`; `src/main/resources/templates/vendor/product-single-search.html`; `src/main/resources/templates/vendor/product-single-form.html`; `src/main/java/com/tcgshop/service/impl/CloudinaryServiceImpl.java`.

### Bước 4: Tạo và quản lý khuyến mãi chi nhánh

- **Thao tác trên web:** Mở `/vendor/promotions`, bấm **“Tạo chương trình”**, nhập thông tin khuyến mãi theo form và lưu. Quay lại danh sách để chỉ ra trạng thái, thời gian áp dụng; với chương trình demo có sẵn, có thể mở sửa hoặc dùng nút bật/tắt để minh họa hành động.
- **Lời thoại:** “Vendor có thể tạo chương trình khuyến mãi cho chi nhánh, theo dõi thời gian và trạng thái, đồng thời bật hoặc tắt chương trình từ danh sách. Em dùng dữ liệu đã chuẩn bị để thể hiện màn hình mà không tạo ưu đãi ngoài kế hoạch.”
- **File code minh họa:** `src/main/java/com/tcgshop/controller/web/VendorPromotionController.java`; `src/main/java/com/tcgshop/service/impl/VendorPromotionServiceImpl.java`; `src/main/resources/templates/vendor/promotions.html`; `src/main/resources/templates/vendor/promotion-form.html`; `src/main/resources/static/js/vendor-promotions.js`.

### Bước 5: Tiếp nhận và xem đơn hàng vendor

- **Thao tác trên web:** Mở `/vendor/orders`, chọn tab trạng thái có đơn demo, nhập mã đơn vào ô tìm kiếm nếu cần và bấm **“Tìm kiếm”**. Bấm **“Xem chi tiết”** để trình bày thông tin đơn. Nếu có hành động xử lý phù hợp với trạng thái của đơn, chỉ thực hiện khi đã thống nhất dữ liệu quay.
- **Lời thoại:** “Màn hình vendor tập trung các đơn thuộc chi nhánh và cho phép lọc theo nhóm trạng thái hoặc tìm theo mã đơn. Em mở chi tiết để xem dữ liệu phục vụ quá trình xử lý; những hành động chuyển trạng thái phụ thuộc trạng thái hiện tại của đơn.”
- **File code minh họa:** `src/main/java/com/tcgshop/controller/web/VendorOrderController.java`; `src/main/java/com/tcgshop/service/impl/VendorOrderServiceImpl.java`; `src/main/resources/templates/vendor/orders.html`; `src/main/resources/templates/vendor/order-detail.html`.

### Bước 6: Xem báo cáo doanh thu

- **Thao tác trên web:** Mở `/vendor/revenue`, giới thiệu các chỉ số doanh thu cơ sở, hoa hồng, thực nhận, số đơn hoàn tất và giá trị đơn trung bình. Chọn khoảng **“Từ ngày”**, **“Đến ngày”**, bấm **“Lọc”**; thử chuyển nhóm **“Day”**/ **“Month”** để trình bày biểu đồ nếu có dữ liệu.
- **Lời thoại:** “Báo cáo này tổng hợp từ snapshot của các đơn đã giao thành công. Vendor có thể lọc khoảng thời gian, xem doanh thu và các biểu đồ theo dữ liệu của chính chi nhánh. Nếu dữ liệu demo chưa có đơn hoàn tất, giao diện sẽ thể hiện trạng thái chưa có dữ liệu.”
- **File code minh họa:** `src/main/java/com/tcgshop/controller/web/VendorRevenueController.java`; `src/main/java/com/tcgshop/controller/rest/VendorRevenueApiController.java`; `src/main/java/com/tcgshop/service/impl/VendorRevenueServiceImpl.java`; `src/main/resources/templates/vendor/revenue.html`.

### Bước 7: Theo dõi đơn ở vai trò shipper

- **Thao tác trên web:** Đăng xuất, đăng nhập bằng `[tài khoản SHIPPER demo]`, mở `/shipper/orders` và chọn tab đang có đơn được phân công. Mở **“Xem chi tiết”** để trình bày điểm lấy/giao và COD. Chỉ khi đơn ở trạng thái tương ứng mới bấm **“Đã lấy hàng”**, **“Bắt đầu giao”** hoặc hành động kết quả giao hàng đang hiển thị.
- **Lời thoại:** “Ở vai trò shipper, danh sách chỉ hiển thị các đơn được phân công. Chi tiết đơn cho biết shop, người nhận, địa chỉ và số COD nếu có. Các nút cập nhật xuất hiện theo trạng thái, giúp mô tả tiến trình giao nhận theo đúng trình tự.”
- **File code minh họa:** `src/main/java/com/tcgshop/controller/web/ShipperOrderController.java`; `src/main/java/com/tcgshop/controller/rest/ShipperOrderStatusRestController.java`; `src/main/resources/templates/shipper/orders.html`; `src/main/resources/templates/shipper/order-detail.html`; `src/main/resources/static/js/shipper-order-status.js`.

### Bước 8: Chatbot và sơ đồ Archify

- **Thao tác trên web:** Đăng nhập để hiện nút trợ lý, bấm nút mở chatbot, nhập một câu hỏi ngắn đã chuẩn bị và gửi; trình bày phản hồi, thao tác xóa lịch sử hoặc đóng khung chat. Sau đó mở file sơ đồ đã tạo ở trình duyệt: `D:\LTWeb\tcgshop-architecture.html` và `D:\LTWeb\tcgshop-checkout-workflow.html`.
- **Lời thoại:** “Em kết thúc phần giao diện bằng trợ lý TCG Shop. Đây là trợ lý tự động theo luồng xử lý có sẵn, không phải AI tạo sinh. Hai file HTML tiếp theo là sơ đồ kiến trúc tổng quan và workflow checkout được tạo từ việc đối chiếu mã nguồn, giúp nhóm trình bày các thành phần và luồng nghiệp vụ.”
- **File code minh họa:** `src/main/resources/templates/fragments/chatbot-widget.html`; `src/main/resources/static/js/chatbot.js`; `src/main/java/com/tcgshop/controller/rest/ChatbotRestController.java`; `D:\LTWeb\tcgshop-architecture.html`; `D:\LTWeb\tcgshop-checkout-workflow.html`.

---

## PHẦN 3: KỊCH BẢN CỦA PHÁT (PA)

### Bước 1: Tổng quan quản trị và quản lý người dùng

- **Thao tác trên web:** Đăng nhập bằng `[tài khoản MANAGER/ADMIN demo]`, mở trang tổng quan/quản lý từ menu quản trị. Mở **Quản lý người dùng**, nhập tên hoặc email demo, chọn vai trò/trạng thái và bấm **“Tìm kiếm”**. Mở hồ sơ người dùng bằng **“Xem”**; chỉ minh họa khóa/mở khóa trên tài khoản demo không phải tài khoản đang đăng nhập.
- **Lời thoại:** “Em phụ trách các màn hình quản trị và vận hành. Tại danh sách người dùng, quản trị viên có thể lọc theo từ khóa, vai trò và trạng thái. Các thao tác thay đổi trạng thái có bước xác nhận để tránh thao tác nhầm.”
- **File code minh họa:** `src/main/java/com/tcgshop/controller/web/ManagerUserController.java`; `src/main/java/com/tcgshop/controller/rest/ManagerUserRestController.java`; `src/main/resources/templates/manager/users/list.html`; `src/main/resources/static/js/manager-users.js`.

### Bước 2: Duyệt hồ sơ shop

- **Thao tác trên web:** Mở `/manager/shops`, lọc trạng thái **Đang chờ duyệt** rồi bấm **“Lọc”**. Bấm **“Xem hồ sơ”** ở hồ sơ demo, kiểm tra chủ shop và thông tin đăng ký; bấm **“Duyệt chi nhánh”** và xác nhận. Có thể quay riêng nhánh từ chối bằng hồ sơ khác: nhập lý do rồi bấm **“Từ chối hồ sơ”**.
- **Lời thoại:** “Hồ sơ shop được quản trị viên xem xét trước khi hoạt động. Em kiểm tra thông tin rồi duyệt hồ sơ demo; khi từ chối, hệ thống yêu cầu nhập lý do. Trạng thái hồ sơ quyết định các chức năng shop được phép sử dụng.”
- **File code minh họa:** `src/main/java/com/tcgshop/controller/web/ManagerShopReviewController.java`; `src/main/java/com/tcgshop/service/impl/ShopReviewServiceImpl.java`; `src/main/resources/templates/manager/shops/list.html`; `src/main/resources/templates/manager/shops/detail.html`; `src/main/resources/static/js/manager-shop-review.js`.

### Bước 3: Quản lý catalog game và series

- **Thao tác trên web:** Mở `/manager/games`, tìm game demo hoặc dùng **“Thêm game”** nếu có dữ liệu tạo mới đã chuẩn bị. Dùng **“Series”** để xem/quản lý series của game; giới thiệu lọc theo tên/mã và trạng thái. Chỉ dùng **“Ẩn/Hiện”** hoặc **“Xóa”** trên bản ghi demo phù hợp, không xóa dữ liệu dùng chung.
- **Lời thoại:** “Catalog game và series là dữ liệu nền để chuẩn hóa các thẻ mà shop đăng bán. Quản trị viên có thể tìm, thêm, sửa và điều chỉnh trạng thái hiển thị. Khi bản ghi đã có dữ liệu liên quan, hệ thống có thể giới hạn hành vi xóa để bảo toàn quan hệ.”
- **File code minh họa:** `src/main/java/com/tcgshop/controller/web/GameCatalogController.java`; `src/main/java/com/tcgshop/controller/web/SeriesCatalogController.java`; `src/main/java/com/tcgshop/service/impl/GameCatalogServiceImpl.java`; `src/main/java/com/tcgshop/service/impl/SeriesCatalogServiceImpl.java`; `src/main/resources/templates/manager/games/list.html`; `src/main/resources/static/js/manager-catalog-actions.js`.

### Bước 4: Xem lịch sử và biểu đồ giá

- **Thao tác trên web:** Mở `/manager/price-history`, chọn/tìm mặt hàng đã có dữ liệu lịch sử theo các bộ lọc đang hiển thị rồi xem kết quả/biểu đồ. Nếu demo có trang tra cứu giá cho người dùng, mở trang đó từ menu tương ứng và chọn thẻ đã chuẩn bị để so sánh giá.
- **Lời thoại:** “Phần giá thị trường giúp theo dõi dữ liệu lịch sử của thẻ. Em chọn một thẻ có dữ liệu để trình bày kết quả trên màn hình. Biểu đồ chỉ có ý nghĩa khi dữ liệu lịch sử đã được chuẩn bị, nên em không tạo số liệu minh họa ngoài dữ liệu hệ thống.”
- **File code minh họa:** `src/main/java/com/tcgshop/controller/web/ManagerPriceHistoryController.java`; `src/main/java/com/tcgshop/controller/web/CheckMyCardController.java`; `src/main/java/com/tcgshop/controller/rest/PriceChartRestController.java`; `src/main/java/com/tcgshop/service/impl/PriceHistoryServiceImpl.java`; `src/main/resources/templates/manager/price-history/list.html`; `src/main/resources/templates/user/check-my-card.html`.

### Bước 5: Phân công shipper và xem đơn vận hành

- **Thao tác trên web:** Mở `/manager/shipper-assignment`, chọn tab **“Chưa phân công”**, tìm theo mã đơn/tên người nhận nếu cần. Ở đơn demo, chọn `[shipper đang hoạt động]` trong danh sách rồi bấm **“Gán”**. Nếu quay nhánh thay shipper, mở tab **“Đã phân công, chưa lấy hàng”**, chọn shipper khác và bấm **“Đổi”**.
- **Lời thoại:** “Quản trị viên phân công đơn cho shipper đang hoạt động. Danh sách tách đơn chưa phân công và đơn đã gán nhưng chưa lấy hàng; nếu cần, quản trị viên có thể đổi người được phân công trước khi đơn được lấy.”
- **File code minh họa:** `src/main/java/com/tcgshop/controller/web/ShipperAssignmentController.java`; `src/main/java/com/tcgshop/service/impl/ShipperAssignmentServiceImpl.java`; `src/main/java/com/tcgshop/service/OrderService.java`; `src/main/resources/templates/manager/shipper-assignment/list.html`.

### Bước 6: Tạo mã giảm giá phạm vi nền tảng

- **Thao tác trên web:** Đăng nhập bằng `[tài khoản ADMIN demo]`, mở `/admin/platform-coupons`, bấm **“Tạo mã giảm giá”**. Nhập mã demo chưa sử dụng, chọn loại giảm, giá trị, điều kiện đơn tối thiểu, giới hạn lượt dùng/trạng thái và thời gian bắt đầu-kết thúc hợp lệ; bấm **“Lưu mã giảm giá”**. Trở về danh sách, dùng bộ lọc trạng thái hoặc mở sửa/ngừng mã demo khi cần.
- **Lời thoại:** “Mã ở đây thuộc phạm vi nền tảng, khác với khuyến mãi do từng chi nhánh quản lý. Quản trị viên cấu hình kiểu giảm, điều kiện và thời hạn; khi checkout, dịch vụ coupon kiểm tra khả năng áp dụng và tính số tiền giảm.”
- **File code minh họa:** `src/main/java/com/tcgshop/controller/web/PlatformCouponAdminController.java`; `src/main/java/com/tcgshop/service/impl/PlatformCouponAdminServiceImpl.java`; `src/main/java/com/tcgshop/service/impl/CouponServiceImpl.java`; `src/main/resources/templates/manager/platform-coupons/list.html`; `src/main/resources/templates/manager/platform-coupons/form.html`.

---

## Ghi chú khi quay và trình bày giới hạn

- Nếu một nút không xuất hiện, kiểm tra vai trò đăng nhập và trạng thái bản ghi/đơn; không mô tả đó là lỗi nếu hành động không hợp lệ ở trạng thái hiện tại.
- Luồng MoMo trên video là **giả lập**, không xác nhận giao dịch với cổng thanh toán thật.
- Một số hạng mục PA đã được ghi nhận là còn giới hạn trong tài liệu dự án: lưu khu vực shipper, ngày hiệu lực commission, upload logo carrier và phụ thuộc dữ liệu lịch sử xem. Không trình bày các mục này như tính năng đã hoàn thiện; nếu được hỏi, nêu rõ trạng thái theo tài liệu blocker hiện hành.
- Nếu không có dữ liệu cho biểu đồ, doanh thu hoặc danh sách, có thể quay trạng thái rỗng thực tế thay vì tạo kết quả giả.
