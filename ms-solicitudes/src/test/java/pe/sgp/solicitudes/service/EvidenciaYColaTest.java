package pe.sgp.solicitudes.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import pe.sgp.solicitudes.client.ClienteClient;
import pe.sgp.solicitudes.client.UsuarioClient;
import pe.sgp.solicitudes.dto.ClienteDTO;
import pe.sgp.solicitudes.dto.SolicitudPublicaRequest;
import pe.sgp.solicitudes.dto.SolicitudResponse;
import pe.sgp.solicitudes.model.CategoriaServicio;
import pe.sgp.solicitudes.model.EstadoSolicitud;
import pe.sgp.solicitudes.model.Urgencia;
import pe.sgp.solicitudes.repository.SolicitudRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Fotos de evidencia, cola de atención y alertas, con la base H2 y las migraciones reales (V1 a V4). */
@SpringBootTest(properties = {
    "spring.config.import=", "spring.datasource.url=jdbc:h2:mem:evidencia_test;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=validate",
    "spring.flyway.locations=classpath:db/migration/h2"
})
class EvidenciaYColaTest {

    @Autowired SolicitudService service;
    @Autowired SolicitudRepository repositorio;

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
                @Override public List<pe.sgp.solicitudes.dto.TecnicoDTO> tecnicos() { return List.of(); }
            };
        }
    }

    private SolicitudResponse nueva(Urgencia urgencia) {
        var req = new SolicitudPublicaRequest();
        req.setNombre("Ana Prueba"); req.setTelefono("987654321"); req.setCorreo("ana@example.com");
        req.setDireccion("Av. Prueba 123"); req.setCategoria(CategoriaServicio.BOMBAS);
        req.setDescripcion("Revisar bomba de agua del edificio"); req.setUrgencia(urgencia);
        return service.registrarDesdePortal(req);
    }

    private static MultipartFile jpg(String nombre, int extra) {
        byte[] b = new byte[16 + extra];
        b[0] = (byte) 0xFF; b[1] = (byte) 0xD8; b[2] = (byte) 0xFF; b[3] = (byte) 0xE0;
        return new MockMultipartFile("fotos", nombre, "image/jpeg", b);
    }

    private void esperaEstado(HttpStatus esperado, Runnable accion) {
        var e = assertThrows(ResponseStatusException.class, accion::run);
        assertEquals(esperado, e.getStatusCode(), e.getReason());
    }

    // ---------- Fotos ----------

    @Test void guardaFotosValidasYLasCuentaEnLaSolicitud() {
        var s = nueva(Urgencia.MEDIA);
        assertEquals(0, s.fotos());
        var resumen = service.guardarFotos(s.id(), List.of(jpg("../../baño.jpg", 0), jpg("sala.jpg", 100)));
        assertEquals(2, resumen.size());
        assertEquals("baño.jpg", resumen.get(0).nombre());          // sin carpetas
        assertEquals("image/jpeg", resumen.get(0).tipo());
        assertEquals(116, resumen.get(1).tamano());
        assertEquals(2, service.obtener(s.id()).fotos());
        assertEquals(2, service.fotos(s.id()).size());
        assertArrayEquals(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0},
                java.util.Arrays.copyOf(service.foto(s.id(), resumen.get(0).id()).getContenido(), 4));
    }

    @Test void rechazaUnArchivoQueNoEsImagenAunqueDigaSerJpg() {
        var s = nueva(Urgencia.MEDIA);
        var falso = new MockMultipartFile("fotos", "virus.jpg", "image/jpeg", "MZ programa.exe".getBytes());
        esperaEstado(HttpStatus.BAD_REQUEST, () -> service.guardarFotos(s.id(), List.of(falso)));
        assertEquals(0, service.obtener(s.id()).fotos());
    }

    @Test void respetaElMaximoDeCuatroFotosPorSolicitud() {
        var s = nueva(Urgencia.MEDIA);
        service.guardarFotos(s.id(), List.of(jpg("1.jpg", 0), jpg("2.jpg", 0), jpg("3.jpg", 0)));
        esperaEstado(HttpStatus.BAD_REQUEST, () -> service.guardarFotos(s.id(), List.of(jpg("4.jpg", 0), jpg("5.jpg", 0))));
        assertEquals(3, service.obtener(s.id()).fotos());        // el envío rechazado no guardó ninguna
        service.guardarFotos(s.id(), List.of(jpg("4.jpg", 0)));
        assertEquals(4, service.obtener(s.id()).fotos());
    }

    @Test void rechazaFotosDeMasDe6MegasYEnviosVacios() {
        var s = nueva(Urgencia.MEDIA);
        esperaEstado(HttpStatus.BAD_REQUEST, () -> service.guardarFotos(s.id(), List.of(jpg("grande.jpg", SolicitudService.MAX_BYTES_FOTO))));
        esperaEstado(HttpStatus.BAD_REQUEST, () -> service.guardarFotos(s.id(),
                List.of(new MockMultipartFile("fotos", "vacia.jpg", "image/jpeg", new byte[0]))));
    }

    @Test void soloSeAgreganFotosMientrasLaSolicitudEsRecienteYNoFueRevisada() {
        var revisada = nueva(Urgencia.MEDIA);
        service.cambiarEstado(revisada.id(), EstadoSolicitud.PENDIENTE_EVALUACION, "admin@sgp.local");
        esperaEstado(HttpStatus.CONFLICT, () -> service.guardarFotos(revisada.id(), List.of(jpg("tarde.jpg", 0))));

        var antigua = nueva(Urgencia.MEDIA);
        var entidad = repositorio.findById(antigua.id()).orElseThrow();
        entidad.setFechaCreacion(LocalDateTime.now().minusMinutes(30));
        repositorio.save(entidad);
        esperaEstado(HttpStatus.CONFLICT, () -> service.guardarFotos(antigua.id(), List.of(jpg("vieja.jpg", 0))));
    }

    @Test void solicitudOFotoInexistenteDan404() {
        esperaEstado(HttpStatus.NOT_FOUND, () -> service.guardarFotos(999_999L, List.of(jpg("x.jpg", 0))));
        esperaEstado(HttpStatus.NOT_FOUND, () -> service.fotos(999_999L));
        var s = nueva(Urgencia.MEDIA);
        esperaEstado(HttpStatus.NOT_FOUND, () -> service.foto(s.id(), 999_999L));
    }

    @Test void unaFotoNoSeVeDesdeLaSolicitudDeOtro() {
        var a = nueva(Urgencia.MEDIA);
        var b = nueva(Urgencia.MEDIA);
        long fotoDeA = service.guardarFotos(a.id(), List.of(jpg("a.jpg", 0))).get(0).id();
        esperaEstado(HttpStatus.NOT_FOUND, () -> service.foto(b.id(), fotoDeA));
    }

    // ---------- Cola de atención ----------

    @Test void laColaOrdenaPorPrioridadYFechaYNoIncluyeCerradas() throws Exception {
        var baja = nueva(Urgencia.BAJA);   Thread.sleep(5);
        var alta1 = nueva(Urgencia.ALTA);  Thread.sleep(5);
        var media = nueva(Urgencia.MEDIA); Thread.sleep(5);
        var alta2 = nueva(Urgencia.ALTA);
        List<Long> nuestras = List.of(baja.id(), alta1.id(), media.id(), alta2.id());

        assertEquals(List.of(alta1.id(), alta2.id(), media.id(), baja.id()), soloNuestras(service.cola(), nuestras));

        service.cambiarEstado(alta1.id(), EstadoSolicitud.CERRADA, "admin@sgp.local");
        assertEquals(List.of(alta2.id(), media.id(), baja.id()), soloNuestras(service.cola(), nuestras));
        // La lista general (sin filtro) sí la incluye, pero al final de las nuestras.
        assertEquals(List.of(alta2.id(), media.id(), baja.id(), alta1.id()), soloNuestras(service.listar(null), nuestras));
    }

    private static List<Long> soloNuestras(List<SolicitudResponse> lista, List<Long> ids) {
        return lista.stream().map(SolicitudResponse::id).filter(ids::contains).toList();
    }

    // ---------- Alertas (pulso) ----------

    @Test void elPulsoCuentaLasSinRevisarYEntregaLasNuevasDespuesDeUnId() {
        var primera = nueva(Urgencia.MEDIA);
        long sinRevisarAntes = service.pulso(null).sinRevisar();
        var segunda = nueva(Urgencia.ALTA);

        var pulso = service.pulso(primera.id());
        assertEquals(sinRevisarAntes + 1, pulso.sinRevisar());
        assertTrue(pulso.ultimoId() >= segunda.id());
        assertTrue(pulso.nuevas().stream().anyMatch(n -> n.id().equals(segunda.id())));
        assertTrue(pulso.nuevas().stream().noneMatch(n -> n.id() <= primera.id()), "solo las posteriores al id pedido");

        assertTrue(service.pulso(null).nuevas().isEmpty(), "sin desdeId no se entregan solicitudes");

        service.cambiarEstado(segunda.id(), EstadoSolicitud.PENDIENTE_EVALUACION, "admin@sgp.local");
        assertEquals(sinRevisarAntes, service.pulso(null).sinRevisar());   // revisarla la saca del contador
    }
}
