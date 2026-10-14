# CUSTOMER WEB STOREFRONT RESULT

## 1. Architecture

Luồng storefront là `Browser -> Spring MVC Controller -> Service -> Repository -> MySQL`. Thymeleaf gọi trực tiếp các service hiện hữu; không gọi HTTP ngược vào `/api/**` và không tạo backend thứ hai.

## 2. Routes

- Public: `/`, `/shop`, `/shop/products`, `/shop/products/{id}`, `/shop/cart`, `/login`, `/register`.
- Mutation giỏ hàng: `/shop/cart/add`, `/update`, `/remove`, `/clear`, `/buy-now`.
- CUSTOMER: `/shop/checkout`, `/shop/orders`, `/shop/orders/{id}`, `/shop/orders/{id}/success`, `/shop/profile`, `/shop/profile/edit`.

## 3. Security

Web dùng form login + session và giữ CSRF. API Android tiếp tục dùng JWT stateless. CUSTOMER không vào được `/admin/**`; ADMIN/STAFF đăng nhập về `/admin`; CUSTOMER trở về trang shop đã lưu hoặc `/shop`.

## 4. Home

Trang chủ có hero, ưu đãi thật, danh mục động và sản phẩm mới. `/` chuyển về `/shop`.

## 5. Promotion Banners

Hiển thị ba nhóm dữ liệu thật: product promotion, order threshold promotion và voucher; không hard-code chương trình khuyến mãi.

## 6. Category / Search / Filters

Header có tìm kiếm. Danh sách hỗ trợ `keyword`, `categoryId`, `sizeId`, `colorId`, `sort`, `page`, và panel filter thu gọn trên màn hình nhỏ.

## 7. Product List

Phân trang 12 sản phẩm mặc định, giới hạn tối đa 24. Card dùng ảnh chính, danh mục, giá gốc, giá hiệu lực và badge giảm giá.

## 8. Product Detail

Có breadcrumb, gallery ảnh thật, mô tả, danh mục, giá, promotion, SKU, tồn kho, số lượng, CTA và 4–6 sản phẩm tương tự.

## 9. Variant Selection

JavaScript chỉ cho phép tổ hợp size/màu tồn tại trong `ProductVariant`. Chọn một thuộc tính sẽ khóa lựa chọn không tương thích; biến thể hợp lệ cập nhật SKU, giá và tồn kho.

## 10. Add to Cart

POST có CSRF, kiểm tra biến thể đang hoạt động và số lượng không vượt tồn kho; phản hồi bằng flash alert tiếng Việt.

## 11. Buy Now

Buy Now lưu một checkout state tạm trong session, tách khỏi giỏ hàng thường và được dọn sau khi đặt hàng.

## 12. Cart

Session chỉ lưu `variantId -> quantity`. Khi render, server tải lại product/variant/pricing. Có gộp cùng biến thể, cập nhật, xóa, xóa toàn bộ, liên kết về sản phẩm và kiểm tra tồn kho.

## 13. Checkout

Checkout yêu cầu CUSTOMER, hiển thị địa chỉ, danh sách nhiều sản phẩm, ưu đãi, COD và price summary. Submit được khóa sau lần nhấn đầu. `OrderService` tính và kiểm tra lại toàn bộ dữ liệu.

## 14. Voucher

Có vùng nhập mã, danh sách voucher khả dụng, trạng thái đang áp dụng và nút bỏ mã. Mỗi preview đều gọi `VoucherService` để xác thực lại.

## 15. Order Promotion

Order threshold promotion được `PricingService` tự chọn và hiển thị riêng trong price summary.

## 16. Shipping Fee

Phí giao hàng lấy từ kết quả pricing backend; cấu hình hiện tại là 20.000 VNĐ.

## 17. Orders

Danh sách đơn có status chips, phân trang, trạng thái và tổng tiền. Chi tiết chỉ tải đơn thuộc customer hiện tại; hỗ trợ xem sản phẩm, promotion, voucher, shipping và hủy đơn qua `OrderService`.

## 18. Profile

Customer xem và sửa họ tên, email, điện thoại, địa chỉ qua `CustomerService`. Form không cung cấp role, status, password hash hoặc user ID.

## 19. Responsive UI

CSS có layout desktop/laptop/tablet/mobile với breakpoint 992, 768 và 420 px; grid 4 cột desktop, 2–3 cột tablet và 1–2 cột mobile. Navigation, filter, gallery, cart, checkout và order chuyển layout theo viewport.

## 20. Shared Components

Các fragment dùng chung gồm layout, header, navigation, footer, alerts, product card, promotion card và price summary.

## 21. Backend Reuse

Web/API/Mobile dùng chung `ProductService`, `PricingService`, `VoucherService`, `OrderService`, `CustomerService`, `CategoryService`, `PromotionService` và `OrderPromotionService`. Web order tiếp tục phát `OrderCreatedEvent`, nên dùng chung Telegram notification sau commit.

## 22. Files Created

- 9 MVC/advice class trong `controller/web/store`.
- 4 DTO/view model storefront, `StoreCartService`, `RoleAwareAuthenticationSuccessHandler`.
- 11 page template và 8 shared fragment/template auth.
- `store.css`, `store.js`, `product-detail.js`, ảnh placeholder.
- `WebStorefrontTest`.

## 23. Files Modified

- `SecurityConfig`: phân quyền shop, auth session và role-aware redirect; API JWT không đổi.
- `LoginController`: root về shop và dùng login chung theo role.
- `auth/login.html`: giao diện storefront nhẹ nhưng vẫn dùng được cho admin/staff.
- `TestSchema`: đọc đúng `database/lariel.sql` và tương thích collation/schema hiện tại.

## 24. Tests

`WebStorefrontTest` kiểm tra public pages, gallery, register validation, CSRF, cart merge/different variant/update/remove/stock, Buy Now, anonymous protection, CUSTOMER/admin boundary, pricing kết hợp, voucher, checkout, ownership, cancel/restore stock và profile. Toàn bộ suite: **442 tests, 0 failures, 0 errors, 1 skipped**.

## 25. Build Result

- `./mvnw.cmd clean test`: SUCCESS.
- `./mvnw.cmd clean package`: SUCCESS.
- Artifact: `target/lauriel-store-0.0.1-SNAPSHOT.jar` (65.1 MB).

## 26. MySQL Runtime

JAR đã khởi động thành công trên cổng kiểm tra 18080, kết nối `jdbc:mysql://localhost:3306/lauriel`. JWT secret dùng cho smoke test được sinh ngẫu nhiên trong tiến trình và không ghi vào source. Telegram được tắt trong smoke test để không gửi thông báo ngoài ý muốn.

## 27. Manual Browser Verification

Không có trình duyệt đồ họa trong phiên làm việc. HTTP smoke test trên MySQL thật xác nhận `200` cho home, products, product detail có gallery, cart, login, register và static CSS; anonymous checkout trả `302` về `/login`. Tiến trình cổng 18080 đã được dừng sau kiểm tra.

## 28. Remaining Issues

Không còn lỗi chức năng đã biết. Cần kiểm tra trực quan cuối trên Chrome/Edge ở 1920, 1366, 768 và 390 px vì phiên này không có browser UI. Telegram end-to-end không được gửi thật; formatter/service đã nằm trong suite test hiện hữu.

## 29. Git Diff Summary

Thay đổi được giới hạn trong Customer Web Storefront, auth routing/security liên quan và test support. Không thay đổi entity/schema production, REST API contract, logic admin, mobile hoặc migration database.
