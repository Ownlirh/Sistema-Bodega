package pe.edu.cibertec.bodega.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.edu.cibertec.bodega.model.Usuario;
import pe.edu.cibertec.bodega.repository.*;
import pe.edu.cibertec.bodega.service.TransferenciaService;

/**
 * Pantalla de transferencias entre tiendas (integrante 7).
 *
 *   GET  /transferencias          formulario + historial
 *   POST /transferencias/guardar  registra la transferencia
 */
@Controller
@RequestMapping("/transferencias")
public class TransferenciaController {

    @Autowired private TransferenciaService transferenciaService;
    @Autowired private TransferenciaRepository transferenciaRepo;
    @Autowired private ProductoRepository productoRepo;
    @Autowired private TiendaRepository tiendaRepo;
    @Autowired private UsuarioRepository usuarioRepo;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("productos", productoRepo.findByEstadoTrue());
        model.addAttribute("tiendas", tiendaRepo.findByEstadoTrue());
        model.addAttribute("transferencias", transferenciaRepo.findAllByOrderByFechaDesc());
        return "transferencias/lista";   // -> templates/transferencias/lista.html
    }

    @PostMapping("/guardar")
    public String guardar(@RequestParam Integer idProducto,
                          @RequestParam Integer idOrigen,
                          @RequestParam Integer idDestino,
                          @RequestParam Integer cantidad,
                          @RequestParam(required = false) String observacion,
                          RedirectAttributes flash) {
        try {
            transferenciaService.registrar(idProducto, idOrigen, idDestino,
                    cantidad, observacion, usuarioActual());
            flash.addFlashAttribute("ok", "Transferencia registrada correctamente.");
        } catch (RuntimeException e) {
            // El mensaje viene de las validaciones del servicio
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/transferencias";
    }

    /**
     * TEMPORAL: todavia no hay login (lo hace el integrante 3).
     * Mientras tanto se usa el usuario con id 1 (administrador).
     * Cuando exista Spring Security, aqui se toma el usuario de la sesion.
     */
    private Usuario usuarioActual() {
        return usuarioRepo.findById(1)
                .orElseThrow(() -> new RuntimeException("No existe el usuario 1."));
    }
}
