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
 * Traduce entidades de dominio a los DTOs que expone la API.
 *
 * <p>El mapeo se escribe a mano en lugar de generarlo con MapStruct: la
 * transformacion no es campo a campo, sino que agrupa las partes por rol, y el
 * codigo explicito resulta mas facil de leer y de depurar que la configuracion
 * equivalente.</p>
 */
@Component
public class ContractMapper {

    /** Orden estable de las partes dentro de cada rol, por apellidos y nombres. */
    private static final Comparator<ContractParty> BY_PERSON_NAME =
            Comparator.comparing((ContractParty party) -> party.getPerson().getLastName())
                    .thenComparing(party -> party.getPerson().getFirstName());

    /**
     * Construye la fila de la tabla de resultados a partir de un contrato ya
     * cargado con sus partes.
     *
     * <p>Presupone que el contrato llega con las asociaciones inicializadas
     * (vease {@code ContractRepository#findAllWithPartiesByIdIn}); de lo
     * contrario cada acceso dispararia una consulta adicional.</p>
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
     * Devuelve el arrendatario del contrato.
     *
     * <p>El esquema garantiza que no puede haber mas de uno. Se devuelve
     * {@code null} en lugar de lanzar una excepcion si faltara, para que un dato
     * incompleto no impida mostrar el resto de la busqueda.</p>
     */
    private PartyResponse findTenant(Contract contract) {
        return contract.getParties().stream()
                .filter(party -> party.getRole() == PartyRole.ARRENDATARIO)
                .findFirst()
                .map(party -> PartyResponse.from(party.getPerson()))
                .orElse(null);
    }

    /** Partes del rol indicado, ordenadas por nombre. Lista vacia si no hay. */
    private List<PartyResponse> toPartyResponses(Contract contract, PartyRole role) {
        return contract.getParties().stream()
                .filter(party -> party.getRole() == role)
                .sorted(BY_PERSON_NAME)
                .map(party -> PartyResponse.from(party.getPerson()))
                .toList();
    }
}
