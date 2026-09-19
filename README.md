# Lauriel Store

Hệ thống bán quần áo Web Admin + Android, dùng chung backend Spring Boot.
Phase 7 đã tích hợp Web Admin Thymeleaf với Spark: Session Login, Dashboard, Catalog, Inventory/Purchase receipt, Orders, Marketing và Customer read-only. Backend Authentication/Catalog/Inventory/Order/Voucher/Promotion tiếp tục dùng chung service. Payment và Return flow chưa triển khai.

## Công nghệ

- Java 21 LTS, Spring Boot 4.0.8, Maven 3.9.11 qua Maven Wrapper.
- Spring MVC, Spring Data JPA, Jakarta Validation, MySQL Connector/J.
- Thymeleaf phục vụ Web Admin MVC; Lombok dùng getter/setter và khóa ghép.
- Spring Security 7: BCrypt, JWT HS256 cho API và Session/Form Login cho Web Admin.

## Chuẩn bị MySQL

Database đích là **lauriel**. Schema tham chiếu: `database/lauriel.sql`.
Ứng dụng yêu cầu schema đã tồn tại và chỉ chạy Hibernate **validate**.
SQL initialization bị tắt; ứng dụng không tự import dump hoặc thay đổi bảng.

Nếu schema chưa tồn tại, có thể import thủ công bằng MySQL Workbench:

1. Sao lưu database đích nếu có dữ liệu.
2. Chọn **Server → Data Import → Import from Self-Contained File** và chọn dump.
3. Chọn đúng schema đích `lauriel` trước khi import.

Dump chứa `DROP TABLE`, `GTID_PURGED` và `SQL_LOG_BIN`. Chỉ import vào schema
đã xác định phù hợp; các statement GTID/binlog có thể yêu cầu quyền bổ sung.
Giữ dump gốc; nếu cần điều chỉnh cách import, dùng bản sao đã kiểm tra.

## Cấu hình và chạy

Cài JDK 21 và đặt `JAVA_HOME` trỏ tới JDK. Wrapper tự tải Maven lần đầu nên cần Internet.

| Biến môi trường | Mặc định local |
|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/lauriel` |
| `DB_USERNAME` | `root` |
| `DB_PASSWORD` | Rỗng; đặt bằng credential local của bạn |
| `JWT_SECRET` | Bắt buộc: Base64 của ít nhất 32 byte ngẫu nhiên |
| `JWT_EXPIRATION` | `PT1H` (ISO-8601 duration, tối thiểu 1 giây) |
| `ROLE_INITIALIZATION_ENABLED` | `true`; chỉ thêm role còn thiếu |

Cần đặt JWT_SECRET hợp lệ trước khi chạy; xem phần Authentication bên dưới.

PowerShell:

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/lauriel'
$env:DB_USERNAME = 'your-local-user'
# Đặt DB_PASSWORD trong môi trường local; không commit credential.
.\mvnw.cmd spring-boot:run
```

Linux/macOS, sau khi đặt các biến môi trường:

```sh
sh ./mvnw spring-boot:run
# Hoặc chmod +x mvnw, rồi ./mvnw spring-boot:run
```

`.env.example` chỉ là tài liệu: Spring Boot không tự đọc `.env`.

Nếu chạy backend bằng nút Run trong IntelliJ IDEA, đặt `JWT_SECRET` ở
**Run → Edit Configurations → LaurielStoreApplication → Modify options → Operating System → Environment variables** (nếu mục này đang bị ẩn).
Tạo một secret Base64 từ 32 byte ngẫu nhiên bằng PowerShell (chạy một lần, lưu riêng cho
môi trường local và dùng lại sau mỗi lần khởi động):

```powershell
$jwtKeyBytes = New-Object byte[] 32
$jwtRandom = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$jwtRandom.GetBytes($jwtKeyBytes)
[Convert]::ToBase64String($jwtKeyBytes)
$jwtRandom.Dispose()
```

Không điền `JWT_SECRET` vào **Active profiles**: mục này chỉ nhận tên Spring profile. Nếu log ghi `The following 1 profile is active: "JWT_SECRET"`, hãy xóa mục đó rồi đặt biến trong **Environment variables**.

Sao chép chuỗi được in ra vào `JWT_SECRET` trong Run Configuration; không thêm dấu nháy,
không ghi secret vào `application.properties` hoặc commit lên Git. Biến đặt trong một
PowerShell khác sẽ không tự truyền vào IDE đang mở. Nếu dùng `mvnw spring-boot:run`
ngay trong PowerShell, đặt `$env:JWT_SECRET = '<chuỗi vừa tạo>'` trong chính cửa sổ đó.
Server mặc định chạy cổng 8080. Mở `/login` để đăng nhập bằng tài khoản ADMIN/STAFF có sẵn trong database; đăng nhập thành công chuyển `/admin`. `/` chuyển về dashboard. Startup thành công được xác nhận qua log `Started LaurielStoreApplication`.

