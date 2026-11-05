package pe.sgp.frontend.model;

public enum EstadoSolicitud {
    REGISTRADA("Registrada", "text-bg-secondary"),
    PENDIENTE_EVALUACION("Pendiente de evaluación", "text-bg-warning"),
    COTIZADA("Cotizada", "text-bg-info"),
    APROBADA("Aprobada", "text-bg-primary"),
    PROGRAMADA("Programada", "text-bg-primary"),
    EN_EJECUCION("En ejecución", "text-bg-warning"),
    CERRADA("Cerrada", "text-bg-success");

    private final String etiqueta;
    /** Clase de Bootstrap para la insignia de color del estado. */
    private final String clase;

    EstadoSolicitud(String etiqueta, String clase) {
        this.etiqueta = etiqueta;
        this.clase = clase;
    }

    public String getEtiqueta() { return etiqueta; }
    public String getClase() { return clase; }
}
