package pe.edu.cibertec.bodega.controller;

import pe.edu.cibertec.bodega.model.Rol;
import pe.edu.cibertec.bodega.model.Usuario;
import pe.edu.cibertec.bodega.security.AppUserDetails;

/**
 * Principal admin reutilizable para pruebas MVC que atraviesan Spring Security.
 *
 * OJO con el alcance: @WebMvcTest NO carga la SecurityConfig del proyecto, porque
 * esa clase no es un controlador. Spring Boot autoconfigura la cadena de filtros
 * POR DEFECTO, que solo exige estar autenticado. Es decir: estas pruebas verifican
 * que el controlador responde bien, pero NO validan la matriz de roles (quien puede
 * entrar a /compras, /reportes, etc.). Esa matriz hoy no la prueba nadie; hace falta
 * un @SpringBootTest para cubrirla.
 */
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
