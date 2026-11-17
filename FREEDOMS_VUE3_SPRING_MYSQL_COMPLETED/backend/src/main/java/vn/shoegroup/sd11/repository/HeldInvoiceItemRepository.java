package vn.shoegroup.sd11.repository;
import org.springframework.data.jpa.repository.JpaRepository; import vn.shoegroup.sd11.entity.HeldInvoiceItem; import java.util.List;
public interface HeldInvoiceItemRepository extends JpaRepository<HeldInvoiceItem, Long> { List<HeldInvoiceItem> findByHeldInvoice_IdOrderByIdAsc(Long heldInvoiceId); void deleteByHeldInvoice_Id(Long heldInvoiceId); }
