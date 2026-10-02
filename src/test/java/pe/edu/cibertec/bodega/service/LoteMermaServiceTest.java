package pe.edu.cibertec.bodega.service;
import jakarta.persistence.EntityManager;
import jakarta.validation.*;
import org.junit.jupiter.api.*;
import pe.edu.cibertec.bodega.dto.*;
import pe.edu.cibertec.bodega.model.*;
import pe.edu.cibertec.bodega.repository.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoteMermaServiceTest {
    LoteRepository lotes; MermaRepository mermas; StockRepository stocks; UsuarioRepository usuarios;
    KardexService kardex; EntityManager em; ValidatorFactory factory;
    LoteService loteService; MermaService mermaService;
    Producto producto; Tienda tienda; Usuario usuario; Stock stock; Lote lote;
    LocalDate hoy = LocalDate.now(ZoneId.of("America/Lima"));
    @BeforeEach void preparar() {
        lotes = mock(LoteRepository.class); mermas = mock(MermaRepository.class);
        stocks = mock(StockRepository.class); usuarios = mock(UsuarioRepository.class);
        kardex = mock(KardexService.class); em = mock(EntityManager.class);
        factory = Validation.buildDefaultValidatorFactory();
        loteService = new LoteService(lotes, stocks, usuarios, factory.getValidator());
        mermaService = new MermaService(mermas, lotes, stocks, usuarios, kardex, factory.getValidator(), em);
        producto = new Producto(); producto.setIdProducto(1);
        tienda = new Tienda(); tienda.setIdTienda(1);
        usuario = new Usuario(); usuario.setIdUsuario(1);
        stock = new Stock(); stock.setProducto(producto); stock.setTienda(tienda); stock.setCantidad(20);
        lote = new Lote(); lote.setIdLote(1); lote.setProducto(producto); lote.setTienda(tienda);
        lote.setCodigo("LOTE-A"); lote.setDisponible(5); lote.setCantidadInicial(5); lote.setVencimiento(hoy.minusDays(1));
        when(usuarios.findById(1)).thenReturn(Optional.of(usuario));
        when(stocks.findParaActualizar(1, 1)).thenReturn(Optional.of(stock));
        when(lotes.findById(1)).thenReturn(Optional.of(lote)); when(lotes.bloquear(1)).thenReturn(Optional.of(lote));
        when(lotes.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mermas.saveAndFlush(any())).thenAnswer(i -> { Merma m = i.getArgument(0); m.setIdMerma(7); return m; });
    }
    @AfterEach void cerrar() { factory.close(); }
    LoteForm loteForm() {
        LoteForm f = new LoteForm(); f.setIdProducto(1); f.setIdTienda(1); f.setCodigo(" LOTE-B ");
        f.setVencimiento(hoy.plusDays(10)); f.setCantidad(3); return f;
    }
    MermaForm mermaForm() {
        MermaForm f = new MermaForm(); f.setIdLote(1); f.setCantidad(2);
        f.setMotivo(Merma.Motivo.VENCIMIENTO); f.setDescripcion("Retiro verificado"); return f;
    }
    @Test void asignarLoteNoSumaStock() {
        Lote l = loteService.registrar(loteForm(), 1);
        assertEquals(3, l.getDisponible()); assertEquals("LOTE-B", l.getCodigo());
        assertEquals(20, stock.getCantidad()); verify(stocks, never()).save(any()); verifyNoInteractions(kardex);
    }
    @Test void noAsignaDosVecesElMismoStock() {
        when(lotes.cantidadAsignada(1, 1)).thenReturn(19L);
        assertThrows(IllegalArgumentException.class, () -> loteService.registrar(loteForm(), 1));
        verify(lotes, never()).save(any());
    }
    @Test void noAsignaCantidadCero() {
        LoteForm f = loteForm(); f.setCantidad(0);
        assertThrows(IllegalArgumentException.class, () -> loteService.registrar(f, 1));
        verify(lotes, never()).save(any());
    }
    @Test void permiteIdentificarLoteYaVencido() {
        LoteForm f = loteForm(); f.setVencimiento(hoy.minusDays(3));
        assertEquals(f.getVencimiento(), loteService.registrar(f, 1).getVencimiento());
    }
    @Test void mermaDescuentaAmbosSaldosYRegistraKardex() {
        Merma m = mermaService.registrar(mermaForm(), 1);
        assertEquals(18, stock.getCantidad()); assertEquals(3, lote.getDisponible());
        assertSame(lote, m.getLote());
        verify(kardex).registrar(producto, tienda, "MERMA", 20, -2, "Merma #7 - lote #1", usuario);
        verify(em).refresh(lote, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
    }
    @Test void rechazaMermaMayorQueLote() {
        MermaForm f = mermaForm(); f.setCantidad(6);
        assertThrows(IllegalArgumentException.class, () -> mermaService.registrar(f, 1));
        assertEquals(20, stock.getCantidad()); verifyNoInteractions(mermas, kardex);
    }
    @Test void rechazaMermaMayorQueStock() {
        stock.setCantidad(1);
        assertThrows(IllegalArgumentException.class, () -> mermaService.registrar(mermaForm(), 1));
        assertEquals(5, lote.getDisponible()); verifyNoInteractions(mermas, kardex);
    }
    @Test void loteQueVenceHoyNoEstaVencido() {
        lote.setVencimiento(hoy);
        assertThrows(IllegalArgumentException.class, () -> mermaService.registrar(mermaForm(), 1));
        verifyNoInteractions(mermas, kardex);
    }
    @Test void danoPermiteRetirarAntesDelVencimiento() {
        lote.setVencimiento(hoy.plusDays(30)); MermaForm f = mermaForm(); f.setMotivo(Merma.Motivo.DANO);
        assertEquals(Merma.Motivo.DANO, mermaService.registrar(f, 1).getMotivo());
    }
    @Test void rechazaUsuarioDeOtraTienda() {
        Tienda otra = new Tienda(); otra.setIdTienda(2); usuario.setTienda(otra);
        assertThrows(IllegalArgumentException.class, () -> mermaService.registrar(mermaForm(), 1));
        verifyNoInteractions(mermas, kardex);
    }
    @Test void falloKardexSePropagaParaRollback() {
        doThrow(new IllegalStateException("fallo simulado")).when(kardex).registrar(any(), any(), anyString(), anyInt(), anyInt(), anyString(), any());
        assertThrows(IllegalStateException.class, () -> mermaService.registrar(mermaForm(), 1));
    }
    @Test void transferenciaConservaLoteYVencimientoEnDestino() {
        Tienda destino = new Tienda(); destino.setIdTienda(2);
        when(lotes.bloquearDisponibles(1, 1)).thenReturn(List.of(lote));
        loteService.trasladar(producto, tienda, destino, 3, usuario);
        assertEquals(2, lote.getDisponible());
        verify(lotes).save(argThat(l -> l.getTienda() == destino && l.getDisponible() == 3
            && l.getCodigo().equals("LOTE-A") && l.getVencimiento().equals(hoy.minusDays(1))));
        verifyNoInteractions(stocks, kardex);
    }
}
