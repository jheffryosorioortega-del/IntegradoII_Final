package pe.sgp.solicitudes.controller;

import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pe.sgp.solicitudes.dto.AsignarTecnicoRequest;
import pe.sgp.solicitudes.dto.FotoResumen;
import pe.sgp.solicitudes.dto.HistorialResponse;
import pe.sgp.solicitudes.dto.PulsoResponse;
import pe.sgp.solicitudes.dto.SolicitudPublicaRequest;
import pe.sgp.solicitudes.dto.SolicitudResponse;
import pe.sgp.solicitudes.model.EstadoSolicitud;
import pe.sgp.solicitudes.model.FotoSolicitud;
import pe.sgp.solicitudes.service.SolicitudService;

import java.util.Map;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/** API REST de ms-solicitudes. La consume ms-frontend (portal público y panel),
 *  siempre a través del Gateway. */
@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudController {

    /** El Gateway pone aquí el correo del usuario cuyo JWT verificó (y borra el que llegue de afuera). */
    static final String HEADER_USUARIO = "X-SGP-Usuario";
    private static final Pattern CORREO = Pattern.compile("[A-Za-z0-9._%+\\-@]{1,120}");

    private final SolicitudService solicitudService;

    public SolicitudController(SolicitudService solicitudService) {
        this.solicitudService = solicitudService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SolicitudResponse registrar(@Valid @RequestBody SolicitudPublicaRequest request) {
        return solicitudService.registrarDesdePortal(request);
    }

    /** Todas las solicitudes (o las de un estado) en orden de atención: prioridad y luego fecha de llegada. */
    @GetMapping
    public List<SolicitudResponse> listar(@RequestParam(required = false) EstadoSolicitud estado) {
        return solicitudService.listar(estado);
    }

    /** Cola de atención: solo las abiertas; la primera es la que se atiende ahora. */
    @GetMapping("/cola")
    public List<SolicitudResponse> cola() {
        return solicitudService.cola();
    }

    /** Alerta de solicitudes nuevas: cuántas están sin revisar y cuáles llegaron después de "desdeId". */
    @GetMapping("/pulso")
    public PulsoResponse pulso(@RequestParam(required = false) Long desdeId) {
        return solicitudService.pulso(desdeId);
    }

    @GetMapping("/recientes")
    public List<SolicitudResponse> recientes() {
        return solicitudService.recientes();
    }

    @GetMapping("/resumen")
    public Map<EstadoSolicitud, Long> resumen() {
        return solicitudService.resumenPorEstado();
    }

    /** Datos completos de una solicitud (la ficha que se revisa en el panel). */
    @GetMapping("/{id}")
    public SolicitudResponse obtener(@PathVariable Long id) {
        return solicitudService.obtener(id);
    }

    @GetMapping("/{id}/historial")
    public List<HistorialResponse> historial(@PathVariable Long id) {
        return solicitudService.historial(id);
    }

    @PutMapping("/{id}/estado")
    public SolicitudResponse cambiarEstado(@PathVariable Long id, @RequestParam EstadoSolicitud estado,
                                           @RequestHeader(value = HEADER_USUARIO, required = false) String usuario) {
        return solicitudService.cambiarEstado(id, estado, quien(usuario));
    }

    @PutMapping("/{id}/tecnico")
    public SolicitudResponse asignarTecnico(@PathVariable Long id, @Valid @RequestBody AsignarTecnicoRequest request,
                                            @RequestHeader(value = HEADER_USUARIO, required = false) String usuario) {
        return solicitudService.asignarTecnico(id, request.correo(), quien(usuario));
    }

    /** Fotos de evidencia que el cliente adjunta a su solicitud (multipart, campo "fotos"). */
    @PostMapping(path = "/{id}/fotos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public List<FotoResumen> subirFotos(@PathVariable Long id, @RequestParam("fotos") List<MultipartFile> fotos) {
        return solicitudService.guardarFotos(id, fotos);
    }

    @GetMapping("/{id}/fotos")
    public List<FotoResumen> fotos(@PathVariable Long id) {
        return solicitudService.fotos(id);
    }

    /** El contenido de una foto, tal cual (JPG, PNG o WebP). */
    @GetMapping("/{id}/fotos/{fotoId}")
    public ResponseEntity<byte[]> foto(@PathVariable Long id, @PathVariable Long fotoId) {
        FotoSolicitud f = solicitudService.foto(id, fotoId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(f.getTipo()))
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                .body(f.getContenido());
    }

    /** Quién figura en el historial. Si la llamada no vino por el Gateway (sin cabecera) o trae algo raro, "desconocido". */
    private static String quien(String cabecera) {
        return cabecera != null && CORREO.matcher(cabecera.trim()).matches() ? cabecera.trim() : "desconocido";
    }
}
