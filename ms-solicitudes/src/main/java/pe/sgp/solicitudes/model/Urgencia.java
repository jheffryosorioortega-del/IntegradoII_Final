package pe.sgp.solicitudes.model;

public enum Urgencia {
    BAJA("Baja"), MEDIA("Media"), ALTA("Alta");

    private final String etiqueta;

    Urgencia(String etiqueta) { this.etiqueta = etiqueta; }

    public String getEtiqueta() { return etiqueta; }
}
