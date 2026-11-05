package pe.sgp.frontend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pe.sgp.frontend.validation.Reglas;

/** Formulario "Nuevo cliente" del panel (RF02). Las mismas reglas que valida ms-clientes. */
public class ClienteForm {

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
