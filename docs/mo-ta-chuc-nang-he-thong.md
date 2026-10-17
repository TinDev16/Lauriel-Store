# Mô tả chức năng hệ thống Lauriel Store

Tài liệu này được đối chiếu với source code hiện tại của Lauriel Store, gồm Web Admin, Customer Web, REST API và ứng dụng Android. Tên **thuộc tính sản phẩm** trong giao diện tương ứng với thực thể kỹ thuật `ProductVariant` trong source code.

## Phạm vi quyền truy cập

| Vai trò | Phạm vi chính |
|---|---|
| `SUPER_ADMIN` | Toàn bộ chức năng; riêng quản lý tài khoản và phân quyền chỉ dành cho vai trò này. |
| `ADMIN` | Toàn bộ nghiệp vụ quản trị, trừ quản lý tài khoản hệ thống. |
| `STAFF` | Sản phẩm, loại hàng, kích cỡ, màu sắc, số lượng hàng, phiếu nhập, đơn hàng, khách hàng, nhà cung cấp và thống kê. |
| `CUSTOMER` | Hồ sơ cá nhân, giỏ hàng, đặt hàng, voucher và lịch sử đơn của chính mình. |
| Khách chưa đăng nhập | Xem danh mục, sản phẩm và sử dụng giỏ hàng Web theo session; phải đăng nhập khi thanh toán. |

## 1. Quản lý tài khoản và vai trò

| Chức năng con | Xử lý | Input | Output |
|---|---|---|---|
| Đăng ký tài khoản khách hàng | Chuẩn hóa tên đăng nhập/email; kiểm tra trùng; mã hóa mật khẩu; tạo đồng thời `User` có vai trò `CUSTOMER` và hồ sơ `Customer`; mặc định trạng thái hoạt động. | `username`, `email` tùy chọn, `password` 8–72 ký tự, `fullName`, `phone` tùy chọn. | Mã người dùng, tên đăng nhập, email, vai trò `CUSTOMER` và thông báo đăng ký thành công. |
| Đăng nhập | Xác thực tên đăng nhập và mật khẩu bằng Spring Security; kiểm tra trạng thái tài khoản; phát hành JWT cho API hoặc tạo phiên đăng nhập cho Web. | `username`, `password`. | JWT, loại token, thời hạn, `userId`, tên đăng nhập và vai trò; Web chuyển đến trang phù hợp với vai trò. |
| Xem tài khoản hiện tại | Đọc thông tin từ principal đã xác thực. | JWT hoặc session đăng nhập. | `userId`, `username`, `role`. |
| Tra cứu danh sách tài khoản | Phân trang, tìm kiếm và lọc; sắp tài khoản theo trạng thái rồi theo thời gian tạo mới nhất. | `page`, `size`, `keyword`, `role`, `status`. | Danh sách gồm mã tài khoản, tên đăng nhập, họ tên, email, điện thoại, vai trò, trạng thái và ngày tạo. |
| Xem chi tiết tài khoản | Tải thông tin tài khoản; nếu là khách hàng thì lấy thêm hồ sơ khách hàng đang liên kết. | `userId`. | Chi tiết tài khoản và `customerProfile` nếu có. |
| Tạo tài khoản quản lý | Chỉ `SUPER_ADMIN`; kiểm tra mật khẩu xác nhận, trùng username/email; chỉ cho tạo `ADMIN` hoặc `STAFF`; mã hóa mật khẩu trước khi lưu. | `username`, `password`, `confirmPassword`, `fullName`, `email`, `phone`, `role`, `status`. | Chi tiết tài khoản vừa tạo và thông báo thành công. |
| Cập nhật tài khoản | Chỉ `SUPER_ADMIN`; cập nhật họ tên, email, điện thoại, vai trò và trạng thái; kiểm tra email trùng và các quy tắc chuyển vai trò. | `userId`, `fullName`, `email`, `phone`, `role`, `status`. | Chi tiết tài khoản sau cập nhật. |
| Đổi vai trò | Ngăn đổi `CUSTOMER` sang vai trò quản lý vì đang liên kết hồ sơ khách hàng; không cho cấp `SUPER_ADMIN` từ giao diện; bảo vệ `SUPER_ADMIN` hoạt động cuối cùng. | `userId`, vai trò mới. | Vai trò được cập nhật hoặc thông báo lỗi nghiệp vụ. |
| Khóa/mở tài khoản | Thay đổi `ACTIVE/INACTIVE`; không cho khóa hoặc hạ quyền `SUPER_ADMIN` hoạt động cuối cùng. | `userId`, trạng thái mới. | Trạng thái mới hoặc thông báo xung đột. |
| Đặt lại mật khẩu | Chỉ `SUPER_ADMIN`; chỉ áp dụng cho `ADMIN/STAFF`; kiểm tra hai mật khẩu khớp và độ dài, sau đó mã hóa lại. | `userId`, `password`, `confirmPassword`. | Thông báo đặt lại mật khẩu thành công hoặc lỗi hợp lệ. |

