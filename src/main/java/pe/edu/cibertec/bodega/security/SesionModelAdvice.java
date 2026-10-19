package pe.edu.cibertec.bodega.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.ControllerAdvice;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class SesionModelAdvice {
    @ModelAttribute
    public void agregarDatosSesion(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof AppUserDetails usuario) {
            model.addAttribute("usuarioSesion", usuario.getNombreCompleto());
            model.addAttribute("rolSesion", usuario.getNombreRol());
            model.addAttribute("tiendaSesion", usuario.getIdTienda());
        } else {
            model.addAttribute("usuarioSesion", "Sin sesión");
            model.addAttribute("rolSesion", "");
            model.addAttribute("tiendaSesion", null);
        }
    }
}
