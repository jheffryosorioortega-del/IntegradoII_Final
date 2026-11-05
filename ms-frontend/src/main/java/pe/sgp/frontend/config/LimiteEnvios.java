package pe.sgp.frontend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** Freno al abuso de lo que cualquiera puede usar sin iniciar sesión:
 *  - enviar el formulario de solicitud (POST /contacto): cada envío puede traer hasta 24 MB de fotos que se guardan
 *    en la base, así que sin tope bastarían unos pocos envíos para llenar el disco;
 *  - consultar DNI o RUC: cada consulta gasta cupo del proveedor externo.
 *  Va ANTES del filtro CSRF, que es el que lee (y guarda en memoria) el formulario con sus fotos: así un envío
 *  rechazado no llega a cargarse. */
public class LimiteEnvios extends OncePerRequestFilter {

    private static final String FORMULARIO = "/contacto";
    private static final String CONSULTA_DNI = "/contacto/consultar-dni";
    private static final String CONSULTA_RUC = "/contacto/consultar-ruc";

    private final LimiteIntentos limite;
    private final LimitesSgp config;

    public LimiteEnvios(LimiteIntentos limite, LimitesSgp config) {
        this.limite = limite;
        this.config = config;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"POST".equals(request.getMethod())) return true;
        String ruta = request.getServletPath();
        return !(FORMULARIO.equals(ruta) || CONSULTA_DNI.equals(ruta) || CONSULTA_RUC.equals(ruta));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String ip = req.getRemoteAddr();
        if (FORMULARIO.equals(req.getServletPath())) {
            boolean cupo = limite.intentar("envio:ip:" + ip, config.enviosPorIp(), Duration.ofMinutes(config.enviosVentanaMinutos()))
                    && limite.intentar("envio:dia:" + ip, config.enviosPorIpDia(), Duration.ofHours(24));
            if (!cupo) {
                res.sendRedirect(req.getContextPath() + "/contacto?limite");
                return;
            }
        } else if (!limite.intentar("consulta:ip:" + ip, config.consultasPorIp(), Duration.ofMinutes(config.consultasVentanaMinutos()))) {
            res.setStatus(429);
            res.setHeader("Cache-Control", "no-store");
            res.setContentType("application/json;charset=UTF-8");
            res.getOutputStream().write("{\"mensaje\":\"Demasiadas consultas desde tu conexión. Intenta de nuevo en unos minutos.\"}"
                    .getBytes(StandardCharsets.UTF_8));
            return;
        }
        chain.doFilter(req, res);
    }
}