## 2. Quản lý loại hàng

| Chức năng con | Xử lý | Input | Output |
|---|---|---|---|
| Xem danh sách loại hàng | Lấy toàn bộ loại hàng cho quản trị, kèm số sản phẩm thuộc từng loại; loại ngừng hoạt động được sắp phía dưới. | Không có hoặc yêu cầu mở trang danh sách. | `categoryId`, tên loại hàng, trạng thái, ngày tạo và số lượng sản phẩm. |
| Xem loại hàng đang hoạt động | Chỉ lấy loại hàng `ACTIVE`, sắp theo tên; dùng cho menu và bộ lọc phía khách hàng. | Không có. | Danh sách `categoryId`, `categoryName`. |
| Thêm loại hàng | Chuẩn hóa khoảng trắng trong tên; kiểm tra tên không trùng không phân biệt hoa thường; lưu trạng thái. | `categoryName` tối đa 100 ký tự, `status`. | Loại hàng vừa tạo, số sản phẩm ban đầu bằng 0. |
| Sửa loại hàng | Kiểm tra tồn tại và tên không trùng với loại khác; cập nhật tên và trạng thái. | `categoryId`, `categoryName`, `status`. | Loại hàng sau cập nhật và số sản phẩm hiện có. |
| Thay đổi trạng thái | Bật hoặc ngừng hoạt động loại hàng. Loại ngừng hoạt động không thể được gán cho sản phẩm mới. | `categoryId`, `ACTIVE/INACTIVE`. | Trạng thái loại hàng mới. |

## 3. Quản lý sản phẩm

