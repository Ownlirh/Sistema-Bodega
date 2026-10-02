package pe.edu.cibertec.bodega.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.cibertec.bodega.dto.CompraForm;
import pe.edu.cibertec.bodega.model.*;
import pe.edu.cibertec.bodega.repository.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CompraService {
    private final CompraRepository compras;
    private final ProveedorRepository proveedores;
    private final TiendaRepository tiendas;
    private final UsuarioRepository usuarios;
    private final StockRepository stocks;
    private final KardexService kardex;
    private final EntityManager em;
    private final Validator validator;

    @Transactional
    public Compra registrar(CompraForm form, Integer idUsuario) {
        if (form == null || !validator.validate(form).isEmpty())
            throw new IllegalArgumentException("Complete proveedor, tienda y productos con cantidades y costos válidos.");
        var proveedor = proveedores.findById(form.getIdProveedor())
                .filter(p -> Boolean.TRUE.equals(p.getEstado()))
                .orElseThrow(() -> new IllegalArgumentException("Seleccione un proveedor activo."));
        var tienda = tiendas.findById(form.getIdTienda())
                .filter(t -> Boolean.TRUE.equals(t.getEstado()))
                .orElseThrow(() -> new IllegalArgumentException("Seleccione una tienda activa."));
        var usuario = usuarios.findById(idUsuario)
                .filter(u -> Boolean.TRUE.equals(u.getEstado()))
                .orElseThrow(() -> new IllegalArgumentException("El usuario no está disponible."));
        if (usuario.getTienda() != null && !usuario.getTienda().getIdTienda().equals(tienda.getIdTienda()))
            throw new IllegalArgumentException("El usuario no pertenece a esta tienda.");
        Set<Integer> ids = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;
        for (var linea : form.getLineas()) {
            if (!ids.add(linea.getIdProducto()))
                throw new IllegalArgumentException("Cada producto debe aparecer una sola vez.");
            total = total.add(linea.getCosto().multiply(BigDecimal.valueOf(linea.getCantidad())));
        }
        if (total.compareTo(new BigDecimal("99999999.99")) > 0)
            throw new IllegalArgumentException("El total supera el importe permitido.");
        Compra compra = new Compra();
        compra.setFecha(LocalDateTime.now());
        compra.setProveedor(proveedor);
        compra.setTienda(tienda);
        compra.setUsuario(usuario);
        compra.setTotal(total.setScale(2));
        // Orden estable de bloqueo para compras simultáneas con varios productos.
        var lineas = new ArrayList<>(form.getLineas());
        lineas.sort(Comparator.comparing(CompraForm.Linea::getIdProducto));
        Map<Integer, Integer> anteriores = new HashMap<>();
        for (var linea : lineas) {
            // Bloquear también el producto serializa la creación del primer stock entre compras.
            Producto producto = em.find(Producto.class, linea.getIdProducto(), LockModeType.PESSIMISTIC_WRITE);
            if (producto == null || !Boolean.TRUE.equals(producto.getEstado()))
                throw new IllegalArgumentException("Seleccione productos activos.");
            Stock stock = stocks.findParaActualizar(producto.getIdProducto(), tienda.getIdTienda())
                    .orElseGet(() -> {
                        Stock nuevo = new Stock();
                        nuevo.setProducto(producto); nuevo.setTienda(tienda); nuevo.setCantidad(0);
                        return nuevo;
                    });
            int antes = stock.getCantidad();
            if ((long) antes + linea.getCantidad() > Integer.MAX_VALUE)
                throw new IllegalArgumentException("La cantidad supera el stock permitido.");
            anteriores.put(producto.getIdProducto(), antes);
            stock.setCantidad(antes + linea.getCantidad());
            stocks.save(stock);
            CompraDetalle detalle = new CompraDetalle();
            detalle.setProducto(producto); detalle.setCantidad(linea.getCantidad());
            detalle.setPrecioUnitario(linea.getCosto().setScale(2));
            detalle.setSubtotal(linea.getCosto().multiply(BigDecimal.valueOf(linea.getCantidad())).setScale(2));
            compra.agregarDetalle(detalle);
        }
        compras.saveAndFlush(compra);
        for (var detalle : compra.getDetalles())
            kardex.registrar(detalle.getProducto(), tienda, "COMPRA",
                    anteriores.get(detalle.getProducto().getIdProducto()), detalle.getCantidad(),
                    "Compra #" + compra.getIdCompra(), usuario);
        return compra;
    }
}
