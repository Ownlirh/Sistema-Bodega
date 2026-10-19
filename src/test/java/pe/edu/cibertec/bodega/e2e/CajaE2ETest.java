package pe.edu.cibertec.bodega.e2e;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.cibertec.bodega.model.*;
import pe.edu.cibertec.bodega.repository.AuditoriaRepository;
import pe.edu.cibertec.bodega.repository.CajaRepository;
import pe.edu.cibertec.bodega.repository.VentaRepository;
import pe.edu.cibertec.bodega.security.AppUserDetails;
import pe.edu.cibertec.bodega.service.CajaService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Caja diaria y arqueo, de punta a punta.
 *
 * Este modulo no tenia ni una prueba, y encima su pantalla no existe
 * (CajaController devuelve "caja/index" y la plantilla falta), asi que nada
 * de esto se habia ejecutado jamas. Se prueba por el servicio, que es donde
 * esta toda la logica.
 *
 * El arqueo es la cuenta que importa:
 *
 *   esperado = monto inicial + ventas del turno + entradas - salidas
 *   diferencia = contado - esperado
 */
class CajaE2ETest extends BaseE2E {

    @Autowired CajaService cajas;
    @Autowired CajaRepository cajaRepo;
    @Autowired VentaRepository ventas;
    @Autowired AuditoriaRepository auditorias;

    private final HttpServletRequest peticion = new MockHttpServletRequest();

    private AppUserDetails como(Usuario usuario) {
        return new AppUserDetails(usuario);
    }

    /** Venta directa: el modulo de ventas vive en otra rama. */
    @Transactional
    void venta(Tienda tienda, String total, LocalDateTime cuando) {
        Venta v = new Venta();
        v.setFecha(cuando);
        v.setTienda(tienda);
        v.setUsuario(datos.getAdmin());
        v.setEstado(true);
        VentaDetalle d = new VentaDetalle();
        d.setProducto(datos.getAgua());
        d.setCantidad(1);
        d.setPrecioUnitario(new BigDecimal(total));
        d.setSubtotal(new BigDecimal(total));
        d.setVenta(v);
        v.setDetalles(List.of(d));
        v.setTotal(new BigDecimal(total));
        ventas.save(v);
    }

    // ------------------------------------------------------------------
    // Apertura
    // ------------------------------------------------------------------

    @Test
    @DisplayName("abrir una caja la deja en estado ABIERTA con su monto inicial")
    void abrirCaja() {
        var caja = cajas.abrir(datos.getCentral().getIdTienda(), new BigDecimal("100.00"),
                como(datos.getManagerCentral()), peticion);

        assertNotNull(caja.getIdCaja());
        assertEquals("ABIERTA", caja.getEstado());
        assertEquals(0, new BigDecimal("100.00").compareTo(caja.getMontoInicial()));
        assertEquals(1, cajaRepo.count());
    }

    @Test
    @DisplayName("no se pueden tener dos cajas abiertas en la misma tienda")
    void unaSolaCajaAbiertaPorTienda() {
        cajas.abrir(datos.getCentral().getIdTienda(), new BigDecimal("100.00"),
                como(datos.getManagerCentral()), peticion);

        var error = assertThrows(IllegalArgumentException.class, () -> cajas.abrir(
                datos.getCentral().getIdTienda(), new BigDecimal("50.00"),
                como(datos.getManagerCentral()), peticion));

        assertTrue(error.getMessage().contains("Ya existe una caja abierta"));
        assertEquals(1, cajaRepo.count());
    }

    @Test
    @DisplayName("dos tiendas distintas si pueden tener su caja abierta a la vez")
    void cadaTiendaSuCaja() {
        cajas.abrir(datos.getCentral().getIdTienda(), new BigDecimal("100.00"),
                como(datos.getAdmin()), peticion);
        cajas.abrir(datos.getNorte().getIdTienda(), new BigDecimal("80.00"),
                como(datos.getAdmin()), peticion);

        assertEquals(2, cajaRepo.count());
    }

    @Test
    @DisplayName("un monto inicial negativo se rechaza")
    void rechazaMontoNegativo() {
        assertThrows(IllegalArgumentException.class, () -> cajas.abrir(
                datos.getCentral().getIdTienda(), new BigDecimal("-1.00"),
                como(datos.getAdmin()), peticion));

        assertEquals(0, cajaRepo.count());
    }

    @Test
    @DisplayName("un manager no puede abrir caja en otra tienda")
    void managerNoAbreEnOtraTienda() {
        assertThrows(RuntimeException.class, () -> cajas.abrir(
                datos.getNorte().getIdTienda(), new BigDecimal("100.00"),
                como(datos.getManagerCentral()), peticion));

        assertEquals(0, cajaRepo.count());
    }

    // ------------------------------------------------------------------
    // Movimientos
    // ------------------------------------------------------------------

