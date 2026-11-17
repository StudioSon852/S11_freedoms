package vn.shoegroup.sd11.api;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.shoegroup.sd11.config.CustomUserDetails;
import vn.shoegroup.sd11.dto.CartEntry;
import vn.shoegroup.sd11.entity.*;
import vn.shoegroup.sd11.repository.*;
import vn.shoegroup.sd11.service.HeldInvoiceService;
import vn.shoegroup.sd11.service.OrderService;
import vn.shoegroup.sd11.service.RefundService;
import java.math.BigDecimal;
import java.util.*;
import java.time.LocalDate;
import java.util.Locale;
import static vn.shoegroup.sd11.api.ApiDtos.*;

@RestController
@RequestMapping("/api/pos")
public class ApiPosController {
    private final CatalogApiService catalog;
    private final HeldInvoiceService held;
    private final PaymentRepository payments;
    private final CustomerOrderRepository orders;
    private final HeldInvoiceItemRepository heldItems;
    private final HeldInvoiceRepository heldRepo;
    private final vn.shoegroup.sd11.service.PricingService pricing;
    private final OrderService orderService;
    private final RefundService refundService;
    private final UserRepository users;
    public ApiPosController(CatalogApiService catalog, HeldInvoiceService held, PaymentRepository payments,
            CustomerOrderRepository orders, HeldInvoiceItemRepository heldItems, HeldInvoiceRepository heldRepo,
            vn.shoegroup.sd11.service.PricingService pricing, OrderService orderService, RefundService refundService, UserRepository users) {
        this.catalog=catalog; this.held=held; this.payments=payments; this.orders=orders; this.heldItems=heldItems; this.heldRepo=heldRepo; this.pricing=pricing; this.orderService=orderService; this.refundService=refundService; this.users=users;
    }

    @GetMapping("/products")
    public List<ProductView> products(@RequestParam(defaultValue = "") String q,
                                      @RequestParam(defaultValue = "false") boolean salesOnly) {
        boolean salesSearch = salesOnly || "sales".equalsIgnoreCase(q.trim()) || "sale".equalsIgnoreCase(q.trim());
        String textQuery = salesSearch && ("sales".equalsIgnoreCase(q.trim()) || "sale".equalsIgnoreCase(q.trim())) ? "" : q;
        return catalog.products(textQuery, "OFFLINE", salesSearch, false);
    }

    @GetMapping("/coupons")
    public List<CouponView> coupons() { return catalog.activeCoupons("OFFLINE"); }

    @GetMapping("/sessions")
    public List<HeldSessionView> sessions(@AuthenticationPrincipal CustomUserDetails principal) {
        return held.listFor(principal.getUser()).stream().map(h -> mapSession(h, principal.getUser())).toList();
    }

    @PostMapping("/sessions")
    public HeldSessionView create(@RequestBody(required = false) PosSessionCreateRequest request,
                                  @AuthenticationPrincipal CustomUserDetails principal) {
        String name = request == null ? "Khách lẻ" : request.customerName();
        String phone = request == null ? "" : request.customerPhone();
        return mapSession(held.createDraft(principal.getUser(), name, phone), principal.getUser());
    }

    @PutMapping("/sessions/{id}")
    public HeldSessionView update(@PathVariable Long id, @RequestBody PosSessionUpdateRequest request,
                                  @AuthenticationPrincipal CustomUserDetails principal) {
        List<CartEntry> entries = request.items() == null ? List.of()
                : request.items().stream().map(i -> new CartEntry(i.variantId(), i.quantity())).toList();
        HeldInvoice saved = held.replaceDraft(id, principal.getUser(), request.customerName(), request.customerPhone(),
                request.note(), request.couponCode(), entries);
        return mapSession(saved, principal.getUser());
    }

