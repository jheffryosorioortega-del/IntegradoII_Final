package pe.sgp.frontend.dto;

public record ClienteDTO(Long id, String nombre, String telefono, String correo,
                         String direccion, String distrito) {
}
