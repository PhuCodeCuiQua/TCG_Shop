# Danh sách kịch bản kiểm thử TCG Shop

Tài liệu này là checklist kiểm thử thủ công theo thứ tự mã TC, chia theo phần việc PU (Phú), PG (Phong) và PA (Phát) trong tài liệu dự án. Trọng tâm là luồng mua hàng từ giỏ đến đặt đơn, thanh toán COD/MoMo giả lập và các bước xử lý đơn liên quan. Các ID, email, mã coupon và số tiền dưới đây là dữ liệu fixture minh họa; thay bằng bản ghi đang có trong môi trường kiểm thử.

## Quy ước chuẩn bị dữ liệu

- Tạo tối thiểu hai tài khoản USER khác nhau (A và B), một Vendor sở hữu Shop đã `APPROVED`, một Manager/Admin và một Shipper `ACTIVE` có `ROLE_SHIPPER`.
- Tạo sản phẩm ACTIVE còn hàng ở Shop đã duyệt; chuẩn bị thêm sản phẩm hết hàng/INACTIVE, Shop chưa duyệt hoặc đã xóa, và sản phẩm thuộc Shop của chính người mua.
- Với kiểm thử nhiều shop, chuẩn bị ít nhất hai sản phẩm được chọn thuộc hai Shop khác nhau và một sản phẩm không được chọn.
- Chuẩn bị địa chỉ mặc định hợp lệ cho USER A; thêm trường hợp không có địa chỉ và địa chỉ thuộc USER B.
- Chuẩn bị carrier ACTIVE có phí thường và ngưỡng miễn phí, carrier INACTIVE, cùng coupon shop/coupon sàn ở các trạng thái hợp lệ và không hợp lệ.
- Với luồng đổi trả, cần đơn `DELIVERED` còn hạn, đơn đã hết hạn và đơn có yêu cầu trả hàng `PENDING`/`REJECTED`/`ESCALATED`.
- Dùng request có CSRF hợp lệ cho mọi thao tác ghi qua trình duyệt/AJAX. Không sửa dữ liệu trực tiếp giữa các bước, trừ khi kịch bản yêu cầu kiểm tra thay đổi tồn kho/trạng thái từ một phiên khác.

## A. Xác thực và phiên người dùng — PU

### TC-01 — Đăng ký tài khoản hợp lệ
- **Mã Test Case:** TC-01
- **Tên chức năng:** Đăng ký
- **Điều kiện tiên quyết:** Email chưa được sử dụng.
- **Các bước thực hiện:** 1. Mở `/auth/register`. 2. Nhập họ tên, email, số điện thoại hợp lệ, mật khẩu mạnh và xác nhận trùng khớp. 3. Gửi biểu mẫu.
- **Dữ liệu test (nếu có):** Email mới; điện thoại `0912345678`; mật khẩu đạt StrongPassword.
- **Kết quả mong đợi:** Tài khoản được tạo ở trạng thái chờ kích hoạt, chỉ có vai trò USER; chuyển tới trang nhập OTP và thông báo mã đã được gửi.

### TC-02 — Validation dữ liệu đăng ký
- **Mã Test Case:** TC-02
- **Tên chức năng:** Kiểm tra dữ liệu đăng ký
- **Điều kiện tiên quyết:** Đang ở form đăng ký.
- **Các bước thực hiện:** 1. Lần lượt gửi form bỏ trống trường bắt buộc, email sai định dạng, số điện thoại sai, mật khẩu yếu, xác nhận mật khẩu không khớp và họ tên dài hơn giới hạn. 2. Quan sát form sau mỗi lần gửi.
- **Dữ liệu test (nếu có):** Email `abc`, điện thoại `123`, mật khẩu thiếu chữ hoa/số, confirm khác password; họ tên trên 100 ký tự.
- **Kết quả mong đợi:** Form báo lỗi đúng trường, không tạo tài khoản; password và confirmPassword không được giữ lại trong form khi render lỗi.

### TC-03 — Email đã đăng ký hoặc tài khoản chờ kích hoạt
- **Mã Test Case:** TC-03
- **Tên chức năng:** Đăng ký trùng email
- **Điều kiện tiên quyết:** Có email ACTIVE và một email INACTIVE đã đăng ký.
- **Các bước thực hiện:** 1. Đăng ký lại bằng email ACTIVE. 2. Thử đăng ký bằng email INACTIVE.
- **Dữ liệu test (nếu có):** Hai email nêu trong điều kiện tiên quyết.
- **Kết quả mong đợi:** Email ACTIVE bị từ chối vì đã sử dụng; tài khoản INACTIVE được cập nhật thông tin đăng ký và cấp OTP mới nếu không vi phạm thời gian chờ gửi lại; tài khoản BLOCKED không được biến thành tài khoản đăng ký dở.

### TC-04 — Xác thực OTP đúng, sai, hết hạn và giới hạn lần thử
- **Mã Test Case:** TC-04
- **Tên chức năng:** Kích hoạt tài khoản bằng OTP
- **Điều kiện tiên quyết:** Có tài khoản INACTIVE với OTP đăng ký chưa dùng.
- **Các bước thực hiện:** 1. Gửi OTP sai. 2. Lặp lại đến giới hạn `app.otp.max-attempts`. 3. Yêu cầu OTP mới sau khi hết cooldown rồi gửi OTP đã hết hạn. 4. Với OTP mới hợp lệ, xác thực lại.
- **Dữ liệu test (nếu có):** OTP sai, OTP hết hạn, OTP hợp lệ 6 chữ số (đọc từ hộp thư kiểm thử).
- **Kết quả mong đợi:** OTP sai tăng bộ đếm và bị từ chối; quá số lần thì mã bị vô hiệu hóa và phải xin mã mới; OTP hết hạn/đã dùng không kích hoạt tài khoản; OTP hợp lệ đổi tài khoản sang ACTIVE, emailVerified=true và chuyển về đăng nhập.

### TC-05 — Cooldown gửi lại OTP và phản hồi không tiết lộ email
- **Mã Test Case:** TC-05
- **Tên chức năng:** Gửi lại OTP / quên mật khẩu
- **Điều kiện tiên quyết:** Có tài khoản INACTIVE, ACTIVE và email không tồn tại.
- **Các bước thực hiện:** 1. Gửi lại OTP hai lần trong khoảng cooldown. 2. Gửi yêu cầu quên mật khẩu cho từng email. 3. So sánh thông báo hiển thị.
- **Dữ liệu test (nếu có):** Email tồn tại và email không tồn tại.
- **Kết quả mong đợi:** OTP đăng ký chỉ được phát lại sau cooldown cấu hình; endpoint gửi lại OTP không xác nhận email có tồn tại hay không; yêu cầu quên mật khẩu luôn trả thông báo chung, không tiết lộ email/cooldown.

### TC-06 — Đăng nhập theo trạng thái tài khoản
- **Mã Test Case:** TC-06
- **Tên chức năng:** Đăng nhập và điều hướng theo vai trò
- **Điều kiện tiên quyết:** Có tài khoản ACTIVE, INACTIVE, BLOCKED và tài khoản bị khóa tạm thời; có nhiều vai trò mẫu nếu môi trường hỗ trợ.
- **Các bước thực hiện:** 1. Đăng nhập từng tài khoản bằng mật khẩu đúng. 2. Thử mật khẩu sai và email không tồn tại. 3. Gửi `returnUrl` hợp lệ rồi gửi URL ngoài hệ thống hoặc có `//`.
- **Dữ liệu test (nếu có):** `returnUrl=/cart`; URL ngoài `https://example.invalid`.
- **Kết quả mong đợi:** ACTIVE nhận cookie access/refresh và chuyển theo returnUrl nội bộ an toàn hoặc đích theo role; mustChangePassword chuyển tới `/profile/change-password`; INACTIVE được hướng tới xác thực OTP; BLOCKED/đang bị khóa bị từ chối; thông tin đăng nhập sai không giữ password và không phân biệt email có tồn tại.

### TC-07 — Đổi mật khẩu, đăng xuất và refresh token
- **Mã Test Case:** TC-07
- **Tên chức năng:** Quản lý phiên xác thực
- **Điều kiện tiên quyết:** USER A đăng nhập, có refresh token hợp lệ.
- **Các bước thực hiện:** 1. Đổi mật khẩu với mật khẩu hiện tại sai. 2. Thử dùng lại mật khẩu cũ làm mật khẩu mới. 3. Đổi thành công. 4. Thử refresh bằng token cũ, refresh token hợp lệ và gửi logout lặp lại.
- **Dữ liệu test (nếu có):** Mật khẩu hiện tại sai, mật khẩu mới trùng cũ, mật khẩu mới mạnh.
- **Kết quả mong đợi:** Hai trường hợp sai bị từ chối; đổi thành công xóa cờ mustChangePassword, thu hồi refresh token cũ và cấp phiên mới; refresh xoay token; logout xóa cookie và có thể gọi lặp an toàn.

## B. Giỏ hàng — PU-12

### TC-08 — Xem giỏ rỗng và số lượng giỏ
- **Mã Test Case:** TC-08
- **Tên chức năng:** Xem giỏ hàng
- **Điều kiện tiên quyết:** USER A chưa có cart item.
- **Các bước thực hiện:** 1. Mở `/cart`. 2. Gọi `GET /api/cart` và `GET /api/cart/count`.
- **Dữ liệu test (nếu có):** Không có.
- **Kết quả mong đợi:** Hiển thị empty state; API trả danh sách shop rỗng, selectedItemCount=0, total=0 và cartCount=0.

### TC-09 — Guest hoặc tài khoản không có quyền USER truy cập giỏ
- **Mã Test Case:** TC-09
- **Tên chức năng:** Phân quyền giỏ hàng
- **Điều kiện tiên quyết:** Có trình duyệt Guest và tài khoản không có ROLE_USER.
- **Các bước thực hiện:** 1. Gọi API cart khi Guest. 2. Gọi lại bằng tài khoản chỉ có role Manager hoặc Shipper.
- **Dữ liệu test (nếu có):** GET/POST `/api/cart/**`.
- **Kết quả mong đợi:** Guest bị yêu cầu đăng nhập/401; tài khoản đã đăng nhập nhưng không có ROLE_USER bị từ chối/403; không tạo hoặc đọc giỏ của người khác.

