package pe.sgp.frontend.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class LimiteIntentosTest {

    private final AtomicLong ahora = new AtomicLong(1_000_000);
    private final LimiteIntentos limite = new LimiteIntentos(ahora::get);
    private static final Duration DIEZ_MIN = Duration.ofMinutes(10);

    @Test
    void dejaPasarHastaElTopeYDespuesRechaza() {
        for (int i = 0; i < 3; i++) assertTrue(limite.intentar("a", 3, DIEZ_MIN), "intento " + (i + 1));
        assertFalse(limite.intentar("a", 3, DIEZ_MIN));
        assertFalse(limite.intentar("a", 3, DIEZ_MIN), "un rechazo no gasta cupo ni lo devuelve");
    }

    @Test
    void laVentanaSeDesliza() {
        assertTrue(limite.intentar("a", 1, DIEZ_MIN));
        assertFalse(limite.intentar("a", 1, DIEZ_MIN));
        ahora.addAndGet(Duration.ofMinutes(9).toMillis());
        assertFalse(limite.intentar("a", 1, DIEZ_MIN), "a los 9 minutos todavía cuenta");
        ahora.addAndGet(Duration.ofMinutes(1).toMillis());
        assertTrue(limite.intentar("a", 1, DIEZ_MIN), "a los 10 minutos el intento viejo ya no cuenta");
    }

    @Test
    void cadaClaveTieneSuPropioContador() {
        assertTrue(limite.intentar("ip:1", 1, DIEZ_MIN));
        assertFalse(limite.intentar("ip:1", 1, DIEZ_MIN));
        assertTrue(limite.intentar("ip:2", 1, DIEZ_MIN));
    }

    @Test
    void alcanzadoNoCuentaYRegistrarSi() {
        assertFalse(limite.alcanzado("x", 2, DIEZ_MIN));
        assertFalse(limite.alcanzado("x", 2, DIEZ_MIN), "preguntar no gasta cupo");
        limite.registrar("x", DIEZ_MIN);
        assertFalse(limite.alcanzado("x", 2, DIEZ_MIN));
        limite.registrar("x", DIEZ_MIN);
        assertTrue(limite.alcanzado("x", 2, DIEZ_MIN));
        ahora.addAndGet(DIEZ_MIN.toMillis());
        assertFalse(limite.alcanzado("x", 2, DIEZ_MIN), "pasada la ventana se levanta el bloqueo");
    }

    @Test
    void reiniciarBorraElContador() {
        limite.registrar("x", DIEZ_MIN);
        limite.registrar("x", DIEZ_MIN);
        assertTrue(limite.alcanzado("x", 2, DIEZ_MIN));
        limite.reiniciar("x");
        assertFalse(limite.alcanzado("x", 2, DIEZ_MIN));
    }

    @Test
    void lasClavesVencidasSeLimpianYNoCrecenSinFin() {
        for (int i = 0; i < 100; i++) limite.registrar("ip:" + i, DIEZ_MIN);
        assertEquals(100, limite.claves());
        ahora.addAndGet(Duration.ofMinutes(11).toMillis());
        limite.registrar("otra", DIEZ_MIN);   // dispara la limpieza periódica
        assertEquals(1, limite.claves(), "solo queda la clave viva");
    }

    @Test
    void conVariosHilosNuncaDejaPasarMasDelTope() throws Exception {
        int max = 20;
        var aceptados = new java.util.concurrent.atomic.AtomicInteger();
        var hilos = new Thread[16];
        for (int h = 0; h < hilos.length; h++) {
            hilos[h] = new Thread(() -> {
                for (int i = 0; i < 50; i++) if (limite.intentar("carrera", max, DIEZ_MIN)) aceptados.incrementAndGet();
            });
            hilos[h].start();
        }
        for (Thread t : hilos) t.join();
        assertEquals(max, aceptados.get());
    }
}
