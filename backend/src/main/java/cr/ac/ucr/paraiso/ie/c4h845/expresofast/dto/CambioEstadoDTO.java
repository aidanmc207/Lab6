package cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Cuerpo del PATCH /api/envios/{id}/estado; las observaciones quedan en la bitacora. */
public class CambioEstadoDTO {

    @NotBlank(message = "El nuevo estado es obligatorio")
    @Pattern(regexp = "^(PENDIENTE|EN_TRANSITO|ENTREGADO|CANCELADO)$",
            message = "Estado invalido. Valores permitidos: PENDIENTE, EN_TRANSITO, ENTREGADO, CANCELADO")
    private String nuevoEstado;

    @Size(max = 250, message = "Las observaciones no pueden superar 250 caracteres")
    private String observaciones;

    public CambioEstadoDTO() {
    }

    public String getNuevoEstado() {
        return nuevoEstado;
    }

    public void setNuevoEstado(String nuevoEstado) {
        this.nuevoEstado = nuevoEstado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}
