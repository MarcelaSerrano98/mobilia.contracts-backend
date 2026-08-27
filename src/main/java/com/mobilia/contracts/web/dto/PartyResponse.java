package com.mobilia.contracts.web.dto;

import com.mobilia.contracts.domain.Person;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Persona vinculada a un contrato")
public record PartyResponse(

        @Schema(description = "Nombres y apellidos completos", example = "Juan Carlos Pérez Gómez")
        String fullName,

        @Schema(description = "Documento de identidad", example = "1020304050")
        String documentNumber
) {

    public static PartyResponse from(Person person) {
        return new PartyResponse(person.getFullName(), person.getDocumentNumber());
    }
}
