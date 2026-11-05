package pe.sgp.frontend.dto;

import java.util.List;

/** Respuesta de ms-solicitudes a "¿llegó algo nuevo?": cuántas solicitudes están sin revisar,
 *  el id de la más reciente y las que llegaron después de la última que el panel conocía. */
public record PulsoDTO(long sinRevisar, long ultimoId, List<SolicitudDTO> nuevas) {
}
