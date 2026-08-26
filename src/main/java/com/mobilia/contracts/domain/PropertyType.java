package com.mobilia.contracts.domain;

/**
 * Tipos de inmueble admitidos por el enunciado.
 *
 * <p>Los nombres de las constantes coinciden exactamente con los valores
 * almacenados en la columna {@code property.type}, porque el mapeo usa
 * {@link jakarta.persistence.EnumType#STRING}.</p>
 */
public enum PropertyType {

    CASA,
    APARTAMENTO,
    LOCAL
}
