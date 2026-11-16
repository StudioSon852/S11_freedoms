package vn.shoegroup.sd11.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class ApiDtos {
    private ApiDtos() {}
    public record ApiUser(Long id, String email, String fullName, String role, String tier,
                          BigDecimal totalSpent, BigDecimal tierDiscountPercent) {}
    public record VariantImageView(Long id, String imageUrl) {}
    public record VariantView(Long id, String sku, String size, String color, Long sizeId, Long colorId, BigDecimal unitPrice,
                              BigDecimal discountPerUnit, BigDecimal salePrice, boolean hasSale,
                              int stockQuantity, boolean active, List<VariantImageView> images) {}
    public record ProductView(Long id, String name, String sku, String description, String imageUrl,
                              String category, String brand, boolean featured, boolean active,
                              boolean hasSale, List<VariantView> variants) {}
    public record CouponView(Long id, String code, String salesChannel, String discountType,
                             BigDecimal discountValue, BigDecimal minOrderAmount, BigDecimal maxDiscount,
                             Integer usageLimit, Integer usageCount, LocalDateTime startsAt,
                             LocalDateTime endsAt, boolean active) {}
    public record CartLineInput(Long variantId, Integer quantity) {}
    public record PosSessionCreateRequest(String customerName, String customerPhone) {}
    public record PosSessionUpdateRequest(String customerName, String customerPhone, String note,
                                          String couponCode, List<CartLineInput> items) {}
    public record PosCheckoutRequest(String paymentMethod) {}
    public record HeldItemView(Long variantId, String productName, String variantLabel, String sku,
                               BigDecimal unitPrice, int quantity, BigDecimal lineTotal,
                               int currentStock) {}
    public record HeldSessionView(Long id, String code, String customerName, String customerPhone,
                                  String note, String couponCode, String status, int itemCount,
                                  BigDecimal subtotal, List<HeldItemView> items,
                                  LocalDateTime createdAt, LocalDateTime updatedAt) {}
    public record ProductCreateRequest(String productName, String parentSku, String description,
                                       String imageUrl, Long categoryId, Long brandId,
                                       String sku, Long sizeId, Long colorId,
                                       BigDecimal unitPrice, Integer stockQuantity, Boolean featured) {}
    public record ProductUpdateRequest(String productName, String description, String imageUrl,
                                       Long categoryId, Long brandId, Boolean active, Boolean featured) {}
    public record VariantCreateRequest(String sku, Long sizeId, Long colorId,
                                       BigDecimal unitPrice, Integer stockQuantity) {}
    public record VariantUpdateRequest(String sku, Long sizeId, Long colorId, BigDecimal unitPrice, Integer stockQuantity, Boolean active) {}
    public record CouponSaveRequest(String couponCode, String salesChannel, String discountType,
                                    BigDecimal discountValue, BigDecimal minOrderAmount,
                                    BigDecimal maxDiscount, Integer usageLimit,
                                    LocalDateTime startsAt, LocalDateTime endsAt, Boolean active) {}
    public record VariantDiscountSaveRequest(Long variantId, String salesChannel, String discountType,
                                             BigDecimal discountValue, LocalDateTime startsAt,
                                             LocalDateTime endsAt, String reason, Boolean active) {}
    public record CheckoutRequest(List<CartLineInput> items, String couponCode, String paymentMethod,
                                  String customerName, String customerPhone, String shippingAddress,
                                  String province, Integer toDistrictId, String toWardCode,
                                  String note, Integer weightGram) {}
    public record ShippingQuoteRequest(String province, Integer toDistrictId, String toWardCode,
                                       Integer weightGram, List<CartLineInput> items, String couponCode) {}
    public record OrderItemView(String productName, String variantLabel, String sku,
                                BigDecimal unitPrice, int quantity, BigDecimal lineTotal) {}
    public record RefundEligibilityView(boolean eligible, BigDecimal remainingAmount, int requestsUsed,
                                        int requestsRemaining, LocalDateTime deadline, int windowHours,
                                        String message) {}
    public record OrderView(Long id, String code, String salesChannel, String status,
                            String customerName, String customerPhone, String shippingAddress,
                            BigDecimal subtotal, BigDecimal variantDiscount, BigDecimal memberDiscount,
                            BigDecimal couponDiscount, BigDecimal shippingFee, BigDecimal totalAmount,
                            String paymentMethod, String paymentStatus, LocalDateTime createdAt,
                            List<OrderItemView> items, RefundEligibilityView refundEligibility) {}
    public record RefundView(Long id, Long orderId, String orderCode, String customerName,
                             String customerPhone, BigDecimal amount, String method, String status,
                             String reason, String reference, LocalDateTime requestedAt) {}
    public record RefundRequest(BigDecimal amount, Boolean fullRefund, String reason) {}
    public record ShippingQuoteView(BigDecimal fee, String source, String message) {}
    public record UserView(Long id, String email, String fullName, String phone, String role, boolean active) {}
    public record UserCreateRequest(String email, String password, String fullName, String phone, String roleCode) {}
    public record ProfileUpdateRequest(String fullName, String phone, String defaultAddress) {}
    public record CustomerSummaryView(Long id, String fullName, String email, String phone,
                                      String tierCode, String tierName, BigDecimal totalSpent,
                                      BigDecimal discountPercent, String defaultAddress,
                                      LocalDateTime createdAt) {}
    public record CustomerDetailView(CustomerSummaryView customer, long orderCount,
                                     List<OrderView> recentOrders) {}
    public record MessageView(boolean success, String message) {}
}