| Chức năng con | Xử lý | Input | Output |
|---|---|---|---|
| Tra cứu danh sách sản phẩm quản trị | Phân trang; tìm theo tên; lọc loại hàng và trạng thái; tính số thuộc tính và tổng số lượng còn lại của từng sản phẩm. | `page`, `size`, `keyword`, `categoryId`, `status`. | Danh sách sản phẩm gồm hình, tên, loại, giá cơ bản, trạng thái, số thuộc tính và tổng tồn hàng. |
| Xem danh mục sản phẩm bán | Chỉ lấy sản phẩm và loại hàng hoạt động; hỗ trợ tìm kiếm, lọc loại/kích cỡ/màu và sắp xếp; tính giá hiệu lực sau khuyến mãi. | `page`, `size`, `keyword`, `categoryId`, `sizeId`, `colorId`, trường sắp xếp và chiều `asc/desc`. | Trang sản phẩm gồm giá gốc, giá hiệu lực, ảnh, loại hàng và thông tin khuyến mãi. |
| Xem chi tiết sản phẩm | Tải sản phẩm, thư viện ảnh, các thuộc tính đang bán và giá sau khuyến mãi; chỉ trả sản phẩm hoạt động thuộc loại hàng hoạt động. | `productId`. | Tên, mô tả, loại hàng, giá cơ bản, ảnh chính, trạng thái, danh sách thuộc tính và thư viện ảnh. |
| Thêm sản phẩm | Kiểm tra loại hàng đang hoạt động; lưu tên, mô tả, giá cơ bản và trạng thái; sau đó lưu các tệp ảnh đã chọn. | `productName`, `categoryId`, `description`, `price >= 0`, `status`, các tệp ảnh và màu gắn với từng ảnh nếu có. | Sản phẩm mới và thông báo thành công. |
| Sửa sản phẩm | Kiểm tra sản phẩm tồn tại; cập nhật thông tin; giữ ảnh chính hiện tại nếu không tải ảnh mới; chỉ cho chuyển sang loại hàng hợp lệ. | `productId` và các trường như khi thêm. | Sản phẩm sau cập nhật. |
| Thay đổi trạng thái sản phẩm | Bật/ngừng bán sản phẩm mà không xóa dữ liệu. | `productId`, `ACTIVE/INACTIVE`. | Trạng thái mới. |
| Quản lý hình ảnh | Kiểm tra nội dung thật của tệp; chỉ nhận PNG, JPEG hoặc WebP, tối đa 5 MB/ảnh và tối đa 8 ảnh/sản phẩm; cho gắn ảnh theo màu, đặt ảnh đại diện và xóa ảnh. | `productId`, một/nhiều tệp ảnh, `colorId` tùy chọn, `imageId` khi đặt ảnh chính hoặc xóa. | Danh sách ảnh gồm URL, cờ ảnh chính, thứ tự hiển thị và màu liên kết; giao diện Web/Mobile dùng URL mới. |
| Xem thuộc tính sản phẩm | Phân trang, tìm theo SKU/kích cỡ/màu; lọc trạng thái và màu; gom nhóm theo màu. | `productId`, `page`, `size`, `keyword`, `status`, `colorId`. | Danh sách thuộc tính gồm SKU, kích cỡ, màu, giá, giá sau giảm, số lượng và trạng thái. |
| Thêm nhanh thuộc tính | Tạo tích Descartes giữa nhiều kích cỡ và nhiều màu; bỏ qua tổ hợp đã tồn tại; tự sinh SKU dạng `SP-{product}-S{size}-C{color}`; số lượng ban đầu bằng 0. | `productId`, danh sách `sizeIds`, danh sách `colorIds`, giá, trạng thái. Giá form mặc định lấy từ giá cơ bản sản phẩm. | Số tổ hợp đã tạo, số tổ hợp bị bỏ qua và danh sách thuộc tính mới trong hệ thống. |
| Sửa thuộc tính | Kiểm tra tổ hợp kích cỡ–màu không trùng trong cùng sản phẩm; cập nhật kích cỡ, màu, giá và trạng thái; SKU đã sinh được giữ ổn định. | `productId`, `variantId`, `sizeId`, `colorId`, `price`, `status`. | Thuộc tính sau cập nhật; số lượng chỉ thay đổi qua nghiệp vụ nhập/bán/hoàn hàng. |
| Quản lý kích cỡ và màu sắc | Xem, thêm và sửa danh mục kích cỡ/màu để dùng khi tạo thuộc tính sản phẩm. | Tên kích cỡ hoặc tên màu. | Mã và tên kích cỡ/màu. |
| Gợi ý sản phẩm liên quan | Lấy tối đa 1–10 sản phẩm hoạt động cùng loại, loại trừ sản phẩm hiện tại và áp dụng giá khuyến mãi. | `productId`, `limit`. | Danh sách sản phẩm liên quan. |

## 4. Quản lý giỏ hàng

| Chức năng con | Xử lý | Input | Output |
|---|---|---|---|
| Xem giỏ hàng | Tải lại thông tin sản phẩm/thuộc tính và giá hiện hành; bỏ qua sản phẩm đã ngừng hoạt động; tính thành tiền từng dòng, tổng số lượng và tạm tính. | Session Web hoặc giỏ cục bộ của người dùng Android. | Danh sách sản phẩm, ảnh, SKU, kích cỡ, màu, giá gốc/giá hiệu lực, số lượng, tồn hiện có, thành tiền và tổng tạm tính. |
| Thêm vào giỏ | Kiểm tra sản phẩm và thuộc tính đang hoạt động; cộng dồn nếu cùng `variantId`; từ chối số lượng không dương hoặc vượt số lượng hiện có. | `variantId`, `quantity`, `productId` để quay lại trang chi tiết trên Web. | Giỏ hàng đã cập nhật và thông báo thành công/lỗi. |
| Mua ngay | Kiểm tra khả dụng và tạo vùng dữ liệu mua ngay tách khỏi giỏ chính. | `variantId`, `quantity`. | Chuyển đến trang thanh toán ở chế độ `buy-now`. |
| Cập nhật số lượng | Kiểm tra dòng hàng tồn tại và số lượng không vượt tồn hiện tại; Android coi số lượng 0 là xóa. | `variantId`, số lượng mới. | Giỏ hàng và tổng tiền mới. |
| Xóa sản phẩm | Xóa dòng theo `variantId`; đồng thời bỏ voucher đang chọn trên Web. | `variantId`. | Giỏ hàng sau khi xóa. |
| Xóa toàn bộ | Xóa toàn bộ dữ liệu giỏ và voucher đã chọn. | Yêu cầu xóa giỏ. | Giỏ hàng rỗng. |
| Lưu giỏ Android | Lưu riêng theo `userId` bằng `SharedPreferences`; lưu ảnh, tên, SKU, kích cỡ, màu, giá hiển thị, số lượng và tồn đã biết; cập nhật ảnh khi sản phẩm đổi ảnh. | Tài khoản hiện tại và `CartItem`. | Giỏ vẫn còn sau khi đóng/mở ứng dụng; tổng tạm tính cục bộ. |
| Đồng bộ giỏ lên máy chủ | Gộp các dòng trùng `variantId`, kiểm tra số lượng 1–999 và thuộc tính tồn tại; thay snapshot giỏ của tài khoản, đồng thời cập nhật thời điểm hoạt động. | Danh sách `{variantId, quantity}` và người dùng từ JWT. | HTTP `204 No Content`; dữ liệu dùng cho thống kê khách có giỏ và sản phẩm được quan tâm. |

