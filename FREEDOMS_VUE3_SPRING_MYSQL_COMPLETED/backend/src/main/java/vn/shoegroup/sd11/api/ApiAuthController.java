package vn.shoegroup.sd11.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import vn.shoegroup.sd11.config.CustomUserDetails;
import vn.shoegroup.sd11.dto.RegisterForm;
import vn.shoegroup.sd11.service.AuthService;
import java.util.Map;
import static vn.shoegroup.sd11.api.ApiDtos.*;

@RestController
@RequestMapping("/api/auth")
public class ApiAuthController {
    private final AuthService authService;
    public ApiAuthController(AuthService authService) { this.authService = authService; }

    @GetMapping("/csrf")
    public Map<String,String> csrf(CsrfToken token) { return Map.of("headerName", token.getHeaderName(), "token", token.getToken()); }

    @GetMapping("/me")
    public ApiUser me(@AuthenticationPrincipal CustomUserDetails principal) {
        if (principal == null) throw new IllegalArgumentException("Bạn chưa đăng nhập.");
        return toUser(principal);
    }

    @PostMapping("/register")
    public MessageView register(@Valid @RequestBody RegisterRequest request) {
        RegisterForm form = new RegisterForm();
        form.setEmail(request.email()); form.setPassword(request.password()); form.setConfirmPassword(request.confirmPassword());
        form.setFullName(request.fullName()); form.setPhone(request.phone());
        authService.register(form);
        return new MessageView(true, "Tạo tài khoản thành công. Hãy đăng nhập để tiếp tục.");
    }

    public static ApiUser toUser(CustomUserDetails principal) {
        var u = principal.getUser();
        return new ApiUser(u.getId(), u.getEmail(), u.getFullName(), u.getRole().getRoleCode(),
                u.getTier() == null ? "Mới" : u.getTier().getTierName(), u.getTotalSpent(),
                u.getTier() == null ? java.math.BigDecimal.ZERO : u.getTier().getDiscountPercent());
    }

    public record RegisterRequest(String email, String password, String confirmPassword, String fullName, String phone) {}
}
