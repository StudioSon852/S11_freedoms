package vn.shoegroup.sd11.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(name = "orders")
public class CustomerOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="order_id")
    private Long id;

    @Column(name="order_code", nullable=false, unique=true, length=40)
    private String orderCode;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="customer_id")
    private AppUser customer;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="created_by")
    private AppUser createdBy;

    @Column(name="sales_channel", nullable=false, length=20)
    private String salesChannel;

    @Column(name="order_status", nullable=false, length=30)
    private String orderStatus;

    @Column(name="customer_name", nullable=false, length=150)
    private String customerName;

    @Column(name="customer_phone", nullable=false, length=30)
    private String customerPhone;

    @Column(name="shipping_address", length=500)
    private String shippingAddress;

    @Column(name="customer_note", length=500)
    private String customerNote;

    @Column(name="coupon_code", length=60)
    private String couponCode;

    @Column(name="subtotal", nullable=false, precision=15, scale=2)
    private BigDecimal subtotal;

    @Column(name="variant_discount_total", nullable=false, precision=15, scale=2)
    private BigDecimal variantDiscountTotal;

    @Column(name="member_discount_total", nullable=false, precision=15, scale=2)
    private BigDecimal memberDiscountTotal;

    @Column(name="coupon_discount_total", nullable=false, precision=15, scale=2)
    private BigDecimal couponDiscountTotal;

    @Column(name="shipping_fee", nullable=false, precision=15, scale=2)
    private BigDecimal shippingFee;

    @Column(name="total_amount", nullable=false, precision=15, scale=2)
    private BigDecimal totalAmount;

    @Column(name="spending_recorded", nullable=false)
    private Boolean spendingRecorded;

    @Column(name="created_at", nullable=false)
    private LocalDateTime createdAt;

    @Column(name="updated_at", nullable=false)
    private LocalDateTime updatedAt;

    public CustomerOrder() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String orderCode) { this.orderCode = orderCode; }
    public AppUser getCustomer() { return customer; }
    public void setCustomer(AppUser customer) { this.customer = customer; }
    public AppUser getCreatedBy() { return createdBy; }
    public void setCreatedBy(AppUser createdBy) { this.createdBy = createdBy; }
    public String getSalesChannel() { return salesChannel; }
    public void setSalesChannel(String salesChannel) { this.salesChannel = salesChannel; }
    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }
    public String getCustomerNote() { return customerNote; }
    public void setCustomerNote(String customerNote) { this.customerNote = customerNote; }
    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public BigDecimal getVariantDiscountTotal() { return variantDiscountTotal; }
    public void setVariantDiscountTotal(BigDecimal variantDiscountTotal) { this.variantDiscountTotal = variantDiscountTotal; }
    public BigDecimal getMemberDiscountTotal() { return memberDiscountTotal; }
    public void setMemberDiscountTotal(BigDecimal memberDiscountTotal) { this.memberDiscountTotal = memberDiscountTotal; }
    public BigDecimal getCouponDiscountTotal() { return couponDiscountTotal; }
    public void setCouponDiscountTotal(BigDecimal couponDiscountTotal) { this.couponDiscountTotal = couponDiscountTotal; }
    public BigDecimal getShippingFee() { return shippingFee; }
    public void setShippingFee(BigDecimal shippingFee) { this.shippingFee = shippingFee; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public Boolean getSpendingRecorded() { return spendingRecorded; }
    public void setSpendingRecorded(Boolean spendingRecorded) { this.spendingRecorded = spendingRecorded; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}