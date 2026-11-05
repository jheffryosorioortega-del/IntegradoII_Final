package pe.sgp.usuarios.config;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import pe.sgp.usuarios.model.Rol;
import pe.sgp.usuarios.model.Usuario;
import pe.sgp.usuarios.repository.UsuarioRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DataLoaderTest {

    private final UsuarioRepository repo = mock(UsuarioRepository.class);

    private void ejecutar(String clave) throws Exception {
        new DataLoader().cargarUsuarios(repo, clave).run();
    }

    @Test
    void conLaTablaVaciaCreaLosTresUsuariosConLaClaveDelEntorno() throws Exception {
        when(repo.count()).thenReturn(0L);
        ejecutar("clave-de-prueba-123");

        ArgumentCaptor<Usuario> captura = ArgumentCaptor.forClass(Usuario.class);
        verify(repo, times(3)).save(captura.capture());
        List<Usuario> creados = captura.getAllValues();
        assertEquals(List.of(Rol.ADMINISTRADOR, Rol.COORDINADOR, Rol.TECNICO),
                creados.stream().map(Usuario::getRol).toList());
        var encoder = new BCryptPasswordEncoder();
        for (Usuario u : creados) {
            assertNotEquals("clave-de-prueba-123", u.getContrasena(), "se guarda el hash, nunca la clave");
            assertTrue(encoder.matches("clave-de-prueba-123", u.getContrasena()));
        }
    }

    @Test
    void siYaHayUsuariosNoTocaNadaNiExigeLaClave() throws Exception {
        when(repo.count()).thenReturn(3L);
        ejecutar("");
        verify(repo, never()).save(any());
    }

    @Test
    void sinClaveYConLaTablaVaciaNoArranca() {
        when(repo.count()).thenReturn(0L);
        var e = assertThrows(IllegalStateException.class, () -> ejecutar(""));
        assertTrue(e.getMessage().contains("SGP_SEED_PASSWORD"));
        verify(repo, never()).save(any());
    }

    @Test
    void unaClaveCortaSeRechaza() {
        when(repo.count()).thenReturn(0L);
        var e = assertThrows(IllegalStateException.class, () -> ejecutar("corta123"));
        assertTrue(e.getMessage().contains("demasiado corta"));
        verify(repo, never()).save(any());
    }
}
