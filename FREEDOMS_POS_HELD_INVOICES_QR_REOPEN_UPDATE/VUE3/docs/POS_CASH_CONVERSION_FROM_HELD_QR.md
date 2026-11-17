# FREEDOMS POS – đổi hóa đơn chờ chuyển khoản sang tiền mặt

## Phạm vi

Chỉ áp dụng cho đơn đã được tạo từ kênh `POS` và còn chờ thanh toán chuyển khoản (`PENDING` hoặc `WAITING_CONFIRMATION`). Đơn online không có API chuyển đổi này.

## Cách sử dụng

1. Mở `/admin/held-invoices` hoặc `/employee/held-invoices`.
2. Tại mục **Đơn POS chờ xác nhận chuyển khoản QR**, chọn **Chuyển sang tiền mặt** trên đúng đơn.
3. Nhập **Số tiền khách đưa**. POS tự tính tiền thừa và không cho xác nhận nếu khách đưa thiếu hoặc nhập số lẻ.
4. Chỉ xác nhận sau khi đã nhận tiền mặt và đã kiểm tra tài khoản ngân hàng chưa nhận khoản chuyển của đơn đó.
5. Hệ thống giữ nguyên mã đơn, sản phẩm, số lượng và tồn kho; đổi giao dịch hiện tại sang `CASH / PAID`, chuyển đơn thành `COMPLETED`, cập nhật lịch sử và ghi nhận doanh thu/thành viên đúng một lần.
6. Chọn **In hóa đơn** để in mã đơn, tổng thanh toán, tiền khách đưa và tiền thừa.

## Kỹ thuật

- API mới: `POST /api/pos/orders/{orderId}/convert-to-cash`.
- Backend xác minh `salesChannel = POS`, quyền của nhân viên và trạng thái thanh toán còn chờ.
- Dùng lại bản ghi `payments` hiện tại, không tạo đơn hoặc payment mới, không trừ kho lần hai.
- Số tiền khách đưa và tiền thừa lưu trong `payments.note` hiện có; không cần SQL migration.
- `PrintDocumentView.vue` đã đọc `cashReceived` và `cashChange` từ API POS nên in theo giao dịch tiền mặt sau khi chuyển đổi.

## Lưu ý nghiệp vụ

`WAITING_CONFIRMATION` nghĩa là có thể đã có người thông báo chuyển khoản. Nhân viên phải đối soát tài khoản ngân hàng trước khi xác nhận thu tiền mặt để tránh ghi nhận hai lần cho cùng một đơn.
