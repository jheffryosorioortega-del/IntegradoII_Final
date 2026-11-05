package pe.sgp.frontend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import pe.sgp.frontend.client.SolicitudClient;
import pe.sgp.frontend.dto.SolicitudDTO;
import pe.sgp.frontend.model.CategoriaServicio;
import pe.sgp.frontend.model.EstadoSolicitud;
import pe.sgp.frontend.model.Urgencia;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
public class PanelController {

    /** Cuántas solicitudes se muestran en la cola del tablero (la primera más las siguientes). */
    private static final int TAMANO_COLA = 6;

    /** Una barra del gráfico: etiqueta, cantidad y ancho relativo (0 a 100). */
    public record Barra(String etiqueta, long valor, int pct, boolean tope) {}

    /** Un tramo de la dona de prioridad. */
    public record Tramo(String etiqueta, String clase, long valor) {}

    /** Un puesto de la cola de atención: posición (1 = se atiende ahora) y la solicitud. */
    public record Turno(int posicion, SolicitudDTO s) {}

    private final SolicitudClient solicitudClient;

    public PanelController(SolicitudClient solicitudClient) {
        this.solicitudClient = solicitudClient;
    }

    @GetMapping("/panel")
    public String tablero(Model model) {
        Map<EstadoSolicitud, Long> resumen = solicitudClient.resumen();
        List<SolicitudDTO> todas = solicitudClient.listar(null);
        // Cola de atención: abiertas, ya ordenadas por prioridad y fecha de llegada por ms-solicitudes.
        List<SolicitudDTO> abiertas = solicitudClient.cola();
        List<Turno> cola = IntStream.range(0, Math.min(abiertas.size(), TAMANO_COLA))
                .mapToObj(i -> new Turno(i + 1, abiertas.get(i)))
                .toList();

        long total = resumen.values().stream().mapToLong(Long::longValue).sum();
        long porAtender = resumen.get(EstadoSolicitud.REGISTRADA) + resumen.get(EstadoSolicitud.PENDIENTE_EVALUACION);
        long enCurso = resumen.get(EstadoSolicitud.COTIZADA) + resumen.get(EstadoSolicitud.APROBADA)
                + resumen.get(EstadoSolicitud.PROGRAMADA) + resumen.get(EstadoSolicitud.EN_EJECUCION);
        long cerradas = resumen.get(EstadoSolicitud.CERRADA);

        Map<CategoriaServicio, Long> porCategoria = todas.stream()
                .filter(s -> s.categoria() != null)
                .collect(Collectors.groupingBy(SolicitudDTO::categoria, Collectors.counting()));
        long maximo = porCategoria.values().stream().mapToLong(Long::longValue).max().orElse(0);
        List<Barra> categorias = Arrays.stream(CategoriaServicio.values())
                .map(c -> {
                    long v = porCategoria.getOrDefault(c, 0L);
                    return new Barra(c.getEtiqueta(), v, maximo == 0 ? 0 : (int) Math.round(v * 100.0 / maximo), maximo > 0 && v == maximo);
                })
                .sorted(Comparator.comparingLong(Barra::valor).reversed())
                .toList();

        Map<Urgencia, Long> porPrioridad = todas.stream()
                .filter(s -> s.urgencia() != null)
                .collect(Collectors.groupingBy(SolicitudDTO::urgencia, Collectors.counting()));
        List<Tramo> urgencias = List.of(Urgencia.ALTA, Urgencia.MEDIA, Urgencia.BAJA).stream()
                .map(u -> new Tramo(u.getEtiqueta(), "ur-" + u.name().toLowerCase(), porPrioridad.getOrDefault(u, 0L)))
                .toList();

        String hoy = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale.of("es", "PE")));

        model.addAttribute("resumen", resumen);
        model.addAttribute("cola", cola);
        model.addAttribute("enCola", abiertas.size());
        model.addAttribute("total", total);
        model.addAttribute("porAtender", porAtender);
        model.addAttribute("enCurso", enCurso);
        model.addAttribute("cerradas", cerradas);
        model.addAttribute("categorias", categorias);
        model.addAttribute("urgencias", urgencias);
        model.addAttribute("hoy", Character.toUpperCase(hoy.charAt(0)) + hoy.substring(1));
        return "panel/tablero";
    }
}
