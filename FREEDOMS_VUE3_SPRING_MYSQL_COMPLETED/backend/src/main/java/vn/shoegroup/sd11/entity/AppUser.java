package vn.shoegroup.sd11.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(name = "users")
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="user_id")
    private Long id;

    @ManyToOne(fetch=FetchType.EAGER)
    @JoinColumn(name="role_id", nullable=false)
    private Role role;

    @ManyToOne(fetch=FetchType.EAGER)
    @JoinColumn(name="tier_id")
    private MemberTier tier;

    @Column(name="email", nullable=false, unique=true, length=190)
    private String email;

    @Column(name="password_hash", nullable=false, length=100)
    private String passwordHash;

    @Column(name="full_name", nullable=false, length=150)
    private String fullName;

    @Column(name="phone", length=30)
    private String phone;

    @Column(name="default_address", length=500)
    private String defaultAddress;

    @Column(name="is_active", nullable=false)
    private Boolean active;

    @Column(name="total_spent", nullable=false, precision=15, scale=2)
    private BigDecimal totalSpent;

    @Column(name="created_at", nullable=false)
    private LocalDateTime createdAt;

    public AppUser() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public MemberTier getTier() { return tier; }
    public void setTier(MemberTier tier) { this.tier = tier; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getDefaultAddress() { return defaultAddress; }
    public void setDefaultAddress(String defaultAddress) { this.defaultAddress = defaultAddress; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public BigDecimal getTotalSpent() { return totalSpent; }
    public void setTotalSpent(BigDecimal totalSpent) { this.totalSpent = totalSpent; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}