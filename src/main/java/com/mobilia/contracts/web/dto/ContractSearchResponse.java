package com.mobilia.contracts.web.dto;

import com.mobilia.contracts.domain.ContractStatus;
import com.mobilia.contracts.domain.PropertyType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Fila de la tabla de resultados de la busqueda.
 *
 * <p>Se devuelve un DTO y no la entidad {@link com.mobilia.contracts.domain.Contract}
 * por tres motivos: no se filtra al cliente el modelo interno ni columnas de
 * auditoria; se evita que Jackson recorra asociaciones perezosas fuera de la
 * transaccion; y el contrato de la API queda desacoplado del esquema, de modo
 * que renombrar una columna no rompe al consumidor.</p>
 */
@Schema(description = "Contrato encontrado, con sus partes agrupadas por rol")
public record ContractSearchResponse(

        @Schema(description = "Codigo del contrato", example = "CT-2024-001")
        String contractCode,

        @Schema(description = "Estado del contrato", example = "ACTIVO")
        ContractStatus contractStatus,

        @Schema(description = "Direccion del inmueble", example = "Calle 45 # 12-34 Apto 501, Bogotá")
        String propertyAddress,

        @Schema(description = "Tipo de inmueble", example = "APARTAMENTO")
        PropertyType propertyType,

        @Schema(description = "Arrendatario del contrato. Siempre hay exactamente uno")
        PartyResponse tenant,

        @Schema(description = "Propietarios del inmueble. Siempre hay al menos uno")
        List<PartyResponse> owners,

        @Schema(description = "Deudores solidarios. Lista vacia si el contrato no tiene")
        List<PartyResponse> guarantors
) {
}
