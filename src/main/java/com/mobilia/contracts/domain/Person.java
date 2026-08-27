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

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    /** Clave natural del negocio: identifica a la persona, de ahi el UNIQUE. */
    @Column(name = "document_number", nullable = false, length = 30)
    private String documentNumber;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    /** No necesita {@code @Transient}: el mapeo es por campo, no por getter. */
    public String getFullName() {
        return firstName + " " + lastName;
    }
}
