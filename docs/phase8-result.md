# PHASE 8 RESULT

## 1. Android Project Structure

```text
mobile/
├── app/src/main/
│   ├── java/com/lauriel/mobile/
│   │   ├── data/api, data/model, data/local, data/repository
│   │   ├── ui/auth, ui/home, ui/product, ui/cart, ui/checkout, ui/order, ui/profile
│   │   ├── adapter, util
│   ├── res/layout, res/menu, res/values, res/drawable
│   └── AndroidManifest.xml
├── app/src/debug/ (chỉ cho phép cleartext host emulator)
├── app/src/test/ (JUnit, MockWebServer, Robolectric)
├── gradle/wrapper/
├── settings.gradle, build.gradle, gradle.properties
└── README.md
```

Java/XML project độc lập với Spring Boot source; package `com.lauriel.mobile`.

## 2. Files Created

66 file trong `mobile/`: Gradle project/wrapper, 30 Java production files, 3 test files,
15 XML layout, resources/manifest/debug network config và mobile README.
Ngoài ra `docs/phase8-result.md` được tạo.

## 3. Files Modified

`README.md` tại root cập nhật liên kết Phase 8. **Không sửa Backend**:
không thay REST controllers, DTO, services, entities, schema, security hoặc `pom.xml`.

## 4. Dependencies