    @GetMapping("/sessions/{id}/preview")
    public Map<String,Object> preview(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        HeldInvoice invoice = heldRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy quầy khách."));
        String role = principal.getUser().getRole().getRoleCode();
        if (!Set.of("ADMIN", "MANAGER").contains(role) && !invoice.getCreatedBy().getId().equals(principal.getUser().getId()))
            throw new IllegalArgumentException("Bạn không có quyền xem phiên bán hàng này.");
        List<CartEntry> entries = held.getItems(id, principal.getUser()).stream()
                .map(i -> new CartEntry(i.getProductVariant().getId(), i.getQuantity())).toList();
        if (entries.isEmpty()) return Map.of("subtotal", BigDecimal.ZERO, "variantDiscount", BigDecimal.ZERO,
                "memberDiscount", BigDecimal.ZERO, "couponDiscount", BigDecimal.ZERO, "total", BigDecimal.ZERO, "lines", List.of());
        AppUser currentCustomer = invoice.getCustomer() == null ? null : users.findById(invoice.getCustomer().getId()).orElse(null);
        var result = pricing.calculate(entries, currentCustomer, "OFFLINE", invoice.getCouponCode(), false);
        return Map.of("subtotal", result.subtotal(), "variantDiscount", result.variantDiscountTotal(),
                "memberDiscount", result.memberDiscountTotal(), "couponDiscount", result.couponDiscountTotal(),
                "total", result.total(), "lines", result.lines().stream().map(l -> Map.of("variantId", l.variant().getId(),
                        "name", l.variant().getProduct().getProductName(), "sku", l.variant().getSku(),
                        "quantity", l.quantity(), "lineTotal", l.lineTotal(), "variantDiscount", l.variantDiscount(),
                        "memberDiscount", l.memberDiscount(), "couponDiscount", l.couponDiscount())).toList());
    }

    @PostMapping("/sessions/{id}/checkout")
    public Map<String,Object> checkout(@PathVariable Long id, @RequestBody PosCheckoutRequest request,
                                       @AuthenticationPrincipal CustomUserDetails principal) {
        String method = request == null || request.paymentMethod() == null ? "BANK_TRANSFER" : request.paymentMethod().trim().toUpperCase(Locale.ROOT);
        BigDecimal cashReceived = request == null ? null : request.cashReceived();
        if ("CASH".equals(method) && (cashReceived == null || cashReceived.signum() < 0
                || cashReceived.stripTrailingZeros().scale() > 0))
            throw new IllegalArgumentException("Vui lòng nhập số tiền khách đưa là số nguyên VNĐ hợp lệ.");
        CustomerOrder order = held.convert(id, principal.getUser(), method, cashReceived);
        Payment payment = payments.findFirstByOrder_IdOrderByCreatedAtDesc(order.getId()).orElseThrow();
        Map<String,Object> response = new LinkedHashMap<>();
        response.put("orderId", order.getId()); response.put("orderCode", order.getOrderCode());
        response.put("orderStatus", order.getOrderStatus()); response.put("paymentId", payment.getId());
        response.put("paymentMethod", payment.getPaymentMethod()); response.put("paymentStatus", payment.getPaymentStatus());
        response.put("total", order.getTotalAmount());
        if ("CASH".equals(payment.getPaymentMethod()) && cashReceived != null) {
            response.put("cashReceived", cashReceived.setScale(0, java.math.RoundingMode.UNNECESSARY));
            response.put("cashChange", cashReceived.setScale(0, java.math.RoundingMode.UNNECESSARY).subtract(order.getTotalAmount()));
        }
        return response;
    }

    @PostMapping("/sessions/{id}/cancel")
    public MessageView cancel(@PathVariable Long id, @RequestBody(required = false) Map<String,String> body,
                              @AuthenticationPrincipal CustomUserDetails principal) {
        held.cancel(id, principal.getUser(), body == null ? "Đóng quầy khách" : body.get("reason"));
        return new MessageView(true, "Đã đóng quầy khách. Không tạo hóa đơn thanh toán và không trừ kho.");
    }

