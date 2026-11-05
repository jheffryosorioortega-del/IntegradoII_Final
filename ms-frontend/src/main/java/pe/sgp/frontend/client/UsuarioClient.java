package pe.sgp.frontend.client;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import pe.sgp.frontend.dto.TecnicoDTO;
import pe.sgp.frontend.dto.UsuarioDTO;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Llama a ms-usuarios, siempre a través del Gateway (http://localhost:8080/api/usuarios/...). */
@Component
public class UsuarioClient {

    private final RestClient restClient;

    public UsuarioClient(RestClient gatewayRestClient) {
        this.restClient = gatewayRestClient;
    }

    /** Técnicos activos, para el desplegable de «Asignar técnico». Solo Administrador y Coordinador pueden pedirlo. */
    public List<TecnicoDTO> tecnicos() {
        List<TecnicoDTO> lista = restClient.get().uri("/api/usuarios/tecnicos").retrieve()
                .body(new ParameterizedTypeReference<List<TecnicoDTO>>() {});
        return lista == null ? List.of() : lista;
    }

    /** Vacío si las credenciales no sirven (401) o el formato de los datos no es válido (400).
     *  Si ms-usuarios no responde se lanza la excepción de conexión; la pantalla de login avisa de ello. */
    public Optional<UsuarioDTO> login(String correo, String contrasena) {
        try {
            UsuarioDTO usuario = restClient.post()
                    .uri("/api/usuarios/login")
                    .body(Map.of("correo", correo, "contrasena", contrasena))
                    .retrieve()
                    .body(UsuarioDTO.class);
            return Optional.ofNullable(usuario);
        } catch (HttpClientErrorException e) {
            // 401/400: credenciales o datos inválidos. Cualquier otro 4xx (p. ej. 403 por clave API mal
            // configurada) no es culpa del usuario: se relanza para que el login avise "servicio no disponible".
            int estado = e.getStatusCode().value();
            if (estado == 401 || estado == 400) return Optional.empty();
            throw e;
        }
    }
}
