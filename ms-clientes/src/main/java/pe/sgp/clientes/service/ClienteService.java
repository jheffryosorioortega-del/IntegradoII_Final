package pe.sgp.clientes.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.sgp.clientes.dto.ClienteRequest;
import pe.sgp.clientes.dto.ClienteResponse;
import pe.sgp.clientes.model.Cliente;
import pe.sgp.clientes.repository.ClienteRepository;

import java.util.List;
import java.util.Optional;

@Service
public class ClienteService {

    private static final String MSG_DUPLICADO = "Ya existe un cliente registrado con ese teléfono.";

    private final ClienteRepository clientes;

    public ClienteService(ClienteRepository clientes) {
        this.clientes = clientes;
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> listar() {
        return clientes.findAllByOrderByNombreAsc().stream().map(this::aResponse).toList();
    }

    @Transactional(readOnly = true)
    public Optional<ClienteResponse> buscarPorTelefono(String telefono) {
        return clientes.findFirstByTelefono(telefono.trim()).map(this::aResponse);
    }

    /** Evita duplicados por teléfono (CUS2, flujo alternativo): 409 si ya existe.
     *  Primero se consulta (mensaje claro) y además la restricción UNIQUE de la base de datos
     *  cubre el caso de dos peticiones simultáneas con el mismo teléfono. */
    @Transactional
    public ClienteResponse crear(ClienteRequest req) {
        if (clientes.findFirstByTelefono(req.getTelefono()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, MSG_DUPLICADO);
        }
        Cliente c = new Cliente();
        c.setNombre(req.getNombre());
        c.setTelefono(req.getTelefono());
        c.setCorreo(vacioANulo(req.getCorreo()));
        c.setDireccion(vacioANulo(req.getDireccion()));
        c.setDistrito(vacioANulo(req.getDistrito()));
        try {
            return aResponse(clientes.saveAndFlush(c));
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, MSG_DUPLICADO);
        }
    }

    private static String vacioANulo(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private ClienteResponse aResponse(Cliente c) {
        return new ClienteResponse(c.getId(), c.getNombre(), c.getTelefono(), c.getCorreo(),
                c.getDireccion(), c.getDistrito());
    }
}
