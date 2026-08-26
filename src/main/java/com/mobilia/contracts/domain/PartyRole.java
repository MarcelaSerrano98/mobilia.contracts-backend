package com.mobilia.contracts.domain;

/**
 * Rol que ejerce una persona dentro de un contrato.
 *
 * <p>Cardinalidades exigidas por el enunciado:</p>
 * <ul>
 *   <li>{@link #ARRENDATARIO}: exactamente 1 por contrato.</li>
 *   <li>{@link #PROPIETARIO}: 1 o mas por contrato.</li>
 *   <li>{@link #DEUDOR_SOLIDARIO}: opcional, 0 o mas por contrato.</li>
 * </ul>
 */
public enum PartyRole {

    ARRENDATARIO,
    PROPIETARIO,
    DEUDOR_SOLIDARIO
}
