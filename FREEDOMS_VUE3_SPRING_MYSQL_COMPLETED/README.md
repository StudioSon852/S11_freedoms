# FREEDOMS — Vue 3 + Spring MVC / REST API + Thymeleaf + Session + MySQL

Bản nâng cấp từ `FREEDOMS_SPRING_MVC_THYMELEAF.zip` theo kiến trúc dùng trong Java Web: Spring MVC Controller, REST Controller, Service, Repository, JPA Entity, Thymeleaf và Spring Security Session. Frontend quản trị/bán hàng mới dùng Vue 3 + Vite + Pinia + Vue Router + Axios. JWT/Bearer Token không được dùng trong luồng mới.

## 1. Công nghệ

- JDK 17; Spring Boot 3.2.5.
- Spring Web MVC và REST API (`@Controller`, `@RestController`).
- Spring Security Form Login + `JSESSIONID` Session Cookie + BCrypt + CSRF Cookie.
- Spring Data JPA / Hibernate.
- MySQL 8.
- Thymeleaf vẫn có trong backend cho các trang server-rendered cũ / fallback.
- Vue 3, Vite, Pinia, Vue Router, Axios cho giao diện chính.

## 2. Cấu trúc

```text
FREEDOMS_SPRING_MVC_THYMELEAF/
├── backend/
│   ├── pom.xml
│   ├── .env.example
│   └── src/main/
│       ├── java/vn/shoegroup/sd11/
│       │   ├── api/          # REST API cho Vue 3
│       │   ├── config/       # Security Session, seed data
│       │   ├── controller/   # Spring MVC + Vue SPA entry
│       │   ├── entity/       # JPA Entity
│       │   ├── repository/   # Spring Data JPA
│       │   ├── service/      # Order, POS, pricing, refund
│       │   └── shipping/     # GHN + regional fallback
│       └── resources/
│           ├── static/img/   # Picture1 và logo chim
│           └── templates/    # Thymeleaf fallback
├── database/
│   ├── freedoms_shop_mysql.sql
│   ├── 02_FEATURES_VUE_POS_SHIPPING_REFUNDS.sql
│   ├── 03_REFUND_PARTIAL_TWO_REQUESTS.sql
│   └── 04_VARIANT_IMAGES_CATALOG.sql
└── frontend/
    ├── package.json
    ├── vite.config.js
    └── src/
        ├── layouts/          # Header ngang + sidebar dọc
        ├── stores/           # Auth Session state + cart
        └── views/            # Store, checkout, POS, admin, employee
```

## Các cập nhật giao diện/nghiệp vụ trong bản này

- Quản lý sản phẩm cho phép chọn sản phẩm cũ để nạp dữ liệu vào biểu mẫu, cập nhật và lưu; tải ảnh đại diện, tải nhiều ảnh cho từng biến thể, thêm URL ảnh và gỡ riêng từng ảnh.
- Admin có CRUD danh mục, thương hiệu, màu sắc và kích cỡ tại các mục con của Quản lý sản phẩm. Dữ liệu master đang được sử dụng sẽ không bị xóa vật lý.
- Coupon và giảm giá biến thể hỗ trợ tìm kiếm, sửa, chuyển trạng thái **Hoạt động / Dừng**; bản ghi lịch sử được giữ lại khi dừng. Với giảm giá biến thể hợp lệ, hệ thống ưu tiên bản ghi mới nhất trước (ID lớn hơn được xét trước).
- Hồ sơ khách hàng có thể sửa tên, số điện thoại và địa chỉ mặc định; hồ sơ nhân viên có thể sửa thông tin liên hệ. Hồ sơ khách hàng hiển thị ưu đãi áp dụng kênh ONLINE hoặc BOTH.
- Tổng quan Admin có danh sách đơn gần đây giống tổng quan nhân viên; có báo cáo doanh thu lọc theo ngày tạo đơn, khách hàng (tên/điện thoại/email) và mã đơn. Doanh thu lọc trừ các khoản hoàn đã xác nhận.
- Mục Khách hàng xuất hiện trong panel Admin và thanh điều hướng nhân viên: tìm kiếm tên/email/số điện thoại/địa chỉ, lọc hạng, xem thông tin đầy đủ, địa chỉ mặc định và 10 đơn gần nhất.
- Đơn hàng & thanh toán ở cả Admin và nhân viên có bộ lọc ngày, mã đơn, trạng thái đơn hoặc trạng thái thanh toán.
- Hạng thành viên được cập nhật: Mới (0đ, 0%), Bạc (100.000đ, 2%), Vàng (250.000đ, 3%), Bạch kim (500.000đ, 4%), Kim cương (1.000.000đ, 5%). Tổng chi tiêu tích lũy được ghi nhận khi đơn đã thanh toán/hoàn thành; số tiền hoàn thực tế đã xác nhận được trừ khỏi tổng tích lũy và có thể làm thay đổi hạng. Chiết khấu hạng của đơn mới được áp dụng theo hạng hiện tại trước khi chốt đơn.
- Nhân viên có thanh điều hướng ngang gồm Tổng quan, POS, Hóa đơn chờ, Đơn hàng & thanh toán và Hoàn tiền; hồ sơ truy cập từ khu vực tài khoản, không nằm trong panel. POS Admin hiện hữu được giữ nguyên.
- Có trang in riêng cho hóa đơn đơn hàng và phiếu hoàn tiền. Trạng thái và số tiền được nhấn màu rõ hơn; logo FREEDOMS có chữ S màu đỏ.

