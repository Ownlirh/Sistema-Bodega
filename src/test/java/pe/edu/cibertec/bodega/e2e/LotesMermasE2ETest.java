package pe.edu.cibertec.bodega.e2e;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pe.edu.cibertec.bodega.dto.LoteForm;
import pe.edu.cibertec.bodega.dto.MermaForm;
import pe.edu.cibertec.bodega.model.Lote;
import pe.edu.cibertec.bodega.model.Merma;
import pe.edu.cibertec.bodega.repository.KardexRepository;
import pe.edu.cibertec.bodega.repository.LoteRepository;
import pe.edu.cibertec.bodega.repository.MermaRepository;
import pe.edu.cibertec.bodega.service.LoteService;
import pe.edu.cibertec.bodega.service.MermaService;
import pe.edu.cibertec.bodega.service.TransferenciaService;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lotes y mermas de punta a punta.
 *
 * La regla de fondo: los lotes son una particion del stock, no algo paralelo.
 * No se puede asignar a lotes mas de lo que hay en stock, y una merma tiene
 * que bajar las dos cosas a la vez.
 */
class LotesMermasE2ETest extends BaseE2E {

    @Autowired LoteService lotes;
    @Autowired MermaService mermas;
    @Autowired TransferenciaService transferencias;
    @Autowired LoteRepository loteRepo;
    @Autowired MermaRepository mermaRepo;
    @Autowired KardexRepository kardexRepo;

    private LoteForm loteDe(Integer idProducto, Integer idTienda, String codigo,
                            LocalDate vence, int cantidad) {
        LoteForm form = new LoteForm();
        form.setIdProducto(idProducto);
        form.setIdTienda(idTienda);
        form.setCodigo(codigo);
        form.setVencimiento(vence);
        form.setCantidad(cantidad);
        return form;
    }

    private Lote loteEnCentral(String codigo, LocalDate vence, int cantidad) {
        return lotes.registrar(
                loteDe(datos.getAgua().getIdProducto(), datos.getCentral().getIdTienda(),
                        codigo, vence, cantidad),
                datos.getAdmin().getIdUsuario());
    }

    private MermaForm mermaDe(Integer idLote, int cantidad, Merma.Motivo motivo) {
        MermaForm form = new MermaForm();
        form.setIdLote(idLote);
        form.setCantidad(cantidad);
        form.setMotivo(motivo);
        form.setDescripcion("Envase roto");
        return form;
    }

    // ------------------------------------------------------------------
    // Lotes
    // ------------------------------------------------------------------

    @Test
    @DisplayName("asignar un lote no cambia el stock: solo lo etiqueta")
    void elLoteNoSumaStock() {
        loteEnCentral("L-001", LocalDate.now().plusMonths(6), 10);

        assertEquals(20, datos.stockDe(datos.getAgua(), datos.getCentral()),
                "el lote reparte el stock que ya habia, no agrega");
        assertEquals(1, loteRepo.count());
    }

    @Test
    @DisplayName("no se puede asignar a lotes mas de lo que hay en stock")
    void noSePuedeAsignarDeMas() {
        var error = assertThrows(IllegalArgumentException.class,
                () -> loteEnCentral("L-001", LocalDate.now().plusMonths(6), 21));

        assertTrue(error.getMessage().contains("stock sin lote"));
        assertEquals(0, loteRepo.count());
    }

    @Test
    @DisplayName("dos lotes no pueden superar juntos el stock disponible")
    void dosLotesNoSuperanElStock() {
        loteEnCentral("L-001", LocalDate.now().plusMonths(6), 15);

        var error = assertThrows(IllegalArgumentException.class,
                () -> loteEnCentral("L-002", LocalDate.now().plusMonths(6), 6));

        assertTrue(error.getMessage().contains("Disponible para asignar: 5"));
        assertEquals(1, loteRepo.count());
    }

    @Test
    @DisplayName("sin stock en la tienda no se puede asignar un lote")
    void sinStockNoHayLote() {
        var error = assertThrows(IllegalArgumentException.class, () -> lotes.registrar(
                loteDe(datos.getArroz().getIdProducto(), datos.getNorte().getIdTienda(),
                        "L-ARROZ", LocalDate.now().plusMonths(3), 1),
                datos.getAdmin().getIdUsuario()));

        // Norte tiene fila de stock de arroz en 0.
        assertTrue(error.getMessage().contains("stock sin lote"));
    }

    @Test
    @DisplayName("un manager no puede asignar lotes en otra tienda")
    void managerNoAsignaEnOtraTienda() {
        assertThrows(IllegalArgumentException.class, () -> lotes.registrar(
                loteDe(datos.getAgua().getIdProducto(), datos.getNorte().getIdTienda(),
                        "L-NORTE", LocalDate.now().plusMonths(3), 1),
                datos.getManagerCentral().getIdUsuario()));

        assertEquals(0, loteRepo.count());
    }

    // ------------------------------------------------------------------
    // Mermas
    // ------------------------------------------------------------------

