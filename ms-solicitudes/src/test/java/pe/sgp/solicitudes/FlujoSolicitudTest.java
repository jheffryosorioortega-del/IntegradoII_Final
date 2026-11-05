package pe.sgp.solicitudes;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import pe.sgp.solicitudes.client.ClienteClient;
import pe.sgp.solicitudes.client.UsuarioClient;
import pe.sgp.solicitudes.dto.ClienteDTO;
import pe.sgp.solicitudes.dto.SolicitudPublicaRequest;
import pe.sgp.solicitudes.dto.SolicitudResponse;
import pe.sgp.solicitudes.dto.TecnicoDTO;
import pe.sgp.solicitudes.model.CategoriaServicio;
import pe.sgp.solicitudes.model.EstadoSolicitud;
import pe.sgp.solicitudes.model.Urgencia;
import pe.sgp.solicitudes.service.SolicitudService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static pe.sgp.solicitudes.model.EstadoSolicitud.*;

/** Flujo de estados, técnico asignado e historial, contra una base H2 con las migraciones reales (V1 a V3). */
@SpringBootTest(properties = {
    "spring.config.import=", "spring.datasource.url=jdbc:h2:mem:flujo_test;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=validate",
    "spring.flyway.locations=classpath:db/migration/h2"
})
class FlujoSolicitudTest {
    private static final TecnicoDTO LUIS = new TecnicoDTO(3L, "Luis Técnico", "luis@sgp.local");

    @Autowired SolicitudService service;

    @TestConfiguration
    static class Config {
        @Bean @Primary ClienteClient clientePrueba() {
            return new ClienteClient(RestClient.builder(), "http://localhost:1", "") {
                @Override public Optional<ClienteDTO> buscarPorTelefono(String telefono) {
                    return Optional.of(new ClienteDTO(1L, "Ana Prueba", telefono, null, null, null));
                }
            };
        }
        @Bean @Primary UsuarioClient usuariosPrueba() {
            return new UsuarioClient(RestClient.builder(), "http://localhost:1", "") {
                @Override public List<TecnicoDTO> tecnicos() { return List.of(LUIS); }
            };
        }
    }

    private SolicitudResponse nueva() {
        var req = new SolicitudPublicaRequest();
        req.setNombre("Ana Prueba"); req.setTelefono("987654321"); req.setCorreo("ana@example.com");
        req.setDireccion("Av. Prueba 123"); req.setCategoria(CategoriaServicio.BOMBAS);
        req.setDescripcion("Revisar bomba de agua del edificio"); req.setUrgencia(Urgencia.MEDIA);
        return service.registrarDesdePortal(req);
    }

    private void esperaConflicto(Runnable accion, String fragmentoDelMensaje) {
        var e = assertThrows(ResponseStatusException.class, accion::run);
        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
        assertTrue(e.getReason().contains(fragmentoDelMensaje), e.getReason());
    }

    @Test void alRegistrarQuedaLaPrimeraLineaDelHistorialYLasEtapasPermitidas() {
        var s = nueva();
        assertEquals(REGISTRADA, s.estado());
        assertEquals(List.of(PENDIENTE_EVALUACION, CERRADA), s.siguientes());
        assertNull(s.tecnicoCorreo());
        var h = service.historial(s.id());
        assertEquals(1, h.size());
        assertEquals("CREACION", h.get(0).accion());
        assertEquals("portal", h.get(0).usuario());
        assertEquals("Solicitud registrada", h.get(0).descripcion());
    }

    @Test void rechazaSaltarEtapasYNoDejaHistorial() {
        var s = nueva();
        esperaConflicto(() -> service.cambiarEstado(s.id(), COTIZADA, "admin@sgp.local"), "No se puede pasar de «Registrada» a «Cotizada»");
        assertEquals(REGISTRADA, service.obtener(s.id()).estado());
        assertEquals(1, service.historial(s.id()).size());
    }

