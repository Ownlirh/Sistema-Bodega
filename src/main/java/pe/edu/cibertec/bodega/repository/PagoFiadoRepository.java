
package pe.edu.cibertec.bodega.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.cibertec.bodega.model.PagoFiado;

public interface PagoFiadoRepository extends JpaRepository<PagoFiado, Integer> {

    List<PagoFiado> findByFiadoIdFiadoOrderByFechaDesc(Integer idFiado);

}
