package pe.sgp.frontend.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.sgp.frontend.client.SolicitudClient;
import pe.sgp.frontend.client.UsuarioClient;
import pe.sgp.frontend.dto.FotoDTO;
import pe.sgp.frontend.dto.SolicitudDTO;
import pe.sgp.frontend.model.EstadoSolicitud;

import java.util.List;

@Controller
public class SolicitudController {

    private final SolicitudClient solicitudClient;
    private final UsuarioClient usuarioClient;

    public SolicitudController(SolicitudClient solicitudClient, UsuarioClient usuarioClient) {
        this.solicitudClient = solicitudClient;
        this.usuarioClient = usuarioClient;
    }

    /** Lista en orden de atención: prioridad y, a igual prioridad, la que llegó antes. */
    @GetMapping("/panel/solicitudes")
    public String listar(@RequestParam(required = false) EstadoSolicitud estado, Model model) {
        model.addAttribute("solicitudes", solicitudClient.listar(estado));
        model.addAttribute("estados", EstadoSolicitud.values());
        model.addAttribute("filtro", estado);
        return "panel/solicitudes";
    }

    /** Ficha de una solicitud: datos, fotos del cliente, técnico asignado, cambio de etapa e historial.
     *  La lista de técnicos solo se pide si el usuario puede asignarlos (el Gateway se la negaría a un Técnico). */
    @GetMapping("/panel/solicitudes/{id}")
    public String detalle(@PathVariable Long id, Model model, HttpServletRequest request) {
        SolicitudDTO solicitud;
        try {
            solicitud = solicitudClient.obtener(id);
        } catch (HttpClientErrorException.NotFound e) {
            return "redirect:/panel/solicitudes";
        }
        boolean gestiona = request.isUserInRole("ADMINISTRADOR") || request.isUserInRole("COORDINADOR");
        model.addAttribute("s", solicitud);
        model.addAttribute("fotos", fotosDe(id, solicitud));
        model.addAttribute("historial", solicitudClient.historial(id));
        model.addAttribute("tecnicos", gestiona ? usuarioClient.tecnicos() : List.of());
        return "panel/solicitud-detalle";
    }

    /** Las fotos de la ficha. Si no se pueden cargar, la ficha se muestra igual, sin fotos. */
    private List<FotoDTO> fotosDe(Long id, SolicitudDTO solicitud) {
        if (solicitud.fotos() == 0) return List.of();
        try {
            return solicitudClient.fotos(id);
        } catch (Exception e) {
            return List.of();
        }
    }

    /** {@code volver=detalle} deja al usuario en la ficha, {@code volver=tablero} en el tablero; cualquier otro valor, en la lista. */
    @PostMapping("/panel/solicitudes/{id}/estado")
    public String cambiarEstado(@PathVariable Long id, @RequestParam EstadoSolicitud estado,
                                @RequestParam(required = false) String volver, RedirectAttributes flash) {
        try {
            solicitudClient.cambiarEstado(id, estado);
            flash.addFlashAttribute("ok", "Estado actualizado.");
        } catch (HttpClientErrorException.Conflict e) {
            // El flujo no permite ese paso, o falta técnico: el mensaje lo redactó ms-solicitudes.
            flash.addFlashAttribute("error", ApiErrores.mensaje(e));
        }
        if ("detalle".equals(volver)) return "redirect:/panel/solicitudes/" + id;
        if ("tablero".equals(volver)) return "redirect:/panel";
        return "redirect:/panel/solicitudes";
    }

    @PostMapping("/panel/solicitudes/{id}/tecnico")
    public String asignarTecnico(@PathVariable Long id, @RequestParam(defaultValue = "") String correo,
                                 RedirectAttributes flash) {
        try {
            solicitudClient.asignarTecnico(id, correo);
            flash.addFlashAttribute("ok", correo.isBlank() ? "Se quitó el técnico." : "Técnico asignado.");
        } catch (HttpClientErrorException.Conflict | HttpClientErrorException.BadRequest e) {
            flash.addFlashAttribute("error", ApiErrores.mensaje(e));
        }
        return "redirect:/panel/solicitudes/" + id;
    }
}
