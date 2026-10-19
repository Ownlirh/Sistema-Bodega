package pe.edu.cibertec.bodega.e2e;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import pe.edu.cibertec.bodega.repository.AuditoriaRepository;
import pe.edu.cibertec.bodega.repository.UsuarioRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Seguridad de punta a punta, con la cadena de filtros real de SecurityConfig.
 *
 * Esto es lo que ningun @WebMvcTest puede probar: esos slices no cargan
 * SecurityConfig, asi que la matriz de roles del proyecto no la verificaba
 * nadie. Aca se comprueba ruta por ruta y rol por rol.
 */
class SeguridadE2ETest extends BaseE2E {

    @Autowired UsuarioRepository usuarios;
    @Autowired AuditoriaRepository auditorias;

    // ------------------------------------------------------------------
    // Inicio y cierre de sesion
    // ------------------------------------------------------------------

    @Test
    @DisplayName("sin iniciar sesion, cualquier ruta manda al login")
    void anonimoVaAlLogin() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("el login valido deja la sesion abierta")
    void loginCorrecto() throws Exception {
        mvc.perform(formLogin("/login").user(datos.getAdmin().getUsername()).password(DatosDePrueba.CLAVE))
                .andExpect(status().is3xxRedirection())
                .andExpect(authenticated().withUsername(datos.getAdmin().getUsername()));
    }

