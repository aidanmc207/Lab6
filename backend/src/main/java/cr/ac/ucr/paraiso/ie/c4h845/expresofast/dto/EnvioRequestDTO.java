package cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class EnvioRequestDTO {

    @NotBlank(message = "El codigo de rastreo es obligatorio")
    @Pattern(regexp = "^EXP-\\d{4}$", message = "Formato invalido. Ejemplo: EXP-1234")
    private String codigoRastreo;

    @NotBlank(message = "La direccion de destino es obligatoria")
    @Size(max = 200, message = "La direccion no puede superar 200 caracteres")
    private String direccionDestino;

    @NotNull(message = "El peso en kg es obligatorio")
    @Positive(message = "El peso debe ser mayor a cero")
    private BigDecimal pesoKg;

    @NotNull(message = "El costo es obligatorio")
    @PositiveOrZero(message = "El costo no puede ser negativo")
    private BigDecimal costo;

    @NotNull(message = "Debe indicar el vehiculo asignado")
    @Positive(message = "El id del vehiculo debe ser positivo")
    private Integer vehiculoId;

    @NotNull(message = "Debe indicar el conductor asignado")
    @Positive(message = "El id del conductor debe ser positivo")
    private Integer conductorId;

    public EnvioRequestDTO() {
    }

    public String getCodigoRastreo() {
        return codigoRastreo;
    }

    public void setCodigoRastreo(String codigoRastreo) {
        this.codigoRastreo = codigoRastreo;
    }

    public String getDireccionDestino() {
        return direccionDestino;
    }

    public void setDireccionDestino(String direccionDestino) {
        this.direccionDestino = direccionDestino;
    }

    public BigDecimal getPesoKg() {
        return pesoKg;
    }

    public void setPesoKg(BigDecimal pesoKg) {
        this.pesoKg = pesoKg;
    }

    public BigDecimal getCosto() {
        return costo;
    }

    public void setCosto(BigDecimal costo) {
        this.costo = costo;
    }

    public Integer getVehiculoId() {
        return vehiculoId;
    }

    public void setVehiculoId(Integer vehiculoId) {
        this.vehiculoId = vehiculoId;
    }

    public Integer getConductorId() {
        return conductorId;
    }

    public void setConductorId(Integer conductorId) {
        this.conductorId = conductorId;
    }
}
