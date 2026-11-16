package vn.shoegroup.sd11.repository;
import jakarta.persistence.LockModeType; import org.springframework.data.jpa.repository.JpaRepository; import org.springframework.data.jpa.repository.Lock; import org.springframework.data.jpa.repository.Query; import org.springframework.data.repository.query.Param; import vn.shoegroup.sd11.entity.ProductVariant; import java.util.List; import java.util.Optional;
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
 List<ProductVariant> findByActiveTrueOrderByIdAsc(); List<ProductVariant> findByProduct_IdAndActiveTrueOrderByIdAsc(Long productId); List<ProductVariant> findByProduct_IdOrderByIdAsc(Long productId); boolean existsBySkuIgnoreCase(String sku); boolean existsBySize_Id(Long sizeId); boolean existsByColor_Id(Long colorId);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select v from ProductVariant v where v.id = :id") Optional<ProductVariant> findByIdForUpdate(@Param("id") Long id);
}
