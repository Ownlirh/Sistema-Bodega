package pe.edu.cibertec.bodega.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.*;
import pe.edu.cibertec.bodega.dto.CompraForm;
import pe.edu.cibertec.bodega.model.*;
import pe.edu.cibertec.bodega.repository.*;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CompraServiceTest {
    CompraRepository compras;
    ProveedorRepository proveedores;
    TiendaRepository tiendas;
    UsuarioRepository usuarios;
    StockRepository stocks;
    KardexService kardex;
    EntityManager em;
    CompraService servicio;
    ValidatorFactory factory;
    Producto producto;
    Tienda tienda;
    Usuario usuario;
    Stock stock;

    @BeforeEach void preparar() {
        compras = mock(CompraRepository.class); proveedores = mock(ProveedorRepository.class);
        tiendas = mock(TiendaRepository.class); usuarios = mock(UsuarioRepository.class);
        stocks = mock(StockRepository.class); kardex = mock(KardexService.class); em = mock(EntityManager.class);
        factory = Validation.buildDefaultValidatorFactory();
        servicio = new CompraService(compras, proveedores, tiendas, usuarios, stocks, kardex, em, factory.getValidator());
        Proveedor proveedor = new Proveedor(); proveedor.setIdProveedor(1);
        tienda = new Tienda(); tienda.setIdTienda(1);
        usuario = new Usuario(); usuario.setIdUsuario(1);
        producto = new Producto(); producto.setIdProducto(2);
        stock = new Stock(); stock.setCantidad(7); stock.setProducto(producto); stock.setTienda(tienda);
        when(proveedores.findById(1)).thenReturn(Optional.of(proveedor));
        when(tiendas.findById(1)).thenReturn(Optional.of(tienda));
        when(usuarios.findById(1)).thenReturn(Optional.of(usuario));
        when(em.find(Producto.class, 2, LockModeType.PESSIMISTIC_WRITE)).thenReturn(producto);
        when(stocks.findParaActualizar(2, 1)).thenReturn(Optional.of(stock));
        when(compras.saveAndFlush(any())).thenAnswer(i -> {
            Compra c = i.getArgument(0); c.setIdCompra(10); return c;
        });
    }
    @AfterEach void cerrar() { factory.close(); }
    CompraForm formulario() {
        CompraForm f = new CompraForm(); f.setIdProveedor(1); f.setIdTienda(1);
        var l = f.getLineas().get(0); l.setIdProducto(2); l.setCantidad(3); l.setCosto(new BigDecimal("2.35"));
        return f;
    }
    @Test void calculaTotalYAumentaStockYRegistraKardex() {
        Compra c = servicio.registrar(formulario(), 1);
        assertEquals(new BigDecimal("7.05"), c.getTotal());
        assertEquals(10, stock.getCantidad());
        assertSame(c, c.getDetalles().get(0).getCompra());
        verify(kardex).registrar(producto, tienda, "COMPRA", 7, 3, "Compra #10", usuario);
    }
    @Test void creaStockCuandoNoExiste() {
        when(stocks.findParaActualizar(2, 1)).thenReturn(Optional.empty());
        servicio.registrar(formulario(), 1);
        verify(stocks).save(argThat(s -> s.getCantidad() == 3 && s.getTienda() == tienda));
        verify(kardex).registrar(producto, tienda, "COMPRA", 0, 3, "Compra #10", usuario);
    }
    @Test void rechazaCantidadesInvalidasAntesDeGuardar() {
        CompraForm f = formulario(); f.getLineas().get(0).setCantidad(0);
        assertThrows(IllegalArgumentException.class, () -> servicio.registrar(f, 1));
        verifyNoInteractions(stocks, compras, kardex);
    }
    @Test void rechazaProductosDuplicados() {
        CompraForm f = formulario(); f.getLineas().add(f.getLineas().get(0));
        assertThrows(IllegalArgumentException.class, () -> servicio.registrar(f, 1));
        verifyNoInteractions(stocks, compras, kardex);
    }
    @Test void rechazaImporteFueraDePrecision() {
        CompraForm f = formulario(); f.getLineas().get(0).setCosto(new BigDecimal("99999999.99"));
        assertThrows(IllegalArgumentException.class, () -> servicio.registrar(f, 1));
        verifyNoInteractions(stocks, compras, kardex);
    }
    @Test void rechazaUsuarioDeOtraTienda() {
        Tienda otra = new Tienda(); otra.setIdTienda(3); usuario.setTienda(otra);
        assertThrows(IllegalArgumentException.class, () -> servicio.registrar(formulario(), 1));
        verifyNoInteractions(stocks, compras, kardex);
    }
    @Test void propagaFalloKardexParaQueLaTransaccionSeRevierta() {
        doThrow(new IllegalStateException("fallo simulado")).when(kardex)
                .registrar(any(), any(), anyString(), anyInt(), anyInt(), anyString(), any());
        assertThrows(IllegalStateException.class, () -> servicio.registrar(formulario(), 1));
    }
}
