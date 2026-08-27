package com.mobilia.contracts.exception;

/**
 * No comprobada porque es un error del cliente que quien llama no puede
 * reparar. {@code GlobalExceptionHandler} la traduce a un HTTP 400.
 */
public class InvalidSearchQueryException extends RuntimeException {

    public InvalidSearchQueryException(String message) {
        super(message);
    }
}
