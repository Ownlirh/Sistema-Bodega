package pe.edu.cibertec.bodega.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.cibertec.bodega.model.Transferencia;

import java.util.List;

public interface TransferenciaRepository extends JpaRepository<Transferencia, Integer> {

    /** Historial de transferencias, las mas recientes primero. */
    List<Transferencia> findAllByOrderByFechaDesc();
}
