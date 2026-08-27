package com.mobilia.contracts.domain;

/**
 * Cardinalidades que exige el enunciado: exactamente un arrendatario,
 * uno o mas propietarios y cero o mas deudores solidarios.
 */
public enum PartyRole {

    ARRENDATARIO,
    PROPIETARIO,
    DEUDOR_SOLIDARIO
}
