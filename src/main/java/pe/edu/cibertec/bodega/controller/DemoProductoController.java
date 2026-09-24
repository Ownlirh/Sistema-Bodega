package pe.edu.cibertec.bodega.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import pe.edu.cibertec.bodega.repository.ProductoRepository;

/**
 * EJEMPLO DE REFERENCIA - no es parte del sistema final.
 *
 * Muestra el camino completo de una pantalla:
 *
 *   navegador  ->  @GetMapping  ->  Repository  ->  Model  ->  plantilla
 *
 * El integrante 5 puede copiar este patron para los mantenimientos reales,
 * agregando los metodos que faltan:
 *
 *   @GetMapping("/nuevo")        formulario vacio
 *   @PostMapping("/guardar")     recibe el objeto con @ModelAttribute y graba
 *   @GetMapping("/editar/{id}")  formulario con los datos cargados
 *   @GetMapping("/eliminar/{id}") baja logica: poner estado en false
 *
 * Esta clase se borra cuando exista el mantenimiento de verdad.
 */
@Controller
@RequestMapping("/demo/productos")
public class DemoProductoController {

    @Autowired private ProductoRepository productoRepo;

    @GetMapping
    public String listar(Model model) {
        // findAll() lo hereda de JpaRepository: no hay que escribir la consulta.
        model.addAttribute("productos", productoRepo.findAll());
        return "demo/productos";   // -> templates/demo/productos.html
    }
}
