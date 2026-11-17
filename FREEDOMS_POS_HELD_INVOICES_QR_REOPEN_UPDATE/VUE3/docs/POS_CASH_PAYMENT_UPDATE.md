# FREEDOMS POS – thanh toán tiền mặt và in hóa đơn

## Luồng tiền mặt tại quầy

1. Thêm sản phẩm và chọn khách hàng như trước.
2. Chọn phương thức **Tiền mặt**.
3. Nhập **Số tiền khách đưa (VNĐ)**.
4. POS hiển thị tổng khách phải trả, số tiền khách đưa và **tiền thừa trả khách**.
5. Chỉ khi số tiền khách đưa là số nguyên VNĐ và lớn hơn hoặc bằng tổng phải trả, nút thanh toán mới được bật.
6. Xác nhận thanh toán sẽ tạo đơn POS đã thanh toán, hoàn thành ngay và lưu số tiền khách đưa/tiền thừa vào ghi chú thanh toán.
7. Nhấn **In hóa đơn** trên thông báo kết quả. Trang hóa đơn có thêm hai dòng tiền khách đưa và tiền thừa, sau đó nhấn **In / Lưu PDF**.

## Chuyển khoản

Phương thức **Chuyển khoản** vẫn hoạt động riêng. Chỉ phương thức này mới tạo QR VietQR và chờ nhân viên xác nhận tiền vào. Tiền mặt không hiển thị QR và không đi qua luồng xác nhận chuyển khoản.

## Các phần đã sửa

- `frontend/src/views/PosView.vue`: nhập tiền khách đưa, tính tiền thừa, chặn thanh toán thiếu tiền, chỉ dựng QR khi chọn chuyển khoản và thêm nút in hóa đơn.
- `frontend/src/views/PrintDocumentView.vue`: in số tiền khách đưa và tiền thừa với hóa đơn tiền mặt.
- `backend/src/main/java/vn/shoegroup/sd11/api/ApiDtos.java`: bổ sung trường tiền nhận/tiền thừa cho yêu cầu checkout và dữ liệu hóa đơn.
- `backend/src/main/java/vn/shoegroup/sd11/api/ApiPosController.java`: kiểm tra dữ liệu tiền mặt và trả lại kết quả thanh toán.
- `backend/src/main/java/vn/shoegroup/sd11/service/HeldInvoiceService.java` và `OrderService.java`: truyền số tiền khách đưa vào giao dịch, kiểm tra đủ tiền và lưu số tiền nhận/tiền thừa trong `payments.note`.
- Các API đọc hóa đơn trả thêm `cashReceived` và `cashChange` để trang in đọc lại được.

## Cơ sở dữ liệu

Không cần chạy migration mới. Số tiền khách đưa và tiền thừa được lưu trong cột `payments.note` đã có sẵn; cấu trúc bảng không thay đổi.

## Chạy dự án

- Backend: mở thư mục `backend` bằng IntelliJ, dùng JDK 17 và chạy ứng dụng Spring Boot như hiện tại.
- Frontend: mở terminal tại thư mục `frontend`, chạy `npm install` nếu máy chưa cài dependencies, sau đó `npm run dev`.
