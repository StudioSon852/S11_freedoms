package vn.shoegroup.sd11.repository;
import org.springframework.data.jpa.repository.JpaRepository; import vn.shoegroup.sd11.entity.VariantDiscount; import java.util.List;
public interface VariantDiscountRepository extends JpaRepository<VariantDiscount, Long> { List<VariantDiscount> findByProductVariant_IdAndActiveTrueOrderByIdDesc(Long variantId); List<VariantDiscount> findAllByOrderByIdDesc(); }
