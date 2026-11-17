package vn.shoegroup.sd11.api;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.shoegroup.sd11.config.CustomUserDetails;
import vn.shoegroup.sd11.dto.CartEntry;
import vn.shoegroup.sd11.entity.*;
import vn.shoegroup.sd11.repository.*;
import vn.shoegroup.sd11.service.OrderService;
import vn.shoegroup.sd11.service.RefundService;
import vn.shoegroup.sd11.service.PricingService;
import vn.shoegroup.sd11.shipping.ShippingService;
import java.math.BigDecimal;
import java.util.*;
import static vn.shoegroup.sd11.api.ApiDtos.*;

@RestController
public class ApiCheckoutController {
    private final PricingService pricing;
    private final ShippingService shipping;
    private final OrderService orders;
    private final CustomerOrderRepository orderRepo;
    private final OrderItemRepository orderItems;
    private final PaymentRepository payments;
    private final OrderStatusHistoryRepository histories;
    private final RefundService refundService;
    private final UserRepository users;
    @org.springframework.beans.factory.annotation.Value("${app.payment.bank-code:MB}") private String bankCode;
    @org.springframework.beans.factory.annotation.Value("${app.payment.bank-account:}") private String bankAccount;
    @org.springframework.beans.factory.annotation.Value("${app.payment.bank-account-name:FREEDOMS SHOE STORE}") private String bankAccountName;

    public ApiCheckoutController(PricingService pricing, ShippingService shipping, OrderService orders,
            CustomerOrderRepository orderRepo, OrderItemRepository orderItems, PaymentRepository payments,
            OrderStatusHistoryRepository histories, RefundService refundService, UserRepository users) {
        this.pricing=pricing; this.shipping=shipping; this.orders=orders; this.orderRepo=orderRepo;
        this.orderItems=orderItems; this.payments=payments; this.histories=histories; this.refundService=refundService; this.users=users;
    }

    @PostMapping("/api/checkout/preview")
    public Map<String,Object> preview(@RequestBody CheckoutRequest request, @AuthenticationPrincipal CustomUserDetails principal) {
        AppUser customer = latestCustomer(principal);
        List<CartEntry> entries = toEntries(request.items());
        var priced = pricing.calculate(entries, customer, "ONLINE", request.couponCode(), false);
        int weight = weight(request.weightGram(), entries);
        var quote = shipping.calculate("ONLINE", request.province(), request.toDistrictId(), request.toWardCode(), weight, priced.total());
        return Map.of("subtotal", priced.subtotal(), "variantDiscount", priced.variantDiscountTotal(),
                "memberDiscount", priced.memberDiscountTotal(), "couponDiscount", priced.couponDiscountTotal(),
                "shippingFee", quote.fee(), "shippingSource", quote.source(), "shippingMessage", quote.message(),
                "total", priced.total().add(quote.fee()), "lines", priced.lines().stream().map(l -> Map.of(
                        "variantId", l.variant().getId(), "name", l.variant().getProduct().getProductName(),
                        "sku", l.variant().getSku(), "quantity", l.quantity(), "lineTotal", l.lineTotal())).toList());
    }

    @PostMapping("/api/checkout")
    public Map<String,Object> checkout(@RequestBody CheckoutRequest request, @AuthenticationPrincipal CustomUserDetails principal) {
        AppUser customer = latestCustomer(principal);
        List<CartEntry> entries = toEntries(request.items());
        if (entries.isEmpty()) throw new IllegalArgumentException("Bạn chưa chọn sản phẩm nào để thanh toán.");
        if (request.shippingAddress() == null || request.shippingAddress().isBlank())
            throw new IllegalArgumentException("Vui lòng nhập địa chỉ nhận hàng.");
        var priced = pricing.calculate(entries, customer, "ONLINE", request.couponCode(), false);
        var quote = shipping.calculate("ONLINE", request.province(), request.toDistrictId(), request.toWardCode(),
                weight(request.weightGram(), entries), priced.total());
        String address = request.shippingAddress().trim();
        if (request.province() != null && !request.province().isBlank()) {
            String province = request.province().trim();
            if (!address.toLowerCase(Locale.ROOT).endsWith(province.toLowerCase(Locale.ROOT))) address += ", " + province;
        }
        String method = request.paymentMethod() == null ? "BANK_TRANSFER" : request.paymentMethod().toUpperCase(Locale.ROOT);
        if (!List.of("BANK_TRANSFER", "COD").contains(method)) throw new IllegalArgumentException("Phương thức thanh toán online không hợp lệ.");
        CustomerOrder order = orders.createOrder(entries, customer, null, "ONLINE", method,
                request.customerName(), request.customerPhone(), address, request.note(), request.couponCode(), quote.fee());
        Payment payment = payments.findFirstByOrder_IdOrderByCreatedAtDesc(order.getId()).orElseThrow();
        String qrUrl = "";
        if ("BANK_TRANSFER".equals(payment.getPaymentMethod()) && bankAccount != null && !bankAccount.isBlank()) {
            qrUrl = "https://img.vietqr.io/image/" + bankCode + "-" + bankAccount + "-compact2.png?amount="
                    + order.getTotalAmount().longValue() + "&addInfo=" + java.net.URLEncoder.encode(order.getOrderCode(), java.nio.charset.StandardCharsets.UTF_8)
                    + "&accountName=" + java.net.URLEncoder.encode(bankAccountName, java.nio.charset.StandardCharsets.UTF_8);
        }
        return Map.of("orderId", order.getId(), "orderCode", order.getOrderCode(), "orderStatus", order.getOrderStatus(),
                "paymentId", payment.getId(), "paymentStatus", payment.getPaymentStatus(),
                "paymentMethod", payment.getPaymentMethod(), "shippingFee", order.getShippingFee(), "total", order.getTotalAmount(), "qrUrl", qrUrl);
    }

