package pe.edu.cibertec.bodega.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.cibertec.bodega.dto.PanelTendencias;
import pe.edu.cibertec.bodega.model.Tienda;
import pe.edu.cibertec.bodega.repository.TiendaRepository;
import pe.edu.cibertec.bodega.service.PanelService;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Ojo con el alcance: @WebMvcTest no carga la SecurityConfig del proyecto, asi
 * que esto NO verifica que /panel quede reservado al ADMINISTRADOR. Ver la nota
 * en TestSecurityUsers.
 */
@WebMvcTest(PanelController.class)
class PanelControllerTest {

    @Autowired MockMvc mvc;
    @MockBean PanelService servicio;
    @MockBean TiendaRepository tiendas;

    private static PanelTendencias panel(List<PanelTendencias.Mes> meses) {
        return new PanelTendencias("Todas las tiendas", meses, List.of(), List.of());
    }

    @BeforeEach
    void preparar() {
        Tienda t = new Tienda();
        t.setIdTienda(1);
        t.setNombre("Bodega Centro");
        when(tiendas.findAll(any(Sort.class))).thenReturn(List.of(t));
    }

    @Test
    void muestraLosMesesYElMargen() throws Exception {
        when(servicio.consultar(null)).thenReturn(panel(List.of(
                new PanelTendencias.Mes(2026, 9,
                        new BigDecimal("1000.00"), new BigDecimal("400.00"), new BigDecimal("150.00")))));

        mvc.perform(get("/panel").with(user(TestSecurityUsers.admin())))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("panel"))
                .andExpect(content().string(containsString("09/2026")))
                .andExpect(content().string(containsString("450.00")))
                .andExpect(content().string(containsString("Bodega Centro")));
    }

    @Test
    void sinMovimientoAvisaEnVezDeMostrarUnaTablaVacia() throws Exception {
        when(servicio.consultar(null)).thenReturn(panel(List.of()));

        mvc.perform(get("/panel").with(user(TestSecurityUsers.admin())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Todavía no hay movimiento registrado")));
    }

    @Test
    void pasaElFiltroDeTiendaAlServicio() throws Exception {
        when(servicio.consultar(2)).thenReturn(panel(List.of()));

        mvc.perform(get("/panel").param("idTienda", "2").with(user(TestSecurityUsers.admin())))
                .andExpect(status().isOk())
                .andExpect(model().attribute("idTiendaSeleccionada", 2));

        verify(servicio).consultar(2);
    }

    @Test
    void tiendaInexistenteAvisaSinReventar() throws Exception {
        when(servicio.consultar(99)).thenThrow(new IllegalArgumentException("La tienda no existe."));

        mvc.perform(get("/panel").param("idTienda", "99").with(user(TestSecurityUsers.admin())))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("panel"))
                .andExpect(content().string(containsString("La tienda no existe.")));
    }
}