### TC-10 — Thêm sản phẩm hợp lệ và cộng dồn số lượng
- **Mã Test Case:** TC-10
- **Tên chức năng:** Thêm sản phẩm vào giỏ
- **Điều kiện tiên quyết:** USER A; sản phẩm ACTIVE, shop APPROVED, còn đủ tồn kho.
- **Các bước thực hiện:** 1. Gửi `POST /api/cart/items` với sản phẩm và quantity=1. 2. Gửi lại cùng productId với quantity=2. 3. Đọc giỏ và cartCount.
- **Dữ liệu test (nếu có):** `productId` hợp lệ, quantity 1 rồi 2.
- **Kết quả mong đợi:** Lần đầu tạo cart/item đã chọn; lần sau cộng dồn thành 3 trên cùng một dòng; cartCount đếm số dòng sản phẩm khác nhau; giá do server lấy từ giá hiệu lực, không nhận giá client.

### TC-11 — Validation productId và quantity khi thêm
- **Mã Test Case:** TC-11
- **Tên chức năng:** Validation thêm vào giỏ
- **Điều kiện tiên quyết:** USER A đã đăng nhập.
- **Các bước thực hiện:** 1. Gửi request thiếu productId/quantity. 2. Thử productId không dương và quantity lần lượt 0, 100.
- **Dữ liệu test (nếu có):** productId=0; quantity=0, 100.
- **Kết quả mong đợi:** Request bị validation từ chối; không có dòng giỏ hàng mới.

### TC-12 — Sản phẩm không tồn tại hoặc không thể mua
- **Mã Test Case:** TC-12
- **Tên chức năng:** Kiểm tra khả năng mua khi thêm giỏ
- **Điều kiện tiên quyết:** Có ID không tồn tại và các sản phẩm đã xóa, không ACTIVE, hết hàng, shop chưa APPROVED/đã xóa.
- **Các bước thực hiện:** 1. Gửi POST thêm từng sản phẩm vào giỏ. 2. Kiểm tra giỏ sau mỗi request.
- **Dữ liệu test (nếu có):** ID không tồn tại; các trạng thái sản phẩm/shop nêu trên.
- **Kết quả mong đợi:** ID không tồn tại trả lỗi not found; sản phẩm hoặc shop không khả dụng bị từ chối với thông báo nghiệp vụ; không phát sinh dòng giỏ hàng.

### TC-13 — Không cho mua sản phẩm của shop mình
- **Mã Test Case:** TC-13
- **Tên chức năng:** Chặn tự mua hàng
- **Điều kiện tiên quyết:** USER A đồng thời là chủ shop của sản phẩm thử nghiệm.
- **Các bước thực hiện:** Gửi `POST /api/cart/items` với productId thuộc shop của USER A.
- **Dữ liệu test (nếu có):** Sản phẩm shop do USER A sở hữu.
- **Kết quả mong đợi:** Request bị từ chối; số lượng và cart không đổi.

### TC-14 — Thêm/cập nhật vượt tồn kho
- **Mã Test Case:** TC-14
- **Tên chức năng:** Kiểm tra tồn kho của giỏ
- **Điều kiện tiên quyết:** Sản phẩm còn stock=3.
- **Các bước thực hiện:** 1. Thêm quantity=3. 2. Thử thêm tiếp 1. 3. Thử cập nhật dòng sang quantity=4 rồi cập nhật về 2.
- **Dữ liệu test (nếu có):** Tồn kho 3; quantity 3, 1, 4 và 2.
- **Kết quả mong đợi:** Quantity tối đa bằng stock; thao tác vượt kho bị từ chối và giữ nguyên giỏ; cập nhật về 2 thành công.

### TC-15 — Cập nhật số lượng và giá hiệu lực
- **Mã Test Case:** TC-15
- **Tên chức năng:** Cập nhật giỏ khi giá thay đổi
- **Điều kiện tiên quyết:** Có item trong giỏ và có thể thay đổi giá khuyến mãi hiệu lực của sản phẩm.
- **Các bước thực hiện:** 1. Ghi nhận giá hiển thị. 2. Thay đổi giá hiệu lực ở nguồn quản trị hợp lệ. 3. Tải lại giỏ, sau đó cập nhật quantity.
- **Dữ liệu test (nếu có):** Giá ban đầu và giá hiệu lực mới khác nhau.
- **Kết quả mong đợi:** Giỏ hiển thị giá hiện hành và cảnh báo priceChanged so với priceWhenAdded; cập nhật quantity lấy lại giá hiệu lực hiện tại.

### TC-16 — Chọn từng dòng, theo shop và chọn tất cả
- **Mã Test Case:** TC-16
- **Tên chức năng:** Chọn sản phẩm cần thanh toán
- **Điều kiện tiên quyết:** Giỏ có sản phẩm từ hai shop, trong đó có ít nhất một dòng không khả dụng.
- **Các bước thực hiện:** 1. Bỏ chọn một dòng. 2. Chọn tất cả cho một shop qua `shopId`. 3. Chọn tất cả giỏ. 4. Bỏ chọn tất cả.
- **Dữ liệu test (nếu có):** Hai shop; một sản phẩm hết hàng/không còn ACTIVE.
- **Kết quả mong đợi:** Chỉ các dòng thuộc shop mục tiêu đổi lựa chọn; thao tác select-all không chọn dòng không khả dụng; tổng/selectedItemCount chỉ tính dòng được chọn, khả dụng và không vượt tồn.

### TC-17 — Không thể chọn sản phẩm mất khả năng bán
- **Mã Test Case:** TC-17
- **Tên chức năng:** Chọn dòng không khả dụng
- **Điều kiện tiên quyết:** Có item trong giỏ, sau đó sản phẩm hết hàng hoặc shop bị ngừng duyệt.
- **Các bước thực hiện:** Gọi `PATCH /api/cart/items/{itemId}/selected` với selected=true; thử GET cart và thao tác select-all.
- **Dữ liệu test (nếu có):** Item trở thành hết hàng/không khả dụng sau khi thêm.
- **Kết quả mong đợi:** Chọn trực tiếp bị từ chối; dòng được đánh dấu không khả dụng/không chọn được; select-all không bật lại dòng đó.

### TC-18 — Ownership, xóa dòng và xóa toàn bộ dòng không khả dụng
- **Mã Test Case:** TC-18
- **Tên chức năng:** Quản lý dòng giỏ hàng
- **Điều kiện tiên quyết:** USER A và B đều có cart item; USER A có thêm một dòng không khả dụng.
- **Các bước thực hiện:** 1. USER A cập nhật/xóa item của USER B bằng itemId. 2. USER A xóa item của mình. 3. Gọi `DELETE /api/cart/unavailable`.
- **Dữ liệu test (nếu có):** itemId của A và B; sản phẩm không khả dụng của A.
- **Kết quả mong đợi:** Item người khác không lộ ra và không thể sửa/xóa; item của A bị xóa đúng; xóa unavailable chỉ loại dòng không còn khả dụng, giữ lại dòng còn bán.

## C. Checkout, coupon, vận chuyển và thanh toán — PU-13/14/15/17

### TC-19 — Không có sản phẩm được chọn
- **Mã Test Case:** TC-19
- **Tên chức năng:** Mở checkout khi giỏ rỗng hoặc chưa chọn hàng
- **Điều kiện tiên quyết:** Giỏ rỗng hoặc có item nhưng tất cả đều bỏ chọn.
- **Các bước thực hiện:** 1. Mở `/checkout`. 2. Gọi `POST /api/checkout/preview` với payload hợp lệ.
- **Dữ liệu test (nếu có):** Không có dòng selected.
- **Kết quả mong đợi:** Web chuyển về `/cart` kèm thông báo; preview trả issue yêu cầu chọn ít nhất một sản phẩm, tổng 0 và không cho đặt.

### TC-20 — Lấy địa chỉ mặc định hoặc chọn địa chỉ khác
- **Mã Test Case:** TC-20
- **Tên chức năng:** Chọn địa chỉ giao hàng
- **Điều kiện tiên quyết:** USER A có địa chỉ mặc định và một địa chỉ phụ thuộc chính USER A.
- **Các bước thực hiện:** 1. Preview không truyền addressId. 2. Preview với addressId của địa chỉ phụ. 3. Đặt hàng và xem snapshot người nhận/địa chỉ.
- **Dữ liệu test (nếu có):** Hai địa chỉ hợp lệ của USER A.
- **Kết quả mong đợi:** Request thiếu addressId dùng địa chỉ mặc định; addressId hợp lệ dùng đúng địa chỉ đã chọn; đơn lưu snapshot tên, điện thoại và địa chỉ tại thời điểm đặt.

### TC-21 — Thiếu địa chỉ hoặc địa chỉ không thuộc người mua
- **Mã Test Case:** TC-21
- **Tên chức năng:** Kiểm tra quyền sở hữu địa chỉ
- **Điều kiện tiên quyết:** USER A không có địa chỉ mặc định; USER B có addressId riêng.
- **Các bước thực hiện:** 1. Preview checkout của A khi không có địa chỉ. 2. Gửi addressId của B trong request của A.
- **Dữ liệu test (nếu có):** Address ID của USER B.
- **Kết quả mong đợi:** Thiếu địa chỉ tạo issue chặn đặt hàng; addressId không thuộc A bị từ chối, không sử dụng/tiết lộ dữ liệu địa chỉ của B.

### TC-22 — Preview theo đúng dòng đã chọn và chia đơn theo shop
- **Mã Test Case:** TC-22
- **Tên chức năng:** Tính preview nhiều chi nhánh
- **Điều kiện tiên quyết:** Giỏ có dòng selected và unselected từ ít nhất hai shop.
- **Các bước thực hiện:** 1. Chọn một số dòng ở cả hai shop. 2. Gọi preview. 3. Đối chiếu line, subtotal từng shop và tổng hàng với giỏ.
- **Dữ liệu test (nếu có):** Một item unselected phải bị loại khỏi preview.
- **Kết quả mong đợi:** Preview chỉ chứa dòng được chọn; mỗi shop có subtotal/đơn con riêng; tổng không bao gồm sản phẩm bỏ chọn.

