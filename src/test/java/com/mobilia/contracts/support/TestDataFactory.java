package com.mobilia.contracts.support;

import com.mobilia.contracts.domain.Contract;
import com.mobilia.contracts.domain.ContractParty;
import com.mobilia.contracts.domain.ContractStatus;
import com.mobilia.contracts.domain.PartyRole;
import com.mobilia.contracts.domain.Person;
import com.mobilia.contracts.domain.Property;
import com.mobilia.contracts.domain.PropertyType;

/**
 * Constructores de objetos de dominio para los tests.
 *
 * <p>Concentrar aqui la creacion evita repetir el armado de un contrato con sus
 * partes en cada test y deja a la vista, en el propio test, solo el dato que
 * ese test considera relevante.</p>
 */
public final class TestDataFactory {

    private TestDataFactory() {
    }

    public static Person person(String firstName, String lastName, String document, String email) {
        return Person.builder()
                .firstName(firstName)
                .lastName(lastName)
                .documentNumber(document)
                .email(email)
                .build();
    }

    public static Property property(String address, PropertyType type) {
        return Property.builder()
                .address(address)
                .type(type)
                .build();
    }

    public static Contract contract(String code, ContractStatus status, Property property) {
        return Contract.builder()
                .code(code)
                .status(status)
                .property(property)
                .build();
    }

    /** Anade una parte al contrato manteniendo sincronizada la relacion. */
    public static ContractParty addParty(Contract contract, Person person, PartyRole role) {
        ContractParty party = ContractParty.builder()
                .person(person)
                .role(role)
                .build();
        contract.addParty(party);
        return party;
    }
}
