package com.mobilia.contracts.domain;

/**
 * Un inmueble admite como maximo un contrato {@code ACTIVO}; los
 * {@code INACTIVO} conforman su historial y no tienen limite.
 */
public enum ContractStatus {

    ACTIVO,
    INACTIVO
}
