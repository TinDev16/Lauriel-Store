# LAURIEL Mobile — Phase 8

Ứng dụng Android **Java + XML** cho CUSTOMER, nằm riêng trong `mobile/`. Spring Boot Web Admin và REST API giữ nguyên.

## Yêu cầu

- Android Studio tương thích Android Gradle Plugin **8.13.2**.
- JDK **17** cho Gradle, Gradle Wrapper **8.13**.
- Android SDK Platform **36** và Build Tools từ **35** trở lên; minSdk **26**.
- Backend Spring Boot Java 21 đang chạy và truy cập được qua mạng.
- Tài khoản CUSTOMER do người dùng tự đăng ký bằng app hoặc POST `/api/auth/register`. Không có tài khoản/password production mặc định.

AGP 8.13 hỗ trợ API 36.1 và yêu cầu Gradle 8.13/JDK 17 theo tài liệu Android:
https://developer.android.com/build/releases/agp-8-13-0-release-notes

## Mở và build

Mở **thư mục `mobile/`** trong Android Studio, không mở `src/main/java` của backend như Android project.
Trên Windows:

```powershell
cd mobile
$env:ANDROID_HOME = 'C:\Users\Lenovo\AppData\Local\Android\Sdk'
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

APK debug: `mobile/app/build/outputs/apk/debug/app-debug.apk`.
Sau khi cập nhật mã Android, nhấn Run trong Android Studio để cài bản mới lên emulator. Lệnh assembleDebug chỉ tạo APK, không cập nhật ứng dụng đang cài; nếu cần có thể dùng adb install -r mobile/app/build/outputs/apk/debug/app-debug.apk.
Unit test JUnit/MockWebServer/Robolectric dùng fixture riêng, không gọi MySQL thật.

## Backend URL

Mặc định debug dùng **`http://10.0.2.2:8080/`**: trong Android Emulator, `10.0.2.2` trỏ về host chạy Spring Boot; **`localhost` trong emulator là chính emulator**.
URL được tập trung ở `app/build.gradle`, truyền URL khác bằng:

```powershell
.\gradlew.bat assembleDebug -PapiBaseUrl=http://192.168.x.x:8080/
```

Thiết bị thật dùng IP LAN của máy backend, không dùng `10.0.2.2`; đảm bảo backend lắng nghe trên interface LAN và firewall cho phép. Không commit IP cá nhân.
Debug network-security config chỉ cho HTTP tới `10.0.2.2`. Nếu thử **thiết bị thật qua HTTP**, thêm đúng IP LAN vào `app/src/debug/res/xml/debug_network_security_config.xml` trên máy phát triển. Release chỉ dùng HTTPS và URL riêng của môi trường triển khai; bản release mẫu `example.invalid` cần thay bằng HTTPS thật trước phát hành.
Không đặt JWT signing secret trong Android. App chỉ lưu access token trả về từ backend.

## Luồng demo

1. Chạy Spring Boot với schema MySQL hợp lệ và `JWT_SECRET` phía backend.
2. Cài APK debug trên emulator; đảm bảo backend ở port 8080.
3. Đăng ký CUSTOMER bằng username/email (email optional)/password/fullName/phone (phone optional), rồi đăng nhập.
4. Home lấy Promotion active và Product từ API, Shop search/filter Size/Color/phân trang.
5. Detail chọn **combination thực** của Variant, xem `effectivePrice`, thêm Cart.
6. Cart gắn `userId`, sửa quantity/xóa, checkout nhập `shippingAddress` và optional Voucher.
7. Voucher preview gọi `POST /api/vouchers/validate`. Cart/code đổi sẽ bắt áp dụng lại.
8. Checkout gửi duy nhất `items[{variantId,quantity}]`, `shippingAddress`, optional `voucherCode`; server tính lại giá/stock/discount.
9. Orders xem snapshot và hủy nếu trạng thái PENDING/CONFIRMED còn hợp lệ; server quyết định và hoàn kho/voucher.
10. Profile lấy `GET /api/auth/me`; logout xóa session local. User khác không nhìn thấy Cart của user trước.

## API đã dùng

- `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me`
- `GET /api/promotions/active`
- `GET /api/products`, `GET /api/products/{id}`, `GET /api/sizes`, `GET /api/colors`
- `POST /api/vouchers/validate`
- `POST /api/orders`, `GET /api/orders/my-orders`, `GET /api/orders/{id}`, `POST /api/orders/{id}/cancel`

Không tạo endpoint/backend mới. Product summary chỉ có base price; effective price chỉ từ Variant trong Product detail.
Profile API hiện trả `userId`, `username`, `role`, chưa có profile đầy đủ.
Cart và tiền tạm tính là hiển thị local; giá cuối cùng và tồn kho do backend quyết định lúc checkout.

## Ghi chú

JWT lưu trong SharedPreferences gắn với cài đặt app; password không lưu, Android backup bị tắt.
API 401 của request có Bearer sẽ xóa session và điều hướng về Login; 403/409 giữ message phù hợp.
Ảnh sản phẩm do web quản trị tải lên dùng đường dẫn /uploads/products/...; ứng dụng ghép đường dẫn này với địa chỉ backend (10.0.2.2 trên emulator) để Glide tải ảnh ở danh sách, chi tiết và giỏ hàng. URL ảnh cũ dạng đầy đủ vẫn hiển thị; nếu ảnh trống/lỗi, ứng dụng dùng ảnh mặc định.
Không có dữ liệu giả trong đường chạy chính.
