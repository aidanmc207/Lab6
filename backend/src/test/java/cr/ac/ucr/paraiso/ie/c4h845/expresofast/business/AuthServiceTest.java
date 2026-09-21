package cr.ac.ucr.paraiso.ie.c4h845.expresofast.business;

import cr.ac.ucr.paraiso.ie.c4h845.expresofast.data.UsuarioRepository;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.domain.Rol;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.domain.Usuario;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto.AuthRequestDTO;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto.AuthResponseDTO;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.exception.ResourceNotFoundException;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** Pruebas unitarias de la emision del token JWT, con el AuthenticationManager simulado. */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService - autenticacion y emision de tokens")
class AuthServiceTest {

    private static final String TOKEN = "token.jwt.simulado";

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("autenticar con credenciales correctas retorna el token y los roles")
    void autenticar_CredencialesCorrectas_RetornaToken() {
        LocalDateTime expiracion = LocalDateTime.of(2026, 9, 16, 18, 0);

        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(autenticacion());
        when(tokenProvider.generarToken(any(UserDetails.class))).thenReturn(TOKEN);
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuario()));
        when(tokenProvider.obtenerExpiracion(TOKEN)).thenReturn(expiracion);

        AuthResponseDTO respuesta = authService.autenticar(solicitud("admin", "Admin123!"));

        assertEquals(TOKEN, respuesta.token());
        assertEquals("Bearer", respuesta.tipo());
        assertEquals("admin", respuesta.username());
        assertTrue(respuesta.roles().contains(Rol.ADMIN));
        assertEquals(expiracion, respuesta.expirationTime());
    }

    @Test
    @DisplayName("autenticar con credenciales incorrectas propaga BadCredentialsException")
    void autenticar_CredencialesIncorrectas_LanzaExcepcion() {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class,
                () -> authService.autenticar(solicitud("admin", "clave-incorrecta")));
    }

    @Test
    @DisplayName("autenticar con un usuario ausente en la base lanza ResourceNotFoundException")
    void autenticar_UsuarioNoRegistrado_LanzaExcepcion() {
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(autenticacion());
        when(tokenProvider.generarToken(any(UserDetails.class))).thenReturn(TOKEN);
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> authService.autenticar(solicitud("admin", "Admin123!")));
    }

    @Test
    @DisplayName("perfil sin token devuelve los datos del usuario sin expiracion")
    void perfil_SinToken_RetornaDatosDelUsuario() {
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuario()));

        AuthResponseDTO respuesta = authService.perfil("admin", null);

        assertEquals("Administrador ExpresoFast", respuesta.nombreCompleto());
        assertTrue(respuesta.roles().contains(Rol.ADMIN));
        assertNull(respuesta.expirationTime());
    }

    @Test
    @DisplayName("perfil con token devuelve la fecha de expiracion")
    void perfil_ConToken_RetornaExpiracion() {
        LocalDateTime expiracion = LocalDateTime.of(2026, 9, 16, 18, 0);

        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuario()));
        when(tokenProvider.obtenerExpiracion(TOKEN)).thenReturn(expiracion);

        AuthResponseDTO respuesta = authService.perfil("admin", TOKEN);

        assertEquals(expiracion, respuesta.expirationTime());
    }

    @Test
    @DisplayName("perfil de un usuario inexistente lanza ResourceNotFoundException")
    void perfil_UsuarioInexistente_LanzaExcepcion() {
        when(usuarioRepository.findByUsername("fantasma")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.perfil("fantasma", null));
    }

    // Fabricas de datos de prueba

    private AuthRequestDTO solicitud(String username, String password) {
        AuthRequestDTO solicitud = new AuthRequestDTO();
        solicitud.setUsername(username);
        solicitud.setPassword(password);
        return solicitud;
    }

    private Authentication autenticacion() {
        UserDetails detalles = new User("admin", "{bcrypt}hash",
                List.of(new SimpleGrantedAuthority(Rol.ADMIN)));
        return new UsernamePasswordAuthenticationToken(detalles, null, detalles.getAuthorities());
    }

    private Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setId(1);
        usuario.setUsername("admin");
        usuario.setNombreCompleto("Administrador ExpresoFast");
        usuario.setRoles(Set.of(new Rol(Rol.ADMIN)));
        return usuario;
    }
}
