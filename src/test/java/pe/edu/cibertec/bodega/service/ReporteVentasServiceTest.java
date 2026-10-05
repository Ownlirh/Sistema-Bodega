package pe.edu.cibertec.bodega.service;
import jakarta.validation.*;
import org.junit.jupiter.api.*;
import pe.edu.cibertec.bodega.dto.*;
import pe.edu.cibertec.bodega.model.*;
import pe.edu.cibertec.bodega.repository.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class ReporteVentasServiceTest {
    VentaRepository ventas; TiendaRepository tiendas; ValidatorFactory factory; ReporteVentasService servicio;
    @BeforeEach void preparar() {
        ventas = mock(VentaRepository.class); tiendas = mock(TiendaRepository.class);
        factory = Validation.buildDefaultValidatorFactory(); servicio = new ReporteVentasService(ventas, tiendas, factory.getValidator());
    }
    @AfterEach void cerrar() { factory.close(); }
    ReporteVentasForm filtro() {
        var f = new ReporteVentasForm(); f.setDesde(LocalDate.of(2026, 9, 1)); f.setHasta(LocalDate.of(2026, 9, 30)); return f;
    }
    @Test void incluyeDiaFinalCompletoYTotaliza() {
        Tienda t = new Tienda(); t.setNombre("Central");
        Usuario u = new Usuario(); u.setNombres("Ana"); u.setApellidos("Torres");
        Venta v = new Venta(); v.setIdVenta(1); v.setTienda(t); v.setUsuario(u);
        v.setFecha(LocalDateTime.of(2026, 9, 30, 23, 59, 59)); v.setTotal(new BigDecimal("15.75"));
        when(ventas.buscarParaReporte(any(), any(), isNull(), any())).thenReturn(List.of(v));
        var reporte = servicio.consultar(filtro());
        assertEquals(new BigDecimal("15.75"), reporte.getTotal()); assertEquals(1, reporte.getFilas().size());
        verify(ventas).buscarParaReporte(eq(LocalDateTime.of(2026,9,1,0,0)),eq(LocalDateTime.of(2026,10,1,0,0)),isNull(),any());
    }
    @Test void admitePeriodoSinVentas() {
        when(ventas.buscarParaReporte(any(), any(), isNull(), any())).thenReturn(List.of());
        assertEquals(BigDecimal.ZERO, servicio.consultar(filtro()).getTotal());
    }
    @Test void rechazaRangoInvertido() {
        var f = filtro(); f.setDesde(f.getHasta().plusDays(1));
        assertThrows(IllegalArgumentException.class, () -> servicio.consultar(f)); verifyNoInteractions(ventas);
    }
    @Test void rechazaTiendaInexistente() {
        var f = filtro(); f.setIdTienda(99);
        assertThrows(IllegalArgumentException.class, () -> servicio.consultar(f)); verifyNoInteractions(ventas);
    }
    @Test void aplicaFiltroDeTienda() {
        var f = filtro(); f.setIdTienda(3); Tienda t = new Tienda(); t.setNombre("Tienda histórica"); t.setEstado(false);
        when(tiendas.findById(3)).thenReturn(Optional.of(t));
        when(ventas.buscarParaReporte(any(), any(), eq(3), any())).thenReturn(List.of());
        assertEquals("Tienda histórica", servicio.consultar(f).getTienda());
        verify(ventas).buscarParaReporte(any(), any(), eq(3), any());
    }
    @Test void rechazaReporteExcesivoSinTruncarlo() {
        when(ventas.buscarParaReporte(any(), any(), isNull(), any())).thenReturn(Collections.nCopies(10001, new Venta()));
        assertThrows(IllegalArgumentException.class, () -> servicio.consultar(filtro()));
    }
}
