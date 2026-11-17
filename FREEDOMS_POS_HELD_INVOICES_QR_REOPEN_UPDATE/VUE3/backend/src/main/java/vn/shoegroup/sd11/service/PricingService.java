package vn.shoegroup.sd11.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.shoegroup.sd11.dto.CartEntry;
import vn.shoegroup.sd11.dto.PricedLine;
import vn.shoegroup.sd11.dto.PricingResult;
import vn.shoegroup.sd11.entity.*;
import vn.shoegroup.sd11.repository.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class PricingService {
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private final ProductVariantRepository variants;
    private final VariantDiscountRepository variantDiscounts;
    private final CouponRepository coupons;
    public PricingService(ProductVariantRepository variants, VariantDiscountRepository variantDiscounts, CouponRepository coupons) {
        this.variants = variants; this.variantDiscounts = variantDiscounts; this.coupons = coupons;
    }

    @Transactional
    public PricingResult calculate(List<CartEntry> entries, AppUser customer, String channel, String couponCode, boolean lockStock) {
        if (entries == null || entries.isEmpty()) throw new IllegalArgumentException("Giỏ hàng chưa có sản phẩm.");
        String discountChannel = "POS".equalsIgnoreCase(channel) ? "OFFLINE" : channel;
        List<CartEntry> orderedEntries = new ArrayList<>(entries);
        // Acquire inventory locks in a stable order to reduce deadlocks between overlapping carts.
        orderedEntries.sort((a, b) -> Long.compare(
                a.variantId() == null ? Long.MIN_VALUE : a.variantId(),
                b.variantId() == null ? Long.MIN_VALUE : b.variantId()));
        List<RawLine> raw = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal variantTotal = BigDecimal.ZERO;
        BigDecimal memberTotal = BigDecimal.ZERO;
        LocalDateTime now = LocalDateTime.now();
        BigDecimal memberPercent = customer != null && customer.getTier() != null
                ? safe(customer.getTier().getDiscountPercent()) : BigDecimal.ZERO;

        for (CartEntry entry : orderedEntries) {
            if (entry.variantId() == null || entry.quantity() == null || entry.quantity() < 1) {
                throw new IllegalArgumentException("Số lượng sản phẩm không hợp lệ.");
            }
            ProductVariant variant = (lockStock ? variants.findByIdForUpdate(entry.variantId()) : variants.findById(entry.variantId()))
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy biến thể sản phẩm #" + entry.variantId()));
            if (!Boolean.TRUE.equals(variant.getActive()) || !Boolean.TRUE.equals(variant.getProduct().getActive()))
                throw new IllegalArgumentException("Sản phẩm đã ngừng kinh doanh: " + variant.getSku());
            if (variant.getStockQuantity() < entry.quantity())
                throw new IllegalArgumentException("Không đủ tồn kho cho SKU " + variant.getSku() + ". Còn " + variant.getStockQuantity());

            BigDecimal gross = money(variant.getUnitPrice().multiply(BigDecimal.valueOf(entry.quantity())));
            BigDecimal variantDiscount = findVariantDiscount(variant, discountChannel, now, entry.quantity());
            BigDecimal afterVariant = gross.subtract(variantDiscount);
            BigDecimal memberDiscount = money(afterVariant.multiply(memberPercent).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP));
            subtotal = subtotal.add(gross);
            variantTotal = variantTotal.add(variantDiscount);
            memberTotal = memberTotal.add(memberDiscount);
            raw.add(new RawLine(variant, entry.quantity(), gross, variantDiscount, memberDiscount, afterVariant.subtract(memberDiscount)));
        }

        BigDecimal afterTier = subtotal.subtract(variantTotal).subtract(memberTotal);
        Coupon coupon = validateCoupon(couponCode, discountChannel, afterTier, now);
        BigDecimal couponTotal = BigDecimal.ZERO;
        if (coupon != null) {
            if ("PERCENT".equalsIgnoreCase(coupon.getDiscountType())) {
                couponTotal = afterTier.multiply(safe(coupon.getDiscountValue())).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
            } else {
                couponTotal = safe(coupon.getDiscountValue());
            }
            if (coupon.getMaxDiscount() != null) couponTotal = couponTotal.min(coupon.getMaxDiscount());
            couponTotal = money(couponTotal.min(afterTier));
        }

        List<PricedLine> resultLines = new ArrayList<>();
        BigDecimal allocated = BigDecimal.ZERO;
        for (int i = 0; i < raw.size(); i++) {
            RawLine line = raw.get(i);
            BigDecimal couponShare = BigDecimal.ZERO;
            if (couponTotal.signum() > 0 && afterTier.signum() > 0) {
                BigDecimal remainingCoupon = couponTotal.subtract(allocated).max(BigDecimal.ZERO);
                if (i == raw.size() - 1) couponShare = remainingCoupon;
                else couponShare = money(couponTotal.multiply(line.afterTier()).divide(afterTier, 2, RoundingMode.HALF_UP))
                        .max(BigDecimal.ZERO).min(remainingCoupon);
                allocated = allocated.add(couponShare);
            }
            BigDecimal lineTotal = line.afterTier().subtract(couponShare).max(BigDecimal.ZERO);
            resultLines.add(new PricedLine(line.variant(), line.quantity(), line.gross(), line.variantDiscount(),
                    line.memberDiscount(), couponShare, money(lineTotal)));
        }
        BigDecimal total = money(afterTier.subtract(couponTotal).max(BigDecimal.ZERO));
        return new PricingResult(resultLines, money(subtotal), money(variantTotal), money(memberTotal),
                money(couponTotal), total, coupon);
    }

    @Transactional
    public void consumeCoupon(Coupon coupon) {
        if (coupon == null) return;
        coupon.setUsageCount(coupon.getUsageCount() + 1);
        coupons.save(coupon);
    }

    @Transactional
    public void releaseCoupon(String couponCode) {
        if (couponCode == null || couponCode.isBlank()) return;
        coupons.findByCodeForUpdate(couponCode.trim()).ifPresent(coupon -> {
            if (coupon.getUsageCount() > 0) {
                coupon.setUsageCount(coupon.getUsageCount() - 1);
                coupons.save(coupon);
            }
        });
    }

    @Transactional(readOnly = true)
    public VariantDiscount getCurrentVariantDiscount(ProductVariant variant, String channel) {
        LocalDateTime now = LocalDateTime.now();
        String normalized = "POS".equalsIgnoreCase(channel) ? "OFFLINE" : channel;
        return variantDiscounts.findByProductVariant_IdAndActiveTrueOrderByIdDesc(variant.getId()).stream()
                .filter(d -> "BOTH".equalsIgnoreCase(d.getSalesChannel()) || normalized.equalsIgnoreCase(d.getSalesChannel()))
                .filter(d -> d.getStartsAt() == null || !d.getStartsAt().isAfter(now))
                .filter(d -> d.getEndsAt() == null || !d.getEndsAt().isBefore(now))
                .findFirst().orElse(null);
    }

    public BigDecimal getCurrentDiscountPerUnit(ProductVariant variant, String channel) {
        VariantDiscount discount = getCurrentVariantDiscount(variant, channel);
        if (discount == null) return BigDecimal.ZERO;
        BigDecimal value = safe(discount.getDiscountValue());
        BigDecimal perUnit = "PERCENT".equalsIgnoreCase(discount.getDiscountType())
                ? variant.getUnitPrice().multiply(value).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP)
                : value;
        return money(perUnit.min(variant.getUnitPrice()));
    }

    private BigDecimal findVariantDiscount(ProductVariant variant, String channel, LocalDateTime now, int quantity) {
        return variantDiscounts.findByProductVariant_IdAndActiveTrueOrderByIdDesc(variant.getId()).stream()
                .filter(d -> "BOTH".equalsIgnoreCase(d.getSalesChannel()) || channel.equalsIgnoreCase(d.getSalesChannel()))
                .filter(d -> d.getStartsAt() == null || !d.getStartsAt().isAfter(now))
                .filter(d -> d.getEndsAt() == null || !d.getEndsAt().isBefore(now))
                .findFirst().map(d -> {
                    BigDecimal perUnit;
                    if ("PERCENT".equalsIgnoreCase(d.getDiscountType())) perUnit = variant.getUnitPrice().multiply(safe(d.getDiscountValue())).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
                    else perUnit = safe(d.getDiscountValue());
                    return money(perUnit.min(variant.getUnitPrice()).multiply(BigDecimal.valueOf(quantity)));
                }).orElse(BigDecimal.ZERO);
    }

    private Coupon validateCoupon(String code, String channel, BigDecimal amount, LocalDateTime now) {
        if (code == null || code.isBlank()) return null;
        Coupon coupon = coupons.findByCodeForUpdate(code.trim())
                .orElseThrow(() -> new IllegalArgumentException("Mã giảm giá không tồn tại."));
        if (!Boolean.TRUE.equals(coupon.getActive())) throw new IllegalArgumentException("Mã giảm giá đã ngừng hoạt động.");
        if (!"BOTH".equalsIgnoreCase(coupon.getSalesChannel()) && !channel.equalsIgnoreCase(coupon.getSalesChannel()))
            throw new IllegalArgumentException("Mã giảm giá không áp dụng cho kênh " + channel + ".");
        if (coupon.getStartsAt() != null && coupon.getStartsAt().isAfter(now)) throw new IllegalArgumentException("Mã giảm giá chưa đến thời gian sử dụng.");
        if (coupon.getEndsAt() != null && coupon.getEndsAt().isBefore(now)) throw new IllegalArgumentException("Mã giảm giá đã hết hạn.");
        if (coupon.getUsageLimit() != null && coupon.getUsageCount() >= coupon.getUsageLimit()) throw new IllegalArgumentException("Mã giảm giá đã hết lượt sử dụng.");
        if (amount.compareTo(safe(coupon.getMinOrderAmount())) < 0) throw new IllegalArgumentException("Đơn hàng chưa đạt giá trị tối thiểu của mã giảm giá.");
        return coupon;
    }

    private BigDecimal safe(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private BigDecimal money(BigDecimal value) { return value.setScale(0, RoundingMode.HALF_UP); }
    private record RawLine(ProductVariant variant, int quantity, BigDecimal gross, BigDecimal variantDiscount, BigDecimal memberDiscount, BigDecimal afterTier) {}
}
