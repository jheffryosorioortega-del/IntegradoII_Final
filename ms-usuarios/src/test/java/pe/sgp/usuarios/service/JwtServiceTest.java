package pe.sgp.usuarios.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import pe.sgp.usuarios.model.Rol;
import pe.sgp.usuarios.model.Usuario;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    private static final String SECRETO = "secreto-de-prueba-con-mas-de-32-caracteres!";

    private Usuario usuario() {
        Usuario u = new Usuario();
        u.setNombre("Coordinador SGP");
        u.setCorreo("coordinador@sgp.local");
        u.setRol(Rol.COORDINADOR);
        return u;
    }

    @Test void emiteTokenFirmadoConCorreoRolYEmisor() {
        String token = new JwtService(SECRETO, 8).emitir(usuario());
        var datos = Jwts.parser().verifyWith(Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8)))
                .requireIssuer("sgp-usuarios").build().parseSignedClaims(token).getPayload();
        assertEquals("coordinador@sgp.local", datos.getSubject());
        assertEquals("COORDINADOR", datos.get("rol", String.class));
        assertTrue(datos.getExpiration().after(datos.getIssuedAt()));
    }

    @Test void elTokenNoLlevaContrasenaNiHash() {
        String token = new JwtService(SECRETO, 8).emitir(usuario());
        String cuerpo = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);
        assertFalse(cuerpo.toLowerCase().contains("contrasena"));
        assertFalse(cuerpo.contains("$2a$"));
    }

    @Test void noArrancaSinSecretoOConSecretoCorto() {
        assertThrows(IllegalStateException.class, () -> new JwtService("", 8));
        assertThrows(IllegalStateException.class, () -> new JwtService("corto", 8));
    }
}
