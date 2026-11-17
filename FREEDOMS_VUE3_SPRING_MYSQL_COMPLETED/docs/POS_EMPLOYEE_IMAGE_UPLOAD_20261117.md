# POS nhân viên và tải ảnh – FREEDOMS

## POS nhân viên
`frontend/src/views/PosView.vue` dùng route hiện tại để phân biệt nhân viên và Admin.
- `/employee/pos`: lưới thẻ sản phẩm, ảnh, chọn biến thể size/màu, hiển thị giá/khuyến mại/tồn kho, nút thêm vào hóa đơn.
- `/admin/pos`: giữ danh sách sản phẩm POS cũ.
- Khung khách hàng, các dòng hàng, coupon Offline, tiền hàng, phương thức thanh toán, QR/chuyển khoản, checkout và hóa đơn chờ không bị thay đổi.

Danh sách POS được lấy từ `GET /api/pos/products`; mỗi biến thể trả về thuộc tính `images` do `CatalogApiService` ánh xạ từ `product_variant_images`. Card ưu tiên ảnh đầu tiên của biến thể đang chọn, sau đó dùng ảnh chính sản phẩm và ảnh mặc định `/img/picture1.png`.

## Chặn cửa hàng Online khỏi tài khoản nhân viên
Router có `denyRoles: ['EMPLOYEE']` trên các route cửa hàng/giỏ hàng/chi tiết sản phẩm/checkout/đơn mua/thanh toán. Dashboard nhân viên không còn hiển thị liên kết “Trang bán hàng”. POS và hồ sơ nhân viên không bị ảnh hưởng.

## Ảnh sản phẩm
1. Chọn ảnh ở mục Tải ảnh đại diện. Form hiển thị preview trước khi lưu.
2. Cho phép JPG/JPEG, PNG, WEBP, GIF, BMP; dung lượng tối đa 8 MB mỗi ảnh.
3. Backend đọc chữ ký file để nhận dạng ảnh. Điều này xử lý trường hợp Windows Snipping Tool gửi MIME `application/octet-stream`.
4. File được lưu theo tên UUID trong `UPLOAD_DIR` (mặc định `uploads/products`) và URL công khai `/uploads/products/<uuid>.<ext>`.
5. `UploadResourceConfig` tạo thư mục trước khi đăng ký resource handler. Vite proxy `/uploads` về `http://127.0.0.1:8080` để ảnh không bị 404 trong môi trường dev.
6. Không cần chạy migration mới cho thay đổi này; bảng `product_variant_images` vẫn cần migration `04_VARIANT_IMAGES_CATALOG.sql` đã được cung cấp ở bản trước.

## Ba tài khoản đồng thời
Backend dùng session cookie `JSESSIONID`, được chia sẻ bởi các tab cùng browser profile. Dùng ba profile riêng hoặc ba trình duyệt riêng nếu cần đăng nhập Admin/Manager/Employee đồng thời.
