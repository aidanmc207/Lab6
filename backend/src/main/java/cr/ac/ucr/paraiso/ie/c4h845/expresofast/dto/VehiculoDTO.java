package cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto;

import cr.ac.ucr.paraiso.ie.c4h845.expresofast.domain.Vehiculo;

import java.math.BigDecimal;

/** Catalogo de vehiculos para poblar el formulario del tablero. */
public record VehiculoDTO(
        Integer id,
        String placa,
        BigDecimal capacidadKg,
        String estado,
        Integer empresaId,
        String empresaNombre) {

    public static VehiculoDTO desde(Vehiculo v) {
        var empresa = v.getEmpresa();
        return new VehiculoDTO(
                v.getId(),
                v.getPlaca(),
                v.getCapacidadKg(),
                v.getEstado(),
                empresa != null ? empresa.getId() : null,
                empresa != null ? empresa.getNombre() : null);
    }
}
