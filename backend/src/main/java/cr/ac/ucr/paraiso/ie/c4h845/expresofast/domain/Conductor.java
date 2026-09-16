package cr.ac.ucr.paraiso.ie.c4h845.expresofast.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Conductor")
public class Conductor implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "conductor_id")
    private Integer id;

    @NotBlank(message = "El nombre del conductor es obligatorio")
    @Size(max = 50)
    @Column(name = "nombre", nullable = false, length = 50)
    private String nombre;

    @NotBlank(message = "Los apellidos del conductor son obligatorios")
    @Size(max = 50)
    @Column(name = "apellidos", nullable = false, length = 50)
    private String apellidos;

    @NotBlank(message = "La licencia es obligatoria")
    @Size(max = 20)
    @Column(name = "licencia", nullable = false, length = 20, unique = true)
    private String licencia;

    @NotBlank(message = "El telefono es obligatorio")
    @Size(max = 20)
    @Column(name = "telefono", nullable = false, length = 20)
    private String telefono;

    @JsonIgnore
    @OneToMany(mappedBy = "conductor", fetch = FetchType.LAZY)
    private List<Envio> envios = new ArrayList<>();

    public Conductor() {
    }

    @Transient
    public String getNombreCompleto() {
        return nombre + " " + apellidos;
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

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getLicencia() {
        return licencia;
    }

    public void setLicencia(String licencia) {
        this.licencia = licencia;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public List<Envio> getEnvios() {
        return envios;
    }

    public void setEnvios(List<Envio> envios) {
        this.envios = envios;
    }
}
