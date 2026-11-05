package pe.sgp.solicitudes.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/** Una línea del historial de una solicitud: quién hizo qué y cuándo. Solo se agregan líneas, nunca se editan.
 *  anterior/nuevo guardan el estado (nombre del enumerado) o el nombre del técnico, según la acción. */
@Entity
@Table(name = "solicitud_historial")
public class SolicitudHistorial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long solicitudId;

    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    /** Correo del usuario que hizo el cambio ("portal" si lo creó el cliente, "sistema" si es anterior al historial). */
    @Column(nullable = false, length = 120)
    private String usuario;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private AccionHistorial accion;

    @Column(length = 100)
    private String anterior;

    @Column(length = 100)
    private String nuevo;

    protected SolicitudHistorial() { }

    public SolicitudHistorial(Long solicitudId, String usuario, AccionHistorial accion, String anterior, String nuevo) {
        this.solicitudId = solicitudId;
        this.usuario = usuario;
        this.accion = accion;
        this.anterior = anterior;
        this.nuevo = nuevo;
    }

    public Long getId() { return id; }
    public Long getSolicitudId() { return solicitudId; }
    public LocalDateTime getFecha() { return fecha; }
    public String getUsuario() { return usuario; }
    public AccionHistorial getAccion() { return accion; }
    public String getAnterior() { return anterior; }
    public String getNuevo() { return nuevo; }
}
