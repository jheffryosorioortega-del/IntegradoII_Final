package pe.sgp.clientes.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.sgp.clientes.dto.ClienteRequest;
import pe.sgp.clientes.dto.ClienteResponse;
import pe.sgp.clientes.service.ClienteService;

import java.util.List;

/** API REST de ms-clientes. La consumen ms-frontend (panel de clientes) y
 *  ms-solicitudes (para reutilizar o crear un cliente al registrar una solicitud). */
@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping
    public List<ClienteResponse> listar() {
        return clienteService.listar();
    }

    @GetMapping("/buscar")
    public ResponseEntity<ClienteResponse> buscarPorTelefono(@RequestParam String telefono) {
        return clienteService.buscarPorTelefono(telefono)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClienteResponse crear(@Valid @RequestBody ClienteRequest request) {
        return clienteService.crear(request);
    }
}
