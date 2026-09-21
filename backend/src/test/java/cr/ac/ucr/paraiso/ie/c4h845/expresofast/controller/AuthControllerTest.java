package cr.ac.ucr.paraiso.ie.c4h845.expresofast.controller;

import cr.ac.ucr.paraiso.ie.c4h845.expresofast.business.AuthService;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.config.SecurityConfig;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.domain.Rol;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto.AuthRequestDTO;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto.AuthResponseDTO;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Prueba de corte del controlador de autenticacion (unico endpoint publico de la API). */
@WebMvcTest(controllers = AuthController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuthController - contrato HTTP de /api/auth/login")
class AuthControllerTest {

    private static final String TOKEN = "token.jwt.simulado";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("POST /api/auth/login con credenciales correctas retorna 200 OK y el token JWT")
    void login_CredencialesCorrectas_Retorna200ConToken() throws Exception {
        when(authService.autenticar(any(AuthRequestDTO.class))).thenReturn(new AuthResponseDTO(
                TOKEN, "admin", "Administrador ExpresoFast",
                List.of(Rol.ADMIN), LocalDateTime.of(2026, 9, 16, 18, 0)));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"Admin123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(TOKEN))
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.roles[0]").value(Rol.ADMIN));
    }

    @Test
    @DisplayName("POST /api/auth/login con credenciales incorrectas retorna 401 Unauthorized")
    void login_CredencialesIncorrectas_Retorna401() throws Exception {
        when(authService.autenticar(any(AuthRequestDTO.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"clave-incorrecta\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensaje").value("Usuario o contrasena incorrectos"));
    }

    @Test
    @DisplayName("POST /api/auth/login con una cuenta inactiva retorna 401 Unauthorized")
    void login_CuentaInactiva_Retorna401() throws Exception {
        when(authService.autenticar(any(AuthRequestDTO.class)))
                .thenThrow(new DisabledException("User is disabled"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"operador\",\"password\":\"Operador123!\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("La cuenta esta inactiva. Contacte al administrador"));
    }

    @Test
    @DisplayName("POST /api/auth/login con campos vacios retorna 400 Bad Request")
    void login_PayloadInvalido_Retorna400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errores.username").exists())
                .andExpect(jsonPath("$.errores.password").exists());
    }
}