### TC-23 — Không có hãng vận chuyển hoạt động
- **Mã Test Case:** TC-23
- **Tên chức năng:** Preview khi không có carrier
- **Điều kiện tiên quyết:** Không có shipping carrier ACTIVE.
- **Các bước thực hiện:** Gọi preview với selected item hợp lệ.
- **Dữ liệu test (nếu có):** Carrier đều inactive.
- **Kết quả mong đợi:** Preview đưa issue không có đơn vị vận chuyển khả dụng, canPlaceOrder=false; không đặt được đơn.

### TC-24 — Chọn carrier, mặc định phí thấp nhất và miễn phí ship
- **Mã Test Case:** TC-24
- **Tên chức năng:** Tính phí vận chuyển
- **Điều kiện tiên quyết:** Có ít nhất hai carrier ACTIVE với phí khác nhau và một carrier có freeShipThreshold.
- **Các bước thực hiện:** 1. Preview không gửi carrierId. 2. Preview với carrier phí cao hơn. 3. Thử subtotal bằng ngưỡng miễn phí và thấp hơn ngưỡng một đồng.
- **Dữ liệu test (nếu có):** Subtotal threshold và threshold-1; carrier ID hợp lệ.
- **Kết quả mong đợi:** Thiếu carrier ở preview chọn carrier có baseFee thấp nhất (tie-break theo ID); chọn rõ carrier dùng phí của carrier đó; subtotal đạt ngưỡng phí bằng 0, thấp hơn ngưỡng tính baseFee.

### TC-25 — Carrier sai/ngừng hoạt động và carrier phải chốt lúc đặt
- **Mã Test Case:** TC-25
- **Tên chức năng:** Xác thực hãng vận chuyển khi place
- **Điều kiện tiên quyết:** Preview với carrier ACTIVE, có thêm carrier INACTIVE.
- **Các bước thực hiện:** 1. Gửi preview với carrier không tồn tại hoặc INACTIVE. 2. Đặt hàng thiếu carrierId cho một shop. 3. Đổi carrier sau preview nhưng giữ tổng cũ.
- **Dữ liệu test (nếu có):** Carrier ID inactive hoặc không tồn tại.
- **Kết quả mong đợi:** Preview đánh dấu issue carrier không hợp lệ; place bắt buộc khai báo carrier cho từng shop và carrier phải khớp preview; thiếu/đổi carrier bị từ chối, không ghi đơn.

### TC-26 — Coupon shop hợp lệ, không dùng coupon và nhiều lựa chọn coupon
- **Mã Test Case:** TC-26
- **Tên chức năng:** Áp dụng coupon theo shop
- **Điều kiện tiên quyết:** Coupon shop ACTIVE, đang trong hạn, đủ điều kiện đơn tối thiểu và còn lượt.
- **Các bước thực hiện:** 1. Preview không nhập coupon. 2. Preview nhập mã shop đúng shop. 3. Với giỏ nhiều shop, nhập mã shop A vào option shop B.
- **Dữ liệu test (nếu có):** Coupon loại FIXED_AMOUNT và/hoặc PERCENTAGE thuộc Shop A.
- **Kết quả mong đợi:** Không nhập mã thì không giảm; mã hợp lệ chỉ giảm đơn của shop sở hữu coupon; coupon áp nhầm shop bị báo không đúng phạm vi và không giảm.

### TC-27 — Coupon sàn và phân bổ giảm giá
- **Mã Test Case:** TC-27
- **Tên chức năng:** Áp dụng coupon toàn sàn
- **Điều kiện tiên quyết:** Giỏ có hai shop; coupon sàn hợp lệ và đạt minOrderValue.
- **Các bước thực hiện:** 1. Nhập mã coupon sàn vào trường platformCouponCode. 2. Xem preview từng shop. 3. Cộng platformCouponShare của các shop.
- **Dữ liệu test (nếu có):** Coupon có mức giảm cố định hoặc phần trăm; shop có subtotal khác nhau.
- **Kết quả mong đợi:** Coupon được kiểm tra trên tổng hàng; mức giảm được chia theo phần còn phải trả trước phí ship theo quy tắc tiền; tổng share bằng đúng tổng discount, không tạo total âm.

### TC-28 — Từ chối coupon không hợp lệ, preview không giảm nhưng place bị chặn
- **Mã Test Case:** TC-28
- **Tên chức năng:** Coupon sai ở preview và bước đặt hàng
- **Điều kiện tiên quyết:** Có coupon không tồn tại/INACTIVE/chưa hiệu lực/hết hạn/hết lượt/sai scope/chưa đạt tối thiểu/đã vượt limit user.
- **Các bước thực hiện:** 1. Nhập từng mã vào preview. 2. Thử place khi request vẫn gửi mã không hợp lệ. 3. Gỡ mã và preview/đặt lại.
- **Dữ liệu test (nếu có):** Mỗi coupon đại diện cho một điều kiện từ chối; mã có khoảng trắng/chữ thường để kiểm tra chuẩn hóa.
- **Kết quả mong đợi:** Preview cảnh báo, discount=0, không tự xóa mã khỏi payload; nếu gửi mã sai ở place, order bị từ chối; bỏ/sửa mã rồi preview lại mới có thể tiếp tục. Mã được trim và chuẩn hóa chữ hoa khi tra cứu.

### TC-29 — Cách tính giảm giá và làm tròn
- **Mã Test Case:** TC-29
- **Tên chức năng:** Kiểm tra số tiền checkout
- **Điều kiện tiên quyết:** Có giá sản phẩm tạo ra phần lẻ khi tính tiền, coupon phần trăm có maxDiscountAmount, và coupon cố định.
- **Các bước thực hiện:** 1. Preview giá đơn vị có phần lẻ, số lượng lớn hơn 1. 2. Áp coupon phần trăm. 3. Thử mức giảm lớn hơn subtotal còn lại.
- **Dữ liệu test (nếu có):** Giá đơn vị cần HALF_UP; percentage có max discount.
- **Kết quả mong đợi:** Đơn giá được làm tròn VND trước khi nhân số lượng; phần trăm coupon làm tròn xuống, bị giới hạn bởi maxDiscountAmount và subtotal còn lại; tổng cuối = subtotal − discount + shipping.

### TC-30 — Sai payload checkout và trùng shop
- **Mã Test Case:** TC-30
- **Tên chức năng:** Validation request preview/place
- **Điều kiện tiên quyết:** USER A có dòng được chọn.
- **Các bước thực hiện:** 1. Gửi shops chứa trùng shopId. 2. Gửi carrier/coupon option với shopId null hoặc thiếu. 3. Gửi place thiếu paymentMethod/token/expectedGrandTotal, token sai UUID, tổng âm hoặc note vượt giới hạn.
- **Dữ liệu test (nếu có):** shopId lặp; token `bad-token`; expectedGrandTotal=-1; note trên 500 ký tự.
- **Kết quả mong đợi:** Request lỗi validation/business; payload không hợp lệ không tạo order group, không trừ kho, không xóa giỏ.

### TC-31 — Giá/tổng thay đổi sau preview
- **Mã Test Case:** TC-31
- **Tên chức năng:** Kiểm tra lại giá khi đặt
- **Điều kiện tiên quyết:** Đã preview giỏ và có thể cập nhật giá/khuyến mãi/cước phí trước khi place.
- **Các bước thực hiện:** 1. Lưu expectedGrandTotal từ preview. 2. Làm thay đổi giá hiệu lực hoặc phí ship ở server. 3. Gửi place với tổng cũ; sau đó cập nhật preview và gửi lại bằng token mới.
- **Dữ liệu test (nếu có):** Tổng cũ khác preview mới.
- **Kết quả mong đợi:** Place lần đầu trả conflict/thông báo giá hoặc ưu đãi thay đổi; không lấy tổng client để ghi DB; preview mới tạo ra tổng server chính xác.

### TC-32 — Đặt hàng COD thành công cho nhiều shop
- **Mã Test Case:** TC-32
- **Tên chức năng:** Đặt hàng COD
- **Điều kiện tiên quyết:** USER A có selected item hợp lệ ở hai shop, địa chỉ, carrier và preview canPlaceOrder=true.
- **Các bước thực hiện:** 1. Chọn COD. 2. Gửi `POST /api/checkout/place` với token UUID mới, carrier từng shop và expectedGrandTotal đúng. 3. Mở trang success.
- **Dữ liệu test (nếu có):** Nhiều sản phẩm ở hai shop; có thể kèm coupon đã áp dụng hợp lệ.
- **Kết quả mong đợi:** API tạo mới trả 201/Location; tạo một OrderGroup và các Order con theo shop; trạng thái mỗi Order là NEW, paymentStatus của group vẫn PENDING, COD chưa thu; tồn kho giảm đúng lượng, ghi order snapshot/history/coupon, xóa khỏi giỏ đúng các dòng đã mua và giữ dòng chưa chọn.

### TC-33 — Đặt hàng MoMo giả lập và xác nhận
- **Mã Test Case:** TC-33
- **Tên chức năng:** Thanh toán MoMo giả lập thành công
- **Điều kiện tiên quyết:** Checkout hợp lệ, chọn MOMO.
- **Các bước thực hiện:** 1. Đặt hàng. 2. Gọi `POST /api/checkout/{groupCode}/mock-momo/confirm`. 3. Gọi confirm lần nữa. 4. Tải trang success.
- **Dữ liệu test (nếu có):** GroupCode vừa tạo.
- **Kết quả mong đợi:** Đơn được tạo NEW và group PENDING; confirm chuyển group sang PAID, đặt paidAt và trả redirect thành công; confirm lặp khi đã PAID trả PAID idempotently; không gọi cổng MoMo thật.

### TC-34 — Hủy MoMo giả lập và thử lại giao dịch
- **Mã Test Case:** TC-34
- **Tên chức năng:** Hủy thanh toán MoMo
- **Điều kiện tiên quyết:** Có MoMo group PENDING, đơn con NEW.
- **Các bước thực hiện:** 1. Gọi `POST /api/checkout/{groupCode}/mock-momo/cancel`. 2. Kiểm tra group/order/tồn kho/giỏ. 3. Gọi cancel lần nữa. 4. Thử confirm group đã hủy.
- **Dữ liệu test (nếu có):** Group có một hoặc nhiều order item.
- **Kết quả mong đợi:** Lần đầu group thành FAILED, order NEW thành CANCELLED qua OrderService, stock được hoàn và sản phẩm được thêm lại giỏ; lần cancel lặp trả FAILED mà không hoàn kho/thêm giỏ lần hai; confirm sau khi hủy bị từ chối.

