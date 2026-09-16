package cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto;

/** Contadores por estado que alimentan las tarjetas KPI del tablero. */
public record ResumenEnviosDTO(
        long total,
        long pendientes,
        long enTransito,
        long entregados,
        long cancelados) {
}
