package pe.sgp.solicitudes.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/** Tabla SOLICITUD: vive solo en la base de datos de ms-solicitudes (PostgreSQL).
 *  El esquema real lo define Flyway (db/migration); las anotaciones deben coincidir con él.
 *
 *  OJO: no hay una columna "cliente_id" con clave foránea hacia la tabla CLIENTE,
 *  porque esa tabla está en OTRA base de datos (la de ms-clientes, que ni siquiera es el mismo motor)
 *  y una base de datos no puede hacer un JOIN con otra. En su lugar:
 *   1) guardamos clienteId solo como un número de referencia, sin FK real;
 *   2) duplicamos (clienteNombre, clienteTelefono) en el momento de crear la
 *      solicitud, para poder listar sin tener que llamar a ms-clientes cada vez.
 *  Esto se llama "duplicación de datos para lectura" y es normal en microservicios;
 *  el precio es que si el cliente cambia de nombre después, esta copia queda desactualizada. */
@Entity
public class Solicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 20)
    private String codigo;

    @Column(nullable = false)
    private Long clienteId;

    @Column(nullable = false, length = 60)
    private String clienteNombre;

    @Column(nullable = false, length = 9)
    private String clienteTelefono;

    /** Los enumerados se guardan como texto (VARCHAR); así el mismo código es portable. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private CategoriaServicio categoria;

    @Column(length = 600)
    private String descripcion;

    @Column(length = 120)
    private String direccionServicio;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private Urgencia urgencia = Urgencia.MEDIA;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 25)
    private EstadoSolicitud estado = EstadoSolicitud.REGISTRADA;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();


    // Datos históricos de la empresa y su contacto para esta solicitud.
    @Column(length = 10, nullable = false)
    private String tipoSolicitante = "PERSONA";
    @Column(length = 11)
    private String ruc = null;
    @Column(length = 250)
    @org.hibernate.annotations.Nationalized
    private String razonSocial = null;
    @Column(length = 300)
    @org.hibernate.annotations.Nationalized
    private String direccionFiscal = null;
    @Column(length = 80)
    @org.hibernate.annotations.Nationalized
    private String cargo = null;
    @Column(nullable = false)
    private boolean requiereFactura = false;
    @Column(length = 60)
    @org.hibernate.annotations.Nationalized
    private String personaContacto = null;
    @Column(length = 120)
    @org.hibernate.annotations.Nationalized
    private String correoContacto = null;

    /** Técnico asignado: copia del usuario de ms-usuarios (otra base de datos, sin clave foránea). Null = sin asignar. */
    @Column(length = 120)
    private String tecnicoCorreo = null;
    @Column(length = 60)
    private String tecnicoNombre = null;
    public String getTecnicoCorreo() { return tecnicoCorreo; }
    public void setTecnicoCorreo(String tecnicoCorreo) { this.tecnicoCorreo = tecnicoCorreo; }
    public String getTecnicoNombre() { return tecnicoNombre; }
    public void setTecnicoNombre(String tecnicoNombre) { this.tecnicoNombre = tecnicoNombre; }

    public String getTipoSolicitante() { return tipoSolicitante; }
    public void setTipoSolicitante(String tipoSolicitante) { this.tipoSolicitante = tipoSolicitante; }
    public String getRuc() { return ruc; }
    public void setRuc(String ruc) { this.ruc = ruc; }
    public String getRazonSocial() { return razonSocial; }
    public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }
    public String getDireccionFiscal() { return direccionFiscal; }
    public void setDireccionFiscal(String direccionFiscal) { this.direccionFiscal = direccionFiscal; }
    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = cargo; }
    public boolean isRequiereFactura() { return requiereFactura; }
    public void setRequiereFactura(boolean requiereFactura) { this.requiereFactura = requiereFactura; }
    public String getPersonaContacto() { return personaContacto; }
    public void setPersonaContacto(String personaContacto) { this.personaContacto = personaContacto; }
    public String getCorreoContacto() { return correoContacto; }
    public void setCorreoContacto(String correoContacto) { this.correoContacto = correoContacto; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }
    public String getClienteNombre() { return clienteNombre; }
    public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }
    public String getClienteTelefono() { return clienteTelefono; }
    public void setClienteTelefono(String clienteTelefono) { this.clienteTelefono = clienteTelefono; }
    public CategoriaServicio getCategoria() { return categoria; }
    public void setCategoria(CategoriaServicio categoria) { this.categoria = categoria; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getDireccionServicio() { return direccionServicio; }
    public void setDireccionServicio(String direccionServicio) { this.direccionServicio = direccionServicio; }
    public Urgencia getUrgencia() { return urgencia; }
    public void setUrgencia(Urgencia urgencia) { this.urgencia = urgencia; }
    public EstadoSolicitud getEstado() { return estado; }
    public void setEstado(EstadoSolicitud estado) { this.estado = estado; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
