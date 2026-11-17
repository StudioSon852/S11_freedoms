package vn.shoegroup.sd11.api;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;

@RestControllerAdvice(basePackages = "vn.shoegroup.sd11.api")
public class ApiExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("success", false, "message", ex.getMessage() == null ? "Dữ liệu không hợp lệ." : ex.getMessage()));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> conflict(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("success", false, "message", "Dữ liệu đã được sử dụng hoặc trùng khóa. Hãy kiểm tra mã sản phẩm, mã giảm giá và lịch sử đơn hàng."));
    }
}
