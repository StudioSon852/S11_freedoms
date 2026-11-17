package vn.shoegroup.sd11.dto;
import vn.shoegroup.sd11.entity.ProductVariant;
import java.math.BigDecimal;
public record PricedLine(ProductVariant variant, int quantity, BigDecimal gross,
                         BigDecimal variantDiscount, BigDecimal memberDiscount,
                         BigDecimal couponDiscount, BigDecimal lineTotal) {}
