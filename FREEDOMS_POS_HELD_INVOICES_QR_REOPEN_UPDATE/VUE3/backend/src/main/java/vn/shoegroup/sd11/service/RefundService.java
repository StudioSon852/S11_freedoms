package vn.shoegroup.sd11.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.shoegroup.sd11.api.ApiDtos.RefundEligibilityView;
import vn.shoegroup.sd11.entity.AppUser;
import vn.shoegroup.sd11.entity.CustomerOrder;
import vn.shoegroup.sd11.entity.Payment;
import vn.shoegroup.sd11.entity.Refund;
import vn.shoegroup.sd11.repository.PaymentRepository;
import vn.shoegroup.sd11.repository.RefundRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RefundService {
    private final RefundRepository refunds;
    private final PaymentRepository payments;
    private final OrderService orderService;

    public RefundService(RefundRepository refunds, PaymentRepository payments, OrderService orderService) {
        this.refunds = refunds;
        this.payments = payments;
        this.orderService = orderService;
    }

    @Transactional(readOnly = true)
    public List<Refund> list() { return refunds.findAllByOrderByRequestedAtDesc(); }

    @Transactional(readOnly = true)
    public RefundEligibilityView eligibility(CustomerOrder order, Payment payment) {
        int windowHours = "POS".equalsIgnoreCase(order.getSalesChannel()) ? 24 : 48;
        LocalDateTime paidAt = payment == null ? null : payment.getPaidAt();
        LocalDateTime base = paidAt != null ? paidAt : order.getCreatedAt();
        LocalDateTime deadline = base == null ? null : base.plusHours(windowHours);
        BigDecimal transactionAmount = payment == null || payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount();
        BigDecimal reserved = payment == null || payment.getId() == null ? BigDecimal.ZERO
                : zero(refunds.sumReservedAmountByPaymentId(payment.getId()));
        BigDecimal remaining = transactionAmount.subtract(reserved).max(BigDecimal.ZERO);
        int used = payment == null || payment.getId() == null ? 0
                : Math.toIntExact(refunds.countByPayment_Id(payment.getId()));
        boolean paid = payment != null && List.of("PAID", "REFUND_PENDING").contains(payment.getPaymentStatus());
        boolean inWindow = deadline != null && !LocalDateTime.now().isAfter(deadline);
        boolean eligible = paid && inWindow && used < 2 && remaining.signum() > 0;
        String message = eligible ? "Có thể gửi yêu cầu hoàn tiền trong thời hạn."
                : remaining.signum() <= 0 || (payment != null && "REFUNDED".equals(payment.getPaymentStatus()))
                    ? "Giao dịch đã dùng hết hạn mức hoàn tiền."
                : !inWindow ? "Đã quá thời hạn yêu cầu hoàn tiền."
                : used >= 2 ? "Giao dịch đã sử dụng tối đa 2 lần yêu cầu hoàn tiền."
                : "Giao dịch chưa được xác nhận thanh toán.";
        return new RefundEligibilityView(eligible, remaining, used, Math.max(0, 2 - used), deadline, windowHours, message);
    }

    @Transactional
    public Refund complete(Long id, AppUser actor, String reference, String note) {
        Refund refund = refunds.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu hoàn tiền."));
        if ("REFUNDED".equals(refund.getStatus())) return refund;
        if (!"PENDING".equals(refund.getStatus()))
            throw new IllegalArgumentException("Yêu cầu hoàn tiền không ở trạng thái chờ xử lý.");
        if (reference == null || reference.isBlank())
            throw new IllegalArgumentException("Bắt buộc nhập mã giao dịch hoàn tiền hoặc số phiếu chi để lưu dấu vết đối soát.");
        if (reference.trim().length() > 120)
            throw new IllegalArgumentException("Mã giao dịch hoàn tiền không được vượt quá 120 ký tự.");
        Payment payment = payments.findByIdForUpdate(refund.getPayment().getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thanh toán."));
        if (!List.of("REFUND_PENDING", "PAID").contains(payment.getPaymentStatus()))
            throw new IllegalArgumentException("Thanh toán không ở trạng thái có thể xử lý hoàn tiền.");

        BigDecimal alreadyRefunded = zero(refunds.sumRefundedAmountByPaymentId(payment.getId()));
        if (alreadyRefunded.add(zero(refund.getAmount())).compareTo(zero(payment.getAmount())) > 0)
            throw new IllegalArgumentException("Tổng tiền hoàn thực tế sẽ vượt số tiền của giao dịch gốc.");

        refund.setStatus("REFUNDED");
        refund.setProcessedBy(actor);
        refund.setProcessedAt(LocalDateTime.now());
        refund.setTransactionReference(reference.trim());
        appendNote(refund, note);
        refunds.save(refund);
        orderService.applyRefundToCustomerSpending(refund.getOrder(), refund.getAmount());
        refreshPaymentRefundStatus(payment);
        return refund;
    }

    @Transactional
    public Refund reject(Long id, AppUser actor, String note) {
        Refund refund = refunds.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu hoàn tiền."));
        if (!"PENDING".equals(refund.getStatus()))
            throw new IllegalArgumentException("Chỉ có thể từ chối yêu cầu đang chờ xử lý.");
        Payment payment = payments.findByIdForUpdate(refund.getPayment().getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thanh toán."));
        refund.setStatus("REJECTED");
        refund.setProcessedBy(actor);
        refund.setProcessedAt(LocalDateTime.now());
        appendNote(refund, "Từ chối: " + (note == null || note.isBlank() ? "Không đáp ứng điều kiện hoàn tiền." : note.trim()));
        refunds.save(refund);
        refreshPaymentRefundStatus(payment);
        return refund;
    }

    private void refreshPaymentRefundStatus(Payment payment) {
        BigDecimal actuallyRefunded = zero(refunds.sumRefundedAmountByPaymentId(payment.getId()));
        boolean hasPending = refunds.findAllByPayment_IdOrderByRequestedAtAsc(payment.getId()).stream()
                .anyMatch(r -> "PENDING".equals(r.getStatus()));
        BigDecimal transactionAmount = zero(payment.getAmount());
        if (actuallyRefunded.compareTo(transactionAmount) >= 0) {
            payment.setPaymentStatus("REFUNDED");
            payment.setNote("Đã thực tế hoàn đủ số tiền của giao dịch.");
        } else if (hasPending) {
            payment.setPaymentStatus("REFUND_PENDING");
            payment.setNote("Đang xử lý yêu cầu hoàn tiền; tổng đã hoàn thực tế: " + actuallyRefunded.toPlainString() + " VNĐ.");
        } else if (actuallyRefunded.signum() > 0) {
            payment.setPaymentStatus("PAID");
            payment.setNote("Đã hoàn một phần; giao dịch còn số dư chưa hoàn: "
                    + transactionAmount.subtract(actuallyRefunded).max(BigDecimal.ZERO).toPlainString() + " VNĐ.");
        } else {
            payment.setPaymentStatus("PAID");
            payment.setNote("Yêu cầu hoàn tiền đã bị từ chối; giao dịch vẫn được ghi nhận đã thanh toán.");
        }
        // Keep the original incoming transaction reference on payments. The outbound refund reference
        // is stored on each refunds row so both sides of the reconciliation remain auditable.
        payments.save(payment);
    }

    private void appendNote(Refund refund, String note) {
        if (note == null || note.isBlank()) return;
        String old = refund.getReason() == null ? "" : refund.getReason().trim();
        String addition = note.trim();
        // Retain the newest processing/rejection note even when the original customer reason
        // already fills the 500-character column.
        int maxOldLength = Math.max(0, 500 - addition.length() - (old.isBlank() ? 0 : 3));
        if (addition.length() > 500) addition = addition.substring(0, 500);
        if (old.length() > maxOldLength) old = old.substring(0, maxOldLength);
        String merged = old.isBlank() ? addition : old + " | " + addition;
        refund.setReason(merged);
    }

    private BigDecimal zero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
}
