package pe.edu.cibertec.bodega.service;
import jakarta.validation.*;
import org.junit.jupiter.api.*;
import pe.edu.cibertec.bodega.dto.*;
import pe.edu.cibertec.bodega.repository.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class ReporteRentabilidadServiceTest {
    VentaDetalleRepository detalles; TiendaRepository tiendas; ValidatorFactory factory; ReporteRentabilidadService servicio;
    @BeforeEach void preparar() {
        detalles = mock(VentaDetalleRepository.class); tiendas = mock(TiendaRepository.class);
        factory = Validation.buildDefaultValidatorFactory(); servicio = new ReporteRentabilidadService(detalles, tiendas, factory.getValidator());
    }
    @AfterEach void cerrar() { factory.close(); }
    ReporteVentasForm filtro() {
        var f = new ReporteVentasForm(); f.setDesde(LocalDate.of(2026,9,1)); f.setHasta(LocalDate.of(2026,9,30)); return f;
    }
    @Test void calculaMargenPonderadoYLimitesDeFechas() {
        when(detalles.buscarRentabilidad(any(),any(),isNull())).thenReturn(List.of(
            new Object[]{1,"A","Producto",2L,new BigDecimal("100"),new BigDecimal("50")},
            new Object[]{2,"B","Producto",5L,new BigDecimal("900"),new BigDecimal("810")}));
        var r = servicio.consultar(filtro());
        assertEquals(new BigDecimal("140"),r.getGanancia());
        assertEquals(new BigDecimal("14.00"),r.getMargen());
        assertEquals(2,r.getFilas().size());
        verify(detalles).buscarRentabilidad(LocalDateTime.of(2026,9,1,0,0),LocalDateTime.of(2026,10,1,0,0),null);
    }
    @Test void ingresoCeroNoDividePorCero() {
        var fila = new ReporteRentabilidad.Fila(1,"A","Producto",1L,BigDecimal.ZERO,BigDecimal.TEN);
        assertNull(fila.getMargen()); assertEquals(new BigDecimal("-10"),fila.getGanancia());
    }
    @Test void permiteMargenNegativo() {
        var fila = new ReporteRentabilidad.Fila(1,"A","Producto",1L,BigDecimal.TEN,new BigDecimal("15"));
        assertEquals(new BigDecimal("-50.00"),fila.getMargen());
    }
    @Test void periodoVacio() {
        var r = servicio.consultar(filtro()); assertEquals(BigDecimal.ZERO,r.getIngresos()); assertNull(r.getMargen());
    }
    @Test void rechazaRangoInvertido() {
        var f = filtro(); f.setDesde(f.getHasta().plusDays(1));
        assertThrows(IllegalArgumentException.class,()->servicio.consultar(f)); verifyNoInteractions(detalles);
    }
    @Test void rechazaTiendaInexistente() {
        var f = filtro(); f.setIdTienda(99);
        assertThrows(IllegalArgumentException.class,()->servicio.consultar(f)); verifyNoInteractions(detalles);
    }
}
