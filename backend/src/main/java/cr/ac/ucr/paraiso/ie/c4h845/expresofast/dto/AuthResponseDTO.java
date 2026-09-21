package cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponseDTO(
        String token,
        String tipo,
        String username,
        String nombreCompleto,
        List<String> roles,
        LocalDateTime expirationTime,
        Integer conductorId) {

    /** Usuario sin conductor asociado (perfiles administrativos y de operacion). */
    public AuthResponseDTO(String token, String username, String nombreCompleto,
                           List<String> roles, LocalDateTime expirationTime) {
        this(token, "Bearer", username, nombreCompleto, roles, expirationTime, null);
    }

    public AuthResponseDTO(String token, String username, String nombreCompleto,
                           List<String> roles, LocalDateTime expirationTime, Integer conductorId) {
        this(token, "Bearer", username, nombreCompleto, roles, expirationTime, conductorId);
    }
}
