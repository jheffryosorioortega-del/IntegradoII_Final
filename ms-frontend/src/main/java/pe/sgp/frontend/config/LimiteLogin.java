package pe.sgp.frontend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Locale;

/** Freno a las contraseñas adivinadas: tras demasiados intentos fallidos desde una IP (o contra un mismo correo) se
 *  dejan de aceptar logins hasta que pase la ventana, y la persona ve "demasiados intentos" en vez de seguir probando.
 *  Va delante del filtro de login de Spring Security; SecurityConfig avisa aquí de cada fallo y cada éxito. */
public class LimiteLogin extends OncePerRequestFilter {

    private final LimiteIntentos limite;
    private final int maxPorIp;
    private final int maxPorCorreo;
    private final Duration ventana;

    public LimiteLogin(LimiteIntentos limite, LimitesSgp config) {
        this.limite = limite;
        this.maxPorIp = config.loginFallosPorIp();
        this.maxPorCorreo = config.loginFallosPorCorreo();
        this.ventana = Duration.ofMinutes(config.loginVentanaMinutos());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equals(request.getMethod()) && "/login".equals(request.getServletPath()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        if (limite.alcanzado(claveIp(req), maxPorIp, ventana)
                || limite.alcanzado(claveCorreo(req), maxPorCorreo, ventana)) {
            res.sendRedirect(req.getContextPath() + "/login?bloqueado");
            return;
        }
        chain.doFilter(req, res);
    }

    /** Una contraseña errónea: cuenta contra la IP y contra el correo. */
    public void registrarFallo(HttpServletRequest req) {
        limite.registrar(claveIp(req), ventana);
        limite.registrar(claveCorreo(req), ventana);
    }

    /** Login correcto: se limpia el contador de ESE correo. El de la IP no se toca, para que alguien con su propia
     *  cuenta no pueda "reiniciar" el freno entre intentos contra la cuenta de otra persona. */
    public void registrarExito(HttpServletRequest req) {
        limite.reiniciar(claveCorreo(req));
    }

    private static String claveIp(HttpServletRequest req) {
        return "login:ip:" + req.getRemoteAddr();
    }

    private static String claveCorreo(HttpServletRequest req) {
        String correo = req.getParameter("username");
        correo = correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
        if (correo.length() > 120) correo = correo.substring(0, 120);
        return "login:correo:" + correo;
    }
}