    @GetMapping("/orders")
    public List<OrderView> orders(@AuthenticationPrincipal CustomUserDetails principal,
                                  @RequestParam(required=false) LocalDate date,
                                  @RequestParam(defaultValue="") String orderCode,
                                  @RequestParam(defaultValue="") String status) {
        String role = principal.getUser().getRole().getRoleCode();
        String code = orderCode.trim().toLowerCase(Locale.ROOT), state = status.trim().toUpperCase(Locale.ROOT);
        return orders.findAllByOrderByCreatedAtDesc().stream()
                .filter(o -> "POS".equals(o.getSalesChannel()))
                .filter(o -> Set.of("ADMIN", "MANAGER").contains(role) || (o.getCreatedBy() != null && o.getCreatedBy().getId().equals(principal.getUser().getId())))
                .filter(o -> date == null || (o.getCreatedAt() != null && date.equals(o.getCreatedAt().toLocalDate())))
                .filter(o -> code.isBlank() || o.getOrderCode().toLowerCase(Locale.ROOT).contains(code))
                .filter(o -> state.isBlank() || state.equalsIgnoreCase(o.getOrderStatus()) || payments.findFirstByOrder_IdOrderByCreatedAtDesc(o.getId()).map(Payment::getPaymentStatus).filter(state::equalsIgnoreCase).isPresent())
                .map(this::mapPosOrder).toList();
    }

    @GetMapping("/customers")
    public List<CustomerSummaryView> customers(@RequestParam(defaultValue="") String q,
                                               @RequestParam(defaultValue="") String tier) {
        String query = q.trim().toLowerCase(Locale.ROOT), tierQuery = tier.trim().toUpperCase(Locale.ROOT);
        return users.findByRole_RoleCodeOrderByIdDesc("CUSTOMER").stream().filter(u -> {
            if (!tierQuery.isBlank() && (u.getTier() == null || !tierQuery.equalsIgnoreCase(u.getTier().getTierCode()))) return false;
            if (query.isBlank()) return true;
            String blob = (Objects.toString(u.getFullName(), "") + " " + Objects.toString(u.getEmail(), "") + " "
                    + Objects.toString(u.getPhone(), "") + " " + Objects.toString(u.getDefaultAddress(), "") + " "
                    + (u.getTier() == null ? "" : u.getTier().getTierName())).toLowerCase(Locale.ROOT);
            return blob.contains(query);
        }).map(this::mapCustomer).toList();
    }

    @GetMapping("/customers/{id}")
    public CustomerDetailView customerDetail(@PathVariable Long id) {
        AppUser customer = users.findById(id).filter(u -> "CUSTOMER".equals(u.getRole().getRoleCode()))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản khách hàng."));
        List<CustomerOrder> customerOrders = orders.findByCustomer_IdOrderByCreatedAtDesc(id);
        return new CustomerDetailView(mapCustomer(customer), customerOrders.size(), customerOrders.stream().limit(10).map(this::mapPosOrOnlineOrder).toList());
    }

    private CustomerSummaryView mapCustomer(AppUser u) {
        return new CustomerSummaryView(u.getId(), u.getFullName(), u.getEmail(), u.getPhone(),
                u.getTier() == null ? "NEW" : u.getTier().getTierCode(), u.getTier() == null ? "Mới" : u.getTier().getTierName(),
                u.getTotalSpent() == null ? BigDecimal.ZERO : u.getTotalSpent(),
                u.getTier() == null ? BigDecimal.ZERO : u.getTier().getDiscountPercent(),
                Objects.toString(u.getDefaultAddress(), ""), u.getCreatedAt());
    }

    private BigDecimal cashField(Payment payment, String key) {
        if (payment == null || payment.getNote() == null) return null;
        for (String part : payment.getNote().split(";")) {
            String prefix = key + "=";
            if (part.startsWith(prefix)) {
                try { return new BigDecimal(part.substring(prefix.length())); }
                catch (NumberFormatException ignored) { return null; }
            }
        }
        return null;
    }

    private OrderView mapPosOrOnlineOrder(CustomerOrder o) {
        Payment p = payments.findFirstByOrder_IdOrderByCreatedAtDesc(o.getId()).orElse(null);
        var items = orderService.getItems(o.getId()).stream().map(i -> new OrderItemView(i.getProductNameSnapshot(),
                i.getVariantLabelSnapshot(), i.getSkuSnapshot(), i.getUnitPrice(), i.getQuantity(), i.getLineTotal())).toList();
        return new OrderView(o.getId(), o.getOrderCode(), o.getSalesChannel(), o.getOrderStatus(), o.getCustomerName(),
                o.getCustomerPhone(), o.getShippingAddress(), o.getSubtotal(), o.getVariantDiscountTotal(),
                o.getMemberDiscountTotal(), o.getCouponDiscountTotal(), o.getShippingFee(), o.getTotalAmount(),
                p == null ? "" : p.getPaymentMethod(), p == null ? "" : p.getPaymentStatus(), o.getCreatedAt(), items,
                refundService.eligibility(o, p), cashField(p, "CASH_RECEIVED"), cashField(p, "CASH_CHANGE"));
    }

