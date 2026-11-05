package pe.sgp.frontend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class DniControllerTest {
    @Test void usaGatewayYLimitaRepeticionesPorSesion() {
        var builder = RestClient.builder().baseUrl("http://localhost:8080");
        var server = MockRestServiceServer.bindTo(builder).build();
        var controller = new DniController(builder.build(), "internal-test-key");
        var session = new MockHttpSession();
        server.expect(requestTo("http://localhost:8080/api/clientes/dni"))
                .andExpect(header("X-SGP-DNI-Key", "internal-test-key"))
                .andExpect(content().json("{\"dni\":\"00000001\"}"))
                .andRespond(withSuccess("{\"nombre\":\"ANA PRUEBA\"}", MediaType.APPLICATION_JSON));
        var result = controller.consultar(new DniController.Consulta("00000001"), session);
        assertEquals(200, result.getStatusCode().value());
        assertEquals("ANA PRUEBA", ((DniController.Resultado) result.getBody()).nombre());
        assertEquals(429, controller.consultar(new DniController.Consulta("00000001"), session).getStatusCode().value());
        assertEquals(400, controller.consultar(new DniController.Consulta("abc"), new MockHttpSession()).getStatusCode().value());
        server.verify();
    }
}
