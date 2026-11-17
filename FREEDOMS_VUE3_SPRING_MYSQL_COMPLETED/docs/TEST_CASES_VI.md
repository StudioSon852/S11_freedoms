# Kịch bản kiểm thử FREEDOMS Spring MVC + Thymeleaf

## A. Khởi động

- MySQL đã chạy và `freedoms_shop` có đủ 18 bảng.
- `backend/.env` khai báo đúng host/database/user/password.
- Backend khởi động trên `http://localhost:8080` không có lỗi Hibernate schema validation.
- Đăng nhập được bằng admin và employee được seed lần đầu.

## B. Tài khoản và phân quyền

1. Đăng ký customer mới tại `/register`; xác nhận password trong DB là BCrypt hash, không phải mật khẩu gốc.
2. Đăng nhập sai mật khẩu: không vào được trang quản trị.
3. Đăng nhập customer: vào trang sản phẩm/giỏ/checkout/đơn hàng; truy cập `/admin` bị từ chối.
4. Admin vào `/admin/staff`, tạo employee; employee đăng nhập và vào POS.
5. Employee không vào được `/admin/staff` hoặc `/admin/products`.

## C. Website online

1. Thêm variant còn hàng vào giỏ khi chưa đăng nhập.
2. Bấm checkout; hệ thống yêu cầu đăng nhập.
3. Sau khi đăng nhập, checkout có thông tin khách; chọn BANK_TRANSFER và đặt hàng.
4. Kiểm tra `orders`, `order_items`, `payments`, `order_status_history` có dữ liệu; `product_variants.stock_quantity` giảm đúng số lượng.
5. Mở trang thanh toán; QR phải hiện khi đã cấu hình `BANK_ACCOUNT`. Nội dung QR là mã đơn, số tiền khớp `orders.total_amount`.
6. Bấm “Tôi đã chuyển khoản”: payment thành `WAITING_CONFIRMATION`, chưa thành `PAID`.
7. Employee vào `/employee/payments` xác nhận sau khi kiểm tra tài khoản; payment thành `PAID`, order thành `CONFIRMED`. Thử xác nhận hai lần: chi tiêu thành viên chỉ được cộng một lần.
8. Khi payment ở `WAITING_CONFIRMATION`, khách không được tự hủy đơn cho tới khi nhân viên đối soát; thông báo trên màn hình yêu cầu liên hệ cửa hàng.
9. Đơn COD chỉ thành `PAID` khi staff chuyển trạng thái giao hàng sang `COMPLETED`.
10. Admin chuyển lần lượt `PROCESSING`, `SHIPPING`, `COMPLETED`. Không có trạng thái giao hàng lặp.
11. Thử đặt số lượng vượt tồn: hệ thống báo lỗi, transaction không tạo đơn và không trừ kho.

## D. POS / hóa đơn chờ

1. Employee thêm nhiều biến thể vào giỏ POS.
2. Lưu hóa đơn chờ; kiểm tra có `held_invoices` và `held_invoice_items`.
3. So sánh tồn kho trước/sau khi giữ: không thay đổi. Kiểm tra chưa có order/payment tương ứng.
4. Mở lại hóa đơn; chốt CASH: tạo đơn POS, payment `PAID`, order `COMPLETED`, tồn kho giảm đúng số lượng, hóa đơn chờ thành `CONVERTED`.
5. Tạo hóa đơn mới và chốt BANK_TRANSFER: payment `PENDING`, order `PENDING_PAYMENT`; sau xác nhận nhận tiền, payment `PAID`, order `COMPLETED`.
6. Tạo hóa đơn chờ rồi hủy: trạng thái `CANCELLED`; tồn kho không đổi; không phát sinh order/payment.
7. Một employee khác không được mở hóa đơn do người trước tạo.
8. Cố chốt hóa đơn khi tồn kho đã bị bán hết ở đơn khác: chốt thất bại, hóa đơn vẫn `OPEN`, transaction không trừ dở dang.
9. Nhấn chốt cùng một hóa đơn hai lần ở hai phiên làm việc: chỉ được tạo tối đa một đơn hàng.

## E. Ưu đãi

1. Mã `FREEDOMS10` là coupon BOTH seed sẵn; áp dụng với đơn đạt ngưỡng tối thiểu.
2. Đặt hàng online bằng coupon: backend tính giảm giá và tăng lượt sử dụng.
3. POS sử dụng coupon OFFLINE/BOTH; coupon ONLINE-only phải bị từ chối.
4. Đăng nhập customer có tier discount: áp dụng discount tier; khách lẻ POS không được nhận.
5. Bấm xác nhận thanh toán hai lần: không cộng chi tiêu thành viên hai lần.
