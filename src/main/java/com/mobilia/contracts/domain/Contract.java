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
 * Contrato de arrendamiento sobre un inmueble.
 *
 * <p>Las columnas generadas del esquema ({@code active_property_id}) no se
 * mapean: las calcula MySQL y solo existen para sostener el indice unico que
 * impide dos contratos activos sobre el mismo inmueble. La validacion de
 * esquema de Hibernate comprueba que las columnas mapeadas existan, no que se
 * mapeen todas las columnas de la tabla.</p>
 */
@Entity
@Table(name = "contract")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contract extends BaseEntity {

    /** Codigo alfanumerico del contrato. Es uno de los campos de busqueda. */
    @Column(name = "code", nullable = false, length = 30, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ContractStatus status;

    /**
     * Inmueble al que pertenece el contrato.
     *
     * <p>{@link FetchType#LAZY} de forma explicita: el valor por defecto de
     * {@code @ManyToOne} es {@code EAGER}, que dispara un JOIN adicional en
     * cada consulta aunque la direccion no se necesite.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "property_id",
            nullable = false,
            foreignKey = @jakarta.persistence.ForeignKey(name = "fk_contract_property")
    )
    private Property property;

    /**
     * Partes del contrato: arrendatario, propietarios y deudores solidarios.
     *
     * <p>{@code orphanRemoval = true} hace que retirar una parte de la lista
     * la elimine tambien de la base de datos, reflejando que una parte no tiene
     * existencia propia fuera de su contrato.</p>
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
     * Anade una parte manteniendo sincronizados los dos extremos de la
     * relacion bidireccional.
     *
     * <p>Sin este metodo es facil asignar solo un lado y que el cambio no se
     * persista, porque el lado propietario de la relacion es
     * {@link ContractParty#getContract()}, no esta coleccion.</p>
     */
    public void addParty(ContractParty party) {
        parties.add(party);
        party.setContract(this);
    }

    /** Devuelve las partes que ejercen el rol indicado, en el orden cargado. */
    public List<ContractParty> getPartiesByRole(PartyRole role) {
        return parties.stream()
                .filter(party -> party.getRole() == role)
                .toList();
    }
}
