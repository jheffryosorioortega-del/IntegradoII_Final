package pe.sgp.clientes.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;
import java.net.http.HttpClient;
import java.time.Duration;

@Component
public class RucClient {
    private final RestClient api;
    private final String token;
    public RucClient(@Value("${DECOLECTA_TOKEN:}") String token) {
        this.token = token;
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
        factory.setReadTimeout(Duration.ofSeconds(5));
        api = RestClient.builder().baseUrl("https://api.decolecta.com").requestFactory(factory).build();
    }
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Empresa(@JsonProperty("numero_documento") String ruc,
                          @JsonProperty("razon_social") String razonSocial,
                          @JsonProperty("direccion") String direccionFiscal) {}
    public Empresa consultar(String ruc) {
        if (token.isBlank()) throw fallo(HttpStatus.SERVICE_UNAVAILABLE);
        try {
            Empresa data = api.get().uri("/v1/sunat/ruc?numero={ruc}", ruc)
                    .headers(h -> h.setBearerAuth(token)).retrieve().body(Empresa.class);
            if (data == null || !ruc.equals(data.ruc()) || data.razonSocial() == null
                    || data.razonSocial().isBlank() || data.razonSocial().length() > 250
                    || (data.direccionFiscal() != null && data.direccionFiscal().length() > 300)) {
                throw fallo(HttpStatus.BAD_GATEWAY);
            }
            return data;
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            if (status == 404) throw fallo(HttpStatus.NOT_FOUND);
            if (status == 429) throw fallo(HttpStatus.TOO_MANY_REQUESTS);
            throw fallo(HttpStatus.SERVICE_UNAVAILABLE);
        } catch (RestClientException e) {
            throw fallo(HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
    private ResponseStatusException fallo(HttpStatus status) {
        return new ResponseStatusException(status, "Consulta RUC no disponible");
    }
}
