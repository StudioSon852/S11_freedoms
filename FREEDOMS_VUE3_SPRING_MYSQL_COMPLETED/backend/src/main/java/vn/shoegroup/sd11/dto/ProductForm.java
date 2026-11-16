package vn.shoegroup.sd11.dto;
import java.math.BigDecimal;
public class ProductForm {
    private Long categoryId; private Long brandId; private String productName; private String parentSku; private String description; private String imageUrl;
    private String sku; private Long sizeId; private Long colorId; private BigDecimal unitPrice; private Integer stockQuantity;
    private boolean featured;
    public Long getCategoryId(){return categoryId;} public void setCategoryId(Long v){categoryId=v;}
    public Long getBrandId(){return brandId;} public void setBrandId(Long v){brandId=v;}
    public String getProductName(){return productName;} public void setProductName(String v){productName=v;}
    public String getParentSku(){return parentSku;} public void setParentSku(String v){parentSku=v;}
    public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public String getImageUrl(){return imageUrl;} public void setImageUrl(String v){imageUrl=v;}
    public String getSku(){return sku;} public void setSku(String v){sku=v;}
    public Long getSizeId(){return sizeId;} public void setSizeId(Long v){sizeId=v;}
    public Long getColorId(){return colorId;} public void setColorId(Long v){colorId=v;}
    public BigDecimal getUnitPrice(){return unitPrice;} public void setUnitPrice(BigDecimal v){unitPrice=v;}
    public Integer getStockQuantity(){return stockQuantity;} public void setStockQuantity(Integer v){stockQuantity=v;}
    public boolean isFeatured(){return featured;} public void setFeatured(boolean v){featured=v;}
}
