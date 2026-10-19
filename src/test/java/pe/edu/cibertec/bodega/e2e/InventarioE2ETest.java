package pe.edu.cibertec.bodega.e2e;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pe.edu.cibertec.bodega.model.Kardex;
import pe.edu.cibertec.bodega.repository.AjusteRepository;
import pe.edu.cibertec.bodega.repository.KardexRepository;
import pe.edu.cibertec.bodega.repository.StockRepository;
import pe.edu.cibertec.bodega.repository.TransferenciaRepository;
import pe.edu.cibertec.bodega.service.AjusteService;
import pe.edu.cibertec.bodega.service.TransferenciaService;

import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Inventario y logistica de punta a punta: transferencias entre tiendas,
 * ajustes por conteo fisico y el rastro que dejan en el kardex.
 *
 * Lo interesante de las transferencias es que tocan DOS tiendas en una sola
 * transaccion: si falla a la mitad, no puede quedar stock evaporado.
 */
class InventarioE2ETest extends BaseE2E {

    @Autowired TransferenciaService transferencias;
    @Autowired AjusteService ajustes;
    @Autowired TransferenciaRepository transferenciaRepo;
    @Autowired AjusteRepository ajusteRepo;
    @Autowired KardexRepository kardexRepo;
    @Autowired StockRepository stockRepo;

    private List<Kardex> kardexOrdenado() {
        var movimientos = kardexRepo.findAll();
        movimientos.sort(Comparator.comparing(Kardex::getIdKardex));
        return movimientos;
    }

    // ------------------------------------------------------------------
    // Transferencias
    // ------------------------------------------------------------------

    @Test
    @DisplayName("una transferencia mueve el stock de una tienda a la otra")
    void transferenciaCompleta() {
        transferencias.registrar(
                datos.getAgua().getIdProducto(),
                datos.getCentral().getIdTienda(),
                datos.getNorte().getIdTienda(),
                5, "Reposicion", datos.getAdmin());

        assertEquals(15, datos.stockDe(datos.getAgua(), datos.getCentral()), "Central: 20 - 5");
        assertEquals(8, datos.stockDe(datos.getAgua(), datos.getNorte()), "Norte: 3 + 5");
        assertEquals(1, transferenciaRepo.count());
    }

    @Test
    @DisplayName("la transferencia no inventa ni pierde unidades")
    void elTotalSeConserva() {
        int antes = datos.stockDe(datos.getAgua(), datos.getCentral())
                  + datos.stockDe(datos.getAgua(), datos.getNorte());

        transferencias.registrar(datos.getAgua().getIdProducto(),
                datos.getCentral().getIdTienda(), datos.getNorte().getIdTienda(),
                7, "Reposicion", datos.getAdmin());

        int despues = datos.stockDe(datos.getAgua(), datos.getCentral())
                    + datos.stockDe(datos.getAgua(), datos.getNorte());

        assertEquals(antes, despues, "mover stock no cambia el total del sistema");
    }

    @Test
    @DisplayName("la transferencia deja dos movimientos de kardex: salida y entrada")
    void transferenciaDejaDosMovimientos() {
        transferencias.registrar(datos.getAgua().getIdProducto(),
                datos.getCentral().getIdTienda(), datos.getNorte().getIdTienda(),
                5, "Reposicion", datos.getAdmin());

        var movimientos = kardexOrdenado();
        assertEquals(2, movimientos.size());

        var salida = movimientos.get(0);
        assertEquals(datos.getCentral().getIdTienda(), salida.getTienda().getIdTienda());
        assertEquals(20, salida.getStockAntes());
        assertEquals(-5, salida.getCantidad());
        assertEquals(15, salida.getStockDespues());

        var entrada = movimientos.get(1);
        assertEquals(datos.getNorte().getIdTienda(), entrada.getTienda().getIdTienda());
        assertEquals(3, entrada.getStockAntes());
        assertEquals(5, entrada.getCantidad());
        assertEquals(8, entrada.getStockDespues());
    }

