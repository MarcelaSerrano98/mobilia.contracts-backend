package com.mobilia.contracts.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Inmueble objeto del arrendamiento.
 *
 * <p>No se mapea la coleccion inversa de contratos: la aplicacion nunca navega
 * de inmueble a contratos, solo en sentido contrario. Mapear unicamente las
 * relaciones que se recorren evita cargas perezosas accidentales.</p>
 */
@Entity
@Table(name = "property")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Property extends BaseEntity {

    /** Direccion del inmueble. Es uno de los campos sobre los que se busca. */
    @Column(name = "address", nullable = false, length = 255)
    private String address;

    /**
     * Tipo de inmueble.
     *
     * <p>{@link EnumType#STRING} y nunca {@code ORDINAL}: con {@code ORDINAL}
     * se persiste la posicion de la constante, de modo que reordenar o insertar
     * un valor en el enum corrompe en silencio los datos ya almacenados.</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private PropertyType type;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