    @Test void pedirLaMismaEtapaNoHaceNadaNiDuplicaHistorial() {
        var s = nueva();
        service.cambiarEstado(s.id(), PENDIENTE_EVALUACION, "admin@sgp.local");
        service.cambiarEstado(s.id(), PENDIENTE_EVALUACION, "admin@sgp.local");
        assertEquals(2, service.historial(s.id()).size());
    }

    @Test void programarOEjecutarExigeTecnicoAsignado() {
        var s = nueva();
        for (var paso : List.of(PENDIENTE_EVALUACION, COTIZADA, APROBADA)) service.cambiarEstado(s.id(), paso, "coord@sgp.local");
        esperaConflicto(() -> service.cambiarEstado(s.id(), PROGRAMADA, "coord@sgp.local"), "Asigna un técnico");
        assertEquals(APROBADA, service.obtener(s.id()).estado());

        service.asignarTecnico(s.id(), "LUIS@sgp.local", "coord@sgp.local");   // el correo no distingue mayúsculas
        var programada = service.cambiarEstado(s.id(), PROGRAMADA, "coord@sgp.local");
        assertEquals(PROGRAMADA, programada.estado());
        assertEquals("Luis Técnico", programada.tecnicoNombre());
        assertEquals("luis@sgp.local", programada.tecnicoCorreo());
    }

    @Test void soloSeAsignaUnTecnicoActivoDeVerdad() {
        var s = nueva();
        var e = assertThrows(ResponseStatusException.class, () -> service.asignarTecnico(s.id(), "otro@sgp.local", "admin@sgp.local"));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        assertNull(service.obtener(s.id()).tecnicoCorreo());
    }

    @Test void noSeQuitaElTecnicoDeUnaSolicitudProgramadaNiSeCambiaUnaCerrada() {
        var s = nueva();
        service.asignarTecnico(s.id(), "luis@sgp.local", "admin@sgp.local");
        for (var paso : List.of(PENDIENTE_EVALUACION, COTIZADA, APROBADA, PROGRAMADA)) service.cambiarEstado(s.id(), paso, "admin@sgp.local");
        esperaConflicto(() -> service.asignarTecnico(s.id(), "", "admin@sgp.local"), "No puedes quitar al técnico");

        service.cambiarEstado(s.id(), CERRADA, "admin@sgp.local");
        assertTrue(service.obtener(s.id()).siguientes().isEmpty());
        esperaConflicto(() -> service.asignarTecnico(s.id(), "luis@sgp.local", "admin@sgp.local"), "cerrada");
        esperaConflicto(() -> service.cambiarEstado(s.id(), REGISTRADA, "admin@sgp.local"), "No se puede pasar");
    }

    @Test void elHistorialRegistraQuienYQueEnOrden() {
        var s = nueva();
        service.asignarTecnico(s.id(), "luis@sgp.local", "coord@sgp.local");
        service.asignarTecnico(s.id(), "luis@sgp.local", "coord@sgp.local");   // mismo técnico: sin línea nueva
        service.cambiarEstado(s.id(), PENDIENTE_EVALUACION, "admin@sgp.local");
        service.asignarTecnico(s.id(), "", "admin@sgp.local");

        var h = service.historial(s.id());
        assertEquals(4, h.size());
        assertEquals(List.of("Solicitud registrada",
                "Técnico: Sin asignar → Luis Técnico",
                "Estado: Registrada → Pendiente de evaluación",
                "Técnico: Luis Técnico → Sin asignar"),
                h.stream().map(x -> x.descripcion()).toList());
        assertEquals(List.of("portal", "coord@sgp.local", "admin@sgp.local", "admin@sgp.local"),
                h.stream().map(x -> x.usuario()).toList());
    }

    @Test void solicitudInexistenteDa404() {
        var e = assertThrows(ResponseStatusException.class, () -> service.historial(999_999L));
        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        assertThrows(ResponseStatusException.class, () -> service.cambiarEstado(999_999L, CERRADA, "admin@sgp.local"));
    }
}
