package vn.shoegroup.sd11.repository;
import org.springframework.data.jpa.repository.JpaRepository; import vn.shoegroup.sd11.entity.Color; import java.util.List;
public interface ColorRepository extends JpaRepository<Color, Long> { List<Color> findAllByOrderByColorNameAsc(); }
