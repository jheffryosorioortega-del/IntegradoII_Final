package pe.sgp.frontend.controller;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import pe.sgp.frontend.dto.SolicitudPublicaForm;
import pe.sgp.frontend.model.CategoriaServicio;
import pe.sgp.frontend.model.Urgencia;

/** Si el envío supera el tamaño máximo (por ejemplo, fotos muy pesadas), el servidor lo rechaza antes de llegar
 *  al formulario. Aquí se devuelve al usuario a la misma pantalla con un mensaje claro en lugar de un error técnico. */
@ControllerAdvice
public class ErroresFotos {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String demasiadoPesado(Model model) {
        model.addAttribute("form", new SolicitudPublicaForm());
        model.addAttribute("errorFotos", "Las fotos pesan demasiado. Elige fotos más livianas (hasta 6 MB cada una) y vuelve a enviar.");
        model.addAttribute("categorias", CategoriaServicio.values());
        model.addAttribute("urgencias", Urgencia.values());
        return "portal/contacto";
    }
}
