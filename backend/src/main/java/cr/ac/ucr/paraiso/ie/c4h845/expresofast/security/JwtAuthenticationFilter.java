package cr.ac.ucr.paraiso.ie.c4h845.expresofast.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIJO_BEARER = "Bearer ";

    private final JwtTokenProvider tokenProvider;
    private final UsuarioDetailsService usuarioDetailsService;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider,
                                   UsuarioDetailsService usuarioDetailsService) {
        this.tokenProvider = tokenProvider;
        this.usuarioDetailsService = usuarioDetailsService;
    }

    /** El pre-vuelo CORS viaja sin token: filtrarlo aqui evita el 401 en el OPTIONS. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return HttpMethod.OPTIONS.matches(request.getMethod());
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String token = extraerToken(request);

        if (token != null && tokenProvider.validarToken(token)
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            UserDetails usuario = usuarioDetailsService.loadUserByUsername(
                    tokenProvider.obtenerUsername(token));

            var autenticacion = new UsernamePasswordAuthenticationToken(
                    usuario, null, usuario.getAuthorities());
            autenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(autenticacion);
        }

        filterChain.doFilter(request, response);
    }

    private String extraerToken(HttpServletRequest request) {
        String encabezado = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (encabezado != null && encabezado.startsWith(PREFIJO_BEARER)) {
            String token = encabezado.substring(PREFIJO_BEARER.length()).trim();
            return token.isEmpty() ? null : token;
        }
        return null;
    }
}
