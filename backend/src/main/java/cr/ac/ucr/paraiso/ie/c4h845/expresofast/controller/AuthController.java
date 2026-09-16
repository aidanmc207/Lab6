package cr.ac.ucr.paraiso.ie.c4h845.expresofast.controller;

import cr.ac.ucr.paraiso.ie.c4h845.expresofast.business.AuthService;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto.AuthRequestDTO;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto.AuthResponseDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Unico controlador publico: entrega el token JWT con el que se consume el resto de la API. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** POST /api/auth/login -> { token, tipo, username, nombreCompleto, roles, expirationTime }. */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody AuthRequestDTO solicitud) {
        return ResponseEntity.ok(authService.autenticar(solicitud));
    }

    /** GET /api/auth/perfil -> datos del portador del token (requiere estar autenticado). */
    @GetMapping("/perfil")
    public ResponseEntity<AuthResponseDTO> perfil(@AuthenticationPrincipal UserDetails usuario) {
        return ResponseEntity.ok(authService.perfil(usuario.getUsername(), null));
    }
}
