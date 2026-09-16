package cr.ac.ucr.paraiso.ie.c4h845.expresofast.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    /** Claim propio donde viajan las authorities para no releer la BD en cada peticion. */
    private static final String CLAIM_ROLES = "roles";

    private final SecretKey claveFirma;
    private final long vigenciaMs;

    public JwtTokenProvider(@Value("${expresofast.jwt.secret}") String secreto,
                            @Value("${expresofast.jwt.expiration-ms}") long vigenciaMs) {
        // HS256 exige al menos 256 bits de clave; si el secreto es corto el arranque falla.
        this.claveFirma = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.vigenciaMs = vigenciaMs;
    }

    /** Firma un token cuyo subject es el username y cuyo claim "roles" lleva el RBAC. */
    public String generarToken(UserDetails usuario) {
        Date emision = new Date();
        Date expiracion = new Date(emision.getTime() + vigenciaMs);

        List<String> roles = usuario.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return Jwts.builder()
                .subject(usuario.getUsername())
                .claim(CLAIM_ROLES, roles)
                .issuedAt(emision)
                .expiration(expiracion)
                .signWith(claveFirma, Jwts.SIG.HS256)
                .compact();
    }

    public String obtenerUsername(String token) {
        return leerClaims(token).getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> obtenerRoles(String token) {
        Object roles = leerClaims(token).get(CLAIM_ROLES);
        return roles instanceof List<?> lista ? (List<String>) lista : List.of();
    }

    public LocalDateTime obtenerExpiracion(String token) {
        Date expiracion = leerClaims(token).getExpiration();
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(expiracion.getTime()), ZoneId.systemDefault());
    }

    /** Valida firma y vigencia. Devuelve false en vez de propagar: el filtro solo decide pasar o no. */
    public boolean validarToken(String token) {
        try {
            leerClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.debug("Token JWT expirado: {}", ex.getMessage());
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Token JWT invalido: {}", ex.getMessage());
        }
        return false;
    }

    private Claims leerClaims(String token) {
        return Jwts.parser()
                .verifyWith(claveFirma)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