    @Test
    @DisplayName("sin stock suficiente no se mueve nada en ninguna de las dos tiendas")
    void stockInsuficienteNoDejaRastro() {
        var error = assertThrows(RuntimeException.class, () -> transferencias.registrar(
                datos.getArroz().getIdProducto(),
                datos.getNorte().getIdTienda(),       // Norte tiene 0 de arroz
                datos.getCentral().getIdTienda(),
                1, "Imposible", datos.getAdmin()));

        assertTrue(error.getMessage().contains("Stock insuficiente"));
        assertEquals(0, datos.stockDe(datos.getArroz(), datos.getNorte()));
        assertEquals(50, datos.stockDe(datos.getArroz(), datos.getCentral()));
        assertEquals(0, transferenciaRepo.count());
        assertEquals(0, kardexRepo.count());
    }

    @Test
    @DisplayName("no se puede transferir de una tienda a si misma")
    void rechazaMismaTienda() {
        assertThrows(RuntimeException.class, () -> transferencias.registrar(
                datos.getAgua().getIdProducto(),
                datos.getCentral().getIdTienda(), datos.getCentral().getIdTienda(),
                1, "Absurdo", datos.getAdmin()));

        assertEquals(20, datos.stockDe(datos.getAgua(), datos.getCentral()));
        assertEquals(0, transferenciaRepo.count());
    }

    @Test
    @DisplayName("transferir una cantidad no positiva se rechaza")
    void rechazaCantidadNoPositiva() {
        assertThrows(RuntimeException.class, () -> transferencias.registrar(
                datos.getAgua().getIdProducto(),
                datos.getCentral().getIdTienda(), datos.getNorte().getIdTienda(),
                0, "Cero", datos.getAdmin()));

        assertEquals(0, transferenciaRepo.count());
        assertEquals(0, kardexRepo.count());
    }

    @Test
    @DisplayName("se puede vaciar el stock de origen, pero no pasarse")
    void puedeTransferirTodoPeroNoMas() {
        transferencias.registrar(datos.getAgua().getIdProducto(),
                datos.getCentral().getIdTienda(), datos.getNorte().getIdTienda(),
                20, "Todo", datos.getAdmin());

        assertEquals(0, datos.stockDe(datos.getAgua(), datos.getCentral()));
        assertEquals(23, datos.stockDe(datos.getAgua(), datos.getNorte()));

        // Y una unidad mas ya no entra.
        assertThrows(RuntimeException.class, () -> transferencias.registrar(
                datos.getAgua().getIdProducto(),
                datos.getCentral().getIdTienda(), datos.getNorte().getIdTienda(),
                1, "Una mas", datos.getAdmin()));

        assertEquals(0, datos.stockDe(datos.getAgua(), datos.getCentral()),
                "el stock nunca puede quedar negativo");
    }

    // ------------------------------------------------------------------
    // Ajustes por conteo fisico
    // ------------------------------------------------------------------

    @Test
    @DisplayName("un conteo menor al sistema deja el stock en lo contado, con diferencia negativa")
    void ajusteFaltante() {
        var ajuste = ajustes.registrar(datos.getAgua().getIdProducto(), datos.getCentral().getIdTienda(),
                17, "Conteo mensual", datos.getAdmin());

        assertEquals(20, ajuste.getStockSistema());
        assertEquals(17, ajuste.getStockFisico());
        assertEquals(-3, ajuste.getDiferencia());
        assertEquals(17, datos.stockDe(datos.getAgua(), datos.getCentral()));
    }

    @Test
    @DisplayName("un conteo mayor al sistema sube el stock, con diferencia positiva")
    void ajusteSobrante() {
        var ajuste = ajustes.registrar(datos.getAgua().getIdProducto(), datos.getCentral().getIdTienda(),
                26, "Aparecieron cajas", datos.getAdmin());

        assertEquals(6, ajuste.getDiferencia());
        assertEquals(26, datos.stockDe(datos.getAgua(), datos.getCentral()));
    }

