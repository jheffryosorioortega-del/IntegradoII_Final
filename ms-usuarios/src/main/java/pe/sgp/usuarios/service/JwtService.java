package pe.sgp.usuarios.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pe.sgp.usuarios.model.Usuario;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/** Emite el JWT (firmado con HS256) que prueba quién es el usuario y qué rol tiene.
 *  Solo ms-usuarios FIRMA; el Gateway lo VERIFICA con el mismo secreto (SGP_JWT_SECRET).
 *  El Front-End solo lo guarda en la sesión del servidor y lo reenvía: no tiene el secreto,
 *  así que no puede fabricar ni modificar un token. */
@Service
public class JwtService {

    public static final String EMISOR = "sgp-usuarios";

    private final SecretKey clave;
    private final Duration vigencia;

    public JwtService(@Value("${SGP_JWT_SECRET:}") String secreto,
                      @Value("${sgp.jwt.vigencia-horas:8}") long vigenciaHoras) {
        byte[] bytes = secreto.trim().getBytes(StandardCharsets.UTF_8);
        // Falla al arrancar (y no en el primer login) para que el error de configuración sea evidente.
        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "SGP_JWT_SECRET no está definido o es muy corto (mínimo 32 caracteres). Agrégalo al archivo .env.");
        }
        this.clave = Keys.hmacShaKeyFor(bytes);
        this.vigencia = Duration.ofHours(vigenciaHoras);
    }

    public String emitir(Usuario u) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .issuer(EMISOR)
                .subject(u.getCorreo())
                .claim("rol", u.getRol().name())
                .claim("nombre", u.getNombre())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(vigencia)))
                .signWith(clave, Jwts.SIG.HS256)
                .compact();
    }
}
