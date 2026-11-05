package pe.sgp.frontend.util;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Textos de fecha y espera para el panel. Las plantillas lo llaman como ${@tiempo.hace(s.fechaCreacion)}. */
@Component("tiempo")
public class Tiempo {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Cuánto lleva esperando: "Recién llegó", "hace 12 min", "hace 3 h" o "hace 2 d". */
    public String hace(LocalDateTime desde) {
        if (desde == null) return "";
        long min = Math.max(0, Duration.between(desde, LocalDateTime.now()).toMinutes());
        if (min < 1) return "Recién llegó";
        if (min < 60) return "hace " + min + " min";
        long horas = min / 60;
        if (horas < 24) return "hace " + horas + " h";
        return "hace " + (horas / 24) + " d";
    }

    /** Fecha y hora de llegada: "04/11/2026 22:15". */
    public String fecha(LocalDateTime f) {
        return f == null ? "" : f.format(FECHA);
    }
}
