package pe.sgp.solicitudes.dto;

import java.time.LocalDateTime;

/** Una línea del historial lista para mostrar: "descripcion" ya viene redactada en español. */
public record HistorialResponse(LocalDateTime fecha, String usuario, String accion, String descripcion) {
}
