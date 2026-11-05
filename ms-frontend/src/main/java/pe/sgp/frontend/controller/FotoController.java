package pe.sgp.frontend.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import pe.sgp.frontend.client.SolicitudClient;

import java.time.Duration;

/** Una foto de evidencia de la solicitud. Solo la ve quien entró al panel (/panel/** exige sesión) y el Gateway
 *  vuelve a comprobarlo con el JWT. Va en su propia clase, fuera de PanelAvisos, para que cada imagen no dispare
 *  además la consulta del contador de «sin revisar» (una ficha con 4 fotos haría 4 llamadas de más). */
@RestController
public class FotoController {

    private final SolicitudClient solicitudClient;

    public FotoController(SolicitudClient solicitudClient) {
        this.solicitudClient = solicitudClient;
    }

    @GetMapping("/panel/solicitudes/{id}/fotos/{fotoId}")
    public ResponseEntity<byte[]> foto(@PathVariable Long id, @PathVariable Long fotoId) {
        try {
            ResponseEntity<byte[]> r = solicitudClient.foto(id, fotoId);
            if (r.getBody() == null || r.getHeaders().getContentType() == null) return ResponseEntity.notFound().build();
            return ResponseEntity.ok()
                    .contentType(r.getHeaders().getContentType())
                    .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
                    .body(r.getBody());
        } catch (HttpClientErrorException.NotFound e) {
            return ResponseEntity.notFound().build();
        }
    }
}