    @GetMapping("/api/orders/mine")
    public List<OrderView> mine(@AuthenticationPrincipal CustomUserDetails principal) {
        return orderRepo.findByCustomer_IdOrderByCreatedAtDesc(principal.getUser().getId()).stream().map(this::mapOrder).toList();
    }

    @GetMapping("/api/orders/{id}")
    public OrderView detail(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        CustomerOrder order = orderRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));
        if (order.getCustomer() == null || !order.getCustomer().getId().equals(principal.getUser().getId()))
            throw new IllegalArgumentException("Bạn không có quyền xem đơn hàng này.");
        return mapOrder(order);
    }

    @PostMapping("/api/orders/{id}/transfer-submitted")
    public MessageView transferSubmitted(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        orders.markTransferSubmitted(id, principal.getUser());
        return new MessageView(true, "Đã gửi thông báo chuyển khoản. Đơn đang chờ cửa hàng đối soát.");
    }

    @PostMapping("/api/orders/{id}/cancel")
    public MessageView cancel(@PathVariable Long id, @RequestBody(required = false) Map<String,String> body,
                              @AuthenticationPrincipal CustomUserDetails principal) {
        orders.cancelCustomerOrder(id, principal.getUser());
        return new MessageView(true, "Đã hủy đơn. Nếu đã thanh toán, yêu cầu hoàn tiền sẽ được cửa hàng xử lý.");
    }

    @PostMapping("/api/orders/{id}/refund-request")
    public RefundView requestRefund(@PathVariable Long id, @RequestBody RefundRequest request,
                                    @AuthenticationPrincipal CustomUserDetails principal) {
        var refund = orders.requestRefund(id, principal.getUser(), request == null ? null : request.amount(),
                request != null && Boolean.TRUE.equals(request.fullRefund()), request == null ? null : request.reason());
        return new RefundView(refund.getId(), refund.getOrder().getId(), refund.getOrder().getOrderCode(),
                refund.getOrder().getCustomerName(), refund.getOrder().getCustomerPhone(), refund.getAmount(),
                refund.getRefundMethod(), refund.getStatus(), refund.getReason(), refund.getTransactionReference(), refund.getRequestedAt());
    }

    private OrderView mapOrder(CustomerOrder o) {
        Payment p = payments.findFirstByOrder_IdOrderByCreatedAtDesc(o.getId()).orElse(null);
        List<OrderItemView> items = orderItems.findByOrder_IdOrderByIdAsc(o.getId()).stream().map(i ->
                new OrderItemView(i.getProductNameSnapshot(), i.getVariantLabelSnapshot(), i.getSkuSnapshot(),
                        i.getUnitPrice(), i.getQuantity(), i.getLineTotal())).toList();
        return new OrderView(o.getId(), o.getOrderCode(), o.getSalesChannel(), o.getOrderStatus(), o.getCustomerName(),
                o.getCustomerPhone(), o.getShippingAddress(), o.getSubtotal(), o.getVariantDiscountTotal(),
                o.getMemberDiscountTotal(), o.getCouponDiscountTotal(), o.getShippingFee(), o.getTotalAmount(),
                p == null ? "" : p.getPaymentMethod(), p == null ? "" : p.getPaymentStatus(), o.getCreatedAt(), items,
                refundService.eligibility(o, p));
    }

    private AppUser latestCustomer(CustomUserDetails principal) {
        if (principal == null || principal.getUser() == null) throw new IllegalArgumentException("Vui lòng đăng nhập.");
        return users.findById(principal.getUser().getId())
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản khách hàng không còn tồn tại."));
    }

    private List<CartEntry> toEntries(List<CartLineInput> items) {
        if (items == null) return List.of();
        return items.stream().map(i -> new CartEntry(i.variantId(), i.quantity())).toList();
    }
    private int weight(Integer requested, List<CartEntry> entries) {
        if (requested != null && requested > 0) return requested;
        return entries.stream().mapToInt(i -> i.quantity() * 800).sum();
    }

}
