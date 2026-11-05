package pe.sgp.usuarios.dto;

/** Lo mínimo que se necesita para asignar un técnico a una solicitud: nunca incluye contraseña, hash ni token. */
public record TecnicoResponse(Long id, String nombre, String correo) {
}
