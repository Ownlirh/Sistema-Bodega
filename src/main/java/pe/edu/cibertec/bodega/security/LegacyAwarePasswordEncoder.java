package pe.edu.cibertec.bodega.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Compatibilidad de migración: acepta temporalmente claves heredadas en texto
 * plano y las convierte a BCrypt después de un inicio de sesión correcto.
 * Las contraseñas nuevas siempre se guardan con BCrypt.
 */
public class LegacyAwarePasswordEncoder implements PasswordEncoder {
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    @Override public String encode(CharSequence rawPassword) {
        return bcrypt.encode(rawPassword);
    }

    @Override public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) return false;
        if (esBcrypt(encodedPassword)) {
            try { return bcrypt.matches(rawPassword, encodedPassword); }
            catch (IllegalArgumentException ex) { return false; }
        }
        // Solo se usa durante la transición de los usuarios demo del SQL inicial.
        return rawPassword.toString().equals(encodedPassword);
    }

    @Override public boolean upgradeEncoding(String encodedPassword) {
        return !esBcrypt(encodedPassword);
    }

    private boolean esBcrypt(String value) {
        return value != null && (value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$"));
    }
}
