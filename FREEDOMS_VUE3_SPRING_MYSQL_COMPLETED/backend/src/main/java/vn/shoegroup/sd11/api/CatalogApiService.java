package vn.shoegroup.sd11.api;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.shoegroup.sd11.entity.*;
import vn.shoegroup.sd11.repository.*;
import vn.shoegroup.sd11.service.PricingService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import static vn.shoegroup.sd11.api.ApiDtos.*;

@Service
public class CatalogApiService {
    private final ProductRepository products;
    private final ProductVariantRepository variants;
    private final CouponRepository coupons;
    private final PricingService pricing;
    private final ProductVariantImageRepository images;
    public CatalogApiService(ProductRepository products, ProductVariantRepository variants,
                             CouponRepository coupons, PricingService pricing, ProductVariantImageRepository images) {
        this.products = products; this.variants = variants; this.coupons = coupons; this.pricing = pricing; this.images = images;
    }

    @Transactional(readOnly = true)
    public List<ProductView> products(String q, String channel, boolean salesOnly, boolean includeInactive) {
        String query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        List<Product> source = includeInactive ? products.findAll() : products.searchActive(query);
        if (includeInactive && !query.isBlank()) source = source.stream().filter(p ->
                contains(p.getProductName(), query) || contains(p.getParentSku(), query)
                        || contains(p.getCategory() == null ? null : p.getCategory().getCategoryName(), query)
                        || contains(p.getBrand() == null ? null : p.getBrand().getBrandName(), query)).toList();
        return source.stream().sorted(Comparator.comparing(Product::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(p -> mapProduct(p, channel, salesOnly, includeInactive)).filter(Objects::nonNull).toList();
    }

    @Transactional(readOnly = true)
    public ProductView product(Long id, String channel, boolean includeInactive) {
        Product p = products.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm."));
        if (!includeInactive && !Boolean.TRUE.equals(p.getActive())) throw new IllegalArgumentException("Sản phẩm đã ngừng kinh doanh.");
        return mapProduct(p, channel, false, includeInactive);
    }

    private ProductView mapProduct(Product p, String channel, boolean salesOnly, boolean includeInactive) {
        if (!includeInactive && !Boolean.TRUE.equals(p.getActive())) return null;
        List<ProductVariant> source = includeInactive ? variants.findByProduct_IdOrderByIdAsc(p.getId())
                : variants.findByProduct_IdAndActiveTrueOrderByIdAsc(p.getId());
        List<VariantView> vws = source.stream().map(v -> {
            BigDecimal discount = pricing.getCurrentDiscountPerUnit(v, channel);
            boolean sale = discount.signum() > 0;
            return new VariantView(v.getId(), v.getSku(), v.getSize() == null ? "" : v.getSize().getSizeLabel(),
                    v.getColor() == null ? "" : v.getColor().getColorName(), v.getSize() == null ? null : v.getSize().getId(), v.getColor() == null ? null : v.getColor().getId(), v.getUnitPrice(), discount,
                    v.getUnitPrice().subtract(discount).max(BigDecimal.ZERO), sale,
                    v.getStockQuantity() == null ? 0 : v.getStockQuantity(), Boolean.TRUE.equals(v.getActive()),
                    images.findByVariant_IdOrderBySortOrderAscIdAsc(v.getId()).stream().map(i -> new VariantImageView(i.getId(), i.getImageUrl())).toList());
        }).filter(v -> includeInactive || v.active()).toList();
        boolean hasSale = vws.stream().anyMatch(VariantView::hasSale);
        if (salesOnly && !hasSale) return null;
        if (!includeInactive && vws.isEmpty()) return null;
        return new ProductView(p.getId(), p.getProductName(), p.getParentSku(), p.getDescription(), p.getImageUrl(),
                p.getCategory() == null ? "" : p.getCategory().getCategoryName(),
                p.getBrand() == null ? "" : p.getBrand().getBrandName(), Boolean.TRUE.equals(p.getFeatured()),
                Boolean.TRUE.equals(p.getActive()), hasSale, vws);
    }

    @Transactional(readOnly = true)
    public List<CouponView> activeCoupons(String channel) {
        String ch = channel == null ? "ONLINE" : channel.toUpperCase(Locale.ROOT);
        LocalDateTime now = LocalDateTime.now();
        return coupons.findAllByOrderByIdDesc().stream()
                .filter(c -> Boolean.TRUE.equals(c.getActive()))
                .filter(c -> "BOTH".equalsIgnoreCase(c.getSalesChannel()) || ch.equalsIgnoreCase(c.getSalesChannel()))
                .filter(c -> c.getStartsAt() == null || !c.getStartsAt().isAfter(now))
                .filter(c -> c.getEndsAt() == null || !c.getEndsAt().isBefore(now))
                .filter(c -> c.getUsageLimit() == null || c.getUsageCount() < c.getUsageLimit())
                .map(this::mapCoupon).toList();
    }

    public CouponView mapCoupon(Coupon c) {
        return new CouponView(c.getId(), c.getCouponCode(), c.getSalesChannel(), c.getDiscountType(),
                c.getDiscountValue(), c.getMinOrderAmount(), c.getMaxDiscount(), c.getUsageLimit(),
                c.getUsageCount(), c.getStartsAt(), c.getEndsAt(), Boolean.TRUE.equals(c.getActive()));
    }

    private boolean contains(String value, String query) { return value != null && value.toLowerCase(Locale.ROOT).contains(query); }
}
