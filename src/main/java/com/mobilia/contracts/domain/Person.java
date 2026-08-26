package com.mobilia.contracts.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Persona que puede intervenir en uno o varios contratos.
 *
 * <p>Se almacena una unica vez aunque participe en varios contratos y con
 * roles distintos; el vinculo se establece a traves de {@link ContractParty}.</p>
 */
@Entity
@Table(
        name = "person",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_person_document_number",
                columnNames = "document_number"
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Person extends BaseEntity {

    /** Nombres de la persona. */
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    /** Apellidos de la persona. */
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    /** Documento de identidad. Clave natural del negocio, por eso es unico. */
    @Column(name = "document_number", nullable = false, length = 30)
    private String documentNumber;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    /**
     * Nombre completo tal y como debe mostrarse en la tabla de resultados.
     *
     * <p>Se marca {@code @Transient} de forma implicita al no llevar
     * {@code @Column}: es un valor derivado, no una columna de la tabla.</p>
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }
}
