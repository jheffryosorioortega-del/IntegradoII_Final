package pe.sgp.frontend.validation;

import org.springframework.stereotype.Component;

/** Expone las reglas a las plantillas Thymeleaf como ${@reglas.nombre}, para usarlas en el atributo
 *  "pattern" de los campos. Así la regla del HTML y la del servidor salen de un solo lugar. */
@Component("reglas")
public class ReglasWeb {

    public String getNombre() { return Reglas.NOMBRE; }
    public String getTelefono() { return Reglas.TELEFONO; }
    public String getDireccion() { return Reglas.DIRECCION; }
    public String getDistrito() { return Reglas.DISTRITO; }
    public String getCorreo() { return Reglas.CORREO; }

    public String getMsgNombre() { return Reglas.MSG_NOMBRE; }
    public String getMsgTelefono() { return Reglas.MSG_TELEFONO; }
    public String getMsgDireccion() { return Reglas.MSG_DIRECCION; }
    public String getMsgDistrito() { return Reglas.MSG_DISTRITO; }
    public String getMsgCorreo() { return Reglas.MSG_CORREO; }
    public String getMsgDescripcion() { return Reglas.MSG_DESCRIPCION; }
}
