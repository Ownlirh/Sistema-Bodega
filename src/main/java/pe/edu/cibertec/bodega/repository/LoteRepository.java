package pe.edu.cibertec.bodega.repository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import pe.edu.cibertec.bodega.model.Lote;
import java.util.*;

public interface LoteRepository extends JpaRepository<Lote, Integer> {
    List<Lote> findByDisponibleGreaterThanOrderByVencimientoAscIdLoteAsc(Integer disponible);
    @Query("select coalesce(sum(l.disponible), 0) from Lote l where l.producto.idProducto = :producto and l.tienda.idTienda = :tienda")
    long cantidadAsignada(@Param("producto") Integer producto, @Param("tienda") Integer tienda);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Lote l where l.idLote = :id")
    Optional<Lote> bloquear(@Param("id") Integer id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Lote l where l.producto.idProducto = :producto and l.tienda.idTienda = :tienda and l.disponible > 0 order by l.vencimiento, l.idLote")
    List<Lote> bloquearDisponibles(@Param("producto") Integer producto, @Param("tienda") Integer tienda);
}
