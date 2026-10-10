package pe.edu.cibertec.bodega.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.edu.cibertec.bodega.model.Usuario;
import pe.edu.cibertec.bodega.repository.*;
import pe.edu.cibertec.bodega.service.AjusteService;

/**
 * Ajustes de inventario por conteo fisico (integrante 7).
 *
 *   GET  /ajustes          formulario + historial
 *   POST /ajustes/guardar  registra el ajuste
 */
@Controller
@RequestMapping("/ajustes")
public class AjusteController {

    @Autowired private AjusteService ajusteService;
    @Autowired private AjusteRepository ajusteRepo;
    @Autowired private ProductoRepository productoRepo;
    @Autowired private TiendaRepository tiendaRepo;
    @Autowired private UsuarioRepository usuarioRepo;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("productos", productoRepo.findByEstadoTrue());
        model.addAttribute("tiendas", tiendaRepo.findByEstadoTrue());
        model.addAttribute("ajustes", ajusteRepo.findAllByOrderByFechaDesc());
        return "ajustes/lista";   // -> templates/ajustes/lista.html
    }

    @PostMapping("/guardar")
    public String guardar(@RequestParam Integer idProducto,
                          @RequestParam Integer idTienda,
                          @RequestParam Integer stockFisico,
                          @RequestParam String motivo,
                          RedirectAttributes flash) {
        try {
            ajusteService.registrar(idProducto, idTienda, stockFisico, motivo, usuarioActual());
            flash.addFlashAttribute("ok", "Ajuste registrado. El stock quedo igual al conteo fisico.");
        } catch (RuntimeException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/ajustes";
    }

    /** TEMPORAL: usuario 1 (admin) hasta que exista el login del integrante 3. */
    private Usuario usuarioActual() {
        return usuarioRepo.findById(1)
                .orElseThrow(() -> new RuntimeException("No existe el usuario 1."));
    }
}
