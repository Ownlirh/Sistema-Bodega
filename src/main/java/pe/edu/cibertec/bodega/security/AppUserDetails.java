package pe.edu.cibertec.bodega.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import pe.edu.cibertec.bodega.model.Usuario;

import java.util.Collection;
import java.util.List;

/** Principal de seguridad con los datos mínimos que necesita la aplicación. */
public class AppUserDetails implements UserDetails {
    private final Usuario usuario;

    public AppUserDetails(Usuario usuario) {
        this.usuario = usuario;
    }

    public Integer getIdUsuario() { return usuario.getIdUsuario(); }
    public Integer getIdTienda() { return usuario.getTienda() == null ? null : usuario.getTienda().getIdTienda(); }
    public String getNombreCompleto() { return usuario.getNombreCompleto(); }
    public String getNombreRol() { return usuario.getRol().getNombre(); }
    public boolean esAdministrador() { return "ADMINISTRADOR".equalsIgnoreCase(getNombreRol()); }
    public Usuario getUsuario() { return usuario; }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().getNombre().toUpperCase()));
    }
    @Override public String getPassword() { return usuario.getPassword(); }
    @Override public String getUsername() { return usuario.getUsername(); }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return Boolean.TRUE.equals(usuario.getEstado()); }
}
