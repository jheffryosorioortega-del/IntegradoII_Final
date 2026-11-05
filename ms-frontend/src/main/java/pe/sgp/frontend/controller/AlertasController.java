package pe.sgp.frontend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.sgp.frontend.client.SolicitudClient;
import pe.sgp.frontend.dto.PulsoDTO;
import pe.sgp.frontend.dto.SolicitudDTO;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Alerta de solicitudes nuevas. El script del panel (alertas.js) llama a esta dirección cada 15 segundos
 *  con el id de la última solicitud que ya conoce; aquí se responde qué llegó después. */
@RestController
public class AlertasController {

    private final SolicitudClient solicitudClient;

    public AlertasController(SolicitudClient solicitudClient) {
        this.solicitudClient = solicitudClient;
    }

    @GetMapping("/panel/alertas")
    public Map<String, Object> alertas(@RequestParam Long desde) {
        PulsoDTO pulso = solicitudClient.pulso(desde);
        List<Map<String, Object>> nuevas = pulso.nuevas().stream().map(AlertasController::aviso).toList();
        Map<String, Object> salida = new LinkedHashMap<>();
        salida.put("sinRevisar", pulso.sinRevisar());
        salida.put("ultimoId", pulso.ultimoId());
        salida.put("nuevas", nuevas);
        return salida;
    }

    /** Solo lo necesario para dibujar el aviso: qué llegó, de quién, qué servicio y con qué prioridad. */
    private static Map<String, Object> aviso(SolicitudDTO s) {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("id", s.id());
        a.put("codigo", s.codigo() == null ? "" : s.codigo());
        a.put("solicitante", s.solicitante());
        a.put("servicio", s.categoria() == null ? "" : s.categoria().getEtiqueta());
        a.put("prioridad", s.urgencia() == null ? "" : s.urgencia().getEtiqueta());
        a.put("clase", s.urgencia() == null ? "ur-media" : "ur-" + s.urgencia().name().toLowerCase());
        return a;
    }
}