### TC-35 — Từ chối MoMo sai chủ, sai payment method hoặc trạng thái
- **Mã Test Case:** TC-35
- **Tên chức năng:** Kiểm tra quyền và trạng thái MoMo
- **Điều kiện tiên quyết:** Có group MOMO của USER A, group COD và group MOMO đã PAID/FAILED.
- **Các bước thực hiện:** 1. USER B gọi confirm/cancel group A. 2. USER A gọi với group COD. 3. Gọi thao tác không phù hợp với group PAID/FAILED.
- **Dữ liệu test (nếu có):** GroupCode sai/không tồn tại.
- **Kết quả mong đợi:** Group không thuộc user hoặc không tồn tại trả not found; COD bị từ chối vì không dùng MoMo; giao dịch đã PAID không hủy được, FAILED không confirm được; không thay đổi trạng thái ngoài dự kiến.

### TC-36 — Idempotency token và chống đặt trùng
- **Mã Test Case:** TC-36
- **Tên chức năng:** Gửi lại yêu cầu đặt hàng
- **Điều kiện tiên quyết:** Có request place thành công với token UUID T của USER A.
- **Các bước thực hiện:** 1. Gửi lại nguyên request cùng token T từ USER A. 2. Gửi token T từ USER B. 3. Gửi request mới với token khác.
- **Dữ liệu test (nếu có):** Token UUID hợp lệ T và token mới U.
- **Kết quả mong đợi:** A nhận lại kết quả nhóm đơn cũ với alreadyPlaced=true và không tạo đơn/giảm kho lần nữa; B bị từ chối; token mới chỉ tạo order nếu giỏ và preview hiện tại hợp lệ.

### TC-37 — Hết hàng giữa preview và place / rollback toàn giao dịch
- **Mã Test Case:** TC-37
- **Tên chức năng:** Trừ kho nguyên tử
- **Điều kiện tiên quyết:** Có ít nhất hai selected item thuộc hai sản phẩm, đủ kho khi preview.
- **Các bước thực hiện:** 1. Preview. 2. Giảm stock của một sản phẩm ở phiên quản trị/phiên bán khác xuống dưới số lượng cần mua hoặc đặt status không ACTIVE. 3. Place. 4. Kiểm tra tất cả đơn, kho, coupon và giỏ.
- **Dữ liệu test (nếu có):** Một sản phẩm còn ít hơn quantity yêu cầu.
- **Kết quả mong đợi:** Place từ chối vì sản phẩm hết/thiếu hàng hoặc không còn bán; transaction rollback toàn bộ, không có order group/order mới, không giữ phần stock đã trừ cho sản phẩm khác, không ghi usage coupon và không xóa item khỏi giỏ.

## D. Đơn hàng người mua và đổi trả — PU-15/18/20

### TC-38 — Danh sách, tìm kiếm và chi tiết đơn của người mua
- **Mã Test Case:** TC-38
- **Tên chức năng:** Tra cứu đơn hàng
- **Điều kiện tiên quyết:** USER A có đơn thuộc nhiều trạng thái; USER B có đơn riêng.
- **Các bước thực hiện:** 1. Mở `/orders`, đổi tab trạng thái và tìm order code. 2. Mở chi tiết đơn A. 3. USER A thử mở order ID của B.
- **Dữ liệu test (nếu có):** Keyword có khoảng trắng thừa và dài hơn giới hạn 50 ký tự.
- **Kết quả mong đợi:** Danh sách chỉ gồm đơn của A, bộ lọc/tab và phân trang hoạt động; từ khóa được chuẩn hóa/cắt tối đa 50 ký tự; đơn của B và ID không tồn tại đều không lộ dữ liệu (404).

### TC-39 — Hủy đơn khi còn trạng thái cho phép
- **Mã Test Case:** TC-39
- **Tên chức năng:** Người mua hủy đơn NEW/CONFIRMED
- **Điều kiện tiên quyết:** USER A có đơn NEW và một đơn CONFIRMED thuộc mình.
- **Các bước thực hiện:** 1. Hủy từng đơn với lý do có sẵn. 2. Thử lý do OTHER không có chi tiết, chi tiết dưới 10 ký tự và trên 200 ký tự. 3. Gửi lại hủy đơn đã CANCELLED.
- **Dữ liệu test (nếu có):** CancelReason; detail OTHER 9/10/201/200 ký tự.
- **Kết quả mong đợi:** NEW/CONFIRMED được hủy khi dữ liệu hợp lệ; OTHER bắt buộc chi tiết 10–200 ký tự; thông tin lý do được ghi qua OrderService; hủy lặp trả thông báo đã hủy trước đó và không hoàn kho lần nữa.

### TC-40 — Hủy đơn sai chủ hoặc sai trạng thái
- **Mã Test Case:** TC-40
- **Tên chức năng:** Từ chối hủy đơn không hợp lệ
- **Điều kiện tiên quyết:** Có đơn USER B và đơn USER A ở PICKED_UP/SHIPPING/DELIVERED.
- **Các bước thực hiện:** 1. USER A gửi hủy đơn USER B. 2. USER A gửi hủy đơn đã qua trạng thái cho phép.
- **Dữ liệu test (nếu có):** IDs của đơn khác chủ và đơn không thể hủy.
- **Kết quả mong đợi:** Đơn khác chủ không được tiết lộ; đơn đang giao/đã giao bị từ chối với thông báo không thể hủy; trạng thái, tồn kho và lịch sử không đổi.

### TC-41 — Yêu cầu trả hàng hợp lệ với ảnh bắt buộc
- **Mã Test Case:** TC-41
- **Tên chức năng:** Tạo yêu cầu trả hàng
- **Điều kiện tiên quyết:** USER A sở hữu đơn DELIVERED còn trong hạn; chưa có yêu cầu trả hàng.
- **Các bước thực hiện:** 1. Mở form trả hàng. 2. Chọn lý do, nhập detail 10–500 ký tự và đính kèm ảnh hợp lệ. 3. Gửi form; kiểm tra đơn, ReturnRequest và timeline.
- **Dữ liệu test (nếu có):** Ảnh JPG/PNG hợp lệ; video MP4/WebM tùy chọn.
- **Kết quả mong đợi:** Yêu cầu ở trạng thái PENDING, đơn chuyển RETURN_REQUESTED qua OrderService, ảnh/video URL và publicId được lưu; timeline ghi lý do đã chuẩn hóa.

### TC-42 — Validation và điều kiện từ chối yêu cầu trả hàng
- **Mã Test Case:** TC-42
- **Tên chức năng:** Từ chối yêu cầu trả hàng không hợp lệ
- **Điều kiện tiên quyết:** Có đơn NEW, đơn DELIVERED quá hạn, đơn DELIVERED đã có yêu cầu trả hàng và đơn của USER B.
- **Các bước thực hiện:** 1. Gửi thiếu lý do/detail, detail dưới 10 hoặc trên 500 ký tự. 2. Gửi thiếu ảnh. 3. Thử đơn sai trạng thái, quá hạn, trùng yêu cầu và đơn người khác.
- **Dữ liệu test (nếu có):** Ảnh rỗng; các đơn theo điều kiện.
- **Kết quả mong đợi:** Validation/BusinessException phù hợp; ảnh là bằng chứng bắt buộc, video không bắt buộc; đơn của người khác trả not found; không tạo request hoặc thay trạng thái khi bị từ chối.

### TC-43 — Hủy đổi trả khi upload media lỗi
- **Mã Test Case:** TC-43
- **Tên chức năng:** Kiểm tra file bằng chứng và rollback Cloudinary
- **Điều kiện tiên quyết:** Đơn trả hàng đủ điều kiện; có file đúng/ sai định dạng, file giả đuôi và tình huống transaction lỗi sau upload.
- **Các bước thực hiện:** 1. Gửi ảnh có đuôi đúng nhưng nội dung không phải ảnh hợp lệ. 2. Gửi ảnh hợp lệ cùng video không hợp lệ. 3. Mô phỏng lỗi lưu sau upload theo fixture test/dev được phép.
- **Dữ liệu test (nếu có):** File rỗng, sai magic bytes, ảnh JPG/PNG; video MP4/WebM.
- **Kết quả mong đợi:** File sai bị từ chối; không thay đổi trạng thái đơn; nếu lỗi sau upload thì tài nguyên Cloudinary mới được lên lịch xóa khi rollback, không để lại asset mồ côi.

### TC-44 — Khiếu nại sau khi Vendor từ chối
- **Mã Test Case:** TC-44
- **Tên chức năng:** Khiếu nại yêu cầu trả hàng
- **Điều kiện tiên quyết:** Đơn USER A ở RETURN_REJECTED; request bị từ chối, chưa có adminNote và còn trong thời hạn khiếu nại cấu hình.
- **Các bước thực hiện:** 1. Gửi khiếu nại hợp lệ. 2. Manager/Admin kết luận từ chối bằng note hợp lệ hoặc chấp nhận qua luồng trạng thái. 3. Thử khiếu nại lần hai, sai chủ, quá hạn hoặc note kết luận rỗng/quá 500 ký tự.
- **Dữ liệu test (nếu có):** Nội dung khiếu nại hợp lệ; admin note 1–500 ký tự.
- **Kết quả mong đợi:** Khiếu nại chuyển ReturnRequest sang ESCALATED và phát sự kiện; chỉ chủ đơn được khiếu nại, mỗi yêu cầu một lần trong thời hạn; kết luận từ chối yêu cầu note và ngăn khiếu nại tiếp; kết luận chấp nhận chuyển trạng thái qua OrderService.

## E. Xử lý đơn và đổi trả của Vendor — PG-18/20

### TC-45 — Vendor chỉ xem đơn thuộc shop mình
- **Mã Test Case:** TC-45
- **Tên chức năng:** Danh sách và chi tiết đơn Vendor
- **Điều kiện tiên quyết:** Có hai Vendor sở hữu hai shop; mỗi shop có đơn ở các trạng thái khác nhau.
- **Các bước thực hiện:** 1. Vendor A mở danh sách, chuyển tab new/confirmed/shipping/delivered/cancelled/returns và tìm mã đơn. 2. Mở chi tiết đơn của shop A. 3. Thử mở order ID shop B.
- **Dữ liệu test (nếu có):** Keyword có khoảng trắng thừa hoặc trên 50 ký tự.
- **Kết quả mong đợi:** Danh sách/tab/count chỉ tính shop do Vendor hiện tại sở hữu; chi tiết chứa snapshot và timeline; truy cập đơn shop khác bị 403, không thay đổi dữ liệu.

