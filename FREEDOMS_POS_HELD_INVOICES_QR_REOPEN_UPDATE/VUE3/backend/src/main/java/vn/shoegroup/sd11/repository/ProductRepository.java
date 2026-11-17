package vn.shoegroup.sd11.repository;
import org.springframework.data.jpa.repository.JpaRepository; import org.springframework.data.jpa.repository.Query; import org.springframework.data.repository.query.Param; import vn.shoegroup.sd11.entity.Product; import java.util.List;
public interface ProductRepository extends JpaRepository<Product, Long> {
 boolean existsByCategory_Id(Long categoryId); boolean existsByBrand_Id(Long brandId); boolean existsByImageUrl(String imageUrl);
 List<Product> findByActiveTrueOrderByCreatedAtDesc(); List<Product> findByFeaturedTrueAndActiveTrueOrderByCreatedAtDesc();
 @Query("select p from Product p where p.active = true and (:q = '' or lower(p.productName) like lower(concat('%', :q, '%')) or lower(coalesce(p.parentSku,'')) like lower(concat('%', :q, '%')) or lower(coalesce(p.category.categoryName,'')) like lower(concat('%', :q, '%')) or lower(coalesce(p.brand.brandName,'')) like lower(concat('%', :q, '%'))) order by p.createdAt desc")
 List<Product> searchActive(@Param("q") String q);
}
