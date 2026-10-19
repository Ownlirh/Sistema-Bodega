package pe.edu.cibertec.bodega.e2e;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.cibertec.bodega.dto.CompraForm;
import pe.edu.cibertec.bodega.dto.GastoForm;
import pe.edu.cibertec.bodega.model.*;
import pe.edu.cibertec.bodega.repository.VentaRepository;
import pe.edu.cibertec.bodega.service.CompraService;
import pe.edu.cibertec.bodega.service.GastoService;
import pe.edu.cibertec.bodega.service.PanelService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * El panel de tendencias contra una base de verdad.
 *
 * El PanelServiceTest de unidad mockea los repositorios, asi que las cinco
 * consultas JPQL de agregacion (totalPorMes de ventas, compras y gastos,
 * totalPorCategoria y totalPorTienda) nunca se ejecutaban. Aca si.
 */
class PanelE2ETest extends BaseE2E {

    @Autowired PanelService panel;
    @Autowired CompraService compras;
    @Autowired GastoService gastos;
    @Autowired VentaRepository ventas;

    /** Graba una venta directamente: el modulo de ventas vive en otra rama. */
    @Transactional
    void venta(Tienda tienda, Usuario usuario, Producto producto, int cantidad,
               String precio, LocalDateTime cuando) {
        Venta v = new Venta();
        v.setFecha(cuando);
        v.setTienda(tienda);
        v.setUsuario(usuario);
        v.setEstado(true);

        VentaDetalle d = new VentaDetalle();
        d.setProducto(producto);
        d.setCantidad(cantidad);
        d.setPrecioUnitario(new BigDecimal(precio));
        d.setSubtotal(new BigDecimal(precio).multiply(BigDecimal.valueOf(cantidad)));
        d.setVenta(v);
        v.setDetalles(List.of(d));
        v.setTotal(d.getSubtotal());

        ventas.save(v);
    }

    private void compra(Tienda tienda, Producto producto, int cantidad, String costo) {
        CompraForm form = new CompraForm();
        form.setIdProveedor(datos.getProveedor().getIdProveedor());
        form.setIdTienda(tienda.getIdTienda());
        CompraForm.Linea linea = new CompraForm.Linea();
        linea.setIdProducto(producto.getIdProducto());
        linea.setCantidad(cantidad);
        linea.setCosto(new BigDecimal(costo));
        form.setLineas(List.of(linea));
        compras.registrar(form, datos.getAdmin().getIdUsuario());
    }

    private void gasto(Tienda tienda, String monto) {
        GastoForm form = new GastoForm();
        form.setIdTienda(tienda.getIdTienda());
        form.setIdCategoriaGasto(datos.getServicios().getIdCategoriaGasto());
        form.setFecha(LocalDate.now());
        form.setMonto(new BigDecimal(monto));
        form.setDescripcion("Luz");
        gastos.registrar(form, datos.getAdmin().getIdUsuario());
    }

    // ------------------------------------------------------------------

    @Test
    @DisplayName("sin movimiento el panel queda vacio y en cero, sin dividir por cero")
    void panelVacio() {
        var p = panel.consultar(null);

        assertTrue(p.isVacio());
        assertEquals(0, BigDecimal.ZERO.compareTo(p.getIngresos()));
        assertEquals(0, BigDecimal.ZERO.compareTo(p.getMargen()));
        assertNull(p.getMargenPorcentaje(), "sin ingresos el porcentaje es N/D, no cero");
    }

    @Test
    @DisplayName("el margen sale de las tres series cruzadas")
    void margenConLasTresSeries() {
        venta(datos.getCentral(), datos.getAdmin(), datos.getAgua(), 100, "1.50", LocalDateTime.now());
        compra(datos.getCentral(), datos.getAgua(), 50, "1.00");
        gasto(datos.getCentral(), "30.00");

        var p = panel.consultar(null);

        assertEquals(0, new BigDecimal("150.00").compareTo(p.getIngresos()));
        assertEquals(0, new BigDecimal("50.00").compareTo(p.getMercaderia()));
        assertEquals(0, new BigDecimal("30.00").compareTo(p.getGastos()));
        // 150 - 50 - 30
        assertEquals(0, new BigDecimal("70.00").compareTo(p.getMargen()));
    }

    @Test
    @DisplayName("un mes con gastos y sin ventas aparece igual en la serie")
    void mesSoloConGastos() {
        // El gasto va a hoy; la venta, al mes pasado. Son meses distintos.
        venta(datos.getCentral(), datos.getAdmin(), datos.getAgua(), 10, "1.50",
                LocalDateTime.now().minusMonths(1));
        gasto(datos.getCentral(), "40.00");

        var p = panel.consultar(null);

        assertEquals(2, p.getMeses().size(),
                "deben verse los dos meses, aunque cada serie traiga solo uno");

        var viejo = p.getMeses().get(0);
        var reciente = p.getMeses().get(1);
        assertEquals(0, new BigDecimal("15.00").compareTo(viejo.getIngresos()));
        assertEquals(0, BigDecimal.ZERO.compareTo(viejo.getGastos()));
        assertEquals(0, BigDecimal.ZERO.compareTo(reciente.getIngresos()));
        assertEquals(0, new BigDecimal("40.00").compareTo(reciente.getGastos()));
    }

