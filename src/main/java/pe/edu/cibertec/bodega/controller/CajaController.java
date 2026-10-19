package pe.edu.cibertec.bodega.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.edu.cibertec.bodega.model.Caja;
import pe.edu.cibertec.bodega.repository.CajaRepository;
import pe.edu.cibertec.bodega.security.AppUserDetails;
import pe.edu.cibertec.bodega.service.CajaService;
import pe.edu.cibertec.bodega.service.AuditoriaService;
import pe.edu.cibertec.bodega.service.TiendaAccessService;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/caja")
@RequiredArgsConstructor
public class CajaController {
    private final CajaRepository cajaRepository;
    private final CajaService cajaService;
    private final TiendaAccessService tiendaAccessService;
    private final AuditoriaService auditoriaService;

    @GetMapping
    public String inicio(@RequestParam(required = false) Integer idTienda,
                         @AuthenticationPrincipal AppUserDetails principal, Model model) {
        Integer tiendaSeleccionada = idTienda;
        if (!principal.esAdministrador()) tiendaSeleccionada = principal.getIdTienda();
        if (tiendaSeleccionada != null) tiendaAccessService.exigirTiendaPermitida(tiendaSeleccionada, principal);

        model.addAttribute("tiendas", tiendaAccessService.tiendasVisibles(principal));
        model.addAttribute("esAdministrador", principal.esAdministrador());
        model.addAttribute("idTiendaSeleccionada", tiendaSeleccionada);
        Caja cajaAbierta = tiendaSeleccionada == null ? null :
                cajaRepository.findFirstByTiendaIdTiendaAndEstadoOrderByFechaAperturaDesc(tiendaSeleccionada, "ABIERTA").orElse(null);
        model.addAttribute("cajaAbierta", cajaAbierta);
        model.addAttribute("movimientosCaja", cajaAbierta == null ? List.of() : cajaService.movimientos(cajaAbierta.getIdCaja(), principal));
        model.addAttribute("historial", tiendaSeleccionada == null
                ? cajaRepository.findTop50ByOrderByFechaAperturaDesc()
                : cajaRepository.findTop50ByTiendaIdTiendaOrderByFechaAperturaDesc(tiendaSeleccionada));
        return "caja/index";
    }

    @PostMapping("/abrir")
    public String abrir(@RequestParam Integer idTienda, @RequestParam BigDecimal montoInicial,
                        @AuthenticationPrincipal AppUserDetails principal, HttpServletRequest request,
                        RedirectAttributes flash) {
        try {
            Caja caja = cajaService.abrir(idTienda, montoInicial, principal, request);
            flash.addFlashAttribute("ok", "Caja #" + caja.getIdCaja() + " abierta correctamente.");
        } catch (IllegalArgumentException ex) {
            auditoriaService.registrar(principal.getUsername(), "APERTURA_CAJA",
                    "Operación rechazada: " + ex.getMessage(), "FALLIDO", request);
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/caja?idTienda=" + idTienda;
    }

    @PostMapping("/{idCaja}/movimientos")
    public String movimiento(@PathVariable Long idCaja, @RequestParam String tipo,
                             @RequestParam String concepto, @RequestParam BigDecimal monto,
                             @AuthenticationPrincipal AppUserDetails principal, HttpServletRequest request,
                             RedirectAttributes flash) {
        try {
            cajaService.registrarMovimiento(idCaja, tipo, concepto, monto, principal, request);
            flash.addFlashAttribute("ok", "Movimiento de caja registrado.");
        } catch (IllegalArgumentException ex) {
            auditoriaService.registrar(principal.getUsername(), "MOVIMIENTO_CAJA",
                    "Operación rechazada en caja #" + idCaja + ": " + ex.getMessage(), "FALLIDO", request);
            flash.addFlashAttribute("error", ex.getMessage());
        }
        Caja caja = cajaRepository.findById(idCaja).orElseThrow(() -> new IllegalArgumentException("La caja no existe."));
        return "redirect:/caja?idTienda=" + caja.getTienda().getIdTienda();
    }

    @PostMapping("/{idCaja}/cerrar")
    public String cerrar(@PathVariable Long idCaja, @RequestParam BigDecimal efectivoContado,
                         @RequestParam(required = false) String observacion,
                         @AuthenticationPrincipal AppUserDetails principal, HttpServletRequest request,
                         RedirectAttributes flash) {
        Caja caja;
        try {
            caja = cajaService.cerrar(idCaja, efectivoContado, observacion, principal, request);
            flash.addFlashAttribute("ok", "Caja cerrada. Diferencia del arqueo: S/ " + caja.getDiferencia());
        } catch (IllegalArgumentException ex) {
            auditoriaService.registrar(principal.getUsername(), "CIERRE_ARQUEO_CAJA",
                    "Operación rechazada en caja #" + idCaja + ": " + ex.getMessage(), "FALLIDO", request);
            flash.addFlashAttribute("error", ex.getMessage());
            caja = cajaRepository.findById(idCaja).orElseThrow(() -> new IllegalArgumentException("La caja no existe."));
        }
        return "redirect:/caja?idTienda=" + caja.getTienda().getIdTienda();
    }
}