### TC-46 — Vendor xác nhận hoặc từ chối đơn mới
- **Mã Test Case:** TC-46
- **Tên chức năng:** Cập nhật trạng thái đơn mới của Vendor
- **Điều kiện tiên quyết:** Vendor A có đơn NEW thuộc shop mình.
- **Các bước thực hiện:** 1. POST status CONFIRMED. 2. Với đơn NEW khác, POST CANCELLED kèm lý do. 3. Thử CANCELLED không note, status khác hoặc thao tác lại sau khi trạng thái đã đổi.
- **Dữ liệu test (nếu có):** CONFIRMED; CANCELLED với note; note dài hơn 255 ký tự.
- **Kết quả mong đợi:** Chỉ NEW → CONFIRMED hoặc NEW → CANCELLED được chấp nhận; từ chối bắt buộc lý do; transition khác bị từ chối; trạng thái, timeline và event được cập nhật thông qua OrderService.

### TC-47 — Vendor xử lý yêu cầu trả hàng
- **Mã Test Case:** TC-47
- **Tên chức năng:** Duyệt/từ chối yêu cầu trả hàng
- **Điều kiện tiên quyết:** Vendor sở hữu đơn RETURN_REQUESTED có request PENDING; có thêm yêu cầu của shop khác.
- **Các bước thực hiện:** 1. Vendor A duyệt request của mình. 2. Từ chối request khác với lý do hợp lệ. 3. Thử từ chối không lý do và xử lý ID shop khác/không có request.
- **Dữ liệu test (nếu có):** Note từ chối không rỗng.
- **Kết quả mong đợi:** Duyệt chuyển RETURN_APPROVED; từ chối chuyển RETURN_REJECTED và ghi lý do; sai quyền bị 403, ID không có request bị 404; mọi chuyển trạng thái đi qua OrderService.

## F. Quản lý phân công — PA-21 / PA-13

### TC-48 — Manager/Admin phân công Shipper hợp lệ
- **Mã Test Case:** TC-48
- **Tên chức năng:** Phân công đơn cho Shipper
- **Điều kiện tiên quyết:** Đơn CONFIRMED chưa lấy hàng; Shipper ACTIVE có ROLE_SHIPPER.
- **Các bước thực hiện:** 1. Manager hoặc Admin phân công Shipper. 2. Kiểm tra chi tiết đơn, lịch sử trạng thái và Shipper nhận được đơn.
- **Dữ liệu test (nếu có):** Order CONFIRMED; tài khoản Shipper ACTIVE.
- **Kết quả mong đợi:** Shipper được gán; history ghi CONFIRMED → CONFIRMED với người thao tác và ghi chú phân công; OrderAssignedEvent được phát.

### TC-49 — Từ chối phân công không hợp lệ
- **Mã Test Case:** TC-49
- **Tên chức năng:** Kiểm tra điều kiện phân công Shipper
- **Điều kiện tiên quyết:** Có đơn NEW/SHIPPING; có người dùng không có ROLE_SHIPPER và Shipper INACTIVE.
- **Các bước thực hiện:** 1. USER/Vendor gọi chức năng phân công. 2. Manager thử phân công vào đơn không CONFIRMED. 3. Thử gán người không phải Shipper hoặc Shipper không ACTIVE.
- **Dữ liệu test (nếu có):** Các order/user theo điều kiện.
- **Kết quả mong đợi:** Chỉ Manager/Admin mới được phân công; chỉ đơn CONFIRMED và tài khoản có ROLE_SHIPPER + ACTIVE hợp lệ; lỗi không đổi assignment/history.

## G. Giao hàng của Shipper — PG-23/24/25

### TC-50 — Danh sách và quyền sở hữu đơn được giao
- **Mã Test Case:** TC-50
- **Tên chức năng:** Tra cứu đơn Shipper
- **Điều kiện tiên quyết:** Hai Shipper; mỗi người có đơn được phân công ở trạng thái CONFIRMED, PICKED_UP, SHIPPING, DELIVERED, DELIVERY_FAILED và RETURN_APPROVED.
- **Các bước thực hiện:** 1. Shipper A lần lượt mở các tab pickup/shipping/completed/failed/returns. 2. Mở chi tiết đơn được gán cho A. 3. Thử mở đơn của B và ID không tồn tại.
- **Dữ liệu test (nếu có):** Các order status theo điều kiện.
- **Kết quả mong đợi:** Tab chỉ chứa đơn đúng trạng thái và được gán cho A; đơn tồn tại nhưng gán B trả 403, ID không tồn tại trả 404; thông tin giao hàng, COD và snapshot item hiển thị đúng.

### TC-51 — Chuyển trạng thái giao hàng theo đúng thứ tự
- **Mã Test Case:** TC-51
- **Tên chức năng:** Cập nhật tiến độ giao hàng
- **Điều kiện tiên quyết:** Shipper được phân công đơn CONFIRMED.
- **Các bước thực hiện:** 1. Chuyển CONFIRMED → PICKED_UP. 2. Chuyển PICKED_UP → SHIPPING. 3. Chuyển SHIPPING → DELIVERY_FAILED kèm lý do. 4. Chuyển DELIVERY_FAILED → SHIPPING rồi hoàn tất giao.
- **Dữ liệu test (nếu có):** Note giao thất bại không rỗng.
- **Kết quả mong đợi:** Chỉ các transition được policy cho phép mới thành công; ghi timeline, side effect/event; DELIVERY_FAILED cần note; chuyển trạng thái nhảy cóc hoặc lặp transition không có trong policy bị từ chối.

### TC-52 — Shipper giao COD thành công và thất bại
- **Mã Test Case:** TC-52
- **Tên chức năng:** Xác nhận thu COD
- **Điều kiện tiên quyết:** Đơn COD trạng thái SHIPPING đã được giao Shipper; có đơn MoMo để đối chiếu.
- **Các bước thực hiện:** 1. Shipper chuyển đơn COD sang DELIVERED với codCollectedConfirmed=false. 2. Thử lại với true. 3. Với đơn MoMo, hoàn tất giao không cần xác nhận COD.
- **Dữ liệu test (nếu có):** COD amount > 0; request xác nhận true/false.
- **Kết quả mong đợi:** COD thiếu xác nhận bị từ chối; xác nhận true cho phép DELIVERED và OrderService đánh dấu đã thu COD; đơn MoMo không yêu cầu xác nhận COD.

### TC-53 — Hủy/giao lỗi và quyền Shipper
- **Mã Test Case:** TC-53
- **Tên chức năng:** Từ chối transition và người thao tác sai
- **Điều kiện tiên quyết:** Đơn SHIPPING được gán cho Shipper A; một đơn gán Shipper B.
- **Các bước thực hiện:** 1. Gửi DELIVERY_FAILED thiếu note. 2. Shipper A thử update đơn B. 3. Thử chuyển sang CANCELLED hoặc RETURN_APPROVED từ trạng thái không cho phép.
- **Dữ liệu test (nếu có):** Note null/blank; status không hợp lệ.
- **Kết quả mong đợi:** Thiếu note bị validation/service từ chối; Shipper chỉ thao tác đơn được gán cho mình; transition không được policy cho phép không thay đổi đơn.

### TC-54 — Thống kê Shipper và khoảng ngày
- **Mã Test Case:** TC-54
- **Tên chức năng:** Xem thống kê giao hàng
- **Điều kiện tiên quyết:** Shipper A có đơn DELIVERED, DELIVERY_FAILED, COD đã thu và đơn đang giao; Shipper B có dữ liệu riêng.
- **Các bước thực hiện:** 1. Chọn khoảng ngày hợp lệ. 2. Thử ngày bắt đầu sau ngày kết thúc và khoảng dài hơn 366 ngày. 3. Mở thống kê tài khoản B hoặc khoảng không có kết quả.
- **Dữ liệu test (nếu có):** Ngày hợp lệ, ngày đảo ngược, khoảng 367 ngày.
- **Kết quả mong đợi:** Thống kê chỉ dựa đơn được giao cho Shipper hiện tại; success rate tính delivered/(delivered+failed), COD chỉ cộng đơn COD đã giao; workload còn đang giao vẫn hiển thị; khoảng ngày sai bị từ chối; không có đơn kết thúc trả tỷ lệ 0%.

## H. Kiểm tra kết quả sau giao dịch — PU/PG/PA

### TC-55 — Lịch sử trạng thái và tác dụng phụ đơn hàng
- **Mã Test Case:** TC-55
- **Tên chức năng:** Kiểm tra timeline và side effects
- **Điều kiện tiên quyết:** Có thể lần lượt xác nhận, giao hàng và hủy đơn fixture.
- **Các bước thực hiện:** 1. Thực hiện transition hợp lệ. 2. Kiểm tra order_status_histories, tồn kho/sold_count, deliveredAt, commission snapshot và COD.
- **Dữ liệu test (nếu có):** Đơn COD và MoMo; đơn giao thành công/hủy/trả.
- **Kết quả mong đợi:** Mỗi transition có history đúng previous/new status và actor; chỉ DELIVERED chốt deliveredAt/sold_count/commission/shopRevenue; hủy hoàn stock theo quy tắc nhưng không hoàn lại coupon usage; tác dụng phụ không liên quan không bị cập nhật.

### TC-56 — Thông báo sau khi đặt đơn và cập nhật trạng thái
- **Mã Test Case:** TC-56
- **Tên chức năng:** Notification đơn hàng
- **Điều kiện tiên quyết:** Buyer, chủ shop và Shipper được gán có thể nhận notification; WebSocket phiên đã xác thực.
- **Các bước thực hiện:** 1. Đặt đơn nhiều shop. 2. Vendor xác nhận và Manager phân công Shipper. 3. Shipper cập nhật trạng thái. 4. Mở chuông thông báo và trang danh sách.
- **Dữ liệu test (nếu có):** Hai shop, một Buyer, hai Vendor và một Shipper.
- **Kết quả mong đợi:** Sự kiện sau commit tạo notification cho đúng người nhận theo trạng thái; kênh đơn hàng là user-private, không phát thông tin đơn lên topic công khai; link điều hướng theo role tới đúng trang đơn.

## I. Giao diện và trải nghiệm người dùng — PG

