package vn.shoegroup.sd11.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.shoegroup.sd11.entity.Role;
import java.util.Optional;
public interface RoleRepository extends JpaRepository<Role, Long> { Optional<Role> findByRoleCode(String roleCode); }
