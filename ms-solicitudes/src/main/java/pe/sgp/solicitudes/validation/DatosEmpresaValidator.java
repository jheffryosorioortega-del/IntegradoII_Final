package pe.sgp.solicitudes.validation;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import pe.sgp.solicitudes.dto.SolicitudPublicaRequest;
public class DatosEmpresaValidator implements ConstraintValidator<DatosEmpresaValidos, SolicitudPublicaRequest> {
    @Override public boolean isValid(SolicitudPublicaRequest form, ConstraintValidatorContext context) {
        if (form == null || !"EMPRESA".equals(form.getTipoSolicitante())) return true;
        context.disableDefaultConstraintViolation();
        boolean valid = true;
        if (form.getRuc() == null || !form.getRuc().matches("[0-9]{11}")) {
            error(context, "ruc", "Escribe un RUC de 11 dígitos."); valid = false;
        }
        if (!texto(form.getRazonSocial(), 2, 250)) {
            error(context, "razonSocial", "Indica la razón social (2 a 250 caracteres, sin < ni >)."); valid = false;
        }
        if (form.getCorreo() == null || form.getCorreo().isBlank()) {
            error(context, "correo", "El correo es obligatorio para empresas."); valid = false;
        }
        if (form.getDireccionFiscal() != null && !form.getDireccionFiscal().isBlank() && !texto(form.getDireccionFiscal(), 1, 300)) {
            error(context, "direccionFiscal", "La dirección fiscal admite hasta 300 caracteres, sin < ni >."); valid = false;
        }
        if (form.getCargo() != null && !form.getCargo().isBlank() && !texto(form.getCargo(), 1, 80)) {
            error(context, "cargo", "El cargo admite hasta 80 caracteres, sin < ni >."); valid = false;
        }
        return valid;
    }
    private boolean texto(String value, int min, int max) {
        return value != null && value.trim().length() >= min && value.length() <= max
                && value.indexOf('<') < 0 && value.indexOf('>') < 0 && value.chars().noneMatch(Character::isISOControl);
    }
    private void error(ConstraintValidatorContext c, String field, String message) {
        c.buildConstraintViolationWithTemplate(message).addPropertyNode(field).addConstraintViolation();
    }
}
