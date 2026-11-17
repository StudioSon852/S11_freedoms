package vn.shoegroup.sd11.repository;
import org.springframework.data.jpa.repository.JpaRepository; import vn.shoegroup.sd11.entity.OrderItem; import java.util.List;
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> { List<OrderItem> findByOrder_IdOrderByIdAsc(Long orderId); boolean existsByProductVariant_Product_Id(Long productId); }
