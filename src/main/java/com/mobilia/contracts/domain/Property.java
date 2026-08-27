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
 * Sin coleccion inversa de contratos a proposito: nadie navega de inmueble a
 * contratos, y mapear solo lo que se recorre evita cargas perezosas accidentales.
 */
@Entity
@Table(name = "property")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Property extends BaseEntity {

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    /**
     * {@code STRING} y nunca {@code ORDINAL} (que es el valor por defecto):
     * {@code ORDINAL} guarda la posicion, asi que reordenar el enum corrompe
     * en silencio los datos existentes.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private PropertyType type;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
