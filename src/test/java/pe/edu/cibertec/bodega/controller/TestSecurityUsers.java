package pe.edu.cibertec.bodega.controller;

import pe.edu.cibertec.bodega.model.Rol;
import pe.edu.cibertec.bodega.model.Usuario;
import pe.edu.cibertec.bodega.security.AppUserDetails;

/** Principal admin reutilizable para pruebas MVC que atraviesan Spring Security. */
final class TestSecurityUsers {
    private TestSecurityUsers() {}

    static AppUserDetails admin() {
        Rol rol = new Rol();
        rol.setIdRol(1);
        rol.setNombre("ADMINISTRADOR");
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(1);
        usuario.setUsername("admin-prueba");
        usuario.setPassword("no-se-usa-en-la-prueba");
        usuario.setNombres("Admin");
        usuario.setApellidos("Prueba");
        usuario.setRol(rol);
        usuario.setEstado(true);
        usuario.setTienda(null);
        return new AppUserDetails(usuario);
    }
}
