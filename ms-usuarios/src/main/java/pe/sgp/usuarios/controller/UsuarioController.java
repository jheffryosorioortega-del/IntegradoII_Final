package pe.sgp.usuarios.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.sgp.usuarios.dto.LoginRequest;
import pe.sgp.usuarios.dto.TecnicoResponse;
import pe.sgp.usuarios.dto.UsuarioResponse;
import pe.sgp.usuarios.service.UsuarioService;

import java.util.List;

/** API REST de ms-usuarios. La llama el Front-End (a través del Gateway)
 *  cada vez que alguien intenta iniciar sesión. */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /** Técnicos activos (nombre y correo). El Gateway solo lo deja usar a Administrador y Coordinador;
     *  ms-solicitudes lo consulta para comprobar a quién se asigna. */
    @GetMapping("/tecnicos")
    public List<TecnicoResponse> tecnicos() {
        return usuarioService.tecnicos();
    }

    @PostMapping("/login")
    public ResponseEntity<UsuarioResponse> login(@Valid @RequestBody LoginRequest request) {
        return usuarioService.login(request)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(401).build());
    }
}
