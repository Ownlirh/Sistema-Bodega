package pe.edu.cibertec.bodega.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import pe.edu.cibertec.bodega.model.Lote;
import pe.edu.cibertec.bodega.model.Stock;
import pe.edu.cibertec.bodega.model.Tienda;
import pe.edu.cibertec.bodega.repository.LoteRepository;
import pe.edu.cibertec.bodega.repository.StockRepository;
import pe.edu.cibertec.bodega.repository.TiendaRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Alertas de stock y de productos por vencer (integrante 7).
 *
 *   GET /alertas
 *
 * Junta en una sola pantalla:
 *   - productos AGOTADOS (stock 0)
 *   - productos BAJO EL MINIMO (stock > 0 pero <= stock minimo)
 *   - lotes VENCIDOS (fecha de vencimiento ya paso)
 *   - lotes POR VENCER (vencen en los proximos 30 dias)
 *
 * Los lotes y su fecha de vencimiento vienen del modulo del integrante 6.
 */
@Controller
@RequestMapping("/alertas")
public class AlertaController {

    /** Cuantos dias antes avisamos que un lote esta por vencer. */
    private static final int DIAS_AVISO = 30;

    @Autowired private StockRepository stockRepo;
    @Autowired private TiendaRepository tiendaRepo;
    @Autowired private LoteRepository loteRepo;

    @GetMapping
    public String ver(Model model) {

        // ---------- Alertas de stock ----------
        List<Stock> agotados = new ArrayList<>();
        List<Stock> bajos = new ArrayList<>();

        for (Tienda t : tiendaRepo.findByEstadoTrue()) {
            for (Stock s : stockRepo.findStockBajo(t.getIdTienda())) {
                if (s.getCantidad() == 0) {
                    agotados.add(s);
                } else {
                    bajos.add(s);
                }
            }
        }

        // ---------- Alertas de vencimiento ----------
        LocalDate hoy = LocalDate.now();
        LocalDate limite = hoy.plusDays(DIAS_AVISO);

        List<Lote> vencidos = new ArrayList<>();
        List<Lote> porVencer = new ArrayList<>();

        // Solo lotes que todavia tienen unidades (disponible > 0), ordenados por vencimiento
        for (Lote l : loteRepo.findByDisponibleGreaterThanOrderByVencimientoAscIdLoteAsc(0)) {
            if (l.getVencimiento().isBefore(hoy)) {
                vencidos.add(l);
            } else if (!l.getVencimiento().isAfter(limite)) {
                porVencer.add(l);
            }
        }

        model.addAttribute("agotados", agotados);
        model.addAttribute("bajos", bajos);
        model.addAttribute("vencidos", vencidos);
        model.addAttribute("porVencer", porVencer);
        model.addAttribute("hoy", hoy);
        model.addAttribute("diasAviso", DIAS_AVISO);
        return "alertas/lista";   // -> templates/alertas/lista.html
    }
}
