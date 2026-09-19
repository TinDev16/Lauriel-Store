# PHASE 7 RESULT

Phase 7 tích hợp Spark Admin vào Spring MVC/Thymeleaf, tái sử dụng nghiệp vụ Phase 1–6.
Báo cáo dưới đây mô tả giao diện và các bước kiểm chứng. Dừng trước Phase 8 Android.

## 1. Files Created

- MVC: `src/main/java/com/lauriel/store/controller/web/`: LoginController, DashboardController,
  WebProductController, WebVariantController, WebSizeController, WebColorController,
  WebSupplierController, WebPurchaseReceiptController, WebInventoryController, WebOrderController,
  WebVoucherController, WebPromotionController, WebCustomerController, WebErrorController.
- Hỗ trợ Web: WebActions, WebModelAdvice, WebViewSupport, WebPaginationInterceptor,
  WebExceptionHandler, WebRoutingExceptionHandler.
- Read service: DashboardService, CustomerService; DTO: CustomerResponse.
- Config: WebFormattingConfig.
- Templates: fragments, auth/login, dashboard, forms/lists của từng module, error/400/403/404/500.
- Static: các file Spark được sử dụng, cộng `assets/css/admin.css` và `assets/js/admin.js`.
- Test: `src/test/java/com/lauriel/store/controller/WebAdminTest.java`.
- Tài liệu: báo cáo này. Danh sách file chính xác nằm ở cuối báo cáo.

## 2. Files Modified

SecurityConfig; GlobalExceptionHandler; ProductService; ProductVariantService; SizeService; ColorService;
SupplierService; OrderService; OrderStatePolicy; VoucherService; PromotionService;
ProductRepository; ProductVariantRepository; CustomerRepository; OrderRepository; PurchaseReceiptRepository;
AuthSecurityTest; README.

AuthSecurityTest chỉ thêm LoginController vào cấu hình test thủ công vì trang login nay do MVC phục vụ.
Không thay assertions, không bỏ test cũ. Không thêm dependency, không đổi pom.xml/schema/entities.

## 3. Template Structure

```text
templates/
├── fragments/ (layout, head, sidebar, header, footer, alerts, fields, pagination)
├── auth/login.html
├── admin/dashboard.html
├── admin/products/ (list, form, variants, variant-form)
├── admin/sizes/ (list, form)
├── admin/colors/ (list, form)
├── admin/suppliers/ (list, form)
├── admin/inventory/ (list, transactions)
├── admin/purchase-receipts/ (list, form, detail)
├── admin/orders/ (list, detail)
├── admin/vouchers/ (list, form)
├── admin/promotions/ (list, form, products)
├── admin/customers/list.html
└── error/ (400, 403, 404, 500)
```

## 4. Static Assets

Copy nguyên bản 7 file từ Spark: main.css, bootstrap.min.css, bootstrap.bundle.min.js,
bootstrap-icons.css, bootstrap-icons.woff/woff2 và favicon.ico. Giữ nguyên đường dẫn tương đối của icon fonts.

Không copy ApexCharts/Flatpickr, dashboard.js hay auth.js vì các trang này không sử dụng.
JS riêng chỉ xử lý confirm và sidebar (breakpoint 1200px khớp Spark); không gọi REST, không có SPA.
CSS riêng bổ sung thumbnail, form/table và trang lỗi; không sửa library hoặc template gốc.

## 5. Login / Logout

- GET /login: trang Spark login chuyển sang Thymeleaf; username/password, autocomplete, CSRF.
- POST /login: Spring Security DaoAuthenticationProvider xử lý BCrypt và account status.
- ADMIN/STAFF login thành công về /admin. CUSTOMER cũng không được truy cập admin.
- Sai credential hoặc INACTIVE/LOCKED về /login?error với thông báo chung.
- POST /logout + CSRF: Spring Security hủy session, về /login?logout.
- GET / chuyển /admin; API vẫn dùng Bearer, không nhận authentication từ Web Session.

## 6. Security Rules

| Role | Web được phép |
|---|---|
| ADMIN | Tất cả module |
| STAFF | Dashboard, Products/Variants, Sizes/Colors, Inventory/Transactions, Suppliers, Purchase receipts, Orders, Customers read-only |
| CUSTOMER | Không được dùng Admin, HTTP 403 |
| Anonymous | Login, assets, trang lỗi; admin chuyển login |

