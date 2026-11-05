package pe.sgp.clientes.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class ApiKeyFilterTest {

    private MockHttpServletResponse llamar(String claveConfigurada, String ruta, String claveEnviada) throws Exception {
        var filtro = new ApiKeyFilter(claveConfigurada);
        var req = new MockHttpServletRequest("GET", ruta);
        req.setRequestURI(ruta);
        if (claveEnviada != null) req.addHeader(ApiKeyFilter.HEADER, claveEnviada);
        var res = new MockHttpServletResponse();
        FilterChain cadena = new MockFilterChain();
        filtro.doFilter(req, res, cadena);
        return res;
    }

    @Test void dejaPasarConLaClaveCorrecta() throws Exception {
        assertEquals(200, llamar("clave-de-prueba", "/api/clientes", "clave-de-prueba").getStatus());
    }

    @Test void rechazaSinClaveOConClaveIncorrecta() throws Exception {
        assertEquals(403, llamar("clave-de-prueba", "/api/clientes", null).getStatus());
        assertEquals(403, llamar("clave-de-prueba", "/api/clientes", "otra").getStatus());
        assertEquals(403, llamar("clave-de-prueba", "/api/clientes/dni", "").getStatus());
    }

    @Test void siNoHayClaveConfiguradaRechazaTodoIncluidoUnaCabeceraVacia() throws Exception {
        assertEquals(403, llamar("", "/api/clientes", "").getStatus());
        assertEquals(403, llamar("  ", "/api/clientes", "  ").getStatus());
    }

    @Test void noTocaRutasQueNoSonDeLaApi() throws Exception {
        assertEquals(200, llamar("clave-de-prueba", "/actuator/health", null).getStatus());
    }
}
