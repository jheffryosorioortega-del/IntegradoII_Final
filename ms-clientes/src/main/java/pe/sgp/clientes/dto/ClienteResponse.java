package pe.sgp.clientes.dto;

public record ClienteResponse(Long id, String nombre, String telefono, String correo,
                              String direccion, String distrito) {
}