## 3. Cài đặt lần đầu

### A. MySQL Workbench 8

1. Tạo database riêng tên `freedoms_shop`.
2. Nếu dựng database mới hoàn toàn: mở và chạy `database/freedoms_shop_mysql.sql`.
3. Nếu đang có database `freedoms_shop` từ bản ZIP Vue/API trước: **không chạy lại file cài đặt đầy đủ** vì file đó có lệnh DROP TABLE. Chạy `database/02_FEATURES_VUE_POS_SHIPPING_REFUNDS.sql` nếu database chưa có shipping/refunds/POS features.
4. Chạy tiếp `database/03_REFUND_PARTIAL_TWO_REQUESTS.sql`. Đây là migration bổ sung, gỡ ràng buộc UNIQUE cũ trên `refunds.order_id` để một giao dịch có tối đa hai yêu cầu hoàn tiền; không xóa dữ liệu hoàn tiền hiện có.
5. Chạy `database/04_VARIANT_IMAGES_CATALOG.sql` để bổ sung thư viện ảnh nhiều ảnh theo từng biến thể (an toàn khi chạy lại; không xóa dữ liệu ảnh).
6. Chạy `database/05_CUSTOMER_TIERS_ADDRESS_REPORTS.sql` để thêm địa chỉ mặc định khách hàng, cập nhật 5 hạng thành viên và tính lại hạng từ tổng chi tiêu hiện có (không xóa đơn hàng/khách hàng/thanh toán).
7. Kiểm tra database dùng `InnoDB` và tài khoản MySQL có quyền SELECT/INSERT/UPDATE/DELETE/ALTER/CREATE. Nếu chạy SQL bằng MySQL Workbench, chọn schema `freedoms_shop` và chạy từng file theo thứ tự trên.

### B. Backend trong IntelliJ IDEA 2024.1.7

1. Mở `backend/pom.xml` bằng IntelliJ và chọn Project SDK/Maven JDK là JDK 17.
2. Sao chép `backend/.env.example` thành `backend/.env`.
3. Sửa `DB_USERNAME` và `DB_PASSWORD` đúng với MySQL trên máy.
4. Điền thông tin tài khoản nhận tiền:

```properties
BANK_CODE=MB
BANK_ACCOUNT=SO_TAI_KHOAN_THAT
BANK_ACCOUNT_NAME=TEN_CHU_TAI_KHOAN
```

5. Maven Reload, sau đó chạy `vn.shoegroup.sd11.Sd11FreedomsApplication` hoặc:

```bash
cd backend
mvn clean spring-boot:run
```

Backend mặc định chạy tại `http://localhost:8080`.

### C. Frontend Vue 3

Mở terminal trong thư mục `frontend`:

```bash
npm install
npm run dev
```

Truy cập `http://localhost:5173/app/`.

Vite proxy `/api`, `/login`, `/logout`, `/uploads` về Spring Boot port 8080; do đó Vue dùng chung Session Cookie, không lưu JWT.

### D. Build Vue vào Spring Boot để chạy một cổng

Sau khi `npm install` thành công:

```bash
cd frontend
npm run build
```

Vite đặt kết quả trong `backend/src/main/resources/static/app`. Chạy backend và truy cập `http://localhost:8080/app/`.

## 4. Tài khoản demo

Khi `SEED_DEMO_DATA=true` và database chưa có các tài khoản tương ứng, backend tạo:

