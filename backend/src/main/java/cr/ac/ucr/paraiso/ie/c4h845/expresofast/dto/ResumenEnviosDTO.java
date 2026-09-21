package cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto;

/** Contadores que alimentan las tarjetas KPI de la consola de operacion. */
public record ResumenEnviosDTO(
        long total,
        long pendientes,
        long enTransito,
        long entregados,
        long cancelados,
        long vehiculosActivos) {

    /** Resumen sin el dato de flota, para los consumidores que solo miran envios. */
    public ResumenEnviosDTO(long total, long pendientes, long enTransito,
                            long entregados, long cancelados) {
        this(total, pendientes, enTransito, entregados, cancelados, 0L);
    }
}
