package pe.edu.cibertec.bodega.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.edu.cibertec.bodega.dto.GastoForm;
import pe.edu.cibertec.bodega.repository.*;
import pe.edu.cibertec.bodega.service.GastoService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;

@Controller
@RequestMapping("/gastos")
@RequiredArgsConstructor
@Slf4j
public class GastoController {
    private final GastoService servicio;
    private final GastoRepository gastos;
    private final CategoriaGastoRepository categorias;
    private final TiendaRepository tiendas;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("gastoForm", new GastoForm());
        return pantalla(model);
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute GastoForm gastoForm, BindingResult errores,
                          Model model, RedirectAttributes flash) {
        if (errores.hasErrors()) return pantalla(model);
        try {
            // Temporal, igual que compras y transferencias. Integrar sesión del alumno 3.
            var gasto = servicio.registrar(gastoForm, 1);
            flash.addFlashAttribute("ok", "Gasto #" + gasto.getIdGasto() + " registrado correctamente.");
            return "redirect:/gastos";
        } catch (IllegalArgumentException e) {
            errores.reject("gasto", e.getMessage());
        } catch (DataAccessException e) {
            log.error("Error al registrar gasto operativo", e);
            errores.reject("gasto", "No se pudo guardar el gasto. Revise la conexión a la base de datos.");
        }
        return pantalla(model);
    }

    private String pantalla(Model model) {
        var lista = gastos.findAll(PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "fecha", "idGasto"))).getContent();
        model.addAttribute("gastos", lista);
        model.addAttribute("totalMostrado", lista.stream().map(g -> g.getMonto()).reduce(BigDecimal.ZERO, BigDecimal::add));
        model.addAttribute("categorias", categorias.findAll(Sort.by("nombre")));
        model.addAttribute("tiendas", tiendas.findByEstadoTrue());
        model.addAttribute("hoy", LocalDate.now(ZoneId.of("America/Lima")));
        return "gastos/lista";
    }
}
