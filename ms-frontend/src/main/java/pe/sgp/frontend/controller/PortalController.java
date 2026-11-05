package pe.sgp.frontend.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.sgp.frontend.client.SolicitudClient;
import pe.sgp.frontend.dto.SolicitudDTO;
import pe.sgp.frontend.dto.SolicitudPublicaForm;
import pe.sgp.frontend.model.CategoriaServicio;
import pe.sgp.frontend.model.Urgencia;

import java.util.List;
import java.util.Set;

/** Portal público. La única diferencia de fondo con la versión monolito:
 *  "enviar" ya no guarda en una base de datos local, hace un POST HTTP al
 *  Gateway, que lo reenvía a ms-solicitudes. */
@Controller
public class PortalController {

    /** Fotos de evidencia: hasta 4, de hasta 6 MB cada una, en estos formatos. ms-solicitudes lo vuelve a comprobar. */
    private static final int MAX_FOTOS = 4;
    private static final long MAX_BYTES_FOTO = 6L * 1024 * 1024;
    private static final Set<String> TIPOS_FOTO = Set.of("image/jpeg", "image/png", "image/webp");

    private static final Logger LOG = LoggerFactory.getLogger(PortalController.class);

    private final SolicitudClient solicitudClient;

    public PortalController(SolicitudClient solicitudClient) {
        this.solicitudClient = solicitudClient;
    }

    @GetMapping("/")
    public String inicio(Model model) {
        model.addAttribute("servicios", CategoriaServicio.values());
        return "portal/inicio";
    }

    @GetMapping("/servicios")
    public String servicios(Model model) {
        model.addAttribute("servicios", CategoriaServicio.values());
        return "portal/servicios";
    }

    @GetMapping("/contacto")
    public String contacto(@RequestParam(required = false) CategoriaServicio categoria, Model model) {
        SolicitudPublicaForm form = new SolicitudPublicaForm();
        form.setCategoria(categoria);
        model.addAttribute("form", form);
        cargarCatalogos(model);
        return "portal/contacto";
    }

    @PostMapping("/contacto")
    public String enviar(@Valid @ModelAttribute("form") SolicitudPublicaForm form,
                         BindingResult errores,
                         @RequestParam(name = "fotos", required = false) List<MultipartFile> fotos,
                         Model model, RedirectAttributes flash) {
        return procesarEnvio(form, errores, fotos, model, flash);
    }

    private String procesarEnvio(SolicitudPublicaForm form, BindingResult errores, List<MultipartFile> fotos,
                                 Model model, RedirectAttributes flash) {
        List<MultipartFile> adjuntas = fotos == null ? List.of() : fotos.stream().filter(f -> !f.isEmpty()).toList();
        String problemaFotos = revisarFotos(adjuntas);
        if (problemaFotos != null) {
            model.addAttribute("errorFotos", problemaFotos);
        } else if (!errores.hasErrors()) {
            try {
                SolicitudDTO s = solicitudClient.registrarDesdePortal(form);
                flash.addFlashAttribute("codigo", s.codigo());
                // La solicitud ya quedó registrada; si las fotos fallan, se avisa pero no se pierde lo demás.
                if (!adjuntas.isEmpty()) {
                    try {
                        solicitudClient.subirFotos(s.id(), adjuntas);
                        flash.addFlashAttribute("fotosRecibidas", adjuntas.size());
                    } catch (Exception e) {
                        // El detalle queda solo en el registro del servidor (consola), no en archivos ni en pantalla.
                        LOG.warn("No se pudieron subir las fotos de la solicitud {}", s.codigo(), e);
                        flash.addFlashAttribute("fotosFallaron", true);
                    }
                }
                return "redirect:/contacto/enviado";
            } catch (HttpClientErrorException.BadRequest e) {
                // El servicio rechazó datos que la pantalla dejó pasar: se muestran junto a su campo.
                ApiErrores.aplicar(e, errores);
            }
        }
        cargarCatalogos(model);
        return "portal/contacto";
    }

    @GetMapping("/contacto/enviado")
    public String enviado() {
        return "portal/enviado";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    private static String revisarFotos(List<MultipartFile> fotos) {
        if (fotos.size() > MAX_FOTOS) return "Adjunta hasta " + MAX_FOTOS + " fotos.";
        for (MultipartFile f : fotos) {
            if (f.getSize() > MAX_BYTES_FOTO) return "Cada foto debe pesar menos de 6 MB.";
            if (f.getContentType() == null || !TIPOS_FOTO.contains(f.getContentType())) return "Las fotos deben ser JPG, PNG o WebP.";
        }
        return null;
    }

    private void cargarCatalogos(Model model) {
        model.addAttribute("categorias", CategoriaServicio.values());
        model.addAttribute("urgencias", Urgencia.values());
    }
}
