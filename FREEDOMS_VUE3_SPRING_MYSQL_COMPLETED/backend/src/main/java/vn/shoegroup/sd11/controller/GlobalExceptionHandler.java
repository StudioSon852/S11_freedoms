package vn.shoegroup.sd11.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public String handleBusinessError(IllegalArgumentException ex, Model model) {
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/error";
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public String handleDuplicate(DataIntegrityViolationException ex, Model model) {
        model.addAttribute("errorMessage", "Dữ liệu bị trùng hoặc vi phạm khóa ngoại. Hãy kiểm tra SKU, email và các dữ liệu liên kết.");
        return "error/error";
    }
}
