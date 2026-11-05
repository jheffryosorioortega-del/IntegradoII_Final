package pe.sgp.frontend.dto;

/** Una foto de evidencia de una solicitud (sin su contenido, que se pide aparte). */
public record FotoDTO(Long id, String nombre, String tipo, int tamano) {
}
