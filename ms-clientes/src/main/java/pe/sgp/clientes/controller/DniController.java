package pe.sgp.clientes.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Consulta auxiliar: no registra clientes ni conserva documentos consultados. */
@RestController
@RequestMapping("/api/clientes/dni")
public class DniController {
    private final RestClient api;
    private final String token;
    private final String internalKey;
    private long windowStart = System.nanoTime();
    private int consultas;

    public DniController(@Value("${DECOLECTA_TOKEN:}") String token,
                         @Value("${SGP_DNI_INTERNAL_KEY:}") String internalKey) {
        this.token = token;
        this.internalKey = internalKey;
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2)).build());
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.api = RestClient.builder().baseUrl("https://api.decolecta.com")
                .requestFactory(factory).build();
    }

    public record Consulta(String dni) {}
    public record Persona(@JsonProperty("first_name") String nombres,
                          @JsonProperty("first_last_name") String paterno,
                          @JsonProperty("second_last_name") String materno,
                          @JsonProperty("document_number") String documento) {}

    @PostMapping
    public ResponseEntity<?> consultar(@RequestHeader(value = "X-SGP-DNI-Key", defaultValue = "") String key,
                                       @RequestBody Consulta request) {
        if (internalKey.isBlank() || !MessageDigest.isEqual(internalKey.getBytes(StandardCharsets.UTF_8),
                key.getBytes(StandardCharsets.UTF_8))) {
            return error(HttpStatus.FORBIDDEN, "Consulta no autorizada.");
        }
        if (request.dni() == null || !request.dni().matches("[0-9]{8}")) {
            return error(HttpStatus.BAD_REQUEST, "Escribe un DNI de 8 dígitos.");
        }
        if (token.isBlank()) return error(HttpStatus.SERVICE_UNAVAILABLE, "La consulta DNI no está configurada.");
        if (!permitirConsulta()) return error(HttpStatus.TOO_MANY_REQUESTS, "Espera un minuto antes de volver a consultar.");
        try {
            Persona persona = api.get().uri("/v1/reniec/dni?numero={dni}", request.dni())
                    .headers(h -> h.setBearerAuth(token)).retrieve().body(Persona.class);
            if (persona == null || !request.dni().equals(persona.documento())
                    || persona.nombres() == null || persona.nombres().isBlank()
                    || persona.paterno() == null || persona.paterno().isBlank()) {
                return error(HttpStatus.BAD_GATEWAY, "No se recibió un nombre válido. Puedes escribirlo manualmente.");
            }
            String nombre = Stream.of(persona.nombres(), persona.paterno(), persona.materno())
                    .filter(s -> s != null && !s.isBlank()).map(String::trim).collect(Collectors.joining(" "));
            return ResponseEntity.ok().header("Cache-Control", "no-store").body(Map.of("nombre", nombre));
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) return error(HttpStatus.NOT_FOUND, "No se encontraron datos para ese DNI.");
            if (e.getStatusCode().value() == 429) return error(HttpStatus.TOO_MANY_REQUESTS, "Se alcanzó el límite de consultas. Intenta más tarde.");
            return error(HttpStatus.SERVICE_UNAVAILABLE, "La consulta DNI no está disponible. Puedes escribir tu nombre manualmente.");
        } catch (RestClientException e) {
            return error(HttpStatus.SERVICE_UNAVAILABLE, "No pudimos conectar con el proveedor. Puedes escribir tu nombre manualmente.");
        }
    }

    private synchronized boolean permitirConsulta() {
        long now = System.nanoTime();
        if (now - windowStart >= Duration.ofMinutes(1).toNanos()) { windowStart = now; consultas = 0; }
        return ++consultas <= 30;
    }

    private ResponseEntity<?> error(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status).header("Cache-Control", "no-store").body(Map.of("mensaje", mensaje));
    }
}
