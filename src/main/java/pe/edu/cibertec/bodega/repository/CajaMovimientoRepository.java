package pe.edu.cibertec.bodega.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.cibertec.bodega.model.CajaMovimiento;
import java.util.List;

public interface CajaMovimientoRepository extends JpaRepository<CajaMovimiento, Long> {
    List<CajaMovimiento> findByCajaIdCajaOrderByFechaDesc(Long idCaja);
    List<CajaMovimiento> findByCajaIdCaja(Long idCaja);
}
