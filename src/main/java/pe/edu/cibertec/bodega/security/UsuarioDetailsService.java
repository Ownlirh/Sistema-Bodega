package pe.edu.cibertec.bodega.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.cibertec.bodega.model.Usuario;
import pe.edu.cibertec.bodega.repository.UsuarioRepository;

@Service
@RequiredArgsConstructor
public class UsuarioDetailsService implements UserDetailsService, UserDetailsPasswordService {
    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        return new AppUserDetails(usuario);
    }

    /** Spring Security invoca este método para migrar la clave heredada a BCrypt tras validar el login. */
    @Override
    @Transactional
    public UserDetails updatePassword(UserDetails user, String newPassword) {
        Usuario usuario = usuarioRepository.findByUsername(user.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        usuario.setPassword(newPassword);
        return new AppUserDetails(usuarioRepository.save(usuario));
    }
}
