package pe.edu.cibertec.bodega.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.cibertec.bodega.model.Venta;

import java.time.LocalDateTime;
import java.util.List;

public interface VentaRepository extends JpaRepository<Venta, Integer> {

    /**
     * CONSULTA 1 del proyecto: ventas por rango de fechas y tienda.
     * Si idTienda llega en null trae todas las tiendas, que es lo que
     * necesita el administrador.
     */
    @Query("SELECT v FROM Venta v "
         + "WHERE v.fecha BETWEEN :desde AND :hasta "
         + "AND (:idTienda IS NULL OR v.tienda.idTienda = :idTienda) "
         + "ORDER BY v.fecha DESC")
    List<Venta> buscarPorFechaYTienda(@Param("desde") LocalDateTime desde,
                                      @Param("hasta") LocalDateTime hasta,
                                      @Param("idTienda") Integer idTienda);

    List<Venta> findByUsuarioIdUsuario(Integer idUsuario);

    /**
     * PANEL: total vendido por mes. Devuelve filas [anio, mes, total].
     * Es la serie de ingresos del grafico de tendencias.
     */
    @Query("SELECT YEAR(v.fecha), MONTH(v.fecha), SUM(v.total) "
         + "FROM Venta v WHERE v.estado = true "
         + "AND (:idTienda IS NULL OR v.tienda.idTienda = :idTienda) "
         + "GROUP BY YEAR(v.fecha), MONTH(v.fecha) "
         + "ORDER BY YEAR(v.fecha), MONTH(v.fecha)")
    List<Object[]> totalPorMes(@Param("idTienda") Integer idTienda);

    /**
     * PANEL: total vendido por tienda, para el grafico comparativo.
     * Devuelve filas [nombreTienda, total].
     */
    @Query("SELECT v.tienda.nombre, SUM(v.total) "
         + "FROM Venta v WHERE v.estado = true "
         + "GROUP BY v.tienda.nombre "
         + "ORDER BY SUM(v.total) DESC")
    List<Object[]> totalPorTienda();
}
