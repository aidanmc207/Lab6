package cr.ac.ucr.paraiso.ie.c4h845.expresofast.exception;

import cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/** Traduce toda excepcion a un unico cuerpo application/problem+json (RFC 7807). */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Identificadores estables del tipo de problema (miembro "type" de RFC 7807).
    private static final String TIPO_VALIDACION = "urn:expresofast:error:validacion";
    private static final String TIPO_NO_ENCONTRADO = "urn:expresofast:error:recurso-no-encontrado";
    private static final String TIPO_TRANSICION = "urn:expresofast:error:transicion-invalida";
    private static final String TIPO_REGLA_NEGOCIO = "urn:expresofast:error:regla-de-negocio";
    private static final String TIPO_CUERPO_ILEGIBLE = "urn:expresofast:error:cuerpo-ilegible";
    private static final String TIPO_CREDENCIALES = "urn:expresofast:error:credenciales";
    private static final String TIPO_ACCESO_DENEGADO = "urn:expresofast:error:acceso-denegado";
    private static final String TIPO_INTERNO = "urn:expresofast:error:interno";

    /** 400 con el detalle campo por campo de las anotaciones jakarta.validation. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> validacion(MethodArgumentNotValidException ex,
                                                       HttpServletRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errores.merge(error.getField(), error.getDefaultMessage(), (a, b) -> a + "; " + b);
        }

        return problema(HttpStatus.BAD_REQUEST).body(ErrorResponseDTO.deValidacion(
                TIPO_VALIDACION,
                "Datos de entrada invalidos",
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Los datos enviados no superaron la validacion",
                request.getRequestURI(),
                errores));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> noEncontrado(ResourceNotFoundException ex,
                                                         HttpServletRequest request) {
        return construir(TIPO_NO_ENCONTRADO, "Recurso no encontrado",
                HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    /** Secuencia de estados invalida -> 400. */
    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<ErrorResponseDTO> transicionInvalida(InvalidStateTransitionException ex,
                                                               HttpServletRequest request) {
        return construir(TIPO_TRANSICION, "Transicion de estado no permitida",
                HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorResponseDTO> reglaNegocio(ReglaNegocioException ex,
                                                         HttpServletRequest request) {
        return construir(TIPO_REGLA_NEGOCIO, "Regla de negocio incumplida",
                HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    /** JSON malformado o tipos incompatibles en el cuerpo de la peticion. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> cuerpoIlegible(HttpMessageNotReadableException ex,
                                                           HttpServletRequest request) {
        log.debug("Cuerpo de peticion ilegible: {}", ex.getMessage());
        return construir(TIPO_CUERPO_ILEGIBLE, "Cuerpo de la peticion ilegible",
                HttpStatus.BAD_REQUEST, "El cuerpo de la peticion no es un JSON valido", request);
    }

    /** Credenciales incorrectas o cuenta inactiva -> 401 (mensaje generico). */
    @ExceptionHandler({BadCredentialsException.class, DisabledException.class,
            AuthenticationException.class})
    public ResponseEntity<ErrorResponseDTO> autenticacion(AuthenticationException ex,
                                                          HttpServletRequest request) {
        String mensaje = ex instanceof DisabledException
                ? "La cuenta esta inactiva. Contacte al administrador"
                : "Usuario o contrasena incorrectos";
        return construir(TIPO_CREDENCIALES, "Autenticacion fallida",
                HttpStatus.UNAUTHORIZED, mensaje, request);
    }

    /** Rol insuficiente en un endpoint protegido -> 403. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> accesoDenegado(AccessDeniedException ex,
                                                           HttpServletRequest request) {
        return construir(TIPO_ACCESO_DENEGADO, "Acceso denegado", HttpStatus.FORBIDDEN,
                "Su rol no tiene permisos para ejecutar esta operacion", request);
    }

    /** Ultima red de seguridad: se registra el detalle y se responde 500 sin revelarlo. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> general(Exception ex, HttpServletRequest request) {
        log.error("Error inesperado en {}", request.getRequestURI(), ex);
        return construir(TIPO_INTERNO, "Error interno del servidor",
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrio un error interno. Intente de nuevo o contacte al administrador", request);
    }

    private ResponseEntity<ErrorResponseDTO> construir(String type, String title, HttpStatus estado,
                                                       String detalle, HttpServletRequest request) {
        return problema(estado).body(ErrorResponseDTO.de(type, title, estado.value(),
                estado.getReasonPhrase(), detalle, request.getRequestURI()));
    }

    private ResponseEntity.BodyBuilder problema(HttpStatus estado) {
        return ResponseEntity.status(estado).contentType(MediaType.APPLICATION_PROBLEM_JSON);
    }
}
