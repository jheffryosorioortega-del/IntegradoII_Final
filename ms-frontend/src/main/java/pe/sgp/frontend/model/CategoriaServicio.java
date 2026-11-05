package pe.sgp.frontend.model;

/** Copia local del enum de ms-solicitudes: el Front-End lo necesita para
 *  pintar el combo del formulario y las tarjetas de servicios. Ver la nota
 *  sobre duplicación de tipos en Rol.java de ms-usuarios. */
public enum CategoriaServicio {
    GASFITERIA("Gasfitería y redes hidráulicas", "Reparación de fugas, reemplazo de tuberías y redes de agua.", "gasfiteria"),
    CISTERNAS("Cisternas y tanques", "Limpieza, desinfección y mantenimiento de cisternas y tanques.", "cisternas"),
    BOMBAS("Bombas y presurizadores", "Instalación y mantenimiento de bombas y sistemas de almacenamiento.", "bombas"),
    AGUA_CALIENTE("Termas y agua caliente", "Instalación y mantenimiento de termas y sistemas de agua caliente.", "agua-caliente"),
    ELECTRICIDAD("Electricidad e iluminación", "Instalaciones eléctricas y trabajos técnicos complementarios.", "electricidad"),
    MANTENIMIENTO("Mantenimiento y desatoros", "Mantenimiento preventivo, desatoros y revisión de accesorios.", "mantenimiento");

    private final String etiqueta;
    private final String descripcion;
    private final String icono;

    CategoriaServicio(String etiqueta, String descripcion, String icono) {
        this.etiqueta = etiqueta;
        this.descripcion = descripcion;
        this.icono = icono;
    }

    public String getEtiqueta() { return etiqueta; }
    public String getDescripcion() { return descripcion; }
    public String getIcono() { return icono; }
}
