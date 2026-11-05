package pe.sgp.frontend.model;

public enum Urgencia {
    BAJA("Baja", "text-bg-success"),
    MEDIA("Media", "text-bg-warning"),
    ALTA("Alta", "text-bg-danger");

    private final String etiqueta;
    /** Clase de Bootstrap para la insignia de color de la urgencia. */
    private final String clase;

    Urgencia(String etiqueta, String clase) {
        this.etiqueta = etiqueta;
        this.clase = clase;
    }

    public String getEtiqueta() { return etiqueta; }
    public String getClase() { return clase; }
}
