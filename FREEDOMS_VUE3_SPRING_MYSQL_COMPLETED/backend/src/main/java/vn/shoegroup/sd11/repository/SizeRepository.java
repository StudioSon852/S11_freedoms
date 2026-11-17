package vn.shoegroup.sd11.repository;
import org.springframework.data.jpa.repository.JpaRepository; import vn.shoegroup.sd11.entity.Size; import java.util.List;
public interface SizeRepository extends JpaRepository<Size, Long> { List<Size> findAllByOrderBySortOrderAsc(); }
