package pe.edu.cibertec.bodega.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.cibertec.bodega.model.CompraDetalle;

import java.util.List;

public interface CompraDetalleRepository extends JpaRepository<CompraDetalle, Integer> {

    List<CompraDetalle> findByCompraIdCompra(Integer idCompra);
}