### TC-57 — Responsive layout trang công khai
- **Mã Test Case:** TC-57
- **Tên chức năng:** Bố cục trang công khai trên các kích thước màn hình
- **Điều kiện tiên quyết:** Có thể mở trang chủ, danh sách sản phẩm và chi tiết sản phẩm.
- **Các bước thực hiện:** 1. Mở từng trang ở desktop, tablet và mobile. 2. Kiểm tra header, nội dung, footer, ảnh và các nút chính. 3. Cuộn đến cuối trang.
- **Dữ liệu test (nếu có):** Viewport desktop, tablet và mobile theo công cụ trình duyệt.
- **Kết quả mong đợi:** Nội dung co giãn theo Bootstrap, không tràn ngang hoặc chồng lấn; các thành phần chính vẫn đọc được và thao tác được.

### TC-58 — Responsive giỏ hàng và checkout
- **Mã Test Case:** TC-58
- **Tên chức năng:** Bố cục giỏ hàng và checkout trên mobile/tablet
- **Điều kiện tiên quyết:** USER A có nhiều sản phẩm được chọn, thuộc ít nhất hai shop.
- **Các bước thực hiện:** 1. Mở giỏ hàng và checkout ở mobile/tablet. 2. Thay đổi số lượng, chọn địa chỉ/carrier và cuộn tới nút đặt hàng. 3. Thử xoay màn hình.
- **Dữ liệu test (nếu có):** Giỏ nhiều dòng, địa chỉ dài và ghi chú giao hàng.
- **Kết quả mong đợi:** Các dòng hàng, tổng tiền và biểu mẫu không bị cắt; có thể thao tác các điều khiển và nút tiếp tục/đặt hàng mà không phải cuộn ngang.

### TC-59 — Responsive dashboard Admin/Manager
- **Mã Test Case:** TC-59
- **Tên chức năng:** Bố cục dashboard trên màn hình nhỏ
- **Điều kiện tiên quyết:** Đăng nhập bằng ROLE_ADMIN hoặc ROLE_MANAGER.
- **Các bước thực hiện:** 1. Mở dashboard và một trang quản lý có bảng/form. 2. Thu nhỏ viewport qua kích thước tablet/mobile. 3. Thao tác mở menu và cuộn bảng.
- **Dữ liệu test (nếu có):** Bảng có nhiều cột và nội dung dài.
- **Kết quả mong đợi:** Sidebar/nội dung không che lấp nhau; bảng cuộn theo bố cục responsive; tiêu đề, form và nút thao tác vẫn sử dụng được.

### TC-60 — Responsive trang Vendor và Shipper
- **Mã Test Case:** TC-60
- **Tên chức năng:** Bố cục danh sách công việc Vendor/Shipper
- **Điều kiện tiên quyết:** Có tài khoản Vendor và Shipper với dữ liệu danh sách.
- **Các bước thực hiện:** 1. Mở danh sách đơn của Vendor và Shipper trên desktop, tablet, mobile. 2. Kiểm tra bảng, bộ lọc và nút cập nhật trạng thái. 3. Thử cuộn bảng trên mobile.
- **Dữ liệu test (nếu có):** Đơn có mã dài, nhiều sản phẩm và thông tin giao hàng.
- **Kết quả mong đợi:** Bảng/danh sách hiển thị trong vùng cuộn phù hợp; dữ liệu không đè lên nhau và các thao tác chính vẫn truy cập được.

### TC-61 — Menu khi chưa đăng nhập
- **Mã Test Case:** TC-61
- **Tên chức năng:** Menu của Guest
- **Điều kiện tiên quyết:** Đã đăng xuất và mở trang công khai.
- **Các bước thực hiện:** 1. Kiểm tra header trên desktop và mobile. 2. Mở menu điều hướng. 3. Đối chiếu các mục với trạng thái chưa đăng nhập.
- **Dữ liệu test (nếu có):** Phiên Guest.
- **Kết quả mong đợi:** Chỉ hiển thị các mục phù hợp với Guest; không xuất hiện liên kết dashboard, giỏ hàng cá nhân hoặc thao tác dành riêng cho tài khoản đã đăng nhập.

### TC-62 — Menu và sidebar theo vai trò
- **Mã Test Case:** TC-62
- **Tên chức năng:** Phân quyền hiển thị điều hướng
- **Điều kiện tiên quyết:** Có tài khoản USER, Vendor, Shipper, Manager và Admin.
- **Các bước thực hiện:** 1. Đăng nhập lần lượt từng vai trò. 2. Kiểm tra menu/header/sidebar. 3. Đăng xuất rồi đăng nhập vai trò khác để so sánh.
- **Dữ liệu test (nếu có):** Mỗi tài khoản chỉ mang vai trò tương ứng trong phạm vi kiểm thử.
- **Kết quả mong đợi:** Điều hướng chỉ hiển thị các mục phù hợp với vai trò; layout dashboard dùng marker Admin/Manager không áp dụng nhầm cho Vendor/Shipper.

### TC-63 — Giao diện tối của Admin/Manager
- **Mã Test Case:** TC-63
- **Tên chức năng:** Kiểm tra theme dashboard Admin/Manager
- **Điều kiện tiên quyết:** Đăng nhập Admin/Manager và mở trang có panel, form, bảng và stat/chart card.
- **Các bước thực hiện:** 1. Kiểm tra màu nền, chữ, tiêu đề, viền và bóng. 2. Tập trung vào input/select và xem trạng thái focus. 3. Cuộn bảng trên viewport hẹp.
- **Dữ liệu test (nếu có):** Trang quản lý có dữ liệu dài và biểu mẫu.
- **Kết quả mong đợi:** Nội dung có độ tương phản dễ đọc; trạng thái focus nhìn thấy được; bảng không phá vỡ layout và cách trình bày không làm thay đổi dữ liệu/nghiệp vụ.

### TC-64 — Trạng thái rỗng giỏ hàng và wishlist
- **Mã Test Case:** TC-64
- **Tên chức năng:** Empty state của giỏ hàng và wishlist
- **Điều kiện tiên quyết:** USER A có giỏ trống và wishlist trống.
- **Các bước thực hiện:** 1. Mở giỏ hàng. 2. Mở wishlist. 3. Kiểm tra thông báo và liên kết/hành động được hiển thị.
- **Dữ liệu test (nếu có):** Không có cart item hoặc sản phẩm yêu thích.
- **Kết quả mong đợi:** Mỗi trang hiển thị empty state thay vì bảng/card rỗng hoặc lỗi; không hiện tổng tiền sai và có thể tiếp tục điều hướng theo các liên kết đang có.

### TC-65 — Trạng thái rỗng danh sách và kết quả tìm kiếm
- **Mã Test Case:** TC-65
- **Tên chức năng:** Empty state của danh sách đơn và danh sách quản lý
- **Điều kiện tiên quyết:** Có tài khoản/điều kiện lọc không có đơn cho Buyer, Vendor hoặc Shipper.
- **Các bước thực hiện:** 1. Mở danh sách đơn với tài khoản không có kết quả. 2. Áp dụng bộ lọc tạo kết quả rỗng. 3. Kiểm tra trang quản lý tương ứng.
- **Dữ liệu test (nếu có):** Khoảng ngày hoặc trạng thái không khớp dữ liệu.
- **Kết quả mong đợi:** Hiển thị empty state phù hợp; không có hàng dữ liệu giả, lỗi phân trang hoặc thông tin thuộc tài khoản khác.

### TC-66 — Lọc danh sách Vendor bằng AJAX
- **Mã Test Case:** TC-66
- **Tên chức năng:** Bộ lọc AJAX danh sách sản phẩm Vendor
- **Điều kiện tiên quyết:** Vendor có sản phẩm ở nhiều trạng thái/danh mục; mở trang danh sách sản phẩm.
- **Các bước thực hiện:** 1. Chọn từng bộ lọc đang có trên trang. 2. Theo dõi request và nội dung danh sách. 3. Thay đổi bộ lọc liên tiếp.
- **Dữ liệu test (nếu có):** Trạng thái và danh mục có sẵn trong fixture.
- **Kết quả mong đợi:** Bộ lọc cập nhật danh sách bằng luồng AJAX được trang hỗ trợ, kết quả khớp điều kiện chọn và không tải nhầm dữ liệu của shop khác.

### TC-67 — Lọc không có kết quả và xóa bộ lọc
- **Mã Test Case:** TC-67
- **Tên chức năng:** Xử lý kết quả lọc rỗng
- **Điều kiện tiên quyết:** Trang danh sách có bộ lọc và dữ liệu phù hợp để đối chiếu.
- **Các bước thực hiện:** 1. Chọn điều kiện không có bản ghi khớp. 2. Kiểm tra empty state. 3. Xóa/đặt lại bộ lọc nếu giao diện cung cấp thao tác này.
- **Dữ liệu test (nếu có):** Giá trị lọc không khớp bất kỳ bản ghi nào.
- **Kết quả mong đợi:** Danh sách chuyển sang trạng thái rỗng rõ ràng; thao tác xóa lọc đưa danh sách về dữ liệu ban đầu, không giữ nội dung cũ gây hiểu nhầm.

### TC-68 — Phân trang sau khi lọc
- **Mã Test Case:** TC-68
- **Tên chức năng:** Giữ bộ lọc khi chuyển trang
- **Điều kiện tiên quyết:** Có đủ dữ liệu để danh sách sau lọc có nhiều trang.
- **Các bước thực hiện:** 1. Áp dụng bộ lọc. 2. Chuyển sang trang kế tiếp. 3. Quay lại trang trước và xóa bộ lọc.
- **Dữ liệu test (nếu có):** Bộ lọc trả về ít nhất hai trang kết quả.
- **Kết quả mong đợi:** Danh sách và chỉ báo trang tương ứng; bộ lọc được giữ trong thao tác phân trang nếu trang hỗ trợ; xóa lọc không để lại trang hiện tại ngoài phạm vi kết quả.