Android Gradle Plugin 8.13.2; Gradle Wrapper 8.13; compileSdk 36, targetSdk 35, minSdk 26; Java 17.
AppCompat, Material Components, RecyclerView, Lifecycle ViewModel/LiveData, Retrofit 2.11,
Gson converter, OkHttp 4.12, Glide 4.16.
Test: JUnit 4, MockWebServer, Robolectric.
AGP 8.13/Gradle/JDK/API compatibility theo
[Android Developer release notes](https://developer.android.com/build/releases/agp-8-13-0-release-notes).

## 5. Navigation Architecture

Splash kiểm tra token và `GET /api/auth/me`. Không token/invalid → Login.
Sau CUSTOMER login → MainActivity với 5 Fragment: Home, Shop, Cart, Orders, Profile.
Product Detail, Checkout và Order Detail là Activity cho flow riêng.

## 6. Authentication

Register đúng 5 field backend: username, email optional, password, fullName, phone optional.
Register 201 quay lại Login; không tự đăng nhập. Login POST backend lưu token/userId/username/role;
ADMIN/STAFF bị chặn trước khi mở Main. Password không lưu.
Splash so khớp CUSTOMER và userId từ `/api/auth/me`.
JWT Bearer tự thêm qua OkHttp; 401 của request có token xóa session và điều hướng Login.

## 7. Home

Home gọi Product page đầu và `GET /api/promotions/active`. Không có Promotion thì ẩn phần banner.
Không tạo Popular/Featured giả. Product card dùng URL ảnh backend với placeholder/error của Glide.

## 8. Product Catalog

Shop RecyclerView 2 cột, submit search (không gọi mỗi ký tự), dialog Size/Color lấy từ
`/api/sizes` và `/api/colors`. Filter giữ lựa chọn còn lại và có Clear.
ViewModel giữ state qua rotation, phân trang page++/load-more, chặn request trùng,
hủy request cũ khi filter đổi, loại ID trùng và dừng tại trang cuối.
Summary chỉ có base price; không bịa effective price cho Product card.

## 9. Product Detail / Variant

Gọi `GET /api/products/{id}` và chỉ hiển thị Size–Color combination có Variant thật.
Hiển thị price/effectivePrice/promotion backend, gạch giá gốc khi sale; không tự tính discount.
Stock 0 vô hiệu hóa Add to Cart, quantity tối đa theo stock đã biết; server vẫn kiểm tra lại khi checkout.

## 10. Cart

SharedPreferences JSON + Gson, namespace `cart_<userId>`.
Lưu variantId/productId/name/SKU/size/color/imgUrl/displayPrice/quantity/knownStock.
Thêm cùng Variant gộp quantity, chỉnh +/-/xóa, không vượt stock đã biết.
CartStore dùng revision và đồng bộ lại từ preferences khi instance khác thay đổi.
Subtotal chỉ là preview BigDecimal. User B không thấy Cart User A.
Checkout success clear Cart sau 201; failure giữ Cart. Nếu Cart đổi khi request đang chạy,
không xóa phiên bản mới hơn.

## 11. Voucher

Checkout gọi `POST /api/vouchers/validate` với `code` và `items[{variantId,quantity}]`.
Preview lấy `orderAmount`, `discountAmount`, `finalAmount` từ server.
Đổi code hoặc Cart revision làm preview cũ hết hiệu lực; phải áp dụng lại.

## 12. Checkout

Request chính xác:

```json
{
  "items": [{"variantId": 15, "quantity": 2}],
  "shippingAddress": "12 Main St",
  "voucherCode": "WEB10"
}
```

`voucherCode` optional. Không gửi price, total, discount, customerId hay userId.
CheckoutUseCase clear Cart sau response thành công có Order; 400/409/network giữ Cart.
Có success dialog → Order Detail hoặc tiếp tục mua. Giá/stock/voucher cuối cùng do backend quyết định.

## 13. Orders

`GET /api/orders/my-orders` phân trang, trạng thái tiếng Việt; `GET /api/orders/{id}`
hiển thị snapshot unitPrice/promotion discount/subtotal/totals/voucher/shipping address.
Chỉ PENDING/CONFIRMED hiện nút Hủy; gọi POST cancel với confirm.
409 dẫn tới reload Order, backend xử lý hoàn kho/voucher.

## 14. Profile / Logout

`GET /api/auth/me` hiển thị username/role. Không invent API profile sâu hơn.
Logout xóa token/identity, về Login; Cart per-user vẫn lưu riêng và không lộ sang user khác.

## 15. API Architecture

StoreApi Retrofit khớp endpoint hiện có; Auth/Product/Order/Voucher client repositories
tách khỏi Fragment/Activity. ApiClient tập trung URL/OkHttp/Gson/401/error mapping.
Call async, loading/error/empty/retry cơ bản. Không dùng Web Admin route hoặc gọi HTTP localhost
từ Backend. ISO LocalDateTime lưu thành String và format an toàn cho UI; money dùng BigDecimal.

## 16. UI / Design

Brand LAURIEL, nền trắng/off-white, chữ đen, accent tối, Material button, ít màu.
colors.xml/strings.xml/dimens.xml tập trung token UI chính. Login/Register/Checkout cuộn trên
điện thoại nhỏ; Product image lớn; RecyclerView với ViewHolder/Glide.
Không dùng logo/artwork Nike hoặc fake catalog.

## 17. Security

SessionManager SharedPreferences chỉ lưu token/userId/username/role; không lưu password/JWT signing secret.
`android:allowBackup=false`. Debug HTTP chỉ cho domain `10.0.2.2` trong Network Security Config,
release không cho cleartext và đặt URL HTTPS riêng.
Android docs xác nhận Network Security Config điều khiển cleartext theo domain từ API 24:
[security config](https://developer.android.com/privacy-and-security/security-config),
[application manifest](https://developer.android.com/guide/topics/manifest/application-element).
Thiết bị thật qua HTTP cần thêm đúng LAN IP vào debug config, không hard-code IP cá nhân final.
Bearer token không log.

## 18. Tests

**41 Android unit tests: 41 pass, 0 fail/error/skip.**
- 13 ApiClient/HTTP/DTO tests: login payload, Bearer, public no token, 401 clear,
  voucher preview request, checkout payload, order list/detail, cancel/409, effective price.
- 25 Session/Cart/format/error tests: customer-only session, persistence/logout, cart merge/quantity,
  per-user isolation, revision, payload only IDs/qty, BigDecimal, ISO, business error mapping.
- 3 CheckoutUseCase + MockWebServer tests: 201 clear Cart, stock 409 giữ Cart, voucher 400 giữ Cart.

Smoke test trên AVD Pixel_5 (Android 15): APK cài thành công, Splash → Login → Register hiển thị và điều hướng đúng; không có AndroidRuntime crash. Đã sửa inset system bars sau khi phát hiện tiêu đề Register chồng status bar. Không chạy instrumentation UI test/visual regression tự động; chưa có backend credentials.

## 19. Android Build

`.\gradlew.bat clean testDebugUnitTest assembleDebug --no-daemon --console plain`:
**BUILD SUCCESSFUL**. APK debug `mobile/app/build/outputs/apk/debug/app-debug.apk`, khoảng 7 MB.
Gradle wrapper/SDK/JDK local đã xác nhận. Manifest merged giữ Internet permission,
debug network security config và backup disabled.

## 20. Backend Changes

**NONE.** Tất cả dữ liệu cần cho Phase 8 đã có. Không tạo endpoint mới và không đổi response contract.
Giới hạn hiện tại: Product summary chỉ có base price; `/api/auth/me` chỉ có identity cơ bản.

## 21. Backend Regression

Không sửa Backend nên không cần chạy lại Maven theo yêu cầu mục 79.
Baseline Phase 7 đã kiểm chứng **380 tests: 379 pass, 1 opt-in MySQL skip**.
Android tests dùng MockWebServer/Robolectric; không đụng database.

## 22. Remaining Issues

- Chưa kiểm chứng end-to-end với Spring Boot + MySQL thật vì chưa có credential/JWT secret.
- Cần thử toàn bộ shopping flow trên thiết bị/emulator với backend chạy để đánh giá ảnh, spacing và tương tác thực tế.
- Release build cần HTTPS backend URL thật thay `https://example.invalid/`.
- Không có profile chi tiết, payment, return order, wishlist, push hay admin mobile (ngoài phạm vi).

## 23. Git Diff Summary

Phase 8 thêm **66 file `mobile/` + báo cáo này**, sửa root README.
`mobile/build/`, `mobile/.gradle/`, APK và `local.properties` được ignore.
Không có commit; không đổi Spring Boot/SQL/Spark. Dừng trước Payment Gateway.
