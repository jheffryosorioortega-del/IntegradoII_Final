package pe.sgp.usuarios.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.sgp.usuarios.model.Rol;
import pe.sgp.usuarios.model.Usuario;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByCorreo(String correo);

    List<Usuario> findByRolAndActivoTrueOrderByNombreAsc(Rol rol);
}
