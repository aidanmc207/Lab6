package cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** Payload de alta y edicion de vehiculos (/api/vehiculos, solo ROLE_ADMIN). */
public class VehiculoRequestDTO {

    @NotBlank(message = "La placa es obligatoria")
    @Pattern(regexp = "^[A-Z]{2,3}-\\d{3,4}$", message = "Formato invalido. Ejemplo: CRC-1001")
    private String placa;

    @NotNull(message = "La capacidad en kg es obligatoria")
    @Positive(message = "La capacidad debe ser mayor a cero")
    private BigDecimal capacidadKg;

    @NotBlank(message = "El estado del vehiculo es obligatorio")
    @Pattern(regexp = "^(DISPONIBLE|EN_RUTA|MANTENIMIENTO)$",
            message = "Estado invalido. Valores permitidos: DISPONIBLE, EN_RUTA, MANTENIMIENTO")
    private String estado;

    @NotNull(message = "Debe indicar la empresa propietaria")
    @Positive(message = "El id de la empresa debe ser positivo")
    private Integer empresaId;

    public VehiculoRequestDTO() {
    }

    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        this.placa = placa;
    }

    public BigDecimal getCapacidadKg() {
        return capacidadKg;
    }

    public void setCapacidadKg(BigDecimal capacidadKg) {
        this.capacidadKg = capacidadKg;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Integer getEmpresaId() {
        return empresaId;
    }

    public void setEmpresaId(Integer empresaId) {
        this.empresaId = empresaId;
    }
}
