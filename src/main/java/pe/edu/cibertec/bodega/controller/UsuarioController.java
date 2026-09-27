package pe.edu.cibertec.bodega.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import pe.edu.cibertec.bodega.model.Usuario;
import pe.edu.cibertec.bodega.repository.RolRepository;
import pe.edu.cibertec.bodega.repository.TiendaRepository;
import pe.edu.cibertec.bodega.repository.UsuarioRepository;

@Controller
@RequestMapping("/empleados")
public class UsuarioController {

    @Autowired
    private UsuarioRepository usuarioRepo;

    @Autowired
    private RolRepository rolRepo;

    @Autowired
    private TiendaRepository tiendaRepo;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("usuarios", usuarioRepo.findAll());
        return "usuarios/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("usuario", new Usuario());
        model.addAttribute("roles", rolRepo.findAll());
        model.addAttribute("tiendas", tiendaRepo.findAll());
        return "usuarios/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Usuario usuario) {
        if (usuario.getEstado() == null) {
            usuario.setEstado(true);
        }
        // Spring mapeará automáticamente tienda nula si se selecciona "Todas (Admin)"
        usuarioRepo.save(usuario);
        return "redirect:/empleados";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable("id") Integer id, Model model) {
        Usuario usuario = usuarioRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ID de empleado inválido"));
        model.addAttribute("usuario", usuario);
        model.addAttribute("roles", rolRepo.findAll());
        model.addAttribute("tiendas", tiendaRepo.findAll());
        return "usuarios/formulario";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable("id") Integer id) {
        Usuario usuario = usuarioRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ID de empleado inválido"));
        usuario.setEstado(false); // Baja lógica
        usuarioRepo.save(usuario);
        return "redirect:/empleados";
    }
}