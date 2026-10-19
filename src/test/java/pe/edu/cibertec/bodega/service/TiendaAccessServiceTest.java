package pe.edu.cibertec.bodega.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import pe.edu.cibertec.bodega.model.Rol;
import pe.edu.cibertec.bodega.model.Tienda;
import pe.edu.cibertec.bodega.model.Usuario;
import pe.edu.cibertec.bodega.repository.TiendaRepository;
import pe.edu.cibertec.bodega.security.AppUserDetails;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TiendaAccessServiceTest {
    @Mock TiendaRepository tiendaRepository;
    @InjectMocks TiendaAccessService servicio;

    @Test
    void empleadoSoloVeSuTiendaActiva() {
        Tienda tienda = tienda(7, true);
        when(tiendaRepository.findById(7)).thenReturn(Optional.of(tienda));

        List<Tienda> visibles = servicio.tiendasVisibles(usuario("MANAGER", 7));

        assertEquals(1, visibles.size());
        assertEquals(7, visibles.get(0).getIdTienda());
    }

    @Test
    void empleadoNoPuedeElegirOtraTiendaManipulandoElId() {
        AppUserDetails principal = usuario("VENDEDOR", 7);

        assertThrows(AccessDeniedException.class, () -> servicio.exigirTiendaPermitida(8, principal));
        verifyNoInteractions(tiendaRepository);
    }

    @Test
    void administradorPuedeVerTodasLasTiendasActivas() {
        when(tiendaRepository.findByEstadoTrue()).thenReturn(List.of(tienda(1, true), tienda(2, true)));

        assertEquals(2, servicio.tiendasVisibles(usuario("ADMINISTRADOR", null)).size());
    }

    private AppUserDetails usuario(String nombreRol, Integer idTienda) {
        Rol rol = new Rol();
        rol.setIdRol(1);
        rol.setNombre(nombreRol);
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(1);
        usuario.setUsername("prueba");
        usuario.setPassword("no-se-usa");
        usuario.setNombres("Usuario");
        usuario.setApellidos("Prueba");
        usuario.setRol(rol);
        usuario.setTienda(idTienda == null ? null : tienda(idTienda, true));
        usuario.setEstado(true);
        return new AppUserDetails(usuario);
    }

    private Tienda tienda(Integer id, boolean activa) {
        Tienda tienda = new Tienda();
        tienda.setIdTienda(id);
        tienda.setNombre("Tienda " + id);
        tienda.setEstado(activa);
        return tienda;
    }
}
