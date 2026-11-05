package pe.sgp.solicitudes.dto;

/** Datos de una foto sin su contenido: lo que se necesita para listarla. El contenido se pide aparte. */
public record FotoResumen(Long id, String nombre, String tipo, int tamano) {
}
