package cr.ac.ucr.paraiso.ie.c4h845.expresofast.config;

import cr.ac.ucr.paraiso.ie.c4h845.expresofast.domain.Rol;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.security.JwtAccessDeniedHandler;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.security.JwtAuthenticationEntryPoint;
import cr.ac.ucr.paraiso.ie.c4h845.expresofast.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // habilita @PreAuthorize en los controladores
public class SecurityConfig {

    /** Sin el prefijo ROLE_, porque hasAnyAuthority compara el valor literal del claim. */
    private static final String ADMIN = Rol.ADMIN;
    private static final String OPERADOR = Rol.OPERADOR;
    private static final String CONDUCTOR = Rol.CONDUCTOR;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          JwtAuthenticationEntryPoint authenticationEntryPoint,
                          JwtAccessDeniedHandler accessDeniedHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                // CSRF no aplica: no hay cookies de sesion, el token viaja en un encabezado.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)  // 401
                        .accessDeniedHandler(accessDeniedHandler))           // 403
                .authorizeHttpRequests(auth -> auth
                        // El pre-vuelo CORS viaja sin token: debe pasar antes que cualquier regla.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Unico endpoint publico de la matriz RBAC.
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers("/error").permitAll()

                        // Matriz RBAC
                        .requestMatchers(HttpMethod.GET, "/api/envios/optimizados")
                        .hasAnyAuthority(ADMIN, OPERADOR, CONDUCTOR)
                        .requestMatchers(HttpMethod.GET, "/api/envios/resumen")
                        .hasAnyAuthority(ADMIN, OPERADOR, CONDUCTOR)
                        .requestMatchers(HttpMethod.GET, "/api/envios/*/bitacora")
                        .hasAnyAuthority(ADMIN, OPERADOR)
                        .requestMatchers(HttpMethod.GET, "/api/envios", "/api/envios/*")
                        .hasAnyAuthority(ADMIN, OPERADOR, CONDUCTOR)
                        .requestMatchers(HttpMethod.POST, "/api/envios")
                        .hasAnyAuthority(ADMIN, OPERADOR)
                        .requestMatchers(HttpMethod.PATCH, "/api/envios/*/estado")
                        .hasAnyAuthority(ADMIN, CONDUCTOR)
                        .requestMatchers(HttpMethod.PATCH, "/api/envios/vehiculo/*/estado")
                        .hasAuthority(ADMIN)
                        .requestMatchers("/api/vehiculos/**").hasAuthority(ADMIN)
                        .requestMatchers("/api/catalogos/**")
                        .hasAnyAuthority(ADMIN, OPERADOR, CONDUCTOR)

                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** BCrypt con factor de trabajo 10 (el mismo que produce los hashes de las semillas SQL). */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuracion)
            throws Exception {
        return configuracion.getAuthenticationManager();
    }
}
