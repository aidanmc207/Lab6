package cr.ac.ucr.paraiso.ie.c4h845.expresofast.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "EmpresaLogistica")
public class EmpresaLogistica implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "empresa_id")
    private Integer id;

    @NotBlank(message = "El nombre de la empresa es obligatorio")
    @Size(max = 100)
    @Column(name = "nombre", nullable = false, length = 100, unique = true)
    private String nombre;

    @NotBlank(message = "La cedula juridica es obligatoria")
    @Size(max = 20)
    @Column(name = "cedula_juridica", nullable = false, length = 20, unique = true)
    private String cedulaJuridica;

    @NotBlank(message = "El telefono es obligatorio")
    @Size(max = 20)
    @Column(name = "telefono", nullable = false, length = 20)
    private String telefono;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    /** Lado inverso 1:N. @JsonIgnore corta la recursion infinita al serializar. */
    @JsonIgnore
    @OneToMany(mappedBy = "empresa", fetch = FetchType.LAZY)
    private List<Vehiculo> vehiculos = new ArrayList<>();

    public EmpresaLogistica() {
    }

    @PrePersist
    private void asignarFechaRegistro() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCedulaJuridica() {
        return cedulaJuridica;
    }

    public void setCedulaJuridica(String cedulaJuridica) {
        this.cedulaJuridica = cedulaJuridica;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public List<Vehiculo> getVehiculos() {
        return vehiculos;
    }

    public void setVehiculos(List<Vehiculo> vehiculos) {
        this.vehiculos = vehiculos;
    }
}
