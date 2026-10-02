package pe.edu.cibertec.bodega.controller;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import pe.edu.cibertec.bodega.dto.*;
import pe.edu.cibertec.bodega.repository.TiendaRepository;
import pe.edu.cibertec.bodega.service.*;

@Controller @RequiredArgsConstructor @Slf4j
@RequestMapping("/reportes")
public class ReporteController {
    private final ReporteVentasService ventas;
    private final ReporteVentasPdf pdf;
    private final TiendaRepository tiendas;
    @GetMapping
    public String pantalla(@Valid @ModelAttribute("filtro") ReporteVentasForm filtro, BindingResult errores, Model model) {
        if (!errores.hasErrors()) {
            try { agregarReporte(model, ventas.consultar(filtro)); }
            catch (IllegalArgumentException e) { errores.reject("reporte", e.getMessage()); }
        }
        model.addAttribute("tiendas", tiendas.findAll(Sort.by("nombre")));
        return "reportes/ventas";
    }
    @GetMapping("/ventas/pdf")
    public Object descargar(@Valid @ModelAttribute("filtro") ReporteVentasForm filtro, BindingResult errores,
                            Model model, HttpServletResponse response) {
        if (!errores.hasErrors()) {
            try {
                var reporte = ventas.consultar(filtro);
                return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ventas-" + filtro.getDesde() + "-" + filtro.getHasta() + ".pdf")
                    .cacheControl(CacheControl.noStore()).body(pdf.generar(reporte));
            } catch (IllegalArgumentException e) { errores.reject("reporte", e.getMessage()); }
            catch (IllegalStateException e) {
                log.error("Error al exportar PDF de ventas", e);
                errores.reject("pdf", "No se pudo generar el PDF. Revise la consola del servidor.");
                response.setStatus(500);
            }
        }
        if (response.getStatus() != 500) response.setStatus(400);
        model.addAttribute("tiendas", tiendas.findAll(Sort.by("nombre")));
        return "reportes/ventas";
    }
    private void agregarReporte(Model model, ReporteVentas reporte) {
        model.addAttribute("reporte", reporte);
        model.addAttribute("vista", reporte.getFilas().stream().limit(100).toList());
    }
}
