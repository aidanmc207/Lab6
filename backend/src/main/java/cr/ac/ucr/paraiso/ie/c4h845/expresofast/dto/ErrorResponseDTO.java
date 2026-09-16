package cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponseDTO(
        LocalDateTime timestamp,
        int status,
        String error,
        String mensaje,
        String path,
        Map<String, String> errores) {

    public static ErrorResponseDTO de(int status, String error, String mensaje, String path) {
        return new ErrorResponseDTO(LocalDateTime.now(), status, error, mensaje, path, null);
    }

    public static ErrorResponseDTO deValidacion(int status, String error, String mensaje,
                                                String path, Map<String, String> errores) {
        return new ErrorResponseDTO(LocalDateTime.now(), status, error, mensaje, path, errores);
    }
}
