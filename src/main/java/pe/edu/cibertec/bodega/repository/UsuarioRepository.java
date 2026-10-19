package pe.edu.cibertec.bodega.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.cibertec.bodega.model.Usuario;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    /** Lo usa el login del integrante 3. */
    Optional<Usuario> findByUsername(String username);

    /** Empleados de una tienda: lo usa el mantenimiento de empleados. */
    List<Usuario> findByTiendaIdTienda(Integer idTienda);

    long countByTiendaIdTienda(Integer idTienda);

    List<Usuario> findByEstadoTrue();
}
