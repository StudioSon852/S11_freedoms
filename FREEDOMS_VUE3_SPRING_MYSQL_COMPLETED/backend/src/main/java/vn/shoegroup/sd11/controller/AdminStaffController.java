package vn.shoegroup.sd11.controller;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.shoegroup.sd11.entity.AppUser;
import vn.shoegroup.sd11.repository.MemberTierRepository;
import vn.shoegroup.sd11.repository.RoleRepository;
import vn.shoegroup.sd11.repository.UserRepository;
import java.math.BigDecimal;import java.time.LocalDateTime;
@Controller
@RequestMapping("/admin/staff")
public class AdminStaffController {
    private final UserRepository users;private final RoleRepository roles;private final MemberTierRepository tiers;private final PasswordEncoder encoder;
    public AdminStaffController(UserRepository users,RoleRepository roles,MemberTierRepository tiers,PasswordEncoder encoder){this.users=users;this.roles=roles;this.tiers=tiers;this.encoder=encoder;}
    @GetMapping public String list(Model model){model.addAttribute("staff",users.findAllByOrderByIdDesc().stream().filter(u->!"CUSTOMER".equals(u.getRole().getRoleCode())).toList());return "admin/staff";}
    @PostMapping @Transactional public String create(@RequestParam String fullName,@RequestParam String email,@RequestParam String password,
            @RequestParam String roleCode,@RequestParam(required=false) String phone,RedirectAttributes redirect){
        if(users.existsByEmailIgnoreCase(email)){redirect.addFlashAttribute("errorMessage","Email đã tồn tại.");return "redirect:/admin/staff";}
        if(!"EMPLOYEE".equals(roleCode)&&!"MANAGER".equals(roleCode)){redirect.addFlashAttribute("errorMessage","Chỉ được tạo tài khoản Nhân viên hoặc Quản lý.");return "redirect:/admin/staff";}
        if(password==null||password.length()<8){redirect.addFlashAttribute("errorMessage","Mật khẩu phải có ít nhất 8 ký tự.");return "redirect:/admin/staff";}
        AppUser u=new AppUser();u.setRole(roles.findByRoleCode(roleCode).orElseThrow());u.setTier(tiers.findByTierCode("NEW").orElse(null));
        u.setEmail(email.trim().toLowerCase());u.setPasswordHash(encoder.encode(password));u.setFullName(fullName.trim());u.setPhone(phone);
        u.setActive(true);u.setTotalSpent(BigDecimal.ZERO);u.setCreatedAt(LocalDateTime.now());users.save(u);
        redirect.addFlashAttribute("successMessage","Đã tạo tài khoản "+roleCode+".");return "redirect:/admin/staff";
    }
}
