package pe.sgp.solicitudes.dto;

import jakarta.validation.constraints.*;
import pe.sgp.solicitudes.model.CategoriaServicio;
import pe.sgp.solicitudes.model.Urgencia;
import pe.sgp.solicitudes.validation.Reglas;

/** Lo que el Front-End envía por POST /api/solicitudes cuando alguien llena
 *  el formulario público "Solicita tu servicio" (HU01).
 *  Los setters normalizan el texto antes de validar (recortan y colapsan espacios). */
@pe.sgp.solicitudes.validation.DatosEmpresaValidos
public class SolicitudPublicaRequest {

    @NotBlank(message = "Indica tu nombre")
    @Pattern(regexp = Reglas.NOMBRE, message = Reglas.MSG_NOMBRE)
    private String nombre;

    @NotBlank(message = "Indica un teléfono de contacto")
    @Pattern(regexp = Reglas.TELEFONO, message = Reglas.MSG_TELEFONO)
    private String telefono;

    @Size(max = 120, message = "El correo no puede superar 120 caracteres")
    @Pattern(regexp = Reglas.CORREO, message = Reglas.MSG_CORREO)
    private String correo;

    @NotBlank(message = "Indica la dirección del servicio")
    @Pattern(regexp = Reglas.DIRECCION, message = Reglas.MSG_DIRECCION)
    private String direccion;

    @Pattern(regexp = Reglas.DISTRITO, message = Reglas.MSG_DISTRITO)
    private String distrito;

    @NotNull(message = "Selecciona el tipo de servicio")
    private CategoriaServicio categoria;

    @NotBlank(message = "Describe brevemente el problema")
    @Size(min = 10, max = 600, message = "Describe el problema con 10 a 600 caracteres")
    @Pattern(regexp = Reglas.DESCRIPCION, message = Reglas.MSG_DESCRIPCION)
    private String descripcion;

    @NotNull(message = "Selecciona la prioridad")
    private Urgencia urgencia = Urgencia.MEDIA;


    @NotBlank
    @Pattern(regexp = "PERSONA|EMPRESA", message = "Selecciona persona natural o empresa")
    private String tipoSolicitante = "PERSONA";
    private String ruc = null;
    private String razonSocial = null;
    private String direccionFiscal = null;
    private String cargo = null;
    private boolean requiereFactura = false;
    public String getTipoSolicitante() { return tipoSolicitante; }
    public void setTipoSolicitante(String tipoSolicitante) { this.tipoSolicitante = Reglas.limpiar(tipoSolicitante); }
    public String getRuc() { return ruc; }
    public void setRuc(String ruc) { this.ruc = Reglas.limpiar(ruc); }
    public String getRazonSocial() { return razonSocial; }
    public void setRazonSocial(String razonSocial) { this.razonSocial = Reglas.limpiar(razonSocial); }
    public String getDireccionFiscal() { return direccionFiscal; }
    public void setDireccionFiscal(String direccionFiscal) { this.direccionFiscal = Reglas.limpiar(direccionFiscal); }
    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = Reglas.limpiar(cargo); }
    public boolean isRequiereFactura() { return requiereFactura; }
    public void setRequiereFactura(boolean requiereFactura) { this.requiereFactura = requiereFactura; }

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
    public CategoriaServicio getCategoria() { return categoria; }
    public void setCategoria(CategoriaServicio categoria) { this.categoria = categoria; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = Reglas.limpiar(descripcion); }
    public Urgencia getUrgencia() { return urgencia; }
    public void setUrgencia(Urgencia urgencia) { this.urgencia = urgencia; }
}
