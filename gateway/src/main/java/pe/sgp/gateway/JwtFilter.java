package pe.sgp.gateway;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

/** Segunda puerta del Gateway (la primera es la clave compartida, ApiKeyFilter): comprueba QUIÉN llama y
 *  si su ROL alcanza para lo que pide. El usuario se identifica con el JWT que firmó ms-usuarios al hacer login
 *  (cabecera Authorization: Bearer ...). Aquí solo se verifica la firma con SGP_JWT_SECRET; no se firma nada.
 *
 *  La tabla {@link #REGLAS} es la política de acceso de toda la API. Lo que no aparece en ella exige
 *  rol ADMINISTRADOR (se niega por defecto): una ruta nueva nunca queda abierta por olvido.
 *  Códigos: 401 = sin token, token inválido o vencido; 403 = token válido pero el rol no alcanza. */
@Component
public class JwtFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);

    static final String EMISOR = "sgp-usuarios";
    static final String HEADER_USUARIO = "X-SGP-Usuario";
    static final String HEADER_ROL = "X-SGP-Rol";

    private static final Set<String> TODOS = Set.of("ADMINISTRADOR", "COORDINADOR", "TECNICO");
    private static final Set<String> GESTION = Set.of("ADMINISTRADOR", "COORDINADOR");
    private static final Set<String> SOLO_ADMIN = Set.of("ADMINISTRADOR");

    /** @param metodo null = cualquier método; @param roles null = ruta pública (solo exige la clave compartida). */
    private record Regla(HttpMethod metodo, String patron, Set<String> roles) { }

    private static final List<Regla> REGLAS = List.of(
            // Públicas: las usa el portal sin que haya nadie con sesión. (DNI y RUC tienen además su propia clave.)
            new Regla(HttpMethod.POST, "/api/usuarios/login", null),
            new Regla(HttpMethod.POST, "/api/solicitudes", null),
            new Regla(HttpMethod.POST, "/api/solicitudes/*/fotos", null),   // el portal sube las fotos tras registrar
            new Regla(HttpMethod.POST, "/api/clientes/dni", null),
            new Regla(HttpMethod.POST, "/api/clientes/ruc", null),
            // Panel: cualquier usuario con sesión puede ver solicitudes, su cola, las alertas y sus fotos...
            new Regla(HttpMethod.GET, "/api/solicitudes", TODOS),
            new Regla(HttpMethod.GET, "/api/solicitudes/recientes", TODOS),
            new Regla(HttpMethod.GET, "/api/solicitudes/resumen", TODOS),
            new Regla(HttpMethod.GET, "/api/solicitudes/cola", TODOS),
            new Regla(HttpMethod.GET, "/api/solicitudes/pulso", TODOS),
            new Regla(HttpMethod.GET, "/api/solicitudes/*", TODOS),
            new Regla(HttpMethod.GET, "/api/solicitudes/*/historial", TODOS),
            new Regla(HttpMethod.GET, "/api/solicitudes/*/fotos", TODOS),
            new Regla(HttpMethod.GET, "/api/solicitudes/*/fotos/*", TODOS),
            // ...pero solo Administrador y Coordinador cambian estados, asignan técnicos y gestionan clientes
            // (igual que en el Front-End: /panel/clientes/** y POST /panel/solicitudes/**).
            new Regla(HttpMethod.PUT, "/api/solicitudes/*/estado", GESTION),
            new Regla(HttpMethod.PUT, "/api/solicitudes/*/tecnico", GESTION),
            new Regla(HttpMethod.GET, "/api/usuarios/tecnicos", GESTION),
            new Regla(null, "/api/clientes", GESTION),
            new Regla(HttpMethod.GET, "/api/clientes/buscar", GESTION));

    private final AntPathMatcher comparador = new AntPathMatcher();
    private final SecretKey clave;

    public JwtFilter(@Value("${SGP_JWT_SECRET:}") String secreto) {
        byte[] bytes = secreto.trim().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            this.clave = null;
            log.warn("SGP_JWT_SECRET no está definido (o es menor a 32 caracteres) en .env: "
                    + "el Gateway rechazará toda ruta que exija sesión.");
        } else {
            this.clave = Keys.hmacShaKeyFor(bytes);
        }
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        var peticion = exchange.getRequest();
        // Estas cabeceras las pone SOLO el Gateway, nunca se aceptan de afuera (evita suplantar un usuario/rol).
        var limpia = peticion.mutate().headers(h -> {
            h.remove(HEADER_USUARIO);
            h.remove(HEADER_ROL);
        });

        Set<String> roles = rolesExigidos(peticion.getMethod(), peticion.getURI().getRawPath());
        if (roles == null) {                       // ruta pública
            return chain.filter(exchange.mutate().request(limpia.build()).build());
        }

        Claims datos = leerToken(peticion.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        if (datos == null) {
            return rechazar(exchange, HttpStatus.UNAUTHORIZED, "Sesión no válida o vencida. Inicia sesión de nuevo.");
        }
        String rol = datos.get("rol", String.class);
        if (rol == null || !roles.contains(rol)) {
            return rechazar(exchange, HttpStatus.FORBIDDEN, "No tienes permiso para esta acción.");
        }
        limpia.header(HEADER_USUARIO, datos.getSubject()).header(HEADER_ROL, rol);
        return chain.filter(exchange.mutate().request(limpia.build()).build());
    }

    /** Roles que pueden usar la ruta; null si es pública. */
    private Set<String> rolesExigidos(HttpMethod metodo, String ruta) {
        for (Regla r : REGLAS) {
            if ((r.metodo() == null || r.metodo().equals(metodo)) && comparador.match(r.patron(), ruta)) {
                return r.roles();
            }
        }
        return SOLO_ADMIN;
    }

    /** Devuelve los datos del token si la firma, el emisor y la fecha de vencimiento son correctos; si no, null. */
    private Claims leerToken(String autorizacion) {
        if (clave == null || autorizacion == null || !autorizacion.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        try {
            return Jwts.parser().verifyWith(clave).requireIssuer(EMISOR).build()
                    .parseSignedClaims(autorizacion.substring(7).trim()).getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return null;   // firma falsa, token alterado, vencido o mal formado
        }
    }

    private Mono<Void> rechazar(ServerWebExchange exchange, HttpStatus estado, String mensaje) {
        var respuesta = exchange.getResponse();
        respuesta.setStatusCode(estado);
        respuesta.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        if (estado == HttpStatus.UNAUTHORIZED) {
            respuesta.getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        }
        byte[] cuerpo = ("{\"mensaje\":\"" + mensaje + "\"}").getBytes(StandardCharsets.UTF_8);
        return respuesta.writeWith(Mono.just(respuesta.bufferFactory().wrap(cuerpo)));
    }

    /** Justo después de la clave compartida (ApiKeyFilter va primero). */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
