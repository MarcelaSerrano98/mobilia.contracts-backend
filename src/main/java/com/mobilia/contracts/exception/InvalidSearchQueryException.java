package com.mobilia.contracts.exception;

/**
 * El texto de busqueda recibido no cumple las condiciones minimas para
 * ejecutar la consulta.
 *
 * <p>Se declara como excepcion no comprobada ({@link RuntimeException}) porque
 * representa un error del cliente que el codigo llamante no puede reparar; la
 * traduce a una respuesta HTTP 400 el
 * {@link com.mobilia.contracts.exception.GlobalExceptionHandler}.</p>
 */
public class InvalidSearchQueryException extends RuntimeException {

    public InvalidSearchQueryException(String message) {
        super(message);
    }
}
