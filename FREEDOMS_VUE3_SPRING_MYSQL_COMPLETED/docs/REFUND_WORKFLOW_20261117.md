# Quy trình yêu cầu hoàn tiền FREEDOMS — 17/11/2026

## 1. Phạm vi và nguyên tắc

Hệ thống sử dụng mô hình một đơn hàng có giao dịch thanh toán chính. Mỗi yêu cầu hoàn liên kết bằng `refunds.payment_id`; `order_id` không còn UNIQUE để cho phép nhiều yêu cầu hoàn cho cùng đơn/giao dịch. Quy tắc kiểm soát áp dụng ở service backend, không chỉ ở giao diện Vue.

- ONLINE: hạn gửi đến 48 giờ tính từ `payments.paid_at`; nếu dữ liệu cũ thiếu `paid_at`, dùng `orders.created_at`.
- POS: hạn gửi đến 24 giờ theo cùng cách xác định thời điểm.
- Mỗi `payment_id`: tối đa 2 lần gửi yêu cầu ở mọi trạng thái. Yêu cầu `REJECTED` vẫn tính là một lần gửi nhưng giải phóng phần hạn mức tiền đã giữ.
- Tổng tiền của yêu cầu đang chờ và đã hoàn (`PENDING + REFUNDED`) không vượt quá `payments.amount`.
- Yêu cầu hỗ trợ hoàn toàn phần (phần còn có thể hoàn) hoặc hoàn một phần (số tiền dương do người yêu cầu nhập).
- Một yêu cầu chỉ được đánh dấu `REFUNDED` sau khi nhân viên xác nhận đã chi/chuyển tiền và nhập mã tham chiếu/phiếu chi.
- `payments.transaction_reference` lưu mã giao dịch tiền vào ban đầu; `refunds.transaction_reference` lưu mã chuyển tiền hoàn đi.
- Đơn `COMPLETED` không được chuyển trực tiếp sang `CANCELLED`. Người dùng gửi yêu cầu hoàn tiền riêng; trạng thái giao hàng/hoàn thành vẫn bảo toàn lịch sử đơn.

## 2. Endpoint

| Ai | Endpoint | Mục đích |
|---|---|---|
| Khách | `POST /api/orders/{id}/refund-request` | Gửi yêu cầu với `{ "fullRefund": true, "amount": null, "reason": "..." }` hoặc `{ "fullRefund": false, "amount": 150000, "reason": "..." }` |
| Nhân viên POS | `POST /api/pos/orders/{orderId}/refund-request` | Gửi yêu cầu với cùng body; backend kiểm tra quyền sở hữu đơn POS |
| Admin/Manager | `POST /api/admin/orders/{id}/refund-request` | Hỗ trợ tạo yêu cầu từ màn quản lý đơn |
| Nhân viên/Admin/Manager | `GET /api/admin/refunds` | Xem hàng đợi yêu cầu |
| Nhân viên/Admin/Manager | `POST /api/admin/refunds/{id}/complete` | Xác nhận đã hoàn tiền; body có `reference` bắt buộc và `note` tùy chọn |
| Nhân viên/Admin/Manager | `POST /api/admin/refunds/{id}/reject` | Từ chối yêu cầu đang chờ; body có `note` |

## 3. Ma trận kiểm thử

Dùng dữ liệu thử có `payments.amount = 1,000,000` VND. Trước mỗi ca, tạo đơn/giao dịch độc lập để không ảnh hưởng kết quả các ca còn lại.

