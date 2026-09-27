package pe.edu.cibertec.bodega.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import pe.edu.cibertec.bodega.model.Tienda;
import pe.edu.cibertec.bodega.repository.TiendaRepository;

@Controller
@RequestMapping("/tiendas")
public class TiendaController {

    @Autowired
    private TiendaRepository tiendaRepo;

    @GetMapping
    public String listar(Model model) {
        // Muestra todas las tiendas registradas
        model.addAttribute("tiendas", tiendaRepo.findAll());
        return "tiendas/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("tienda", new Tienda());
        return "tiendas/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Tienda tienda) {
        if (tienda.getEstado() == null) {
            tienda.setEstado(true);
        }
        tiendaRepo.save(tienda);
        return "redirect:/tiendas";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable("id") Integer id, Model model) {
        Tienda tienda = tiendaRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ID de tienda inválido"));
        model.addAttribute("tienda", tienda);
        return "tiendas/formulario";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable("id") Integer id) {
        Tienda tienda = tiendaRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ID de tienda inválido"));
        tienda.setEstado(false); // Baja lógica
        tiendaRepo.save(tienda);
        return "redirect:/tiendas";
    }
}