package pe.edu.cibertec.bodega.e2e;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pe.edu.cibertec.bodega.dto.CompraForm;
import pe.edu.cibertec.bodega.dto.GastoForm;
import pe.edu.cibertec.bodega.repository.*;
import pe.edu.cibertec.bodega.service.CompraService;
import pe.edu.cibertec.bodega.service.GastoService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Compras y gastos de punta a punta: servicio real, transaccion real, base real.
 *
 * Lo que interesa comprobar no es que guarde, sino los efectos de costado: que
 * el stock suba, que quede el rastro en el kardex y que un fallo no deje la
 * base a medias.
 */
class ComprasGastosE2ETest extends BaseE2E {

    @Autowired CompraService compras;
    @Autowired GastoService gastos;
    @Autowired CompraRepository compraRepo;
    @Autowired GastoRepository gastoRepo;
    @Autowired KardexRepository kardexRepo;
    @Autowired StockRepository stockRepo;

    private CompraForm compraDe(Integer idTienda, Integer idProducto, int cantidad, String costo) {
        CompraForm form = new CompraForm();
        form.setIdProveedor(datos.getProveedor().getIdProveedor());
        form.setIdTienda(idTienda);
        CompraForm.Linea linea = new CompraForm.Linea();
        linea.setIdProducto(idProducto);
        linea.setCantidad(cantidad);
        linea.setCosto(new BigDecimal(costo));
        form.setLineas(List.of(linea));
        return form;
    }

    // ------------------------------------------------------------------
    // Compras
    // ------------------------------------------------------------------

    @Test
    @DisplayName("una compra sube el stock, calcula el total y deja kardex")
    void compraCompleta() {
        var compra = compras.registrar(
                compraDe(datos.getCentral().getIdTienda(), datos.getAgua().getIdProducto(), 30, "1.20"),
                datos.getAdmin().getIdUsuario());

        // 30 x 1.20 = 36.00
        assertEquals(0, new BigDecimal("36.00").compareTo(compra.getTotal()));

        // El stock de Central pasa de 20 a 50.
        assertEquals(50, datos.stockDe(datos.getAgua(), datos.getCentral()));

        // Y el kardex guarda el antes y el despues.
        var movimientos = kardexRepo.findAll();
        assertEquals(1, movimientos.size());
        var k = movimientos.get(0);
        assertEquals("COMPRA", k.getTipo());
        assertEquals(20, k.getStockAntes());
        assertEquals(30, k.getCantidad());
        assertEquals(50, k.getStockDespues());
    }

    @Test
    @DisplayName("comprar para una tienda sin stock previo crea la fila en cero y suma")
    void compraCreaElStockSiNoExistia() {
        // Nadie borro la fila, pero Norte arranca ARROZ en 0: sirve igual para
        // comprobar que el alta sobre una tienda sin movimiento funciona.
        stockRepo.deleteAll(stockRepo.findByTiendaIdTienda(datos.getNorte().getIdTienda()));

        compras.registrar(
                compraDe(datos.getNorte().getIdTienda(), datos.getArroz().getIdProducto(), 12, "3.00"),
                datos.getAdmin().getIdUsuario());

        assertEquals(12, datos.stockDe(datos.getArroz(), datos.getNorte()));
    }

    @Test
    @DisplayName("un producto repetido en la misma compra se rechaza sin guardar nada")
    void rechazaProductoDuplicado() {
        CompraForm form = compraDe(datos.getCentral().getIdTienda(), datos.getAgua().getIdProducto(), 5, "1.00");
        CompraForm.Linea repetida = new CompraForm.Linea();
        repetida.setIdProducto(datos.getAgua().getIdProducto());
        repetida.setCantidad(3);
        repetida.setCosto(new BigDecimal("1.00"));
        form.setLineas(List.of(form.getLineas().get(0), repetida));

        assertThrows(IllegalArgumentException.class,
                () -> compras.registrar(form, datos.getAdmin().getIdUsuario()));

        // Nada tocado: ni compra, ni stock, ni kardex.
        assertEquals(0, compraRepo.count());
        assertEquals(0, kardexRepo.count());
        assertEquals(20, datos.stockDe(datos.getAgua(), datos.getCentral()));
    }

