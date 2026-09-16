package cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto;

import cr.ac.ucr.paraiso.ie.c4h845.expresofast.domain.Envio;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EnvioResponseDTO(
        Integer id,
        String codigoRastreo,
        String direccionDestino,
        BigDecimal pesoKg,
        BigDecimal costo,
        String estadoEnvio,
        String placaVehiculo,
        String nombreConductor,
        Integer vehiculoId,
        BigDecimal vehiculoCapacidadKg,
        String vehiculoEstado,
        Integer empresaId,
        String empresaNombre,
        Integer conductorId,
        String conductorLicencia,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaModificacion) {

    /** Requiere una entidad ya cargada con JOIN FETCH; con proxies LAZY daria LazyInitializationException. */
    public static EnvioResponseDTO desde(Envio e) {
        var vehiculo = e.getVehiculo();
        var empresa = vehiculo != null ? vehiculo.getEmpresa() : null;
        var conductor = e.getConductor();

        return new EnvioResponseDTO(
                e.getId(),
                e.getCodigoRastreo(),
                e.getDireccionDestino(),
                e.getPesoKg(),
                e.getCosto(),
                e.getEstadoEnvio(),
                vehiculo != null ? vehiculo.getPlaca() : null,
                conductor != null ? conductor.getNombreCompleto() : null,
                vehiculo != null ? vehiculo.getId() : null,
                vehiculo != null ? vehiculo.getCapacidadKg() : null,
                vehiculo != null ? vehiculo.getEstado() : null,
                empresa != null ? empresa.getId() : null,
                empresa != null ? empresa.getNombre() : null,
                conductor != null ? conductor.getId() : null,
                conductor != null ? conductor.getLicencia() : null,
                e.getFechaCreacion(),
                e.getFechaModificacion());
    }
}
