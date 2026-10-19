package pe.edu.cibertec.bodega.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import pe.edu.cibertec.bodega.dto.PanelTendencias;
import pe.edu.cibertec.bodega.model.Tienda;
import pe.edu.cibertec.bodega.repository.CompraRepository;
import pe.edu.cibertec.bodega.repository.GastoRepository;
import pe.edu.cibertec.bodega.repository.TiendaRepository;
import pe.edu.cibertec.bodega.repository.VentaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PanelServiceTest {

    @Mock VentaRepository ventas;
    @Mock CompraRepository compras;
    @Mock GastoRepository gastos;
    @Mock TiendaRepository tiendas;
    @InjectMocks PanelService servicio;

    /**
     * Arma la lista de filas. Hace falta porque List.of(unSoloObjectArray) lo
     * toma como los elementos de la lista, no como un elemento que es array.
     */
    private static List<Object[]> filas(Object[]... filas) {
        return List.of(filas);
    }

    /** Fila tal como la devuelve el repositorio: [anio, mes, total]. */
    private static Object[] mes(int anio, int mes, String total) {
        return new Object[]{anio, mes, new BigDecimal(total)};
    }

    private static Object[] rubro(String nombre, String total) {
        return new Object[]{nombre, new BigDecimal(total)};
    }

    private void series(List<Object[]> v, List<Object[]> c, List<Object[]> g) {
        when(ventas.totalPorMes(any())).thenReturn(v);
        when(compras.totalPorMes(any())).thenReturn(c);
        when(gastos.totalPorMes(any())).thenReturn(g);
        when(gastos.totalPorCategoria(any())).thenReturn(List.of());
        when(ventas.totalPorTienda()).thenReturn(List.of());
    }

    @Test
    void cruzaLasTresSeriesPorMes() {
        series(filas(mes(2026, 9, "1000.00")),
               filas(mes(2026, 9, "400.00")),
               filas(mes(2026, 9, "150.00")));

        PanelTendencias panel = servicio.consultar(null);

        assertEquals(1, panel.getMeses().size());
        PanelTendencias.Mes septiembre = panel.getMeses().get(0);
        assertEquals(0, new BigDecimal("1000.00").compareTo(septiembre.getIngresos()));
        assertEquals(0, new BigDecimal("550.00").compareTo(septiembre.getEgresos()));
        assertEquals(0, new BigDecimal("450.00").compareTo(septiembre.getMargen()));
    }

    @Test
    void unMesConGastosYSinVentasIgualAparece() {
        // Es el caso que justifica cruzar las series en vez de mostrarlas
        // una al lado de la otra: agosto solo existe en la serie de gastos.
        series(filas(mes(2026, 9, "1000.00")),
               List.of(),
               filas(mes(2026, 8, "80.00")));

        PanelTendencias panel = servicio.consultar(null);

        assertEquals(2, panel.getMeses().size());
        PanelTendencias.Mes agosto = panel.getMeses().get(0);
        assertEquals(8, agosto.getMes());
        assertEquals(0, BigDecimal.ZERO.compareTo(agosto.getIngresos()));
        assertEquals(0, new BigDecimal("80.00").compareTo(agosto.getGastos()));
        // Sin ingresos el margen es negativo, no cero.
        assertEquals(-1, agosto.getMargen().signum());
    }

    @Test
    void losMesesSalenOrdenadosDeMasViejoAMasNuevo() {
        series(filas(mes(2027, 1, "10.00"), mes(2026, 12, "10.00")),
               List.of(),
               filas(mes(2026, 11, "10.00")));

        List<String> etiquetas = servicio.consultar(null).getMeses().stream()
                .map(PanelTendencias.Mes::getEtiqueta).toList();

        assertEquals(List.of("11/2026", "12/2026", "01/2027"), etiquetas);
    }

    @Test
    void sinIngresosElMargenPorcentualEsNuloYNoDividePorCero() {
        series(List.of(), List.of(), filas(mes(2026, 9, "50.00")));

        PanelTendencias panel = servicio.consultar(null);

        assertNull(panel.getMargenPorcentaje());
        assertNull(panel.getMeses().get(0).getMargenPorcentaje());
    }

    @Test
    void periodoSinMovimientoQuedaVacio() {
        series(List.of(), List.of(), List.of());

        PanelTendencias panel = servicio.consultar(null);

        assertTrue(panel.isVacio());
        assertEquals(0, BigDecimal.ZERO.compareTo(panel.getIngresos()));
        assertEquals(0, BigDecimal.ZERO.compareTo(panel.getMargen()));
        // Sin datos no hay techo contra el cual dimensionar barras.
        assertEquals(0, panel.ancho(new BigDecimal("10.00")));
    }

    @Test
    void elMargenPuedeSerNegativo() {
        series(filas(mes(2026, 9, "100.00")),
               filas(mes(2026, 9, "90.00")),
               filas(mes(2026, 9, "40.00")));

        PanelTendencias panel = servicio.consultar(null);

        assertEquals(0, new BigDecimal("-30.00").compareTo(panel.getMargen()));
        assertEquals(0, new BigDecimal("-30.00").compareTo(panel.getMargenPorcentaje()));
    }

    @Test
    void lasBarrasSeDimensionanContraElMesMasAlto() {
        series(filas(mes(2026, 9, "1000.00"), mes(2026, 10, "500.00")),
               List.of(), List.of());

        PanelTendencias panel = servicio.consultar(null);

        assertEquals(100, panel.ancho(new BigDecimal("1000.00")));
        assertEquals(50, panel.ancho(new BigDecimal("500.00")));
        assertEquals(0, panel.ancho(BigDecimal.ZERO));
    }

    @Test
    void calculaLaParticipacionDeCadaCategoriaDeGasto() {
        series(List.of(), List.of(), List.of());
        when(gastos.totalPorCategoria(any()))
                .thenReturn(filas(rubro("Servicios", "75.00"), rubro("Alquiler", "25.00")));

        List<PanelTendencias.Rubro> rubros = servicio.consultar(null).getGastosPorCategoria();

        assertEquals(75, rubros.get(0).getParticipacion());
        assertEquals(25, rubros.get(1).getParticipacion());
    }

    @Test
    void unaSumaNulaSeTrataComoCero() {
        // SUM() devuelve null cuando no hay filas que sumar.
        series(filas(new Object[]{2026, 9, null}), List.of(), List.of());

        PanelTendencias panel = servicio.consultar(null);

        assertEquals(0, BigDecimal.ZERO.compareTo(panel.getIngresos()));
    }

    @Test
    void filtrarPorTiendaUsaSuNombre() {
        series(List.of(), List.of(), List.of());
        Tienda t = new Tienda();
        t.setIdTienda(3);
        t.setNombre("Bodega Centro");
        when(tiendas.findById(3)).thenReturn(Optional.of(t));

        assertEquals("Bodega Centro", servicio.consultar(3).getTienda());
        verify(ventas).totalPorMes(3);
        verify(compras).totalPorMes(3);
        verify(gastos).totalPorMes(3);
    }

    @Test
    void sinFiltroElTituloDiceTodasLasTiendas() {
        series(List.of(), List.of(), List.of());

        assertEquals("Todas las tiendas", servicio.consultar(null).getTienda());
        verify(ventas).totalPorMes(null);
    }

    @Test
    void elComparativoEntreTiendasIgnoraElFiltro() {
        // Comparar una tienda contra si misma no sirve de nada.
        series(List.of(), List.of(), List.of());
        Tienda t = new Tienda();
        t.setIdTienda(3);
        t.setNombre("Bodega Centro");
        when(tiendas.findById(3)).thenReturn(Optional.of(t));

        servicio.consultar(3);

        verify(ventas).totalPorTienda();
    }

    @Test
    void rechazaTiendaInexistente() {
        when(tiendas.findById(99)).thenReturn(Optional.empty());

        var error = assertThrows(IllegalArgumentException.class, () -> servicio.consultar(99));

        assertEquals("La tienda no existe.", error.getMessage());
        verifyNoInteractions(ventas, compras, gastos);
    }
}
