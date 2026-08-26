package com.mobilia.contracts.domain;

/**
 * Estados posibles de un contrato.
 *
 * <p>Un inmueble admite como maximo un contrato {@link #ACTIVO} y cualquier
 * cantidad de contratos {@link #INACTIVO}, que conforman su historial.</p>
 */
public enum ContractStatus {

    ACTIVO,
    INACTIVO
}
