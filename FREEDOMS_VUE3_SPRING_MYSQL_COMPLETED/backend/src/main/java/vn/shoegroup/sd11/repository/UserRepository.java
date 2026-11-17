package vn.shoegroup.sd11.repository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.shoegroup.sd11.entity.AppUser;
import java.util.List; import java.util.Optional;
public interface UserRepository extends JpaRepository<AppUser, Long> {
 Optional<AppUser> findByEmailIgnoreCase(String email); Optional<AppUser> findFirstByPhone(String phone);
 List<AppUser> findByRole_RoleCodeOrderByIdDesc(String roleCode); List<AppUser> findAllByOrderByIdDesc(); boolean existsByEmailIgnoreCase(String email);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from AppUser u where u.id = :id")
    Optional<AppUser> findByIdForUpdate(@Param("id") Long id);
}
