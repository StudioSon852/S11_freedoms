package vn.shoegroup.sd11.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.shoegroup.sd11.entity.HeldInvoice;
import java.util.List;
import java.util.Optional;

public interface HeldInvoiceRepository extends JpaRepository<HeldInvoice, Long> {
    List<HeldInvoice> findByStatusOrderByCreatedAtDesc(String status);
    List<HeldInvoice> findByCreatedBy_IdAndStatusOrderByCreatedAtDesc(Long staffId, String status);
    Optional<HeldInvoice> findByIdAndCreatedBy_Id(Long id, Long staffId);
    long countByCreatedBy_IdAndStatus(Long staffId, String status);
    long countByStatus(String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from HeldInvoice h where h.id = :id")
    Optional<HeldInvoice> findByIdForUpdate(@Param("id") Long id);
}
