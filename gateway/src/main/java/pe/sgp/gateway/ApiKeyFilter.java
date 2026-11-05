package pe.sgp.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Puerta de seguridad del Gateway: solo deja pasar peticiones que traigan la clave compartida
 *  (cabecera X-SGP-Api-Key). Sin ella, o con otra distinta, responde 403 sin llegar al microservicio.
 *  Si SGP_API_KEY no está configurada se rechaza todo (falla cerrado), nunca se deja todo abierto. */
@Component
public class ApiKeyFilter implements GlobalFilter, Ordered {

    public static final String HEADER = "X-SGP-Api-Key";
    private static final Logger log = LoggerFactory.getLogger(ApiKeyFilter.class);

    private final byte[] clave;

    public ApiKeyFilter(@Value("${SGP_API_KEY:}") String clave) {
        this.clave = clave.trim().getBytes(StandardCharsets.UTF_8);
        if (this.clave.length == 0) {
            log.warn("SGP_API_KEY no está definida en .env: el Gateway rechazará TODAS las peticiones.");
        }
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String recibida = exchange.getRequest().getHeaders().getFirst(HEADER);
        if (clave.length > 0 && recibida != null
                && MessageDigest.isEqual(clave, recibida.getBytes(StandardCharsets.UTF_8))) {
            return chain.filter(exchange);
        }
        var respuesta = exchange.getResponse();
        respuesta.setStatusCode(HttpStatus.FORBIDDEN);
        respuesta.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] cuerpo = "{\"mensaje\":\"Acceso no autorizado.\"}".getBytes(StandardCharsets.UTF_8);
        return respuesta.writeWith(Mono.just(respuesta.bufferFactory().wrap(cuerpo)));
    }

    /** Primero de todos: nada se reenvía antes de comprobar la clave. */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
