package pe.sgp.frontend.client;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import pe.sgp.frontend.dto.ClienteDTO;
import pe.sgp.frontend.dto.ClienteForm;

import java.util.List;
import java.util.Map;

/** Llama a ms-clientes, siempre a través del Gateway. */
@Component
public class ClienteClient {

    private final RestClient restClient;

    public ClienteClient(RestClient gatewayRestClient) {
        this.restClient = gatewayRestClient;
    }

    public List<ClienteDTO> listar() {
        List<ClienteDTO> lista = restClient.get()
                .uri("/api/clientes")
                .retrieve()
                .body(new ParameterizedTypeReference<List<ClienteDTO>>() {});
        return lista == null ? List.of() : lista;
    }

    /** Lanza HttpClientErrorException.Conflict (409) si el teléfono ya existe;
     *  el controlador la traduce en un error de formulario. */
    public ClienteDTO crear(ClienteForm form) {
        return restClient.post()
                .uri("/api/clientes")
                .body(Map.of(
                        "nombre", form.getNombre(),
                        "telefono", form.getTelefono(),
                        "correo", form.getCorreo() == null ? "" : form.getCorreo(),
                        "direccion", form.getDireccion() == null ? "" : form.getDireccion(),
                        "distrito", form.getDistrito() == null ? "" : form.getDistrito()))
                .retrieve()
                .body(ClienteDTO.class);
    }
}
