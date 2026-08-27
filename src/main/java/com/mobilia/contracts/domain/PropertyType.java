package com.mobilia.contracts.domain;

/**
 * El nombre de cada constante es el valor que se guarda en
 * {@code property.type}, porque el mapeo usa {@code EnumType.STRING}:
 * renombrar una constante invalida los datos ya almacenados.
 */
public enum PropertyType {

    CASA,
    APARTAMENTO,
    LOCAL
}
