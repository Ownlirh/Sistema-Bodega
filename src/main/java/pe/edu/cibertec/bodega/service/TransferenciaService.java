package pe.edu.cibertec.bodega.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.cibertec.bodega.model.*;
import pe.edu.cibertec.bodega.repository.*;

import java.time.LocalDateTime;

/**
 * Transferencia de stock entre tiendas (integrante 7).
 *
 * Pasos:
 *   1. Validar los datos.
 *   2. Restar el stock en la tienda ORIGEN.
 *   3. Sumar el stock en la tienda DESTINO.
 *   4. Guardar la transferencia.
 *   5. Dejar 2 filas en el kardex (salida y entrada).
 *
 * @Transactional: si CUALQUIER paso falla, se deshace TODO.
 * Nunca queda el stock restado en una tienda sin sumarse en la otra.
 */
@Service
public class TransferenciaService {

    @Autowired private TransferenciaRepository transferenciaRepo;
    @Autowired private StockRepository stockRepo;
    @Autowired private ProductoRepository productoRepo;
    @Autowired private TiendaRepository tiendaRepo;
    @Autowired private KardexService kardexService;

    @Transactional
    public Transferencia registrar(Integer idProducto, Integer idOrigen, Integer idDestino,
                                   Integer cantidad, String observacion, Usuario usuario) {

        // ---------- 1. Validaciones ----------
        if (cantidad == null || cantidad <= 0) {
            throw new RuntimeException("La cantidad debe ser mayor a cero.");
        }
        if (idOrigen.equals(idDestino)) {
            throw new RuntimeException("La tienda origen y destino no pueden ser la misma.");
        }

        Producto producto = productoRepo.findById(idProducto)
                .orElseThrow(() -> new RuntimeException("El producto no existe."));
        Tienda origen = tiendaRepo.findById(idOrigen)
                .orElseThrow(() -> new RuntimeException("La tienda origen no existe."));
        Tienda destino = tiendaRepo.findById(idDestino)
                .orElseThrow(() -> new RuntimeException("La tienda destino no existe."));

        // findParaActualizar bloquea la fila mientras dura la transaccion,
        // asi nadie mas puede vender ese producto al mismo tiempo.
        Stock stockOrigen = stockRepo.findParaActualizar(idProducto, idOrigen)
                .orElseThrow(() -> new RuntimeException("La tienda origen no tiene ese producto."));

        if (stockOrigen.getCantidad() < cantidad) {
            throw new RuntimeException("Stock insuficiente en " + origen.getNombre()
                    + ". Disponible: " + stockOrigen.getCantidad());
        }

        // Si la tienda destino nunca tuvo el producto, se crea su fila de stock en 0.
        Stock stockDestino = stockRepo.findParaActualizar(idProducto, idDestino)
                .orElseGet(() -> {
                    Stock nuevo = new Stock();
                    nuevo.setProducto(producto);
                    nuevo.setTienda(destino);
                    nuevo.setCantidad(0);
                    return nuevo;
                });

        // ---------- 2 y 3. Mover el stock ----------
        int antesOrigen  = stockOrigen.getCantidad();
        int antesDestino = stockDestino.getCantidad();

        stockOrigen.setCantidad(antesOrigen - cantidad);
        stockDestino.setCantidad(antesDestino + cantidad);
        stockRepo.save(stockOrigen);
        stockRepo.save(stockDestino);

        // ---------- 4. Guardar la transferencia ----------
        Transferencia t = new Transferencia();
        t.setFecha(LocalDateTime.now());
        t.setProducto(producto);
        t.setTiendaOrigen(origen);
        t.setTiendaDestino(destino);
        t.setCantidad(cantidad);
        t.setObservacion(observacion);
        t.setUsuario(usuario);
        t = transferenciaRepo.save(t);

        // ---------- 5. Kardex: una salida y una entrada ----------
        String ref = "Transferencia #" + t.getIdTransferencia();
        kardexService.registrar(producto, origen,  "TRANSFERENCIA_SALIDA",
                antesOrigen, -cantidad, ref, usuario);
        kardexService.registrar(producto, destino, "TRANSFERENCIA_ENTRADA",
                antesDestino, cantidad, ref, usuario);

        return t;
    }
}