    @PostMapping("/orders/{orderId}/convert-to-cash")
    public Map<String,Object> convertPosTransferToCash(@PathVariable Long orderId,
            @RequestBody(required = false) PosCashConversionRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        CustomerOrder existing = orders.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn POS."));
        if (!"POS".equals(existing.getSalesChannel()))
            throw new IllegalArgumentException("Chức năng đổi sang tiền mặt chỉ áp dụng cho đơn POS tại quầy.");
        String role = principal.getUser().getRole().getRoleCode();
        if (!Set.of("ADMIN", "MANAGER").contains(role)
                && (existing.getCreatedBy() == null || !existing.getCreatedBy().getId().equals(principal.getUser().getId())))
            throw new IllegalArgumentException("Bạn không có quyền đổi phương thức thanh toán đơn POS này.");
        BigDecimal cashReceived = request == null ? null : request.cashReceived();
        if (cashReceived == null || cashReceived.signum() < 0 || cashReceived.stripTrailingZeros().scale() > 0)
            throw new IllegalArgumentException("Vui lòng nhập số tiền khách đưa là số nguyên VNĐ hợp lệ.");

        CustomerOrder converted = orderService.convertPosTransferToCash(orderId, principal.getUser(), cashReceived);
        Payment payment = payments.findFirstByOrder_IdOrderByCreatedAtDesc(converted.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch thanh toán sau khi cập nhật."));
        BigDecimal received = cashReceived.setScale(0, java.math.RoundingMode.UNNECESSARY);
        BigDecimal change = received.subtract(payment.getAmount()).setScale(0, java.math.RoundingMode.UNNECESSARY);
        Map<String,Object> response = new LinkedHashMap<>();
        response.put("orderId", converted.getId());
        response.put("orderCode", converted.getOrderCode());
        response.put("orderStatus", converted.getOrderStatus());
        response.put("paymentId", payment.getId());
        response.put("paymentMethod", payment.getPaymentMethod());
        response.put("paymentStatus", payment.getPaymentStatus());
        response.put("total", converted.getTotalAmount());
        response.put("cashReceived", received);
        response.put("cashChange", change);
        response.put("message", "Đã đổi đơn POS sang tiền mặt và hoàn thành thanh toán.");
        return response;
    }

    @PostMapping("/orders/{orderId}/confirm-payment")
    public MessageView confirmOrderPayment(@PathVariable Long orderId, @RequestBody(required = false) Map<String,String> body,
                                           @AuthenticationPrincipal CustomUserDetails principal) {
        CustomerOrder order = orders.findById(orderId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn POS."));
        if (!"POS".equals(order.getSalesChannel())) throw new IllegalArgumentException("Endpoint POS chỉ xác nhận đơn tại quầy.");
        String role = principal.getUser().getRole().getRoleCode();
        if (!Set.of("ADMIN", "MANAGER").contains(role) && (order.getCreatedBy() == null || !order.getCreatedBy().getId().equals(principal.getUser().getId())))
            throw new IllegalArgumentException("Bạn không có quyền xác nhận thanh toán đơn POS này.");
        Payment payment = payments.findFirstByOrder_IdOrderByCreatedAtDesc(orderId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khoản thanh toán."));
        orderService.confirmPayment(payment.getId(), principal.getUser(), body == null ? null : body.get("reference"));
        return new MessageView(true, "Đã xác nhận tiền vào. Đơn POS tự động chuyển sang COMPLETED.");
    }

    @PostMapping("/payments/{paymentId}/confirm")
    public MessageView confirmPayment(@PathVariable Long paymentId, @RequestBody(required = false) Map<String,String> body,
                                      @AuthenticationPrincipal CustomUserDetails principal) {
        Payment payment = payments.findById(paymentId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khoản thanh toán."));
        if (!"POS".equals(payment.getOrder().getSalesChannel())) throw new IllegalArgumentException("Nhân viên POS chỉ xác nhận được thanh toán của đơn tại quầy.");
        String role = principal.getUser().getRole().getRoleCode();
        if (!Set.of("ADMIN", "MANAGER").contains(role) && (payment.getOrder().getCreatedBy() == null || !payment.getOrder().getCreatedBy().getId().equals(principal.getUser().getId())))
            throw new IllegalArgumentException("Bạn không có quyền xác nhận khoản thanh toán này.");
        orderService.confirmPayment(paymentId, principal.getUser(), body == null ? null : body.get("reference"));
        return new MessageView(true, "Đã xác nhận tiền vào. Đơn POS tự động chuyển sang COMPLETED.");
    }

    @PostMapping("/orders/{orderId}/refund-request")
    public RefundView requestPosRefund(@PathVariable Long orderId, @RequestBody RefundRequest request,
                                       @AuthenticationPrincipal CustomUserDetails principal) {
        CustomerOrder order = orders.findById(orderId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn POS."));
        if (!"POS".equals(order.getSalesChannel())) throw new IllegalArgumentException("Chỉ áp dụng cho đơn bán tại quầy.");
        String role = principal.getUser().getRole().getRoleCode();
        if (!Set.of("ADMIN", "MANAGER").contains(role) && (order.getCreatedBy() == null || !order.getCreatedBy().getId().equals(principal.getUser().getId())))
            throw new IllegalArgumentException("Bạn không có quyền gửi yêu cầu hoàn tiền cho đơn POS này.");
        var refund = orderService.requestRefund(orderId, principal.getUser(), request == null ? null : request.amount(),
                request != null && Boolean.TRUE.equals(request.fullRefund()), request == null ? null : request.reason());
        return new RefundView(refund.getId(), refund.getOrder().getId(), refund.getOrder().getOrderCode(),
                refund.getOrder().getCustomerName(), refund.getOrder().getCustomerPhone(), refund.getAmount(),
                refund.getRefundMethod(), refund.getStatus(), refund.getReason(), refund.getTransactionReference(), refund.getRequestedAt());
    }

    private HeldSessionView mapSession(HeldInvoice h, AppUser actor) {
        List<HeldItemView> lines = held.getItems(h.getId(), actor).stream().map(i -> new HeldItemView(
                i.getProductVariant().getId(), i.getProductNameSnapshot(), i.getVariantLabelSnapshot(), i.getSkuSnapshot(),
                i.getUnitPrice(), i.getQuantity(), i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())),
                i.getProductVariant().getStockQuantity())).toList();
        BigDecimal subtotal = lines.stream().map(HeldItemView::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new HeldSessionView(h.getId(), h.getHeldCode(), h.getCustomerName(), h.getCustomerPhone(),
                h.getCustomerNote(), h.getCouponCode(), h.getStatus(), lines.stream().mapToInt(HeldItemView::quantity).sum(),
                subtotal, lines, h.getCreatedAt(), h.getUpdatedAt());
    }

    private OrderView mapPosOrder(CustomerOrder o) {
        Payment p = payments.findFirstByOrder_IdOrderByCreatedAtDesc(o.getId()).orElse(null);
        var items = orderService.getItems(o.getId()).stream().map(i -> new OrderItemView(i.getProductNameSnapshot(),
                i.getVariantLabelSnapshot(), i.getSkuSnapshot(), i.getUnitPrice(), i.getQuantity(), i.getLineTotal())).toList();
        return new OrderView(o.getId(), o.getOrderCode(), o.getSalesChannel(), o.getOrderStatus(), o.getCustomerName(),
                o.getCustomerPhone(), o.getShippingAddress(), o.getSubtotal(), o.getVariantDiscountTotal(),
                o.getMemberDiscountTotal(), o.getCouponDiscountTotal(), o.getShippingFee(), o.getTotalAmount(),
                p == null ? "" : p.getPaymentMethod(), p == null ? "" : p.getPaymentStatus(), o.getCreatedAt(), items,
                refundService.eligibility(o, p), cashField(p, "CASH_RECEIVED"), cashField(p, "CASH_CHANGE"));
    }
}