| Vai trò | Email | Mật khẩu |
|---|---|---|
| Admin | `admin@company.com` | `adminTest!123` |
| Nhân viên | `staff01@company.com` | `staffTest!123` |

Đổi mật khẩu demo và thông tin ngân hàng trước khi đưa lên môi trường thật. Khách hàng đăng ký qua màn hình `/login` → Đăng ký khách hàng.

## 5. Nghiệp vụ đã được thiết kế

### POS nhiều người mua cùng lúc

- Mỗi tab POS là một `held_invoices` độc lập; có thể chuyển đổi nhiều người mua mà không gộp giỏ.
- Hóa đơn chờ lưu tên/điện thoại/ghi chú/coupon và snapshot sản phẩm.
- Lưu hóa đơn chờ **không trừ kho**, không ghi nhận doanh thu và không tạo payment.
- Tồn kho chỉ được khóa và kiểm tra lại khi chốt bán trong transaction.
- POS tìm sản phẩm theo tên/SKU; nhập `sales` hoặc bật bộ lọc Sales để chỉ thấy biến thể đang giảm.
- Mặt hàng có ưu đãi hiển thị “Có mặt hàng đang giảm”; coupon Offline đang hoạt động hiển thị để nhân viên/khách chọn.
- Nhập số điện thoại đã đăng ký sẽ nhận diện khách có tài khoản và tính ưu đãi hạng thành viên.
- Có thể chuyển đổi giữa CASH và BANK_TRANSFER trước khi nhấn nút thanh toán.
- CASH: ghi nhận PAID và COMPLETED ngay.
- BANK_TRANSFER: tạo đơn PENDING_PAYMENT; sau khi nhân viên xác nhận tiền thực nhận, đơn POS tự chuyển COMPLETED.

### Online / giỏ hàng / thanh toán

- Giỏ hàng cho phép tích chọn một phần sản phẩm để thanh toán trước; các sản phẩm chưa chọn vẫn ở lại giỏ.
- Máy chủ tự đọc giá, stock, variant discount, tier discount và coupon; không tin giá tổng do trình duyệt gửi.
- Coupon được tách kênh `ONLINE`, `OFFLINE`, hoặc `BOTH`; danh sách Online chỉ hiển thị ở checkout, Offline chỉ hiển thị tại POS.
- Đơn Online chuyển khoản bắt đầu `PENDING_PAYMENT`; sau khi nhân viên xác nhận tiền, chuyển `CONFIRMED` để tiếp tục xử lý.

### Giảm giá

Thứ tự tính giá: giảm giá biến thể → giảm giá hạng thành viên → coupon theo channel. Tìm kiếm POS `sales` lọc theo giảm giá biến thể đang có hiệu lực cho kênh OFFLINE. Trang Admin tách riêng quản lý giảm giá biến thể và coupon Online/Offline.

### Vận chuyển / phí ship

- Checkout gọi backend preview để tính lại giá hàng và phí ship trước khi tạo đơn; backend tính lại lần nữa khi chốt đơn.
- Có thể cấu hình GHN qua `GHN_TOKEN`, `GHN_SHOP_ID`, `GHN_BASE_URL`; điền mã quận và mã phường GHN ở checkout để lấy phí báo từ GHN.
- Nếu thiếu cấu hình/mã địa chỉ hoặc GHN lỗi, backend dùng phí dự phòng khu vực: Hà Nội 20.000đ, Đà Nẵng 32.000đ, TP.HCM 38.000đ, khu vực khác 30.000đ.
- Miễn phí ship khi tiền hàng sau giảm đạt ngưỡng mặc định 1.000.000đ. Có thể chỉnh qua `SHIPPING_FREE_THRESHOLD`.
- POS/nhận tại cửa hàng không tính phí giao hàng.
- Khối lượng mặc định để báo GHN là 800g/đôi nếu frontend chưa gửi trọng lượng thực tế.

### Hủy đơn và hoàn tiền

