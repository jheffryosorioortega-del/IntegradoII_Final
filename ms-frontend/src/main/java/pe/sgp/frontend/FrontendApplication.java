package pe.sgp.frontend;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestClient;
import pe.sgp.frontend.config.SgpPrincipal;

import java.net.http.HttpClient;
import java.time.Duration;

@SpringBootApplication
public class FrontendApplication {

    public static void main(String[] args) {
        SpringApplication.run(FrontendApplication.class, args);
    }

    /** Un único cliente HTTP apuntando al Gateway; lo reutilizan UsuarioClient,
     *  ClienteClient y SolicitudClient. Con tiempos máximos de espera: si el Gateway o un
     *  servicio no contesta, la pantalla muestra un aviso en vez de quedarse cargando para siempre. */
    @Bean
    public RestClient gatewayRestClient(@Value("${sgp.gateway.url}") String gatewayUrl,
                                        @Value("${SGP_API_KEY:}") String apiKey) {
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
        fabrica.setReadTimeout(Duration.ofSeconds(10));
        RestClient.Builder builder = RestClient.builder().baseUrl(gatewayUrl).requestFactory(fabrica);
        // Clave compartida: el Gateway y los servicios rechazan toda petición /api que no la traiga.
        if (!apiKey.isBlank()) builder.defaultHeader("X-SGP-Api-Key", apiKey.trim());
        // Si hay un usuario con sesión, su JWT viaja en cada llamada: el Gateway decide por rol.
        // Sin sesión (portal público) no se manda nada y solo funcionan las rutas públicas.
        builder.requestInterceptor((peticion, cuerpo, ejecucion) -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof SgpPrincipal usuario && usuario.token() != null) {
                peticion.getHeaders().setBearerAuth(usuario.token());
            }
            return ejecucion.execute(peticion, cuerpo);
        });
        return builder.build();
    }
}
