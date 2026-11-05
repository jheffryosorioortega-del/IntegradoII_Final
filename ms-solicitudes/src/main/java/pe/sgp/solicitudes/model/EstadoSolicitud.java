package pe.sgp.solicitudes.model;

import java.util.EnumSet;
import java.util.Set;

/** Etapas de una solicitud y a cuáles se puede pasar desde cada una. Esta es la ÚNICA definición del flujo:
 *  el servicio la aplica y la API la entrega en cada solicitud ("siguientes") para que la pantalla ofrezca solo lo permitido.
 *
 *  REGISTRADA → PENDIENTE_EVALUACION → COTIZADA → APROBADA → PROGRAMADA → EN_EJECUCION → CERRADA
 *  Se puede retroceder una etapa donde tiene sentido de negocio (re-evaluar, reprogramar) y cerrar en cualquier momento
 *  (por ejemplo, si el cliente rechaza la cotización). CERRADA es final: no se reabre. */
public enum EstadoSolicitud {
    REGISTRADA("Registrada"),
    PENDIENTE_EVALUACION("Pendiente de evaluación"),
    COTIZADA("Cotizada"),
    APROBADA("Aprobada"),
    PROGRAMADA("Programada"),
    EN_EJECUCION("En ejecución"),
    CERRADA("Cerrada");

    private final String etiqueta;

    EstadoSolicitud(String etiqueta) { this.etiqueta = etiqueta; }

    public String getEtiqueta() { return etiqueta; }

    /** Estados a los que se puede pasar desde este, en el orden del flujo. */
    public Set<EstadoSolicitud> siguientes() {
        return switch (this) {
            case REGISTRADA -> EnumSet.of(PENDIENTE_EVALUACION, CERRADA);
            case PENDIENTE_EVALUACION -> EnumSet.of(COTIZADA, CERRADA);
            case COTIZADA -> EnumSet.of(PENDIENTE_EVALUACION, APROBADA, CERRADA);
            case APROBADA -> EnumSet.of(PROGRAMADA, CERRADA);
            case PROGRAMADA -> EnumSet.of(APROBADA, EN_EJECUCION, CERRADA);
            case EN_EJECUCION -> EnumSet.of(PROGRAMADA, CERRADA);
            case CERRADA -> EnumSet.noneOf(EstadoSolicitud.class);
        };
    }

    public boolean puedePasarA(EstadoSolicitud nuevo) { return siguientes().contains(nuevo); }
}
