package com.mobilia.contracts.web.dto;

import com.mobilia.contracts.domain.Person;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Persona que interviene en un contrato, en la forma minima que necesita la
 * tabla de resultados.
 *
 * <p>Se modela como {@code record}: es un objeto de transporte inmutable y sin
 * comportamiento, exactamente el caso de uso para el que se disenno el tipo.</p>
 */
@Schema(description = "Persona vinculada a un contrato")
public record PartyResponse(

        @Schema(description = "Nombres y apellidos completos", example = "Juan Carlos Pérez Gómez")
        String fullName,

        @Schema(description = "Documento de identidad", example = "1020304050")
        String documentNumber
) {

    /** Construye la respuesta a partir de la entidad de dominio. */
    public static PartyResponse from(Person person) {
        return new PartyResponse(person.getFullName(), person.getDocumentNumber());
    }
}
