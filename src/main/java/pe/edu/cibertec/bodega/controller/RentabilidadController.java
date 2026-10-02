package pe.edu.cibertec.bodega.controller;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import pe.edu.cibertec.bodega.dto.ReporteVentasForm;
import pe.edu.cibertec.bodega.repository.TiendaRepository;
import pe.edu.cibertec.bodega.service.ReporteRentabilidadService;

@Controller @RequiredArgsConstructor
public class RentabilidadController {
    private final ReporteRentabilidadService servicio;
    private final TiendaRepository tiendas;
    @GetMapping("/reportes/rentabilidad")
    public String mostrar(@Valid @ModelAttribute("filtro") ReporteVentasForm filtro, BindingResult errores, Model model) {
        if (!errores.hasErrors()) {
            try { model.addAttribute("reporte", servicio.consultar(filtro)); }
            catch (IllegalArgumentException e) { errores.reject("reporte", e.getMessage()); }
        }
        model.addAttribute("tiendas", tiendas.findAll(Sort.by("nombre")));
        return "reportes/rentabilidad";
    }
}
