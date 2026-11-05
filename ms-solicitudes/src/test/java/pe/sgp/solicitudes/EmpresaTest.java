package pe.sgp.solicitudes;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;
import jakarta.validation.Validator;
import pe.sgp.solicitudes.client.ClienteClient;
import pe.sgp.solicitudes.dto.*;
import pe.sgp.solicitudes.model.*;
import pe.sgp.solicitudes.service.SolicitudService;
import pe.sgp.solicitudes.repository.SolicitudRepository;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.config.import=", "spring.datasource.url=jdbc:h2:mem:empresa_test;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=validate",
    "spring.flyway.locations=classpath:db/migration/h2"
})
class EmpresaTest {
    @Autowired Validator validator;
    @Autowired SolicitudService service;
    @Autowired SolicitudRepository repository;
    @TestConfiguration
    static class Config {
        @Bean @Primary ClienteClient clientePrueba() {
            return new ClienteClient(RestClient.builder(), "http://localhost:1", "") {
                @Override public Optional<ClienteDTO> buscarPorTelefono(String telefono) {
                    return Optional.of(new ClienteDTO(1L, "Nombre anterior", telefono, null, null, null));
                }
            };
        }
    }
    private SolicitudPublicaRequest datos(boolean empresa) {
        var req = new SolicitudPublicaRequest();
        req.setNombre("Ana Prueba"); req.setTelefono("987654321"); req.setCorreo("ana@example.com");
        req.setDireccion("Av. Prueba 123"); req.setCategoria(CategoriaServicio.BOMBAS);
        req.setDescripcion("Revisar bomba de agua del edificio"); req.setUrgencia(Urgencia.MEDIA);
        if (empresa) {
            req.setTipoSolicitante("EMPRESA"); req.setRuc("20000000001");
            req.setRazonSocial("EMPRESA 123 & HIJOS S.A.C."); req.setDireccionFiscal("Av. Fiscal 456");
            req.setCargo("Jefa de mantenimiento"); req.setRequiereFactura(true);
        }
        return req;
    }
    @Test void guardaYRecuperaTodosLosDatosEmpresaConContactoActual() {
        var req = datos(true);
        assertTrue(validator.validate(req).isEmpty());
        var result = service.registrarDesdePortal(req);
        var saved = repository.findById(result.id()).orElseThrow();
        assertEquals("EMPRESA", saved.getTipoSolicitante());
        assertEquals(req.getRuc(), saved.getRuc());
        assertEquals(req.getRazonSocial(), saved.getRazonSocial());
        assertEquals(req.getDireccionFiscal(), saved.getDireccionFiscal());
        assertEquals("Ana Prueba", saved.getPersonaContacto());
        assertEquals(req.getCargo(), saved.getCargo());
        assertEquals(req.getCorreo(), saved.getCorreoContacto());
        assertTrue(saved.isRequiereFactura());
        assertEquals(req.getRazonSocial(), service.listar(null).stream().filter(s -> s.id().equals(saved.getId())).findFirst().orElseThrow().razonSocial());
    }
    @Test void exigeRucRazonYCorreoSoloEnEmpresa() {
        var req = datos(true); req.setRuc("123"); req.setRazonSocial(""); req.setCorreo("");
        var fields = validator.validate(req).stream().map(v -> v.getPropertyPath().toString()).toList();
        assertTrue(fields.containsAll(java.util.List.of("ruc","razonSocial","correo")));
        req.setTipoSolicitante("PERSONA");
        assertTrue(validator.validate(req).isEmpty());
        var saved = service.registrarDesdePortal(req);
        assertEquals("PERSONA", saved.tipoSolicitante());
        assertNull(saved.ruc()); assertNull(saved.razonSocial()); assertFalse(saved.requiereFactura());
    }
    @Test void rechazaHtmlYTextoExcesivoEnEmpresa() {
        var req = datos(true); req.setRazonSocial("<script>"); req.setDireccionFiscal("x".repeat(301)); req.setCargo("x".repeat(81));
        var fields = validator.validate(req).stream().map(v -> v.getPropertyPath().toString()).toList();
        assertTrue(fields.containsAll(java.util.List.of("razonSocial","direccionFiscal","cargo")));
    }
}
