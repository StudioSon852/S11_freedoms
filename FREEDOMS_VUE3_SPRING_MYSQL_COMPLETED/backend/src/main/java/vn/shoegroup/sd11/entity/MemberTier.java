package vn.shoegroup.sd11.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(name = "member_tiers")
public class MemberTier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="tier_id")
    private Long id;

    @Column(name="tier_code", nullable=false, unique=true, length=30)
    private String tierCode;

    @Column(name="tier_name", nullable=false, length=80)
    private String tierName;

    @Column(name="min_spending", nullable=false, precision=15, scale=2)
    private BigDecimal minSpending;

    @Column(name="discount_percent", nullable=false, precision=5, scale=2)
    private BigDecimal discountPercent;

    @Column(name="sort_order", nullable=false)
    private Integer sortOrder;

    public MemberTier() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTierCode() { return tierCode; }
    public void setTierCode(String tierCode) { this.tierCode = tierCode; }
    public String getTierName() { return tierName; }
    public void setTierName(String tierName) { this.tierName = tierName; }
    public BigDecimal getMinSpending() { return minSpending; }
    public void setMinSpending(BigDecimal minSpending) { this.minSpending = minSpending; }
    public BigDecimal getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(BigDecimal discountPercent) { this.discountPercent = discountPercent; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}