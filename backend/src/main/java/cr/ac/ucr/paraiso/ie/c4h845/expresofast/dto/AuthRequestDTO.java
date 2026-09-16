package cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cuerpo del POST /api/auth/login. */
public class AuthRequestDTO {

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(max = 50, message = "El nombre de usuario no puede superar 50 caracteres")
    private String username;

    @NotBlank(message = "La contrasena es obligatoria")
    @Size(max = 100, message = "La contrasena no puede superar 100 caracteres")
    private String password;

    public AuthRequestDTO() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
