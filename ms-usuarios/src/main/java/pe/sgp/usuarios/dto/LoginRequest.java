package pe.sgp.usuarios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Lo que el Front-End envía por POST /api/usuarios/login. */
public class LoginRequest {

    @NotBlank
    @Size(max = 120)
    @Pattern(regexp = "^[A-Za-z0-9._%+\\-]{1,64}@[A-Za-z0-9.\\-]{1,100}\\.[A-Za-z]{2,}$",
             message = "Correo no válido")
    private String correo;

    @NotBlank
    @Size(max = 100)
    private String contrasena;

    public String getCorreo() { return correo == null ? null : correo.trim(); }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getContrasena() { return contrasena; }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }
}
