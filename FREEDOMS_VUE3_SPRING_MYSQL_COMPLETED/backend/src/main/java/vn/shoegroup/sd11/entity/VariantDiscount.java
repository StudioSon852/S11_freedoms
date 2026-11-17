package vn.shoegroup.sd11.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(name = "variant_discounts")
public class VariantDiscount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="variant_discount_id")
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="product_variant_id", nullable=false)
    private ProductVariant productVariant;

    @Column(name="sales_channel", nullable=false, length=20)
    private String salesChannel;

    @Column(name="discount_type", nullable=false, length=20)
    private String discountType;

    @Column(name="discount_value", nullable=false, precision=15, scale=2)
    private BigDecimal discountValue;

    @Column(name="starts_at")
    private LocalDateTime startsAt;

    @Column(name="ends_at")
    private LocalDateTime endsAt;

    @Column(name="is_active", nullable=false)
    private Boolean active;

    @Column(name="reason", length=255)
    private String reason;

    public VariantDiscount() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ProductVariant getProductVariant() { return productVariant; }
    public void setProductVariant(ProductVariant productVariant) { this.productVariant = productVariant; }
    public String getSalesChannel() { return salesChannel; }
    public void setSalesChannel(String salesChannel) { this.salesChannel = salesChannel; }
    public String getDiscountType() { return discountType; }
    public void setDiscountType(String discountType) { this.discountType = discountType; }
    public BigDecimal getDiscountValue() { return discountValue; }
    public void setDiscountValue(BigDecimal discountValue) { this.discountValue = discountValue; }
    public LocalDateTime getStartsAt() { return startsAt; }
    public void setStartsAt(LocalDateTime startsAt) { this.startsAt = startsAt; }
    public LocalDateTime getEndsAt() { return endsAt; }
    public void setEndsAt(LocalDateTime endsAt) { this.endsAt = endsAt; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}