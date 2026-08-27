package com.mobilia.contracts.domain;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.hibernate.Hibernate;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * {@code @MappedSuperclass} y no herencia de entidades: las hijas comparten el
 * mapeo pero cada una vive en su propia tabla, sin jerarquia en la base de datos.
 */
@MappedSuperclass
@Getter
public abstract class BaseEntity {

    /**
     * {@code IDENTITY} es la unica estrategia nativa de MySQL. A cambio,
     * Hibernate no puede agrupar los INSERT en lotes: necesita el id al insertar.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    /** La escribe MySQL con {@code DEFAULT CURRENT_TIMESTAMP}; aqui solo se lee. */
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Se compara solo por id, y no con {@code @EqualsAndHashCode} de Lombok,
     * porque comparar todos los campos forzaria la carga de las asociaciones
     * {@code LAZY}. {@code Hibernate.getClass()} desenvuelve el proxy que
     * Hibernate crea para una entidad perezosa.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) {
            return false;
        }
        BaseEntity that = (BaseEntity) other;
        return id != null && Objects.equals(id, that.getId());
    }

    /**
     * Constante por tipo: una entidad todavia sin id cambiaria de hash al
     * persistirse y se perderia dentro de un {@code HashSet}.
     */
    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
