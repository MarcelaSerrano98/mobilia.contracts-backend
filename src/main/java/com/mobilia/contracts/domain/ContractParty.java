package com.mobilia.contracts.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tabla de union con atributo. Poner los roles como columnas de {@code contract}
 * ({@code propietario_1}, {@code propietario_2}...) seria un grupo repetitivo:
 * viola la 1FN y limitaria de forma artificial el numero de partes.
 */
@Entity
@Table(
        name = "contract_party",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_contract_party_unique_role",
                columnNames = {"contract_id", "person_id", "role"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractParty extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "contract_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_contract_party_contract")
    )
    private Contract contract;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "person_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_contract_party_person")
    )
    private Person person;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private PartyRole role;
}