    @Test
    @DisplayName("una merma baja el lote y el stock a la vez, y deja kardex")
    void mermaDescuentaAmbosSaldos() {
        var lote = loteEnCentral("L-001", LocalDate.now().plusMonths(6), 10);

        mermas.registrar(mermaDe(lote.getIdLote(), 4, Merma.Motivo.DANO),
                datos.getAdmin().getIdUsuario());

        assertEquals(6, loteRepo.findById(lote.getIdLote()).orElseThrow().getDisponible(),
                "el lote baja de 10 a 6");
        assertEquals(16, datos.stockDe(datos.getAgua(), datos.getCentral()),
                "y el stock de 20 a 16");

        var movimientos = kardexRepo.findAll();
        assertEquals(1, movimientos.size());
        assertEquals("MERMA", movimientos.get(0).getTipo());
        assertEquals(20, movimientos.get(0).getStockAntes());
        assertEquals(-4, movimientos.get(0).getCantidad());
        assertEquals(16, movimientos.get(0).getStockDespues());
    }

    @Test
    @DisplayName("no se puede mermar mas de lo que tiene el lote")
    void noSePuedeMermarDeMas() {
        var lote = loteEnCentral("L-001", LocalDate.now().plusMonths(6), 10);

        assertThrows(IllegalArgumentException.class, () -> mermas.registrar(
                mermaDe(lote.getIdLote(), 11, Merma.Motivo.DANO), datos.getAdmin().getIdUsuario()));

        assertEquals(10, loteRepo.findById(lote.getIdLote()).orElseThrow().getDisponible());
        assertEquals(20, datos.stockDe(datos.getAgua(), datos.getCentral()));
        assertEquals(0, mermaRepo.count());
        assertEquals(0, kardexRepo.count());
    }

    @Test
    @DisplayName("no se puede dar por vencido un lote que todavia no vencio")
    void vencimientoAntesDeTiempo() {
        var lote = loteEnCentral("L-001", LocalDate.now().plusMonths(6), 10);

        var error = assertThrows(IllegalArgumentException.class, () -> mermas.registrar(
                mermaDe(lote.getIdLote(), 1, Merma.Motivo.VENCIMIENTO), datos.getAdmin().getIdUsuario()));

        assertTrue(error.getMessage().contains("todavía no está vencido"));
        assertEquals(0, mermaRepo.count());
    }

    @Test
    @DisplayName("un lote ya vencido si se puede mermar por vencimiento")
    void vencimientoDeLoteVencido() {
        var lote = loteEnCentral("L-VIEJO", LocalDate.now().minusDays(1), 10);

        mermas.registrar(mermaDe(lote.getIdLote(), 10, Merma.Motivo.VENCIMIENTO),
                datos.getAdmin().getIdUsuario());

        assertEquals(0, loteRepo.findById(lote.getIdLote()).orElseThrow().getDisponible());
        assertEquals(10, datos.stockDe(datos.getAgua(), datos.getCentral()));
        assertEquals(1, mermaRepo.count());
    }

    @Test
    @DisplayName("mermar sobre un lote inexistente se rechaza")
    void loteInexistente() {
        assertThrows(IllegalArgumentException.class, () -> mermas.registrar(
                mermaDe(9999, 1, Merma.Motivo.DANO), datos.getAdmin().getIdUsuario()));

        assertEquals(0, mermaRepo.count());
    }

    // ------------------------------------------------------------------
    // Lotes y transferencias
    // ------------------------------------------------------------------

    @Test
    @DisplayName("transferir arrastra el lote a la tienda destino")
    void elLoteViajaConLaTransferencia() {
        var lote = loteEnCentral("L-001", LocalDate.now().plusMonths(6), 10);

        transferencias.registrar(datos.getAgua().getIdProducto(),
                datos.getCentral().getIdTienda(), datos.getNorte().getIdTienda(),
                4, "Reposicion", datos.getAdmin());

        assertEquals(6, loteRepo.findById(lote.getIdLote()).orElseThrow().getDisponible(),
                "el lote de origen baja");

        var enNorte = loteRepo.findAll().stream()
                .filter(l -> l.getTienda().getIdTienda().equals(datos.getNorte().getIdTienda()))
                .toList();
        assertEquals(1, enNorte.size(), "y aparece uno nuevo en destino");
        assertEquals("L-001", enNorte.get(0).getCodigo(), "conservando el codigo de lote");
        assertEquals(4, enNorte.get(0).getDisponible());
        assertEquals(lote.getVencimiento(), enNorte.get(0).getVencimiento(),
                "y su fecha de vencimiento");
    }

    @Test
    @DisplayName("el stock sin lote viaja sin inventar lotes en destino")
    void stockSinLoteViajaSinLote() {
        // Central tiene 20, de los cuales solo 5 estan en un lote.
        loteEnCentral("L-001", LocalDate.now().plusMonths(6), 5);

        transferencias.registrar(datos.getAgua().getIdProducto(),
                datos.getCentral().getIdTienda(), datos.getNorte().getIdTienda(),
                8, "Reposicion", datos.getAdmin());

        var enNorte = loteRepo.findAll().stream()
                .filter(l -> l.getTienda().getIdTienda().equals(datos.getNorte().getIdTienda()))
                .toList();

        assertEquals(1, enNorte.size());
        assertEquals(5, enNorte.get(0).getDisponible(),
                "solo viajan con lote las 5 unidades que lo tenian");
        assertEquals(11, datos.stockDe(datos.getAgua(), datos.getNorte()),
                "pero el stock se mueve completo: 3 + 8");
    }
}
