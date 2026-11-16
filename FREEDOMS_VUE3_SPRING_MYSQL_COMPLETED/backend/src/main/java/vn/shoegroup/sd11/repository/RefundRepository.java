package vn.shoegroup.sd11.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.shoegroup.sd11.entity.Refund;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface RefundRepository extends JpaRepository<Refund, Long> {
    List<Refund> findAllByOrderByRequestedAtDesc();
    List<Refund> findAllByPayment_IdOrderByRequestedAtAsc(Long paymentId);
    long countByPayment_Id(Long paymentId);

    @Query("select coalesce(sum(r.amount), 0) from Refund r where r.payment.id = :paymentId and r.status <> 'REJECTED'")
    BigDecimal sumReservedAmountByPaymentId(@Param("paymentId") Long paymentId);

    @Query("select coalesce(sum(r.amount), 0) from Refund r where r.payment.id = :paymentId and r.status = 'REFUNDED'")
    BigDecimal sumRefundedAmountByPaymentId(@Param("paymentId") Long paymentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Refund r where r.id = :id")
    Optional<Refund> findByIdForUpdate(@Param("id") Long id);
}
