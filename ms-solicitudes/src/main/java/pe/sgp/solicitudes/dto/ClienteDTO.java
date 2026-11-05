package pe.sgp.solicitudes.dto;

/** Forma del JSON que devuelve ms-clientes. Es una COPIA del contrato de esa API,
 *  escrita a mano en este microservicio (no se comparte la clase Java entre
 *  proyectos). Si ms-clientes cambia su respuesta, hay que actualizar esto también. */
public record ClienteDTO(Long id, String nombre, String telefono, String correo,
                         String direccion, String distrito) {
}
