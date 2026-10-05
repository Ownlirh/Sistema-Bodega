package pe.edu.cibertec.bodega.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pe.edu.cibertec.bodega.model.Kardex;
import pe.edu.cibertec.bodega.repository.*;

import java.util.List;

/**
 * Consulta del kardex (integrante 7).
 *
 *   GET /kardex                          todos los movimientos
 *   GET /kardex?idTienda=1               movimientos de una tienda
 *   GET /kardex?idProducto=1             movimientos de un producto
 *   GET /kardex?idProducto=1&idTienda=1  kardex de un producto en una tienda
 *
 * Los filtros son opcionales (required = false): si no llegan, valen null.
 */
@Controller
@RequestMapping("/kardex")
public class KardexController {

    @Autowired private KardexRepository kardexRepo;
    @Autowired private ProductoRepository productoRepo;
    @Autowired private TiendaRepository tiendaRepo;
    @Autowired private StockRepository stockRepo;

    @GetMapping
    public String consultar(@RequestParam(required = false) Integer idProducto,
                            @RequestParam(required = false) Integer idTienda,
                            Model model) {

        // Elegimos la consulta segun los filtros que llegaron
        List<Kardex> movimientos;
        if (idProducto != null && idTienda != null) {
            // Kardex "clasico": un producto en una tienda, del mas antiguo al mas nuevo
            movimientos = kardexRepo.findByProductoIdProductoAndTiendaIdTiendaOrderByFechaAsc(idProducto, idTienda);

            // Stock actual de ese producto en esa tienda (para mostrarlo arriba)
            stockRepo.findByProductoIdProductoAndTiendaIdTienda(idProducto, idTienda)
                    .ifPresent(s -> model.addAttribute("stockActual", s.getCantidad()));
        } else if (idTienda != null) {
            movimientos = kardexRepo.findByTiendaIdTiendaOrderByFechaDesc(idTienda);
        } else if (idProducto != null) {
            movimientos = kardexRepo.findByProductoIdProductoOrderByFechaDesc(idProducto);
        } else {
            movimientos = kardexRepo.findAllByOrderByFechaDesc();
        }

        // Datos para los combos del filtro
        model.addAttribute("productos", productoRepo.findByEstadoTrue());
        model.addAttribute("tiendas", tiendaRepo.findByEstadoTrue());

        // Lo que el usuario eligio, para que los combos sigan seleccionados
        model.addAttribute("idProducto", idProducto);
        model.addAttribute("idTienda", idTienda);

        model.addAttribute("movimientos", movimientos);
        return "kardex/lista";   // -> templates/kardex/lista.html
    }
}
