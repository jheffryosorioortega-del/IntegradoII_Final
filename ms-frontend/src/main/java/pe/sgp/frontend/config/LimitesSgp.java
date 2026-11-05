package pe.sgp.frontend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Topes contra abuso, leídos de "sgp.limites" en application.yml (cada uno se puede cambiar con una variable de
 *  entorno, por ejemplo SGP_LIMITES_ENVIOS_POR_IP=100). Detrás de un proxy con HTTPS, "por IP" es por visitante real;
 *  sin proxy, todos los que lleguen por el mismo camino comparten IP. */
@ConfigurationProperties("sgp.limites")
public record LimitesSgp(
        /** Contraseñas erróneas permitidas desde una misma IP dentro de la ventana de login. */
        @DefaultValue("10") int loginFallosPorIp,
        /** Contraseñas erróneas permitidas contra un mismo correo, vengan de la IP que vengan. */
        @DefaultValue("15") int loginFallosPorCorreo,
        @DefaultValue("10") int loginVentanaMinutos,
        /** Solicitudes enviadas desde una IP en la ventana corta ... */
        @DefaultValue("8") int enviosPorIp,
        @DefaultValue("10") int enviosVentanaMinutos,
        /** ... y en 24 horas. */
        @DefaultValue("30") int enviosPorIpDia,
        /** Consultas DNI/RUC por IP (cada una gasta cupo del proveedor externo). */
        @DefaultValue("20") int consultasPorIp,
        @DefaultValue("10") int consultasVentanaMinutos) {
}
