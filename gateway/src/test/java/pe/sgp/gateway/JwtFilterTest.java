package pe.sgp.gateway;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class JwtFilterTest {
    private static final String SECRETO = "secreto-de-prueba-con-mas-de-32-caracteres!";
    private final JwtFilter filtro = new JwtFilter(SECRETO);

    private String token(String rol, String secreto, long vencimientoMs) {
        return Jwts.builder().issuer("sgp-usuarios").subject("ana@sgp.local").claim("rol", rol)
                .expiration(new Date(System.currentTimeMillis() + vencimientoMs))
                .signWith(Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256).compact();
    }

    private static String b64(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    /** Ejecuta el filtro y devuelve el código HTTP (200 si dejó pasar la petición hacia el microservicio). */
    private int llamar(HttpMethod metodo, String ruta, String authorization, AtomicReference<ServerWebExchange> pasoPor) {
        var req = MockServerHttpRequest.method(metodo, ruta);
        if (authorization != null) req.header("Authorization", authorization);
        req.header("X-SGP-Rol", "ADMINISTRADOR").header("X-SGP-Usuario", "intruso@x.com");   // intento de suplantar
        var exchange = MockServerWebExchange.from(req);
        GatewayFilterChain cadena = ex -> { pasoPor.set(ex); return Mono.empty(); };
        filtro.filter(exchange, cadena).block();
        return pasoPor.get() != null ? 200 : exchange.getResponse().getStatusCode().value();
    }

    private int llamar(HttpMethod metodo, String ruta, String authorization) {
        return llamar(metodo, ruta, authorization, new AtomicReference<>());
    }

    @Test void rutasPublicasPasanSinToken() {
        assertEquals(200, llamar(HttpMethod.POST, "/api/usuarios/login", null));
        assertEquals(200, llamar(HttpMethod.POST, "/api/solicitudes", null));
        assertEquals(200, llamar(HttpMethod.POST, "/api/clientes/dni", null));
        assertEquals(200, llamar(HttpMethod.POST, "/api/clientes/ruc", null));
    }

    @Test void rutasProtegidasSinTokenDan401() {
        assertEquals(401, llamar(HttpMethod.GET, "/api/clientes", null));
        assertEquals(401, llamar(HttpMethod.GET, "/api/solicitudes", null));
        assertEquals(401, llamar(HttpMethod.PUT, "/api/solicitudes/1/estado", null));
        assertEquals(401, llamar(HttpMethod.GET, "/api/solicitudes", "Basic abc"));
    }

    @Test void tokenFalsoVencidoOAlteradoDa401() {
        assertEquals(401, llamar(HttpMethod.GET, "/api/solicitudes", "Bearer basura"));
        assertEquals(401, llamar(HttpMethod.GET, "/api/solicitudes",
                "Bearer " + token("ADMINISTRADOR", "otro-secreto-distinto-de-mas-de-32-caracteres", 60_000)));
        assertEquals(401, llamar(HttpMethod.GET, "/api/solicitudes", "Bearer " + token("ADMINISTRADOR", SECRETO, -60_000)));

        // Un TECNICO que cambia su propio token a ADMINISTRADOR sin conocer el secreto: la firma ya no coincide.
        String[] partes = token("TECNICO", SECRETO, 60_000).split("\\.");
        String cuerpoAdmin = b64("{\"iss\":\"sgp-usuarios\",\"sub\":\"ana@sgp.local\",\"rol\":\"ADMINISTRADOR\"}");
        assertEquals(401, llamar(HttpMethod.GET, "/api/clientes", "Bearer " + partes[0] + "." + cuerpoAdmin + "." + partes[2]));

        // Ataque clásico "alg: none": token sin firma.
        String sinFirma = b64("{\"alg\":\"none\"}") + "." + cuerpoAdmin + ".";
        assertEquals(401, llamar(HttpMethod.GET, "/api/clientes", "Bearer " + sinFirma));
    }

    @Test void rolesComoEnElPanel() {
        String admin = "Bearer " + token("ADMINISTRADOR", SECRETO, 60_000);
        String coord = "Bearer " + token("COORDINADOR", SECRETO, 60_000);
        String tecnico = "Bearer " + token("TECNICO", SECRETO, 60_000);
        for (String t : new String[]{admin, coord, tecnico}) {
            assertEquals(200, llamar(HttpMethod.GET, "/api/solicitudes", t));
            assertEquals(200, llamar(HttpMethod.GET, "/api/solicitudes/resumen", t));
            assertEquals(200, llamar(HttpMethod.GET, "/api/solicitudes/recientes", t));
        }
        for (String t : new String[]{admin, coord}) {
            assertEquals(200, llamar(HttpMethod.PUT, "/api/solicitudes/7/estado", t));
            assertEquals(200, llamar(HttpMethod.GET, "/api/clientes", t));
            assertEquals(200, llamar(HttpMethod.POST, "/api/clientes", t));
        }
        assertEquals(403, llamar(HttpMethod.PUT, "/api/solicitudes/7/estado", tecnico));
        assertEquals(403, llamar(HttpMethod.GET, "/api/clientes", tecnico));
        assertEquals(403, llamar(HttpMethod.POST, "/api/clientes", tecnico));
    }

    @Test void fichaHistorialYAsignacionDeTecnico() {
        String coord = "Bearer " + token("COORDINADOR", SECRETO, 60_000);
        String tecnico = "Bearer " + token("TECNICO", SECRETO, 60_000);
        // Ver la ficha y el historial: cualquier usuario con sesión.
        assertEquals(200, llamar(HttpMethod.GET, "/api/solicitudes/7", tecnico));
        assertEquals(200, llamar(HttpMethod.GET, "/api/solicitudes/7/historial", tecnico));
        assertEquals(401, llamar(HttpMethod.GET, "/api/solicitudes/7/historial", null));
        // Asignar técnico y listar técnicos: solo Administrador y Coordinador.
        assertEquals(200, llamar(HttpMethod.PUT, "/api/solicitudes/7/tecnico", coord));
        assertEquals(200, llamar(HttpMethod.GET, "/api/usuarios/tecnicos", coord));
        assertEquals(403, llamar(HttpMethod.PUT, "/api/solicitudes/7/tecnico", tecnico));
        assertEquals(403, llamar(HttpMethod.GET, "/api/usuarios/tecnicos", tecnico));
        assertEquals(401, llamar(HttpMethod.GET, "/api/usuarios/tecnicos", null));
    }

    @Test void fotosColaYAlertas() {
        String tecnico = "Bearer " + token("TECNICO", SECRETO, 60_000);
        // El portal sube las fotos de evidencia sin sesión (solo con la clave de API)...
        assertEquals(200, llamar(HttpMethod.POST, "/api/solicitudes/7/fotos", null));
        // ...pero verlas, como la cola y las alertas del panel, exige sesión.
        assertEquals(401, llamar(HttpMethod.GET, "/api/solicitudes/7/fotos", null));
        assertEquals(401, llamar(HttpMethod.GET, "/api/solicitudes/7/fotos/3", null));
        assertEquals(401, llamar(HttpMethod.GET, "/api/solicitudes/cola", null));
        assertEquals(401, llamar(HttpMethod.GET, "/api/solicitudes/pulso", null));
        assertEquals(200, llamar(HttpMethod.GET, "/api/solicitudes/7/fotos", tecnico));
        assertEquals(200, llamar(HttpMethod.GET, "/api/solicitudes/7/fotos/3", tecnico));
        assertEquals(200, llamar(HttpMethod.GET, "/api/solicitudes/cola", tecnico));
        assertEquals(200, llamar(HttpMethod.GET, "/api/solicitudes/pulso", tecnico));
        // No hay forma de borrar ni reemplazar una foto desde la API: solo el Administrador podría, y la ruta ni existe.
        assertEquals(403, llamar(HttpMethod.DELETE, "/api/solicitudes/7/fotos/3", tecnico));
    }

    @Test void loNoListadoSeNiegaPorDefectoSalvoAdministrador() {
        assertEquals(403, llamar(HttpMethod.GET, "/api/usuarios/lo-que-sea", "Bearer " + token("COORDINADOR", SECRETO, 60_000)));
        assertEquals(403, llamar(HttpMethod.DELETE, "/api/solicitudes/1", "Bearer " + token("TECNICO", SECRETO, 60_000)));
        assertEquals(200, llamar(HttpMethod.GET, "/api/usuarios/lo-que-sea", "Bearer " + token("ADMINISTRADOR", SECRETO, 60_000)));
        // Una variante de la ruta pública (con "/" final) no cuenta como pública.
        assertEquals(401, llamar(HttpMethod.POST, "/api/solicitudes/", null));
    }

    @Test void sinSecretoSeRechazaLoProtegido() {
        var sinSecreto = new JwtFilter("");
        var req = MockServerHttpRequest.get("/api/solicitudes")
                .header("Authorization", "Bearer " + token("ADMINISTRADOR", SECRETO, 60_000));
        var exchange = MockServerWebExchange.from(req);
        sinSecreto.filter(exchange, e -> Mono.empty()).block();
        assertEquals(401, exchange.getResponse().getStatusCode().value());
    }

    @Test void seBorranLasCabecerasDeUsuarioYRolEnviadasDesdeAfuera() {
        var paso = new AtomicReference<ServerWebExchange>();
        llamar(HttpMethod.POST, "/api/solicitudes", null, paso);
        assertNull(paso.get().getRequest().getHeaders().getFirst("X-SGP-Rol"));
        assertNull(paso.get().getRequest().getHeaders().getFirst("X-SGP-Usuario"));

        paso.set(null);
        llamar(HttpMethod.GET, "/api/solicitudes", "Bearer " + token("TECNICO", SECRETO, 60_000), paso);
        assertEquals("TECNICO", paso.get().getRequest().getHeaders().getFirst("X-SGP-Rol"));
        assertEquals("ana@sgp.local", paso.get().getRequest().getHeaders().getFirst("X-SGP-Usuario"));
    }
}
