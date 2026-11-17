package vn.shoegroup.sd11.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(name = "held_invoice_items")
public class HeldInvoiceItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="held_invoice_item_id")
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="held_invoice_id", nullable=false)
    private HeldInvoice heldInvoice;

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

    public HeldInvoiceItem() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public HeldInvoice getHeldInvoice() { return heldInvoice; }
    public void setHeldInvoice(HeldInvoice heldInvoice) { this.heldInvoice = heldInvoice; }
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
}