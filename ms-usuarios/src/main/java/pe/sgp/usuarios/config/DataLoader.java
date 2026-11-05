package pe.sgp.usuarios.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import pe.sgp.usuarios.model.Rol;
import pe.sgp.usuarios.model.Usuario;
import pe.sgp.usuarios.repository.UsuarioRepository;

/** Crea los 3 usuarios iniciales (administrador, coordinador y técnico) la primera vez que arranca el servicio,
 *  cuando la tabla de usuarios está vacía. Su contraseña NO está escrita en el código: sale de la variable de
 *  entorno SGP_SEED_PASSWORD (archivo .env). Si la tabla ya tiene usuarios, no se toca nada. */
@Configuration
public class DataLoader {

    /** Largo mínimo de la contraseña inicial: una clave corta en un servidor público se adivina en minutos. */
    static final int LARGO_MINIMO = 10;

    @Bean
    CommandLineRunner cargarUsuarios(UsuarioRepository usuarios, @Value("${sgp.seed.password:}") String clave) {
        return args -> {
            if (usuarios.count() > 0) return;
            if (clave == null || clave.isBlank()) {
                throw new IllegalStateException("No hay usuarios y falta SGP_SEED_PASSWORD en el archivo .env: "
                        + "es la contraseña con la que se crean el administrador, el coordinador y el técnico.");
            }
            if (clave.length() < LARGO_MINIMO) {
                throw new IllegalStateException("SGP_SEED_PASSWORD es demasiado corta: usa al menos "
                        + LARGO_MINIMO + " caracteres.");
            }
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            crear(usuarios, encoder, clave, "Administrador SGP", "admin@sgp.local", Rol.ADMINISTRADOR);
            crear(usuarios, encoder, clave, "Coordinador SGP", "coordinador@sgp.local", Rol.COORDINADOR);
            crear(usuarios, encoder, clave, "Técnico SGP", "tecnico@sgp.local", Rol.TECNICO);
        };
    }

    private void crear(UsuarioRepository repo, BCryptPasswordEncoder encoder, String clave,
                       String nombre, String correo, Rol rol) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setCorreo(correo);
        u.setContrasena(encoder.encode(clave));
        u.setRol(rol);
        repo.save(u);
    }
}
