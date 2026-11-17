package vn.shoegroup.sd11.api;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.shoegroup.sd11.config.CustomUserDetails;
import vn.shoegroup.sd11.entity.AppUser;
import vn.shoegroup.sd11.repository.UserRepository;
import vn.shoegroup.sd11.api.ApiDtos.ProfileUpdateRequest;
import vn.shoegroup.sd11.api.ApiDtos.MessageView;
import java.util.Map;

@RestController
@RequestMapping("/api/account")
public class ApiAccountController {
    private final UserRepository users;
    public ApiAccountController(UserRepository users) { this.users = users; }

    @GetMapping("/me")
    public Map<String,Object> me(@AuthenticationPrincipal CustomUserDetails principal) {
        AppUser u = requireUser(principal);
        return Map.of("id",u.getId(),"email",u.getEmail(),"fullName",u.getFullName(),"phone",u.getPhone()==null?"":u.getPhone(),
                "role",u.getRole().getRoleCode(),"tier",u.getTier()==null?"Mới":u.getTier().getTierName(),
                "totalSpent",u.getTotalSpent()==null?java.math.BigDecimal.ZERO:u.getTotalSpent(),"tierDiscountPercent",u.getTier()==null?java.math.BigDecimal.ZERO:u.getTier().getDiscountPercent(),
                "defaultAddress",u.getDefaultAddress()==null?"":u.getDefaultAddress());
    }

    @PatchMapping("/me")
    public MessageView update(@AuthenticationPrincipal CustomUserDetails principal, @RequestBody ProfileUpdateRequest request) {
        AppUser u = requireUser(principal);
        if (request.fullName()==null || request.fullName().isBlank()) throw new IllegalArgumentException("Họ tên không được để trống.");
        u.setFullName(request.fullName().trim()); u.setPhone(request.phone()==null?null:request.phone().trim());
        u.setDefaultAddress(request.defaultAddress()==null?null:request.defaultAddress().trim());
        users.save(u);
        return new MessageView(true,"Đã lưu thông tin tài khoản.");
    }

    private AppUser requireUser(CustomUserDetails principal) {
        if(principal==null) throw new IllegalArgumentException("Bạn chưa đăng nhập.");
        return users.findById(principal.getUser().getId()).orElseThrow(()->new IllegalArgumentException("Tài khoản không còn tồn tại."));
    }
}