## 5. Quản lý đơn đặt hàng

| Chức năng con | Xử lý | Input | Output |
|---|---|---|---|
| Xem trước đơn hàng | Chuẩn hóa các dòng trùng; lấy giá hiện hành; tính giảm giá sản phẩm, khuyến mãi theo tổng đơn, voucher và phí giao hàng mà chưa ghi đơn. | Danh sách `{variantId, quantity}`, `voucherCode` tùy chọn. | Tạm tính, giảm theo sản phẩm, giảm theo tổng đơn, giảm voucher, phí giao hàng và số tiền cuối cùng. |
| Đặt hàng | Xác định khách hàng; khóa các dòng thuộc tính để chống bán vượt tồn; kiểm tra trạng thái/tồn; chụp giá tại thời điểm mua; áp dụng ưu đãi; tạo đơn `PENDING` và chi tiết; trừ số lượng; ghi lịch sử `SALE`; phát sự kiện gửi thông báo Telegram. | Tối đa 100 dòng `{variantId, quantity}`, địa chỉ giao hàng, mã voucher tùy chọn, tài khoản khách hàng. | `orderId`, trạng thái, các khoản tiền, ưu đãi đã dùng, địa chỉ, thời gian và danh sách chi tiết đơn. |
| Xem đơn của khách hàng | Chỉ truy cập đơn thuộc đúng tài khoản; phân trang và lọc theo trạng thái. | `page`, `size`, `status` tùy chọn và người dùng đăng nhập. | Danh sách tóm tắt đơn theo thời gian mới nhất. |
| Xem chi tiết đơn | Kiểm tra quyền sở hữu đối với khách hàng; quản trị có thể xem mọi đơn. | `orderId`, người dùng hiện tại. | Toàn bộ thông tin đơn và từng mặt hàng: SKU, sản phẩm, kích cỡ, màu, ảnh, giá, giảm giá, số lượng, thành tiền. |
| Tìm kiếm/lọc đơn quản trị | Tìm theo mã đơn, tên hoặc số điện thoại khách; lọc trạng thái và khoảng thời gian; phân trang. | `page`, `size`, `keyword`, `status`, `from`, `to`. | Danh sách đơn và tên khách hàng tương ứng. |
| Cập nhật trạng thái | Chỉ cho đi đúng chuỗi `PENDING → CONFIRMED → PROCESSING → SHIPPING → DELIVERED`; nhân viên có thể hủy ở `PENDING/CONFIRMED/PROCESSING`. | `orderId`, trạng thái đích, tài khoản nhân viên thực hiện. | Đơn hàng với trạng thái mới hoặc lỗi chuyển trạng thái không hợp lệ. |
| Khách hàng hủy đơn | Chỉ cho hủy đơn của mình ở `PENDING` hoặc `CONFIRMED`; hoàn lại số lượng dựa trên lịch sử `SALE`, ghi `RETURN` và hoàn số lượng voucher. | `orderId`, tài khoản khách hàng. | Đơn ở trạng thái `CANCELLED`, số lượng và voucher được phục hồi. |

## 6. Quản lý hóa đơn

Source hiện tại **chưa có module hóa đơn độc lập**: không có thực thể `Invoice`, controller/service hóa đơn, số hóa đơn, xuất PDF hay chức năng in hóa đơn. Dữ liệu có thể dùng để hình thành hóa đơn hiện nằm trong `Order`, `OrderDetail` và `Payment`.

