package pe.edu.cibertec.bodega.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import pe.edu.cibertec.bodega.model.Producto;
import pe.edu.cibertec.bodega.model.Stock;
import pe.edu.cibertec.bodega.model.Tienda;
import pe.edu.cibertec.bodega.model.Usuario;
import pe.edu.cibertec.bodega.model.Venta;
import pe.edu.cibertec.bodega.model.VentaDetalle;
import pe.edu.cibertec.bodega.repository.ProductoRepository;
import pe.edu.cibertec.bodega.repository.StockRepository;
import pe.edu.cibertec.bodega.repository.TiendaRepository;
import pe.edu.cibertec.bodega.repository.VentaDetalleRepository;
import pe.edu.cibertec.bodega.repository.VentaRepository;
import pe.edu.cibertec.bodega.service.VentaService;
import pe.edu.cibertec.bodega.service.VentaService.ItemVenta;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class VentaController {

    @Autowired
    private VentaService ventaService;

    @Autowired
    private VentaDetalleRepository ventaDetalleRepo;

    @Autowired
    private ProductoRepository productoRepo;

    @Autowired
    private TiendaRepository tiendaRepo;

    @Autowired
    private VentaRepository ventaRepo;

    @Autowired
    private StockRepository stockRepo;


    // ============================================================
    // NUEVA VENTA
    // ============================================================
    @GetMapping("/venta/nueva")
    public String nuevaVenta(Model model) {

        List<Producto> productos =
                productoRepo.findByEstadoTrue();

        List<Tienda> tiendas =
                tiendaRepo.findAll();

        model.addAttribute("productos", productos);
        model.addAttribute("tiendas", tiendas);

        return "venta/nueva";
    }


    // ============================================================
    // STOCK POR TIENDA
    // ============================================================
    @GetMapping("/venta/stock")
    @ResponseBody
    public Map<Integer, Integer> stockPorTienda(
            @RequestParam Integer idTienda) {

        Map<Integer, Integer> stockMap =
                new HashMap<>();

        if (idTienda == null) {
            return stockMap;
        }

        List<Stock> stocks =
                stockRepo.findByTiendaIdTienda(idTienda);

        for (Stock stock : stocks) {

            if (stock.getProducto() != null) {

                stockMap.put(
                        stock.getProducto().getIdProducto(),
                        stock.getCantidad());
            }
        }

        return stockMap;
    }


    // ============================================================
    // REGISTRAR VENTA
    // ============================================================
    @PostMapping("/venta/registrar")
    public String registrarVenta(
            @RequestParam Integer idTienda,
            @RequestParam Integer idUsuario,
            @RequestParam List<Integer> idProducto,
            @RequestParam List<Integer> cantidad,
            Model model) {

        try {

            Usuario usuario = new Usuario();
            usuario.setIdUsuario(idUsuario);

            List<ItemVenta> items =
                    new ArrayList<>();

            for (int i = 0;
                 i < idProducto.size();
                 i++) {

                Integer producto =
                        idProducto.get(i);

                Integer cant =
                        cantidad.get(i);

                if (producto != null
                        && cant != null
                        && cant > 0) {

                    items.add(
                            new ItemVenta(
                                    producto,
                                    cant));
                }
            }

            ventaService.registrar(
                    idTienda,
                    usuario,
                    items);

            return "redirect:/consultas/ventas";

        } catch (RuntimeException e) {

            model.addAttribute(
                    "error",
                    e.getMessage());

            model.addAttribute(
                    "productos",
                    productoRepo.findByEstadoTrue());

            model.addAttribute(
                    "tiendas",
                    tiendaRepo.findAll());

            return "venta/nueva";
        }
    }


    // ============================================================
    // CONSULTAR VENTAS
    // ============================================================
    @GetMapping("/consultas/ventas")
    public String listarVentas(
            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE)
            LocalDate desde,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE)
            LocalDate hasta,

            @RequestParam(required = false)
            Integer idTienda,

            Model model) {

        List<Venta> ventas;
        String error = null;

        if ((desde == null && hasta != null)
                || (desde != null && hasta == null)) {

            error =
                    "Debe seleccionar las fechas Desde y Hasta.";

            ventas =
                    ventaRepo.findAll(
                            Sort.by(
                                    Sort.Direction.DESC,
                                    "fecha"));

        } else if (desde != null && hasta != null) {

            if (desde.isAfter(hasta)) {

                error =
                        "La fecha Desde no puede ser posterior a la fecha Hasta.";

                ventas =
                        ventaRepo.findAll(
                                Sort.by(
                                        Sort.Direction.DESC,
                                        "fecha"));

            } else {

                ventas =
                        ventaRepo.buscarPorFechaYTienda(
                                desde.atTime(LocalTime.MIN),
                                hasta.atTime(LocalTime.MAX),
                                idTienda);
            }

        } else {

            ventas =
                    ventaRepo.findAll(
                            Sort.by(
                                    Sort.Direction.DESC,
                                    "fecha"));
        }

        model.addAttribute(
                "ventas",
                ventas);

        model.addAttribute(
                "tiendas",
                tiendaRepo.findAll());

        model.addAttribute(
                "desde",
                desde);

        model.addAttribute(
                "hasta",
                hasta);

        model.addAttribute(
                "idTienda",
                idTienda);

        if (error != null) {
            model.addAttribute(
                    "error",
                    error);
        }

        return "venta/lista";
    }


    // ============================================================
    // VER COMPROBANTE
    // ============================================================
    @GetMapping("/venta/{id}/comprobante")
    public String comprobanteVenta(
            @PathVariable Integer id,
            @RequestParam(
                    defaultValue = "false")
            boolean autoPrint,
            Model model) {

        Venta venta =
                ventaRepo.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "La venta no existe."));

        List<VentaDetalle> detalles =
                ventaDetalleRepo
                        .findByVentaIdVenta(id);

        model.addAttribute(
                "venta",
                venta);

        model.addAttribute(
                "detalles",
                detalles);

        model.addAttribute(
                "autoPrint",
                autoPrint);

        return "venta/comprobante";
    }


    // ============================================================
    // MOSTRAR FORMULARIO DE EDICIÓN
    // ============================================================
    @GetMapping("/venta/{id}/editar")
    public String mostrarEditar(
            @PathVariable Integer id,
            Model model) {

        Venta venta =
                ventaRepo.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "La venta no existe."));

        if (!Boolean.TRUE.equals(
                venta.getEstado())) {

            throw new RuntimeException(
                    "No se puede editar una venta anulada.");
        }

        cargarDatosEdicion(
                venta,
                model,
                null);

        return "venta/editar";
    }


    // ============================================================
    // GUARDAR EDICIÓN
    // ============================================================
    @PostMapping("/venta/{id}/editar")
    public String guardarEdicion(
            @PathVariable Integer id,
            @RequestParam Integer idTienda,
            @RequestParam List<Integer> idProducto,
            @RequestParam List<Integer> cantidad,
            Model model) {

        try {

            List<ItemVenta> items =
                    new ArrayList<>();

            for (int i = 0;
                 i < idProducto.size();
                 i++) {

                Integer producto =
                        idProducto.get(i);

                Integer cant =
                        cantidad.get(i);

                if (producto != null
                        && cant != null
                        && cant > 0) {

                    items.add(
                            new ItemVenta(
                                    producto,
                                    cant));
                }
            }

            ventaService.editar(
                    id,
                    idTienda,
                    items);

            return "redirect:/consultas/ventas";

        } catch (RuntimeException e) {

            Venta venta =
                    ventaRepo.findById(id)
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "La venta no existe."));

            cargarDatosEdicion(
                    venta,
                    model,
                    e.getMessage());

            return "venta/editar";
        }
    }


    // ============================================================
    // CARGAR DATOS DE EDICIÓN
    // ============================================================
    private void cargarDatosEdicion(
            Venta venta,
            Model model,
            String error) {

        model.addAttribute(
                "venta",
                venta);

        model.addAttribute(
                "productos",
                productoRepo.findByEstadoTrue());

        model.addAttribute(
                "tiendas",
                tiendaRepo.findAll());

        model.addAttribute(
                "detalles",
                ventaDetalleRepo
                        .findByVentaIdVenta(
                                venta.getIdVenta()));

        if (error != null) {

            model.addAttribute(
                    "error",
                    error);
            
        }
        
    }
 // ============================================================
 // ANULAR VENTA
 // ============================================================
 @PostMapping("/venta/{id}/anular")
 public String anularVenta(
         @PathVariable Integer id,
         org.springframework.web.servlet.mvc.support.RedirectAttributes flash) {

     try {

         ventaService.anular(id);

         flash.addFlashAttribute(
                 "ok",
                 "La venta #" + id + " fue anulada correctamente.");

     } catch (RuntimeException e) {

         flash.addFlashAttribute(
                 "error",
                 e.getMessage());
     }

     return "redirect:/consultas/ventas";
 }
}