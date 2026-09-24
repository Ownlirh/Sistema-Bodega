package pe.edu.cibertec.bodega.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.cibertec.bodega.model.Gasto;

import java.time.LocalDate;
import java.util.List;

public interface GastoRepository extends JpaRepository<Gasto, Integer> {

    List<Gasto> findByTiendaIdTienda(Integer idTienda);

    List<Gasto> findByFechaBetween(LocalDate desde, LocalDate hasta);

    /** PANEL: gastos operativos por mes. Devuelve [anio, mes, total]. */
    @Query("SELECT YEAR(g.fecha), MONTH(g.fecha), SUM(g.monto) "
         + "FROM Gasto g "
         + "WHERE (:idTienda IS NULL OR g.tienda.idTienda = :idTienda) "
         + "GROUP BY YEAR(g.fecha), MONTH(g.fecha) "
         + "ORDER BY YEAR(g.fecha), MONTH(g.fecha)")
    List<Object[]> totalPorMes(@Param("idTienda") Integer idTienda);

    /** PANEL: en que se va la plata. Devuelve [categoria, total]. */
    @Query("SELECT g.categoriaGasto.nombre, SUM(g.monto) "
         + "FROM Gasto g "
         + "WHERE (:idTienda IS NULL OR g.tienda.idTienda = :idTienda) "
         + "GROUP BY g.categoriaGasto.nombre "
         + "ORDER BY SUM(g.monto) DESC")
    List<Object[]> totalPorCategoria(@Param("idTienda") Integer idTienda);
}
