package pe.sgp.frontend.dto;

import pe.sgp.frontend.model.Rol;

/** Respuesta del login de ms-usuarios. {@code token} es el JWT que se reenvía al Gateway. */
public record UsuarioDTO(Long id, String nombre, String correo, Rol rol, String token) {
}
