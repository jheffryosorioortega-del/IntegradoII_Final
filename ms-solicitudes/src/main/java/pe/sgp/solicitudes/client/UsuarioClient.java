package pe.sgp.solicitudes.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import pe.sgp.solicitudes.dto.TecnicoDTO;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

/** Única puerta por la que ms-solicitudes habla con ms-usuarios (directo, sin Gateway, como con ms-clientes).
 *  Sirve para comprobar que quien se asigna a una solicitud de verdad es un técnico activo. */
@Component
public class UsuarioClient {

    private final RestClient restClient;

    public UsuarioClient(RestClient.Builder builder, @Value("${sgp.usuarios.url}") String baseUrl,
                         @Value("${SGP_API_KEY:}") String apiKey) {
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
        fabrica.setReadTimeout(Duration.ofSeconds(5));
        builder.baseUrl(baseUrl).requestFactory(fabrica);
        if (!apiKey.isBlank()) builder.defaultHeader("X-SGP-Api-Key", apiKey.trim());
        this.restClient = builder.build();
    }

    /** Técnicos activos. Si ms-usuarios no responde se lanza la excepción de conexión (la API responde 503). */
    public List<TecnicoDTO> tecnicos() {
        List<TecnicoDTO> lista = restClient.get().uri("/api/usuarios/tecnicos").retrieve()
                .body(new ParameterizedTypeReference<List<TecnicoDTO>>() {});
        return lista == null ? List.of() : lista;
    }
}
