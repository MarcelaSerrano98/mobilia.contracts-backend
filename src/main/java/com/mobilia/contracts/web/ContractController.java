package com.mobilia.contracts.web;

import com.mobilia.contracts.exception.ApiError;
import com.mobilia.contracts.service.ContractSearchService;
import com.mobilia.contracts.web.dto.ContractSearchResponse;
import com.mobilia.contracts.web.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Punto de entrada HTTP para la consulta de contratos.
 *
 * <p>La ruta incluye la version ({@code /api/v1}) para poder publicar en el
 * futuro un formato de respuesta distinto sin romper a los clientes que ya
 * consumen el actual.</p>
 *
 * <p>El controlador no contiene logica de negocio: valida la entrada, delega en
 * {@link ContractSearchService} y traduce el resultado a una respuesta HTTP.</p>
 */
@RestController
@RequestMapping(value = "/api/v1/contracts", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
@Tag(name = "Contratos", description = "Consulta del historial de inmuebles y contratos")
public class ContractController {

    private static final String DEFAULT_PAGE = "0";
    private static final String DEFAULT_PAGE_SIZE = "20";

    private final ContractSearchService contractSearchService;

    /**
     * Busca contratos que contengan el texto recibido en cualquiera de los
     * campos definidos por el enunciado.
     *
     * @param query texto libre a buscar
     * @param page  indice de pagina, empezando en 0
     * @param size  numero de resultados por pagina
     */
    @Operation(
            summary = "Buscar contratos por texto libre",
            description = """
                    Devuelve los contratos en los que el texto aparece en alguno de estos campos:
                    nombres, apellidos, documento de identidad o email de cualquiera de sus partes;
                    direccion del inmueble; o codigo del contrato.
                    La comparacion ignora mayusculas y tildes.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Busqueda ejecutada correctamente"),
            @ApiResponse(
                    responseCode = "400",
                    description = "El texto de busqueda es vacio o demasiado corto",
                    content = @Content(schema = @Schema(implementation = ApiError.class))
            )
    })
    @GetMapping("/search")
    public ResponseEntity<PagedResponse<ContractSearchResponse>> search(

            @Parameter(description = "Texto a buscar", example = "Gomez", required = true)
            @RequestParam("q") @NotBlank String query,

            @Parameter(description = "Indice de pagina, empezando en 0")
            @RequestParam(value = "page", defaultValue = DEFAULT_PAGE) @Min(0) int page,

            @Parameter(description = "Numero de resultados por pagina")
            @RequestParam(value = "size", defaultValue = DEFAULT_PAGE_SIZE) @Min(1) @Max(100) int size) {

        return ResponseEntity.ok(contractSearchService.search(query, page, size));
    }
}
