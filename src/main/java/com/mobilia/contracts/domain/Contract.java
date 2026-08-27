package com.mobilia.contracts.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * La columna generada {@code active_property_id} no se mapea a proposito: la
 * calcula MySQL para sostener el indice que impide dos contratos activos sobre
 * el mismo inmueble, y {@code ddl-auto: validate} no exige mapearlo todo.
 */
@Entity
@Table(name = "contract")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contract extends BaseEntity {

    @Column(name = "code", nullable = false, length = 30, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ContractStatus status;

    /** {@code LAZY} explicito: el defecto de {@code @ManyToOne} es {@code EAGER}. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "property_id",
            nullable = false,
            foreignKey = @jakarta.persistence.ForeignKey(name = "fk_contract_property")
    )
    private Property property;

    /**
     * {@code orphanRemoval}: una parte no existe fuera de su contrato, asi que
     * sacarla de la lista debe borrarla tambien de la base de datos.
     */
    @OneToMany(
            mappedBy = "contract",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<ContractParty> parties = new ArrayList<>();

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    /**
     * El lado propietario de la relacion es {@code ContractParty.contract}, no
     * esta lista: anadir solo a la lista no persistiria nada.
     */
    public void addParty(ContractParty party) {
        parties.add(party);
        party.setContract(this);
    }

    public List<ContractParty> getPartiesByRole(PartyRole role) {
        return parties.stream()
                .filter(party -> party.getRole() == role)
                .toList();
    }
}