    @Test
    @DisplayName("el ajuste deja en el kardex la diferencia, no el total contado")
    void ajusteDejaKardexConLaDiferencia() {
        ajustes.registrar(datos.getAgua().getIdProducto(), datos.getCentral().getIdTienda(),
                17, "Conteo mensual", datos.getAdmin());

        var movimientos = kardexOrdenado();
        assertEquals(1, movimientos.size());
        assertEquals("AJUSTE", movimientos.get(0).getTipo());
        assertEquals(20, movimientos.get(0).getStockAntes());
        assertEquals(-3, movimientos.get(0).getCantidad());
        assertEquals(17, movimientos.get(0).getStockDespues());
    }

    @Test
    @DisplayName("si el conteo coincide con el sistema se avisa y no se guarda nada")
    void conteoQueCoincideNoAjusta() {
        var error = assertThrows(RuntimeException.class, () -> ajustes.registrar(
                datos.getAgua().getIdProducto(), datos.getCentral().getIdTienda(),
                20, "Conteo mensual", datos.getAdmin()));

        assertTrue(error.getMessage().contains("coincide"));
        assertEquals(0, ajusteRepo.count());
        assertEquals(0, kardexRepo.count());
    }

    @Test
    @DisplayName("un conteo negativo se rechaza")
    void rechazaConteoNegativo() {
        assertThrows(RuntimeException.class, () -> ajustes.registrar(
                datos.getAgua().getIdProducto(), datos.getCentral().getIdTienda(),
                -1, "Imposible", datos.getAdmin()));

        assertEquals(20, datos.stockDe(datos.getAgua(), datos.getCentral()));
        assertEquals(0, ajusteRepo.count());
    }

    @Test
    @DisplayName("un ajuste sin motivo se rechaza")
    void rechazaAjusteSinMotivo() {
        assertThrows(RuntimeException.class, () -> ajustes.registrar(
                datos.getAgua().getIdProducto(), datos.getCentral().getIdTienda(),
                15, "   ", datos.getAdmin()));

        assertEquals(20, datos.stockDe(datos.getAgua(), datos.getCentral()));
        assertEquals(0, ajusteRepo.count());
    }

    // ------------------------------------------------------------------
    // Stock bajo el minimo
    // ------------------------------------------------------------------

    @Test
    @DisplayName("un ajuste puede dejar un producto bajo el minimo y la alerta lo ve")
    void elAjustePuedeDispararLaAlerta() {
        assertTrue(stockRepo.findStockBajo(datos.getCentral().getIdTienda()).isEmpty(),
                "Central arranca sin alertas");

        // AGUA tiene stock minimo 5: dejarlo en 4 lo pone bajo el minimo.
        ajustes.registrar(datos.getAgua().getIdProducto(), datos.getCentral().getIdTienda(),
                4, "Conteo", datos.getAdmin());

        var bajos = stockRepo.findStockBajo(datos.getCentral().getIdTienda());
        assertEquals(1, bajos.size());
        assertEquals(datos.getAgua().getIdProducto(), bajos.get(0).getProducto().getIdProducto());
    }

    @Test
    @DisplayName("una transferencia que repone saca al producto de la alerta")
    void transferirSacaDeLaAlerta() {
        // Norte arranca con AGUA en 3, bajo el minimo de 5.
        assertEquals(2, stockRepo.findStockBajo(datos.getNorte().getIdTienda()).size());

        transferencias.registrar(datos.getAgua().getIdProducto(),
                datos.getCentral().getIdTienda(), datos.getNorte().getIdTienda(),
                10, "Reposicion", datos.getAdmin());

        // Ahora Norte tiene 13 de agua: solo queda el arroz en alerta.
        var bajos = stockRepo.findStockBajo(datos.getNorte().getIdTienda());
        assertEquals(1, bajos.size());
        assertEquals(datos.getArroz().getIdProducto(), bajos.get(0).getProducto().getIdProducto());
    }
}