    @Test
    @DisplayName("se registran entradas y salidas de efectivo")
    void registrarMovimientos() {
        var caja = cajas.abrir(datos.getCentral().getIdTienda(), new BigDecimal("100.00"),
                como(datos.getManagerCentral()), peticion);

        cajas.registrarMovimiento(caja.getIdCaja(), "ENTRADA", "Cambio del dia",
                new BigDecimal("50.00"), como(datos.getManagerCentral()), peticion);
        cajas.registrarMovimiento(caja.getIdCaja(), "SALIDA", "Compra de bolsas",
                new BigDecimal("20.00"), como(datos.getManagerCentral()), peticion);

        var movimientos = cajas.movimientos(caja.getIdCaja(), como(datos.getManagerCentral()));
        assertEquals(2, movimientos.size());
    }

    @Test
    @DisplayName("un tipo de movimiento invalido se rechaza")
    void rechazaTipoInvalido() {
        var caja = cajas.abrir(datos.getCentral().getIdTienda(), new BigDecimal("100.00"),
                como(datos.getAdmin()), peticion);

        assertThrows(IllegalArgumentException.class, () -> cajas.registrarMovimiento(
                caja.getIdCaja(), "PRESTAMO", "Lo que sea",
                new BigDecimal("10.00"), como(datos.getAdmin()), peticion));

        assertTrue(cajas.movimientos(caja.getIdCaja(), como(datos.getAdmin())).isEmpty());
    }

    @Test
    @DisplayName("un concepto demasiado corto se rechaza")
    void rechazaConceptoCorto() {
        var caja = cajas.abrir(datos.getCentral().getIdTienda(), new BigDecimal("100.00"),
                como(datos.getAdmin()), peticion);

        assertThrows(IllegalArgumentException.class, () -> cajas.registrarMovimiento(
                caja.getIdCaja(), "ENTRADA", "ab",
                new BigDecimal("10.00"), como(datos.getAdmin()), peticion));
    }

    @Test
    @DisplayName("no se puede mover plata en una caja ya cerrada")
    void noSeMueveEnCajaCerrada() {
        var caja = cajas.abrir(datos.getCentral().getIdTienda(), new BigDecimal("100.00"),
                como(datos.getAdmin()), peticion);
        cajas.cerrar(caja.getIdCaja(), new BigDecimal("100.00"), "Sin novedad",
                como(datos.getAdmin()), peticion);

        var error = assertThrows(IllegalArgumentException.class, () -> cajas.registrarMovimiento(
                caja.getIdCaja(), "ENTRADA", "Tarde",
                new BigDecimal("10.00"), como(datos.getAdmin()), peticion));

        assertTrue(error.getMessage().contains("caja cerrada"));
    }

    // ------------------------------------------------------------------
    // Arqueo
    // ------------------------------------------------------------------

    @Test
    @DisplayName("el arqueo cuadra cuando lo contado coincide con lo esperado")
    void arqueoSinDiferencia() {
        var caja = cajas.abrir(datos.getCentral().getIdTienda(), new BigDecimal("100.00"),
                como(datos.getAdmin()), peticion);
        cajas.registrarMovimiento(caja.getIdCaja(), "ENTRADA", "Cambio del dia",
                new BigDecimal("50.00"), como(datos.getAdmin()), peticion);
        cajas.registrarMovimiento(caja.getIdCaja(), "SALIDA", "Compra de bolsas",
                new BigDecimal("20.00"), como(datos.getAdmin()), peticion);

        // esperado = 100 + 0 ventas + 50 - 20 = 130
        var cerrada = cajas.cerrar(caja.getIdCaja(), new BigDecimal("130.00"), "Sin novedad",
                como(datos.getAdmin()), peticion);

        assertEquals("CERRADA", cerrada.getEstado());
        assertEquals(0, new BigDecimal("130.00").compareTo(cerrada.getEfectivoEsperado()));
        assertEquals(0, BigDecimal.ZERO.compareTo(cerrada.getDiferencia()));
        assertEquals(0, new BigDecimal("50.00").compareTo(cerrada.getTotalIngresos()));
        assertEquals(0, new BigDecimal("20.00").compareTo(cerrada.getTotalEgresos()));
    }

    @Test
    @DisplayName("las ventas del turno entran al efectivo esperado")
    void lasVentasCuentanEnElArqueo() {
        var caja = cajas.abrir(datos.getCentral().getIdTienda(), new BigDecimal("100.00"),
                como(datos.getAdmin()), peticion);

        venta(datos.getCentral(), "45.00", LocalDateTime.now());

        // esperado = 100 + 45 = 145
        var cerrada = cajas.cerrar(caja.getIdCaja(), new BigDecimal("145.00"), null,
                como(datos.getAdmin()), peticion);

        assertEquals(0, new BigDecimal("45.00").compareTo(cerrada.getTotalVentas()));
        assertEquals(0, new BigDecimal("145.00").compareTo(cerrada.getEfectivoEsperado()));
        assertEquals(0, BigDecimal.ZERO.compareTo(cerrada.getDiferencia()));
    }