- Đơn `COMPLETED` là trạng thái hoàn tất bán hàng/giao hàng và **không thể chuyển trực tiếp sang `CANCELLED`**, kể cả đơn POS. Muốn hủy giao dịch đã thanh toán, khách hàng hoặc nhân viên có quyền phải gửi yêu cầu hoàn tiền từ đơn đã thanh toán.
- Đơn chưa hoàn tất có thể hủy trước trạng thái `SHIPPING` theo luồng nghiệp vụ. Nếu payment đã `PAID`, hệ thống tạo yêu cầu hoàn cho phần tiền còn có thể hoàn và chuyển payment sang `REFUND_PENDING`. Nếu payment đang `WAITING_CONFIRMATION`, hệ thống bắt buộc đối soát ngân hàng trước, không tự giả định đã nhận tiền.
- Thời hạn yêu cầu tính từ `paid_at` (nếu thiếu dữ liệu cũ thì dự phòng `orders.created_at`): Online tối đa **48 giờ**; POS tối đa **24 giờ**. Hết thời hạn, backend từ chối cả yêu cầu hoàn mới và thao tác hủy có phát sinh hoàn.
- Mỗi giao dịch thanh toán (`payment_id`) được tạo tối đa **2 lần gửi yêu cầu**, kể cả lần bị từ chối; yêu cầu `REJECTED` giải phóng số tiền đã giữ nhưng vẫn tiêu thụ một lượt gửi. Có thể chọn hoàn toàn phần hoặc hoàn một phần. Tổng tiền đang chờ cộng tiền đã hoàn không được vượt `payments.amount`.
- Payment chỉ chuyển `REFUNDED` khi tổng các yêu cầu đã được xác nhận hoàn thực tế (`refunds.status = REFUNDED`) đạt số tiền giao dịch. Nếu còn yêu cầu đang chờ, payment giữ `REFUND_PENDING`; nếu chỉ hoàn một phần và không còn yêu cầu chờ, payment trở lại `PAID` với số dư chưa hoàn.
- Nhân viên/Admin/Manager phải chuyển khoản hoặc chi tiền thật trước khi nhấn xác nhận. Mã tham chiếu hoàn tiền lưu tại từng bản ghi `refunds.transaction_reference`; mã giao dịch tiền vào ban đầu của `payments` được giữ nguyên để đối soát hai chiều. Hệ thống không tự chuyển tiền ngân hàng.
- Nhân viên chỉ được tạo yêu cầu hoàn cho đơn POS thuộc phạm vi tài khoản mình trên endpoint POS; Admin/Manager quản lý toàn bộ đơn. Nhân viên có thể xử lý hàng đợi hoàn tiền, nhưng không có quyền truy cập các API quản trị sản phẩm/tài khoản.

## 6. API chính cho Vue

| API | Chức năng |
|---|---|
| `GET /api/auth/csrf`, `GET /api/auth/me` | CSRF token và tài khoản Session |
| `GET /api/store/products` | Danh sách sản phẩm, biến thể, giá sale |
| `GET /api/store/coupons?channel=ONLINE` | Coupon Online |
| `POST /api/checkout/preview` | Preview giá và phí ship |
| `POST /api/checkout` | Tạo đơn Online |
| `GET /api/orders/mine` | Lịch sử đơn khách hàng |
| `POST /api/orders/{id}/transfer-submitted` | Khách báo đã chuyển khoản |
| `GET/POST /api/pos/sessions` | Lấy/tạo phiên POS độc lập |
| `PUT /api/pos/sessions/{id}` | Lưu khách và giỏ hàng chờ |
| `GET /api/pos/sessions/{id}/preview` | Tính ưu đãi cho phiên POS |
| `POST /api/pos/sessions/{id}/checkout` | Chốt hóa đơn POS |
| `/api/admin/products`, `/api/admin/variant-discounts` | Quản lý kho và giảm giá biến thể |
| `/api/admin/users` | Tạo/khóa tài khoản và đổi vai trò nhân sự; bảo vệ Admin cuối cùng, chỉ Admin được cấp thêm quyền Admin |
| `/api/admin/products/{id}/variants` | Thêm biến thể size/màu; `/api/admin/variants/{id}` cập nhật giá/tồn kho/trạng thái |
| `/api/admin/coupons?channel=...` | Quản lý coupon theo kênh |
| `/api/admin/orders?date=&orderCode=&status=` | Danh sách đơn hàng có lọc theo ngày, mã đơn và trạng thái |
| `/api/admin/revenue?fromDate=&toDate=&customer=&orderCode=` | Báo cáo doanh thu có lọc theo thời gian, khách hàng, mã đơn |
| `/api/admin/customers?q=&tier=` và `/api/pos/customers?q=&tier=` | Tìm kiếm/lọc khách hàng; API `/customers/{id}` trả hồ sơ, địa chỉ mặc định và đơn gần đây |
| `/api/admin/refunds` | Tạo, xác nhận hoặc từ chối yêu cầu hoàn tiền |
| `/api/pos/orders/{id}/refund-request` | Nhân viên gửi yêu cầu hoàn tiền cho đơn POS thuộc phạm vi của mình |
| `/api/orders/{id}/refund-request` | Khách hàng gửi yêu cầu hoàn tiền cho đơn của chính mình |

