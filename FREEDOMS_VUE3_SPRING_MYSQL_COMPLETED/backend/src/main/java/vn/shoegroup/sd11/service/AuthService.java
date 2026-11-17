package vn.shoegroup.sd11.service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.shoegroup.sd11.dto.RegisterForm;
import vn.shoegroup.sd11.entity.AppUser;
import vn.shoegroup.sd11.repository.MemberTierRepository;
import vn.shoegroup.sd11.repository.RoleRepository;
import vn.shoegroup.sd11.repository.UserRepository;
import java.math.BigDecimal; import java.time.LocalDateTime;
@Service
public class AuthService {
    private final UserRepository users; private final RoleRepository roles; private final MemberTierRepository tiers; private final PasswordEncoder encoder;
    public AuthService(UserRepository users,RoleRepository roles,MemberTierRepository tiers,PasswordEncoder encoder){this.users=users;this.roles=roles;this.tiers=tiers;this.encoder=encoder;}
    @Transactional
    public void register(RegisterForm form){
        if (form == null || form.getFullName() == null || form.getFullName().isBlank())
            throw new IllegalArgumentException("Vui lòng nhập họ tên.");
        if (form.getEmail() == null || form.getEmail().isBlank() || !form.getEmail().contains("@"))
            throw new IllegalArgumentException("Vui lòng nhập email hợp lệ.");
        if (form.getPassword() == null || form.getPassword().length() < 8 || form.getPassword().length() > 72)
            throw new IllegalArgumentException("Mật khẩu phải có từ 8 đến 72 ký tự.");
        if (!form.getPassword().equals(form.getConfirmPassword())) throw new IllegalArgumentException("Mật khẩu nhập lại không khớp.");
        if(users.existsByEmailIgnoreCase(form.getEmail())) throw new IllegalArgumentException("Email này đã được đăng ký.");
        AppUser user=new AppUser(); user.setRole(roles.findByRoleCode("CUSTOMER").orElseThrow());
        user.setTier(tiers.findByTierCode("NEW").orElse(null)); user.setEmail(form.getEmail().trim().toLowerCase());
        user.setPasswordHash(encoder.encode(form.getPassword())); user.setFullName(form.getFullName().trim());
        user.setPhone(form.getPhone()); user.setActive(true); user.setTotalSpent(BigDecimal.ZERO); user.setCreatedAt(LocalDateTime.now()); users.save(user);
    }
}
