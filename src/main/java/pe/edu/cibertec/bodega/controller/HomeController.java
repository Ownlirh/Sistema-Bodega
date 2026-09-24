package pe.edu.cibertec.bodega.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import pe.edu.cibertec.bodega.repository.*;

/**
 * Pantalla de inicio.
 *
 * Es la referencia minima de un controlador MVC: recibe la peticion, pide
 * datos a los repositorios, los deja en el Model y devuelve el nombre de la
 * plantilla (sin .html) que Thymeleaf va a renderizar.
 */
@Controller
public class HomeController {

    @Autowired private TiendaRepository tiendaRepo;
    @Autowired private UsuarioRepository usuarioRepo;
    @Autowired private ProductoRepository productoRepo;
    @Autowired private VentaRepository ventaRepo;

    @GetMapping("/")
    public String inicio(Model model) {
        model.addAttribute("tiendas", tiendaRepo.count());
        model.addAttribute("usuarios", usuarioRepo.count());
        model.addAttribute("productos", productoRepo.count());
        model.addAttribute("ventas", ventaRepo.count());
        return "index";   // -> templates/index.html
    }
}
