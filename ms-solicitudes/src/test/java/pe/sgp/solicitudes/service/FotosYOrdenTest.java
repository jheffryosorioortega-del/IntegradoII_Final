package pe.sgp.solicitudes.service;

import org.junit.jupiter.api.Test;
import pe.sgp.solicitudes.model.EstadoSolicitud;
import pe.sgp.solicitudes.model.Solicitud;
import pe.sgp.solicitudes.model.Urgencia;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Funciones puras de las fotos de evidencia y del orden de atención (sin base de datos ni Spring). */
class FotosYOrdenTest {

    private static byte[] bytes(int... valores) {
        byte[] b = new byte[valores.length];
        for (int i = 0; i < valores.length; i++) b[i] = (byte) valores[i];
        return b;
    }

    // ---------- Tipo de imagen: se reconoce por el contenido, no por el nombre ----------

    @Test void reconoceJpgPngYWebpPorSusPrimerosBytes() {
        assertEquals("image/jpeg", SolicitudService.tipoDeImagen(bytes(0xFF, 0xD8, 0xFF, 0xE0, 0, 16)));
        assertEquals("image/png", SolicitudService.tipoDeImagen(bytes(0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0)));
        assertEquals("image/webp", SolicitudService.tipoDeImagen(
                bytes('R', 'I', 'F', 'F', 1, 2, 3, 4, 'W', 'E', 'B', 'P', 'V', 'P')));
    }

    @Test void rechazaLoQueNoEsUnaImagenAunqueSeLlameJpg() {
        assertNull(SolicitudService.tipoDeImagen("MZ\u0090\u0000 programa.exe".getBytes(StandardCharsets.ISO_8859_1)));
        assertNull(SolicitudService.tipoDeImagen("<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8)));
        assertNull(SolicitudService.tipoDeImagen("GIF89a....".getBytes(StandardCharsets.UTF_8)));   // GIF no está permitido
        assertNull(SolicitudService.tipoDeImagen(new byte[0]));
        assertNull(SolicitudService.tipoDeImagen(bytes(0xFF, 0xD8)));                                // cabecera incompleta
        assertNull(SolicitudService.tipoDeImagen(bytes('R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'A', 'V', 'E')));   // RIFF pero es audio
    }

    // ---------- Nombre del archivo ----------

    @Test void elNombreNoPuedeLlevarCarpetasNiCaracteresPeligrosos() {
        assertEquals("passwd", SolicitudService.nombreSeguro("../../etc/passwd"));
        assertEquals("baño.jpg", SolicitudService.nombreSeguro("C:\\fotos\\baño.jpg"));
        assertEquals("script.jpg", SolicitudService.nombreSeguro("<script>alert(1)</script>.jpg"));   // queda lo de después de la última "/", sin "<" ni ">"
        assertEquals("foto", SolicitudService.nombreSeguro(null));
        assertEquals("foto", SolicitudService.nombreSeguro("///"));
        assertEquals("foto", SolicitudService.nombreSeguro("<>"));
    }

    @Test void elNombreLargoSeRecortaA100ConservandoLaExtension() {
        String largo = "a".repeat(300) + ".png";
        String n = SolicitudService.nombreSeguro(largo);
        assertEquals(100, n.length());
        assertTrue(n.endsWith(".png"));
    }

    // ---------- Orden de atención ----------

    private static Solicitud sol(long id, Urgencia u, EstadoSolicitud e, int minutosAtras) {
        Solicitud s = new Solicitud();
        s.setId(id);
        s.setUrgencia(u);
        s.setEstado(e);
        s.setFechaCreacion(LocalDateTime.of(2026, 11, 5, 12, 0).minusMinutes(minutosAtras));
        return s;
    }

    private static List<Long> orden(Solicitud... lista) {
        List<Solicitud> l = new ArrayList<>(List.of(lista));
        l.sort(SolicitudService.ORDEN_ATENCION);
        return l.stream().map(Solicitud::getId).toList();
    }

    @Test void primeroLaPrioridadMasAlta() {
        var baja = sol(1, Urgencia.BAJA, EstadoSolicitud.REGISTRADA, 300);    // la más antigua pero de prioridad baja
        var media = sol(2, Urgencia.MEDIA, EstadoSolicitud.REGISTRADA, 100);
        var alta = sol(3, Urgencia.ALTA, EstadoSolicitud.REGISTRADA, 1);      // la más reciente pero urgente
        assertEquals(List.of(3L, 2L, 1L), orden(baja, media, alta));
    }

    @Test void conLaMismaPrioridadSeAtiendeLaQueLlegoAntes() {
        var tarde = sol(1, Urgencia.MEDIA, EstadoSolicitud.REGISTRADA, 5);
        var temprano = sol(2, Urgencia.MEDIA, EstadoSolicitud.PENDIENTE_EVALUACION, 50);
        assertEquals(List.of(2L, 1L), orden(tarde, temprano));
    }

    @Test void siLlegaronEnElMismoInstanteDecideElIdMenor() {
        assertEquals(List.of(4L, 9L), orden(sol(9, Urgencia.ALTA, EstadoSolicitud.REGISTRADA, 10), sol(4, Urgencia.ALTA, EstadoSolicitud.REGISTRADA, 10)));
    }

    @Test void lasCerradasVanAlFinalAunqueSeanUrgentes() {
        var cerradaAlta = sol(1, Urgencia.ALTA, EstadoSolicitud.CERRADA, 500);
        var abiertaBaja = sol(2, Urgencia.BAJA, EstadoSolicitud.EN_EJECUCION, 1);
        assertEquals(List.of(2L, 1L), orden(cerradaAlta, abiertaBaja));
    }
}