    @Test
    @DisplayName("los meses salen ordenados de mas viejo a mas nuevo")
    void mesesOrdenados() {
        venta(datos.getCentral(), datos.getAdmin(), datos.getAgua(), 1, "1.00",
                LocalDateTime.now().minusMonths(2));
        venta(datos.getCentral(), datos.getAdmin(), datos.getAgua(), 1, "1.00", LocalDateTime.now());
        venta(datos.getCentral(), datos.getAdmin(), datos.getAgua(), 1, "1.00",
                LocalDateTime.now().minusMonths(1));

        var meses = panel.consultar(null).getMeses();

        assertEquals(3, meses.size());
        for (int i = 1; i < meses.size(); i++) {
            var anterior = meses.get(i - 1);
            var actual = meses.get(i);
            assertTrue(anterior.getAnio() < actual.getAnio()
                    || (anterior.getAnio() == actual.getAnio() && anterior.getMes() < actual.getMes()),
                    "los meses deben venir en orden cronologico");
        }
    }

    @Test
    @DisplayName("una venta anulada no suma a los ingresos")
    void ventaAnuladaNoSuma() {
        venta(datos.getCentral(), datos.getAdmin(), datos.getAgua(), 10, "1.50", LocalDateTime.now());

        var todas = ventas.findAll();
        assertEquals(1, todas.size());
        var anulada = todas.get(0);
        anulada.setEstado(false);
        ventas.save(anulada);

        var p = panel.consultar(null);
        assertEquals(0, BigDecimal.ZERO.compareTo(p.getIngresos()),
                "las consultas filtran por estado = true");
    }

    @Test
    @DisplayName("el filtro por tienda deja fuera a las demas")
    void filtroPorTienda() {
        venta(datos.getCentral(), datos.getAdmin(), datos.getAgua(), 10, "1.50", LocalDateTime.now());
        venta(datos.getNorte(), datos.getAdmin(), datos.getAgua(), 4, "1.50", LocalDateTime.now());

        assertEquals(0, new BigDecimal("21.00").compareTo(panel.consultar(null).getIngresos()));
        assertEquals(0, new BigDecimal("15.00")
                .compareTo(panel.consultar(datos.getCentral().getIdTienda()).getIngresos()));
        assertEquals(0, new BigDecimal("6.00")
                .compareTo(panel.consultar(datos.getNorte().getIdTienda()).getIngresos()));
    }

    @Test
    @DisplayName("el comparativo entre tiendas ignora el filtro a proposito")
    void comparativoIgnoraElFiltro() {
        venta(datos.getCentral(), datos.getAdmin(), datos.getAgua(), 10, "1.50", LocalDateTime.now());
        venta(datos.getNorte(), datos.getAdmin(), datos.getAgua(), 4, "1.50", LocalDateTime.now());

        var p = panel.consultar(datos.getCentral().getIdTienda());

        assertEquals(2, p.getVentasPorTienda().size(),
                "comparar una tienda contra si misma no dice nada");
    }

    @Test
    @DisplayName("la participacion de gastos por categoria suma el total")
    void participacionDeGastos() {
        gasto(datos.getCentral(), "75.00");
        gasto(datos.getCentral(), "25.00");

        var rubros = panel.consultar(null).getGastosPorCategoria();

        assertEquals(1, rubros.size(), "ambos gastos van a la misma categoria");
        assertEquals(0, new BigDecimal("100.00").compareTo(rubros.get(0).getTotal()));
        assertEquals(100, rubros.get(0).getParticipacion());
    }

    @Test
    @DisplayName("el margen puede dar negativo cuando se compra mas de lo que se vende")
    void margenNegativo() {
        venta(datos.getCentral(), datos.getAdmin(), datos.getAgua(), 10, "1.50", LocalDateTime.now());
        compra(datos.getCentral(), datos.getAgua(), 100, "1.00");

        var p = panel.consultar(null);

        // 15 - 100 = -85
        assertEquals(0, new BigDecimal("-85.00").compareTo(p.getMargen()));
        assertEquals(-1, p.getMargenPorcentaje().signum());
    }

    @Test
    @DisplayName("una tienda inexistente avisa sin reventar")
    void tiendaInexistente() {
        var error = assertThrows(IllegalArgumentException.class, () -> panel.consultar(9999));
        assertEquals("La tienda no existe.", error.getMessage());
    }

    // ------------------------------------------------------------------
    // La pantalla
    // ------------------------------------------------------------------

    @Test
    @DisplayName("GET /panel responde y muestra los totales")
    void laPantallaMuestraLosNumeros() throws Exception {
        venta(datos.getCentral(), datos.getAdmin(), datos.getAgua(), 100, "1.50", LocalDateTime.now());
        gasto(datos.getCentral(), "30.00");

        mvc.perform(get("/panel").with(user(new pe.edu.cibertec.bodega.security.AppUserDetails(datos.getAdmin()))))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("panel"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("150.00")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("120.00")));
    }

    @Test
    @DisplayName("GET /panel sin datos avisa en vez de mostrar una tabla rota")
    void laPantallaSinDatos() throws Exception {
        mvc.perform(get("/panel").with(user(new pe.edu.cibertec.bodega.security.AppUserDetails(datos.getAdmin()))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Todavía no hay movimiento")));
    }
}
