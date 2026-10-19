package pe.edu.cibertec.bodega.e2e;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.cibertec.bodega.model.*;
import pe.edu.cibertec.bodega.repository.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Vacia la base y la vuelve a sembrar antes de cada prueba E2E.
 *
 * Sembramos por los repositorios y no por SQL a proposito: si una entidad
 * cambia, esto deja de compilar y nos enteramos, en vez de descubrirlo con un
 * INSERT que falla en tiempo de ejecucion.
 *
 * El escenario es siempre el mismo y deliberadamente chico, para que cada
 * prueba pueda afirmar numeros exactos:
 *
 *   2 tiendas    Central y Norte
 *   4 usuarios   admin (sin tienda), managerCentral, vendedorCentral, managerNorte
 *   2 productos  AGUA (stock minimo 5) y ARROZ (stock minimo 10)
 *   stock        AGUA: 20 en Central, 3 en Norte     <- Norte arranca bajo el minimo
 *                ARROZ: 50 en Central, 0 en Norte    <- Norte arranca agotado
 */
@Component
@RequiredArgsConstructor
@Getter
public class DatosDePrueba {

    public static final String CLAVE = "clave-de-prueba";

    private final JdbcTemplate jdbc;
    private final RolRepository roles;
    private final TiendaRepository tiendas;
    private final UsuarioRepository usuarios;
    private final CategoriaRepository categorias;
    private final ProveedorRepository proveedores;
    private final ProductoRepository productos;
    private final StockRepository stocks;
    private final CategoriaGastoRepository categoriasGasto;

    private Tienda central;
    private Tienda norte;
    private Usuario admin;
    private Usuario managerCentral;
    private Usuario vendedorCentral;
    private Usuario managerNorte;
    private Producto agua;
    private Producto arroz;
    private Proveedor proveedor;
    private CategoriaGasto servicios;

    /**
     * Deja la base como recien creada. Se apaga la integridad referencial un
     * momento para no tener que acertar el orden de borrado tabla por tabla.
     */
    public void limpiar() {
        jdbc.execute("SET REFERENTIAL_INTEGRITY FALSE");
        List<String> tablas = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables "
              + "WHERE table_schema = 'PUBLIC' AND table_type = 'BASE TABLE'",
                String.class);
        for (String tabla : tablas) {
            jdbc.execute("TRUNCATE TABLE \"" + tabla + "\" RESTART IDENTITY");
        }
        jdbc.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }

    @Transactional
    public void sembrar() {
        Rol rolAdmin = rol("ADMINISTRADOR");
        Rol rolManager = rol("MANAGER");
        Rol rolVendedor = rol("VENDEDOR");

        central = tienda("Bodega Central", "Lima");
        norte = tienda("Bodega Norte", "Comas");

        // El administrador no tiene tienda: ve todas.
        admin = usuario("admin", "Ana", "Torres", rolAdmin, null);
        managerCentral = usuario("manager.central", "Luis", "Ramirez", rolManager, central);
        vendedorCentral = usuario("vendedor.central", "Jorge", "Castro", rolVendedor, central);
        managerNorte = usuario("manager.norte", "Rosa", "Diaz", rolManager, norte);

        Categoria bebidas = new Categoria();
        bebidas.setNombre("Bebidas");
        bebidas.setEstado(true);
        bebidas = categorias.save(bebidas);

        proveedor = new Proveedor();
        proveedor.setRazonSocial("Distribuidora Prueba");
        proveedor.setRuc("20100000001");
        proveedor.setEstado(true);
        proveedor = proveedores.save(proveedor);

        agua = producto("AGUA", "Agua mineral 625ml", "1.00", "1.50", 5, bebidas);
        arroz = producto("ARROZ", "Arroz extra 1kg", "3.00", "4.20", 10, bebidas);

        stock(agua, central, 20);
        stock(agua, norte, 3);
        stock(arroz, central, 50);
        stock(arroz, norte, 0);

        servicios = new CategoriaGasto();
        servicios.setNombre("Servicios");
        servicios = categoriasGasto.save(servicios);
    }

    /** Cantidad en stock de un producto en una tienda, leida de nuevo de la base. */
    public int stockDe(Producto producto, Tienda tienda) {
        return stocks.findByProductoIdProductoAndTiendaIdTienda(
                        producto.getIdProducto(), tienda.getIdTienda())
                .map(Stock::getCantidad)
                .orElse(0);
    }

    private Rol rol(String nombre) {
        Rol r = new Rol();
        r.setNombre(nombre);
        return roles.save(r);
    }

    private Tienda tienda(String nombre, String distrito) {
        Tienda t = new Tienda();
        t.setNombre(nombre);
        t.setDistrito(distrito);
        t.setEstado(true);
        return tiendas.save(t);
    }

    private Usuario usuario(String username, String nombres, String apellidos, Rol rol, Tienda tienda) {
        Usuario u = new Usuario();
        u.setUsername(username);
        // En texto plano a proposito: LegacyAwarePasswordEncoder las acepta y
        // las migra a BCrypt tras el primer login. Una prueba verifica justo eso.
        u.setPassword(CLAVE);
        u.setNombres(nombres);
        u.setApellidos(apellidos);
        u.setRol(rol);
        u.setTienda(tienda);
        u.setEstado(true);
        return usuarios.save(u);
    }

    private Producto producto(String codigo, String descripcion, String compra, String venta,
                              int stockMinimo, Categoria categoria) {
        Producto p = new Producto();
        p.setCodigo(codigo);
        p.setDescripcion(descripcion);
        p.setUnidadMedida("UNIDAD");
        p.setPrecioCompra(new BigDecimal(compra));
        p.setPrecioVenta(new BigDecimal(venta));
        p.setStockMinimo(stockMinimo);
        p.setCategoria(categoria);
        p.setProveedor(proveedor);
        p.setEstado(true);
        return productos.save(p);
    }

    private void stock(Producto producto, Tienda tienda, int cantidad) {
        Stock s = new Stock();
        s.setProducto(producto);
        s.setTienda(tienda);
        s.setCantidad(cantidad);
        stocks.save(s);
    }
}
