package pe.sgp.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** No tiene controladores propios: Spring Cloud Gateway lee las rutas de
 *  application.yml y, para cada petición que entra por el puerto 8080,
 *  decide a qué microservicio reenviarla según la ruta (Path=...). */
@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
