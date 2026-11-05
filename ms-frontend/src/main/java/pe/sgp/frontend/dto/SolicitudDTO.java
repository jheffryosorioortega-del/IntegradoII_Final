package pe.sgp.frontend.dto;

import pe.sgp.frontend.model.CategoriaServicio;
import pe.sgp.frontend.model.EstadoSolicitud;
import pe.sgp.frontend.model.Urgencia;

import java.time.LocalDateTime;
import java.util.List;

/** Forma del JSON que devuelve ms-solicitudes. Nótese: ya NO tiene un objeto
 *  "cliente" anidado como en la versión monolito (allí era un @ManyToOne con JOIN);
 *  aquí vienen clienteNombre y clienteTelefono sueltos, tal como los guardó
 *  ms-solicitudes al momento de crear la solicitud.
 *  fechaCreacion es el momento en que llegó la solicitud: con ella se calcula cuánto lleva esperando.
 *  fotos es cuántas fotos de evidencia adjuntó el cliente.
 *  "siguientes" son las etapas permitidas desde el estado actual: las decide ms-solicitudes y la pantalla solo las ofrece.
 *  tecnicoCorreo/tecnicoNombre son null mientras no haya técnico asignado. */
public record SolicitudDTO(Long id, String codigo, Long clienteId, String clienteNombre,
                           String clienteTelefono, CategoriaServicio categoria, String descripcion,
                           String direccionServicio, Urgencia urgencia, EstadoSolicitud estado, String tipoSolicitante, String ruc, String razonSocial, String direccionFiscal,
                           String personaContacto, String cargo, String correoContacto, boolean requiereFactura,
                           LocalDateTime fechaCreacion, int fotos,
                           List<EstadoSolicitud> siguientes, String tecnicoCorreo, String tecnicoNombre) {

    /** Sin los datos de fotos, etapas siguientes ni técnico (quedan en 0, vacío y null): para quien arma una solicitud de prueba a mano. */
    public SolicitudDTO(Long id, String codigo, Long clienteId, String clienteNombre, String clienteTelefono,
                        CategoriaServicio categoria, String descripcion, String direccionServicio, Urgencia urgencia,
                        EstadoSolicitud estado, String tipoSolicitante, String ruc, String razonSocial, String direccionFiscal,
                        String personaContacto, String cargo, String correoContacto, boolean requiereFactura,
                        LocalDateTime fechaCreacion) {
        this(id, codigo, clienteId, clienteNombre, clienteTelefono, categoria, descripcion, direccionServicio, urgencia,
                estado, tipoSolicitante, ruc, razonSocial, direccionFiscal, personaContacto, cargo, correoContacto,
                requiereFactura, fechaCreacion, 0, List.of(), null, null);
    }

    /** Nombre que se muestra en el panel: la razón social si es empresa, el nombre del cliente si es persona. */
    public String solicitante() {
        if ("EMPRESA".equals(tipoSolicitante) && razonSocial != null && !razonSocial.isBlank()) return razonSocial;
        return clienteNombre == null ? "" : clienteNombre;
    }
}
