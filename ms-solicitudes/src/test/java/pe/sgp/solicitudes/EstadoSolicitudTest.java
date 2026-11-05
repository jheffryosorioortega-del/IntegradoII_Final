package pe.sgp.solicitudes;

import org.junit.jupiter.api.Test;
import pe.sgp.solicitudes.model.EstadoSolicitud;

import static org.junit.jupiter.api.Assertions.*;
import static pe.sgp.solicitudes.model.EstadoSolicitud.*;

class EstadoSolicitudTest {

    @Test void elFlujoNormalAvanzaUnaEtapaPorVez() {
        EstadoSolicitud[] flujo = {REGISTRADA, PENDIENTE_EVALUACION, COTIZADA, APROBADA, PROGRAMADA, EN_EJECUCION, CERRADA};
        for (int i = 0; i < flujo.length - 1; i++) {
            assertTrue(flujo[i].puedePasarA(flujo[i + 1]), flujo[i] + " debe poder pasar a " + flujo[i + 1]);
        }
    }

    @Test void noSePuedeSaltarEtapas() {
        assertFalse(REGISTRADA.puedePasarA(COTIZADA));
        assertFalse(REGISTRADA.puedePasarA(EN_EJECUCION));
        assertFalse(PENDIENTE_EVALUACION.puedePasarA(PROGRAMADA));
        assertFalse(APROBADA.puedePasarA(EN_EJECUCION));
    }

    @Test void seCierraEnCualquierMomentoPeroNoSeReabre() {
        for (EstadoSolicitud e : EstadoSolicitud.values()) {
            if (e != CERRADA) assertTrue(e.puedePasarA(CERRADA), e + " debe poder cerrarse");
        }
        assertTrue(CERRADA.siguientes().isEmpty());
        for (EstadoSolicitud e : EstadoSolicitud.values()) assertFalse(CERRADA.puedePasarA(e));
    }

    @Test void soloSeRetrocedeDondeTieneSentido() {
        assertTrue(COTIZADA.puedePasarA(PENDIENTE_EVALUACION));   // volver a evaluar
        assertTrue(PROGRAMADA.puedePasarA(APROBADA));             // reprogramar
        assertTrue(EN_EJECUCION.puedePasarA(PROGRAMADA));
        assertFalse(APROBADA.puedePasarA(COTIZADA));
        assertFalse(PENDIENTE_EVALUACION.puedePasarA(REGISTRADA));
    }

    @Test void unEstadoNoEsSiguienteDeSiMismo() {
        for (EstadoSolicitud e : EstadoSolicitud.values()) assertFalse(e.puedePasarA(e));
    }
}
