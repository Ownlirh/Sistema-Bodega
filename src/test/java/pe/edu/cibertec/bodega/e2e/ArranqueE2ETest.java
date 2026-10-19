package pe.edu.cibertec.bodega.e2e;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pe.edu.cibertec.bodega.repository.ProductoRepository;
import pe.edu.cibertec.bodega.repository.StockRepository;
import pe.edu.cibertec.bodega.repository.UsuarioRepository;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prueba de humo del entorno E2E. Si esta falla, las demas no significan nada.
 */
class ArranqueE2ETest extends BaseE2E {

    @Autowired UsuarioRepository usuarios;
    @Autowired ProductoRepository productos;
    @Autowired StockRepository stocks;

    @Test
    @DisplayName("la aplicacion entera levanta contra la base en memoria")
    void levantaYSiembra() {
        assertEquals(4, usuarios.count(), "deberian sembrarse 4 usuarios");
        assertEquals(2, productos.count(), "deberian sembrarse 2 productos");
        assertEquals(4, stocks.count(), "2 productos x 2 tiendas");
    }

    @Test
    @DisplayName("el escenario arranca con los stocks esperados")
    void stocksIniciales() {
        assertEquals(20, datos.stockDe(datos.getAgua(), datos.getCentral()));
        assertEquals(3, datos.stockDe(datos.getAgua(), datos.getNorte()));
        assertEquals(50, datos.stockDe(datos.getArroz(), datos.getCentral()));
        assertEquals(0, datos.stockDe(datos.getArroz(), datos.getNorte()));
    }

    @Test
    @DisplayName("cada prueba empieza con la base recien sembrada")
    void laBaseNoArrastraEstado() {
        // Si limpiar() no funcionara, el contador vendria acumulado de las
        // otras pruebas de esta misma clase.
        assertEquals(4, usuarios.count());
        assertEquals(20, datos.stockDe(datos.getAgua(), datos.getCentral()));
    }

    @Test
    @DisplayName("findStockBajo se ejecuta de verdad contra la base")
    void laConsultaDeStockBajoCorreContraSQL() {
        // En los @WebMvcTest del proyecto este repositorio esta mockeado, asi
        // que esta JPQL nunca se habia ejecutado en ninguna prueba.
        var bajosEnNorte = stocks.findStockBajo(datos.getNorte().getIdTienda());

        // Norte: AGUA 3 (minimo 5) y ARROZ 0 (minimo 10). Los dos bajo minimo.
        assertEquals(2, bajosEnNorte.size());

        var bajosEnCentral = stocks.findStockBajo(datos.getCentral().getIdTienda());
        assertTrue(bajosEnCentral.isEmpty(), "Central arranca con todo por encima del minimo");
    }
}
