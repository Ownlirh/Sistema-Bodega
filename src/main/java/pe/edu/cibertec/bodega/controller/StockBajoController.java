package pe.edu.cibertec.bodega.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pe.edu.cibertec.bodega.model.Stock;
import pe.edu.cibertec.bodega.model.Tienda;
import pe.edu.cibertec.bodega.repository.StockRepository;
import pe.edu.cibertec.bodega.repository.TiendaRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Consulta de stock bajo el minimo (integrante 7).
 *
 *   GET /inventario/stock-bajo             todas las tiendas
 *   GET /inventario/stock-bajo?idTienda=1  una tienda
 *
 * Usa la consulta findStockBajo() que ya existia en StockRepository:
 * trae los productos cuya cantidad es menor o igual a su stock minimo.
 */
@Controller
@RequestMapping("/inventario/stock-bajo")
public class StockBajoController {

    @Autowired private StockRepository stockRepo;
    @Autowired private TiendaRepository tiendaRepo;

    @GetMapping
    public String consultar(@RequestParam(required = false) Integer idTienda, Model model) {
        List<Tienda> tiendas = tiendaRepo.findByEstadoTrue();
        List<Stock> resultado = new ArrayList<>();

        if (idTienda != null) {
            resultado = stockRepo.findStockBajo(idTienda);
        } else {
            // Sin filtro: juntamos el stock bajo de cada tienda
            for (Tienda t : tiendas) {
                resultado.addAll(stockRepo.findStockBajo(t.getIdTienda()));
            }
        }

        model.addAttribute("tiendas", tiendas);
        model.addAttribute("idTienda", idTienda);
        model.addAttribute("stocks", resultado);
        return "inventario/stock-bajo";   // -> templates/inventario/stock-bajo.html
    }
}
