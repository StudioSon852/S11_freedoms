package vn.shoegroup.sd11.repository;
import org.springframework.data.jpa.repository.JpaRepository; import vn.shoegroup.sd11.entity.OrderStatusHistory; import java.util.List;
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> { List<OrderStatusHistory> findByOrder_IdOrderByCreatedAtAsc(Long orderId); }
