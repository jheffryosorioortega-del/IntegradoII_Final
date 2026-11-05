package pe.sgp.frontend.config;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/** Contador de eventos por clave en una ventana de tiempo deslizante ("máx. 8 envíos en 10 minutos por IP").
 *  Vive en memoria: es suficiente para un solo servidor y se reinicia al reiniciar el servicio. La clave la arma
 *  quien llama (por ejemplo "envio:ip:203.0.113.7"). Es seguro entre hilos. */
@Component
public class LimiteIntentos {

    /** Registros guardados por clave como máximo: un atacante no puede inflar la memoria de un solo contador. */
    private static final int MAX_MARCAS_POR_CLAVE = 1_000;
    /** Claves distintas guardadas como máximo; si se pasa, se vacía todo (prefiere memoria acotada a crecer sin fin). */
    private static final int MAX_CLAVES = 50_000;
    private static final long LIMPIEZA_CADA_MS = 60_000;

    private static final class Registro {
        final ArrayDeque<Long> marcas = new ArrayDeque<>();
        long ventanaMs;
    }

    private final LongSupplier reloj;
    private final Map<String, Registro> registros = new ConcurrentHashMap<>();
    private final AtomicLong ultimaLimpieza;

    public LimiteIntentos() {
        this(System::currentTimeMillis);
    }

    /** Para las pruebas: un reloj que se mueve a voluntad. */
    LimiteIntentos(LongSupplier reloj) {
        this.reloj = reloj;
        this.ultimaLimpieza = new AtomicLong(reloj.getAsLong());
    }

    /** Si hay cupo, cuenta este intento y devuelve true; si ya se llegó al tope, no cuenta y devuelve false. */
    public boolean intentar(String clave, int max, Duration ventana) {
        long ahora = reloj.getAsLong();
        long v = ventana.toMillis();
        boolean[] aceptado = {false};
        registros.compute(clave, (k, r) -> {
            if (r == null) r = new Registro();
            r.ventanaMs = v;
            if (contar(r, ahora, v) < max) {
                anotar(r, ahora);
                aceptado[0] = true;
            }
            return r;
        });
        limpiarSiToca(ahora);
        return aceptado[0];
    }

    /** ¿Ya se llegó al tope? No cuenta nada (sirve para decidir antes de dejar pasar una petición). */
    public boolean alcanzado(String clave, int max, Duration ventana) {
        long ahora = reloj.getAsLong();
        long v = ventana.toMillis();
        boolean[] lleno = {false};
        registros.computeIfPresent(clave, (k, r) -> {
            lleno[0] = contar(r, ahora, v) >= max;
            return r;
        });
        return lleno[0];
    }

    /** Anota un evento (por ejemplo, una contraseña errónea) sin comprobar ningún tope. */
    public void registrar(String clave, Duration ventana) {
        long ahora = reloj.getAsLong();
        long v = ventana.toMillis();
        registros.compute(clave, (k, r) -> {
            if (r == null) r = new Registro();
            r.ventanaMs = v;
            contar(r, ahora, v);
            anotar(r, ahora);
            return r;
        });
        limpiarSiToca(ahora);
    }

    /** Borra el contador de una clave (por ejemplo, tras un login correcto). */
    public void reiniciar(String clave) {
        registros.remove(clave);
    }

    // ---- internos ----

    /** Descarta las marcas fuera de la ventana y devuelve cuántas quedan. */
    private static int contar(Registro r, long ahora, long ventanaMs) {
        while (!r.marcas.isEmpty() && ahora - r.marcas.peekFirst() >= ventanaMs) r.marcas.pollFirst();
        return r.marcas.size();
    }

    private static void anotar(Registro r, long ahora) {
        if (r.marcas.size() >= MAX_MARCAS_POR_CLAVE) r.marcas.pollFirst();
        r.marcas.addLast(ahora);
    }

    private void limpiarSiToca(long ahora) {
        long ultima = ultimaLimpieza.get();
        boolean llena = registros.size() > MAX_CLAVES;
        if (!llena && ahora - ultima < LIMPIEZA_CADA_MS) return;
        if (!ultimaLimpieza.compareAndSet(ultima, ahora) && !llena) return;
        for (String clave : registros.keySet()) {
            registros.computeIfPresent(clave, (k, r) -> {
                Long ultimaMarca = r.marcas.peekLast();
                return ultimaMarca == null || ahora - ultimaMarca >= r.ventanaMs ? null : r;
            });
        }
        if (registros.size() > MAX_CLAVES) registros.clear();
    }

    /** Solo para pruebas. */
    int claves() {
        return registros.size();
    }
}
