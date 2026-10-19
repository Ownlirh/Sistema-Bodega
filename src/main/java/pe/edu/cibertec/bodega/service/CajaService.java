package pe.edu.cibertec.bodega.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.cibertec.bodega.model.Caja;
import pe.edu.cibertec.bodega.model.CajaMovimiento;
import pe.edu.cibertec.bodega.model.Tienda;
import pe.edu.cibertec.bodega.model.Usuario;
import pe.edu.cibertec.bodega.repository.CajaMovimientoRepository;
import pe.edu.cibertec.bodega.repository.CajaRepository;
import pe.edu.cibertec.bodega.repository.UsuarioRepository;
import pe.edu.cibertec.bodega.repository.VentaRepository;
import pe.edu.cibertec.bodega.security.AppUserDetails;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CajaService {
    private static final String ABIERTA = "ABIERTA";
    private static final String CERRADA = "CERRADA";
    private final CajaRepository cajaRepository;
    private final CajaMovimientoRepository movimientoRepository;
    private final UsuarioRepository usuarioRepository;
    private final VentaRepository ventaRepository;
    private final TiendaAccessService tiendaAccessService;
    private final AuditoriaService auditoriaService;

    @Transactional
    public Caja abrir(Integer idTienda, BigDecimal montoInicial, AppUserDetails principal, HttpServletRequest request) {
        if (montoInicial == null || montoInicial.signum() < 0) {
            throw new IllegalArgumentException("El monto inicial debe ser cero o mayor.");
        }
        Tienda tienda = tiendaAccessService.exigirTiendaPermitida(idTienda, principal);
        if (cajaRepository.existsByTiendaIdTiendaAndEstado(idTienda, ABIERTA)) {
            throw new IllegalArgumentException("Ya existe una caja abierta para esta tienda. Debe cerrarse antes de abrir otra.");
        }
        Usuario usuario = usuarioActual(principal);
        Caja caja = new Caja();
        caja.setTienda(tienda);
        caja.setUsuarioApertura(usuario);
        caja.setFechaApertura(LocalDateTime.now());
        caja.setMontoInicial(montoInicial);
        caja.setEstado(ABIERTA);
        caja = cajaRepository.save(caja);
        auditoriaService.registrar(usuario.getUsername(), "APERTURA_CAJA",
                "Caja #" + caja.getIdCaja() + " abierta en " + tienda.getNombre() + "; monto inicial=" + montoInicial,
                "OK", request);
        return caja;
    }

    @Transactional
    public CajaMovimiento registrarMovimiento(Long idCaja, String tipo, String concepto, BigDecimal monto,
                                               AppUserDetails principal, HttpServletRequest request) {
        Caja caja = cajaRepository.findById(idCaja).orElseThrow(() -> new IllegalArgumentException("La caja no existe."));
        tiendaAccessService.verificarAcceso(caja.getTienda().getIdTienda(), principal);
        if (!ABIERTA.equals(caja.getEstado())) throw new IllegalArgumentException("No se pueden registrar movimientos en una caja cerrada.");
        if (!"ENTRADA".equals(tipo) && !"SALIDA".equals(tipo)) throw new IllegalArgumentException("Tipo de movimiento no válido.");
        if (concepto == null || concepto.trim().length() < 3 || concepto.trim().length() > 120) {
            throw new IllegalArgumentException("El concepto debe tener entre 3 y 120 caracteres.");
        }
        if (monto == null || monto.signum() <= 0) throw new IllegalArgumentException("El monto debe ser mayor que cero.");
        Usuario usuario = usuarioActual(principal);
        CajaMovimiento movimiento = new CajaMovimiento();
        movimiento.setCaja(caja);
        movimiento.setUsuario(usuario);
        movimiento.setFecha(LocalDateTime.now());
        movimiento.setTipo(tipo);
        movimiento.setConcepto(concepto.trim());
        movimiento.setMonto(monto);
        movimiento = movimientoRepository.save(movimiento);
        auditoriaService.registrar(usuario.getUsername(), "MOVIMIENTO_CAJA",
                tipo + " de S/ " + monto + " en caja #" + idCaja + "; " + concepto.trim(), "OK", request);
        return movimiento;
    }

    @Transactional
    public Caja cerrar(Long idCaja, BigDecimal efectivoContado, String observacion,
                       AppUserDetails principal, HttpServletRequest request) {
        Caja caja = cajaRepository.findById(idCaja).orElseThrow(() -> new IllegalArgumentException("La caja no existe."));
        tiendaAccessService.verificarAcceso(caja.getTienda().getIdTienda(), principal);
        if (!ABIERTA.equals(caja.getEstado())) throw new IllegalArgumentException("La caja ya está cerrada.");
        if (efectivoContado == null || efectivoContado.signum() < 0) {
            throw new IllegalArgumentException("El efectivo contado debe ser cero o mayor.");
        }
        LocalDateTime ahora = LocalDateTime.now();
        BigDecimal ventas = cero(ventaRepository.totalVentasCaja(caja.getTienda().getIdTienda(), caja.getFechaApertura(), ahora));
        List<CajaMovimiento> movimientos = movimientoRepository.findByCajaIdCaja(caja.getIdCaja());
        BigDecimal entradas = movimientos.stream().filter(m -> "ENTRADA".equals(m.getTipo()))
                .map(CajaMovimiento::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal salidas = movimientos.stream().filter(m -> "SALIDA".equals(m.getTipo()))
                .map(CajaMovimiento::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal esperado = caja.getMontoInicial().add(ventas).add(entradas).subtract(salidas);

        caja.setFechaCierre(ahora);
        caja.setUsuarioCierre(usuarioActual(principal));
        caja.setTotalVentas(ventas);
        caja.setTotalIngresos(entradas);
        caja.setTotalEgresos(salidas);
        caja.setEfectivoEsperado(esperado);
        caja.setEfectivoContado(efectivoContado);
        caja.setDiferencia(efectivoContado.subtract(esperado));
        caja.setObservacionCierre(observacion == null || observacion.isBlank() ? null : observacion.trim());
        caja.setEstado(CERRADA);
        caja = cajaRepository.save(caja);
        auditoriaService.registrar(caja.getUsuarioCierre().getUsername(), "CIERRE_ARQUEO_CAJA",
                "Caja #" + idCaja + "; esperado=" + esperado + "; contado=" + efectivoContado + "; diferencia=" + caja.getDiferencia(),
                "OK", request);
        return caja;
    }

    @Transactional(readOnly = true)
    public List<CajaMovimiento> movimientos(Long idCaja, AppUserDetails principal) {
        Caja caja = cajaRepository.findById(idCaja).orElseThrow(() -> new IllegalArgumentException("La caja no existe."));
        tiendaAccessService.verificarAcceso(caja.getTienda().getIdTienda(), principal);
        return movimientoRepository.findByCajaIdCajaOrderByFechaDesc(idCaja);
    }

    private Usuario usuarioActual(AppUserDetails principal) {
        return usuarioRepository.findById(principal.getIdUsuario())
                .orElseThrow(() -> new AccessDeniedException("El usuario de la sesión ya no existe."));
    }
    private BigDecimal cero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
}
