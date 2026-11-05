package pe.sgp.solicitudes.dto;

import java.util.List;

/** Resumen liviano que el panel consulta cada pocos segundos para avisar de solicitudes nuevas.
 *  sinRevisar: cuántas están en estado REGISTRADA (llegaron y nadie las abrió todavía).
 *  ultimoId: el id de la solicitud más reciente.
 *  nuevas: las que llegaron después de "desdeId" (vacío si no se pidió). */
public record PulsoResponse(long sinRevisar, long ultimoId, List<SolicitudResponse> nuevas) {
}
