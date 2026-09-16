package cr.ac.ucr.paraiso.ie.c4h845.expresofast.exception;

/** Violacion de una regla del dominio logistico (HTTP 400). */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