## 7. Hình ảnh / giao diện

- Logo chim FREEDOMS nằm tại `/img/freedoms-bird.png`.
- Ảnh bạn gửi được lưu tại `/img/picture1.png` và thay đường dẫn ảnh của sản phẩm có tên Street Classic trong SQL/migration.
- Trang khách hàng dùng `App.vue` làm khung dùng chung: header tìm kiếm, điều hướng, tài khoản/giỏ hàng và footer nền trắng/chữ đen. Mục “Ưu đãi” được thay bằng phần giới thiệu FREEDOMS trên `HomeView.vue`; mini cart mở dạng drawer, trang chi tiết sản phẩm hỗ trợ chọn size/màu, thêm giỏ và mua ngay. Đăng nhập và đăng ký dùng nền chuyển sắc xanh như bản FREEDOMS_UPDATE; đăng ký có route `/register` (đường dẫn `/signup` chuyển tiếp sang `/register`).
- Admin và Employee dùng `DashboardLayout.vue`: sidebar xanh navy được chia nhóm nghiệp vụ, trang CRUD sản phẩm/tài khoản/đơn hàng/hoàn tiền có biểu mẫu chi tiết tham khảo MVC. POS giữ bố cục bán hàng tại quầy kiểu VictorSport, không đổi thao tác tạo phiên, giữ hóa đơn và thanh toán.
- Các mục điều hướng giữ đúng các route và chức năng hiện có: POS, hóa đơn chờ, đơn hàng, sản phẩm, ưu đãi/coupon và hoàn tiền.

## 8. Kiểm thử nghiệm thu hoàn tiền

`docs/REFUND_WORKFLOW_20261117.md` có ma trận kiểm thử chi tiết. Các ca quan trọng cần chạy trên MySQL sau khi áp dụng migration 03:

1. Giao dịch Online đã PAID trong 48 giờ: gửi hoàn toàn phần; xác nhận đã chuyển tiền; payment chỉ sang `REFUNDED` sau khi tổng hoàn thực tế bằng số tiền gốc.
2. Giao dịch Online đã PAID trong 48 giờ: gửi hoàn một phần; xác nhận; payment vẫn `PAID` nếu không còn yêu cầu đang chờ và số tiền đã hoàn nhỏ hơn số tiền gốc.
3. Với cùng một payment: tạo yêu cầu thứ nhất và thứ hai; yêu cầu thứ ba phải bị từ chối. Tổng tiền yêu cầu đang chờ/đã hoàn không được vượt số tiền giao dịch.
4. POS đã COMPLETED trong 24 giờ: tạo yêu cầu hoàn được; quá 24 giờ bị từ chối. Online quá 48 giờ bị từ chối.
5. Đơn `COMPLETED`: API đổi trạng thái sang `CANCELLED` phải bị từ chối; phải dùng endpoint yêu cầu hoàn tiền.
6. Từ chối một yêu cầu: giải phóng số tiền và lượt yêu cầu đó; payment status được tính lại từ số tiền đã hoàn thực tế và các yêu cầu còn chờ.
7. Xác nhận hoàn tiền phải nhập mã tham chiếu; mã giao dịch tiền vào ban đầu trên `payments.transaction_reference` không bị ghi đè.

## 9. Kiểm tra build và môi trường

Cài dependencies trong thư mục `frontend`, sau đó chạy `npm run build`. Mở `backend/pom.xml` trong IntelliJ IDEA 2024.1.7 với JDK 17, chạy Maven Reload và `mvn clean test` hoặc `mvn clean package`. Cuối cùng cấu hình MySQL ở `backend/.env` và thực hiện các ca ở mục 8. Bản ZIP không chứa thông tin đăng nhập MySQL/ngân hàng thật; không đưa mật khẩu hoặc tài khoản nhận tiền vào mã nguồn.

Tệp `DAT(1).zip` gốc không có trong các tệp đính kèm lượt này; phần shipping giữ mô hình GHN + phí fallback đã có trong dự án nền, không khẳng định đây là bản sao nguyên mã nguồn DAT.


## Bản cập nhật POS nhân viên và tải ảnh (17/11/2026)

