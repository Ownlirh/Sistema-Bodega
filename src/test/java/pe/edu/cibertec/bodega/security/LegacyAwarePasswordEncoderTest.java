package pe.edu.cibertec.bodega.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LegacyAwarePasswordEncoderTest {
    private final LegacyAwarePasswordEncoder encoder = new LegacyAwarePasswordEncoder();

    @Test
    void newPasswordsAreStoredAsBcrypt() {
        String hash = encoder.encode("ClaveSegura-123");
        assertTrue(hash.startsWith("$2"));
        assertTrue(encoder.matches("ClaveSegura-123", hash));
        assertFalse(encoder.matches("otra-clave", hash));
    }

    @Test
    void legacyPasswordCanBeValidatedAndIsMarkedForUpgrade() {
        assertTrue(encoder.matches("demo123", "demo123"));
        assertTrue(encoder.upgradeEncoding("demo123"));
        assertFalse(encoder.upgradeEncoding(encoder.encode("demo123")));
    }

    @Test
    void malformedBcryptDoesNotBreakAuthentication() {
        assertFalse(encoder.matches("clave", "$2a$no-es-un-hash-valido"));
    }
}