| Chức năng con hiện có | Xử lý | Input | Output |
|---|---|---|---|
| Xem chứng từ bán hàng từ đơn | Dùng trang chi tiết đơn làm nội dung chứng từ: thông tin khách, địa chỉ, mặt hàng và các khoản tiền đã chụp tại lúc đặt. | `orderId`. | Chi tiết đơn gồm tổng hàng, giảm sản phẩm, giảm theo tổng đơn, voucher, phí giao hàng và phải trả. |
| Xác định đơn hoàn tất | Đơn được xem là bán thành công khi trạng thái chuyển đến `DELIVERED`; báo cáo doanh thu chỉ cộng đơn đã giao. | Trạng thái đơn hàng. | Trạng thái `DELIVERED` và doanh thu được đưa vào thống kê. |
| Dữ liệu thanh toán nền | Source có bảng `payments` với phương thức `COD/BANKING/MOMO/VNPAY`, trạng thái `PENDING/PAID/FAILED/REFUNDED`, số tiền, mã giao dịch và thời gian thanh toán. | Dữ liệu `Payment` ở tầng database. | Bản ghi thanh toán nếu được tạo trực tiếp từ nghiệp vụ tương lai. Hiện chưa có controller/service thao tác bảng này. |
| Phát hành/in/xuất hóa đơn | Chưa được triển khai trong code hiện tại. | Chưa có. | Chưa có số hóa đơn, file PDF hoặc bản in. |

## 7. Quản lý nhập hàng

| Chức năng con | Xử lý | Input | Output |
|---|---|---|---|
| Tra cứu phiếu nhập | Tìm theo mã phiếu dạng `PN-{id}`, nhà cung cấp, username hoặc họ tên người tạo; lọc trạng thái và thời gian; trả cả các dòng hàng để hiển thị rõ nội dung phiếu. | `page`, `size`, `keyword`, `status`, `from`, `to`. | Danh sách phiếu gồm nhà cung cấp, người tạo, trạng thái, thời gian, tổng tiền và các mặt hàng. |
| Tạo phiếu nhập nháp | Kiểm tra nhà cung cấp và người tạo tồn tại; tạo phiếu `DRAFT`, tổng tiền ban đầu bằng 0. | `supplierId`, tài khoản nhân viên. | Phiếu nhập mới và chuyển đến trang chi tiết. |
| Chọn sản phẩm và thuộc tính | Tải danh sách sản phẩm; sau khi chọn sản phẩm mới cho chọn kích cỡ và màu tương ứng để xác định `variantId`. | Từ khóa sản phẩm, sản phẩm, kích cỡ, màu. | Thuộc tính hợp lệ gồm SKU, sản phẩm, kích cỡ, màu, số lượng hiện tại và giá tham chiếu. |
| Thêm mặt hàng | Chỉ sửa được phiếu `DRAFT`; ngăn thêm trùng thuộc tính; tính `subtotal = quantity × unitPrice` và cộng lại tổng phiếu. | `receiptId`, `variantId`, `quantity > 0`, `unitPrice >= 0`; giao diện điền mặc định theo giá cơ bản sản phẩm và cho phép sửa. | Chi tiết phiếu sau khi thêm dòng và tổng tiền mới. |
| Sửa mặt hàng | Khóa phiếu nháp; tính lại thành tiền dòng và tổng phiếu, kiểm tra giới hạn số. | `receiptId`, `variantId`, số lượng và đơn giá mới. | Phiếu nhập đã cập nhật. |
| Xóa mặt hàng | Chỉ áp dụng phiếu nháp; xóa dòng và trừ thành tiền dòng khỏi tổng phiếu. | `receiptId`, `variantId`. | Phiếu nhập và tổng tiền sau xóa. |
| Hoàn tất phiếu nhập | Khóa phiếu; yêu cầu có ít nhất một mặt hàng; tính lại tổng từ dữ liệu server; khóa từng thuộc tính, cộng số lượng, ghi giao dịch `IMPORT`, đổi trạng thái thành `COMPLETED`. Toàn bộ chạy trong một transaction. | `receiptId`, tài khoản nhân viên thực hiện. | Phiếu hoàn tất, tổng tiền chính xác, số lượng sản phẩm tăng và lịch sử nhập hàng. |
| Hủy phiếu nhập | Chỉ phiếu `DRAFT` được hủy; không làm thay đổi số lượng sản phẩm. | `receiptId`. | Phiếu có trạng thái `CANCELLED`. |
| Xem lịch sử điều chỉnh | Tìm theo SKU/tên sản phẩm/loại tham chiếu/ghi chú; lọc thuộc tính, loại giao dịch và khoảng thời gian. | `page`, `size`, `keyword`, `variantId`, `type`, `from`, `to`. | Mã giao dịch, sản phẩm, SKU, loại `IMPORT/SALE/RETURN`, số lượng, tham chiếu phiếu/đơn, người thực hiện và thời gian. |

