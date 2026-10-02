package pe.edu.cibertec.bodega.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.cibertec.bodega.model.*;
import pe.edu.cibertec.bodega.repository.*;
import pe.edu.cibertec.bodega.service.CompraService;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CompraController.class)
class CompraControllerTest {
    @Autowired MockMvc mvc;
    @MockBean CompraService servicio;
    @MockBean CompraRepository compras;
    @MockBean ProductoRepository productos;
    @MockBean ProveedorRepository proveedores;
    @MockBean TiendaRepository tiendas;
    @BeforeEach void preparar() {
        when(compras.findAll(any(Pageable.class))).thenReturn(Page.empty());
        Producto p = new Producto(); p.setIdProducto(1); p.setCodigo("P001"); p.setDescripcion("Arroz");
        when(productos.findByEstadoTrue()).thenReturn(List.of(p));
        Proveedor pr = new Proveedor(); pr.setIdProveedor(1); pr.setRazonSocial("Proveedor prueba");
        when(proveedores.findByEstadoTrue()).thenReturn(List.of(pr));
        Tienda t = new Tienda(); t.setIdTienda(1); t.setNombre("Tienda prueba");
        when(tiendas.findByEstadoTrue()).thenReturn(List.of(t));
    }
    @Test void muestraFormularioConCatalogos() throws Exception {
        mvc.perform(get("/compras")).andExpect(status().isOk())
            .andExpect(content().string(containsString("Proveedor prueba")))
            .andExpect(content().string(containsString("Arroz")))
            .andExpect(content().string(containsString("lineas[0].cantidad")));
    }
    @Test void rechazaFormularioInvalidoSinGuardar() throws Exception {
        mvc.perform(post("/compras").param("idProveedor", "1").param("idTienda", "1")
            .param("lineas[0].idProducto", "1").param("lineas[0].cantidad", "0")
            .param("lineas[0].costo", "3.25"))
            .andExpect(status().isOk()).andExpect(model().attributeHasErrors("compraForm"));
        verifyNoInteractions(servicio);
    }
    @Test void redirigeDespuesDeGuardar() throws Exception {
        Compra c = new Compra(); c.setIdCompra(8);
        when(servicio.registrar(any(), eq(1))).thenReturn(c);
        mvc.perform(post("/compras").param("idProveedor", "1").param("idTienda", "1")
            .param("lineas[0].idProducto", "1").param("lineas[0].cantidad", "2")
            .param("lineas[0].costo", "3.25"))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/compras"))
            .andExpect(flash().attributeExists("ok"));
    }
}
