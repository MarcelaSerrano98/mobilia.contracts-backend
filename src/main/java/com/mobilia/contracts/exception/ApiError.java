package com.mobilia.contracts.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Cuerpo uniforme de toda respuesta de error de la API.
 *
 * <p>Un formato unico permite al front-end tratar los errores en un solo lugar
 * en vez de interpretar una estructura distinta segun el fallo.</p>
 */
@Schema(description = "Respuesta de error de la API")
public record ApiError(

        @Schema(description = "Instante en que se produjo el error")
        OffsetDateTime timestamp,

        @Schema(description = "Codigo de estado HTTP", example = "400")
        int status,

        @Schema(description = "Descripcion corta del estado HTTP", example = "Bad Request")
        String error,

        @Schema(description = "Mensaje orientado a la persona que usa la aplicacion")
        String message,

        @Schema(description = "Ruta que origino el error", example = "/api/v1/contracts/search")
        String path,

        @Schema(description = "Detalle por campo cuando el fallo es de validacion")
        List<String> details
) {

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(OffsetDateTime.now(), status, error, message, path, List.of());
    }

    public static ApiError of(int status, String error, String message, String path, List<String> details) {
        return new ApiError(OffsetDateTime.now(), status, error, message, path, details);
    }
}
