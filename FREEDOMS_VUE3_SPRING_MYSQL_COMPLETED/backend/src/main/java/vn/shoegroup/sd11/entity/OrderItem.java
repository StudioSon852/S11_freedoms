package vn.shoegroup.sd11.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(name = "order_items")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="order_item_id")
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="order_id", nullable=false)
    private CustomerOrder order;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="product_variant_id", nullable=false)
    private ProductVariant productVariant;

    @Column(name="product_name_snapshot", nullable=false, length=180)
    private String productNameSnapshot;

    @Column(name="variant_label_snapshot", nullable=false, length=100)
    private String variantLabelSnapshot;

    @Column(name="sku_snapshot", nullable=false, length=80)
    private String skuSnapshot;

    @Column(name="unit_price", nullable=false, precision=15, scale=2)
    private BigDecimal unitPrice;

    @Column(name="quantity", nullable=false)
    private Integer quantity;

    @Column(name="variant_discount_amount", nullable=false, precision=15, scale=2)
    private BigDecimal variantDiscountAmount;

    @Column(name="member_discount_amount", nullable=false, precision=15, scale=2)
    private BigDecimal memberDiscountAmount;

    @Column(name="coupon_discount_amount", nullable=false, precision=15, scale=2)
    private BigDecimal couponDiscountAmount;

    @Column(name="line_total", nullable=false, precision=15, scale=2)
    private BigDecimal lineTotal;

    public OrderItem() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public CustomerOrder getOrder() { return order; }
    public void setOrder(CustomerOrder order) { this.order = order; }
    public ProductVariant getProductVariant() { return productVariant; }
    public void setProductVariant(ProductVariant productVariant) { this.productVariant = productVariant; }
    public String getProductNameSnapshot() { return productNameSnapshot; }
    public void setProductNameSnapshot(String productNameSnapshot) { this.productNameSnapshot = productNameSnapshot; }
    public String getVariantLabelSnapshot() { return variantLabelSnapshot; }
    public void setVariantLabelSnapshot(String variantLabelSnapshot) { this.variantLabelSnapshot = variantLabelSnapshot; }
    public String getSkuSnapshot() { return skuSnapshot; }
    public void setSkuSnapshot(String skuSnapshot) { this.skuSnapshot = skuSnapshot; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getVariantDiscountAmount() { return variantDiscountAmount; }
    public void setVariantDiscountAmount(BigDecimal variantDiscountAmount) { this.variantDiscountAmount = variantDiscountAmount; }
    public BigDecimal getMemberDiscountAmount() { return memberDiscountAmount; }
    public void setMemberDiscountAmount(BigDecimal memberDiscountAmount) { this.memberDiscountAmount = memberDiscountAmount; }
    public BigDecimal getCouponDiscountAmount() { return couponDiscountAmount; }
    public void setCouponDiscountAmount(BigDecimal couponDiscountAmount) { this.couponDiscountAmount = couponDiscountAmount; }
    public BigDecimal getLineTotal() { return lineTotal; }
    public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }
}