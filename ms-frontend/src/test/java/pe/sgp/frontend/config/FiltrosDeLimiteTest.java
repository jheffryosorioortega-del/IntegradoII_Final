package pe.sgp.frontend.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/** Prueba los dos filtros de abuso sin levantar el servidor: peticiones y respuestas simuladas. */
class FiltrosDeLimiteTest {

    private final AtomicLong ahora = new AtomicLong(5_000_000);
    private final LimiteIntentos limite = new LimiteIntentos(ahora::get);
    // login: 3 fallos por IP / 4 por correo en 10 min · envíos: 2 en 10 min y 3 al día · consultas: 2 en 10 min
    private final LimitesSgp config = new LimitesSgp(3, 4, 10, 2, 10, 3, 2, 10);

    private static MockHttpServletRequest post(String ruta, String ip) {
        var req = new MockHttpServletRequest("POST", ruta);
        req.setServletPath(ruta);
        req.setRemoteAddr(ip);
        return req;
    }

    private static MockHttpServletRequest loginDe(String correo, String ip) {
        var req = post("/login", ip);
        req.setParameter("username", correo);
        return req;
    }

    /** Pasa la petición por el filtro y dice si llegó a la aplicación (true) o fue frenada (false). */
    private static boolean pasa(org.springframework.web.filter.OncePerRequestFilter filtro,
                                MockHttpServletRequest req, MockHttpServletResponse res) throws Exception {
        var cadena = new MockFilterChain();
        filtro.doFilter(req, res, cadena);
        return cadena.getRequest() != null;
    }

    // ---------------- login ----------------

    @Test
    void elLoginSeBloqueaTrasDemasiadosFallosDeUnaIp() throws Exception {
        var filtro = new LimiteLogin(limite, config);
        for (int i = 0; i < 3; i++) {
            var req = loginDe("usuario" + i + "@x.pe", "203.0.113.7");   // correos distintos: solo cuenta la IP
            assertTrue(pasa(filtro, req, new MockHttpServletResponse()), "intento " + (i + 1) + " aún pasa");
            filtro.registrarFallo(req);
        }
        var res = new MockHttpServletResponse();
        assertFalse(pasa(filtro, loginDe("otro@x.pe", "203.0.113.7"), res));
        assertEquals("/login?bloqueado", res.getRedirectedUrl());

        assertTrue(pasa(filtro, loginDe("otro@x.pe", "198.51.100.9"), new MockHttpServletResponse()),
                "otra IP no se ve afectada");
    }

    @Test
    void elLoginSeBloqueaTrasDemasiadosFallosContraUnMismoCorreoDesdeVariasIps() throws Exception {
        var filtro = new LimiteLogin(limite, config);
        for (int i = 0; i < 4; i++) filtro.registrarFallo(loginDe("Admin@SGP.local", "10.0.0." + i));
        var res = new MockHttpServletResponse();
        assertFalse(pasa(filtro, loginDe("admin@sgp.local ", "10.0.0.99"), res), "mayúsculas y espacios no esquivan el freno");
        assertEquals("/login?bloqueado", res.getRedirectedUrl());
        assertTrue(pasa(filtro, loginDe("coordinador@sgp.local", "10.0.0.99"), new MockHttpServletResponse()));
    }

    @Test
    void unLoginCorrectoLimpiaElCorreoPeroNoLaIp() throws Exception {
        var filtro = new LimiteLogin(limite, config);
        var req = loginDe("tecnico@sgp.local", "203.0.113.7");
        filtro.registrarFallo(req);
        filtro.registrarFallo(req);
        filtro.registrarExito(req);
        // El correo quedó limpio, pero la IP conserva sus 2 fallos: con uno más (3) se bloquea.
        filtro.registrarFallo(loginDe("otro@x.pe", "203.0.113.7"));
        assertFalse(pasa(filtro, loginDe("tecnico@sgp.local", "203.0.113.7"), new MockHttpServletResponse()));
    }

