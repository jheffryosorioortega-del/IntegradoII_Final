package pe.sgp.solicitudes.dto;

/** Lo que ms-usuarios devuelve en GET /api/usuarios/tecnicos. */
public record TecnicoDTO(Long id, String nombre, String correo) {
}
