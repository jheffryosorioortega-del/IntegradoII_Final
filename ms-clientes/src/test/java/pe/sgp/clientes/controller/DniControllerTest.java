package pe.sgp.clientes.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class DniControllerTest {
    private DniController controller;
    private MockRestServiceServer server;

    @BeforeEach void setup() {
        controller = new DniController("test-token", "internal-test-key");
        var builder = RestClient.builder().baseUrl("https://api.decolecta.com");
        server = MockRestServiceServer.bindTo(builder).build();
        ReflectionTestUtils.setField(controller, "api", builder.build());
    }

    @Test void rechazaAccesoDirectoYDocumentoInvalidoSinConsultarProveedor() {
        assertEquals(403, controller.consultar("", new DniController.Consulta("00000001")).getStatusCode().value());
        assertEquals(400, controller.consultar("internal-test-key", new DniController.Consulta("123")).getStatusCode().value());
        server.verify();
    }

    @Test void completaNombreYConservaCerosDelDocumento() {
        server.expect(requestTo("https://api.decolecta.com/v1/reniec/dni?numero=00000001"))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andRespond(withSuccess("""
                    {"document_number":"00000001","first_name":"ANA MARIA",
                     "first_last_name":"PRUEBA","second_last_name":"EJEMPLO"}
                    """, MediaType.APPLICATION_JSON));
        var result = controller.consultar("internal-test-key", new DniController.Consulta("00000001"));
        assertEquals(200, result.getStatusCode().value());
        assertEquals("ANA MARIA PRUEBA EJEMPLO", ((java.util.Map<?, ?>) result.getBody()).get("nombre"));
        assertEquals("no-store", result.getHeaders().getFirst("Cache-Control"));
        server.verify();
    }

    @Test void ocultaErroresDeCredencialesDelProveedor() {
        server.expect(requestTo("https://api.decolecta.com/v1/reniec/dni?numero=00000001"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("private provider details"));
        var result = controller.consultar("internal-test-key", new DniController.Consulta("00000001"));
        assertEquals(503, result.getStatusCode().value());
        assertFalse(result.getBody().toString().contains("private"));
        server.verify();
    }

    @Test void rechazaRespuestaConOtroDocumento() {
        server.expect(requestTo("https://api.decolecta.com/v1/reniec/dni?numero=00000001"))
                .andRespond(withSuccess("""
                    {"document_number":"00000002","first_name":"ANA","first_last_name":"PRUEBA"}
                    """, MediaType.APPLICATION_JSON));
        assertEquals(502, controller.consultar("internal-test-key", new DniController.Consulta("00000001")).getStatusCode().value());
        server.verify();
    }
}
