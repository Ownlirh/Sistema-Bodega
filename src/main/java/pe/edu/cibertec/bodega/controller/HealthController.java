package pe.edu.cibertec.bodega.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.cibertec.bodega.repository.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Comprobacion de que la aplicacion levanta y habla con la base de datos.
 *
 * Es temporal y esta aqui solo para que cada integrante verifique su entorno
 * el primer dia sin depender del trabajo de los demas.
 *
 *   http://localhost:8080/health/db
 *
 * Si devuelve los conteos, la conexion y las entidades estan bien.
 * BORRAR esta clase antes de la entrega final: no es parte del sistema.
 */
@RestController
@RequestMapping("/health")
public class HealthController {

    @Autowired private TiendaRepository tiendaRepo;
    @Autowired private UsuarioRepository usuarioRepo;
    @Autowired private ProductoRepository productoRepo;
    @Autowired private CategoriaRepository categoriaRepo;
    @Autowired private ProveedorRepository proveedorRepo;
    @Autowired private StockRepository stockRepo;
    @Autowired private VentaRepository ventaRepo;
    @Autowired private CompraRepository compraRepo;
    @Autowired private GastoRepository gastoRepo;

    @GetMapping("/db")
    public Map<String, Object> verificarConexion() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("estado", "conectado");
        r.put("tiendas", tiendaRepo.count());
        r.put("usuarios", usuarioRepo.count());
        r.put("categorias", categoriaRepo.count());
        r.put("proveedores", proveedorRepo.count());
        r.put("productos", productoRepo.count());
        r.put("stock", stockRepo.count());
        r.put("ventas", ventaRepo.count());
        r.put("compras", compraRepo.count());
        r.put("gastos", gastoRepo.count());
        return r;
    }

    /**
     * Prueba que las relaciones entre entidades funcionan.
     * Si una relacion esta mal mapeada, aqui revienta.
     */
    @GetMapping("/relaciones")
    public Map<String, Object> verificarRelaciones() {
        Map<String, Object> r = new LinkedHashMap<>();

        productoRepo.findByCodigo("P001").ifPresent(p -> {
            r.put("producto", p.getDescripcion());
            r.put("categoria", p.getCategoria().getNombre());
            r.put("proveedor", p.getProveedor() != null
                    ? p.getProveedor().getRazonSocial() : "sin proveedor");
            r.put("margenUnitario", p.getMargen());
        });

        usuarioRepo.findByUsername("vendedor1").ifPresent(u -> {
            r.put("usuario", u.getNombreCompleto());
            r.put("rol", u.getRol().getNombre());
            r.put("tienda", u.getTienda() != null
                    ? u.getTienda().getNombre() : "todas");
        });

        r.put("stockBajoEnTienda1", stockRepo.findStockBajo(1).size());
        return r;
    }
}
