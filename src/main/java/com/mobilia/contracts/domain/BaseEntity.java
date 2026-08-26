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
 * Estado y comportamiento comunes a todas las entidades del dominio.
 *
 * <p>Se declara {@code @MappedSuperclass} y no {@code @Entity}: Hibernate hereda
 * el mapeo de los atributos a cada tabla hija, pero no crea ninguna tabla ni
 * jerarquia de herencia en la base de datos.</p>
 */
@MappedSuperclass
@Getter
public abstract class BaseEntity {

    /**
     * Clave primaria sustituta.
     *
     * <p>{@link GenerationType#IDENTITY} delega la generacion en la columna
     * {@code AUTO_INCREMENT} de MySQL. Es la unica estrategia que MySQL soporta
     * de forma nativa; su contrapartida es que Hibernate no puede agrupar los
     * INSERT en lotes, porque necesita el identificador inmediatamente.</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    /**
     * Marca de creacion. La escribe la base de datos mediante
     * {@code DEFAULT CURRENT_TIMESTAMP}, por eso el mapeo es de solo lectura.
     */
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Igualdad basada exclusivamente en el identificador.
     *
     * <p>No se usa {@code @EqualsAndHashCode} de Lombok ni {@code @Data}: la
     * implementacion por defecto compara todos los campos, lo que fuerza la
     * carga de las asociaciones {@code LAZY} y puede provocar consultas
     * inesperadas o {@code LazyInitializationException}.</p>
     *
     * <p>Se compara con {@link Hibernate#getClass(Object)} en lugar de
     * {@code getClass()} porque una entidad cargada de forma perezosa es en
     * realidad un proxy de una subclase generada en tiempo de ejecucion.</p>
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
     * Valor constante por tipo.
     *
     * <p>Una entidad transitoria (todavia sin {@code id}) recibiria un hash
     * distinto tras persistirse, rompiendo su presencia dentro de un
     * {@link java.util.HashSet}. Devolver el hash de la clase mantiene el
     * contrato de {@code hashCode} durante todo el ciclo de vida.</p>
     */
    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
