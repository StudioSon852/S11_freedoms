# FREEDOMS – Báo cáo Admin, khách hàng và hạng thành viên

## Thành phần đã cập nhật

- Admin Overview: KPI hiện tại, 8 đơn hàng gần đây và bộ lọc doanh thu theo từ ngày, đến ngày, tên/email/số điện thoại khách hàng và mã đơn. Báo cáo chỉ tính đơn `COMPLETED` có payment `PAID`, trừ tiền hoàn ở trạng thái đã xác nhận thực tế (`REFUNDED`). Tổng doanh thu và số đơn tính trên toàn bộ kết quả; bảng chỉ hiển thị tối đa 100 dòng để tránh giao diện quá nặng.
- Admin Orders & Payments: lọc theo ngày tạo đơn, mã đơn, trạng thái đơn hoặc trạng thái thanh toán.
- Employee Orders & Payments: cùng bộ lọc, nhưng nhân viên chỉ thấy các đơn POS do chính mình tạo.
- Customer panel: Admin dùng `/admin/customers`; Employee dùng `/employee/customers`. Có tìm kiếm tên, email, số điện thoại, địa chỉ; lọc hạng; xem hồ sơ gồm địa chỉ mặc định, tổng chi tiêu, phần trăm thành viên và tối đa 10 đơn mới nhất.
- Hồ sơ khách hàng cho phép sửa địa chỉ mặc định. Migration nạp địa chỉ giao gần nhất từ đơn cũ nếu hồ sơ chưa có; đơn Online mới ghi địa chỉ đầu tiên làm mặc định khi khách chưa lưu trước đó.

## Hạng thành viên

| Hạng | Tổng chi tiêu tích lũy | Giảm mỗi đơn |
|---|---:|---:|
| Mới | 0đ | 0% |
| Bạc | 100.000đ | 2% |
| Vàng | 250.000đ | 3% |
| Bạch kim | 500.000đ | 4% |
| Kim cương | 1.000.000đ | 5% |

Máy chủ tính giảm giá theo hạng hiện có tại thời điểm định giá. Khi thanh toán thành công/đơn COD hoàn tất, tiền thực thu của đơn được cộng vào tổng chi tiêu và hạng được cập nhật. Hạng mới áp dụng từ đơn tiếp theo. Khi yêu cầu hoàn tiền được xác nhận chi trả, tiền hoàn được trừ khỏi chi tiêu tích lũy và hạng được tính lại. Cả preview và tạo đơn đều tải hạng mới nhất từ MySQL để tránh snapshot phiên đăng nhập cũ.

## Migration

Trên database đang sử dụng, chạy `database/05_CUSTOMER_TIERS_ADDRESS_REPORTS.sql` sau migrations 02–04. Migration không xóa đơn hàng, tài khoản, thanh toán hay lịch sử hoàn tiền. Không đổi `spring.jpa.hibernate.ddl-auto=validate`.

## API chính

- `GET /api/admin/overview`
- `GET /api/admin/revenue?fromDate=YYYY-MM-DD&toDate=YYYY-MM-DD&customer=...&orderCode=...`
- `GET /api/admin/orders?date=YYYY-MM-DD&orderCode=...&status=...`
- `GET /api/admin/customers?q=...&tier=...` và `GET /api/admin/customers/{id}`
- `GET /api/pos/orders?date=YYYY-MM-DD&orderCode=...&status=...`
- `GET /api/pos/customers?q=...&tier=...` và `GET /api/pos/customers/{id}`
- `GET/PATCH /api/account/me` trả về/lưu `defaultAddress`.

## Danh sách kiểm tra khi chạy local

1. Chạy migration 05 trên đúng schema trong `spring.datasource.url`.
2. Khởi động backend với JDK 17 và kiểm tra `/api/health`.
3. Đăng nhập Admin, kiểm tra Overview hiển thị đơn gần đây; lọc doanh thu bằng từng trường và kết hợp nhiều trường.
4. Ở Admin và Employee, lọc đơn theo ngày, mã đơn, order status, payment status.
5. Đăng nhập Employee, tìm khách theo tên/email/điện thoại/địa chỉ và lọc đủ 5 hạng.
6. Mở chi tiết khách, đối chiếu địa chỉ mặc định và 10 đơn gần nhất.
7. Thử tài khoản có tổng chi tiêu lần lượt dưới, đúng và trên các mốc 100.000đ, 250.000đ, 500.000đ, 1.000.000đ.
8. Tạo đơn Online và POS với khách đã đăng ký; kiểm tra `memberDiscount` và dòng giảm thành viên trong đơn.
9. Xác nhận thanh toán một đơn làm khách vừa vượt mốc; kiểm tra đơn hiện tại giữ mức giảm cũ và đơn tiếp theo dùng mức mới.
10. Xác nhận hoàn tiền một phần/toàn phần; kiểm tra tổng chi tiêu và hạng được tính lại tương ứng.

## Giới hạn xác nhận

Kiểm tra cú pháp tĩnh frontend và cấu trúc mã nguồn đã chạy trong môi trường đóng gói. Cần chạy `mvn clean package`, `npm run build` và kiểm thử MySQL thực tế trên máy phát triển để xác nhận runtime.
