package vn.shoegroup.sd11.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;
import org.springframework.data.repository.query.Param;
import vn.shoegroup.sd11.entity.CustomerOrder;
import java.util.List;
import java.util.Optional;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
    List<CustomerOrder> findByCustomer_IdOrderByCreatedAtDesc(Long customerId);
    List<CustomerOrder> findAllByOrderByCreatedAtDesc();
    Optional<CustomerOrder> findByOrderCode(String orderCode);

    @Query(value = "select coalesce(sum(greatest(o.total_amount - coalesce((select sum(r.amount) from refunds r where r.payment_id = p.payment_id and r.status = 'REFUNDED'), 0), 0)), 0) from orders o join payments p on p.order_id = o.order_id where o.order_status = 'COMPLETED' and p.payment_status = 'PAID'", nativeQuery = true)
    BigDecimal sumCompletedRevenue();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from CustomerOrder o where o.id = :id")
    Optional<CustomerOrder> findByIdForUpdate(@Param("id") Long id);
}
