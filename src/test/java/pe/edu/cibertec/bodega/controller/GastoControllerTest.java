package pe.edu.cibertec.bodega.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.cibertec.bodega.model.*;
import pe.edu.cibertec.bodega.repository.*;
import pe.edu.cibertec.bodega.service.GastoService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GastoController.class)
class GastoControllerTest {
    @Autowired MockMvc mvc;
    @MockBean GastoService servicio;
    @MockBean GastoRepository gastos;
    @MockBean CategoriaGastoRepository categorias;
    @MockBean TiendaRepository tiendas;
    @BeforeEach void preparar() {
        Tienda t = new Tienda(); t.setIdTienda(1); t.setNombre("Tienda prueba");
        CategoriaGasto c = new CategoriaGasto(); c.setIdCategoriaGasto(2); c.setNombre("Servicios");
        Usuario u = new Usuario(); u.setNombres("Ana"); u.setApellidos("Torres");
        Gasto g = new Gasto(); g.setIdGasto(1); g.setTienda(t); g.setCategoriaGasto(c); g.setUsuario(u);
        g.setFecha(LocalDate.of(2026, 1, 1)); g.setMonto(new BigDecimal("25.50")); g.setDescripcion("Recibo de luz");
        when(gastos.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(g)));
        when(categorias.findAll(any(Sort.class))).thenReturn(List.of(c));
        when(tiendas.findByEstadoTrue()).thenReturn(List.of(t));
    }
    @Test void muestraFormularioEHistorial() throws Exception {
        mvc.perform(get("/gastos").with(user(TestSecurityUsers.admin()))).andExpect(status().isOk())
            .andExpect(content().string(containsString("Servicios")))
            .andExpect(content().string(containsString("Recibo de luz")))
            .andExpect(model().attribute("totalMostrado", new BigDecimal("25.50")));
    }
    @Test void rechazaImporteInvalido() throws Exception {
        mvc.perform(post("/gastos").with(user(TestSecurityUsers.admin())).with(csrf()).param("idTienda", "1").param("idCategoriaGasto", "2")
            .param("fecha", "2026-01-01").param("monto", "-2"))
            .andExpect(status().isOk()).andExpect(model().attributeHasErrors("gastoForm"));
        verifyNoInteractions(servicio);
    }
    @Test void mantieneDatosAnteErrorDeNegocio() throws Exception {
        when(servicio.registrar(any(), eq(1))).thenThrow(new IllegalArgumentException("La fecha del gasto no puede ser futura."));
        mvc.perform(post("/gastos").with(user(TestSecurityUsers.admin())).with(csrf()).param("idTienda", "1").param("idCategoriaGasto", "2")
            .param("fecha", "2099-01-01").param("monto", "25.50").param("descripcion", "Pago ingresado"))
            .andExpect(status().isOk()).andExpect(model().attributeHasErrors("gastoForm"))
            .andExpect(content().string(containsString("Pago ingresado")))
            .andExpect(content().string(containsString("La fecha del gasto no puede ser futura.")));
    }
    @Test void redirigeTrasRegistrar() throws Exception {
        Gasto g = new Gasto(); g.setIdGasto(20); when(servicio.registrar(any(), eq(1))).thenReturn(g);
        mvc.perform(post("/gastos").with(user(TestSecurityUsers.admin())).with(csrf()).param("idTienda", "1").param("idCategoriaGasto", "2")
            .param("fecha", "2026-01-01").param("monto", "25.50"))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/gastos"))
            .andExpect(flash().attributeExists("ok"));
    }
}
