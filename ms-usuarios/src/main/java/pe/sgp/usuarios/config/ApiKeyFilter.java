package pe.sgp.usuarios.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Segunda barrera (la primera es el Gateway): este servicio también exige la clave compartida
 *  en la cabecera X-SGP-Api-Key para todo lo que cuelga de /api. Así, llamar directo al puerto
 *  del servicio (saltándose el Gateway) tampoco funciona sin la clave.
 *  Si SGP_API_KEY no está configurada se rechaza todo (falla cerrado). */
@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-SGP-Api-Key";
    private static final Logger log = LoggerFactory.getLogger(ApiKeyFilter.class);

    private final byte[] clave;

    public ApiKeyFilter(@Value("${SGP_API_KEY:}") String clave) {
        this.clave = clave.trim().getBytes(StandardCharsets.UTF_8);
        if (this.clave.length == 0) {
            log.warn("SGP_API_KEY no está definida en .env: este servicio rechazará TODAS las peticiones /api.");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String recibida = request.getHeader(HEADER);
        if (clave.length > 0 && recibida != null
                && MessageDigest.isEqual(clave, recibida.getBytes(StandardCharsets.UTF_8))) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"mensaje\":\"Acceso no autorizado.\"}");
    }
}
