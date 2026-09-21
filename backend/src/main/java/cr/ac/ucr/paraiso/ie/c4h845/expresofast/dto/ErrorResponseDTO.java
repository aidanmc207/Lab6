package cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Cuerpo unico de error de la API, servido como application/problem+json.
 * Los cinco primeros campos son los miembros estandar de RFC 7807; el resto son
 * extensiones propias que conserva el contrato publicado en entregas anteriores.
 * Nunca transporta stacktrace ni detalles internos del servidor.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponseDTO(
        String type,
        String title,
        int status,
        String detail,
        String instance,
        LocalDateTime timestamp,
        String error,
        String mensaje,
        String path,
        Map<String, String> errores) {

    public static ErrorResponseDTO de(String type, String title, int status,
                                      String error, String detalle, String path) {
        return new ErrorResponseDTO(type, title, status, detalle, path,
                LocalDateTime.now(), error, detalle, path, null);
    }

    public static ErrorResponseDTO deValidacion(String type, String title, int status,
                                                String error, String detalle, String path,
                                                Map<String, String> errores) {
        return new ErrorResponseDTO(type, title, status, detalle, path,
                LocalDateTime.now(), error, detalle, path, errores);
    }
}
