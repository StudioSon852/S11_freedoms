package vn.shoegroup.sd11.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.shoegroup.sd11.dto.CartEntry;
import vn.shoegroup.sd11.dto.PricedLine;
import vn.shoegroup.sd11.dto.PricingResult;
import vn.shoegroup.sd11.entity.*;
import vn.shoegroup.sd11.repository.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {
    private final CustomerOrderRepository orders;
    private final OrderItemRepository orderItems;
    private final PaymentRepository payments;
    private final ProductVariantRepository variants;
    private final OrderStatusHistoryRepository history;
    private final MemberTierRepository tiers;
    private final UserRepository users;
    private final PricingService pricing;
    private final RefundRepository refunds;

    public OrderService(CustomerOrderRepository orders, OrderItemRepository orderItems, PaymentRepository payments,
            ProductVariantRepository variants, OrderStatusHistoryRepository history, MemberTierRepository tiers,
            UserRepository users, PricingService pricing, RefundRepository refunds) {
        this.orders = orders; this.orderItems = orderItems; this.payments = payments; this.variants = variants;
        this.history = history; this.tiers = tiers; this.users = users; this.pricing = pricing; this.refunds = refunds;
    }

    @Transactional
    public CustomerOrder createOrder(List<CartEntry> entries, AppUser customer, AppUser staff, String channel,
            String paymentMethod, String customerName, String customerPhone, String address, String note, String couponCode) {
        return createOrder(entries, customer, staff, channel, paymentMethod, customerName, customerPhone,
                address, note, couponCode, BigDecimal.ZERO);
    }

    @Transactional
    public CustomerOrder createOrder(List<CartEntry> entries, AppUser customer, AppUser staff, String channel,
            String paymentMethod, String customerName, String customerPhone, String address, String note,
            String couponCode, BigDecimal shippingFee) {
        String normalizedChannel = channel == null ? "" : channel.trim().toUpperCase();
        if (!List.of("ONLINE", "POS").contains(normalizedChannel))
            throw new IllegalArgumentException("Kênh bán hàng không hợp lệ.");
        String normalizedMethod = paymentMethod == null ? "BANK_TRANSFER" : paymentMethod.trim().toUpperCase();
        List<String> allowedMethods = "POS".equals(normalizedChannel)
                ? List.of("BANK_TRANSFER", "CASH") : List.of("BANK_TRANSFER", "COD");
        if (!allowedMethods.contains(normalizedMethod))
            throw new IllegalArgumentException("Phương thức thanh toán không phù hợp với kênh bán hàng.");
        // Reload the latest account/tier from MySQL; the login Session may contain an older tier snapshot.
        if (customer != null) customer = users.findById(customer.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản khách hàng."));
        if (customer != null && "ONLINE".equals(normalizedChannel)
                && (customer.getDefaultAddress() == null || customer.getDefaultAddress().isBlank())
                && address != null && !address.isBlank()) {
            customer.setDefaultAddress(address.trim());
            users.save(customer);
        }
        if (staff != null) staff = users.findById(staff.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản nhân viên."));
        PricingResult result = pricing.calculate(entries, customer, normalizedChannel, couponCode, true);
        LocalDateTime now = LocalDateTime.now();

        CustomerOrder order = new CustomerOrder();
        order.setOrderCode(generateOrderCode());
        order.setCustomer(customer); order.setCreatedBy(staff); order.setSalesChannel(normalizedChannel);
        boolean cashPaid = "CASH".equals(normalizedMethod);
        boolean transfer = "BANK_TRANSFER".equals(normalizedMethod);
        order.setOrderStatus(cashPaid ? "COMPLETED" : (transfer ? "PENDING_PAYMENT" : "CONFIRMED"));
        order.setCustomerName(blankTo(customerName, customer != null ? customer.getFullName() : "Khách lẻ"));
        order.setCustomerPhone(blankTo(customerPhone, customer != null ? customer.getPhone() : ""));
        order.setShippingAddress(address); order.setCustomerNote(note);
        order.setCouponCode(result.coupon() == null ? null : result.coupon().getCouponCode());
        order.setSubtotal(result.subtotal()); order.setVariantDiscountTotal(result.variantDiscountTotal());
        order.setMemberDiscountTotal(result.memberDiscountTotal()); order.setCouponDiscountTotal(result.couponDiscountTotal());
        BigDecimal safeShipping = shippingFee == null ? BigDecimal.ZERO : shippingFee.max(BigDecimal.ZERO);
        order.setShippingFee(safeShipping);
        order.setTotalAmount(result.total().add(safeShipping).setScale(0, java.math.RoundingMode.HALF_UP)); order.setSpendingRecorded(false); order.setCreatedAt(now); order.setUpdatedAt(now);
        orders.save(order);

        for (PricedLine line : result.lines()) {
            ProductVariant variant = line.variant();
            variant.setStockQuantity(variant.getStockQuantity() - line.quantity());
            variants.save(variant);
            OrderItem item = new OrderItem();
            item.setOrder(order); item.setProductVariant(variant); item.setProductNameSnapshot(variant.getProduct().getProductName());
            item.setVariantLabelSnapshot(variantLabel(variant)); item.setSkuSnapshot(variant.getSku());
            item.setUnitPrice(variant.getUnitPrice()); item.setQuantity(line.quantity());
            item.setVariantDiscountAmount(line.variantDiscount()); item.setMemberDiscountAmount(line.memberDiscount());
            item.setCouponDiscountAmount(line.couponDiscount()); item.setLineTotal(line.lineTotal());
            orderItems.save(item);
        }
        pricing.consumeCoupon(result.coupon());

        Payment payment = new Payment();
        payment.setOrder(order); payment.setPaymentMethod(normalizedMethod);
        payment.setPaymentStatus(cashPaid ? "PAID" : (transfer ? "PENDING" : "UNPAID"));
        payment.setAmount(order.getTotalAmount()); payment.setCreatedAt(now); payment.setPaidAt(cashPaid ? now : null);
        if (cashPaid) payment.setTransactionReference("POS-CASH-" + order.getOrderCode());
        payments.save(payment);
        addHistory(order, null, order.getOrderStatus(), staff != null ? staff : customer,
                cashPaid ? "Thanh toán tiền mặt tại quầy" : (transfer ? "Chờ thanh toán chuyển khoản" : "Đơn hàng COD được tạo"));
        if (cashPaid && customer != null) recordCustomerSpending(order);
        return order;
    }

    @Transactional
    public void markTransferSubmitted(Long orderId, AppUser customer) {
        CustomerOrder order = orders.findByIdForUpdate(orderId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));
        if (order.getCustomer() == null || !order.getCustomer().getId().equals(customer.getId()))
            throw new IllegalArgumentException("Bạn không có quyền thao tác đơn hàng này.");
        if (!"PENDING_PAYMENT".equals(order.getOrderStatus())) throw new IllegalArgumentException("Đơn hàng không còn chờ thanh toán.");
        Payment payment = payments.findAllByOrderIdForUpdate(orderId).stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thanh toán."));
        if ("PAID".equals(payment.getPaymentStatus())) return;
        if ("WAITING_CONFIRMATION".equals(payment.getPaymentStatus())) return;
        if (!"PENDING".equals(payment.getPaymentStatus()))
            throw new IllegalArgumentException("Thanh toán hiện không ở trạng thái có thể thông báo chuyển khoản.");
        payment.setPaymentStatus("WAITING_CONFIRMATION");
        payment.setNote("Khách hàng thông báo đã chuyển khoản; chờ nhân viên xác nhận.");
        payments.save(payment);
    }

    @Transactional
    public void confirmPayment(Long paymentId, AppUser actor, String reference) {
        Long orderId = payments.findOrderIdByPaymentId(paymentId);
        if (orderId == null) throw new IllegalArgumentException("Không tìm thấy thanh toán.");
        // Lock theo thứ tự order -> payment để tránh xác nhận đồng thời và giảm nguy cơ deadlock.
        CustomerOrder order = orders.findByIdForUpdate(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));
        Payment payment = payments.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thanh toán."));
        if (!payment.getOrder().getId().equals(order.getId()))
            throw new IllegalArgumentException("Thanh toán không thuộc đơn hàng này.");
        if ("CANCELLED".equals(order.getOrderStatus()))
            throw new IllegalArgumentException("Đơn đã hủy, không thể xác nhận thanh toán.");
        if (!"BANK_TRANSFER".equals(payment.getPaymentMethod()))
            throw new IllegalArgumentException("Chỉ xác nhận thủ công các khoản chuyển khoản. COD tự ghi nhận khi hoàn thành giao hàng, tiền mặt POS được ghi nhận ngay.");
        if ("PAID".equals(payment.getPaymentStatus())) return; // Idempotent: click twice never counts twice.
        if (!List.of("PENDING", "WAITING_CONFIRMATION").contains(payment.getPaymentStatus()))
            throw new IllegalArgumentException("Thanh toán không ở trạng thái chờ xác nhận.");
        payment.setPaymentStatus("PAID");
        payment.setPaidAt(LocalDateTime.now());
        payment.setTransactionReference(blankTo(reference, "MANUAL-" + System.currentTimeMillis()));
        payment.setNote("Đã được nhân viên xác nhận nhận tiền.");
        payments.save(payment);
        String next = "POS".equals(order.getSalesChannel()) ? "COMPLETED" : "CONFIRMED";
        if (!next.equals(order.getOrderStatus())) {
            String old = order.getOrderStatus();
            order.setOrderStatus(next);
            order.setUpdatedAt(LocalDateTime.now());
            orders.save(order);
            addHistory(order, old, next, actor, "Nhân viên xác nhận thanh toán thành công");
        }
        if (order.getCustomer() != null) recordCustomerSpending(order);
    }


    @Transactional
    public Refund requestRefund(Long orderId, AppUser actor, BigDecimal requestedAmount, boolean fullRefund, String reason) {
        CustomerOrder order = orders.findByIdForUpdate(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));
        if (actor == null) throw new IllegalArgumentException("Vui lòng đăng nhập để gửi yêu cầu hoàn tiền.");
        String role = actor.getRole() == null ? "" : actor.getRole().getRoleCode();
        if ("CUSTOMER".equals(role) && (order.getCustomer() == null || !order.getCustomer().getId().equals(actor.getId())))
            throw new IllegalArgumentException("Bạn không có quyền gửi yêu cầu hoàn tiền cho đơn hàng này.");
        if (!List.of("CUSTOMER", "ADMIN", "MANAGER", "EMPLOYEE").contains(role))
            throw new IllegalArgumentException("Tài khoản không được phép gửi yêu cầu hoàn tiền.");

        Payment payment = payments.findAllByOrderIdForUpdate(orderId).stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch thanh toán."));
        if (!List.of("PAID", "REFUND_PENDING").contains(payment.getPaymentStatus()))
            throw new IllegalArgumentException("Chỉ giao dịch đã thanh toán mới được gửi yêu cầu hoàn tiền.");
        validateRefundWindow(order, payment);

        long used = refunds.countByPayment_Id(payment.getId());
        if (used >= 2) throw new IllegalArgumentException("Mỗi giao dịch chỉ được gửi tối đa 2 lần yêu cầu hoàn tiền.");
        BigDecimal reserved = refunds.sumReservedAmountByPaymentId(payment.getId());
        if (reserved == null) reserved = BigDecimal.ZERO;
        BigDecimal remaining = payment.getAmount().subtract(reserved).max(BigDecimal.ZERO);
        BigDecimal amount = fullRefund ? remaining : requestedAmount;
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("Số tiền hoàn phải lớn hơn 0.");
        if (amount.compareTo(remaining) > 0)
            throw new IllegalArgumentException("Số tiền hoàn vượt số tiền giao dịch còn có thể hoàn: " + remaining.toPlainString() + " VNĐ.");
        if (reason != null && reason.length() > 500) throw new IllegalArgumentException("Lý do hoàn tiền không được vượt quá 500 ký tự.");
        // Keep the stored value at two decimals and round down so rounding can never exceed the payment amount.
        amount = amount.setScale(2, java.math.RoundingMode.DOWN);
        if (amount.signum() <= 0) throw new IllegalArgumentException("Số tiền hoàn sau khi làm tròn phải lớn hơn 0.");

        Refund refund = new Refund();
        refund.setOrder(order);
        refund.setPayment(payment);
        refund.setRequestedBy(actor);
        refund.setAmount(amount);
        refund.setRefundMethod(payment.getPaymentMethod());
        refund.setStatus("PENDING");
        refund.setReason(blankTo(reason, fullRefund ? "Yêu cầu hoàn toàn phần" : "Yêu cầu hoàn một phần"));
        refund.setRequestedAt(LocalDateTime.now());
        refunds.save(refund);
        payment.setPaymentStatus("REFUND_PENDING");
        payment.setNote("Đã nhận yêu cầu hoàn tiền " + amount.toPlainString() + " VNĐ; chờ nhân viên xử lý.");
        payments.save(payment);
        return refund;
    }

    private void validateRefundWindow(CustomerOrder order, Payment payment) {
        int hours = "POS".equalsIgnoreCase(order.getSalesChannel()) ? 24 : 48;
        LocalDateTime base = payment.getPaidAt() != null ? payment.getPaidAt() : order.getCreatedAt();
        if (base == null || LocalDateTime.now().isAfter(base.plusHours(hours)))
            throw new IllegalArgumentException("Đã quá thời hạn yêu cầu hoàn tiền: Online tối đa 48 giờ, POS tối đa 24 giờ kể từ lúc thanh toán.");
    }

    @Transactional
    public void cancelCustomerOrder(Long orderId, AppUser customer) {
        CustomerOrder order = orders.findByIdForUpdate(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));
        if (order.getCustomer() == null || !order.getCustomer().getId().equals(customer.getId()))
            throw new IllegalArgumentException("Bạn không có quyền hủy đơn hàng này.");
        if (!List.of("PENDING_PAYMENT", "CONFIRMED").contains(order.getOrderStatus()))
            throw new IllegalArgumentException("Khách hàng chỉ được tự hủy khi đơn đang chờ thanh toán hoặc chờ xác nhận. Khi đơn đã xử lý/giao hàng, hãy liên hệ cửa hàng để làm thủ tục trả hàng.");
        changeOrderStatus(orderId, "CANCELLED", customer, "Khách hàng yêu cầu hủy đơn.");
    }

    @Transactional
    public void cancelAfterNoFundsVerified(Long orderId, AppUser actor, String note) {
        CustomerOrder order = orders.findByIdForUpdate(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));
        Payment payment = payments.findAllByOrderIdForUpdate(orderId).stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thanh toán."));
        if (!"WAITING_CONFIRMATION".equals(payment.getPaymentStatus()))
            throw new IllegalArgumentException("Chỉ dùng thao tác này khi khách đã báo chuyển khoản nhưng cửa hàng chưa xác nhận tiền vào.");
        // Explicit admin reconciliation: staff has checked the bank statement and verified no funds arrived.
        payment.setPaymentStatus("PENDING"); payment.setNote("Admin đã đối soát và xác nhận chưa nhận được tiền chuyển khoản.");
        payments.save(payment);
        changeOrderStatus(orderId, "CANCELLED", actor, blankTo(note, "Đối soát ngân hàng: chưa nhận tiền, hủy đơn."));
    }

    @Transactional
    public void changeOrderStatus(Long orderId, String newStatus, AppUser actor, String note) {
        CustomerOrder order = orders.findByIdForUpdate(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));
        String target = newStatus == null ? "" : newStatus.trim().toUpperCase();
        List<String> allowed = List.of("CONFIRMED", "PROCESSING", "SHIPPING", "COMPLETED", "CANCELLED");
        if (!allowed.contains(target)) throw new IllegalArgumentException("Trạng thái đơn hàng không hợp lệ.");
        String current = order.getOrderStatus();
        if ("CANCELLED".equals(current) || "COMPLETED".equals(current))
            throw new IllegalArgumentException("Đơn hàng đã kết thúc; không thể hủy hoặc đổi trạng thái trực tiếp. Hãy gửi yêu cầu hoàn tiền từ giao dịch đã thanh toán trong thời hạn cho phép.");

        if (!"CANCELLED".equals(target)) {
            boolean valid = switch (current) {
                case "CONFIRMED" -> "PROCESSING".equals(target);
                case "PROCESSING" -> "SHIPPING".equals(target);
                case "SHIPPING" -> "COMPLETED".equals(target);
                default -> false;
            };
            if (!valid) throw new IllegalArgumentException("Luồng trạng thái không hợp lệ: " + current + " → " + target);
        } else {
            boolean cancellableBeforeShipping = List.of("PENDING_PAYMENT", "CONFIRMED", "PROCESSING").contains(current);
            if (!cancellableBeforeShipping)
                throw new IllegalArgumentException("Chỉ được hủy đơn trước khi chuyển sang đang giao. Đơn đã hoàn thành phải gửi yêu cầu hoàn tiền.");
            Payment payment = payments.findAllByOrderIdForUpdate(orderId).stream().findFirst().orElse(null);
            if (payment != null && "WAITING_CONFIRMATION".equals(payment.getPaymentStatus()))
                throw new IllegalArgumentException("Khách đã thông báo chuyển khoản nhưng chưa được đối soát. Cửa hàng cần kiểm tra tài khoản ngân hàng trước khi quyết định hủy.");
            if (payment != null && List.of("PAID", "REFUND_PENDING").contains(payment.getPaymentStatus())) {
                validateRefundWindow(order, payment);
                BigDecimal reserved = refunds.sumReservedAmountByPaymentId(payment.getId());
                if (reserved == null) reserved = BigDecimal.ZERO;
                BigDecimal remaining = payment.getAmount().subtract(reserved).max(BigDecimal.ZERO);
                if (remaining.signum() > 0) {
                    long used = refunds.countByPayment_Id(payment.getId());
                    if (used >= 2) throw new IllegalArgumentException("Giao dịch đã đạt tối đa 2 yêu cầu hoàn tiền; không thể tự động tạo thêm yêu cầu khi hủy.");
                    Refund refund = new Refund();
                    refund.setOrder(order); refund.setPayment(payment); refund.setRequestedBy(actor);
                    refund.setAmount(remaining); refund.setRefundMethod(payment.getPaymentMethod());
                    refund.setStatus("PENDING");
                    refund.setReason(blankTo(note, "Hủy đơn đã thanh toán - yêu cầu hoàn toàn bộ số tiền còn lại."));
                    refund.setRequestedAt(LocalDateTime.now()); refunds.save(refund);
                    payment.setPaymentStatus("REFUND_PENDING");
                    payment.setNote("Đơn đã hủy; yêu cầu hoàn " + remaining.toPlainString() + " VNĐ đang chờ xử lý.");
                }
                BigDecimal actuallyRefunded = refunds.sumRefundedAmountByPaymentId(payment.getId());
                if (actuallyRefunded == null) actuallyRefunded = BigDecimal.ZERO;
                boolean refundStillPending = refunds.findAllByPayment_IdOrderByRequestedAtAsc(payment.getId()).stream()
                        .anyMatch(r -> "PENDING".equals(r.getStatus()));
                if (actuallyRefunded.compareTo(payment.getAmount()) >= 0) {
                    payment.setPaymentStatus("REFUNDED");
                    payment.setNote("Đã thực tế hoàn đủ số tiền của giao dịch.");
                } else if (refundStillPending) {
                    payment.setPaymentStatus("REFUND_PENDING");
                    payment.setNote("Đơn đã hủy; vẫn còn yêu cầu hoàn tiền đang chờ xử lý.");
                } else {
                    payment.setPaymentStatus("PAID");
                    payment.setNote("Đơn đã hủy; giao dịch đã thanh toán, số tiền hoàn thực tế hiện tại: "
                            + actuallyRefunded.toPlainString() + " VNĐ.");
                }
                payments.save(payment);
            } else if (payment != null && !"REFUNDED".equals(payment.getPaymentStatus())) {
                payment.setPaymentStatus("CANCELLED");
                payment.setNote("Thanh toán chưa hoàn tất; đơn đã được hủy.");
                payments.save(payment);
            }
            // Restore inventory once, in the same transaction as order cancellation.
            for (OrderItem item : orderItems.findByOrder_IdOrderByIdAsc(orderId)) {
                ProductVariant variant = variants.findByIdForUpdate(item.getProductVariant().getId()).orElseThrow();
                variant.setStockQuantity(variant.getStockQuantity() + item.getQuantity());
                variants.save(variant);
            }
            pricing.releaseCoupon(order.getCouponCode());
        }

        String old = order.getOrderStatus();
        order.setOrderStatus(target); order.setUpdatedAt(LocalDateTime.now()); orders.save(order);
        if ("COMPLETED".equals(target)) {
            Payment payment = payments.findAllByOrderIdForUpdate(orderId).stream().findFirst().orElse(null);
            if (payment != null && !List.of("PAID", "REFUND_PENDING", "REFUNDED").contains(payment.getPaymentStatus())) {
                payment.setPaymentStatus("PAID");
                if (payment.getPaidAt() == null) payment.setPaidAt(LocalDateTime.now());
                payment.setNote("Đã thu tiền khi hoàn thành đơn COD."); payments.save(payment);
            }
            if (order.getCustomer() != null) recordCustomerSpending(order);
        }
        addHistory(order, old, target, actor, note);
    }

    public List<OrderItem> getItems(Long orderId) { return orderItems.findByOrder_IdOrderByIdAsc(orderId); }

    private void recordCustomerSpending(CustomerOrder order) {
        if (Boolean.TRUE.equals(order.getSpendingRecorded()) || order.getCustomer() == null) return;
        AppUser customer = users.findByIdForUpdate(order.getCustomer().getId()).orElseThrow();
        BigDecimal currentSpent = customer.getTotalSpent() == null ? BigDecimal.ZERO : customer.getTotalSpent();
        BigDecimal accumulated = currentSpent.add(order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount());
        customer.setTotalSpent(accumulated);
        List<MemberTier> all = tiers.findAllByOrderByMinSpendingAsc();
        MemberTier selected = all.isEmpty() ? customer.getTier() : all.get(0);
        for (MemberTier tier : all) if (accumulated.compareTo(tier.getMinSpending()) >= 0) selected = tier;
        customer.setTier(selected); users.save(customer);
        order.setSpendingRecorded(true); orders.save(order);
    }


    @Transactional
    public void applyRefundToCustomerSpending(CustomerOrder order, BigDecimal refundedAmount) {
        if (order == null || !Boolean.TRUE.equals(order.getSpendingRecorded()) || order.getCustomer() == null
                || refundedAmount == null || refundedAmount.signum() <= 0) return;
        AppUser customer = users.findByIdForUpdate(order.getCustomer().getId()).orElseThrow();
        BigDecimal currentSpent = customer.getTotalSpent() == null ? BigDecimal.ZERO : customer.getTotalSpent();
        customer.setTotalSpent(currentSpent.subtract(refundedAmount).max(BigDecimal.ZERO));
        List<MemberTier> all = tiers.findAllByOrderByMinSpendingAsc();
        if (!all.isEmpty()) {
            MemberTier selected = all.get(0);
            for (MemberTier tier : all) if (customer.getTotalSpent().compareTo(tier.getMinSpending()) >= 0) selected = tier;
            customer.setTier(selected);
        }
        users.save(customer);
    }

    private void reverseCustomerSpending(CustomerOrder order) {
        if (!Boolean.TRUE.equals(order.getSpendingRecorded()) || order.getCustomer() == null) return;
        AppUser customer = users.findByIdForUpdate(order.getCustomer().getId()).orElseThrow();
        BigDecimal currentSpent = customer.getTotalSpent() == null ? BigDecimal.ZERO : customer.getTotalSpent();
        BigDecimal accumulated = currentSpent.subtract(order.getTotalAmount()).max(BigDecimal.ZERO);
        customer.setTotalSpent(accumulated);
        List<MemberTier> all = tiers.findAllByOrderByMinSpendingAsc();
        MemberTier selected = all.isEmpty() ? customer.getTier() : all.get(0);
        for (MemberTier tier : all) if (accumulated.compareTo(tier.getMinSpending()) >= 0) selected = tier;
        customer.setTier(selected); users.save(customer);
        order.setSpendingRecorded(false); orders.save(order);
    }

    private void addHistory(CustomerOrder order, String oldStatus, String newStatus, AppUser actor, String note) {
        OrderStatusHistory row = new OrderStatusHistory(); row.setOrder(order); row.setOldStatus(oldStatus);
        row.setNewStatus(newStatus); row.setChangedBy(actor); row.setNote(note); row.setCreatedAt(LocalDateTime.now()); history.save(row);
    }
    public static String variantLabel(ProductVariant v) {
        String size = v.getSize() == null ? "Không rõ size" : "Size " + v.getSize().getSizeLabel();
        String color = v.getColor() == null ? "" : " - " + v.getColor().getColorName();
        return size + color;
    }
    private String generateOrderCode() { return "FD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss")) + UUID.randomUUID().toString().substring(0, 4).toUpperCase(); }
    private String blankTo(String value, String fallback) { return value == null || value.isBlank() ? (fallback == null ? "" : fallback) : value.trim(); }
}
