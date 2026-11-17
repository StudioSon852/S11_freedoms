# Sửa hai lỗi biên dịch Java đã báo

1. `CouponRepository.java`: thêm import `java.util.List` để kiểu trả về `List<Coupon>` được nhận diện.
2. `ApiAdminController.java`: thay `Map.of(...)` 11 cặp key/value bằng `LinkedHashMap` vì Java chỉ cung cấp overload `Map.of` tối đa 10 cặp.

Sau khi giải nén, chạy backend tại thư mục `backend`:

```powershell
mvn -U clean compile
```

Vue là tiến trình riêng, chạy từ thư mục `frontend`:

```powershell
npm install
npm run dev
```

Có thể chạy IntelliJ (Spring Boot, cổng 8080) và Vite (thường cổng 5173) đồng thời. Lỗi compile Java không phải do Vue.
