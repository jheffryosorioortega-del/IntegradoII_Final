package pe.sgp.solicitudes.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import pe.sgp.solicitudes.client.ClienteClient;
import pe.sgp.solicitudes.client.UsuarioClient;
import pe.sgp.solicitudes.dto.ClienteCreateRequest;
import pe.sgp.solicitudes.dto.ClienteDTO;
import pe.sgp.solicitudes.dto.FotoResumen;
import pe.sgp.solicitudes.dto.HistorialResponse;
import pe.sgp.solicitudes.dto.PulsoResponse;
import pe.sgp.solicitudes.dto.SolicitudPublicaRequest;
import pe.sgp.solicitudes.dto.SolicitudResponse;
import pe.sgp.solicitudes.dto.TecnicoDTO;
import pe.sgp.solicitudes.model.AccionHistorial;
import pe.sgp.solicitudes.model.EstadoSolicitud;
import pe.sgp.solicitudes.model.FotoSolicitud;
import pe.sgp.solicitudes.model.Solicitud;
import pe.sgp.solicitudes.model.SolicitudHistorial;
import pe.sgp.solicitudes.model.Urgencia;
import pe.sgp.solicitudes.repository.FotoSolicitudRepository;
import pe.sgp.solicitudes.repository.SolicitudHistorialRepository;
import pe.sgp.solicitudes.repository.SolicitudRepository;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class SolicitudService {

    /** ORDEN DE ATENCIÓN: a quién se atiende primero.
     *  1) Las solicitudes abiertas van antes que las cerradas.
     *  2) Entre las abiertas, primero la prioridad más alta (ALTA, MEDIA, BAJA).
     *  3) Con la misma prioridad, la que llegó antes (fecha más antigua).
     *  4) Si llegaron en el mismo instante, la de id menor. */
    static final Comparator<Solicitud> ORDEN_ATENCION = Comparator
            .comparing((Solicitud s) -> s.getEstado() == EstadoSolicitud.CERRADA)
            .thenComparingInt((Solicitud s) -> peso(s.getUrgencia()))
            .thenComparing(Solicitud::getFechaCreacion)
            .thenComparing(Solicitud::getId);

    private static int peso(Urgencia u) {
        if (u == Urgencia.ALTA) return 0;
        if (u == Urgencia.MEDIA) return 1;
        return 2;
    }

    /** Fotos de evidencia: cuántas como máximo, cuánto puede pesar cada una y cuánto tiempo después de
     *  registrada la solicitud se pueden seguir agregando (el formulario las envía justo después). */
    static final int MAX_FOTOS = 4;
    static final int MAX_BYTES_FOTO = 6 * 1024 * 1024;
    static final Duration VENTANA_FOTOS = Duration.ofMinutes(15);

    /** Quien figura en el historial cuando la solicitud la crea el propio cliente desde el portal. */
    static final String USUARIO_PORTAL = "portal";
    private static final String SIN_TECNICO = "Sin asignar";

    private final SolicitudRepository solicitudes;
    private final SolicitudHistorialRepository historial;
    private final FotoSolicitudRepository fotos;
    private final ClienteClient clienteClient;
    private final UsuarioClient usuarioClient;
    private final TransactionTemplate tx;

    public SolicitudService(SolicitudRepository solicitudes, SolicitudHistorialRepository historial,
                            FotoSolicitudRepository fotos, ClienteClient clienteClient, UsuarioClient usuarioClient,
                            TransactionTemplate tx) {
        this.solicitudes = solicitudes;
        this.historial = historial;
        this.fotos = fotos;
        this.clienteClient = clienteClient;
        this.usuarioClient = usuarioClient;
        this.tx = tx;
    }

    /** Registra una solicitud desde el portal. Primero le pregunta a ms-clientes
     *  (vía HTTP) si el teléfono ya existe; si no, le pide que lo cree.
     *  Esta llamada entre servicios es el corazón de la comunicación en microservicios:
     *  si ms-clientes estuviera caído, esta operación fallaría también (es una
     *  dependencia síncrona; una mejora futura sería hacerla asíncrona con colas).
     *
     *  La llamada HTTP se hace FUERA de la transacción de base de datos, para no tener una
     *  conexión abierta mientras se espera a otro servicio. */
    public SolicitudResponse registrarDesdePortal(SolicitudPublicaRequest req) {
        ClienteDTO cliente = clienteClient.buscarPorTelefono(req.getTelefono())
                .orElseGet(() -> clienteClient.crear(new ClienteCreateRequest(
                        req.getNombre(), req.getTelefono(),
                        req.getCorreo() == null || req.getCorreo().isBlank() ? null : req.getCorreo(),
                        req.getDireccion(), req.getDistrito() == null || req.getDistrito().isBlank() ? null : req.getDistrito())));

        // Guardar, asignar el código y dejar la primera línea del historial en UNA sola transacción:
        // no puede quedar una solicitud sin código ni sin historial.
        return tx.execute(status -> {
            Solicitud s = new Solicitud();
            s.setClienteId(cliente.id());
            s.setClienteNombre(cliente.nombre());
            s.setClienteTelefono(cliente.telefono());
            boolean empresa = "EMPRESA".equals(req.getTipoSolicitante());
            s.setTipoSolicitante(empresa ? "EMPRESA" : "PERSONA");
            s.setRuc(empresa ? req.getRuc() : null);
            s.setRazonSocial(empresa ? req.getRazonSocial() : null);
            s.setDireccionFiscal(empresa ? req.getDireccionFiscal() : null);
            s.setCargo(empresa ? req.getCargo() : null);
            s.setRequiereFactura(empresa && req.isRequiereFactura());
            s.setPersonaContacto(req.getNombre());
            s.setCorreoContacto(req.getCorreo());
            s.setCategoria(req.getCategoria());
            s.setDescripcion(req.getDescripcion());
            s.setDireccionServicio(req.getDireccion());
            s.setUrgencia(req.getUrgencia());
            s.setEstado(EstadoSolicitud.REGISTRADA);
            s = solicitudes.saveAndFlush(s);
            s.setCodigo(String.format("SOL-%06d", s.getId()));
            s = solicitudes.saveAndFlush(s);
            historial.save(new SolicitudHistorial(s.getId(), USUARIO_PORTAL, AccionHistorial.CREACION,
                    null, EstadoSolicitud.REGISTRADA.name()));
            return aResponse(s);
        });
    }

    /** Lista en ORDEN DE ATENCIÓN (prioridad y luego fecha de llegada), con las cerradas al final. */
    @Transactional(readOnly = true)
    public List<SolicitudResponse> listar(EstadoSolicitud estado) {
        List<Solicitud> lista = estado == null
                ? solicitudes.findAllByOrderByFechaCreacionDesc()
                : solicitudes.findByEstadoOrderByFechaCreacionDesc(estado);
        return aResponses(lista.stream().sorted(ORDEN_ATENCION).toList());
    }

    /** La cola de atención: solo las solicitudes abiertas, la primera de la lista es la que se atiende ahora. */
    @Transactional(readOnly = true)
    public List<SolicitudResponse> cola() {
        return aResponses(solicitudes.findAll().stream()
                .filter(s -> s.getEstado() != EstadoSolicitud.CERRADA)
                .sorted(ORDEN_ATENCION)
                .toList());
    }

    @Transactional(readOnly = true)
    public List<SolicitudResponse> recientes() {
        return aResponses(solicitudes.findTop5ByOrderByFechaCreacionDesc());
    }

    @Transactional(readOnly = true)
    public SolicitudResponse obtener(Long id) {
        return aResponse(buscar(id));
    }

    /** Lo que el panel consulta cada pocos segundos para mostrar la alerta de "llegó una solicitud". */
    @Transactional(readOnly = true)
    public PulsoResponse pulso(Long desdeId) {
        long sinRevisar = solicitudes.countByEstado(EstadoSolicitud.REGISTRADA);
        long ultimoId = solicitudes.findTopByOrderByIdDesc().map(Solicitud::getId).orElse(0L);
        List<SolicitudResponse> nuevas = desdeId == null ? List.of()
                : aResponses(solicitudes.findTop10ByIdGreaterThanOrderByIdAsc(desdeId));
        return new PulsoResponse(sinRevisar, ultimoId, nuevas);
    }

    /** Cambia la etapa siguiendo el flujo definido en {@link EstadoSolicitud#siguientes()} (409 si no es válido).
     *  Pedir la etapa en la que ya está no hace nada (la pantalla puede reenviarse sin error ni historial duplicado).
     *  Para programar o ejecutar hace falta un técnico asignado. */
    @Transactional
    public SolicitudResponse cambiarEstado(Long id, EstadoSolicitud nuevo, String usuario) {
        Solicitud s = buscar(id);
        EstadoSolicitud actual = s.getEstado();
        if (actual == nuevo) {
            return aResponse(s);
        }
        if (!actual.puedePasarA(nuevo)) {
            throw conflicto("No se puede pasar de «" + actual.getEtiqueta() + "» a «" + nuevo.getEtiqueta() + "».");
        }
        if ((nuevo == EstadoSolicitud.PROGRAMADA || nuevo == EstadoSolicitud.EN_EJECUCION) && s.getTecnicoCorreo() == null) {
            throw conflicto("Asigna un técnico antes de programar la solicitud o iniciar su ejecución.");
        }
        s.setEstado(nuevo);
        historial.save(new SolicitudHistorial(s.getId(), usuario, AccionHistorial.ESTADO, actual.name(), nuevo.name()));
        return aResponse(s);
    }

    /** Asigna (o quita, con correo vacío) el técnico de una solicitud. Se comprueba con ms-usuarios que el correo
     *  sea de un técnico activo; esa llamada HTTP va fuera de la transacción de base de datos. */
    public SolicitudResponse asignarTecnico(Long id, String correo, String usuario) {
        TecnicoDTO elegido = null;
        if (correo != null && !correo.isBlank()) {
            String buscado = correo.trim();
            elegido = usuarioClient.tecnicos().stream()
                    .filter(t -> t.correo() != null && t.correo().equalsIgnoreCase(buscado))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ese usuario no es un técnico activo."));
        }
        TecnicoDTO tecnico = elegido;
        return tx.execute(status -> {
            Solicitud s = buscar(id);
            if (s.getEstado() == EstadoSolicitud.CERRADA) {
                throw conflicto("La solicitud está cerrada: ya no se puede cambiar el técnico.");
            }
            if (tecnico == null && (s.getEstado() == EstadoSolicitud.PROGRAMADA || s.getEstado() == EstadoSolicitud.EN_EJECUCION)) {
                throw conflicto("No puedes quitar al técnico de una solicitud programada o en ejecución.");
            }
            String correoNuevo = tecnico == null ? null : tecnico.correo();
            if (Objects.equals(s.getTecnicoCorreo(), correoNuevo)) {
                return aResponse(s);   // ya es el mismo: nada que registrar
            }
            String anterior = s.getTecnicoNombre();
            s.setTecnicoCorreo(correoNuevo);
            s.setTecnicoNombre(tecnico == null ? null : recortar(tecnico.nombre(), 60));
            historial.save(new SolicitudHistorial(s.getId(), usuario, AccionHistorial.TECNICO, anterior, s.getTecnicoNombre()));
            return aResponse(s);
        });
    }

    @Transactional(readOnly = true)
    public List<HistorialResponse> historial(Long id) {
        buscar(id);   // 404 si no existe
        return historial.findBySolicitudIdOrderByFechaAscIdAsc(id).stream()
                .map(h -> new HistorialResponse(h.getFecha(), h.getUsuario(), h.getAccion().name(), describir(h)))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<EstadoSolicitud, Long> resumenPorEstado() {
        Map<EstadoSolicitud, Long> resumen = new LinkedHashMap<>();
        for (EstadoSolicitud e : EstadoSolicitud.values()) {
            resumen.put(e, solicitudes.countByEstado(e));
        }
        return resumen;
    }

    // ---------- Fotos de evidencia ----------

    /** Guarda las fotos que el cliente adjuntó. Solo se aceptan JPG, PNG o WebP (se comprueba el contenido,
     *  no el nombre), hasta MAX_FOTOS por solicitud, y solo mientras la solicitud es reciente y sigue sin revisar. */
    @Transactional
    public List<FotoResumen> guardarFotos(Long solicitudId, List<MultipartFile> archivos) {
        Solicitud s = buscar(solicitudId);
        if (s.getEstado() != EstadoSolicitud.REGISTRADA
                || s.getFechaCreacion().isBefore(LocalDateTime.now().minus(VENTANA_FOTOS))) {
            throw conflicto("Ya no se pueden agregar fotos a esta solicitud");
        }
        List<MultipartFile> validos = archivos.stream().filter(a -> !a.isEmpty()).toList();
        if (validos.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se recibió ninguna foto");
        }
        if (fotos.countBySolicitudId(solicitudId) + validos.size() > MAX_FOTOS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Máximo " + MAX_FOTOS + " fotos por solicitud");
        }
        for (MultipartFile a : validos) {
            byte[] bytes;
            try {
                bytes = a.getBytes();
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo leer la foto");
            }
            if (bytes.length > MAX_BYTES_FOTO) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cada foto debe pesar menos de 6 MB");
            }
            String tipo = tipoDeImagen(bytes);
            if (tipo == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo se aceptan fotos JPG, PNG o WebP");
            }
            FotoSolicitud f = new FotoSolicitud();
            f.setSolicitudId(solicitudId);
            f.setNombre(nombreSeguro(a.getOriginalFilename()));
            f.setTipo(tipo);
            f.setTamano(bytes.length);
            f.setContenido(bytes);
            fotos.save(f);
        }
        return fotos.resumenes(solicitudId);
    }

    @Transactional(readOnly = true)
    public List<FotoResumen> fotos(Long solicitudId) {
        if (!solicitudes.existsById(solicitudId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitud no encontrada");
        }
        return fotos.resumenes(solicitudId);
    }

    @Transactional(readOnly = true)
    public FotoSolicitud foto(Long solicitudId, Long fotoId) {
        return fotos.findByIdAndSolicitudId(fotoId, solicitudId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Foto no encontrada"));
    }

    /** Reconoce el formato por los primeros bytes del archivo: así un .exe renombrado a .jpg no pasa. */
    static String tipoDeImagen(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "image/jpeg";
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) return "image/png";
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return "image/webp";
        return null;
    }

    /** Deja solo el nombre del archivo, sin carpetas ni caracteres raros, y lo recorta a 100 caracteres. */
    static String nombreSeguro(String original) {
        String n = original == null ? "" : original.replace('\\', '/');
        n = n.substring(n.lastIndexOf('/') + 1).replaceAll("[^\\p{L}\\p{N} ._-]", "").trim();
        if (n.isEmpty()) n = "foto";
        return n.length() > 100 ? n.substring(n.length() - 100) : n;
    }

    // ---------- Respuestas y textos ----------

    private Solicitud buscar(Long id) {
        return solicitudes.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitud no encontrada"));
    }

    private static ResponseStatusException conflicto(String mensaje) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensaje);
    }

    private static String recortar(String texto, int max) {
        return texto == null || texto.length() <= max ? texto : texto.substring(0, max);
    }

    /** Redacta la línea del historial en español ("Estado: Registrada → Cotizada"). */
    private static String describir(SolicitudHistorial h) {
        return switch (h.getAccion()) {
            case CREACION -> "Solicitud registrada";
            case ESTADO -> "Estado: " + etiqueta(h.getAnterior()) + " → " + etiqueta(h.getNuevo());
            case TECNICO -> "Técnico: " + (h.getAnterior() == null ? SIN_TECNICO : h.getAnterior())
                    + " → " + (h.getNuevo() == null ? SIN_TECNICO : h.getNuevo());
        };
    }

    private static String etiqueta(String estado) {
        try {
            return EstadoSolicitud.valueOf(estado).getEtiqueta();
        } catch (RuntimeException e) {
            return String.valueOf(estado);
        }
    }

    /** Convierte una lista de solicitudes y les agrega cuántas fotos tiene cada una con una sola consulta. */
    private List<SolicitudResponse> aResponses(List<Solicitud> lista) {
        Map<Long, Integer> conteo = new HashMap<>();
        if (!lista.isEmpty()) {
            Collection<Long> ids = lista.stream().map(Solicitud::getId).toList();
            for (Object[] fila : fotos.contarPorSolicitud(ids)) {
                conteo.put((Long) fila[0], ((Number) fila[1]).intValue());
            }
        }
        return lista.stream().map(s -> aResponse(s, conteo.getOrDefault(s.getId(), 0))).toList();
    }

    private SolicitudResponse aResponse(Solicitud s) {
        return aResponse(s, (int) fotos.countBySolicitudId(s.getId()));
    }

    private SolicitudResponse aResponse(Solicitud s, int nFotos) {
        return new SolicitudResponse(s.getId(), s.getCodigo(), s.getClienteId(), s.getClienteNombre(),
                s.getClienteTelefono(), s.getCategoria(), s.getDescripcion(), s.getDireccionServicio(),
                s.getUrgencia(), s.getEstado(), s.getFechaCreacion(),
                s.getTipoSolicitante(), s.getRuc(), s.getRazonSocial(), s.getDireccionFiscal(),
                s.getPersonaContacto(), s.getCargo(), s.getCorreoContacto(), s.isRequiereFactura(),
                List.copyOf(s.getEstado().siguientes()), s.getTecnicoCorreo(), s.getTecnicoNombre(), nFotos);
    }
}