    @Test
    @DisplayName("una clave incorrecta no abre sesion y vuelve al login con error")
    void loginConClaveMala() throws Exception {
        mvc.perform(formLogin("/login").user(datos.getAdmin().getUsername()).password("no-es-la-clave"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    @DisplayName("un usuario deshabilitado no puede entrar")
    void usuarioDeshabilitado() throws Exception {
        var u = datos.getVendedorCentral();
        u.setEstado(false);
        usuarios.save(u);

        mvc.perform(formLogin("/login").user(u.getUsername()).password(DatosDePrueba.CLAVE))
                .andExpect(unauthenticated());
    }

    @Test
    @DisplayName("cerrar sesion la invalida de verdad")
    void logoutCierraLaSesion() throws Exception {
        MvcResult entrada = mvc.perform(
                        formLogin("/login").user(datos.getAdmin().getUsername()).password(DatosDePrueba.CLAVE))
                .andExpect(authenticated())
                .andReturn();
        MockHttpSession sesion = (MockHttpSession) entrada.getRequest().getSession(false);
        assertNotNull(sesion);

        // Se arma a mano en vez de usar logout(): hace falta reutilizar ESTA
        // sesion para comprobar despues que quedo invalidada.
        mvc.perform(post("/logout").session(sesion).with(csrf()))
                .andExpect(redirectedUrl("/login?logout"));

        // Con la MISMA sesion ya no se entra a ningun lado.
        mvc.perform(get("/").session(sesion))
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("la clave en texto plano se migra a BCrypt tras un login valido")
    void migraLaClaveHeredada() throws Exception {
        String antes = usuarios.findById(datos.getAdmin().getIdUsuario()).orElseThrow().getPassword();
        assertEquals(DatosDePrueba.CLAVE, antes, "la semilla la deja en texto plano a proposito");

        mvc.perform(formLogin("/login").user(datos.getAdmin().getUsername()).password(DatosDePrueba.CLAVE))
                .andExpect(authenticated());

        String despues = usuarios.findById(datos.getAdmin().getIdUsuario()).orElseThrow().getPassword();
        assertTrue(despues.startsWith("$2"), "deberia haber quedado hasheada con BCrypt: " + despues);
        assertNotEquals(antes, despues);
    }

    @Test
    @DisplayName("tras migrar a BCrypt la clave original sigue sirviendo")
    void laClaveSigueFuncionandoDespuesDeMigrar() throws Exception {
        mvc.perform(formLogin("/login").user(datos.getAdmin().getUsername()).password(DatosDePrueba.CLAVE))
                .andExpect(authenticated());
        mvc.perform(formLogin("/login").user(datos.getAdmin().getUsername()).password(DatosDePrueba.CLAVE))
                .andExpect(authenticated());
    }

    // ------------------------------------------------------------------
    // Matriz de roles
    // ------------------------------------------------------------------

    /**
     * Rutas reservadas al ADMINISTRADOR segun SecurityConfig. Se prueban con
     * los otros dos roles, que deben terminar en /403.
     */
    @ParameterizedTest(name = "{1} no entra a {0}")
    @CsvSource({
            "/panel,            MANAGER",
            "/panel,            VENDEDOR",
            "/reportes,         MANAGER",
            "/reportes,         VENDEDOR",
            "/transferencias,   MANAGER",
            "/transferencias,   VENDEDOR",
            "/health/db,        MANAGER",
            "/health/db,        VENDEDOR",
            "/compras,          VENDEDOR",
            "/gastos,           VENDEDOR",
            "/ajustes,          VENDEDOR",
            "/vencimientos,     VENDEDOR",
            "/mermas,           VENDEDOR"
    })
    void rutasProhibidas(String ruta, String rol) throws Exception {
        mvc.perform(get(ruta).with(user(principalDe(rol))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/403"));
    }

    @ParameterizedTest(name = "{1} si entra a {0}")
    @CsvSource({
            "/panel,                   ADMINISTRADOR",
            "/reportes,                ADMINISTRADOR",
            "/transferencias,          ADMINISTRADOR",
            "/compras,                 ADMINISTRADOR",
            "/compras,                 MANAGER",
            "/gastos,                  MANAGER",
            "/ajustes,                 MANAGER",
            "/kardex,                  VENDEDOR",
            "/alertas,                 VENDEDOR",
            "/inventario/stock-bajo,   VENDEDOR",
            "/clientes,                VENDEDOR"
    })
    void rutasPermitidas(String ruta, String rol) throws Exception {
        mvc.perform(get(ruta).with(user(principalDe(rol))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("el login y el 403 son publicos")
    void rutasPublicas() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk());
        mvc.perform(get("/403")).andExpect(status().isOk());
    }

    // ------------------------------------------------------------------
    // Bitacora
    // ------------------------------------------------------------------

    @Test
    @DisplayName("el inicio de sesion queda en la bitacora")
    void auditaElLogin() throws Exception {
        assertEquals(0, auditorias.count());

        mvc.perform(formLogin("/login").user(datos.getAdmin().getUsername()).password(DatosDePrueba.CLAVE))
                .andExpect(authenticated());

        var filas = auditorias.findAll();
        assertEquals(1, filas.size());
        assertEquals("INICIO_SESION", filas.get(0).getAccion());
        assertEquals("OK", filas.get(0).getResultado());
        assertEquals(datos.getAdmin().getUsername(), filas.get(0).getUsername());
    }

    @Test
    @DisplayName("el intento fallido tambien queda registrado")
    void auditaElIntentoFallido() throws Exception {
        mvc.perform(formLogin("/login").user(datos.getAdmin().getUsername()).password("no-es-la-clave"))
                .andExpect(unauthenticated());

        var filas = auditorias.findAll();
        assertEquals(1, filas.size());
        assertEquals("INTENTO_SESION", filas.get(0).getAccion());
        assertEquals("FALLIDO", filas.get(0).getResultado());
    }

    @Test
    @DisplayName("el acceso denegado queda registrado")
    void auditaElAccesoDenegado() throws Exception {
        mvc.perform(get("/panel").with(user(principalDe("VENDEDOR"))))
                .andExpect(redirectedUrl("/403"));

        var filas = auditorias.findAll();
        assertEquals(1, filas.size());
        assertEquals("ACCESO_DENEGADO", filas.get(0).getAccion());
        assertEquals("FALLIDO", filas.get(0).getResultado());
        assertTrue(filas.get(0).getDetalle().contains("/panel"));
    }

    private pe.edu.cibertec.bodega.security.AppUserDetails principalDe(String rol) {
        var usuario = switch (rol) {
            case "ADMINISTRADOR" -> datos.getAdmin();
            case "MANAGER" -> datos.getManagerCentral();
            case "VENDEDOR" -> datos.getVendedorCentral();
            default -> throw new IllegalArgumentException("rol desconocido: " + rol);
        };
        return new pe.edu.cibertec.bodega.security.AppUserDetails(usuario);
    }
}
