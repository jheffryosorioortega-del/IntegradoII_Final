package pe.sgp.frontend.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

/** Si el Gateway o un microservicio no responde (o falla), el usuario ve una pantalla de aviso
 *  amigable en lugar de un error técnico. El detalle real solo queda en el registro del servidor. */
@ControllerAdvice
public class ErroresWeb {

    private static final Logger LOG = LoggerFactory.getLogger(ErroresWeb.class);

    /** Un servicio dijo "no existe" (por ejemplo, cambiar el estado de una solicitud que ya no está). */
    @ExceptionHandler(HttpClientErrorException.NotFound.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String noEncontrado(Model model) {
        model.addAttribute("status", 404);
        return "error";
    }

    /** El Gateway rechazó el JWT (vencido o inválido): se cierra la sesión y se pide iniciar sesión de nuevo. */
    @ExceptionHandler(HttpClientErrorException.Unauthorized.class)
    public String sesionVencida(HttpServletRequest request) {
        LOG.info("El Gateway rechazó el token del usuario; se cierra la sesión.");
        SecurityContextHolder.clearContext();
        HttpSession sesion = request.getSession(false);
        if (sesion != null) sesion.invalidate();
        return "redirect:/login?expirada";
    }

    /** El Gateway dijo que el rol del usuario no alcanza (la pantalla normalmente ya lo impide). */
    @ExceptionHandler(HttpClientErrorException.Forbidden.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String sinPermiso(HttpClientErrorException e, Model model) {
        LOG.warn("El Gateway negó el acceso (403): {}", e.getStatusText());
        model.addAttribute("status", 403);
        return "error";
    }

    @ExceptionHandler({ResourceAccessException.class, HttpServerErrorException.class})
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public String servicioNoDisponible(Exception e, Model model) {
        LOG.warn("Un servicio no está disponible: {}", e.getMessage());
        model.addAttribute("status", 503);
        return "error";
    }
}
