package vn.shoegroup.sd11.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(name = "coupons")
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="coupon_id")
    private Long id;

    @Column(name="coupon_code", nullable=false, unique=true, length=60)
    private String couponCode;

    @Column(name="sales_channel", nullable=false, length=20)
    private String salesChannel;

    @Column(name="discount_type", nullable=false, length=20)
    private String discountType;

    @Column(name="discount_value", nullable=false, precision=15, scale=2)
    private BigDecimal discountValue;

    @Column(name="min_order_amount", nullable=false, precision=15, scale=2)
    private BigDecimal minOrderAmount;

    @Column(name="max_discount", precision=15, scale=2)
    private BigDecimal maxDiscount;

    @Column(name="usage_limit")
    private Integer usageLimit;

    @Column(name="usage_count", nullable=false)
    private Integer usageCount;

    @Column(name="starts_at")
    private LocalDateTime startsAt;

    @Column(name="ends_at")
    private LocalDateTime endsAt;

    @Column(name="is_active", nullable=false)
    private Boolean active;

    public Coupon() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }
    public String getSalesChannel() { return salesChannel; }
    public void setSalesChannel(String salesChannel) { this.salesChannel = salesChannel; }
    public String getDiscountType() { return discountType; }
    public void setDiscountType(String discountType) { this.discountType = discountType; }
    public BigDecimal getDiscountValue() { return discountValue; }
    public void setDiscountValue(BigDecimal discountValue) { this.discountValue = discountValue; }
    public BigDecimal getMinOrderAmount() { return minOrderAmount; }
    public void setMinOrderAmount(BigDecimal minOrderAmount) { this.minOrderAmount = minOrderAmount; }
    public BigDecimal getMaxDiscount() { return maxDiscount; }
    public void setMaxDiscount(BigDecimal maxDiscount) { this.maxDiscount = maxDiscount; }
    public Integer getUsageLimit() { return usageLimit; }
    public void setUsageLimit(Integer usageLimit) { this.usageLimit = usageLimit; }
    public Integer getUsageCount() { return usageCount; }
    public void setUsageCount(Integer usageCount) { this.usageCount = usageCount; }
    public LocalDateTime getStartsAt() { return startsAt; }
    public void setStartsAt(LocalDateTime startsAt) { this.startsAt = startsAt; }
    public LocalDateTime getEndsAt() { return endsAt; }
    public void setEndsAt(LocalDateTime endsAt) { this.endsAt = endsAt; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}