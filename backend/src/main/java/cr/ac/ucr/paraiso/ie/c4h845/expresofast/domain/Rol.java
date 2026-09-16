package cr.ac.ucr.paraiso.ie.c4h845.expresofast.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/** Rol del sistema para el control de acceso basado en roles (RBAC). */
@Entity
@Table(name = "Rol")
public class Rol implements Serializable {

    public static final String ADMIN = "ROLE_ADMIN";
    public static final String OPERADOR = "ROLE_OPERADOR";
    public static final String CONDUCTOR = "ROLE_CONDUCTOR";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rol_id")
    private Integer id;

    @NotBlank(message = "El nombre del rol es obligatorio")
    @Size(max = 30)
    @Column(name = "nombre_rol", nullable = false, length = 30, unique = true)
    private String nombreRol;

    public Rol() {
    }

    public Rol(String nombreRol) {
        this.nombreRol = nombreRol;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNombreRol() {
        return nombreRol;
    }

    public void setNombreRol(String nombreRol) {
        this.nombreRol = nombreRol;
    }
}