- POS của **nhân viên** hiển thị sản phẩm dạng thẻ giống danh mục MVC: ảnh, tên/nhãn hàng, chọn size-màu trong một thanh chọn, giá bán/giá khuyến mại, tồn kho và nút **Thêm vào hóa đơn**.
- Phần checkout, giỏ POS, phương thức thanh toán, lưu hóa đơn chờ và các thao tác thanh toán hiện tại được giữ nguyên.
- POS Admin giữ cách trình bày danh sách sản phẩm trước đây; thay đổi dạng thẻ chỉ áp dụng cho `/employee/pos`.
- Tài khoản nhân viên bị chặn khỏi các đường dẫn mua hàng Online (`/store`, `/cart`, `/product/*`, `/checkout`, `/orders`, `/payment/*`); trang hồ sơ nhân viên và các chức năng POS vẫn dùng được.
- Ảnh sản phẩm có preview trước khi lưu. Backend nhận dạng PNG/JPG/WEBP/GIF/BMP dựa trên chữ ký tệp, không phụ thuộc hoàn toàn vào MIME type mà Windows/Snipping Tool gửi lên. Ảnh được giới hạn 8 MB và được phục vụ tại `/uploads/products/**`.
- Vite đã cấu hình proxy `/uploads` về backend port 8080 để ảnh vừa tải lên hiển thị được khi chạy frontend ở port 5173.

### Lưu ý khi đăng nhập ba tài khoản cùng lúc

Dự án hiện dùng phiên đăng nhập Spring Security (`JSESSIONID`). Các tab thông thường trong cùng một Chrome profile dùng chung cookie phiên, vì vậy đăng nhập tài khoản khác ở tab thứ hai sẽ thay đổi phiên của tab thứ nhất. Để chạy Admin, Manager và Employee đồng thời trên một máy, hãy mở ba hồ sơ trình duyệt riêng (Chrome Profile 1/2/3), hoặc dùng Chrome, Edge và Firefox riêng biệt; đăng nhập một tài khoản trong mỗi hồ sơ. Chỉ mở ba tab trong cùng một hồ sơ trình duyệt thì không tạo được ba phiên đăng nhập độc lập.


## Bản cập nhật báo cáo, khách hàng và hạng thành viên (17/11/2026)

- Admin Overview hiển thị 8 đơn gần đây và bộ lọc báo cáo doanh thu theo khoảng ngày, khách hàng, mã đơn; doanh thu lọc chỉ tính đơn `COMPLETED`/`PAID`, trừ các khoản hoàn thực tế đã được xác nhận.
- Đơn hàng & thanh toán (Admin và nhân viên) lọc theo ngày tạo đơn, mã đơn, trạng thái đơn hoặc trạng thái payment.
- Admin và nhân viên có mục Khách hàng, tìm theo tên/email/điện thoại/địa chỉ, lọc theo hạng và mở chi tiết tài khoản gồm địa chỉ mặc định, tổng chi tiêu, ưu đãi và 10 đơn gần nhất.
- Khách hàng có thể tự cập nhật địa chỉ mặc định ở Hồ sơ tài khoản. Migration 05 tự điền địa chỉ giao hàng gần nhất từ đơn cũ nếu hồ sơ chưa có địa chỉ; đơn Online mới cũng lưu địa chỉ mặc định đầu tiên khi khách chưa thiết lập.
- Thang hạng: Mới 0đ/0%; Bạc 100.000đ/2%; Vàng 250.000đ/3%; Bạch kim 500.000đ/4%; Kim cương 1.000.000đ/5%. Tổng tích lũy tăng khi thanh toán thành công/đơn hoàn tất và giảm theo số tiền hoàn đã xác nhận. Chiết khấu dùng hạng hiện tại lúc tính giá; khi đơn giúp khách đạt mốc mới, hạng mới áp dụng cho lần mua kế tiếp.
- Bắt buộc chạy migration `database/05_CUSTOMER_TIERS_ADDRESS_REPORTS.sql` trên database đang dùng trước khi khởi động backend; Hibernate giữ `ddl-auto=validate`, không tự tạo cột.

## Cập nhật Hóa đơn chờ POS (17/11/2026)

Trang Hóa đơn chờ hiện gộp hai nhóm: (1) phiên POS đang chọn hàng, bao gồm phiên chưa có sản phẩm, có thể mở lại đúng phiên bằng `sessionId`; (2) đơn POS chuyển khoản QR có thanh toán `PENDING` hoặc `WAITING_CONFIRMATION`, có thể mở lại QR và đi tới màn Đơn hàng & thanh toán để đối soát. Không cần migration SQL mới. Xem `docs/POS_HELD_INVOICES_QR_PENDING_20261117.md`.

