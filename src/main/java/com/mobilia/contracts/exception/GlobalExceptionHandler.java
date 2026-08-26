package com.mobilia.contracts.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

/**
 * Traduce cualquier excepcion que escape de los controladores a una respuesta
 * JSON con el formato de {@link ApiError}.
 *
 * <p>Centralizar el tratamiento de errores mantiene los controladores libres de
 * bloques {@code try/catch} y garantiza que el front-end reciba siempre la misma
 * estructura, sea cual sea el fallo.</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Texto de busqueda ausente o mas corto que el minimo configurado. */
    @ExceptionHandler(InvalidSearchQueryException.class)
    public ResponseEntity<ApiError> handleInvalidSearchQuery(
            InvalidSearchQueryException exception, HttpServletRequest request) {

        return build(HttpStatus.BAD_REQUEST, exception.getMessage(), request, List.of());
    }

    /** Falta un parametro obligatorio en la peticion, por ejemplo {@code q}. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParameter(
            MissingServletRequestParameterException exception, HttpServletRequest request) {

        String message = "Falta el parametro obligatorio '%s'.".formatted(exception.getParameterName());
        return build(HttpStatus.BAD_REQUEST, message, request, List.of());
    }

    /** Un parametro incumple las restricciones declaradas en el controlador. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(
            ConstraintViolationException exception, HttpServletRequest request) {

        List<String> details = exception.getConstraintViolations().stream()
                .map(violation -> "%s: %s".formatted(violation.getPropertyPath(), violation.getMessage()))
                .toList();

        return build(HttpStatus.BAD_REQUEST, "Parametros de busqueda invalidos.", request, details);
    }

    /** El valor recibido no puede convertirse al tipo esperado, p. ej. page=abc. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception, HttpServletRequest request) {

        String message = "El parametro '%s' no admite el valor '%s'."
                .formatted(exception.getName(), exception.getValue());
        return build(HttpStatus.BAD_REQUEST, message, request, List.of());
    }

    /**
     * Red de seguridad para cualquier fallo no previsto.
     *
     * <p>Se registra la traza completa en el log pero no se devuelve al cliente:
     * exponerla filtraria detalles internos de la aplicacion.</p>
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("Error no controlado en {}", request.getRequestURI(), exception);

        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Se produjo un error inesperado. Intentelo de nuevo mas tarde.",
                request,
                List.of());
    }

    private ResponseEntity<ApiError> build(
            HttpStatus status, String message, HttpServletRequest request, List<String> details) {

        ApiError body = ApiError.of(
                status.value(), status.getReasonPhrase(), message, request.getRequestURI(), details);

        return ResponseEntity.status(status).body(body);
    }
}