## 8. Quản lý chương trình khuyến mãi và mã giảm giá

| Chức năng con | Xử lý | Input | Output |
|---|---|---|---|
| Tra cứu chương trình giảm giá sản phẩm | Phân trang, tìm theo tên và lọc trạng thái; xác định đang chờ, đang chạy hoặc đã hết hạn theo thời gian. | `page`, `size`, `keyword`, `status`. | Danh sách chương trình với phần trăm giảm, thời gian và trạng thái. |
| Thêm/sửa chương trình | Kiểm tra tên, phần trăm trong khoảng `(0,100]`, ngày bắt đầu trước ngày kết thúc và mô tả nằm trong giới hạn lưu trữ. | Tên, mô tả, phần trăm giảm, `startDate`, `endDate`, trạng thái. | Chương trình sau khi lưu. |
| Bật/ngừng chương trình | Đổi trạng thái hoạt động mà không xóa lịch sử liên kết. | `promotionId`, trạng thái. | Trạng thái mới. |
| Gán sản phẩm vào chương trình | Tìm/lọc sản phẩm theo từ khóa hoặc loại hàng; cho chọn một, nhiều hoặc tất cả sản phẩm; bỏ qua liên kết đã tồn tại. | `promotionId`, một/nhiều `productIds`, `categoryId` tùy chọn. | Số sản phẩm được thêm và danh sách sản phẩm áp dụng. |
| Giới hạn khuyến mãi theo thuộc tính | Chọn áp dụng toàn bộ thuộc tính của sản phẩm hoặc chỉ các tổ hợp kích cỡ–màu được chọn; kiểm tra thuộc tính thuộc đúng sản phẩm. | `promotionId`, `productId`, chế độ `ALL/SELECTED`, danh sách `variantIds`. | Phạm vi áp dụng đã lưu; giá app/web chỉ giảm trên thuộc tính hợp lệ. |
| Gỡ sản phẩm khỏi chương trình | Xóa liên kết sản phẩm và phạm vi thuộc tính liên quan. | `promotionId`, `productId`. | Danh sách sản phẩm áp dụng sau khi gỡ. |
| Tính giá khuyến mãi sản phẩm | Lấy các chương trình đang hoạt động và đúng thời gian; so sánh mức giảm, chọn mức có lợi nhất; nếu bằng nhau chọn mã chương trình nhỏ hơn. | Giá thuộc tính, sản phẩm/thuộc tính, thời điểm hiện tại. | Giá gốc, giá hiệu lực, số tiền giảm và thông tin chương trình được chọn. |
| Tra cứu mã giảm giá | Phân trang; tìm theo mã/tên; lọc trạng thái và loại giảm. | `page`, `size`, `keyword`, `status`, `discountType`. | Mã, tên, loại phần trăm/số tiền, giá trị giảm, đơn tối thiểu, giảm tối đa, số lượng, thời gian và trạng thái. |
| Thêm/sửa mã giảm giá | Chuẩn hóa mã; kiểm tra mã duy nhất, phần trăm không quá 100, thời gian hợp lệ và các giới hạn số tiền/số lượng. | `code`, `voucherName`, `discountType`, `discountValue`, `minOrderAmount`, `maxDiscountAmount`, `quantity`, thời gian, trạng thái. | Voucher sau khi lưu. |
| Xem danh sách voucher khả dụng | Chỉ lấy voucher hoạt động, còn số lượng và nằm trong thời gian hiệu lực để hiển thị trên Web/Mobile. | `page`, `size`, thời điểm hiện tại. | Danh sách voucher có thể dùng. |
| Kiểm tra và sử dụng voucher | Tính đơn sau giảm sản phẩm và giảm theo tổng đơn; kiểm tra mã, thời gian, trạng thái, số lượng và giá trị đơn tối thiểu; giới hạn mức giảm tối đa; khi đặt hàng thì trừ số lượng voucher. | `voucherCode`, danh sách mặt hàng hoặc giá trị đơn sau khuyến mãi. | Kết quả hợp lệ, số tiền giảm và số tiền cuối cùng; lỗi cụ thể nếu không dùng được. |
| Quản lý khuyến mãi theo tổng đơn | Thêm/sửa/lọc chương trình theo ngưỡng đơn; chọn chương trình đủ điều kiện có mức giảm tốt nhất và áp dụng trước voucher. | Tên, mô tả, đơn tối thiểu, phần trăm, giảm tối đa, thời gian, trạng thái. | Chương trình được áp dụng, số tiền giảm và số tiền còn lại. |

