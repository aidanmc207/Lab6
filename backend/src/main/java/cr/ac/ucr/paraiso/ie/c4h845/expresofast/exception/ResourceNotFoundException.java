package cr.ac.ucr.paraiso.ie.c4h845.expresofast.exception;

/** Se lanza cuando un id solicitado no existe en la base de datos (HTTP 404). */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String mensaje) {
        super(mensaje);
    }
}
