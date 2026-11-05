package pe.sgp.solicitudes.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

/** Respuestas de error con un formato fijo: {"mensaje": "...", "errores": {"campo": "mensaje"}}.
 *  ms-frontend lee "errores" para pintar cada mensaje junto a su campo del formulario. */
@RestControllerAdvice
public class ErroresApi {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> datosInvalidos(MethodArgumentNotValidException e) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError fe : e.getBindingResult().getFieldErrors()) {
            errores.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("mensaje", "Hay datos que no son válidos.");
        cuerpo.put("errores", errores);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(cuerpo);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> cuerpoIlegible(HttpMessageNotReadableException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("mensaje", "El cuerpo de la petición no es válido."));
    }

    /** ms-clientes rechazó los datos (por ejemplo, un teléfono inválido que se coló): se informa como 400, no como 500. */
    @ExceptionHandler(HttpClientErrorException.BadRequest.class)
    public ResponseEntity<Map<String, Object>> clientesRechazo(HttpClientErrorException.BadRequest e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("mensaje", "El servicio de clientes rechazó los datos del cliente."));
    }

    /** ms-clientes o ms-usuarios no responde: se informa 503 (servicio no disponible) en lugar de un error interno. */
    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<Map<String, Object>> servicioCaido(ResourceAccessException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("mensaje", "Un servicio necesario no está disponible en este momento."));
    }

    /** Reglas de negocio (409 flujo de estados o técnico, 404 no existe, 400 técnico inválido): el mensaje llega a la pantalla. */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> reglaDeNegocio(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("mensaje", String.valueOf(e.getReason())));
    }
}
