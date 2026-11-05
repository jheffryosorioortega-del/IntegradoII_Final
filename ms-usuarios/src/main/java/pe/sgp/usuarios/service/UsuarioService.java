package pe.sgp.usuarios.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import pe.sgp.usuarios.dto.LoginRequest;
import pe.sgp.usuarios.dto.TecnicoResponse;
import pe.sgp.usuarios.dto.UsuarioResponse;
import pe.sgp.usuarios.model.Rol;
import pe.sgp.usuarios.model.Usuario;
import pe.sgp.usuarios.repository.UsuarioRepository;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarios;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final JwtService jwt;

    public UsuarioService(UsuarioRepository usuarios, JwtService jwt) {
        this.usuarios = usuarios;
        this.jwt = jwt;
    }

    /** Técnicos activos, para poder asignarlos a una solicitud. */
    public List<TecnicoResponse> tecnicos() {
        return usuarios.findByRolAndActivoTrueOrderByNombreAsc(Rol.TECNICO).stream()
                .map(u -> new TecnicoResponse(u.getId(), u.getNombre(), u.getCorreo()))
                .toList();
    }

    /** Valida correo + contraseña. Devuelve vacío si no existe, está inactivo
     *  o la contraseña no coincide — sin decir cuál de los tres fue (CUS1). */
    public Optional<UsuarioResponse> login(LoginRequest req) {
        return usuarios.findByCorreo(req.getCorreo())
                .filter(Usuario::isActivo)
                .filter(u -> encoder.matches(req.getContrasena(), u.getContrasena()))
                .map(u -> new UsuarioResponse(u.getId(), u.getNombre(), u.getCorreo(), u.getRol(), jwt.emitir(u)));
    }
}
