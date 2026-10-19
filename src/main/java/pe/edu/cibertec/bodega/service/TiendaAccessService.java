package pe.edu.cibertec.bodega.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import pe.edu.cibertec.bodega.model.Tienda;
import pe.edu.cibertec.bodega.repository.TiendaRepository;
import pe.edu.cibertec.bodega.security.AppUserDetails;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TiendaAccessService {
    private final TiendaRepository tiendaRepository;

    /** Tiendas visibles para la sesión. El administrador puede ver todas. */
    public List<Tienda> tiendasVisibles(AppUserDetails usuario) {
        if (usuario.esAdministrador()) return tiendaRepository.findByEstadoTrue();
        if (usuario.getIdTienda() == null) throw new AccessDeniedException("El usuario no tiene una tienda asignada.");
        return tiendaRepository.findById(usuario.getIdTienda()).filter(t -> Boolean.TRUE.equals(t.getEstado()))
                .map(List::of).orElseThrow(() -> new AccessDeniedException("La tienda asignada no está disponible."));
    }

    /** Rechaza identificadores de tienda manipulados en la URL o en el formulario. */
    public Tienda exigirTiendaPermitida(Integer idTienda, AppUserDetails usuario) {
        if (idTienda == null) throw new IllegalArgumentException("Debe seleccionar una tienda.");
        if (!usuario.esAdministrador() && !idTienda.equals(usuario.getIdTienda())) {
            throw new AccessDeniedException("No tiene permiso para acceder a datos de otra tienda.");
        }
        return tiendaRepository.findById(idTienda)
                .filter(t -> Boolean.TRUE.equals(t.getEstado()))
                .orElseThrow(() -> new IllegalArgumentException("La tienda no existe o está inactiva."));
    }

    public void verificarAcceso(Integer idTienda, AppUserDetails usuario) {
        if (!usuario.esAdministrador() && (usuario.getIdTienda() == null || !usuario.getIdTienda().equals(idTienda))) {
            throw new AccessDeniedException("No tiene permiso para acceder a datos de otra tienda.");
        }
    }
}
