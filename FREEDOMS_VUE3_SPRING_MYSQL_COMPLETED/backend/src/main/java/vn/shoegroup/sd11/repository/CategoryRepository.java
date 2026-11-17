package vn.shoegroup.sd11.repository;
import org.springframework.data.jpa.repository.JpaRepository; import vn.shoegroup.sd11.entity.Category; import java.util.List;
public interface CategoryRepository extends JpaRepository<Category, Long> { List<Category> findByActiveTrueOrderByCategoryNameAsc(); }