### TC-69 — Trạng thái loading của thao tác AJAX
- **Mã Test Case:** TC-69
- **Tên chức năng:** Hiển thị loading khi gửi thao tác bất đồng bộ
- **Điều kiện tiên quyết:** Trang có thao tác AJAX dùng helper giao diện chung.
- **Các bước thực hiện:** 1. Thực hiện thao tác cập nhật giỏ, wishlist hoặc trạng thái đơn. 2. Quan sát nút trong lúc request đang xử lý. 3. Quan sát giao diện sau khi request hoàn tất.
- **Dữ liệu test (nếu có):** Request phản hồi thành công và request có độ trễ trong môi trường kiểm thử.
- **Kết quả mong đợi:** Trạng thái loading được bật cho nút liên quan trong thời gian chờ và được gỡ sau khi hoàn tất; giao diện không để nút mắc kẹt ở trạng thái chờ.

### TC-70 — Ngăn gửi lặp khi đang xử lý
- **Mã Test Case:** TC-70
- **Tên chức năng:** Chống nhấn lặp nút submit
- **Điều kiện tiên quyết:** Có form/thao tác AJAX tạo thay đổi dữ liệu.
- **Các bước thực hiện:** 1. Nhấn submit liên tiếp khi request đầu chưa hoàn tất. 2. Chờ kết quả. 3. Kiểm tra số lần thao tác được tiếp nhận và nội dung nút.
- **Dữ liệu test (nếu có):** Request có độ trễ kiểm soát được.
- **Kết quả mong đợi:** Nút phản ánh trạng thái loading và không tạo nhiều lần gửi ngoài ý muốn; sau khi hoàn tất, giao diện trở lại trạng thái thao tác phù hợp.

### TC-71 — Toast sau thao tác thành công
- **Mã Test Case:** TC-71
- **Tên chức năng:** Thông báo thành công
- **Điều kiện tiên quyết:** Có thao tác thành công dùng flash toast hoặc AJAX toast.
- **Các bước thực hiện:** 1. Thực hiện thao tác hợp lệ, ví dụ cập nhật wishlist hoặc trạng thái đơn được phép. 2. Quan sát toast. 3. Điều hướng/refresh theo luồng thông thường.
- **Dữ liệu test (nếu có):** Dữ liệu hợp lệ.
- **Kết quả mong đợi:** Toast hiển thị thông báo thành công phù hợp với kết quả; không hiển thị thông báo lỗi đồng thời hoặc lặp lại không cần thiết sau điều hướng.

### TC-72 — Toast khi thao tác thất bại
- **Mã Test Case:** TC-72
- **Tên chức năng:** Thông báo lỗi từ phản hồi AJAX
- **Điều kiện tiên quyết:** Có thể tạo lỗi nghiệp vụ đã được backend từ chối, ví dụ transition đơn không hợp lệ.
- **Các bước thực hiện:** 1. Gửi thao tác bị từ chối. 2. Quan sát toast và trạng thái nút. 3. Xác nhận dữ liệu hiển thị trên trang không báo thành công.
- **Dữ liệu test (nếu có):** Trạng thái đơn không được phép chuyển.
- **Kết quả mong đợi:** Giao diện hiển thị lỗi theo phản hồi; trạng thái loading được gỡ; không cập nhật giả thành công hoặc xóa dữ liệu đang xem.

### TC-73 — Phản hồi giao diện khi thiếu CSRF token
- **Mã Test Case:** TC-73
- **Tên chức năng:** Báo lỗi CSRF ở request mutation
- **Điều kiện tiên quyết:** Trang có thao tác AJAX mutation dùng `fetchWithCsrf`.
- **Các bước thực hiện:** 1. Trong môi trường kiểm thử, mở trang không có CSRF token hoặc dùng fixture mô phỏng thiếu token. 2. Thực hiện thao tác mutation. 3. Quan sát request và thông báo.
- **Dữ liệu test (nếu có):** Trang/fixture không có CSRF token.
- **Kết quả mong đợi:** Helper dừng request và hiển thị thông báo yêu cầu tải lại trang/kiểm tra cấu hình CSRF; không báo thao tác thành công.

### TC-74 — Preview ảnh sản phẩm trước khi upload
- **Mã Test Case:** TC-74
- **Tên chức năng:** Xem trước ảnh trong form sản phẩm
- **Điều kiện tiên quyết:** Vendor có quyền mở form tạo/cập nhật sản phẩm.
- **Các bước thực hiện:** 1. Chọn một ảnh JPG/PNG hợp lệ. 2. Quan sát preview trước khi lưu. 3. Thay ảnh bằng một ảnh khác.
- **Dữ liệu test (nếu có):** Ảnh JPG và PNG hợp lệ.
- **Kết quả mong đợi:** Ảnh được preview trong form trước khi upload/lưu; khi chọn ảnh khác, preview phản ánh file mới thay vì ảnh cũ.

### TC-75 — Preview media trong form đánh giá
- **Mã Test Case:** TC-75
- **Tên chức năng:** Xem trước ảnh/video đánh giá
- **Điều kiện tiên quyết:** Buyer đủ điều kiện gửi đánh giá sản phẩm.
- **Các bước thực hiện:** 1. Chọn tối đa số ảnh và video được giao diện hỗ trợ. 2. Quan sát preview và nội dung comment. 3. Gửi đánh giá hợp lệ.
- **Dữ liệu test (nếu có):** Ảnh JPG/PNG và video MP4/WebM hợp lệ.
- **Kết quả mong đợi:** Preview hiển thị đúng media đã chọn; sau khi lưu thành công, review card thể hiện nội dung/media tương ứng.

### TC-76 — Thay hoặc bỏ media đã chọn
- **Mã Test Case:** TC-76
- **Tên chức năng:** Cập nhật lựa chọn media trong form
- **Điều kiện tiên quyết:** Form sản phẩm hoặc đánh giá đang hiển thị preview media.
- **Các bước thực hiện:** 1. Chọn media để tạo preview. 2. Thay file hoặc bỏ lựa chọn theo điều khiển của form. 3. Gửi form nếu còn media hợp lệ.
- **Dữ liệu test (nếu có):** Hai ảnh khác nhau và trường hợp bỏ file.
- **Kết quả mong đợi:** Preview và danh sách file gửi đi đồng bộ với lựa chọn cuối; file đã thay/bỏ không còn xuất hiện trong nội dung gửi.

### TC-77 — Hiển thị lỗi upload media
- **Mã Test Case:** TC-77
- **Tên chức năng:** Thông báo khi upload media không hợp lệ
- **Điều kiện tiên quyết:** Có form upload ảnh/video dùng giới hạn media của hệ thống.
- **Các bước thực hiện:** 1. Chọn file sai định dạng, sai nội dung thực hoặc vượt giới hạn kích thước. 2. Gửi form. 3. Kiểm tra lỗi và dữ liệu form.
- **Dữ liệu test (nếu có):** Ảnh sai magic bytes, file vượt 5 MB hoặc video vượt 50 MB.
- **Kết quả mong đợi:** Hệ thống từ chối file và giao diện hiển thị lỗi; không báo upload thành công hoặc tạo review/sản phẩm với media lỗi.

### TC-78 — Toggle wishlist và đồng bộ biểu tượng
- **Mã Test Case:** TC-78
- **Tên chức năng:** Cập nhật wishlist trên giao diện
- **Điều kiện tiên quyết:** USER A đăng nhập và xem sản phẩm chưa có/trong wishlist.
- **Các bước thực hiện:** 1. Bấm biểu tượng yêu thích. 2. Kiểm tra trạng thái tim/số lượng nếu được hiển thị. 3. Bấm lại và mở trang wishlist.
- **Dữ liệu test (nếu có):** Sản phẩm chưa yêu thích và đã yêu thích.
- **Kết quả mong đợi:** Giao diện phản ánh kết quả toggle AJAX và trang wishlist đồng bộ; thao tác lặp không tạo bản ghi trùng hoặc trạng thái hiển thị sai.

### TC-79 — Cập nhật số lượng và chọn dòng trong giỏ
- **Mã Test Case:** TC-79
- **Tên chức năng:** Đồng bộ giao diện giỏ hàng
- **Điều kiện tiên quyết:** USER A có nhiều dòng hàng và sản phẩm thuộc nhiều shop.
- **Các bước thực hiện:** 1. Tăng/giảm số lượng một dòng. 2. Chọn/bỏ chọn dòng, chọn theo shop và chọn tất cả. 3. Đối chiếu tổng hiển thị và preview checkout.
- **Dữ liệu test (nếu có):** Dòng được chọn và không được chọn; số lượng hợp lệ.
- **Kết quả mong đợi:** Giao diện thể hiện đúng lựa chọn/số lượng đã lưu; tổng và tập sản phẩm checkout phản ánh đúng các dòng được chọn, không tính dòng bị bỏ chọn.

### TC-80 — Hiển thị dòng hàng không còn khả dụng
- **Mã Test Case:** TC-80
- **Tên chức năng:** Trạng thái sản phẩm unavailable trong giỏ
- **Điều kiện tiên quyết:** Giỏ có sản phẩm chuyển sang INACTIVE, hết hàng hoặc không còn thuộc shop được duyệt.
- **Các bước thực hiện:** 1. Mở giỏ hàng. 2. Kiểm tra dòng không còn mua được. 3. Thử chọn và tiếp tục checkout.
- **Dữ liệu test (nếu có):** Sản phẩm INACTIVE/hết hàng.
- **Kết quả mong đợi:** Dòng hàng được nhận biết là không khả dụng và không được đặt mua trái quy tắc; giao diện không hiển thị như một dòng có thể checkout bình thường.

### TC-81 — Hiển thị kết quả áp dụng coupon
- **Mã Test Case:** TC-81
- **Tên chức năng:** Phản hồi giao diện coupon
- **Điều kiện tiên quyết:** USER A đang xem checkout và có coupon hợp lệ cùng coupon không hợp lệ.
- **Các bước thực hiện:** 1. Áp dụng coupon hợp lệ. 2. Quan sát giảm giá/tổng. 3. Thử coupon sai hoặc không đủ điều kiện.
- **Dữ liệu test (nếu có):** Coupon hợp lệ, hết hạn hoặc không đạt điều kiện tối thiểu.
- **Kết quả mong đợi:** Coupon hợp lệ được phản ánh theo preview server; coupon không hợp lệ hiển thị cảnh báo và không làm giảm tổng; không trình bày lỗi coupon như đặt hàng thành công.

