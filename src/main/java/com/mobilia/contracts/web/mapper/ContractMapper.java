package com.mobilia.contracts.web.mapper;

import com.mobilia.contracts.domain.Contract;
import com.mobilia.contracts.domain.ContractParty;
import com.mobilia.contracts.domain.PartyRole;
import com.mobilia.contracts.web.dto.ContractSearchResponse;
import com.mobilia.contracts.web.dto.PartyResponse;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * A mano y no con MapStruct: la transformacion no es campo a campo sino un
 * agrupamiento por rol, y el codigo explicito se lee y depura mejor que la
 * configuracion equivalente.
 */
@Component
public class ContractMapper {

    /** Orden fijo para que la tabla no cambie de disposicion entre consultas. */
    private static final Comparator<ContractParty> BY_PERSON_NAME =
            Comparator.comparing((ContractParty party) -> party.getPerson().getLastName())
                    .thenComparing(party -> party.getPerson().getFirstName());

    /**
     * Exige que el contrato llegue con sus asociaciones ya inicializadas (via
     * {@code findAllWithPartiesByIdIn}); si no, cada acceso dispara una consulta.
     */
    public ContractSearchResponse toSearchResponse(Contract contract) {
        return new ContractSearchResponse(
                contract.getCode(),
                contract.getStatus(),
                contract.getProperty().getAddress(),
                contract.getProperty().getType(),
                findTenant(contract),
                toPartyResponses(contract, PartyRole.PROPIETARIO),
                toPartyResponses(contract, PartyRole.DEUDOR_SOLIDARIO)
        );
    }

    /**
     * Devuelve {@code null} en vez de fallar si no hubiera arrendatario, para
     * que un dato incompleto no tumbe el resto de la busqueda.
     */
    private PartyResponse findTenant(Contract contract) {
        return contract.getParties().stream()
                .filter(party -> party.getRole() == PartyRole.ARRENDATARIO)
                .findFirst()
                .map(party -> PartyResponse.from(party.getPerson()))
                .orElse(null);
    }

    private List<PartyResponse> toPartyResponses(Contract contract, PartyRole role) {
        return contract.getParties().stream()
                .filter(party -> party.getRole() == role)
                .sorted(BY_PERSON_NAME)
                .map(party -> PartyResponse.from(party.getPerson()))
                .toList();
    }
}
