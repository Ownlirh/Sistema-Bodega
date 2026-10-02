package pe.edu.cibertec.bodega.controller;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.cibertec.bodega.dto.*;
import pe.edu.cibertec.bodega.repository.TiendaRepository;
import pe.edu.cibertec.bodega.service.ReporteRentabilidadService;
import java.math.BigDecimal;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RentabilidadController.class)
class RentabilidadControllerTest {
    @Autowired MockMvc mvc;
    @MockBean ReporteRentabilidadService servicio;
    @MockBean TiendaRepository tiendas;
    @Test void muestraProductosYMargenIndefinido() throws Exception {
        var fila = new ReporteRentabilidad.Fila(1,"P01","Agua",1L,BigDecimal.ZERO,BigDecimal.TEN);
        when(servicio.consultar(any())).thenReturn(new ReporteRentabilidad("Central",List.of(fila),BigDecimal.ZERO,BigDecimal.TEN));
        mvc.perform(get("/reportes/rentabilidad")).andExpect(status().isOk())
            .andExpect(content().string(containsString("Agua"))).andExpect(content().string(containsString("N/D")));
    }
    @Test void fechaInvalidaNoConsulta() throws Exception {
        mvc.perform(get("/reportes/rentabilidad").param("desde","incorrecta"))
            .andExpect(status().isOk()).andExpect(model().attributeHasErrors("filtro"));
        verifyNoInteractions(servicio);
    }
}
