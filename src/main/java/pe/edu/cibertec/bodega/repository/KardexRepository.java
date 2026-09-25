package pe.edu.cibertec.bodega.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.cibertec.bodega.model.Kardex;

import java.util.List;

public interface KardexRepository extends JpaRepository<Kardex, Integer> {

    /** Kardex de un producto en una tienda, del mas antiguo al mas nuevo. */
    List<Kardex> findByProductoIdProductoAndTiendaIdTiendaOrderByFechaAsc(Integer idProducto, Integer idTienda);

    /** Todos los movimientos de una tienda, los mas recientes primero. */
    List<Kardex> findByTiendaIdTiendaOrderByFechaDesc(Integer idTienda);
}
