package pe.edu.cibertec.bodega.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.cibertec.bodega.model.Auditoria;
import java.util.List;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {
    List<Auditoria> findTop200ByOrderByFechaDesc();
    List<Auditoria> findTop200ByIdTiendaOrderByFechaDesc(Integer idTienda);
}
