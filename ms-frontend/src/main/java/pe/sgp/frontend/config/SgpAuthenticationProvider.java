package pe.sgp.frontend.config;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import pe.sgp.frontend.client.UsuarioClient;
import pe.sgp.frontend.dto.UsuarioDTO;

import java.util.List;

/** Reemplaza al UserDetailsService que había en la versión monolito.
 *  Aquí el Front-End NO tiene tabla de usuarios: cuando alguien envía el
 *  formulario de login, este Provider le pasa el correo y la contraseña a
 *  ms-usuarios (por REST, vía Gateway) y es ESE microservicio quien decide
 *  si son correctos. El Front-End solo arma la sesión con el rol que le devuelven. */
@Component
public class SgpAuthenticationProvider implements AuthenticationProvider {

    private final UsuarioClient usuarioClient;

    public SgpAuthenticationProvider(UsuarioClient usuarioClient) {
        this.usuarioClient = usuarioClient;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String correo = authentication.getName();
        String contrasena = String.valueOf(authentication.getCredentials());

        UsuarioDTO usuario;
        try {
            usuario = usuarioClient.login(correo, contrasena)
                    .orElseThrow(() -> new BadCredentialsException("Credenciales incorrectas o cuenta inactiva"));
        } catch (RestClientException e) {
            // ms-usuarios (o el Gateway) no responde: no es un problema de credenciales.
            throw new AuthenticationServiceException("El servicio de usuarios no está disponible", e);
        }

        List<SimpleGrantedAuthority> autoridades = List.of(new SimpleGrantedAuthority("ROLE_" + usuario.rol().name()));
        // El principal lleva el JWT de ms-usuarios; gatewayRestClient lo manda en cada llamada al Gateway.
        return new UsernamePasswordAuthenticationToken(
                new SgpPrincipal(usuario.correo(), usuario.nombre(), usuario.token()), null, autoridades);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