Route nghiệp vụ STAFF được liệt kê trước /admin/** ADMIN-only. Marketing không chỉ ẩn menu:
backend chặn cả GET/POST của STAFF. /admin/probe và các đường admin khác vẫn ADMIN-only.
Web CSRF giữ nguyên; chỉ API stateless tắt CSRF như Phase 2.

## 7. Dashboard

DashboardService read-only chạy các aggregate count thật:
Product ACTIVE, tổng Variant, Order PENDING, Order tạo hôm nay [00:00, 00:00 ngày sau),
tổng Customer, Receipt DRAFT, Variant quantity ≤ 5.

Low-stock bao gồm cả Variant inactive để vẫn theo dõi tồn kho. Không có số liệu hoặc biểu đồ demo giả.

## 8. Product UI

Paged admin list gồm cả ACTIVE/INACTIVE, thumbnail, name, base price, status, batch variant count.
Search tên không phân biệt hoa/thường, escape ký tự wildcard; status filter và pagination.
Create/edit giữ dữ liệu và lỗi server-side trên form; activate/deactivate qua POST, không hard delete.

Variant list có SKU/Size/Color/price/effectivePrice/quantity/status và metadata create/edit.
Quantity chỉ hiển thị; request DTO không có quantity. Variant mới quantity=0.
Update kiểm tra variant thuộc product trên URL; UI không thể sửa variant của product khác.
Sizes/Colors có list/create/edit; không có delete.

## 9. Inventory / Purchase Receipt UI

Inventory paged gồm cả inactive; low-stock threshold optional.
History có variant/type/from/to filter, quantity dương, reference/user/time/note.

Tạo Receipt bằng Supplier picker phân trang, tránh tải toàn bộ Supplier; redirect DRAFT detail.
Tra cứu Variant ID/SKU qua Inventory; add/update/remove từng item, complete/cancel bằng POST.
Complete gọi PurchaseReceiptService, không lặp logic lock/transaction/import.
COMPLETED/CANCELLED chỉ đọc; complete lần hai trả flash error và không tăng kho thêm.
Các thao tác complete/cancel/remove có confirm.

## 10. Order UI

Paged Order list có customer name/ID, status/from/to, totals, voucher discount/final amount/time.
Detail hiển thị shipping address và unitPrice/promotion discount/subtotal/totals lưu trong Order.
Không tính lại pricing. Dropdown lấy transition từ chính OrderStatePolicy qua OrderService.
CANCELLED gọi OrderService.updateStatus: transaction hoàn kho/RETURN/hoàn lượt Voucher như Phase 6.
Transition sai trả flash error; cancel lại không hoàn lần hai.

Customer phone/name và SKU/size/color là thông tin liên quan hiện tại;
schema chưa có snapshot phone/recipient hoặc metadata Product. Snapshot tiền vẫn giữ nguyên.

## 11. Voucher / Promotion UI

ADMIN-only list/create/edit/activate/deactivate đầy đủ fields DTO hiện có, validation percent/dates/precision.
Status update là service riêng, Voucher khóa row và chỉ đổi status để không ghi đè usage quantity.

Promotion dùng discountPercent đúng schema; có list Product association phân trang, add/remove.
Remove chỉ xóa association, không xóa Product. Duplicate association hiển thị flash error.
Không có hard delete Voucher/Promotion.

## 12. Customer UI

ADMIN/STAFF read-only paged list: ID, username, customer name, email/phone, linked account status, createdAt.
Customer không có User hiển thị dấu “—” và “No linked account”; không có account/customer mutation.

## 13. Thymeleaf Fragments

Layout nhận content/title/menu, dùng th:replace cho head/sidebar/header/footer/alerts.
Sidebar active menu theo module; Marketing th:if theo authority ADMIN.
Field/error fragments dùng th:field/th:errors; pagination tái sử dụng.

Money format vi-VN + ₫; datetime dd/MM/yyyy HH:mm.
Form datetime-local dùng ISO formatter; không đổi tiền hoặc thời gian database.

## 14. Validation / Error Handling

Request record DTO được bind qua constructor, @Valid + BindingResult.
Không bind Entity, không cho property mass assignment. Form lỗi validation/business giữ nguyên input.
Action thành công dùng RedirectAttributes.successMessage; lỗi nghiệp vụ dùng errorMessage.

WebExceptionHandler ưu tiên cho MVC, trả HTML status phù hợp, 404/403/500 không lộ SQL/stacktrace.
GlobalExceptionHandler giữ JSON regression, priority thấp hơn Web advice.
WebRoutingExceptionHandler xử lý 404/405/media errors xảy ra trước khi chọn controller:
API delegate formatter JSON cũ, Web render HTML và giữ status/header Allow.
Access denied forward /403, không có vòng redirect.

## 15. CSRF

Web không disable CSRF. Mọi POST form dùng th:action + method=post, Spring/Thymeleaf tự chèn hidden token.
Bao gồm login/logout, create/edit/status, receipt items/complete/cancel, order transition,
promotion association. Test kiểm tra token thực sự xuất hiện trong HTML.
Thiếu CSRF trả 403, không thực hiện mutation.

## 16. Query / DTO Strategy

Giữ spring.jpa.open-in-view=false; Thymeleaf chỉ nhận DTO/ViewModel trong transaction read-only đã hoàn tất.
Admin Product summary projection không tải description/variant collection; count variants theo batch IDs.
Variant metadata dùng entity graph product/size/color và batch pricing, không N+1 Promotion.
Customer paged fetch graph User nullable; Order list batch customer names.
Inventory/Receipt/Order/Marketing tiếp tục dùng DTO và fetch strategy đã kiểm chứng Phase 3–6.

Page size mặc định 12, tối đa 100; kiểm tra page/offset trong service.
Pagination interceptor chỉ giữ whitelist filter đã biết, encode Unicode/&/literal '+' một lần,
tạo URL nội bộ và bảo toàn keyword/status/from/to/size/type/threshold/variantId.

## 17. Tests

WebAdminTest: **78 tests**, sử dụng H2 riêng, fixture commit, BCrypt và session login thật, render Thymeleaf thật,
không có production credential. Bao phủ security/static assets, login/logout/status, CSRF, search/pagination,
validation/conversion/duplicates, quantity protection, receipt inventory/history, transition/cancel/Voucher,
marketing/associations, customers nullable User, mapping errors, dashboard aggregates và historical prices.

Toàn bộ project: **380 total, 379 pass, 1 skip, 0 failures, 0 errors**.
Skip duy nhất: DatabaseRuntimeValidationTest opt-in MySQL.

## 18. Build Result

Java 21 portable; các lệnh Maven Wrapper:
- `.\mvnw.cmd -B clean test`: BUILD SUCCESS.
- `.\mvnw.cmd -B clean package`: BUILD SUCCESS.
- JAR: `target/lauriel-store-0.0.1-SNAPSHOT.jar`, gồm templates/static assets.

Log local bị ignore: .tools/phase7-clean-test.log và .tools/phase7-clean-package.log.

## 19. MySQL Runtime Validation

Chưa được cung cấp DB_URL/DB_USERNAME/DB_PASSWORD/JWT_SECRET.
Không đoán password, không import dump, không thay đổi MySQL. Runtime validation vẫn opt-in.

Khi có credential thật: set các biến như README, chạy application, dùng ADMIN/STAFF có sẵn.
Kiểm tra /login → /admin → /admin/products → /admin/inventory → /admin/orders, sau đó logout.
Không tự tạo account admin mặc định. MockMvc/H2 thành công chưa chứng minh runtime MySQL.
Chưa kiểm tra trực quan trên trình duyệt thật trong môi trường này.

## 20. Remaining Issues

- Cần chạy kiểm chứng MySQL và kiểm tra responsive trực quan với credential thật.
- Schema limitation giữ từ Phase 6: selected promotion/rule version, voucher code/recipient/phone snapshot chưa có.
- Variant picker/Supplier picker đơn giản; không có dynamic SPA.
- Không mở rộng Android/payment/return/PDF/Excel/advanced reporting.
- Không bắt đầu Phase 8.

## 21. Git Diff Summary

So sánh SHA-256 file trước Phase 7 với sau Phase 7 để tách phần bổ sung,
vì phần lớn backend từ phase trước vẫn untracked trong Git.
Original spark-admin-1.0.0/ và database/lauriel.sql được kiểm tra không đổi.
Không tạo commit, không thay dependency hoặc cấu trúc database.

Created: 72; Modified: 18

### Created paths

```text
docs/phase7-result.md
src/main/java/com/lauriel/store/config/WebFormattingConfig.java
src/main/java/com/lauriel/store/controller/web/DashboardController.java
src/main/java/com/lauriel/store/controller/web/LoginController.java
src/main/java/com/lauriel/store/controller/web/WebActions.java
src/main/java/com/lauriel/store/controller/web/WebColorController.java
src/main/java/com/lauriel/store/controller/web/WebCustomerController.java
src/main/java/com/lauriel/store/controller/web/WebErrorController.java
src/main/java/com/lauriel/store/controller/web/WebExceptionHandler.java
src/main/java/com/lauriel/store/controller/web/WebInventoryController.java
src/main/java/com/lauriel/store/controller/web/WebModelAdvice.java
src/main/java/com/lauriel/store/controller/web/WebOrderController.java
src/main/java/com/lauriel/store/controller/web/WebPaginationInterceptor.java
src/main/java/com/lauriel/store/controller/web/WebProductController.java
src/main/java/com/lauriel/store/controller/web/WebPromotionController.java
src/main/java/com/lauriel/store/controller/web/WebPurchaseReceiptController.java
src/main/java/com/lauriel/store/controller/web/WebRoutingExceptionHandler.java
src/main/java/com/lauriel/store/controller/web/WebSizeController.java
src/main/java/com/lauriel/store/controller/web/WebSupplierController.java
src/main/java/com/lauriel/store/controller/web/WebVariantController.java
src/main/java/com/lauriel/store/controller/web/WebViewSupport.java
src/main/java/com/lauriel/store/controller/web/WebVoucherController.java
src/main/java/com/lauriel/store/dto/response/CustomerResponse.java
src/main/java/com/lauriel/store/service/CustomerService.java
src/main/java/com/lauriel/store/service/DashboardService.java
src/main/resources/static/assets/css/admin.css
src/main/resources/static/assets/css/main.css
src/main/resources/static/assets/images/favicon.ico
src/main/resources/static/assets/js/admin.js
src/main/resources/static/assets/libs/bootstrap-icons/bootstrap-icons.css
src/main/resources/static/assets/libs/bootstrap-icons/fonts/bootstrap-icons.woff
src/main/resources/static/assets/libs/bootstrap-icons/fonts/bootstrap-icons.woff2
src/main/resources/static/assets/libs/bootstrap/css/bootstrap.min.css
src/main/resources/static/assets/libs/bootstrap/js/bootstrap.bundle.min.js
src/main/resources/templates/admin/colors/form.html
src/main/resources/templates/admin/colors/list.html
src/main/resources/templates/admin/customers/list.html
src/main/resources/templates/admin/dashboard.html
src/main/resources/templates/admin/inventory/list.html
src/main/resources/templates/admin/inventory/transactions.html
src/main/resources/templates/admin/orders/detail.html
src/main/resources/templates/admin/orders/list.html
src/main/resources/templates/admin/products/form.html
src/main/resources/templates/admin/products/list.html
src/main/resources/templates/admin/products/variant-form.html
src/main/resources/templates/admin/products/variants.html
src/main/resources/templates/admin/promotions/form.html
src/main/resources/templates/admin/promotions/list.html
src/main/resources/templates/admin/promotions/products.html
src/main/resources/templates/admin/purchase-receipts/detail.html
src/main/resources/templates/admin/purchase-receipts/form.html
src/main/resources/templates/admin/purchase-receipts/list.html
src/main/resources/templates/admin/sizes/form.html
src/main/resources/templates/admin/sizes/list.html
src/main/resources/templates/admin/suppliers/form.html
src/main/resources/templates/admin/suppliers/list.html
src/main/resources/templates/admin/vouchers/form.html
src/main/resources/templates/admin/vouchers/list.html
src/main/resources/templates/auth/login.html
src/main/resources/templates/error/400.html
src/main/resources/templates/error/403.html
src/main/resources/templates/error/404.html
src/main/resources/templates/error/500.html
src/main/resources/templates/fragments/alerts.html
src/main/resources/templates/fragments/fields.html
src/main/resources/templates/fragments/footer.html
src/main/resources/templates/fragments/head.html
src/main/resources/templates/fragments/header.html
src/main/resources/templates/fragments/layout.html
src/main/resources/templates/fragments/pagination.html
src/main/resources/templates/fragments/sidebar.html
src/test/java/com/lauriel/store/controller/WebAdminTest.java
```

### Modified paths

```text
src/main/java/com/lauriel/store/config/SecurityConfig.java
src/main/java/com/lauriel/store/exception/GlobalExceptionHandler.java
src/main/java/com/lauriel/store/repository/CustomerRepository.java
src/main/java/com/lauriel/store/repository/OrderRepository.java
src/main/java/com/lauriel/store/repository/ProductRepository.java
src/main/java/com/lauriel/store/repository/ProductVariantRepository.java
src/main/java/com/lauriel/store/repository/PurchaseReceiptRepository.java
src/main/java/com/lauriel/store/service/ColorService.java
src/main/java/com/lauriel/store/service/OrderService.java
src/main/java/com/lauriel/store/service/OrderStatePolicy.java
src/main/java/com/lauriel/store/service/ProductService.java
src/main/java/com/lauriel/store/service/ProductVariantService.java
src/main/java/com/lauriel/store/service/PromotionService.java
src/main/java/com/lauriel/store/service/SizeService.java
src/main/java/com/lauriel/store/service/SupplierService.java
src/main/java/com/lauriel/store/service/VoucherService.java
src/test/java/com/lauriel/store/security/AuthSecurityTest.java
README.md
```
