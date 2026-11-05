package pe.sgp.frontend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

/** Misma política de acceso que en la versión monolito (RF01, RNF01): portal público, panel protegido,
 *  gestión de clientes solo para Administrador/Coordinador. Lo único que cambia es CÓMO se valida el
 *  login (ver SgpAuthenticationProvider). Además se agregan cabeceras de seguridad y frenos contra abuso
 *  (LimiteLogin, LimiteEnvios). */
@Configuration
@EnableConfigurationProperties(LimitesSgp.class)
public class SecurityConfig {

    /** Política de contenido (CSP): el navegador solo ejecuta scripts, estilos, fuentes e imágenes que vienen
     *  de este mismo sitio. Aunque alguien lograra inyectar un <script>, el navegador no lo ejecutaría. */
    private static final String CSP = String.join("; ",
            "default-src 'self'",
            "script-src 'self'",
            "style-src 'self'",
            "img-src 'self' data:",
            "font-src 'self'",
            "connect-src 'self'",
            "object-src 'none'",
            "base-uri 'self'",
            "form-action 'self'",
            "frame-ancestors 'none'");

    @Bean
    public AuthenticationManager authenticationManager(SgpAuthenticationProvider provider) {
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, LimiteIntentos limite, LimitesSgp limites) throws Exception {
        // No son @Bean a propósito: Spring Boot registraría cada filtro suelto también fuera de la cadena de seguridad.
        LimiteLogin limiteLogin = new LimiteLogin(limite, limites);
        LimiteEnvios limiteEnvios = new LimiteEnvios(limite, limites);

        http
            // Antes del CSRF: un envío rechazado por exceso no llega a cargar sus fotos en memoria.
            .addFilterBefore(limiteEnvios, CsrfFilter.class)
            .addFilterBefore(limiteLogin, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/servicios", "/contacto", "/contacto/**", "/login", "/error",
                                 "/css/**", "/js/**", "/img/**", "/webjars/**").permitAll()
                .requestMatchers("/panel/clientes/**").hasAnyRole("ADMINISTRADOR", "COORDINADOR")
                .requestMatchers(HttpMethod.POST, "/panel/solicitudes/**").hasAnyRole("ADMINISTRADOR", "COORDINADOR")
                .requestMatchers("/panel/**").authenticated()
                .anyRequest().permitAll())
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler((req, res, auth) -> {
                    limiteLogin.registrarExito(req);
                    res.sendRedirect(req.getContextPath() + "/panel");
                })
                // Credenciales malas -> /login?error (y cuenta para el freno) ; servicio de usuarios caído -> /login?servicio
                .failureHandler((req, res, ex) -> {
                    boolean servicioCaido = ex instanceof AuthenticationServiceException;
                    if (!servicioCaido) limiteLogin.registrarFallo(req);
                    res.sendRedirect(req.getContextPath() + (servicioCaido ? "/login?servicio" : "/login?error"));
                })
                .permitAll())
            .logout(logout -> logout.logoutSuccessUrl("/"))
            .headers(h -> h
                .contentSecurityPolicy(csp -> csp.policyDirectives(CSP))
                .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .permissionsPolicy(p -> p.policy("geolocation=(), camera=(), microphone=()")));
        return http.build();
    }
}
