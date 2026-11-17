package vn.shoegroup.sd11.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(name = "held_invoices")
public class HeldInvoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="held_invoice_id")
    private Long id;

    @Column(name="held_code", nullable=false, unique=true, length=40)
    private String heldCode;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="created_by", nullable=false)
    private AppUser createdBy;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="customer_id")
    private AppUser customer;

    @Column(name="customer_name", nullable=false, length=150)
    private String customerName;

    @Column(name="customer_phone", length=30)
    private String customerPhone;

    @Column(name="customer_note", length=500)
    private String customerNote;

    @Column(name="coupon_code", length=60)
    private String couponCode;

    @Column(name="status", nullable=false, length=20)
    private String status;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="converted_order_id")
    private CustomerOrder convertedOrder;

    @Column(name="created_at", nullable=false)
    private LocalDateTime createdAt;

    @Column(name="updated_at", nullable=false)
    private LocalDateTime updatedAt;

    public HeldInvoice() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getHeldCode() { return heldCode; }
    public void setHeldCode(String heldCode) { this.heldCode = heldCode; }
    public AppUser getCreatedBy() { return createdBy; }
    public void setCreatedBy(AppUser createdBy) { this.createdBy = createdBy; }
    public AppUser getCustomer() { return customer; }
    public void setCustomer(AppUser customer) { this.customer = customer; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public String getCustomerNote() { return customerNote; }
    public void setCustomerNote(String customerNote) { this.customerNote = customerNote; }
    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public CustomerOrder getConvertedOrder() { return convertedOrder; }
    public void setConvertedOrder(CustomerOrder convertedOrder) { this.convertedOrder = convertedOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}