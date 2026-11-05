package pe.sgp.frontend.config;

import java.io.Serializable;
import java.security.Principal;

/** El usuario con sesión iniciada. Guarda el JWT que firmó ms-usuarios para reenviarlo al Gateway en cada llamada
 *  (ver gatewayRestClient). El token vive solo en la sesión del servidor: nunca se envía al navegador.
 *  toString() y getName() devuelven solo el correo, para que el token no termine en registros ni en pantallas. */
public record SgpPrincipal(String correo, String nombre, String token) implements Principal, Serializable {

    @Override
    public String getName() {
        return correo;
    }

    @Override
    public String toString() {
        return correo;
    }
}
