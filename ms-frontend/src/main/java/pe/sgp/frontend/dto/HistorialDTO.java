package pe.sgp.frontend.dto;

import java.time.LocalDateTime;

/** Una línea del historial de una solicitud; "descripcion" ya viene redactada por ms-solicitudes. */
public record HistorialDTO(LocalDateTime fecha, String usuario, String accion, String descripcion) {
}
