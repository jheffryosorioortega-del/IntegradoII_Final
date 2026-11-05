package pe.sgp.frontend.validation;

import java.text.Normalizer;

/** Reglas de validación de los datos de contacto (copia de las de ms-clientes y ms-solicitudes:
 *  cada servicio es dueño de su código). Aquí se usan en dos sitios:
 *   1) como validación del formulario en el servidor (aviso inmediato al usuario);
 *   2) como atributo "pattern" de los campos HTML (ver ReglasWeb), para que el navegador avise antes de enviar.
 *  Ninguna de las dos protege de verdad: la validación que cuenta es la de los microservicios. */
public final class Reglas {

    private Reglas() { }

    private static final String LETRAS = "A-Za-zÁÉÍÓÚÜÑáéíóúüñ";

    /** Solo letras (con tildes y ñ), espacios, apóstrofo, punto y guion. 2 a 60 caracteres. Sin números. */
    public static final String NOMBRE = "^[" + LETRAS + "][" + LETRAS + "'.\\- ]{1,59}$";
    public static final String MSG_NOMBRE =
            "El nombre solo admite letras y espacios (2 a 60 caracteres). No se permiten números ni símbolos.";

    /** Solo dígitos: celular de 9 dígitos que empieza con 9, o teléfono fijo de 7 u 8 dígitos. */
    public static final String TELEFONO = "^(9\\d{8}|[1-8]\\d{6,7})$";
    public static final String MSG_TELEFONO =
            "Teléfono no válido: escribe solo números. Celular de 9 dígitos que empiece con 9, o fijo de 7 u 8 dígitos.";

    /** Letras, números, espacios y . , # ° / - (5 a 120 caracteres). Sin < > ni otros símbolos. */
    public static final String DIRECCION = "^[A-Za-z0-9ÁÉÍÓÚÜÑáéíóúüñ][A-Za-z0-9ÁÉÍÓÚÜÑáéíóúüñ .,#°\\/\\-]{4,119}$";
    public static final String MSG_DIRECCION =
            "La dirección admite letras, números, espacios y . , # ° / - (5 a 120 caracteres).";

    /** Opcional. */
    public static final String DISTRITO = "^$|^[" + LETRAS + "][" + LETRAS + " .'\\-]{1,39}$";
    public static final String MSG_DISTRITO = "El distrito solo admite letras y espacios (2 a 40 caracteres).";

    /** Opcional. */
    public static final String CORREO = "^$|^[A-Za-z0-9._%+\\-]{1,64}@[A-Za-z0-9.\\-]{1,100}\\.[A-Za-z]{2,}$";
    public static final String MSG_CORREO = "Correo no válido (ejemplo: nombre@dominio.com).";

    /** Texto libre del problema: sin los símbolos < y >. */
    public static final String DESCRIPCION = "^[^<>]*$";
    public static final String MSG_DESCRIPCION = "La descripción no puede contener los símbolos < ni >.";

    /** Normaliza texto libre: Unicode NFC, caracteres de control fuera, espacios repetidos colapsados y recortado. */
    public static String limpiar(String s) {
        if (s == null) return null;
        return Normalizer.normalize(s, Normalizer.Form.NFC)
                .replaceAll("\\p{Cntrl}", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /** El teléfono solo se recorta: ni espacios ni guiones se "arreglan" en silencio. */
    public static String limpiarTelefono(String s) {
        return s == null ? null : s.trim();
    }
}
