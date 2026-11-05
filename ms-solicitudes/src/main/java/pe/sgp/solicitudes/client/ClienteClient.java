package pe.sgp.solicitudes.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import pe.sgp.solicitudes.dto.ClienteCreateRequest;
import pe.sgp.solicitudes.dto.ClienteDTO;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Optional;

/** "Cliente REST" (en el sentido de cliente HTTP, no confundir con la entidad Cliente):
 *  esta clase es la ÚNICA puerta por la que ms-solicitudes habla con ms-clientes.
 *  Si mañana ms-clientes cambia de URL, de puerto o hasta de tecnología, solo se
 *  toca este archivo; el resto de ms-solicitudes no se entera. */
@Component
public class ClienteClient {

    private final RestClient restClient;

    public ClienteClient(RestClient.Builder builder, @Value("${sgp.clientes.url}") String baseUrl,
                         @Value("${SGP_API_KEY:}") String apiKey) {
        // Tiempos máximos: si ms-clientes no contesta, fallamos rápido en vez de quedarnos colgados.
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
        fabrica.setReadTimeout(Duration.ofSeconds(5));
        builder.baseUrl(baseUrl).requestFactory(fabrica);
        // Esta llamada va directo a ms-clientes (sin Gateway), que también exige la clave compartida.
        if (!apiKey.isBlank()) builder.defaultHeader("X-SGP-Api-Key", apiKey.trim());
        this.restClient = builder.build();
    }

    public Optional<ClienteDTO> buscarPorTelefono(String telefono) {
        try {
            ClienteDTO cliente = restClient.get()
                    .uri("/api/clientes/buscar?telefono={telefono}", telefono)
                    .retrieve()
                    .body(ClienteDTO.class);
            return Optional.ofNullable(cliente);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }

    /** Crea el cliente. Si otra petición lo creó justo antes (409), se devuelve el que ya existe. */
    public ClienteDTO crear(ClienteCreateRequest request) {
        try {
            return restClient.post()
                    .uri("/api/clientes")
                    .body(request)
                    .retrieve()
                    .body(ClienteDTO.class);
        } catch (HttpClientErrorException.Conflict e) {
            return buscarPorTelefono(request.telefono()).orElseThrow(() -> e);
        }
    }
}
