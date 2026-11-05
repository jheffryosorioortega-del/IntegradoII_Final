package pe.sgp.solicitudes.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Una foto que el cliente adjuntó a su solicitud, para que se pueda evaluar qué tan urgente es la visita.
 *  Igual que en Solicitud, solicitudId es solo un número de referencia (sin objeto anidado).
 *  El esquema real lo define Flyway (V4__fotos_solicitud.sql). */
@Entity
@Table(name = "solicitud_foto")
public class FotoSolicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long solicitudId;

    @Column(nullable = false, length = 100)
    private String nombre;

    /** Siempre image/jpeg, image/png o image/webp: se comprueba por el contenido, no por el nombre. */
    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(nullable = false)
    private int tamano;

    @Column(nullable = false)
    private byte[] contenido;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    public Long getId() { return id; }
    public Long getSolicitudId() { return solicitudId; }
    public void setSolicitudId(Long solicitudId) { this.solicitudId = solicitudId; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public int getTamano() { return tamano; }
    public void setTamano(int tamano) { this.tamano = tamano; }
    public byte[] getContenido() { return contenido; }
    public void setContenido(byte[] contenido) { this.contenido = contenido; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
