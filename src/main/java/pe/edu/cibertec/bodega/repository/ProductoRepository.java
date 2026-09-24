package pe.edu.cibertec.bodega.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.cibertec.bodega.model.Producto;
import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Integer> {

    Optional<Producto> findByCodigo(String codigo);

    List<Producto> findByEstadoTrue();

    List<Producto> findByDescripcionContainingIgnoreCase(String texto);

    List<Producto> findByCategoriaIdCategoria(Integer idCategoria);
}
