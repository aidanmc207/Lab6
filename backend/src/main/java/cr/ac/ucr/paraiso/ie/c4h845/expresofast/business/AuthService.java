package cr.ac.ucr.paraiso.ie.c4h845.expresofast.business;

import cr.ac.ucr.paraiso.ie.c4h845.expresofast.data.UsuarioRepository;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.domain.Usuario;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto.AuthRequestDTO;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto.AuthResponseDTO;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.exception.ResourceNotFoundException;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Autentica contra la tabla Usuario y emite el token JWT de la sesion. */
@Service
@Transactional(readOnly = true)
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UsuarioRepository usuarioRepository;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtTokenProvider tokenProvider,
                       UsuarioRepository usuarioRepository) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.usuarioRepository = usuarioRepository;
    }

    public AuthResponseDTO autenticar(AuthRequestDTO solicitud) {
        Authentication autenticacion = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        solicitud.getUsername(), solicitud.getPassword()));

        UserDetails detalles = (UserDetails) autenticacion.getPrincipal();
        String token = tokenProvider.generarToken(detalles);

        List<String> roles = detalles.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        Usuario usuario = usuarioRepository.findByUsername(detalles.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        return new AuthResponseDTO(
                token,
                usuario.getUsername(),
                usuario.getNombreCompleto(),
                roles,
                tokenProvider.obtenerExpiracion(token));
    }

    /** Datos del usuario que porta el token, para el encabezado del tablero. */
    public AuthResponseDTO perfil(String username, String token) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        return new AuthResponseDTO(
                token,
                usuario.getUsername(),
                usuario.getNombreCompleto(),
                usuario.getRoles().stream().map(r -> r.getNombreRol()).toList(),
                token == null ? null : tokenProvider.obtenerExpiracion(token));
    }
}
