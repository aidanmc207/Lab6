package cr.ac.ucr.paraiso.ie.c4h845.expresofast.security;

import cr.ac.ucr.paraiso.ie.c4h845.expresofast.data.UsuarioRepository;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.domain.Rol;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.domain.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Puente entre la tabla Usuario y el modelo de seguridad de Spring. */
@Service
@Transactional(readOnly = true)
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Credenciales invalidas")); // mensaje generico: no revela si el usuario existe

        List<GrantedAuthority> authorities = usuario.getRoles().stream()
                .map(Rol::getNombreRol)
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();

        return User.withUsername(usuario.getUsername())
                .password(usuario.getPasswordHash())
                .authorities(authorities)
                .disabled(Boolean.FALSE.equals(usuario.getActivo()))
                .build();
    }
}
