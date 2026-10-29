# Import catalog mẫu

`CatalogDataImporter` chỉ chạy khi `app.catalog-import.enabled=true`. Mặc định không bật.

## Chạy

Runner quét tất cả file `*.json` trực tiếp trong `src/main/resources/seed/` qua classpath, sắp xếp theo tên file rồi import từng file độc lập. Cách đọc này cũng hoạt động khi đóng gói JAR. Nội dung được đọc theo cấu trúc `games`, `series`, `cards`, `sealedProducts` và `supplies`; tất cả catalog đều được phân phối theo cùng một quy tắc cho các chi nhánh.

Trong IDE, thêm program argument `--app.catalog-import.enabled=true`, hoặc thêm cấu hình sau vào `application-local.properties` rồi khởi động lại:

```properties
app.catalog-import.enabled=true
```

Cũng có thể dùng Maven Wrapper:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--app.catalog-import.enabled=true"
```

Tắt cờ/gỡ argument sau khi import để các lần khởi động sau không quét catalog ngoài ý muốn. Runner chỉ nhập vào các Shop có trạng thái `APPROVED` và chưa bị xóa mềm. Mỗi item được áp dụng cho từng Shop: nếu Shop chưa có listing trùng thì tạo Product mới cho Shop đó; nếu đã có thì bỏ qua và không thay đổi giá, tồn kho, trạng thái, ảnh hay dữ liệu kinh doanh hiện có. Vì vậy, import lại là idempotent đối với các listing đã tạo, còn Shop mới được duyệt sẽ nhận catalog ở lần import kế tiếp. Nên sao lưu DB trước lần chạy đầu tiên vì số listing tăng theo số Shop đã duyệt.

## Dữ liệu và quy tắc

- JSON hiện có chứa `games`, `series`, `cards`, `sealedProducts`, `supplies`. Item cần `name`, `priceVnd > 0`, `imageUrl` HTTPS tại `res.cloudinary.com`, `imagePublicId`. Thẻ cần thêm `seriesCode`, `number`; sealed cần `seriesCode`, `type` thuộc `BOOSTER_BOX`/`BOOSTER_PACK`/`BUNDLE`. Language mặc định là `ENGLISH`; tình trạng thẻ mặc định là `NEAR_MINT`. Có thể truyền `cardCondition` và `edition` tùy chọn.
- Shop được lấy theo ID và khóa/kiểm tra lại trạng thái `APPROVED` cùng cờ chưa xóa trong transaction cho mỗi item. Các Shop bị xóa mềm hoặc không còn được duyệt trong lúc import không nhận Product.
- Listing SINGLE được đối chiếu trong phạm vi từng Shop theo Card number, Series, language và condition. Các loại còn lại được đối chiếu theo tên Product và loại sản phẩm. Listing đã có — kể cả listing xóa mềm — không bị tạo đè, chuyển Shop, cập nhật ảnh, giá hoặc tồn kho. Nếu một Shop đang có nhiều listing trùng tiêu chí, importer giữ nguyên chúng và không tạo thêm listing cho Shop đó.
- Listing mới được tạo riêng cho từng Shop, sử dụng chung Card/Series/Game phù hợp. Giá và mô tả được lấy từ catalog; trạng thái khởi tạo là `ACTIVE`, stock=10, các bộ đếm=0. Ảnh catalog được lưu dưới dạng tham chiếu Cloudinary có sẵn; importer không upload, tải lại hoặc xóa asset.
- Game/Series/Card chưa có sẽ được tạo từ metadata JSON; dữ liệu Card có sẵn không bị ghi đè. Game được tái sử dụng theo mã/tên; Card SINGLE theo Series, số thẻ và language.
- Với SINGLE, lịch sử giá thuộc Card dùng chung giữa các Shop. Importer chỉ bổ sung các ngày còn thiếu trong 14 ngày gần nhất tính tới hôm nay, không tạo lịch sử lặp theo số listing hoặc ghi đè các bản ghi có sẵn. Giá gốc là `Card.marketPrice`, fallback `Product.price` rồi `priceVnd` của item; hệ số mẫu ngẫu nhiên từ 0,95 đến 1,05, bước 0,0001, làm tròn `HALF_UP` về đồng. `market_price` và `avg_price` bằng nhau, `source=ADMIN`. Đây là dữ liệu demo, không phải giá giao dịch thật; không đổi `Card.marketPrice` và không sinh lịch sử cho sealed/SUPPLY.
- Mỗi item chạy trong transaction `REQUIRES_NEW`; runner ghi lỗi theo file/item và tiếp tục xử lý các item khác. Số sản phẩm mới trong log là tổng listing được tạo trên tất cả Shop; số Card và lịch sử giá chỉ tính theo dữ liệu dùng chung.

## Kiểm tra

Các bài kiểm thử liên quan nằm trong `CatalogDataImporterTest`, `CatalogImportServiceImplTest`, `CatalogImportIntegrationTest` và `DataSeederTest`. Integration test dùng H2 tạm, tạo ba Shop đã duyệt và kỳ vọng mỗi Shop nhận 373 listing (1.119 Product/ảnh tổng cộng), dùng chung 332 Card và 4.648 dòng lịch sử giá. Import lần hai không tạo Product/ảnh/lịch sử trùng; xóa lịch sử trong DB thử nghiệm rồi import lại kiểm tra khả năng bổ sung dữ liệu giá còn thiếu. Đây chỉ là hướng dẫn; không tự chạy importer hoặc test trên Database phát triển.
