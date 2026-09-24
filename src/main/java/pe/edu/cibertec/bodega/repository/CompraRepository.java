package pe.edu.cibertec.bodega.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.cibertec.bodega.model.Compra;

import java.time.LocalDateTime;
import java.util.List;

public interface CompraRepository extends JpaRepository<Compra, Integer> {

    @Query("SELECT c FROM Compra c "
         + "WHERE c.fecha BETWEEN :desde AND :hasta "
         + "AND (:idTienda IS NULL OR c.tienda.idTienda = :idTienda) "
         + "ORDER BY c.fecha DESC")
    List<Compra> buscarPorFechaYTienda(@Param("desde") LocalDateTime desde,
                                       @Param("hasta") LocalDateTime hasta,
                                       @Param("idTienda") Integer idTienda);

    /** PANEL: egresos por mercaderia, mes a mes. Devuelve [anio, mes, total]. */
    @Query("SELECT YEAR(c.fecha), MONTH(c.fecha), SUM(c.total) "
         + "FROM Compra c WHERE c.estado = true "
         + "AND (:idTienda IS NULL OR c.tienda.idTienda = :idTienda) "
         + "GROUP BY YEAR(c.fecha), MONTH(c.fecha) "
         + "ORDER BY YEAR(c.fecha), MONTH(c.fecha)")
    List<Object[]> totalPorMes(@Param("idTienda") Integer idTienda);
}
