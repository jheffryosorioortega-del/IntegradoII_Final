package pe.sgp.solicitudes.dto;

import pe.sgp.solicitudes.model.CategoriaServicio;
import pe.sgp.solicitudes.model.EstadoSolicitud;
import pe.sgp.solicitudes.model.Urgencia;

import java.time.LocalDateTime;
import java.util.List;

/** "siguientes" son las etapas a las que se puede pasar desde el estado actual (la pantalla solo ofrece esas).
 *  tecnicoCorreo/tecnicoNombre son null mientras no haya técnico asignado.
 *  fotos = cuántas fotos de evidencia adjuntó el cliente (el contenido se pide aparte). */
public record SolicitudResponse(Long id, String codigo, Long clienteId, String clienteNombre,
                                String clienteTelefono, CategoriaServicio categoria, String descripcion,
                                String direccionServicio, Urgencia urgencia, EstadoSolicitud estado,
                                LocalDateTime fechaCreacion, String tipoSolicitante, String ruc, String razonSocial, String direccionFiscal,
                                String personaContacto, String cargo, String correoContacto, boolean requiereFactura,
                                List<EstadoSolicitud> siguientes, String tecnicoCorreo, String tecnicoNombre, int fotos) {
}
