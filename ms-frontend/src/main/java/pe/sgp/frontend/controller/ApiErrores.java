package pe.sgp.frontend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.validation.BindingResult;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Iterator;
import java.util.Map;

/** Traduce el error 400 de un microservicio ({"mensaje": "...", "errores": {"campo": "mensaje"}})
 *  a errores del formulario, para mostrar cada mensaje junto a su campo. */
final class ApiErrores {

    private static final ObjectMapper JSON = new ObjectMapper();

    private ApiErrores() { }

    /** El texto {"mensaje": "..."} que mandó el microservicio (una regla de negocio, ya redactada en español),
     *  o un aviso genérico si no se pudo leer. */
    static String mensaje(HttpClientErrorException e) {
        try {
            String texto = JSON.readTree(e.getResponseBodyAsString()).path("mensaje").asText("");
            if (!texto.isBlank()) return texto;
        } catch (Exception ignorado) {
            // cuerpo ilegible: se usa el aviso genérico
        }
        return "No se pudo completar la acción. Revisa los datos e inténtalo de nuevo.";
    }

    static void aplicar(HttpClientErrorException e, BindingResult errores) {
        boolean alguno = false;
        try {
            JsonNode cuerpo = JSON.readTree(e.getResponseBodyAsString());
            JsonNode porCampo = cuerpo.path("errores");
            if (porCampo.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> it = porCampo.fields();
                while (it.hasNext()) {
                    Map.Entry<String, JsonNode> en = it.next();
                    try {
                        errores.rejectValue(en.getKey(), "api", en.getValue().asText());
                        alguno = true;
                    } catch (RuntimeException ignorado) {
                        errores.reject("api", en.getValue().asText());
                        alguno = true;
                    }
                }
            }
        } catch (Exception ignorado) {
            // cuerpo ilegible: se usa el mensaje genérico de abajo
        }
        if (!alguno) {
            errores.reject("api", "No se pudo guardar: revisa los datos e inténtalo de nuevo.");
        }
    }
}
