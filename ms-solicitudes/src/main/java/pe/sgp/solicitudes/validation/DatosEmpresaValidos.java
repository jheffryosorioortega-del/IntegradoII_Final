package pe.sgp.solicitudes.validation;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DatosEmpresaValidator.class)
public @interface DatosEmpresaValidos {
    String message() default "Revisa los datos de la empresa";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
