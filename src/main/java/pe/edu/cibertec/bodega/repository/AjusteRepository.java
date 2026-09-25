package pe.edu.cibertec.bodega.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.cibertec.bodega.model.Ajuste;

import java.util.List;

public interface AjusteRepository extends JpaRepository<Ajuste, Integer> {

    /** Historial de ajustes, los mas recientes primero. */
    List<Ajuste> findAllByOrderByFechaDesc();
}
