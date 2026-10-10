package pe.edu.cibertec.bodega.service;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.*;
import pe.edu.cibertec.bodega.dto.GastoForm;
import pe.edu.cibertec.bodega.model.*;
import pe.edu.cibertec.bodega.repository.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GastoServiceTest {
    GastoRepository gastos;
    CategoriaGastoRepository categorias;
    TiendaRepository tiendas;
    UsuarioRepository usuarios;
    ValidatorFactory factory;
    GastoService servicio;
    Tienda tienda;
    Usuario usuario;
    @BeforeEach void preparar() {
        gastos = mock(GastoRepository.class); categorias = mock(CategoriaGastoRepository.class);
        tiendas = mock(TiendaRepository.class); usuarios = mock(UsuarioRepository.class);
        factory = Validation.buildDefaultValidatorFactory();
        servicio = new GastoService(gastos, categorias, tiendas, usuarios, factory.getValidator());
        tienda = new Tienda(); tienda.setIdTienda(1);
        usuario = new Usuario(); usuario.setIdUsuario(1);
        CategoriaGasto categoria = new CategoriaGasto(); categoria.setIdCategoriaGasto(2);
        when(tiendas.findById(1)).thenReturn(Optional.of(tienda));
        when(categorias.findById(2)).thenReturn(Optional.of(categoria));
        when(usuarios.findById(1)).thenReturn(Optional.of(usuario));
        when(gastos.save(any())).thenAnswer(i -> i.getArgument(0));
    }
    @AfterEach void cerrar() { factory.close(); }
    GastoForm formulario() {
        GastoForm f = new GastoForm(); f.setIdTienda(1); f.setIdCategoriaGasto(2);
        f.setMonto(new BigDecimal("25.5")); f.setDescripcion("  Recibo de luz  "); return f;
    }
    @Test void guardaDatosYNormalizaImporte() {
        Gasto g = servicio.registrar(formulario(), 1);
        assertEquals(new BigDecimal("25.50"), g.getMonto());
        assertEquals("Recibo de luz", g.getDescripcion());
        assertSame(tienda, g.getTienda()); assertSame(usuario, g.getUsuario());
        assertEquals(2, g.getCategoriaGasto().getIdCategoriaGasto());
        verify(gastos).save(g);
    }
    @Test void rechazaImportesInvalidos() {
        for (String monto : new String[]{"0", "-1", "1.001", "100000000"}) {
            GastoForm f = formulario(); f.setMonto(new BigDecimal(monto));
            assertThrows(IllegalArgumentException.class, () -> servicio.registrar(f, 1));
        }
        verifyNoInteractions(gastos);
    }
    @Test void rechazaFechaFutura() {
        GastoForm f = formulario(); f.setFecha(LocalDate.now(ZoneId.of("America/Lima")).plusDays(1));
        assertThrows(IllegalArgumentException.class, () -> servicio.registrar(f, 1)); verifyNoInteractions(gastos);
    }
    @Test void rechazaCategoriaInexistente() {
        when(categorias.findById(2)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> servicio.registrar(formulario(), 1)); verifyNoInteractions(gastos);
    }
    @Test void rechazaTiendaInactiva() {
        tienda.setEstado(false);
        assertThrows(IllegalArgumentException.class, () -> servicio.registrar(formulario(), 1)); verifyNoInteractions(gastos);
    }
    @Test void rechazaUsuarioDeOtraTienda() {
        Tienda otra = new Tienda(); otra.setIdTienda(3); usuario.setTienda(otra);
        assertThrows(IllegalArgumentException.class, () -> servicio.registrar(formulario(), 1)); verifyNoInteractions(gastos);
    }
    @Test void admiteDescripcionOpcional() {
        GastoForm f = formulario(); f.setDescripcion(null);
        assertNull(servicio.registrar(f, 1).getDescripcion());
    }
    @Test void rechazaDescripcionLarga() {
        GastoForm f = formulario(); f.setDescripcion("a".repeat(121));
        assertThrows(IllegalArgumentException.class, () -> servicio.registrar(f, 1)); verifyNoInteractions(gastos);
    }
}
