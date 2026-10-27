# Import catalog mẫu

`CatalogDataImporter` chỉ chạy khi `app.catalog-import.enabled=true`. Mặc định không bật.

## Chạy

Runner quét tất cả file `*.json` trực tiếp trong `src/main/resources/seed/` qua classpath, sắp xếp theo tên file rồi import từng file độc lập. Cách đọc này cũng hoạt động khi đóng gói JAR. Nội dung được đọc theo cấu trúc `games`, `series`, `cards`, `sealedProducts` và `supplies`; riêng tên `yugioh_catalog.json` chọn shop đích Yu-Gi-Oh! như mô tả dưới đây.

Trong IDE, thêm program argument `--app.catalog-import.enabled=true`, hoặc thêm cấu hình sau vào `application-local.properties` rồi khởi động lại:

```properties
app.catalog-import.enabled=true
```

Cũng có thể dùng Maven Wrapper:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--app.catalog-import.enabled=true"
```

Sau import đặt lại `false`/gỡ argument để không chạy vào các lần khởi động tiếp theo. DataSeeder được chạy trước importer nếu seed được bật. Các catalog thông thường gán sản phẩm mới ngẫu nhiên vào shop `APPROVED` chưa bị xóa. Riêng file `yugioh_catalog.json`, importer tìm shop chưa xóa có tên chứa `Yugi`, `Yu-Gi-Oh` hoặc `YuGiOh` (ưu tiên shop APPROVED) và dùng cùng shop đó cho mọi item trong file. Nếu không tìm thấy shop phù hợp, importer tạo `YuGiOhh` với trạng thái `APPROVED`.

## Dữ liệu và quy tắc

- JSON hiện có chứa `games`, `series`, `cards`, `sealedProducts`, `supplies`. Item cần `name`, `priceVnd > 0`, `imageUrl` HTTPS tại res.cloudinary.com, `imagePublicId`. Thẻ cần thêm `seriesCode`, `number`; sealed cần `seriesCode`, `type` thuộc BOOSTER_BOX/BOOSTER_PACK/BUNDLE. Language mặc định ENGLISH.
- Đối chiếu listing qua các shop theo tên sản phẩm hoặc tên Card. SINGLE đối chiếu thêm seriesCode, số thẻ và ngôn ngữ. Asset catalog (URL hoặc public ID ảnh) giúp nhận diện listing đã được importer tạo, kể cả khi shop được chọn ngẫu nhiên ở lần chạy trước. Nếu chỉ có một listing khớp danh tính thì giữ hành vi đồng bộ ảnh cũ; nếu có nhiều listing của các shop và không xác định được listing nhập catalog thì không ghi đè ảnh của họ, mà tạo listing mới ở một shop APPROVED ngẫu nhiên. Sản phẩm xóa mềm đã nhận diện không được khôi phục tự động.
- Nếu chưa có, tạo Product ACTIVE, stock=10, các bộ đếm=0. SINGLE dùng NEAR_MINT và liên kết Card; Game/Series/Card chưa có được tạo từ metadata JSON. Game tái sử dụng theo tên hoặc mã, không đổi mã hiện có. Không ghi đè dữ liệu Card có sẵn.
- Nếu đã có, chỉ đồng bộ ảnh; giữ giá, tồn kho, trạng thái và dữ liệu kinh doanh. Ưu tiên ảnh cùng publicId/URL, sau đó ảnh chính hiện có; chỉ thêm ảnh khi chưa có ảnh phù hợp/ảnh chính. Đảm bảo chỉ một ảnh chính; gallery khác được giữ lại.
- Import chỉ gán tham chiếu asset Cloudinary đã có sẵn, không upload/tải lại hoặc xóa asset. Các URL/publicId catalog có thể dùng chung; việc dọn tài nguyên cũ cần đánh giá tham chiếu riêng, tránh xóa ảnh của dữ liệu khác.
- Mỗi item chạy trong transaction REQUIRES_NEW; runner bắt lỗi ngoài transaction và tiếp tục qua các item và các file. Với catalog thường, shop được khóa và kiểm tra trạng thái `APPROVED` cùng soft-delete trước khi gán. Catalog Yu-Gi-Oh! gán vào shop đích đã tìm/tạo; các listing khớp với asset catalog cũng được chuyển về đúng shop đích để cả file nhất quán. Runner ghi tên file/item lỗi và tổng sản phẩm mới/ảnh thêm hoặc cập nhật/item lỗi.
- Với SINGLE, importer bảo đảm đủ dữ liệu cho 14 ngày gần nhất tính tới hôm nay bằng cách chỉ thêm các ngày còn thiếu; dữ liệu cũ và các ngày đã có được giữ nguyên. Giá gốc là `Card.marketPrice`, fallback `Product.price` rồi `priceVnd` trong item; hệ số ngẫu nhiên từ 0,95 đến 1,05 (bước 0,0001), làm tròn HALF_UP về đồng. `market_price` và `avg_price` bằng nhau. Lưu `source=ADMIN` theo quy ước DataSeeder/schema hiện có; đây là dữ liệu demo, không phải giá giao dịch thật. Không đổi `Card.marketPrice`, không sinh lịch sử cho sealed/SUPPLY. `priceVnd` trong catalog vẫn là dữ liệu mẫu/tham khảo theo phần meta.

## Kiểm tra

```powershell
.\mvnw.cmd test "-Dtest=CatalogDataImporterTest,CatalogImportServiceImplTest,CatalogImportIntegrationTest,DataSeederTest"
```

Integration test dùng H2 tạm, import tất cả file hiện có hai lần và kiểm tra 389 Product, 389 ProductImage, 332 Card, 4.648 dòng lịch sử; Yu-Gi-Oh! được gán vào shop YuGiOhh fixture, các Product khác thuộc các shop APPROVED còn lại. Chạy lại không tạo bản ghi trùng/đổi giá. Xóa lịch sử trong DB thử nghiệm rồi chạy lại để kiểm tra backfill cho sản phẩm cũ. Unit test kiểm tra quét từng file, tiếp tục sau item lỗi, chọn shop APPROVED, cập nhật ảnh tại chỗ, giữ tồn kho khi chạy lại và yêu cầu bật cấu hình. Không tự chạy import vào Database phát triển trong quá trình kiểm thử.
