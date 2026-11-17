package vn.shoegroup.sd11.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="product_id")
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="category_id")
    private Category category;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="brand_id")
    private Brand brand;

    @Column(name="product_name", nullable=false, length=180)
    private String productName;

    @Column(name="parent_sku", unique=true, length=60)
    private String parentSku;

    @Column(name="description", columnDefinition="TEXT")
    private String description;

    @Column(name="image_url", length=500)
    private String imageUrl;

    @Column(name="is_featured", nullable=false)
    private Boolean featured;

    @Column(name="is_active", nullable=false)
    private Boolean active;

    @Column(name="created_at", nullable=false)
    private LocalDateTime createdAt;

    public Product() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public Brand getBrand() { return brand; }
    public void setBrand(Brand brand) { this.brand = brand; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getParentSku() { return parentSku; }
    public void setParentSku(String parentSku) { this.parentSku = parentSku; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Boolean getFeatured() { return featured; }
    public void setFeatured(Boolean featured) { this.featured = featured; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}