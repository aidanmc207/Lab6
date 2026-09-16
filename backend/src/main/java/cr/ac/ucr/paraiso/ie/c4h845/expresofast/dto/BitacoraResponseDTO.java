package cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto;

import cr.ac.ucr.paraiso.ie.c4h845.expresofast.domain.BitacoraEnvio;

import java.time.LocalDateTime;

/** Fila del historial de auditoria tal como la consume el modal de bitacora. */
public record BitacoraResponseDTO(
        Integer id,
        String estadoAnterior,
        String estadoNuevo,
        LocalDateTime fechaCambio,
        String usuario,
        String observaciones) {

    /** La entidad debe venir con JOIN FETCH del usuario (ver BitacoraEnvioRepository). */
    public static BitacoraResponseDTO desde(BitacoraEnvio b) {
        return new BitacoraResponseDTO(
                b.getId(),
                b.getEstadoAnterior(),
                b.getEstadoNuevo(),
                b.getFechaCambio(),
                b.getUsuario() != null ? b.getUsuario().getUsername() : null,
                b.getObservaciones());
    }
}
