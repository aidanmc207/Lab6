package cr.ac.ucr.paraiso.ie.c4h845.expresofast.exception;

import cr.ac.ucr.paraiso.ie.c4h845.expresofast.dto.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
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

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 400 con el detalle campo por campo de las anotaciones jakarta.validation. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> validacion(MethodArgumentNotValidException ex,
                                                       HttpServletRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errores.merge(error.getField(), error.getDefaultMessage(), (a, b) -> a + "; " + b);
        }

        return ResponseEntity.badRequest().body(ErrorResponseDTO.deValidacion(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Los datos enviados no superaron la validacion",
                request.getRequestURI(),
                errores));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> noEncontrado(ResourceNotFoundException ex,
                                                         HttpServletRequest request) {
        return construir(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    /** Reto autonomo: secuencia de estados invalida -> 400. */
    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<ErrorResponseDTO> transicionInvalida(InvalidStateTransitionException ex,
                                                               HttpServletRequest request) {
        return construir(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorResponseDTO> reglaNegocio(ReglaNegocioException ex,
                                                         HttpServletRequest request) {
        return construir(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    /** JSON malformado o tipos incompatibles en el cuerpo de la peticion. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> cuerpoIlegible(HttpMessageNotReadableException ex,
                                                           HttpServletRequest request) {
        log.debug("Cuerpo de peticion ilegible: {}", ex.getMessage());
        return construir(HttpStatus.BAD_REQUEST,
                "El cuerpo de la peticion no es un JSON valido", request);
    }

    /** Credenciales incorrectas o cuenta inactiva -> 401 (mensaje generico). */
    @ExceptionHandler({BadCredentialsException.class, DisabledException.class,
            AuthenticationException.class})
    public ResponseEntity<ErrorResponseDTO> autenticacion(AuthenticationException ex,
                                                          HttpServletRequest request) {
        String mensaje = ex instanceof DisabledException
                ? "La cuenta esta inactiva. Contacte al administrador"
                : "Usuario o contrasena incorrectos";
        return construir(HttpStatus.UNAUTHORIZED, mensaje, request);
    }

    /** Rol insuficiente en un endpoint protegido -> 403. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> accesoDenegado(AccessDeniedException ex,
                                                           HttpServletRequest request) {
        return construir(HttpStatus.FORBIDDEN,
                "Su rol no tiene permisos para ejecutar esta operacion", request);
    }

    /** Ultima red de seguridad: se registra el detalle y se responde 500 sin revelarlo. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> general(Exception ex, HttpServletRequest request) {
        log.error("Error inesperado en {}", request.getRequestURI(), ex);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrio un error interno. Intente de nuevo o contacte al administrador", request);
    }

    private ResponseEntity<ErrorResponseDTO> construir(HttpStatus estado, String mensaje,
                                                       HttpServletRequest request) {
        return ResponseEntity.status(estado).body(ErrorResponseDTO.de(
                estado.value(), estado.getReasonPhrase(), mensaje, request.getRequestURI()));
    }
}