## Build và test

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd clean package
java -jar target/lauriel-store-0.0.1-SNAPSHOT.jar
```

Linux/macOS dùng `sh ./mvnw clean test` và `sh ./mvnw clean package`.

Test mặc định không cần MySQL: đối chiếu ORM với dump gốc, kiểm tra ProductService
và định dạng lỗi HTTP. Phase 2 thêm test filter chain JWT/session/CSRF và transaction trên H2 memory riêng, tạo từ CREATE TABLE trong dump. Không chạy DROP, GTID hoặc SQL dump trên MySQL local. Các test này không thay thế Hibernate validation với MySQL thật.

Khi MySQL và credential đã sẵn sàng, bật test runtime **chỉ đọc schema**:

```powershell
# Cần cấu hình JWT_SECRET hợp lệ trước khi chạy test này.
$env:LAURIEL_DB_VALIDATION = 'true'
.\mvnw.cmd '-Dtest=DatabaseRuntimeValidationTest' test
Remove-Item Env:LAURIEL_DB_VALIDATION
```

## Mapping quan trọng

- 17 Entity map 17 bảng; tiền dùng `BigDecimal`, DATETIME dùng `LocalDateTime`.
- OrderDetail, PurchaseReceiptDetail và PromotionProduct dùng `EmbeddedId/MapsId`.
- Customer → User là one-to-one nullable; Payment → Order là many-to-one.
- Relationship ưu tiên LAZY, không cascade remove; không dùng Lombok `@Data` trên Entity.
- MySQL sở hữu timestamp tự sinh; Hibernate đọc lại giá trị sau insert/update.
- Enum giữ đúng SQL, gồm `BANKING`, `RETURNED`, `MOMO`, `VNPAY`;
  việc có enum không có nghĩa các nghiệp vụ/cổng thanh toán này đã triển khai.

`database/` và `spark-admin-1.0.0/` được giữ nguyên làm nguồn tham chiếu.
## Authentication (Phase 2)

JWT dùng HS256 qua Spring Security OAuth2 Resource Server/Jose; API `/api/**` stateless,
không dùng Web Session, không redirect sang HTML login. CSRF chỉ tắt cho API Bearer.
Web Admin dùng Session/Form Login và giữ CSRF; `/login` là trang Spark/Thymeleaf. POST logout yêu cầu CSRF.

Tạo secret **một lần** cho môi trường local rồi lưu trong cấu hình môi trường riêng;
không đổi key mỗi startup và không commit secret:

```powershell
$jwtKeyBytes = New-Object byte[] 32
$jwtRandom = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$jwtRandom.GetBytes($jwtKeyBytes)
$env:JWT_SECRET = [Convert]::ToBase64String($jwtKeyBytes)
$jwtRandom.Dispose()
$env:JWT_EXPIRATION = 'PT1H'
```

Startup từ chối secret thiếu, Base64 sai, ngắn hơn 32 byte hoặc expiration không hợp lệ.
JWT chỉ chứa `iss`, `sub` (user ID), `role`, `iat`, `exp`. Mỗi request có token hợp lệ
đọc User + Role bằng một query; user bị xóa/khóa/inactive hoặc role đã đổi sẽ trả 401.

| Endpoint | Access | Kết quả |
|---|---|---|
| `POST /api/auth/register` | Public | 201, tạo CUSTOMER + profile trong cùng transaction |
| `POST /api/auth/login` | Public | 200, JWT Bearer và thời gian hiệu lực |
| `GET /api/auth/me` | JWT hợp lệ | 200, userId/username/role |

Register nhận `username`, `password`, `fullName` và tùy chọn `email`, `phone`.
Login nhận `username`, `password`. DTO từ chối trường không hỗ trợ, gồm role/roleId/status/isAdmin.
Password đăng ký tối thiểu 8 ký tự, tối đa 72 byte UTF-8 để phù hợp BCrypt.
Email rỗng được lưu NULL. Duplicate username/email trả 409; login sai, account khóa/inactive
và token sai/hết hạn trả 401 với message chung; thiếu quyền trả 403. Response không có password/hash.

Quy tắc chuẩn bị cho phase sau (không tạo endpoint minh họa trong production):

- `/api/admin/**`: ADMIN; các mutation catalog được liệt kê dưới đây cho phép ADMIN hoặc STAFF. Web `/admin` và các module nghiệp vụ cho ADMIN/STAFF; `/admin/vouchers`, `/admin/promotions` và các route admin khác chỉ ADMIN.
- `/api/staff/**`, `/staff/**`: ADMIN hoặc STAFF.
- `/api/customer/**`: CUSTOMER.
- `/api/auth/me` và API còn lại: cần JWT; register/login POST, các GET catalog và GET /api/promotions/active là public.
- Web còn lại: ADMIN hoặc STAFF; CUSTOMER không truy cập các trang quản trị.

Role initializer chỉ insert ADMIN/STAFF/CUSTOMER còn thiếu theo `role_name`, không phụ thuộc ID,
không sửa/xóa role hoặc tạo tài khoản. Có thể tắt bằng `ROLE_INITIALIZATION_ENABLED=false`
khi role đã được chuẩn bị. Test MySQL runtime luôn tắt initializer để chỉ đọc schema.
Không có tài khoản ADMIN/STAFF hoặc password mặc định.

## Product Catalog (Phase 3)

| Endpoint | Quyền | Thành công |
|---|---|---|
| `GET /api/products` | Public | 200, PageResponse |
| `GET /api/products/{id}` | Public | 200, detail |
| `GET /api/products/{productId}/variants` | Public | 200, variant list |
| `GET /api/sizes`, `GET /api/colors` | Public | 200 |
| `POST /api/admin/products` | ADMIN/STAFF | 201 |
| `PUT /api/admin/products/{id}` | ADMIN/STAFF | 200 |
| `PATCH /api/admin/products/{id}/status` | ADMIN/STAFF | 200 |
| `POST /api/admin/products/{productId}/variants` | ADMIN/STAFF | 201 |
| `PUT /api/admin/product-variants/{variantId}` | ADMIN/STAFF | 200 |
| `POST /api/admin/sizes`, `POST /api/admin/colors` | ADMIN/STAFF | 201 |
| `PUT /api/admin/sizes/{id}`, `PUT /api/admin/colors/{id}` | ADMIN/STAFF | 200 |

Mutation không token trả JSON 401; CUSTOMER trả JSON 403. Chỉ các method/path catalog
cụ thể được mở; các API admin khác vẫn yêu cầu ADMIN. Không có endpoint hard-delete.
Public chỉ thấy Product ACTIVE và Variant ACTIVE; detail/variant list của Product
inactive hoặc không tồn tại trả 404. Product chưa có variant vẫn có thể hiển thị.

Ví dụ: `GET /api/products?page=0&size=12&keyword=shirt&sizeId=1&colorId=2&sortBy=price&direction=asc`.
Page mặc định 0, size 12, giới hạn 1–100; response gồm content/page/size/totalElements/totalPages.
Sort whitelist price/productName/createdAt, mặc định createdAt desc, thêm productId để ổn định thứ tự.
Keyword tìm không phân biệt hoa/thường trong tên và description; ký tự %, _ và ! được escape
để tìm literal. Size/color kết hợp phải khớp cùng một variant ACTIVE. Specification dùng
EXISTS tránh lặp Product và sai count khi nhiều variant khớp.

Product POST/PUT nhận productName, description (tùy chọn), price, imgUrl (tùy chọn), status.
PATCH status chỉ nhận status. Variant POST/PUT nhận sizeId, colorId, sku, price, status.
Size/Color nhận sizeName/colorName. Status bắt buộc dùng ACTIVE/INACTIVE; field không hỗ trợ
như id, variants, quantity trong request bị từ chối 400. PUT cập nhật toàn bộ metadata của đối tượng.

Giá dùng BigDecimal không âm, tối đa DECIMAL(12,2). Product.price là giá hiển thị cơ sở;
detail trả basePrice, mỗi variant trả giá riêng. Sửa giá variant không sửa giá Product.
Product name tối đa 200, SKU 100, size name 20, color name 50 ký tự; TEXT tối đa 65535 byte UTF-8.
Thiếu quan hệ trả 404; trùng SKU hoặc bộ Product/Size/Color trả 409, kể cả variant inactive.
Tên Size/Color được trim và kiểm tra trùng không phân biệt hoa/thường. Lỗi constraint được
chuyển thành JSON 409 không lộ SQL/stack trace.

Quantity chỉ thuộc ProductVariant; variant mới luôn có quantity=0. Catalog không nhận
quantity trong create/update. DynamicUpdate chỉ ghi cột metadata thay đổi, tránh ghi lại
quantity từ snapshot cũ khi tồn kho đã thay đổi. Nghiệp vụ tồn kho dành cho Phase 4.
Ảnh chỉ lưu imgUrl, chưa có upload hoặc tích hợp cloud.

List dùng DTO projection, không lấy description/variants. Detail lấy Product và graph
Variant/Size/Color bằng hai query, không đổi relationship sang EAGER. Test Hibernate statistics
xác nhận list có count và detail đều dùng hai query trên fixture nhiều variant (không tính
query xác thực khi gửi JWT).

Test catalog dùng schema CREATE TABLE từ dump trong H2 riêng, bao gồm HTTP/security,
search/filter/pagination, validation, unique constraint, giá/tồn kho và query count.
MySQL runtime validation vẫn là kiểm tra opt-in ở trên; H2 không thay thế kiểm tra MySQL thật.

## Inventory & Purchase Receipt (Phase 4)

Tất cả endpoint dưới `/api/staff/suppliers`, `/api/staff/purchase-receipts` và
`/api/staff/inventory` yêu cầu JWT của ADMIN hoặc STAFF. CUSTOMER nhận JSON 403;
không token nhận JSON 401. Security rule `/api/staff/**` từ Phase 2 đã bao phủ các API này.

| Endpoint | Hành vi |
|---|---|
| `GET /api/staff/suppliers` | Danh sách phân trang |
| `GET /api/staff/suppliers/{id}` | Chi tiết nhà cung cấp |
| `POST /api/staff/suppliers` | Tạo, 201 |
| `PUT /api/staff/suppliers/{id}` | Sửa metadata/status, 200 |
| `GET /api/staff/purchase-receipts` | Danh sách phân trang, không tải items |
| `GET /api/staff/purchase-receipts/{id}` | Chi tiết với items |
| `POST /api/staff/purchase-receipts` | Tạo DRAFT, 201 |
| `POST /api/staff/purchase-receipts/{id}/items` | Thêm dòng hàng, 201 |
| `PUT /api/staff/purchase-receipts/{id}/items/{variantId}` | Sửa dòng hàng DRAFT, 200 |
| `DELETE /api/staff/purchase-receipts/{id}/items/{variantId}` | Xóa dòng hàng DRAFT, 200 |
| `POST /api/staff/purchase-receipts/{id}/complete` | Nhập kho và chuyển COMPLETED, 200 |
| `POST /api/staff/purchase-receipts/{id}/cancel` | DRAFT → CANCELLED, 200 |
| `GET /api/staff/inventory` | Tồn kho các variant, kể cả inactive |
| `GET /api/staff/inventory/{variantId}` | Tồn kho một variant |
| `GET /api/staff/inventory/low-stock?threshold=5` | Quantity <= threshold |
| `GET /api/staff/inventory/transactions` | Lịch sử phân trang |

Các list mặc định page=0, size=12, tối đa size=100, có totalElements/totalPages.
Threshold mặc định 5, không âm. History hỗ trợ variantId, type, from, to (ISO local datetime,
bao gồm hai đầu mốc thời gian); mặc định sort createdAt desc rồi transactionId desc.

Supplier request gồm supplierName, phone/email/address tùy chọn và status ACTIVE/INACTIVE.
Không thêm unique constraint cho Supplier vì schema không có. Deactivate qua PUT;
không có hard-delete Supplier hoặc Receipt. Supplier inactive vẫn được tham chiếu theo schema.

Receipt create request chỉ nhận supplierId. Backend lấy createdBy từ JWT, đặt totalAmount=0.
Schema thật dùng DRAFT/COMPLETED/CANCELLED, không có PENDING. Add item request gồm
variantId, quantity, unitPrice (tên đúng `unit_price` trong SQL). Update item chỉ nhận quantity,
unitPrice; variantId lấy từ path. Quantity > 0; unitPrice >= 0 và đúng DECIMAL(12,2).
Composite key (receipt_id, variant_id) được giữ nguyên. Duplicate item trả 409.
Backend tính lineTotal/subtotal và totalAmount bằng BigDecimal, kiểm tra giới hạn DECIMAL(14,2).
Client không được gửi userId/createdByUserId/status/totalAmount/subtotal hoặc field không hỗ trợ.

Create/add/update/delete item chưa làm thay đổi stock. Complete bắt buộc có ít nhất một item;
receipt đã COMPLETED/CANCELLED trả 409 khi sửa/complete/cancel. Cancel DRAFT không tạo history
hoặc thay đổi stock; không hỗ trợ đảo ngược phiếu COMPLETED trong phase này.

Complete chạy trong một service transaction:

1. Khóa Receipt bằng PESSIMISTIC_WRITE, kiểm tra DRAFT.
2. Đọc items mà chưa fetch quantity của variant; tính lại subtotal/total từ số lượng và đơn giá.
3. InventoryService khóa từng ProductVariant bằng PESSIMISTIC_WRITE theo variantId tăng dần.
4. Kiểm tra quantity mới không vượt Integer.MAX_VALUE, rồi tăng stock và ghi IMPORT cho mỗi item.
5. Lưu receipt COMPLETED và commit. Bất kỳ lỗi nào rollback cả quantity, history và trạng thái.

InventoryService.importReceipt là thao tác nội bộ dùng propagation MANDATORY; receipt service
sở hữu transaction và receipt lock. Mọi biến động stock hiện có đi qua lớp này; catalog chỉ khởi tạo
variant mới quantity=0 và không cho sửa stock. Phase 5 bổ sung SALE/RETURN cho Order; ADJUSTMENT và API set quantity trực tiếp chưa triển khai. InventoryTransaction lưu quantity dương, referenceType=PURCHASE_RECEIPT,
referenceId=receiptId, user là người hoàn tất (có thể khác người tạo phiếu).

Các read dùng EntityGraph trên quan hệ to-one; list receipt không tải details. Detail dùng hai query
(receipt + graph items), inventory/history/receipt list với count dùng hai query trên fixture kiểm thử.
Lock thứ tự ổn định giảm nguy cơ deadlock; khóa receipt chống complete lặp và sửa items trong khi nhập.

Integration test dùng H2 riêng, fixtures đã commit để kiểm tra rollback thật. Có test submit lại,
failure sau history đầu tiên, overflow, hai lần complete đồng thời trên cùng receipt và hai receipt
nhập cùng các variant. Kết quả H2 không thay thế kiểm tra locking/schema trên MySQL thật;
MySQL runtime validation vẫn là test opt-in ở phần Build và test.

## Order & Checkout (Phase 5)

| Endpoint | Quyền | Thành công |
|---|---|---|
| `POST /api/orders` | CUSTOMER | Checkout, 201 |
| `GET /api/orders/my-orders` | CUSTOMER | Đơn của chính mình, phân trang, 200 |
| `GET /api/orders/{id}` | CUSTOMER | Chi tiết đơn của mình, 200 |
| `POST /api/orders/{id}/cancel` | CUSTOMER | Hủy và hoàn kho, 200 |
| `GET /api/staff/orders` | ADMIN/STAFF | Danh sách phân trang, 200 |
| `GET /api/staff/orders/{id}` | ADMIN/STAFF | Chi tiết, 200 |
| `PATCH /api/staff/orders/{id}/status` | ADMIN/STAFF | Chuyển trạng thái hoặc hủy, 200 |

Không có Order endpoint public. Không token trả JSON 401; sai role trả JSON 403.
ADMIN/STAFF không checkout qua customer API. Customer xem/hủy đơn của người khác nhận 404,
giống đơn không tồn tại. Customer profile lấy qua userId từ JWT; thiếu profile trả 404,
không tạo profile ngầm. List customer luôn scope bằng profile hiện tại, không dùng customerId
client gửi để đổi chủ đơn. List mặc định page=0, size=12, tối đa 100, sort createdAt/orderId desc.
Staff list hỗ trợ status/from/to (ISO local datetime, bao gồm hai đầu mốc thời gian).

Checkout request:

```json
{
  "items": [{"variantId": 10, "quantity": 2}],
  "shippingAddress": "Địa chỉ nhận hàng"
}
```

Items bắt buộc, tối đa 100 entries; variantId/quantity dương. Entries trùng variant được cộng
quantity trước khi lock, có kiểm tra Integer overflow; một variant chỉ tạo một OrderDetail.
Shipping address bắt buộc, tối đa 65535 byte UTF-8, được lưu snapshot trong Order.shippingAddress.
Request từ chối field không hỗ trợ ở cả cấp Order và item, gồm customerId/userId/unitPrice/
subtotal/totalAmount/discountAmount/finalAmount/voucherId. VoucherCode là optional từ Phase 6. Phone, recipient và note không có
cột tương ứng trong Order nên chưa nhận qua checkout.

Checkout chạy trong một OrderService transaction:

1. Xác định Customer từ userId đã xác thực, normalize quantities.
2. InventoryService khóa variant bằng PESSIMISTIC_WRITE theo ID tăng dần.
3. Kiểm tra Product/Variant ACTIVE và stock đủ; thiếu stock trả 409.
4. Đọc ProductVariant.price sau khi khóa; tính subtotal/total bằng BigDecimal, kiểm tra DECIMAL(14,2).
5. Tạo Order PENDING và OrderDetail với composite key (order_id, variant_id), snapshot unitPrice.
6. InventoryService dùng chính các variant đã khóa để trừ stock và ghi SALE cho từng item.
7. Commit cùng Order/Details/stock/history; lỗi bất kỳ bước nào rollback toàn bộ.

Product.price không được dùng làm giá checkout. Từ Phase 6, Promotion ảnh hưởng unitPrice và
Voucher ảnh hưởng Order.discountAmount theo semantics bên dưới. Không tạo Payment giả hoặc
tích hợp gateway. Giá Variant thay đổi không làm thay đổi snapshot Order cũ.

Transition policy:

| Trạng thái hiện tại | Staff chuyển tiếp | Customer hủy | Staff hủy |
|---|---|---|---|
| PENDING | CONFIRMED | Có | Có |
| CONFIRMED | PROCESSING | Có | Có |
| PROCESSING | SHIPPING | Không | Có |
| SHIPPING | DELIVERED | Không | Không |
| DELIVERED/CANCELLED/RETURNED | Không | Không | Không |

Không bỏ qua bước, chuyển ngược hoặc chuyển lại cùng status. RETURNED có trong SQL nhưng
nghiệp vụ return order chưa triển khai; PATCH sang RETURNED trả 409. Invalid enum trả 400.
Staff hủy bằng PATCH status=CANCELLED, dùng cùng business method hoàn kho với customer cancel.

Cancel/update status luôn khóa Order bằng PESSIMISTIC_WRITE. Cancel kiểm tra ownership/state,
đọc OrderDetails mà chưa fetch quantity hiện tại, xác nhận lịch sử SALE của Order khớp các items,
rồi khóa variant theo ID tăng dần. InventoryService hoàn stock và ghi RETURN quantity dương,
referenceType=ORDER/referenceId=orderId, user là người thao tác. Chuyển CANCELLED và commit
trong cùng transaction. Cancel lại trả 409, không hoàn stock lần hai. Đơn cũ không có SALE đầy đủ
bị chặn hoàn kho; cần đối chiếu dữ liệu thay vì tự tăng stock. Restock overflow trả 409 và rollback.

Stock mutation nội bộ dùng propagation MANDATORY. decreaseForSale kiểm tra managed variant
đang giữ PESSIMISTIC_WRITE bằng EntityManager.getLockMode, không query/lock lại khi pricing đã
khóa. Lock ID tăng dần được dùng thống nhất cho nhập hàng, checkout và restock. Không có API
thay stock tùy ý; Catalog tiếp tục không nhận quantity.

Order responses dùng DTO. List không tải items; detail fetch graph Variant/Product/Size/Color
bằng query riêng. Query-count test xác nhận Staff list có count và detail dùng 2 query;
Customer list/detail dùng 3 query do thêm bước resolve profile, chưa tính query xác thực JWT.
Không đổi quan hệ sang EAGER.

Order giữ snapshot địa chỉ và giá. Schema thiếu snapshot recipient/phone; SKU/tên Product/Size/Color
trong response lấy từ dữ liệu catalog hiện tại vì SQL chưa có cột snapshot tương ứng.

Integration test dùng fixture đã commit trong H2 riêng: strict requests, ownership, pricing,
rollback khi detail thứ hai hoặc history lỗi, cancel lặp, restock rollback, overflow và query count.
Concurrency tests gồm hai khách tranh stock cuối cùng, hai cancel đồng thời, cancel vs status update.
H2 không thay thế MySQL runtime validation; test MySQL opt-in vẫn yêu cầu credential/JWT_SECRET
và giữ ddl-auto=validate.

## Voucher & Promotion (Phase 6)

Schema thật: Voucher có DiscountType PERCENT/FIXED; Promotion chỉ có discount_percent,
không có discount_type/discount_value nên chưa hỗ trợ FIXED Promotion. Voucher/Promotion không
có created_at. Giữ nguyên SQL, composite PromotionProduct key và ddl-auto=validate/sql.init.mode=never.

Assumptions được chọn vì schema không mô tả usage/discount semantics:

- Voucher.quantity là số lượt còn lại toàn hệ thống, không phải tổng giới hạn hoặc giới hạn mỗi user.
- OrderDetail.discountAmount là snapshot Promotion discount trên mỗi đơn vị hàng.
  OrderDetail.unitPrice là giá sau Promotion; subtotal=unitPrice*quantity, không trừ discount lần nữa.
  Giá gốc mỗi đơn vị có thể suy ra unitPrice+discountAmount.
- Order.totalAmount là tổng subtotal sau Promotion; Order.discountAmount chỉ là Voucher discount;
  finalAmount=totalAmount-discountAmount. Không stack nhiều Promotion.

| Endpoint | Quyền | Hành vi |
|---|---|---|
| `POST /api/vouchers/validate` | CUSTOMER | Preview từ items, 200 hoặc invalid 400 |
| `GET /api/admin/vouchers` | ADMIN | List phân trang |
| `GET /api/admin/vouchers/{id}` | ADMIN | Chi tiết |
| `POST /api/admin/vouchers` | ADMIN | Tạo, 201 |
| `PUT /api/admin/vouchers/{id}` | ADMIN | Sửa/deactivate, 200 |
| `GET /api/promotions/active` | Public GET | Active theo status và thời gian, phân trang |
| `GET /api/admin/promotions` | ADMIN | List phân trang, không tải Product graph |
| `GET /api/admin/promotions/{id}` | ADMIN | Chi tiết config |
| `POST /api/admin/promotions` | ADMIN | Tạo, 201 |
| `PUT /api/admin/promotions/{id}` | ADMIN | Sửa/deactivate, 200 |
| `GET /api/admin/promotions/{id}/products` | ADMIN | Product associations, phân trang |
| `POST /api/admin/promotions/{id}/products/{productId}` | ADMIN | Thêm association, 201; duplicate 409 |
| `DELETE /api/admin/promotions/{id}/products/{productId}` | ADMIN | Chỉ xóa association, 204 |

Không hard-delete Voucher/Promotion. STAFF không đọc/sửa marketing config; CUSTOMER mutation
admin nhận JSON 403; anonymous protected API nhận JSON 401. Chỉ GET /api/promotions/active được
mở public; không mở toàn bộ /api/promotions hoặc /api/admin. List mặc định page=0,size=12,max100.

VoucherRequest nhận code, voucherName, discountType, discountValue, minOrderAmount,
maxDiscountAmount (optional), quantity, startDate, endDate, status. Min/cap/quantity không âm;
value >0; PERCENT <=100. Code trim + uppercase Locale.ROOT, kiểm tra length sau normalization
và unique không phân biệt hoa/thường; DB unique vẫn là lớp bảo vệ cuối. PromotionRequest nhận
promotionName, description (optional), discountPercent >0 <=100, startDate, endDate, status.
Các số tiền/percent kiểm tra precision/scale đúng SQL; startDate < endDate; status ACTIVE/INACTIVE.
Description TEXT tối đa 65535 byte UTF-8. Field không hỗ trợ bị từ chối 400.

PricingService dùng Clock server local, một now chung cho mỗi lần tính pricing. Active cần
startDate <= now <= endDate và status ACTIVE. Các Promotion candidate phải liên kết đúng Product;
tất cả variants của Product được xét cùng candidates, theo giá gốc riêng của từng Variant.
Chọn discount thực tế lớn nhất; bằng nhau chọn promotionId thấp nhất. Promotion không có usage.
Giảm percent làm tròn HALF_UP đến 2 chữ số tại mức unit discount; effectivePrice=base-discount.
Voucher PERCENT tính trên total sau Promotion, FIXED là số tiền cố định; áp maxDiscountAmount,
clamp discount <= total và final >=0. Không sửa ProductVariant.price trong database.

Checkout thêm optional voucherCode; null/blank bỏ qua Voucher, nhưng Promotion vẫn được xét.
Request không nhận voucherId hoặc giá/total client. Ví dụ:

```json
{
  "items": [{"variantId": 10, "quantity": 2}],
  "shippingAddress": "Địa chỉ nhận hàng",
  "voucherCode": "SALE10"
}
```

Checkout: normalize items → lock Variants ID tăng dần → batch resolve Promotion → snapshot
unitPrice/discountAmount → tính total → lock Voucher PESSIMISTIC_WRITE → validate eligibility
(status/time/minimum/remaining/value/cap) → decrement quantity một lượt → tạo Order/Details →
stock decrease và SALE → commit. Voucher invalid trả 400, không silently bỏ mã và checkout giá khác.
Voucher association được lưu vào Order; Order detail trả voucherCode và snapshot tiền.

Preview nhận code và items giống Checkout, tính giá database và Promotion server-side. Preview
không đọc amount client, không khóa ghi, không reserve stock, không decrement Voucher và không
bắt buộc stock đủ. Nó không bảo đảm mã/stock vẫn khả dụng lúc checkout; Checkout kiểm tra lại.
Variant không tồn tại trả 404; điều kiện Voucher invalid trả 400 với message chung.

Cancel: lock Order → validate ownership/state → verify SALE → lock/restock Variants → RETURN →
lock Voucher theo ID và restore quantity +1 nếu Order có Voucher → CANCELLED → commit. Restore
vẫn thực hiện khi Voucher đã expired/inactive. Cancel lại 409 và không restore lần hai. Overflow
counter trả 409; lỗi bất kỳ bước nào rollback stock/history/usage/state. Admin PUT Voucher cũng
khóa cùng row trước khi sửa config/remaining quantity; không có migration/version column.

Catalog list giữ Product.price làm giá cơ sở, không đưa một effectivePrice chung sai cho nhiều
Variant. Variant public/detail thêm effectivePrice và promotion info; không promotion thì
price=effectivePrice, promotion=null. Public variant query fetch graph và candidates trong một
query, group theo variantId để tránh duplicate. Checkout/preview lấy candidates theo Product IDs
bằng một batch query. Query-count test giữ Product detail 2 query kể cả nhiều active Promotion,
Order staff detail có Voucher cũng 2 query; không đổi relationships sang EAGER.

GET Order cũ chỉ đọc snapshot unitPrice/discountAmount/subtotal/total/final từ Order; không tính
lại pricing. Schema không lưu selected promotionId/name/rule version, hoặc voucher code snapshot
riêng; voucherCode là code hiện tại của Voucher được tham chiếu. Giá/discount lịch sử vẫn giữ.
ShippingAddress snapshot có; recipient/phone snapshot chưa có cột. Payment và RETURNED/return
order hoàn chỉnh chưa triển khai. Phase 7 Web Admin đã tích hợp như phần dưới.

Test Phase 6 gồm pure pricing math/time/rounding và HTTP integration trên H2 riêng với fixture
đã commit: preview, CRUD/security, associations, snapshots, voucher consumption/restoration,
rollback sau decrement/restore, cuối counter concurrency, lock order và N+1. Test cũ giữ nguyên
assertions; ProductServiceTest chỉ cập nhật mock dependency sang PricingService. MySQL runtime
validation vẫn là test opt-in khi có credential và JWT_SECRET; không import dump trên MySQL.


## Phase 7 — Web Admin Thymeleaf

Báo cáo đầy đủ, file list và giới hạn kiểm chứng: [docs/phase7-result.md](docs/phase7-result.md).

| Web path | ADMIN | STAFF |
|---|---|---|
| /admin, /admin/dashboard | Có | Có |
| /admin/products (variants trong từng product), /admin/sizes, /admin/colors | Có | Có |
| /admin/inventory, /admin/inventory/transactions | Read-only | Read-only |
| /admin/suppliers, /admin/purchase-receipts | Có | Có |
| /admin/orders | Theo OrderStatePolicy | Theo OrderStatePolicy |
| /admin/customers | Read-only | Read-only |
| /admin/vouchers, /admin/promotions | Có | 403 |

CUSTOMER không được dùng Admin. Login/assets public; anonymous vào Admin chuyển login.
POST /login do Spring Security xử lý username/password/BCrypt; thành công về /admin;
sai credential hoặc account INACTIVE/LOCKED về /login?error.
Logout dùng POST + CSRF, về /login?logout. Web Session không thay thế JWT cho REST.

Mỗi form POST dùng th:action để Thymeleaf tự chèn CSRF. MVC Controller gọi Service trực tiếp.
Validation dùng @Valid/BindingResult, lỗi nghiệp vụ giữ form hoặc flash sau redirect.
Web errors là HTML; REST errors giữ JSON, kể cả lỗi mapping trước khi chọn controller.
Giữ open-in-view=false, Hibernate validate, không import dump/đổi schema/vendor template.

Catalog list gồm cả inactive, có search/status/pagination và batch Variant count.
Variant quantity chỉ đọc, create luôn zero; nhập kho qua DRAFT Purchase Receipt.
Complete/Cancel Receipt và transition/cancel Order gọi transaction service có sẵn;
không lặp logic lock, SALE/RETURN hoặc Voucher restore.
Voucher/Promotion ADMIN-only; Product association chỉ thêm/bỏ liên kết.
Dashboard aggregate dữ liệu thật; Customer nullable User hiển thị phù hợp.
Money vi-VN + ₫, datetime dd/MM/yyyy HH:mm; pagination giữ filter và encode query.

Test hiện tại: **380 total, 379 pass, 1 skip, 0 failures/errors**.
78 test Web mới render Thymeleaf/CSRF, dùng session login BCrypt thật và H2 fixture commit.
Test security cũ giữ assertions; chỉ thêm LoginController vào cấu hình test thủ công.
MySQL runtime vẫn opt-in, cần credential thật; không có account ADMIN mặc định.
Phase 7 đã hoàn thành; Phase 8 Android được mô tả bên dưới.

## Phase 8 — Android CUSTOMER App

Android Java/XML project nằm riêng trong [mobile/](mobile/README.md). App gồm Splash, đăng ký/đăng nhập
JWT, Home/Shop, Product Variant, Cart local theo user, Voucher preview, Checkout, Orders/Cancel,
Profile và Logout. Dùng REST API Phase 2–6; **không thay Backend**.
Gradle Wrapper 8.13 / AGP 8.13.2 / JDK 17, minSdk 26. Debug emulator dùng
`http://10.0.2.2:8080/`; release cần HTTPS URL thật.

Báo cáo chi tiết: [docs/phase8-result.md](docs/phase8-result.md).
Android `clean testDebugUnitTest assembleDebug` thành công với 41/41 unit test; APK debug trong
`mobile/app/build/outputs/apk/debug/`. Chưa kiểm chứng end-to-end MySQL do chưa có credential/JWT secret.
Không triển khai Payment Gateway hay các chức năng ngoài Phase 8.
