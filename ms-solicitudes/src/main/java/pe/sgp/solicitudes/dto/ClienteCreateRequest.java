package pe.sgp.solicitudes.dto;

public record ClienteCreateRequest(String nombre, String telefono, String correo,
                                   String direccion, String distrito) {
}
