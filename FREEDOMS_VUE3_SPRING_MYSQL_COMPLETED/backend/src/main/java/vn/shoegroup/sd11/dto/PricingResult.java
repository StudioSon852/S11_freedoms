package vn.shoegroup.sd11.dto;
import vn.shoegroup.sd11.entity.Coupon;
import java.math.BigDecimal;
import java.util.List;
public record PricingResult(List<PricedLine> lines, BigDecimal subtotal,
        BigDecimal variantDiscountTotal, BigDecimal memberDiscountTotal,
        BigDecimal couponDiscountTotal, BigDecimal total, Coupon coupon) {}
