package pe.sgp.clientes.controller;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import pe.sgp.clientes.client.RucClient;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class RucControllerTest {
    RucController controller;
    MockRestServiceServer server;
    @BeforeEach void setup() {
        var client = new RucClient("fake-token");
        var builder = RestClient.builder().baseUrl("https://api.decolecta.com");
        server = MockRestServiceServer.bindTo(builder).build();
        ReflectionTestUtils.setField(client, "api", builder.build());
        controller = new RucController(client, "internal-test");
    }
    @Test void consultaMapeaEmpresaYNoFiltraToken() {
        server.expect(requestTo("https://api.decolecta.com/v1/sunat/ruc?numero=20000000001"))
            .andExpect(header("Authorization", "Bearer fake-token"))
            .andRespond(withSuccess("""
                {"numero_documento":"20000000001","razon_social":"EMPRESA 123 & HIJOS S.A.C.",
                 "direccion":"AV. PRUEBA 123","estado":"ACTIVO"}
                """, MediaType.APPLICATION_JSON));
        var result = controller.consultar("internal-test", new RucController.Consulta("20000000001"));
        assertEquals(200, result.getStatusCode().value());
        assertEquals("EMPRESA 123 & HIJOS S.A.C.", ((RucController.Resultado)result.getBody()).razonSocial());
        assertEquals("AV. PRUEBA 123", ((RucController.Resultado)result.getBody()).direccionFiscal());
        assertEquals("no-store", result.getHeaders().getFirst("Cache-Control"));
        server.verify();
    }
    @Test void rechazaAccesoDirectoYFormatoInvalido() {
        assertEquals(403, controller.consultar("", new RucController.Consulta("20000000001")).getStatusCode().value());
        assertEquals(400, controller.consultar("internal-test", new RucController.Consulta("123")).getStatusCode().value());
        server.verify();
    }
    @Test void ocultaFalloCredencialesYPermiteIngresoManual() {
        server.expect(anything()).andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("token privado"));
        var result = controller.consultar("internal-test", new RucController.Consulta("20000000001"));
        assertEquals(503, result.getStatusCode().value());
        assertFalse(result.getBody().toString().contains("token privado"));
    }
    @Test void rechazaRespuestaDeOtroRuc() {
        server.expect(anything()).andRespond(withSuccess("""
            {"numero_documento":"20000000002","razon_social":"OTRA EMPRESA"}
            """, MediaType.APPLICATION_JSON));
        assertEquals(503, controller.consultar("internal-test", new RucController.Consulta("20000000001")).getStatusCode().value());
    }
    @Test void conservaNoEncontradoYLimite() {
        server.expect(anything()).andRespond(withStatus(HttpStatus.NOT_FOUND));
        server.expect(anything()).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        assertEquals(404, controller.consultar("internal-test", new RucController.Consulta("20000000001")).getStatusCode().value());
        assertEquals(429, controller.consultar("internal-test", new RucController.Consulta("20000000001")).getStatusCode().value());
        server.verify();
    }
}
