package pe.sgp.solicitudes.dto;

import jakarta.validation.constraints.Size;

/** Cuerpo de PUT /api/solicitudes/{id}/tecnico. Un correo vacío o nulo quita al técnico asignado. */
public record AsignarTecnicoRequest(@Size(max = 120, message = "Correo demasiado largo") String correo) {
}
