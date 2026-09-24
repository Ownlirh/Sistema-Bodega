package pe.edu.cibertec.bodega.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.cibertec.bodega.model.Tienda;
import java.util.List;

public interface TiendaRepository extends JpaRepository<Tienda, Integer> {
    List<Tienda> findByEstadoTrue();
}
