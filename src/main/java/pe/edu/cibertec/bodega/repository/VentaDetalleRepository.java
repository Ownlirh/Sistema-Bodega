package pe.edu.cibertec.bodega.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pe.edu.cibertec.bodega.model.VentaDetalle;

import java.util.List;

public interface VentaDetalleRepository extends JpaRepository<VentaDetalle, Integer> {

    List<VentaDetalle> findByVentaIdVenta(Integer idVenta);

    /**
     * REPORTE 2 y PANEL: rentabilidad por producto.
     *
     * Devuelve filas [descripcion, unidadesVendidas, ingreso, costo, ganancia].
     *
     * OJO: el costo usa el precio de compra ACTUAL del producto, no el que
     * tenia el dia de la venta. Es una aproximacion suficiente para el curso.
     * Si quisieramos exactitud historica habria que guardar tambien el costo
     * dentro de tb_venta_detalle.
     */
    @Query("SELECT d.producto.descripcion, "
         + "SUM(d.cantidad), "
         + "SUM(d.subtotal), "
         + "SUM(d.cantidad * d.producto.precioCompra), "
         + "SUM(d.subtotal) - SUM(d.cantidad * d.producto.precioCompra) "
         + "FROM VentaDetalle d "
         + "WHERE d.venta.estado = true "
         + "GROUP BY d.producto.idProducto, d.producto.descripcion "
         + "ORDER BY SUM(d.subtotal) - SUM(d.cantidad * d.producto.precioCompra) DESC")
    List<Object[]> rentabilidadPorProducto();
}
