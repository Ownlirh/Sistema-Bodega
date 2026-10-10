package pe.edu.cibertec.bodega.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataAccessException;
import pe.edu.cibertec.bodega.dto.CompraForm;
import pe.edu.cibertec.bodega.repository.*;
import pe.edu.cibertec.bodega.service.CompraService;

@Controller
@RequestMapping("/compras")
@RequiredArgsConstructor
public class CompraController {
    private final CompraService servicio;
    private final CompraRepository compras;
    private final ProductoRepository productos;
    private final ProveedorRepository proveedores;
    private final TiendaRepository tiendas;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("compraForm", new CompraForm());
        return pantalla(model);
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute CompraForm compraForm, BindingResult errores,
                          Model model, RedirectAttributes flash) {
        if (errores.hasErrors()) return pantalla(model);
        try {
            // Integración pendiente con la sesión del alumno 3, igual que TransferenciaController.
            var compra = servicio.registrar(compraForm, 1);
            flash.addFlashAttribute("ok", "Compra #" + compra.getIdCompra() + " registrada. Stock y kardex actualizados.");
            return "redirect:/compras";
        } catch (IllegalArgumentException e) {
            errores.reject("compra", e.getMessage());
        } catch (DataAccessException e) {
            errores.reject("compra", "No se pudo guardar la compra. Revise la base de datos o intente nuevamente si otro proceso actualiza el stock.");
        }
        return pantalla(model);
    }

    private String pantalla(Model model) {
        model.addAttribute("productos", productos.findByEstadoTrue());
        model.addAttribute("proveedores", proveedores.findByEstadoTrue());
        model.addAttribute("tiendas", tiendas.findByEstadoTrue());
        model.addAttribute("compras", compras.findAll(PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "fecha", "idCompra"))).getContent());
        return "compras/lista";
    }
}
