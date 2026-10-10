package pe.edu.cibertec.bodega.controller;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.cibertec.bodega.model.*;
import pe.edu.cibertec.bodega.repository.*;
import pe.edu.cibertec.bodega.service.*;
import java.time.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VencimientoController.class)
class VencimientoControllerTest {
    @Autowired MockMvc mvc;
    @MockBean LoteRepository lotes; @MockBean MermaRepository mermas;
    @MockBean ProductoRepository productos; @MockBean TiendaRepository tiendas;
    @MockBean LoteService loteService; @MockBean MermaService mermaService;
    Lote lote;
    @BeforeEach void preparar() {
        Producto p = new Producto(); p.setIdProducto(1); p.setDescripcion("Yogur");
        Tienda t = new Tienda(); t.setIdTienda(1); t.setNombre("Tienda prueba");
        lote = new Lote(); lote.setIdLote(1); lote.setProducto(p); lote.setTienda(t);
        lote.setCodigo("LOTE-A"); lote.setDisponible(5); lote.setVencimiento(LocalDate.now(ZoneId.of("America/Lima")).minusDays(1));
        when(lotes.findByDisponibleGreaterThanOrderByVencimientoAscIdLoteAsc(0)).thenReturn(List.of(lote));
        when(productos.findAll(any(Sort.class))).thenReturn(List.of(p));
        when(tiendas.findByEstadoTrue()).thenReturn(List.of(t));
        when(mermas.findAll(any(Pageable.class))).thenReturn(Page.empty());
    }
    @Test void muestraVencidosYFormulario() throws Exception {
        mvc.perform(get("/vencimientos")).andExpect(status().isOk())
            .andExpect(model().attribute("vencidos", 1L)).andExpect(model().attribute("proximos", 0L))
            .andExpect(content().string(containsString("LOTE-A"))).andExpect(content().string(containsString("Vencido")));
    }
    @Test void muestraSeleccionLoteEnMermas() throws Exception {
        mvc.perform(get("/mermas")).andExpect(status().isOk())
            .andExpect(content().string(containsString("LOTE-A"))).andExpect(content().string(containsString("Yogur")));
    }
    @Test void muestraHistorialDeMerma() throws Exception {
        Usuario u = new Usuario(); u.setNombres("Ana"); u.setApellidos("Torres");
        Merma m = new Merma(); m.setIdMerma(1); m.setLote(lote); m.setFecha(LocalDateTime.now());
        m.setUsuario(u); m.setCantidad(1); m.setMotivo(Merma.Motivo.DANO); m.setDescripcion("Envase roto");
        when(mermas.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(m)));
        mvc.perform(get("/mermas")).andExpect(status().isOk()).andExpect(content().string(containsString("Envase roto")));
    }
    @Test void rechazaMermaCantidadCero() throws Exception {
        mvc.perform(post("/mermas").param("idLote", "1").param("cantidad", "0")
            .param("motivo", "DANO").param("descripcion", "Envase roto"))
            .andExpect(status().isOk()).andExpect(model().attributeHasErrors("mermaForm"));
        verifyNoInteractions(mermaService);
    }
    @Test void conservaFormularioAnteSaldoInsuficiente() throws Exception {
        when(mermaService.registrar(any(), eq(1))).thenThrow(new IllegalArgumentException("Saldo insuficiente"));
        mvc.perform(post("/mermas").param("idLote", "1").param("cantidad", "99")
            .param("motivo", "DANO").param("descripcion", "Envase roto"))
            .andExpect(status().isOk()).andExpect(content().string(containsString("Saldo insuficiente")))
            .andExpect(content().string(containsString("Envase roto")));
    }
    @Test void redirigeTrasAsignarLote() throws Exception {
        when(loteService.registrar(any(), eq(1))).thenReturn(lote);
        mvc.perform(post("/vencimientos").param("idProducto", "1").param("idTienda", "1")
            .param("codigo", "LOTE-A").param("vencimiento", "2026-12-01").param("cantidad", "5"))
            .andExpect(redirectedUrl("/vencimientos")).andExpect(flash().attributeExists("ok"));
    }
}
