package cr.ac.ucr.paraiso.ie.c4h845.expresofast.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Envio express; hereda de AuditableEntity la gestion de fecha_creacion y fecha_modificacion. */
@Entity
@Table(name = "Envio")
public class Envio extends AuditableEntity {

    public static final String PENDIENTE = "PENDIENTE";
    public static final String EN_TRANSITO = "EN_TRANSITO";
    public static final String ENTREGADO = "ENTREGADO";
    public static final String CANCELADO = "CANCELADO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "envio_id")
    private Integer id;

    @NotBlank(message = "El codigo de rastreo es obligatorio")
    @Size(max = 30)
    @Column(name = "codigo_rastreo", nullable = false, length = 30, unique = true)
    private String codigoRastreo;

    @NotBlank(message = "La direccion de destino es obligatoria")
    @Size(max = 200)
    @Column(name = "direccion_destino", nullable = false, length = 200)
    private String direccionDestino;

    @NotNull(message = "El peso en kg es obligatorio")
    @DecimalMin(value = "0.01", message = "El peso debe ser mayor a cero")
    @Column(name = "peso_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal pesoKg;

    @NotNull(message = "El costo es obligatorio")
    @DecimalMin(value = "0.00", message = "El costo no puede ser negativo")
    @Column(name = "costo", nullable = false, precision = 10, scale = 2)
    private BigDecimal costo;

    @Column(name = "estado_envio", nullable = false, length = 20)
    private String estadoEnvio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehiculo_id", foreignKey = @ForeignKey(name = "FK_Envio_Vehiculo"))
    private Vehiculo vehiculo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conductor_id", foreignKey = @ForeignKey(name = "FK_Envio_Conductor"))
    private Conductor conductor;

    public Envio() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public String getEstadoEnvio() {
        return estadoEnvio;
    }

    public void setEstadoEnvio(String estadoEnvio) {
        this.estadoEnvio = estadoEnvio;
    }

    public Vehiculo getVehiculo() {
        return vehiculo;
    }

    public void setVehiculo(Vehiculo vehiculo) {
        this.vehiculo = vehiculo;
    }

    public Conductor getConductor() {
        return conductor;
    }

    public void setConductor(Conductor conductor) {
        this.conductor = conductor;
    }
}