### TC-82 — Hiển thị trạng thái preview checkout
- **Mã Test Case:** TC-82
- **Tên chức năng:** Loading và lỗi khi tính preview checkout
- **Điều kiện tiên quyết:** USER A có selected item; có thể mô phỏng phản hồi preview hợp lệ và lỗi.
- **Các bước thực hiện:** 1. Thay đổi lựa chọn/carrier/coupon để yêu cầu preview mới. 2. Quan sát loading và kết quả. 3. Tạo lỗi preview do dữ liệu không còn hợp lệ.
- **Dữ liệu test (nếu có):** Preview thành công và lỗi do sản phẩm/địa chỉ không hợp lệ.
- **Kết quả mong đợi:** Tổng hiển thị chỉ được cập nhật theo dữ liệu preview mới từ server; khi lỗi, giao diện báo lỗi và không giữ tổng cũ như kết quả đã xác nhận.

### TC-83 — Trang kết quả đặt hàng
- **Mã Test Case:** TC-83
- **Tên chức năng:** Hiển thị thành công sau khi đặt đơn
- **Điều kiện tiên quyết:** USER A hoàn tất đặt đơn COD hoặc xác nhận MoMo giả lập thành công.
- **Các bước thực hiện:** 1. Hoàn tất luồng đặt hàng. 2. Mở trang success/chi tiết đơn. 3. Đối chiếu mã nhóm đơn, trạng thái thanh toán và liên kết tiếp theo.
- **Dữ liệu test (nếu có):** Một đơn COD và một MoMo giả lập.
- **Kết quả mong đợi:** Trang hiển thị kết quả tương ứng với trạng thái thực tế; không nhầm COD chưa thu với MoMo đã thanh toán; không tạo thêm đơn khi tải lại trang kết quả.

### TC-84 — Loading và toast khi Vendor/Shipper đổi trạng thái
- **Mã Test Case:** TC-84
- **Tên chức năng:** Phản hồi giao diện cập nhật trạng thái đơn
- **Điều kiện tiên quyết:** Vendor/Shipper có đơn được phép chuyển trạng thái.
- **Các bước thực hiện:** 1. Thực hiện một transition hợp lệ. 2. Quan sát loading, toast và dữ liệu trạng thái sau phản hồi. 3. Thử transition bị từ chối.
- **Dữ liệu test (nếu có):** Transition hợp lệ và transition không hợp lệ theo policy.
- **Kết quả mong đợi:** Trường hợp hợp lệ cập nhật giao diện theo phản hồi; trường hợp lỗi hiển thị toast lỗi và giữ trạng thái cũ; loading luôn được gỡ.

### TC-85 — Form hủy đơn/trả hàng khi validation lỗi
- **Mã Test Case:** TC-85
- **Tên chức năng:** Hiển thị lỗi biểu mẫu đơn hàng
- **Điều kiện tiên quyết:** Buyer có đơn đủ điều kiện mở form hủy hoặc yêu cầu trả hàng.
- **Các bước thực hiện:** 1. Mở form/modal. 2. Gửi thiếu trường bắt buộc hoặc nội dung ngoài giới hạn. 3. Sửa dữ liệu hợp lệ và gửi lại.
- **Dữ liệu test (nếu có):** Lý do OTHER thiếu chi tiết; nội dung trả hàng ngoài giới hạn; thiếu ảnh bắt buộc.
- **Kết quả mong đợi:** Lỗi được hiển thị gần form/field phù hợp, không báo thành công khi validation thất bại; dữ liệu hợp lệ có thể gửi theo luồng hiện có.

### TC-86 — Chatbot không hiển thị cho Guest và sau đăng xuất
- **Mã Test Case:** TC-86
- **Tên chức năng:** Ẩn chatbot khi chưa đăng nhập
- **Điều kiện tiên quyết:** Có thể truy cập trang công khai ở trạng thái Guest và đăng xuất khỏi phiên đang hoạt động.
- **Các bước thực hiện:** 1. Mở trang công khai khi Guest. 2. Đăng nhập, xác nhận chatbot xuất hiện nếu được cung cấp. 3. Đăng xuất mà không đóng tab.
- **Dữ liệu test (nếu có):** Guest và tài khoản đã đăng nhập.
- **Kết quả mong đợi:** Chatbot không hiển thị cho Guest; sau logout cũng không còn nút/chatbox của phiên trước.

### TC-87 — Chatbot trên phiên đã xác thực
- **Mã Test Case:** TC-87
- **Tên chức năng:** Hiển thị chatbot cho người dùng đã đăng nhập
- **Điều kiện tiên quyết:** USER đăng nhập và mở trang có tích hợp chatbot.
- **Các bước thực hiện:** 1. Mở trang. 2. Kiểm tra nút mở chatbot. 3. Mở/đóng khung chat và điều hướng sang trang khác có cùng tích hợp.
- **Dữ liệu test (nếu có):** Phiên USER đang hoạt động.
- **Kết quả mong đợi:** Chatbot được hiển thị theo trạng thái xác thực và thao tác mở/đóng không che khuất nội dung thiết yếu.

### TC-88 — Bố cục tương tác chatbot trên mobile
- **Mã Test Case:** TC-88
- **Tên chức năng:** Sử dụng chatbot ở viewport nhỏ
- **Điều kiện tiên quyết:** Đăng nhập trên thiết bị/view mobile có chatbot.
- **Các bước thực hiện:** 1. Mở chatbot ở mobile. 2. Nhập nội dung và gửi. 3. Đóng khung chat rồi mở lại.
- **Dữ liệu test (nếu có):** Nội dung ngắn và nội dung dài.
- **Kết quả mong đợi:** Khung chat và ô nhập không tràn viewport; nội dung dài đọc được; nút gửi/đóng thao tác được và việc mở lại không làm vỡ layout.

### TC-89 — Mở sơ đồ kiến trúc Archify
- **Mã Test Case:** TC-89
- **Tên chức năng:** Kiểm tra artifact sơ đồ kiến trúc
- **Điều kiện tiên quyết:** Có file `tcgshop-architecture.html` tại thư mục gốc `D:\LTWeb`.
- **Các bước thực hiện:** 1. Mở file trong trình duyệt. 2. Kiểm tra các thành phần cốt lõi, đường liên kết và phần phụ thuộc ngoài. 3. Thu nhỏ viewport và kiểm tra các thẻ/card.
- **Dữ liệu test (nếu có):** File sơ đồ kiến trúc đã tạo.
- **Kết quả mong đợi:** File mở được, sơ đồ và card hiển thị đầy đủ, không có lỗi bố cục rõ ràng; các nhóm thành phần và phụ thuộc vẫn phân biệt được.

### TC-90 — Mở sơ đồ workflow checkout Archify
- **Mã Test Case:** TC-90
- **Tên chức năng:** Kiểm tra artifact luồng mua hàng
- **Điều kiện tiên quyết:** Có file `tcgshop-checkout-workflow.html` tại thư mục gốc `D:\LTWeb`.
- **Các bước thực hiện:** 1. Mở file trong trình duyệt. 2. Theo luồng từ giỏ hàng đến thành công. 3. Kiểm tra các nhánh COD/MoMo, coupon và lỗi hiển thị trên sơ đồ.
- **Dữ liệu test (nếu có):** File workflow checkout đã tạo.
- **Kết quả mong đợi:** Sơ đồ mở và đọc được với dark theme; nhánh được thể hiện theo nội dung code đã truy dấu, không xuất hiện lỗi render hoặc mất nội dung.

### TC-91 — Điều hướng mobile và trạng thái menu
- **Mã Test Case:** TC-91
- **Tên chức năng:** Thao tác menu responsive
- **Điều kiện tiên quyết:** Mở trang có header/sidebar điều hướng ở viewport mobile.
- **Các bước thực hiện:** 1. Mở và đóng menu. 2. Chọn một mục điều hướng. 3. Dùng nút Back của trình duyệt rồi mở menu lại.
- **Dữ liệu test (nếu có):** Viewport mobile; các mục điều hướng hợp lệ theo vai trò.
- **Kết quả mong đợi:** Menu mở/đóng đúng, không che vĩnh viễn nội dung sau điều hướng; chỉ hiển thị mục phù hợp quyền và trạng thái menu không bị kẹt sau Back.

### TC-92 — Thông báo và trạng thái rỗng không che nội dung
- **Mã Test Case:** TC-92
- **Tên chức năng:** Hiển thị toast/empty state ở viewport nhỏ
- **Điều kiện tiên quyết:** Có thể kích hoạt toast và một empty state trên mobile.
- **Các bước thực hiện:** 1. Kích hoạt toast thành công hoặc lỗi. 2. Mở trang có empty state. 3. Kiểm tra khả năng đọc và thao tác trong lúc toast hiển thị.
- **Dữ liệu test (nếu có):** Nội dung toast dài và danh sách không có kết quả.
- **Kết quả mong đợi:** Toast và empty state không chồng lấp nút/field thiết yếu; thông báo có thể đọc được, bố cục không tràn ngang.

## Ghi chú phạm vi và kết quả

- Các mã TC được xếp liên tục theo luồng nghiệp vụ cũ, sau đó bổ sung các kiểm tra giao diện PG từ TC-57 đến TC-92.
- Đếm theo **người được giao**, mỗi TC trong nhóm liên nhóm được tính cho từng người được nêu; đếm danh sách duy nhất, mỗi mã chỉ tính một lần: PU 44 TC riêng + TC-55/56 = **46**; PG 8 TC riêng + TC-55/56 + 36 TC mới = **46**; PA 2 TC riêng + TC-55/56 = **4**. Danh sách có **92 mã TC duy nhất**.
- Theo phạm vi yêu cầu chỉ thêm kịch bản PG, phần PG hiện đã tương đương PU; PA vẫn có ít kịch bản được giao trong tài liệu hiện tại, nên không thể tuyên bố đã cân bằng cả ba người mà không bổ sung hoặc phân công lại phần PA.
- MoMo trong code checkout là **giả lập** qua endpoint `mock-momo/confirm` và `mock-momo/cancel`; các kịch bản không giả định có cổng MoMo thật, chữ ký/IPN hoặc OTP thanh toán.
- Coupon invalid được thể hiện là cảnh báo không giảm ở preview, nhưng bị từ chối nếu vẫn gửi mã khi place; dữ liệu giá/tổng cuối luôn được tính lại ở server.
- Danh sách này là kế hoạch test thủ công dựa trên Controller/Service/DTO và tài liệu hiện có, không phải báo cáo đã chạy. Không chạy test/build; không thay đổi file ngoài tài liệu này; không commit/push.
