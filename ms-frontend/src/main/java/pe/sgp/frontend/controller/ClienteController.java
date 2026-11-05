package pe.sgp.frontend.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.client.HttpClientErrorException;
import pe.sgp.frontend.client.ClienteClient;
import pe.sgp.frontend.dto.ClienteForm;

@Controller
public class ClienteController {

    private final ClienteClient clienteClient;

    public ClienteController(ClienteClient clienteClient) {
        this.clienteClient = clienteClient;
    }

    @GetMapping("/panel/clientes")
    public String listar(Model model) {
        model.addAttribute("clientes", clienteClient.listar());
        return "panel/clientes";
    }

    @GetMapping("/panel/clientes/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("cliente", new ClienteForm());
        return "panel/cliente-form";
    }

    @PostMapping("/panel/clientes")
    public String guardar(@Valid @ModelAttribute("cliente") ClienteForm form, BindingResult errores) {
        if (!errores.hasErrors()) {
            try {
                clienteClient.crear(form);
                return "redirect:/panel/clientes";
            } catch (HttpClientErrorException.Conflict e) {
                errores.rejectValue("telefono", "duplicado", "Ya existe un cliente con ese teléfono");
            } catch (HttpClientErrorException.BadRequest e) {
                ApiErrores.aplicar(e, errores);
            }
        }
        return "panel/cliente-form";
    }
}
