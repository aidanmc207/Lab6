package cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto;

import cr.ac.ucr.paraiso.ie.c4h845.expresofast.domain.Conductor;

/** Catalogo de conductores para poblar el formulario del tablero. */
public record ConductorDTO(
        Integer id,
        String nombre,
        String apellidos,
        String nombreCompleto,
        String licencia,
        String telefono) {

    public static ConductorDTO desde(Conductor c) {
        return new ConductorDTO(
                c.getId(),
                c.getNombre(),
                c.getApellidos(),
                c.getNombreCompleto(),
                c.getLicencia(),
                c.getTelefono());
    }
}
