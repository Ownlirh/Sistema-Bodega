package pe.edu.cibertec.bodega.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.cibertec.bodega.model.Caja;
import java.util.List;
import java.util.Optional;

public interface CajaRepository extends JpaRepository<Caja, Long> {
    Optional<Caja> findFirstByTiendaIdTiendaAndEstadoOrderByFechaAperturaDesc(Integer idTienda, String estado);
    boolean existsByTiendaIdTiendaAndEstado(Integer idTienda, String estado);
    List<Caja> findTop50ByOrderByFechaAperturaDesc();
    List<Caja> findTop50ByTiendaIdTiendaOrderByFechaAperturaDesc(Integer idTienda);
}
