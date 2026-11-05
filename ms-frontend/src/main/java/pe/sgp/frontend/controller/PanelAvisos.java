package pe.sgp.frontend.controller;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import pe.sgp.frontend.client.SolicitudClient;
import pe.sgp.frontend.dto.PulsoDTO;

/** Datos que necesitan todas las páginas del panel para el menú: cuántas solicitudes están sin revisar
 *  (el contador rojo) y el id de la última que existe (punto de partida de la alerta de nuevas).
 *  Si ms-solicitudes no responde, el menú se muestra igual, sin contador ni alerta. */
@ControllerAdvice(assignableTypes = {PanelController.class, SolicitudController.class, ClienteController.class})
public class PanelAvisos {

    private final SolicitudClient solicitudClient;

    public PanelAvisos(SolicitudClient solicitudClient) {
        this.solicitudClient = solicitudClient;
    }

    @ModelAttribute
    public void avisos(Model model) {
        try {
            PulsoDTO pulso = solicitudClient.pulso(null);
            if (pulso != null) {
                model.addAttribute("sinRevisar", pulso.sinRevisar());
                model.addAttribute("ultimoId", pulso.ultimoId());
            }
        } catch (Exception e) {
            // Sin datos: no se muestra contador y la alerta queda apagada hasta la próxima página.
        }
    }
}
