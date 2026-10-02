package pe.edu.cibertec.bodega.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.cibertec.bodega.model.*;
import pe.edu.cibertec.bodega.repository.*;

import java.time.LocalDateTime;

/**
 * Ajuste de inventario por conteo fisico (integrante 7).
 *
 * El empleado cuenta lo que hay en el estante y lo registra.
 * Pasos:
 *   1. Validar los datos.
 *   2. Leer el stock del sistema (bloqueando la fila).
 *   3. Calcular la diferencia: fisico - sistema.
 *   4. Dejar el stock igual a lo contado.
 *   5. Guardar el ajuste.
 *   6. Dejar 1 fila en el kardex con tipo AJUSTE.
 *
 * @Transactional: si algo falla, no se guarda nada.
 */
@Service
public class AjusteService {

    @Autowired private AjusteRepository ajusteRepo;
    @Autowired private StockRepository stockRepo;
    @Autowired private ProductoRepository productoRepo;
    @Autowired private TiendaRepository tiendaRepo;
    @Autowired private KardexService kardexService;

    @Transactional
    public Ajuste registrar(Integer idProducto, Integer idTienda, Integer stockFisico,
                            String motivo, Usuario usuario) {

        // ---------- 1. Validaciones ----------
        if (stockFisico == null || stockFisico < 0) {
            throw new RuntimeException("La cantidad contada no puede ser negativa.");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new RuntimeException("Debe indicar el motivo del ajuste.");
        }

        Producto producto = productoRepo.findById(idProducto)
                .orElseThrow(() -> new RuntimeException("El producto no existe."));
        Tienda tienda = tiendaRepo.findById(idTienda)
                .orElseThrow(() -> new RuntimeException("La tienda no existe."));

        // ---------- 2. Stock del sistema (bloqueado mientras dura la transaccion) ----------
        // Si la tienda nunca tuvo el producto, se crea su fila de stock en 0.
        Stock stock = stockRepo.findParaActualizar(idProducto, idTienda)
                .orElseGet(() -> {
                    Stock nuevo = new Stock();
                    nuevo.setProducto(producto);
                    nuevo.setTienda(tienda);
                    nuevo.setCantidad(0);
                    return nuevo;
                });

        // ---------- 3. Diferencia ----------
        int stockSistema = stock.getCantidad();
        int diferencia = stockFisico - stockSistema;

        if (diferencia == 0) {
            throw new RuntimeException("El conteo coincide con el sistema ("
                    + stockSistema + "). No hace falta ajustar.");
        }

        // ---------- 4. El stock queda igual a lo contado ----------
        stock.setCantidad(stockFisico);
        stockRepo.save(stock);

        // ---------- 5. Guardar el ajuste ----------
        Ajuste a = new Ajuste();
        a.setFecha(LocalDateTime.now());
        a.setProducto(producto);
        a.setTienda(tienda);
        a.setStockSistema(stockSistema);
        a.setStockFisico(stockFisico);
        a.setDiferencia(diferencia);
        a.setMotivo(motivo);
        a.setUsuario(usuario);
        a = ajusteRepo.save(a);

        // ---------- 6. Kardex ----------
        // La cantidad del kardex es la diferencia: positiva si sobraba, negativa si faltaba.
        kardexService.registrar(producto, tienda, "AJUSTE",
                stockSistema, diferencia, "Ajuste #" + a.getIdAjuste(), usuario);

        return a;
    }
}
