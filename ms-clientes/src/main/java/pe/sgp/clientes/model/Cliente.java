package pe.sgp.clientes.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import pe.sgp.clientes.validation.Reglas;

import java.time.LocalDateTime;

/** Tabla CLIENTE: vive solo en la base de datos de ms-clientes (PostgreSQL).
 *  ms-solicitudes NO tiene acceso a esta tabla; si necesita datos de un cliente, se los pide
 *  a este microservicio por REST (ver ClienteClient). El esquema real lo define Flyway
 *  (db/migration); las longitudes de abajo deben coincidir con él.
 *  Las anotaciones de validación son la última defensa: Hibernate las revisa antes de guardar. */
@Entity
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Pattern(regexp = Reglas.NOMBRE, message = Reglas.MSG_NOMBRE)
    @Column(nullable = false, length = 60)
    private String nombre;

    @NotBlank
    @Pattern(regexp = Reglas.TELEFONO, message = Reglas.MSG_TELEFONO)
    @Column(nullable = false, unique = true, length = 9)
    private String telefono;

    @Pattern(regexp = Reglas.CORREO, message = Reglas.MSG_CORREO)
    @Column(length = 120)
    private String correo;

    @Pattern(regexp = "^$|" + Reglas.DIRECCION, message = Reglas.MSG_DIRECCION)
    @Column(length = 120)
    private String direccion;

    @Pattern(regexp = Reglas.DISTRITO, message = Reglas.MSG_DISTRITO)
    @Column(length = 40)
    private String distrito;

    @Column(nullable = false)
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getDistrito() { return distrito; }
    public void setDistrito(String distrito) { this.distrito = distrito; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
}