    @Test
    @DisplayName("un manager no puede comprar para otra tienda")
    void managerNoCompraParaOtraTienda() {
        var error = assertThrows(IllegalArgumentException.class, () -> compras.registrar(
                compraDe(datos.getNorte().getIdTienda(), datos.getAgua().getIdProducto(), 5, "1.00"),
                datos.getManagerCentral().getIdUsuario()));

        assertTrue(error.getMessage().contains("no pertenece"));
        assertEquals(0, compraRepo.count());
        assertEquals(3, datos.stockDe(datos.getAgua(), datos.getNorte()), "el stock de Norte no se toco");
    }

    @Test
    @DisplayName("una cantidad invalida no llega a tocar el stock")
    void rechazaCantidadInvalida() {
        assertThrows(IllegalArgumentException.class, () -> compras.registrar(
                compraDe(datos.getCentral().getIdTienda(), datos.getAgua().getIdProducto(), 0, "1.00"),
                datos.getAdmin().getIdUsuario()));

        assertEquals(20, datos.stockDe(datos.getAgua(), datos.getCentral()));
        assertEquals(0, compraRepo.count());
    }

    @Test
    @DisplayName("dos compras seguidas acumulan sobre el mismo stock")
    void dosComprasAcumulan() {
        var id = datos.getAgua().getIdProducto();
        compras.registrar(compraDe(datos.getCentral().getIdTienda(), id, 10, "1.00"),
                datos.getAdmin().getIdUsuario());
        compras.registrar(compraDe(datos.getCentral().getIdTienda(), id, 5, "1.00"),
                datos.getAdmin().getIdUsuario());

        assertEquals(35, datos.stockDe(datos.getAgua(), datos.getCentral()));
        assertEquals(2, compraRepo.count());
        assertEquals(2, kardexRepo.count());

        // El segundo movimiento arranca donde termino el primero.
        var movimientos = kardexRepo.findAll();
        movimientos.sort((a, b) -> a.getIdKardex().compareTo(b.getIdKardex()));
        assertEquals(20, movimientos.get(0).getStockAntes());
        assertEquals(30, movimientos.get(0).getStockDespues());
        assertEquals(30, movimientos.get(1).getStockAntes());
        assertEquals(35, movimientos.get(1).getStockDespues());
    }

    // ------------------------------------------------------------------
    // Gastos
    // ------------------------------------------------------------------

    private GastoForm gastoDe(Integer idTienda, String monto, LocalDate fecha) {
        GastoForm form = new GastoForm();
        form.setIdTienda(idTienda);
        form.setIdCategoriaGasto(datos.getServicios().getIdCategoriaGasto());
        form.setFecha(fecha);
        form.setMonto(new BigDecimal(monto));
        form.setDescripcion("Recibo de luz");
        return form;
    }

    @Test
    @DisplayName("un gasto queda guardado con su importe y categoria")
    void gastoCompleto() {
        var gasto = gastos.registrar(
                gastoDe(datos.getCentral().getIdTienda(), "125.50", LocalDate.now().minusDays(1)),
                datos.getAdmin().getIdUsuario());

        assertNotNull(gasto.getIdGasto());
        assertEquals(0, new BigDecimal("125.50").compareTo(gastoRepo.findById(gasto.getIdGasto()).orElseThrow().getMonto()));
        assertEquals(1, gastoRepo.count());
    }

    @Test
    @DisplayName("un gasto con fecha futura se rechaza")
    void rechazaGastoFuturo() {
        assertThrows(IllegalArgumentException.class, () -> gastos.registrar(
                gastoDe(datos.getCentral().getIdTienda(), "10.00", LocalDate.now().plusDays(2)),
                datos.getAdmin().getIdUsuario()));

        assertEquals(0, gastoRepo.count());
    }

    @Test
    @DisplayName("un gasto no afecta el stock: no es mercaderia")
    void elGastoNoTocaElStock() {
        gastos.registrar(gastoDe(datos.getCentral().getIdTienda(), "80.00", LocalDate.now()),
                datos.getAdmin().getIdUsuario());

        assertEquals(20, datos.stockDe(datos.getAgua(), datos.getCentral()));
        assertEquals(0, kardexRepo.count());
    }
}