| Ca | Thao tác | Kết quả mong đợi |
|---|---|---|
| RF-01 | Online PAID 1 giờ trước; gửi full refund | HTTP thành công, refund 1,000,000, payment `REFUND_PENDING` |
| RF-02 | RF-01; chưa nhập reference, gọi complete | API báo lỗi, refund vẫn `PENDING` |
| RF-03 | RF-01; đã chi tiền, nhập reference rồi complete | refund `REFUNDED`, payment `REFUNDED`, incoming payment reference không đổi |
| RF-04 | Online PAID 1 giờ; gửi partial 300,000 rồi complete | refund `REFUNDED`, payment `PAID`, số dư hoàn còn 700,000 |
| RF-05 | Sau RF-04, yêu cầu partial 700,000 và complete | payment `REFUNDED` sau khi tổng thực tế hoàn = 1,000,000 |
| RF-06 | Gửi hai yêu cầu 300,000 và 700,000, cả hai PENDING | Cả hai có thể tạo; payment `REFUND_PENDING`; số tiền đang giữ = 1,000,000 |
| RF-07 | Sau RF-06, complete yêu cầu thứ nhất | Payment vẫn `REFUND_PENDING` vì yêu cầu thứ hai chưa hoàn thực tế |
| RF-08 | Sau RF-06, complete cả hai | Payment `REFUNDED` chỉ sau khi tổng tiền `REFUNDED` bằng giao dịch gốc |
| RF-09 | Tạo yêu cầu thứ ba cho cùng payment | Backend từ chối vì đạt giới hạn hai yêu cầu |
| RF-10 | Payment 1,000,000; gửi partial 800,000; gửi tiếp 300,000 | Yêu cầu thứ hai bị từ chối vì vượt số dư có thể hoàn 200,000 |
| RF-11 | Gửi partial 0 hoặc số âm | Backend từ chối |
| RF-12 | Online PAID 49 giờ trước | Backend từ chối yêu cầu hoàn |
| RF-13 | POS COMPLETED, PAID 23 giờ trước | Có thể gửi yêu cầu hoàn |
| RF-14 | POS COMPLETED, PAID 25 giờ trước | Backend từ chối yêu cầu hoàn |
| RF-15 | Gọi API cập nhật đơn COMPLETED → CANCELLED | Backend từ chối; trạng thái đơn vẫn COMPLETED |
| RF-16 | Nhân viên gửi yêu cầu hoàn cho đơn POS do nhân viên khác tạo | Backend từ chối quyền truy cập |
| RF-17 | Từ chối một yêu cầu PENDING | Refund `REJECTED`; payment được tính lại; số tiền được giải phóng nhưng lần gửi vẫn tính vào giới hạn hai lần |
| RF-18 | Hai yêu cầu PENDING tổng cộng bằng 1,000,000; complete một, từ chối yêu cầu còn lại | Payment `PAID` (đã hoàn một phần); chỉ số tiền đã thực chi mới được tính là đã hoàn |

## 4. Kiểm tra database sau mỗi ca

```sql
SELECT payment_id, amount, payment_status, transaction_reference, paid_at
FROM payments
WHERE payment_id = <PAYMENT_ID>;

SELECT refund_id, order_id, payment_id, amount, status,
       transaction_reference, requested_at, processed_at
FROM refunds
WHERE payment_id = <PAYMENT_ID>
ORDER BY requested_at, refund_id;

SELECT
  COALESCE(SUM(CASE WHEN status IN ('PENDING','REFUNDED') THEN amount ELSE 0 END), 0) AS reserved_amount,
  COALESCE(SUM(CASE WHEN status = 'REFUNDED' THEN amount ELSE 0 END), 0) AS actually_refunded,
  COUNT(*) AS submitted_requests
FROM refunds
WHERE payment_id = <PAYMENT_ID>;
```

Kiểm tra ràng buộc UNIQUE cũ đã được gỡ:

```sql
SELECT INDEX_NAME, NON_UNIQUE, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS indexed_columns
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'refunds'
GROUP BY INDEX_NAME, NON_UNIQUE;
```

Không được còn index UNIQUE chỉ gồm cột `order_id`. Không xóa các bản ghi order/payment/refund hiện có để chạy bộ kiểm thử; dùng đơn thử riêng hoặc backup database trước khi kiểm thử.
