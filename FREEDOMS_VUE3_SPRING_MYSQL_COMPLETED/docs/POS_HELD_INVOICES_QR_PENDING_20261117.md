# POS Hóa đơn chờ – phiên chọn hàng và đơn QR chưa xác nhận

## Hành vi

- `/api/pos/sessions` cung cấp các phiên `OPEN` chưa hoàn tất. Phiên không có sản phẩm vẫn được giữ và có thể mở lại.
- `/api/pos/orders` cung cấp đơn POS trong phạm vi quyền của Admin/Manager hoặc nhân viên tạo đơn. Trang Hóa đơn chờ lọc những đơn `BANK_TRANSFER` có trạng thái thanh toán `PENDING` hoặc `WAITING_CONFIRMATION`, đồng thời loại đơn đã hủy/hoàn tất.
- Nút **Chọn sản phẩm / Tiếp tục POS** điều hướng đến `/admin/pos?sessionId=...` hoặc `/employee/pos?sessionId=...`; PosView nạp đúng phiên được chọn thay vì luôn chuyển sang phiên đầu tiên.
- Nút **Mở QR** dựng lại QR theo mã đơn và tổng tiền từ cấu hình ngân hàng, hiển thị nội dung chuyển khoản và các dòng sản phẩm. Nút **Đối soát tiền** chuyển sang màn Đơn hàng & thanh toán hiện có, tự điền mã đơn và trạng thái thanh toán để nhân viên mở đúng giao dịch cần xác nhận.

## API và database

Không cần endpoint hoặc migration mới. Tái sử dụng `/api/pos/sessions`, `/api/pos/orders`, `/api/store/payment-config`; dữ liệu đơn và giao dịch vẫn nằm trong các bảng hiện có.

## Kiểm tra thủ công

1. Mở POS, tạo phiên khách mới và lưu khi chưa chọn sản phẩm.
2. Vào Hóa đơn chờ, kiểm tra phiên có nhãn **CHƯA CHỌN SẢN PHẨM**. Mở phiên; POS phải chọn đúng mã phiên.
3. Thêm sản phẩm, lưu phiên, quay lại Hóa đơn chờ và mở **Tiếp tục POS**; các dòng hàng phải được khôi phục.
4. Tạo đơn POS bằng chuyển khoản QR; đơn phải xuất hiện trong mục **Chờ xác nhận QR** với trạng thái `PENDING`.
5. Mở QR, kiểm tra số tiền và nội dung chuyển khoản khớp đơn.
6. Sang Đơn hàng & thanh toán, xác nhận tiền vào sau khi đối soát; quay lại Hóa đơn chờ và làm mới. Đơn đã xác nhận không còn nằm trong danh sách QR chờ.
7. Nhân viên chỉ thấy phiên/đơn POS do mình tạo; Admin/Manager thấy dữ liệu theo quyền hiện có.