    @Test
    void elBloqueoDelLoginSeLevantaConElTiempo() throws Exception {
        var filtro = new LimiteLogin(limite, config);
        for (int i = 0; i < 3; i++) filtro.registrarFallo(loginDe("a@x.pe", "203.0.113.7"));
        assertFalse(pasa(filtro, loginDe("a@x.pe", "203.0.113.7"), new MockHttpServletResponse()));
        ahora.addAndGet(10 * 60_000L);
        assertTrue(pasa(filtro, loginDe("a@x.pe", "203.0.113.7"), new MockHttpServletResponse()));
    }

    @Test
    void elFiltroDeLoginIgnoraLoQueNoEsElPostDeLogin() throws Exception {
        var filtro = new LimiteLogin(limite, config);
        for (int i = 0; i < 5; i++) filtro.registrarFallo(loginDe("a@x.pe", "203.0.113.7"));
        var get = new MockHttpServletRequest("GET", "/login");
        get.setServletPath("/login");
        get.setRemoteAddr("203.0.113.7");
        assertTrue(pasa(filtro, get, new MockHttpServletResponse()), "ver la página de login siempre se puede");
    }

    // ---------------- envíos y consultas ----------------

    @Test
    void elFormularioTieneUnTopePorIpYMuestraUnAviso() throws Exception {
        var filtro = new LimiteEnvios(limite, config);
        assertTrue(pasa(filtro, post("/contacto", "203.0.113.7"), new MockHttpServletResponse()));
        assertTrue(pasa(filtro, post("/contacto", "203.0.113.7"), new MockHttpServletResponse()));
        var res = new MockHttpServletResponse();
        assertFalse(pasa(filtro, post("/contacto", "203.0.113.7"), res));
        assertEquals("/contacto?limite", res.getRedirectedUrl());
        assertTrue(pasa(filtro, post("/contacto", "198.51.100.9"), new MockHttpServletResponse()), "otra IP sigue pudiendo");
    }

    @Test
    void elFormularioTieneTambienUnTopeDiario() throws Exception {
        var filtro = new LimiteEnvios(limite, config);
        // 2 por ventana de 10 min, 3 por día: se espera entre pares para que solo el tope diario frene.
        for (int i = 0; i < 3; i++) {
            assertTrue(pasa(filtro, post("/contacto", "203.0.113.7"), new MockHttpServletResponse()), "envío " + (i + 1));
            ahora.addAndGet(11 * 60_000L);
        }
        var res = new MockHttpServletResponse();
        assertFalse(pasa(filtro, post("/contacto", "203.0.113.7"), res), "el 4.º del día se frena aunque la ventana corta esté libre");
        assertEquals("/contacto?limite", res.getRedirectedUrl());
        ahora.addAndGet(24 * 3_600_000L);
        assertTrue(pasa(filtro, post("/contacto", "203.0.113.7"), new MockHttpServletResponse()), "al día siguiente se puede de nuevo");
    }

    @Test
    void lasConsultasDniYRucRespondenError429EnJson() throws Exception {
        var filtro = new LimiteEnvios(limite, config);
        assertTrue(pasa(filtro, post("/contacto/consultar-dni", "203.0.113.7"), new MockHttpServletResponse()));
        assertTrue(pasa(filtro, post("/contacto/consultar-ruc", "203.0.113.7"), new MockHttpServletResponse()),
                "DNI y RUC comparten el mismo tope");
        var res = new MockHttpServletResponse();
        assertFalse(pasa(filtro, post("/contacto/consultar-dni", "203.0.113.7"), res));
        assertEquals(429, res.getStatus());
        assertTrue(res.getContentType().startsWith("application/json"));
        assertTrue(res.getContentAsString().contains("\"mensaje\""));
    }

    @Test
    void laPaginaDelFormularioNoSeLimita() throws Exception {
        var filtro = new LimiteEnvios(limite, config);
        for (int i = 0; i < 10; i++) {
            var get = new MockHttpServletRequest("GET", "/contacto");
            get.setServletPath("/contacto");
            get.setRemoteAddr("203.0.113.7");
            assertTrue(pasa(filtro, get, new MockHttpServletResponse()));
        }
    }
}
