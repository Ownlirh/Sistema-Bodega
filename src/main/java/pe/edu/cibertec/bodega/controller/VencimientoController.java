package pe.edu.cibertec.bodega.controller;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.edu.cibertec.bodega.dto.*;
import pe.edu.cibertec.bodega.model.Merma;
import pe.edu.cibertec.bodega.repository.*;
import pe.edu.cibertec.bodega.service.*;
import java.time.*;

@Controller @RequiredArgsConstructor @Slf4j
public class VencimientoController {
    private final LoteRepository lotes;
    private final MermaRepository mermas;
    private final ProductoRepository productos;
    private final TiendaRepository tiendas;
    private final LoteService loteService;
    private final MermaService mermaService;

    @GetMapping("/vencimientos")
    public String lotes(Model model) {
        model.addAttribute("loteForm", new LoteForm()); return pantallaLotes(model);
    }
    @PostMapping("/vencimientos")
    public String guardarLote(@Valid @ModelAttribute LoteForm loteForm, BindingResult errores,
                              Model model, RedirectAttributes flash) {
        if (!errores.hasErrors()) {
            try {
                var lote = loteService.registrar(loteForm, 1); // Sustituir por usuario autenticado, alumno 3.
                flash.addFlashAttribute("ok", "Lote #" + lote.getIdLote() + " asignado al stock existente. El stock total no cambió.");
                return "redirect:/vencimientos";
            } catch (IllegalArgumentException e) { errores.reject("lote", e.getMessage()); }
            catch (DataAccessException e) {
                log.error("No se pudo asignar lote", e);
                errores.reject("lote", "No se pudo guardar. Verifique la base o reintente si el inventario está siendo actualizado.");
            }
        }
        return pantallaLotes(model);
    }
    private String pantallaLotes(Model model) {
        var hoy = LocalDate.now(ZoneId.of("America/Lima"));
        var lista = lotes.findByDisponibleGreaterThanOrderByVencimientoAscIdLoteAsc(0);
        model.addAttribute("lotes", lista); model.addAttribute("hoy", hoy);
        model.addAttribute("limite", hoy.plusDays(30));
        model.addAttribute("vencidos", lista.stream().filter(l -> l.getVencimiento().isBefore(hoy)).count());
        model.addAttribute("proximos", lista.stream().filter(l -> !l.getVencimiento().isBefore(hoy) && !l.getVencimiento().isAfter(hoy.plusDays(30))).count());
        model.addAttribute("productos", productos.findAll(Sort.by("descripcion")));
        model.addAttribute("tiendas", tiendas.findByEstadoTrue());
        return "vencimientos/lista";
    }
    @GetMapping("/mermas")
    public String mermas(Model model) {
        model.addAttribute("mermaForm", new MermaForm()); return pantallaMermas(model);
    }
    @PostMapping("/mermas")
    public String guardarMerma(@Valid @ModelAttribute MermaForm mermaForm, BindingResult errores,
                               Model model, RedirectAttributes flash) {
        if (!errores.hasErrors()) {
            try {
                var merma = mermaService.registrar(mermaForm, 1);
                flash.addFlashAttribute("ok", "Merma #" + merma.getIdMerma() + " registrada. Saldo del lote, stock y kardex actualizados.");
                return "redirect:/mermas";
            } catch (IllegalArgumentException e) { errores.reject("merma", e.getMessage()); }
            catch (DataAccessException e) {
                log.error("No se pudo registrar merma", e);
                errores.reject("merma", "No se pudo guardar. Verifique la base o reintente si el inventario está siendo actualizado.");
            }
        }
        return pantallaMermas(model);
    }
    private String pantallaMermas(Model model) {
        model.addAttribute("lotes", lotes.findByDisponibleGreaterThanOrderByVencimientoAscIdLoteAsc(0));
        model.addAttribute("motivos", Merma.Motivo.values());
        model.addAttribute("mermas", mermas.findAll(PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "fecha", "idMerma"))).getContent());
        return "mermas/lista";
    }
}
