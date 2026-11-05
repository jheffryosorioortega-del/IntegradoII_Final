package pe.sgp.usuarios.dto;

import pe.sgp.usuarios.model.Rol;

/** Lo que este microservicio devuelve hacia afuera. Nunca incluye la contraseña,
 *  ni siquiera el hash: una vez validada la contraseña, a nadie más le hace falta.
 *  {@code token} es el JWT firmado que el Front-End reenviará al Gateway en cada llamada. */
public record UsuarioResponse(Long id, String nombre, String correo, Rol rol, String token) {
}
