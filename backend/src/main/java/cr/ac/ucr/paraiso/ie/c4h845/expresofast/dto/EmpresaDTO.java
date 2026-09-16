package cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto;

import cr.ac.ucr.paraiso.ie.c4h845.expresofast.domain.EmpresaLogistica;

import java.time.LocalDateTime;

/** Catalogo de empresas; evita devolver la entidad JPA con su coleccion de vehiculos. */
public record EmpresaDTO(
        Integer id,
        String nombre,
        String cedulaJuridica,
        String telefono,
        LocalDateTime fechaRegistro) {

    public static EmpresaDTO desde(EmpresaLogistica e) {
        return new EmpresaDTO(
                e.getId(),
                e.getNombre(),
                e.getCedulaJuridica(),
                e.getTelefono(),
                e.getFechaRegistro());
    }
}
