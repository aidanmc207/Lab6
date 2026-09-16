package cr.ac.ucr.paraiso.ie.c4h845.expresofast.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Vehiculo")
public class Vehiculo implements Serializable {

    public static final String DISPONIBLE = "DISPONIBLE";
    public static final String EN_RUTA = "EN_RUTA";
    public static final String MANTENIMIENTO = "MANTENIMIENTO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vehiculo_id")
    private Integer id;

    @NotBlank(message = "La placa es obligatoria")
    @Size(max = 15)
    @Column(name = "placa", nullable = false, length = 15, unique = true)
    private String placa;

    @NotNull(message = "La capacidad en kg es obligatoria")
    @DecimalMin(value = "0.01", message = "La capacidad debe ser mayor a cero")
    @Column(name = "capacidad_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal capacidadKg;

    @NotBlank(message = "El estado del vehiculo es obligatorio")
    @Size(max = 20)
    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    /** Lado propietario N:1; LAZY porque la empresa solo se carga via JOIN FETCH cuando hace falta. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", foreignKey = @ForeignKey(name = "FK_Vehiculo_Empresa"))
    private EmpresaLogistica empresa;

    @JsonIgnore
    @OneToMany(mappedBy = "vehiculo", fetch = FetchType.LAZY)
    private List<Envio> envios = new ArrayList<>();

    public Vehiculo() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public EmpresaLogistica getEmpresa() {
        return empresa;
    }

    public void setEmpresa(EmpresaLogistica empresa) {
        this.empresa = empresa;
    }

    public List<Envio> getEnvios() {
        return envios;
    }

    public void setEnvios(List<Envio> envios) {
        this.envios = envios;
    }
}
