package pe.sgp.clientes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pe.sgp.clientes.validation.Reglas;

/** Datos de entrada para crear un cliente, ya sea desde el panel (ms-frontend)
 *  o desde ms-solicitudes cuando llega una solicitud de un cliente nuevo.
 *  Los setters normalizan el texto antes de validar (recortan y colapsan espacios). */
public class ClienteRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Pattern(regexp = Reglas.NOMBRE, message = Reglas.MSG_NOMBRE)
    private String nombre;

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = Reglas.TELEFONO, message = Reglas.MSG_TELEFONO)
    private String telefono;

    @Size(max = 120, message = "El correo no puede superar 120 caracteres")
    @Pattern(regexp = Reglas.CORREO, message = Reglas.MSG_CORREO)
    private String correo;

    @Pattern(regexp = "^$|" + Reglas.DIRECCION, message = Reglas.MSG_DIRECCION)
    private String direccion;

    @Pattern(regexp = Reglas.DISTRITO, message = Reglas.MSG_DISTRITO)
    private String distrito;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = Reglas.limpiar(nombre); }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = Reglas.limpiarTelefono(telefono); }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = Reglas.limpiar(correo); }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = Reglas.limpiar(direccion); }
    public String getDistrito() { return distrito; }
    public void setDistrito(String distrito) { this.distrito = Reglas.limpiar(distrito); }
}
