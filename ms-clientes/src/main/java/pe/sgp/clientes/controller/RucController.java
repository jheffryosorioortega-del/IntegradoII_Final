package pe.sgp.clientes.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pe.sgp.clientes.client.RucClient;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/clientes/ruc")
public class RucController {
    private final RucClient client;
    private final String internalKey;
    private long windowStart = System.nanoTime();
    private int consultas;
    public RucController(RucClient client, @Value("${SGP_DNI_INTERNAL_KEY:}") String internalKey) {
        this.client = client;
        this.internalKey = internalKey;
    }
    public record Consulta(String ruc) {}
    public record Resultado(String ruc, String razonSocial, String direccionFiscal) {}
    @PostMapping
    public ResponseEntity<?> consultar(@RequestHeader(value = "X-SGP-DNI-Key", defaultValue = "") String key,
                                      @RequestBody Consulta request) {
        if (internalKey.isBlank() || !MessageDigest.isEqual(internalKey.getBytes(StandardCharsets.UTF_8), key.getBytes(StandardCharsets.UTF_8)))
            return error(403, "Consulta no autorizada.");
        if (request.ruc() == null || !request.ruc().matches("[0-9]{11}"))
            return error(400, "Escribe un RUC de 11 dígitos.");
        if (!permitirConsulta()) return error(429, "Espera un minuto antes de volver a consultar.");
        try {
            var data = client.consultar(request.ruc());
            return ResponseEntity.ok().header("Cache-Control", "no-store")
                    .body(new Resultado(data.ruc(), data.razonSocial(), data.direccionFiscal()));
        } catch (ResponseStatusException e) {
            int status = e.getStatusCode().value();
            if (status == 404) return error(404, "No se encontraron datos para ese RUC.");
            if (status == 429) return error(429, "Se alcanzó el límite de consultas. Intenta más tarde.");
            return error(503, "La consulta RUC no está disponible. Puedes completar los datos manualmente.");
        }
    }
    private synchronized boolean permitirConsulta() {
        long now = System.nanoTime();
        if (now - windowStart >= Duration.ofMinutes(1).toNanos()) { windowStart = now; consultas = 0; }
        return ++consultas <= 30;
    }
    private ResponseEntity<?> error(int status, String mensaje) {
        return ResponseEntity.status(status).header("Cache-Control", "no-store").body(Map.of("mensaje", mensaje));
    }
}
