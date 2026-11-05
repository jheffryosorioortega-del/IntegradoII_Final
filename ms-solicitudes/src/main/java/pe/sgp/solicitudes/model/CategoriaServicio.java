package pe.sgp.solicitudes.model;

/** Servicios que ofrece SGP. Este enum también existe en ms-frontend (cada
 *  microservicio es dueño de sus propios tipos; ver la nota en Rol.java de ms-usuarios). */
public enum CategoriaServicio {
    GASFITERIA("Gasfitería y redes hidráulicas"),
    CISTERNAS("Cisternas y tanques"),
    BOMBAS("Bombas y presurizadores"),
    AGUA_CALIENTE("Termas y agua caliente"),
    ELECTRICIDAD("Electricidad e iluminación"),
    MANTENIMIENTO("Mantenimiento y desatoros");

    private final String etiqueta;

    CategoriaServicio(String etiqueta) { this.etiqueta = etiqueta; }

    public String getEtiqueta() { return etiqueta; }
}
