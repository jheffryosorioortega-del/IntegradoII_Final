package pe.sgp.solicitudes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@SpringBootApplication
public class SolicitudesApplication {

    public static void main(String[] args) {
        SpringApplication.run(SolicitudesApplication.class, args);
    }

    /** Cliente HTTP reutilizable para llamar a ms-clientes (inyectado en ClienteClient). */
    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
