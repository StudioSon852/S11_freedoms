package vn.shoegroup.sd11.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.shoegroup.sd11.entity.MemberTier;
import java.util.List; import java.util.Optional;
public interface MemberTierRepository extends JpaRepository<MemberTier, Long> { Optional<MemberTier> findByTierCode(String tierCode); List<MemberTier> findAllByOrderByMinSpendingAsc(); }
