package pe.edu.cibertec.bodega.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pe.edu.cibertec.bodega.repository.TiendaRepository;
import pe.edu.cibertec.bodega.service.PanelService;

/**
 * Panel de tendencias (integrante 1).
 *
 *   GET /panel              todas las tiendas
 *   GET /panel?idTienda=1   una sola
 *
 * SecurityConfig ya reserva /panel/** al ADMINISTRADOR, porque la vista cruza
 * datos de todas las tiendas.
 */
@Controller
@RequiredArgsConstructor
public class PanelController {

    private final PanelService servicio;
    private final TiendaRepository tiendas;

    @GetMapping("/panel")
    public String mostrar(@RequestParam(required = false) Integer idTienda, Model model) {
        try {
            model.addAttribute("panel", servicio.consultar(idTienda));
        } catch (IllegalArgumentException e) {
            // Tienda inexistente: se avisa y se muestra el formulario vacio,
            // en vez de reventar con un 500.
            model.addAttribute("error", e.getMessage());
        }
        model.addAttribute("idTiendaSeleccionada", idTienda);
        model.addAttribute("tiendas", tiendas.findAll(Sort.by("nombre")));
        return "panel/index";
    }
}
