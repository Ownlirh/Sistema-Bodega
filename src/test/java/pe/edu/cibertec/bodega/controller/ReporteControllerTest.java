package pe.edu.cibertec.bodega.controller;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.cibertec.bodega.dto.*;
import pe.edu.cibertec.bodega.repository.TiendaRepository;
import pe.edu.cibertec.bodega.service.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReporteController.class)
class ReporteControllerTest {
    @Autowired MockMvc mvc;
    @MockBean ReporteVentasService ventas;
    @MockBean ReporteVentasPdf pdf;
    @MockBean TiendaRepository tiendas;
    @BeforeEach void preparar() {
        when(tiendas.findAll(any(Sort.class))).thenReturn(List.of());
        when(ventas.consultar(any())).thenReturn(new ReporteVentas(LocalDate.of(2026,9,1),LocalDate.of(2026,9,30),"Todas",List.of(),BigDecimal.ZERO));
        when(pdf.generar(any())).thenReturn("%PDF-prueba".getBytes());
    }
    @Test void muestraReporteVacio() throws Exception {
        mvc.perform(get("/reportes").with(user(TestSecurityUsers.admin()))).andExpect(status().isOk())
            .andExpect(content().string(containsString("Sin ventas en este período.")));
    }
    @Test void descargaPDFConNombreYTipo() throws Exception {
        mvc.perform(get("/reportes/ventas/pdf").with(user(TestSecurityUsers.admin())).param("desde","2026-09-01").param("hasta","2026-09-30"))
            .andExpect(status().isOk()).andExpect(content().contentType("application/pdf"))
            .andExpect(header().string("Content-Disposition",containsString("ventas-2026-09-01-2026-09-30.pdf")));
    }
    @Test void errorDeFechasNoEntregaArchivoFalso() throws Exception {
        when(ventas.consultar(any())).thenThrow(new IllegalArgumentException("Rango inválido"));
        mvc.perform(get("/reportes/ventas/pdf").with(user(TestSecurityUsers.admin()))).andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Rango inválido")));
        verifyNoInteractions(pdf);
    }
    @Test void fechaMalFormadaSeMuestraEnFormulario() throws Exception {
        mvc.perform(get("/reportes").with(user(TestSecurityUsers.admin())).param("desde","no-es-fecha"))
            .andExpect(status().isOk()).andExpect(model().attributeHasErrors("filtro"));
        verifyNoInteractions(ventas,pdf);
    }
}
