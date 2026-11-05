package pe.sgp.frontend.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@RestController
public class DniController {
    private final RestClient gateway;
    private final String internalKey;

    public DniController(RestClient gatewayRestClient, @Value("${SGP_DNI_INTERNAL_KEY:}") String internalKey) {
        this.gateway = gatewayRestClient;
        this.internalKey = internalKey;
    }

    public record Consulta(String dni) {}
    public record Resultado(String nombre) {}

    // POST conserva la protección CSRF y evita colocar el DNI en la URL pública.
    @PostMapping("/contacto/consultar-dni")
    public ResponseEntity<?> consultar(@RequestBody Consulta request, HttpSession session) {
        if (request.dni() == null || !request.dni().matches("[0-9]{8}")) {
            return error(400, "Escribe un DNI de 8 dígitos.");
        }
        synchronized (session) {
            Long ultima = (Long) session.getAttribute("ultimaConsultaDni");
            long now = System.currentTimeMillis();
            if (ultima != null && now - ultima < 5000) return error(429, "Espera unos segundos antes de volver a consultar.");
            session.setAttribute("ultimaConsultaDni", now);
        }
        try {
            Resultado resultado = gateway.post().uri("/api/clientes/dni")
                    .header("X-SGP-DNI-Key", internalKey).body(request).retrieve().body(Resultado.class);
            if (resultado == null || resultado.nombre() == null || resultado.nombre().isBlank()) {
                return error(503, "No se recibió un nombre válido. Puedes escribirlo manualmente.");
            }
            return ResponseEntity.ok().header("Cache-Control", "no-store").body(resultado);
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            if (status == 404) return error(404, "No se encontraron datos para ese DNI.");
            if (status == 429) return error(429, "Se alcanzó el límite de consultas. Intenta más tarde.");
            return error(503, "La consulta DNI no está disponible. Puedes escribir tu nombre manualmente.");
        } catch (RestClientException e) {
            return error(503, "No pudimos consultar el DNI. Puedes escribir tu nombre manualmente.");
        }
    }

    private ResponseEntity<?> error(int status, String mensaje) {
        return ResponseEntity.status(status).header("Cache-Control", "no-store").body(Map.of("mensaje", mensaje));
    }
}