## 9. Quản lý nhân viên

Nhân viên không có bảng riêng; đây là các tài khoản trong `users` mang vai trò `ADMIN` hoặc `STAFF`.

| Chức năng con | Xử lý | Input | Output |
|---|---|---|---|
| Xem danh sách nhân viên | Dùng danh sách tài khoản và lọc theo vai trò `ADMIN/STAFF`, từ khóa và trạng thái. | `page`, `size`, `keyword`, `role`, `status`. | Thông tin tài khoản nhân viên và ngày tạo. |
| Thêm nhân viên | `SUPER_ADMIN` tạo tài khoản quản lý, kiểm tra trùng username/email và mã hóa mật khẩu. | Username, mật khẩu/xác nhận, họ tên, email, điện thoại, vai trò `ADMIN/STAFF`, trạng thái. | Tài khoản nhân viên mới. |
| Sửa nhân viên | Cập nhật thông tin liên hệ, vai trò giữa `ADMIN` và `STAFF`, trạng thái. | `userId` và thông tin cập nhật. | Hồ sơ tài khoản mới. |
| Khóa/mở nhân viên | Chuyển trạng thái tài khoản; tài khoản bị khóa không thể tiếp tục xác thực sử dụng hệ thống. | `userId`, trạng thái. | Trạng thái mới. |
| Đặt lại mật khẩu nhân viên | Xác nhận mật khẩu mới, mã hóa và lưu lại; chỉ `SUPER_ADMIN` thực hiện. | `userId`, mật khẩu mới và xác nhận. | Thông báo thành công/lỗi. |

## 10. Quản lý khách hàng

| Chức năng con | Xử lý | Input | Output |
|---|---|---|---|
| Tra cứu khách hàng | Phân trang; tìm theo tên khách, điện thoại, username hoặc email; lọc trạng thái tài khoản; mới nhất hiển thị trước. | `page`, `size`, `keyword`, `status`. | `customerId`, username, họ tên, email, điện thoại, địa chỉ, trạng thái và ngày tạo. |
| Xem hồ sơ cá nhân | Tìm hồ sơ khách hàng theo `userId` của tài khoản đăng nhập. | JWT/session khách hàng. | `customerId`, `userId`, username, họ tên, email, điện thoại, địa chỉ. |
| Cập nhật hồ sơ cá nhân | Chuẩn hóa dữ liệu; kiểm tra email không thuộc tài khoản khác; đồng bộ họ tên, email, điện thoại giữa `User` và `Customer`; cập nhật địa chỉ. | `fullName`, `email`, `phone`, `address`. | Hồ sơ khách hàng sau cập nhật. |
| Theo dõi khách đang hoạt động | App gửi heartbeat; máy chủ cập nhật `last_seen_at`; báo cáo xem là online nếu có hoạt động trong 5 phút gần nhất. | Tài khoản từ JWT. | HTTP `204`; dữ liệu online trong báo cáo thống kê. |
| Quản lý trạng thái khách | Thực hiện qua module tài khoản bởi `SUPER_ADMIN`; không cho chuyển tài khoản `CUSTOMER` sang vai trò quản lý do ràng buộc hồ sơ. | `userId`, trạng thái. | Trạng thái tài khoản khách mới. |

## 11. Quản lý nhà cung cấp

| Chức năng con | Xử lý | Input | Output |
|---|---|---|---|
| Tra cứu nhà cung cấp | Phân trang; tìm theo tên, điện thoại, email hoặc địa chỉ; lọc trạng thái; nhà cung cấp ngừng hoạt động xếp dưới. | `page`, `size`, `keyword`, `status`. | Mã, tên, điện thoại, email, địa chỉ, trạng thái và ngày tạo. |
| Xem chi tiết | Kiểm tra nhà cung cấp tồn tại và ánh xạ đầy đủ thông tin. | `supplierId`. | `SupplierResponse`. |
| Thêm nhà cung cấp | Kiểm tra tên bắt buộc, email đúng định dạng và độ dài các trường; lưu thông tin liên hệ và trạng thái. | `supplierName`, `phone`, `email`, `address`, `status`. | Nhà cung cấp vừa tạo. |
| Sửa/thay đổi trạng thái | Cập nhật thông tin hoặc bật/ngừng hoạt động; dữ liệu phiếu nhập cũ vẫn giữ liên kết. | `supplierId` và các trường cập nhật hoặc trạng thái mới. | Nhà cung cấp sau cập nhật. |

