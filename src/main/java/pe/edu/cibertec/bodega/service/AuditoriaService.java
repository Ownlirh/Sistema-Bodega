package pe.edu.cibertec.bodega.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.cibertec.bodega.model.Auditoria;
import pe.edu.cibertec.bodega.model.Usuario;
import pe.edu.cibertec.bodega.repository.AuditoriaRepository;
import pe.edu.cibertec.bodega.repository.UsuarioRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditoriaService {
    private final AuditoriaRepository auditoriaRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(String username, String accion, String detalle, String resultado, HttpServletRequest request) {
        try {
            Usuario usuario = usuarioRepository.findByUsername(username).orElse(null);
            Auditoria evento = new Auditoria();
            evento.setFecha(LocalDateTime.now());
            evento.setUsername(limitar(username == null ? "desconocido" : username, 45));
            evento.setAccion(limitar(accion, 80));
            evento.setDetalle(limitar(detalle, 500));
            evento.setResultado(limitar(resultado, 15));
            evento.setDireccionIp(limitar(obtenerIp(request), 45));
            if (usuario != null) {
                evento.setIdUsuario(usuario.getIdUsuario());
                evento.setIdTienda(usuario.getTienda() == null ? null : usuario.getTienda().getIdTienda());
            }
            auditoriaRepository.save(evento);
        } catch (RuntimeException ex) {
            // La auditoría no debe convertir una operación funcional en un error de usuario;
            // el fallo queda registrado en el log del servidor para su revisión.
            log.error("No se pudo guardar evento de auditoría: {} - {}", accion, ex.getMessage());
        }
    }

    private String obtenerIp(HttpServletRequest request) {
        if (request == null) return "desconocida";
        // No confiar ciegamente en X-Forwarded-For salvo que exista proxy de confianza configurado.
        return request.getRemoteAddr();
    }

    private String limitar(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