    @Test
    @DisplayName("una venta anulada no infla el efectivo esperado")
    void ventaAnuladaNoCuenta() {
        var caja = cajas.abrir(datos.getCentral().getIdTienda(), new BigDecimal("100.00"),
                como(datos.getAdmin()), peticion);

        venta(datos.getCentral(), "45.00", LocalDateTime.now());
        var v = ventas.findAll().get(0);
        v.setEstado(false);
        ventas.save(v);

        var cerrada = cajas.cerrar(caja.getIdCaja(), new BigDecimal("100.00"), null,
                como(datos.getAdmin()), peticion);

        assertEquals(0, BigDecimal.ZERO.compareTo(cerrada.getTotalVentas()));
        assertEquals(0, BigDecimal.ZERO.compareTo(cerrada.getDiferencia()));
    }

    @Test
    @DisplayName("una venta de otra tienda no entra en este arqueo")
    void ventaDeOtraTiendaNoCuenta() {
        var caja = cajas.abrir(datos.getCentral().getIdTienda(), new BigDecimal("100.00"),
                como(datos.getAdmin()), peticion);

        venta(datos.getNorte(), "70.00", LocalDateTime.now());

        var cerrada = cajas.cerrar(caja.getIdCaja(), new BigDecimal("100.00"), null,
                como(datos.getAdmin()), peticion);

        assertEquals(0, BigDecimal.ZERO.compareTo(cerrada.getTotalVentas()));
    }

    @Test
    @DisplayName("un faltante queda registrado como diferencia negativa")
    void arqueoConFaltante() {
        var caja = cajas.abrir(datos.getCentral().getIdTienda(), new BigDecimal("100.00"),
                como(datos.getAdmin()), peticion);

        var cerrada = cajas.cerrar(caja.getIdCaja(), new BigDecimal("90.00"), "Faltan 10",
                como(datos.getAdmin()), peticion);

        assertEquals(0, new BigDecimal("-10.00").compareTo(cerrada.getDiferencia()));
        assertEquals("Faltan 10", cerrada.getObservacionCierre());
    }

    @Test
    @DisplayName("un sobrante queda registrado como diferencia positiva")
    void arqueoConSobrante() {
        var caja = cajas.abrir(datos.getCentral().getIdTienda(), new BigDecimal("100.00"),
                como(datos.getAdmin()), peticion);

        var cerrada = cajas.cerrar(caja.getIdCaja(), new BigDecimal("112.50"), null,
                como(datos.getAdmin()), peticion);

        assertEquals(0, new BigDecimal("12.50").compareTo(cerrada.getDiferencia()));
    }

    @Test
    @DisplayName("una caja no se puede cerrar dos veces")
    void noSeCierraDosVeces() {
        var caja = cajas.abrir(datos.getCentral().getIdTienda(), new BigDecimal("100.00"),
                como(datos.getAdmin()), peticion);
        cajas.cerrar(caja.getIdCaja(), new BigDecimal("100.00"), null, como(datos.getAdmin()), peticion);

        var error = assertThrows(IllegalArgumentException.class, () -> cajas.cerrar(
                caja.getIdCaja(), new BigDecimal("100.00"), null, como(datos.getAdmin()), peticion));

        assertTrue(error.getMessage().contains("ya está cerrada"));
    }

    @Test
    @DisplayName("tras cerrar se puede abrir una caja nueva en la misma tienda")
    void despuesDeCerrarSePuedeAbrirOtra() {
        var primera = cajas.abrir(datos.getCentral().getIdTienda(), new BigDecimal("100.00"),
                como(datos.getAdmin()), peticion);
        cajas.cerrar(primera.getIdCaja(), new BigDecimal("100.00"), null, como(datos.getAdmin()), peticion);

        var segunda = cajas.abrir(datos.getCentral().getIdTienda(), new BigDecimal("200.00"),
                como(datos.getAdmin()), peticion);

        assertEquals("ABIERTA", segunda.getEstado());
        assertEquals(2, cajaRepo.count());
    }

    // ------------------------------------------------------------------
    // Bitacora
    // ------------------------------------------------------------------

    @Test
    @DisplayName("apertura, movimiento y cierre quedan en la bitacora")
    void todoElCicloQuedaAuditado() {
        var caja = cajas.abrir(datos.getCentral().getIdTienda(), new BigDecimal("100.00"),
                como(datos.getAdmin()), peticion);
        cajas.registrarMovimiento(caja.getIdCaja(), "ENTRADA", "Cambio del dia",
                new BigDecimal("50.00"), como(datos.getAdmin()), peticion);
        cajas.cerrar(caja.getIdCaja(), new BigDecimal("150.00"), null, como(datos.getAdmin()), peticion);

        var acciones = auditorias.findAll().stream().map(Auditoria::getAccion).toList();

        assertTrue(acciones.contains("APERTURA_CAJA"));
        assertTrue(acciones.contains("MOVIMIENTO_CAJA"));
        assertTrue(acciones.contains("CIERRE_ARQUEO_CAJA"));
    }
}
