package pe.sgp.frontend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.config.import=", "SGP_DNI_INTERNAL_KEY=test-internal"})
@AutoConfigureMockMvc
class DniFormularioTest {
    @Autowired MockMvc mvc;

    @Test void renderizaFormularioYExigeCsrfParaConsulta() throws Exception {
        var page = mvc.perform(get("/contacto")).andExpect(status().isOk()).andReturn();
        String html = page.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(html.contains("id=\"dni-consulta\""));
        assertTrue(html.contains("/js/dni.js"));
        var csrf = Pattern.compile("data-csrf=\"([^\"]+)\"").matcher(html);
        assertTrue(csrf.find());
        var session = (MockHttpSession) page.getRequest().getSession(false);
        mvc.perform(post("/contacto/consultar-dni").session(session)
                .contentType(MediaType.APPLICATION_JSON).content("{\"dni\":\"123\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/contacto/consultar-dni").session(session)
                .header("X-CSRF-TOKEN", csrf.group(1))
                .contentType(MediaType.APPLICATION_JSON).content("{\"dni\":\"123\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensaje").value("Escribe un DNI de 8 dígitos."));
    }
}
