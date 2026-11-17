package vn.shoegroup.sd11.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.shoegroup.sd11.entity.Payment;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findFirstByOrder_IdOrderByCreatedAtDesc(Long orderId);
    List<Payment> findAllByOrderByCreatedAtDesc();

    @Query("select p.order.id from Payment p where p.id = :id")
    Long findOrderIdByPaymentId(@Param("id") Long paymentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.id = :id")
    Optional<Payment> findByIdForUpdate(@Param("id") Long paymentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.order.id = :orderId order by p.createdAt desc")
    List<Payment> findAllByOrderIdForUpdate(@Param("orderId") Long orderId);
}
