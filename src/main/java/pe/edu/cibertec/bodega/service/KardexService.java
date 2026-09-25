package pe.edu.cibertec.bodega.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pe.edu.cibertec.bodega.model.*;
import pe.edu.cibertec.bodega.repository.KardexRepository;

import java.time.LocalDateTime;

/**
 * Registra una fila en el kardex (integrante 7).
 *
 * La idea: CADA VEZ que alguien cambia el stock (venta, compra, transferencia,
 * ajuste...) llama a este metodo para dejar el historial.
 * Asi el kardex siempre muestra el stock antes y despues de cada movimiento.
 */
@Service
public class KardexService {

    @Autowired private KardexRepository kardexRepo;

    /**
     * @param stockAntes cantidad que habia ANTES del movimiento
     * @param cantidad   positiva si entra, negativa si sale
     */
    public Kardex registrar(Producto producto, Tienda tienda, String tipo,
                            int stockAntes, int cantidad,
                            String referencia, Usuario usuario) {
        Kardex k = new Kardex();
        k.setFecha(LocalDateTime.now());
        k.setProducto(producto);
        k.setTienda(tienda);
        k.setTipo(tipo);
        k.setCantidad(cantidad);
        k.setStockAntes(stockAntes);
        k.setStockDespues(stockAntes + cantidad);
        k.setReferencia(referencia);
        k.setUsuario(usuario);
        return kardexRepo.save(k);
    }
}
