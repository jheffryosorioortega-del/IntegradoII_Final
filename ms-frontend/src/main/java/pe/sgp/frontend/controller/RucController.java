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
public class RucController {
    private final RestClient gateway;
    private final String internalKey;

    public RucController(RestClient gatewayRestClient, @Value("${SGP_DNI_INTERNAL_KEY:}") String internalKey) {
        this.gateway = gatewayRestClient;
        this.internalKey = internalKey;
    }

    public record Consulta(String ruc) {}
    public record Resultado(String ruc, String razonSocial, String direccionFiscal) {}

    // POST conserva la protección CSRF y evita colocar el RUC en la URL pública.
    @PostMapping("/contacto/consultar-ruc")
    public ResponseEntity<?> consultar(@RequestBody Consulta request, HttpSession session) {
        if (request.ruc() == null || !request.ruc().matches("[0-9]{11}")) {
            return error(400, "Escribe un RUC de 11 dígitos.");
        }
        synchronized (session) {
            Long ultima = (Long) session.getAttribute("ultimaConsultaRuc");
            long now = System.currentTimeMillis();
            if (ultima != null && now - ultima < 5000) return error(429, "Espera unos segundos antes de volver a consultar.");
            session.setAttribute("ultimaConsultaRuc", now);
        }
        try {
            Resultado resultado = gateway.post().uri("/api/clientes/ruc")
                    .header("X-SGP-DNI-Key", internalKey).body(request).retrieve().body(Resultado.class);
            if (resultado == null || !request.ruc().equals(resultado.ruc()) || resultado.razonSocial() == null || resultado.razonSocial().isBlank()) {
                return error(503, "No se recibió una razón social válida. Puedes escribirla manualmente.");
            }
            return ResponseEntity.ok().header("Cache-Control", "no-store").body(resultado);
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            if (status == 404) return error(404, "No se encontraron datos para ese RUC.");
            if (status == 429) return error(429, "Se alcanzó el límite de consultas. Intenta más tarde.");
            return error(503, "La consulta RUC no está disponible. Puedes escribir los datos manualmente.");
        } catch (RestClientException e) {
            return error(503, "No pudimos consultar el RUC. Puedes escribir los datos manualmente.");
        }
    }

    private ResponseEntity<?> error(int status, String mensaje) {
        return ResponseEntity.status(status).header("Cache-Control", "no-store").body(Map.of("mensaje", mensaje));
    }
}