## 12. Báo cáo thống kê

| Chức năng con | Xử lý | Input | Output |
|---|---|---|---|
| Chọn khoảng thống kê | Hỗ trợ hôm nay, 7 ngày, 30 ngày, tháng, quý, năm hoặc khoảng tùy chọn; không cho ngày tương lai, tối đa 366 ngày; nhóm theo ngày/tháng/quý/năm. | `preset`, `fromDate`, `toDate`, `groupBy`, ngưỡng tồn và `top` 5/10. | Khoảng thời gian đã chuẩn hóa dùng thống nhất cho toàn báo cáo. |
| Tổng quan điều hành | Tổng hợp doanh thu mọi thời điểm/hôm nay/tháng/năm, đơn hôm nay/tháng, đơn đã giao/hủy, số khách, sản phẩm hoạt động, tổng số lượng hàng, cảnh báo thấp và phiếu nhập nháp. | Bộ lọc thời gian, ngưỡng tồn, số lượng top. | Các KPI và liên kết xem chi tiết nghiệp vụ. |
| Báo cáo doanh thu–đơn hàng | Chỉ tính doanh thu từ đơn `DELIVERED`; tính số đơn, số giao/hủy, doanh thu, giá trị đơn trung bình, tỷ lệ hủy, chuỗi doanh thu theo thời gian và cơ cấu trạng thái. | Khoảng thời gian và đơn vị nhóm. | KPI bán hàng, biểu đồ doanh thu/số đơn và tỷ trọng trạng thái. |
| Báo cáo sản phẩm | Tổng hợp sản phẩm và thuộc tính bán chạy theo số lượng/doanh thu; số lượng đã bán; thuộc tính tồn thấp/cao và giá trị số lượng hàng theo giá bán. | Khoảng thời gian, `threshold`, `top`. | Top sản phẩm, top thuộc tính, danh sách cảnh báo và chỉ số hàng hóa. |
| Báo cáo khách hàng | Tổng hợp khách có đơn, khách mới trong tháng và theo thời gian; xếp khách theo tổng chi tiêu hoặc số đơn. | Khoảng thời gian, cách nhóm, `top`. | Top khách hàng, lượng khách mới và chuỗi tăng trưởng khách hàng. |
| Báo cáo khuyến mãi | Cộng chi phí voucher, giảm giá sản phẩm và giảm theo tổng đơn; xếp voucher/chương trình theo lượt dùng hoặc tiền giảm. | Khoảng thời gian, `top`. | Tổng chi phí ưu đãi và các bảng xếp hạng voucher/chương trình. |
| Báo cáo nhập hàng | Tổng hợp số phiếu hoàn tất, tổng giá trị, số lượng nhập, xu hướng nhập theo thời gian và nhà cung cấp nổi bật. | Khoảng thời gian, cách nhóm, `top`. | KPI nhập hàng, biểu đồ xu hướng và top nhà cung cấp. |
| Thống kê hoạt động trực tuyến và giỏ hàng | Khách được tính online nếu heartbeat trong 5 phút; tổng hợp số khách có giỏ, số sản phẩm khác nhau và tổng số lượng; xếp sản phẩm theo số khách đã thêm vào giỏ. | Thời điểm hiện tại và `top`. | Số khách online, danh sách khách online, số khách có giỏ và top sản phẩm được quan tâm. |

## Ghi chú về phạm vi hiện tại

- Hệ thống dùng thay đổi trạng thái thay cho xóa cứng đối với tài khoản, loại hàng, sản phẩm, nhà cung cấp, voucher và chương trình khuyến mãi.
- Số lượng thuộc tính sản phẩm chỉ thay đổi qua phiếu nhập, bán hàng và hoàn khi hủy đơn; màn hình sửa sản phẩm/thuộc tính không sửa trực tiếp số lượng.
- Module hóa đơn và xử lý thanh toán chưa hoàn thiện dù đã có thực thể `Payment`; nếu đồ án yêu cầu “Quản lý hóa đơn” độc lập thì cần bổ sung số hóa đơn, ngày phát hành, trạng thái thanh toán, trang xem/in và xuất PDF.
