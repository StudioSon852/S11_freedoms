package vn.shoegroup.sd11.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(name = "product_variants")
public class ProductVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="variant_id")
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="product_id", nullable=false)
    private Product product;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="size_id")
    private Size size;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="color_id")
    private Color color;

    @Column(name="sku", nullable=false, unique=true, length=80)
    private String sku;

    @Column(name="unit_price", nullable=false, precision=15, scale=2)
    private BigDecimal unitPrice;

    @Column(name="stock_quantity", nullable=false)
    private Integer stockQuantity;

    @Column(name="is_active", nullable=false)
    private Boolean active;

    public ProductVariant() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public Size getSize() { return size; }
    public void setSize(Size size) { this.size = size; }
    public Color getColor() { return color; }
    public void setColor(Color color) { this.color = color; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}