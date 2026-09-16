package cr.ac.ucr.paraiso.ie.c4h845.expresofast.exception;

public class InvalidStateTransitionException extends RuntimeException {

    private static final String PLANTILLA = "Transición de estado no permitida para el envío ";

    public InvalidStateTransitionException(String codigoRastreo) {
        super(PLANTILLA + codigoRastreo);
    }

    public InvalidStateTransitionException(String codigoRastreo, String detalle) {
        super(PLANTILLA + codigoRastreo + " (" + detalle + ")");
    }
}
