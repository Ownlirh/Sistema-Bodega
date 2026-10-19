package pe.edu.cibertec.bodega.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import pe.edu.cibertec.bodega.service.AuditoriaService;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final UsuarioDetailsService usuarioDetailsService;
    private final AuditoriaService auditoriaService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new LegacyAwarePasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(usuarioDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        // Migra contraseñas heredadas a BCrypt después de un login válido.
        provider.setUserDetailsPasswordService(usuarioDetailsService);
        return provider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationProvider provider) throws Exception {
        http.authenticationProvider(provider)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/error", "/403", "/css/**", "/js/**", "/images/**").permitAll()
                // Herramientas administrativas y diagnóstico de base de datos.
                .requestMatchers("/seguridad/auditoria/**", "/admin/**", "/empleados/**", "/health/**")
                    .hasRole("ADMINISTRADOR")
                // Las transferencias afectan a dos tiendas y requieren autorización global.
                .requestMatchers("/transferencias/**").hasRole("ADMINISTRADOR")
                .requestMatchers("/ajustes/**", "/compras/**", "/gastos/**", "/vencimientos/**", "/mermas/**")
                    .hasAnyRole("ADMINISTRADOR", "MANAGER")
                // Los reportes actuales consultan varias tiendas; se reservan al administrador
                // hasta que cada consulta incorpore un filtro obligatorio por tienda.
                .requestMatchers("/reportes/**", "/panel/**").hasRole("ADMINISTRADOR")
                .requestMatchers("/caja/**")
                    .hasAnyRole("ADMINISTRADOR", "MANAGER", "VENDEDOR")
                .requestMatchers(HttpMethod.POST, "/productos/**")
                    .hasAnyRole("ADMINISTRADOR", "MANAGER")
                .requestMatchers(HttpMethod.PUT, "/productos/**")
                    .hasAnyRole("ADMINISTRADOR", "MANAGER")
                .requestMatchers(HttpMethod.PATCH, "/productos/**")
                    .hasAnyRole("ADMINISTRADOR", "MANAGER")
                .requestMatchers(HttpMethod.DELETE, "/productos/**")
                    .hasAnyRole("ADMINISTRADOR", "MANAGER")
                .requestMatchers("/productos/**", "/consultas/**", "/venta/**", "/ventas/**",
                                 "/inventario/**", "/kardex/**", "/alertas/**", "/demo/**")
                    .hasAnyRole("ADMINISTRADOR", "MANAGER", "VENDEDOR")
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler((request, response, authentication) -> {
                    auditoriaService.registrar(authentication.getName(), "INICIO_SESION",
                            "Inicio de sesión correcto", "OK", request);
                    response.sendRedirect(request.getContextPath() + "/");
                })
                .failureHandler((request, response, exception) -> {
                    String username = request.getParameter("username");
                    auditoriaService.registrar(username == null ? "desconocido" : username,
                            "INTENTO_SESION", "Credenciales no válidas o usuario deshabilitado", "FALLIDO", request);
                    response.sendRedirect(request.getContextPath() + "/login?error");
                })
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/logout")
                .addLogoutHandler((request, response, authentication) -> {
                    if (authentication != null && authentication.isAuthenticated()) {
                        auditoriaService.registrar(authentication.getName(), "CIERRE_SESION",
                                "Cierre de sesión", "OK", request);
                    }
                })
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID"))
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionFixation(fixation -> fixation.migrateSession()))
            .exceptionHandling(ex -> ex.accessDeniedHandler((request, response, denied) -> {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                String username = authentication == null ? "desconocido" : authentication.getName();
                auditoriaService.registrar(username, "ACCESO_DENEGADO",
                        "Ruta protegida: " + request.getRequestURI(), "FALLIDO", request);
                response.sendRedirect(request.getContextPath() + "/403");
            }));
        // CSRF sigue habilitado; los formularios y el logout deben incluir su token.
        return http.build();
    }
}
