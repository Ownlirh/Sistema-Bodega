package pe.edu.cibertec.bodega.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import pe.edu.cibertec.bodega.model.Proveedor;
import pe.edu.cibertec.bodega.repository.ProveedorRepository;

@Controller
@RequestMapping("/proveedores")
public class ProveedorController {

    @Autowired
    private ProveedorRepository proveedorRepo;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("proveedores", proveedorRepo.findAll());
        return "proveedores/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("proveedor", new Proveedor());
        return "proveedores/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Proveedor proveedor) {
        // Asegurar estado activo al crear
        if (proveedor.getEstado() == null) {
            proveedor.setEstado(true);
        }
        proveedorRepo.save(proveedor);
        return "redirect:/proveedores";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable("id") Integer id, Model model) {
        Proveedor proveedor = proveedorRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ID de proveedor inválido"));
        model.addAttribute("proveedor", proveedor);
        return "proveedores/formulario";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable("id") Integer id) {
        Proveedor proveedor = proveedorRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ID de proveedor inválido"));
        proveedor.setEstado(false); // Baja lógica
        proveedorRepo.save(proveedor);
        return "redirect:/proveedores";
    }
}