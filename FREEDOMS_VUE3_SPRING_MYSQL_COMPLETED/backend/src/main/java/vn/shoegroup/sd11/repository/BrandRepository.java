package vn.shoegroup.sd11.repository;
import org.springframework.data.jpa.repository.JpaRepository; import vn.shoegroup.sd11.entity.Brand; import java.util.List;
public interface BrandRepository extends JpaRepository<Brand, Long> { List<Brand> findByActiveTrueOrderByBrandNameAsc(); }
