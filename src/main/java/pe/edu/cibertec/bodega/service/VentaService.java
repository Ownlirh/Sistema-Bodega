package pe.edu.cibertec.bodega.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.cibertec.bodega.model.*;
import pe.edu.cibertec.bodega.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class VentaService {

    @Autowired
    private VentaRepository ventaRepo;

    @Autowired
    private ProductoRepository productoRepo;

    @Autowired
    private TiendaRepository tiendaRepo;

    @Autowired
    private StockRepository stockRepo;

    @Autowired
    private KardexService kardexService;

    // ============================================================
    // REGISTRAR NUEVA VENTA
    // ============================================================
    @Transactional
    public Venta registrar(Integer idTienda,
                           Usuario usuario,
                           List<ItemVenta> productos) {

        if (idTienda == null) {
            throw new RuntimeException(
                    "Debe seleccionar una tienda.");
        }

        if (usuario == null) {
            throw new RuntimeException(
                    "Debe indicar el usuario que registra la venta.");
        }

        if (productos == null || productos.isEmpty()) {
            throw new RuntimeException(
                    "La venta debe tener al menos un producto.");
        }

        Tienda tienda = tiendaRepo.findById(idTienda)
                .orElseThrow(() ->
                        new RuntimeException(
                                "La tienda no existe."));

        if (!Boolean.TRUE.equals(tienda.getEstado())) {
            throw new RuntimeException(
                    "La tienda seleccionada está inactiva.");
        }

        Venta venta = new Venta();

        venta.setFecha(LocalDateTime.now());
        venta.setTienda(tienda);
        venta.setUsuario(usuario);
        venta.setEstado(true);
        venta.setTotal(BigDecimal.ZERO);

        Venta ventaGuardada =
                ventaRepo.saveAndFlush(venta);

        BigDecimal total = BigDecimal.ZERO;

        for (ItemVenta item : productos) {

            if (item.getIdProducto() == null) {
                throw new RuntimeException(
                        "Debe indicar el producto.");
            }

            if (item.getCantidad() == null
                    || item.getCantidad() <= 0) {

                throw new RuntimeException(
                        "La cantidad debe ser mayor a cero.");
            }

            Producto producto =
                    productoRepo.findById(
                            item.getIdProducto())
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "El producto no existe."));

            if (!Boolean.TRUE.equals(producto.getEstado())) {
                throw new RuntimeException(
                        "El producto "
                        + producto.getDescripcion()
                        + " está inactivo.");
            }

            Stock stock =
                    stockRepo.findParaActualizar(
                            producto.getIdProducto(),
                            tienda.getIdTienda())
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "El producto "
                                    + producto.getDescripcion()
                                    + " no tiene stock registrado en "
                                    + tienda.getNombre() + "."));

            int stockAntes = stock.getCantidad();

            if (stockAntes < item.getCantidad()) {
                throw new RuntimeException(
                        "Stock insuficiente para "
                        + producto.getDescripcion()
                        + ". Disponible: "
                        + stockAntes
                        + ". Solicitado: "
                        + item.getCantidad());
            }

            BigDecimal subtotal =
                    producto.getPrecioVenta()
                            .multiply(
                                    BigDecimal.valueOf(
                                            item.getCantidad()));

            VentaDetalle detalle =
                    new VentaDetalle();

            detalle.setProducto(producto);
            detalle.setCantidad(item.getCantidad());
            detalle.setPrecioUnitario(
                    producto.getPrecioVenta());
            detalle.setSubtotal(subtotal);

            ventaGuardada.agregarDetalle(detalle);

            stock.setCantidad(
                    stockAntes - item.getCantidad());

            stockRepo.save(stock);

            kardexService.registrar(
                    producto,
                    tienda,
                    "VENTA",
                    stockAntes,
                    -item.getCantidad(),
                    "Venta #"
                            + ventaGuardada.getIdVenta(),
                    usuario);

            total = total.add(subtotal);
        }

        ventaGuardada.setTotal(total);

        return ventaGuardada;
    }

    // ============================================================
    // EDITAR VENTA
    // ============================================================
    @Transactional
    public Venta editar(Integer idVenta,
                        Integer idTienda,
                        List<ItemVenta> productos) {

        if (idVenta == null) {
            throw new RuntimeException(
                    "Debe indicar la venta a editar.");
        }

        if (idTienda == null) {
            throw new RuntimeException(
                    "Debe seleccionar una tienda.");
        }

        if (productos == null || productos.isEmpty()) {
            throw new RuntimeException(
                    "La venta debe tener al menos un producto.");
        }

        Venta venta =
                ventaRepo.findById(idVenta)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "La venta no existe."));

        if (!Boolean.TRUE.equals(venta.getEstado())) {
            throw new RuntimeException(
                    "No se puede editar una venta anulada.");
        }

        Tienda tiendaNueva =
                tiendaRepo.findById(idTienda)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "La tienda no existe."));

        if (!Boolean.TRUE.equals(tiendaNueva.getEstado())) {
            throw new RuntimeException(
                    "La tienda seleccionada está inactiva.");
        }

        Tienda tiendaOriginal =
                venta.getTienda();

        // --------------------------------------------------------
        // DEVOLVER STOCK ORIGINAL
        // --------------------------------------------------------
        for (VentaDetalle detalleOriginal
                : List.copyOf(venta.getDetalles())) {

            Producto productoOriginal =
                    detalleOriginal.getProducto();

            Stock stockOriginal =
                    stockRepo.findParaActualizar(
                            productoOriginal.getIdProducto(),
                            tiendaOriginal.getIdTienda())
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "No existe stock para "
                                    + productoOriginal.getDescripcion()
                                    + " en "
                                    + tiendaOriginal.getNombre()
                                    + "."));

            int stockAntes =
                    stockOriginal.getCantidad();

            stockOriginal.setCantidad(
                    stockAntes
                    + detalleOriginal.getCantidad());

            stockRepo.save(stockOriginal);

            kardexService.registrar(
                    productoOriginal,
                    tiendaOriginal,
                    "DEVOLUCION",
                    stockAntes,
                    detalleOriginal.getCantidad(),
                    "Edición Venta #"
                            + venta.getIdVenta(),
                    venta.getUsuario());
        }

        // --------------------------------------------------------
        // ELIMINAR DETALLES ANTERIORES
        // --------------------------------------------------------
        venta.getDetalles().clear();

        // --------------------------------------------------------
        // APLICAR NUEVOS PRODUCTOS
        // --------------------------------------------------------
        BigDecimal nuevoTotal =
                BigDecimal.ZERO;

        for (ItemVenta item : productos) {

            if (item.getIdProducto() == null) {
                throw new RuntimeException(
                        "Debe indicar el producto.");
            }

            if (item.getCantidad() == null
                    || item.getCantidad() <= 0) {

                throw new RuntimeException(
                        "La cantidad debe ser mayor a cero.");
            }

            Producto producto =
                    productoRepo.findById(
                            item.getIdProducto())
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "El producto no existe."));

            if (!Boolean.TRUE.equals(producto.getEstado())) {
                throw new RuntimeException(
                        "El producto "
                        + producto.getDescripcion()
                        + " está inactivo.");
            }

            Stock stock =
                    stockRepo.findParaActualizar(
                            producto.getIdProducto(),
                            tiendaNueva.getIdTienda())
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "El producto "
                                    + producto.getDescripcion()
                                    + " no tiene stock registrado en "
                                    + tiendaNueva.getNombre()
                                    + "."));

            int stockAntes =
                    stock.getCantidad();

            if (stockAntes < item.getCantidad()) {
                throw new RuntimeException(
                        "Stock insuficiente para "
                        + producto.getDescripcion()
                        + ". Disponible: "
                        + stockAntes
                        + ". Solicitado: "
                        + item.getCantidad());
            }

            BigDecimal subtotal =
                    producto.getPrecioVenta()
                            .multiply(
                                    BigDecimal.valueOf(
                                            item.getCantidad()));

            VentaDetalle nuevoDetalle =
                    new VentaDetalle();

            nuevoDetalle.setProducto(producto);
            nuevoDetalle.setCantidad(
                    item.getCantidad());
            nuevoDetalle.setPrecioUnitario(
                    producto.getPrecioVenta());
            nuevoDetalle.setSubtotal(subtotal);

            venta.agregarDetalle(nuevoDetalle);

            stock.setCantidad(
                    stockAntes - item.getCantidad());

            stockRepo.save(stock);

            kardexService.registrar(
                    producto,
                    tiendaNueva,
                    "VENTA",
                    stockAntes,
                    -item.getCantidad(),
                    "Edición Venta #"
                            + venta.getIdVenta(),
                    venta.getUsuario());

            nuevoTotal =
                    nuevoTotal.add(subtotal);
        }

        venta.setTienda(tiendaNueva);
        venta.setTotal(nuevoTotal);

        return ventaRepo.save(venta);
    }

    // ============================================================
    // ANULAR VENTA
    // ============================================================
    @Transactional
    public Venta anular(Integer idVenta) {

        if (idVenta == null) {
            throw new RuntimeException(
                    "Debe indicar la venta a anular.");
        }

        Venta venta =
                ventaRepo.findById(idVenta)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "La venta no existe."));

        if (!Boolean.TRUE.equals(venta.getEstado())) {
            throw new RuntimeException(
                    "La venta ya está anulada.");
        }

        Tienda tienda =
                venta.getTienda();

        /*
         * Devolvemos al stock todos los productos
         * que formaban parte de la venta.
         */
        for (VentaDetalle detalle
                : venta.getDetalles()) {

            Producto producto =
                    detalle.getProducto();

            Stock stock =
                    stockRepo.findParaActualizar(
                            producto.getIdProducto(),
                            tienda.getIdTienda())
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "No existe stock para "
                                    + producto.getDescripcion()
                                    + " en "
                                    + tienda.getNombre()
                                    + "."));

            int stockAntes =
                    stock.getCantidad();

            stock.setCantidad(
                    stockAntes + detalle.getCantidad());

            stockRepo.save(stock);

            /*
             * No borramos el Kardex anterior.
             * Agregamos la devolución de la anulación.
             */
            kardexService.registrar(
                    producto,
                    tienda,
                    "DEVOLUCION",
                    stockAntes,
                    detalle.getCantidad(),
                    "Anulación Venta #"
                            + venta.getIdVenta(),
                    venta.getUsuario());
        }

        /*
         * La venta permanece registrada.
         * Solo cambia su estado.
         */
        venta.setEstado(false);

        return ventaRepo.save(venta);
    }


    // ============================================================
    // ITEM DE VENTA
    // ============================================================
    public static class ItemVenta {

        private Integer idProducto;
        private Integer cantidad;

        public ItemVenta() {
        }

        public ItemVenta(Integer idProducto,
                         Integer cantidad) {

            this.idProducto = idProducto;
            this.cantidad = cantidad;
        }

        public Integer getIdProducto() {
            return idProducto;
        }

        public void setIdProducto(Integer idProducto) {
            this.idProducto = idProducto;
        }

        public Integer getCantidad() {
            return cantidad;
        }

        public void setCantidad(Integer cantidad) {
            this.cantidad = cantidad;
        }
    }
}