package pe.edu.cibertec.bodega.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import pe.edu.cibertec.bodega.model.Producto;
import pe.edu.cibertec.bodega.repository.CategoriaRepository;
import pe.edu.cibertec.bodega.repository.ProductoRepository;
import pe.edu.cibertec.bodega.repository.ProveedorRepository;

@Controller
@RequestMapping("/productos")
public class ProductoController {

    @Autowired
    private ProductoRepository productoRepo;

    @Autowired
    private CategoriaRepository categoriaRepo;

    @Autowired
    private ProveedorRepository proveedorRepo;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("productos", productoRepo.findAll());
        return "productos/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("producto", new Producto());
        model.addAttribute("categorias", categoriaRepo.findAll());
        model.addAttribute("proveedores", proveedorRepo.findAll());
        return "productos/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Producto producto) {
        
        if (producto.getEstado() == null) {
            producto.setEstado(true);
        }
        productoRepo.save(producto);
        return "redirect:/productos";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable("id") Integer id, Model model) {
        Producto producto = productoRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ID de producto inválido"));
        model.addAttribute("producto", producto);
        model.addAttribute("categorias", categoriaRepo.findAll());
        model.addAttribute("proveedores", proveedorRepo.findAll());
        return "productos/formulario";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable("id") Integer id) {
        Producto producto = productoRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ID de producto inválido"));
        producto.setEstado(false); // Baja lógica
        productoRepo.save(producto);
        return "redirect:/productos";
    }
}