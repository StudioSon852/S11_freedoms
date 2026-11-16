package vn.shoegroup.sd11.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.shoegroup.sd11.entity.ProductVariantImage;
import java.util.List;
import java.util.Optional;

public interface ProductVariantImageRepository extends JpaRepository<ProductVariantImage, Long> {
    List<ProductVariantImage> findByVariant_IdOrderBySortOrderAscIdAsc(Long variantId);
    Optional<ProductVariantImage> findByIdAndVariant_Id(Long id, Long variantId);
    boolean existsByImageUrlAndIdNot(String imageUrl, Long id);
}
