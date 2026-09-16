package cr.ac.ucr.paraiso.ie.c4h845.expresofast.domain;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/** Bitacora de auditoria: una fila por cada cambio de estado de un envio. */
@Entity
@Table(name = "BitacoraEnvio")
public class BitacoraEnvio implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bitacora_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "envio_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_Bitacora_Envio"))
    private Envio envio;

    @Column(name = "estado_anterior", nullable = false, length = 20)
    private String estadoAnterior;

    @Column(name = "estado_nuevo", nullable = false, length = 20)
    private String estadoNuevo;

    @Column(name = "fecha_cambio", nullable = false)
    private LocalDateTime fechaCambio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_Bitacora_Usuario"))
    private Usuario usuario;

    @Column(name = "observaciones", length = 250)
    private String observaciones;

    public BitacoraEnvio() {
    }

    public BitacoraEnvio(Envio envio, String estadoAnterior, String estadoNuevo,
                         Usuario usuario, String observaciones) {
        this.envio = envio;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.usuario = usuario;
        this.observaciones = observaciones;
        this.fechaCambio = LocalDateTime.now();
    }

    /** Respaldo por si la entidad se arma con el constructor vacio. */
    @PrePersist
    private void asignarFechaCambio() {
        if (fechaCambio == null) {
            fechaCambio = LocalDateTime.now();
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Envio getEnvio() {
        return envio;
    }

    public void setEnvio(Envio envio) {
        this.envio = envio;
    }

    public String getEstadoAnterior() {
        return estadoAnterior;
    }

    public void setEstadoAnterior(String estadoAnterior) {
        this.estadoAnterior = estadoAnterior;
    }

    public String getEstadoNuevo() {
        return estadoNuevo;
    }

    public void setEstadoNuevo(String estadoNuevo) {
        this.estadoNuevo = estadoNuevo;
    }

    public LocalDateTime getFechaCambio() {
        return fechaCambio;
    }

    public void setFechaCambio(LocalDateTime fechaCambio) {
        this.fechaCambio = fechaCambio;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}
