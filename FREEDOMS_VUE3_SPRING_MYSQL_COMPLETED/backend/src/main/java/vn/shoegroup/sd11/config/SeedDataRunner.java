package vn.shoegroup.sd11.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.shoegroup.sd11.entity.AppUser;
import vn.shoegroup.sd11.entity.MemberTier;
import vn.shoegroup.sd11.entity.Role;
import vn.shoegroup.sd11.repository.MemberTierRepository;
import vn.shoegroup.sd11.repository.RoleRepository;
import vn.shoegroup.sd11.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Configuration
public class SeedDataRunner {
    @Bean
    CommandLineRunner seedDemoAccounts(UserRepository users, RoleRepository roles, MemberTierRepository tiers,
            PasswordEncoder encoder,
            @Value("${app.seed.enabled:true}") boolean enabled,
            @Value("${app.seed.admin-email:admin@company.com}") String adminEmail,
            @Value("${app.seed.admin-password:adminTest!123}") String adminPassword,
            @Value("${app.seed.employee-email:staff01@company.com}") String employeeEmail,
            @Value("${app.seed.employee-password:staffTest!123}") String employeePassword) {
        return args -> {
            if (!enabled) return;
            ensureMemberTiers(tiers, users);
            MemberTier newTier = tiers.findByTierCode("NEW").orElse(null);
            createIfMissing(users, roles, newTier, encoder, adminEmail, adminPassword, "Quản trị FREEDOMS", "0901000001", "ADMIN");
            createIfMissing(users, roles, newTier, encoder, employeeEmail, employeePassword, "Nhân viên bán hàng", "0901000002", "EMPLOYEE");
        };
    }

    private void ensureMemberTiers(MemberTierRepository tiers, UserRepository users) {
        saveTier(tiers, "NEW", "Mới", "0", "0", 1);
        saveTier(tiers, "SILVER", "Bạc", "100000", "2", 2);
        saveTier(tiers, "GOLD", "Vàng", "250000", "3", 3);
        saveTier(tiers, "PLATINUM", "Bạch kim", "500000", "4", 4);
        saveTier(tiers, "DIAMOND", "Kim cương", "1000000", "5", 5);
        var all = tiers.findAllByOrderByMinSpendingAsc();
        for (AppUser customer : users.findByRole_RoleCodeOrderByIdDesc("CUSTOMER")) {
            java.math.BigDecimal spent = customer.getTotalSpent() == null ? BigDecimal.ZERO : customer.getTotalSpent();
            MemberTier selected = all.get(0);
            for (MemberTier tier : all) if (spent.compareTo(tier.getMinSpending()) >= 0) selected = tier;
            if (customer.getTier() == null || !customer.getTier().getId().equals(selected.getId())) {
                customer.setTier(selected);
                users.save(customer);
            }
        }
    }

    private void saveTier(MemberTierRepository tiers, String code, String name, String min, String discount, int sort) {
        MemberTier tier = tiers.findByTierCode(code).orElseGet(MemberTier::new);
        tier.setTierCode(code); tier.setTierName(name); tier.setMinSpending(new BigDecimal(min));
        tier.setDiscountPercent(new BigDecimal(discount)); tier.setSortOrder(sort);
        tiers.save(tier);
    }

    private void createIfMissing(UserRepository users, RoleRepository roles, MemberTier tier, PasswordEncoder encoder,
                                 String email, String password, String name, String phone, String roleCode) {
        if (users.existsByEmailIgnoreCase(email)) return;
        Role role = roles.findByRoleCode(roleCode).orElseThrow(() -> new IllegalStateException("Thiếu role " + roleCode + " trong database"));
        AppUser user = new AppUser();
        user.setRole(role); user.setTier(tier); user.setEmail(email); user.setPasswordHash(encoder.encode(password));
        user.setFullName(name); user.setPhone(phone); user.setActive(true); user.setTotalSpent(BigDecimal.ZERO);
        user.setCreatedAt(LocalDateTime.now()); users.save(user);
    }
}
