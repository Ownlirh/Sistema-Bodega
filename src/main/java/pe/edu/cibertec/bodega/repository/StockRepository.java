package pe.edu.cibertec.bodega.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.cibertec.bodega.model.Stock;

import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Integer> {

    /** Stock de un producto en una tienda. Lectura normal, sin bloqueo. */
    Optional<Stock> findByProductoIdProductoAndTiendaIdTienda(Integer idProducto, Integer idTienda);

    /** Todo el inventario de una tienda: alimenta la consulta de stock. */
    List<Stock> findByTiendaIdTienda(Integer idTienda);

    /**
     * CONSULTA 2 del proyecto: productos por debajo de su stock minimo.
     */
    @Query("SELECT s FROM Stock s "
         + "WHERE s.tienda.idTienda = :idTienda "
         + "AND s.cantidad <= s.producto.stockMinimo "
         + "ORDER BY s.cantidad ASC")
    List<Stock> findStockBajo(@Param("idTienda") Integer idTienda);

    /**
     * ESTA ES LA IMPORTANTE PARA EL CASO TRANSACCIONAL.
     *
     * PESSIMISTIC_WRITE bloquea la fila en la base de datos hasta que termine
     * la transaccion. Si dos cajas venden el mismo producto al mismo tiempo,
     * la segunda espera a que la primera termine en lugar de leer un stock
     * desactualizado y dejarlo en negativo.
     *
     * Usar SIEMPRE este metodo (no el findBy normal) dentro del servicio que
     * registra la venta.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Stock s "
         + "WHERE s.producto.idProducto = :idProducto "
         + "AND s.tienda.idTienda = :idTienda")
    Optional<Stock> findParaActualizar(@Param("idProducto") Integer idProducto,
                                       @Param("idTienda") Integer idTienda);
}
