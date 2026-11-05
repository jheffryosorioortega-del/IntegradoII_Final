package pe.sgp.frontend.controller;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.*;
import org.springframework.web.client.RestClient;
import pe.sgp.frontend.client.SolicitudClient;
import pe.sgp.frontend.dto.*;
import pe.sgp.frontend.model.*;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.config.import="})
@AutoConfigureMockMvc
class EmpresaFormularioTest {
    @Autowired MockMvc mvc;
    @Autowired Captura captura;
    static class Captura extends SolicitudClient {
        SolicitudPublicaForm ultimo;
        Captura() { super(RestClient.builder().baseUrl("http://localhost:1").build()); }
        @Override public java.util.List<SolicitudDTO> listar(EstadoSolicitud estado) {
            return ultimo == null ? java.util.List.of() : java.util.List.of(registrarDesdePortal(ultimo));
        }
        @Override public SolicitudDTO registrarDesdePortal(SolicitudPublicaForm form) {
            ultimo = form;
            return new SolicitudDTO(1L,"SOL-000001",1L,form.getNombre(),form.getTelefono(),
                form.getCategoria(),form.getDescripcion(),form.getDireccion(),form.getUrgencia(),
                EstadoSolicitud.REGISTRADA,form.getTipoSolicitante(),form.getRuc(),form.getRazonSocial(),
                form.getDireccionFiscal(),form.getNombre(),form.getCargo(),form.getCorreo(),form.isRequiereFactura(),
                java.time.LocalDateTime.now(), 0,
                java.util.List.of(EstadoSolicitud.PENDIENTE_EVALUACION, EstadoSolicitud.CERRADA), null, null);
        }
    }
    @Test void panelMuestraEmpresaYDatosDeContacto() throws Exception {
        formularioEmpresaEnviaDatosYConfirma();
        var auth = org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(
            "admin", "", java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMINISTRADOR")));
        var session = new MockHttpSession();
        session.setAttribute("SPRING_SECURITY_CONTEXT", new org.springframework.security.core.context.SecurityContextImpl(auth));
        var html = mvc.perform(get("/panel/solicitudes").session(session)).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(html.contains("Datos de empresa")); assertTrue(html.contains("20000000001"));
        assertTrue(html.contains("ana@example.com")); assertTrue(html.contains("Jefa de mantenimiento"));
    }
    @TestConfiguration static class Config {
        @Bean @Primary Captura captura() { return new Captura(); }
    }
    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder envio() throws Exception {
        var page = mvc.perform(get("/contacto")).andExpect(status().isOk()).andReturn();
        var match = Pattern.compile("data-csrf=\"([^\"]+)\"").matcher(page.getResponse().getContentAsString());
        assertTrue(match.find());
        return post("/contacto").session((MockHttpSession)page.getRequest().getSession(false))
            .param("_csrf",match.group(1)).param("tipoSolicitante","EMPRESA")
            .param("nombre","Ana Prueba").param("telefono","987654321").param("direccion","Av. Prueba 123")
            .param("categoria","BOMBAS").param("urgencia","MEDIA").param("descripcion","Revisar bomba de agua del edificio");
    }
    @Test void empresaIncompletaVuelveALaPestanaConErrores() throws Exception {
        var html = mvc.perform(envio().param("ruc","123").param("razonSocial","").param("correo",""))
            .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form","ruc","razonSocial","correo"))
            .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(html.contains("Persona de contacto"));
        assertTrue(html.contains("empresa.js"));
        assertTrue(Pattern.compile("id=\"tab-empresa\"[^>]*aria-selected=\"true\"").matcher(html).find());
    }
    @Test void formularioEmpresaEnviaDatosYConfirma() throws Exception {
        mvc.perform(envio().param("ruc","20000000001").param("razonSocial","EMPRESA 123 & HIJOS S.A.C.")
                .param("correo","ana@example.com").param("direccionFiscal","Av. Fiscal 456")
                .param("cargo","Jefa de mantenimiento").param("requiereFactura","true"))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/contacto/enviado"));
        assertEquals("20000000001",captura.ultimo.getRuc());
        assertEquals("EMPRESA 123 & HIJOS S.A.C.",captura.ultimo.getRazonSocial());
        assertTrue(captura.ultimo.isRequiereFactura());
    }
    @Test void apiRucExigeCsrf() throws Exception {
        mvc.perform(post("/contacto/consultar-ruc").contentType("application/json").content("{\"ruc\":\"20000000001\"}"))
            .andExpect(status().isForbidden());
    }
}
