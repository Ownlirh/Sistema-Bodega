package pe.edu.cibertec.bodega.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.cibertec.bodega.model.Proveedor;
import java.util.List;

public interface ProveedorRepository extends JpaRepository<Proveedor, Integer> {
    List<Proveedor> findByEstadoTrue();
    List<Proveedor> findByRazonSocialContainingIgnoreCase(String texto);
}
