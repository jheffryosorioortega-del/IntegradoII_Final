package pe.sgp.frontend.client;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import pe.sgp.frontend.dto.FotoDTO;
import pe.sgp.frontend.dto.HistorialDTO;
import pe.sgp.frontend.dto.PulsoDTO;
import pe.sgp.frontend.dto.SolicitudDTO;
import pe.sgp.frontend.dto.SolicitudPublicaForm;
import pe.sgp.frontend.model.EstadoSolicitud;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/** Llama a ms-solicitudes, siempre a través del Gateway. */
@Component
public class SolicitudClient {

    private final RestClient restClient;

    public SolicitudClient(RestClient gatewayRestClient) {
        this.restClient = gatewayRestClient;
    }

    public SolicitudDTO registrarDesdePortal(SolicitudPublicaForm form) {
        return restClient.post()
                .uri("/api/solicitudes")
                .body(form)
                .retrieve()
                .body(SolicitudDTO.class);
    }

    /** Todas las solicitudes (o las de un estado), ya en orden de atención. */
    public List<SolicitudDTO> listar(EstadoSolicitud estado) {
        String uri = estado == null ? "/api/solicitudes" : "/api/solicitudes?estado=" + estado.name();
        List<SolicitudDTO> lista = restClient.get().uri(uri).retrieve()
                .body(new ParameterizedTypeReference<List<SolicitudDTO>>() {});
        return lista == null ? List.of() : lista;
    }

    /** Solo las abiertas, en orden de atención: la primera es la que toca atender ahora. */
    public List<SolicitudDTO> cola() {
        List<SolicitudDTO> lista = restClient.get().uri("/api/solicitudes/cola").retrieve()
                .body(new ParameterizedTypeReference<List<SolicitudDTO>>() {});
        return lista == null ? List.of() : lista;
    }

    /** Datos completos de una solicitud. Si no existe, ms-solicitudes responde 404. */
    public SolicitudDTO obtener(Long id) {
        return restClient.get().uri("/api/solicitudes/{id}", id).retrieve().body(SolicitudDTO.class);
    }

    /** ¿Llegó algo nuevo? Con desdeId trae también las solicitudes posteriores a ese id. */
    public PulsoDTO pulso(Long desdeId) {
        String uri = desdeId == null ? "/api/solicitudes/pulso" : "/api/solicitudes/pulso?desdeId=" + desdeId;
        return restClient.get().uri(uri).retrieve().body(PulsoDTO.class);
    }

    public List<SolicitudDTO> recientes() {
        List<SolicitudDTO> lista = restClient.get().uri("/api/solicitudes/recientes").retrieve()
                .body(new ParameterizedTypeReference<List<SolicitudDTO>>() {});
        return lista == null ? List.of() : lista;
    }

    public Map<EstadoSolicitud, Long> resumen() {
        Map<String, Long> crudo = restClient.get().uri("/api/solicitudes/resumen").retrieve()
                .body(new ParameterizedTypeReference<Map<String, Long>>() {});
        Map<EstadoSolicitud, Long> resultado = new java.util.LinkedHashMap<>();
        for (EstadoSolicitud e : EstadoSolicitud.values()) {
            resultado.put(e, crudo == null ? 0L : crudo.getOrDefault(e.name(), 0L));
        }
        return resultado;
    }

    /** Historial de la solicitud: quién hizo qué y cuándo (ya redactado en español por ms-solicitudes). */
    public List<HistorialDTO> historial(Long id) {
        List<HistorialDTO> lista = restClient.get().uri("/api/solicitudes/{id}/historial", id).retrieve()
                .body(new ParameterizedTypeReference<List<HistorialDTO>>() {});
        return lista == null ? List.of() : lista;
    }

    /** Asigna el técnico con ese correo; un correo vacío o nulo quita al técnico.
     *  409 (flujo de la solicitud) y 400 (no es un técnico activo) los traduce el controlador en un aviso. */
    public void asignarTecnico(Long id, String correo) {
        restClient.put()
                .uri("/api/solicitudes/{id}/tecnico", id)
                .body(Map.of("correo", correo == null ? "" : correo))
                .retrieve()
                .toBodilessEntity();
    }

    public void cambiarEstado(Long id, EstadoSolicitud estado) {
        restClient.put()
                .uri("/api/solicitudes/{id}/estado?estado={estado}", id, estado.name())
                .retrieve()
                .toBodilessEntity();
    }

    /** Envía las fotos de evidencia de una solicitud recién registrada (multipart, campo "fotos"). */
    public void subirFotos(Long solicitudId, List<MultipartFile> archivos) throws IOException {
        org.springframework.util.LinkedMultiValueMap<String, Object> cuerpo = new org.springframework.util.LinkedMultiValueMap<>();
        for (MultipartFile a : archivos) {
            if (a.isEmpty()) continue;
            String nombre = a.getOriginalFilename() == null ? "foto" : a.getOriginalFilename();
            MediaType tipo = a.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(a.getContentType());
            org.springframework.http.HttpHeaders cab = new org.springframework.http.HttpHeaders();
            cab.setContentType(tipo);
            cuerpo.add("fotos", new org.springframework.http.HttpEntity<>(new ByteArrayResource(a.getBytes()) {
                @Override public String getFilename() { return nombre; }
            }, cab));
        }
        restClient.post()
                .uri("/api/solicitudes/{id}/fotos", solicitudId)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(cuerpo)
                .retrieve()
                .toBodilessEntity();
    }

    public List<FotoDTO> fotos(Long solicitudId) {
        List<FotoDTO> lista = restClient.get().uri("/api/solicitudes/{id}/fotos", solicitudId).retrieve()
                .body(new ParameterizedTypeReference<List<FotoDTO>>() {});
        return lista == null ? List.of() : lista;
    }

    /** El contenido de una foto, con su tipo (image/jpeg, image/png o image/webp). */
    public ResponseEntity<byte[]> foto(Long solicitudId, Long fotoId) {
        return restClient.get().uri("/api/solicitudes/{id}/fotos/{fotoId}", solicitudId, fotoId).retrieve().toEntity(byte[].class);
    }
}
