package com.mobilia.contracts.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Envoltura de resultados paginados propia de la API.
 *
 * <p>Spring Data desaconseja serializar {@code PageImpl} directamente (desde
 * Spring Boot 3.3 registra el aviso {@code PageModule}): su estructura JSON no
 * forma parte de ninguna garantia de compatibilidad y puede cambiar entre
 * versiones. Definir la envoltura hace que el contrato de la API sea estable y
 * dependa solo de este proyecto.</p>
 *
 * @param <T> tipo de los elementos de la pagina
 */
@Schema(description = "Pagina de resultados")
public record PagedResponse<T>(

        @Schema(description = "Elementos de la pagina actual")
        List<T> content,

        @Schema(description = "Indice de la pagina actual, empezando en 0", example = "0")
        int page,

        @Schema(description = "Tamanno de pagina solicitado", example = "20")
        int size,

        @Schema(description = "Total de elementos que cumplen el criterio", example = "3")
        long totalElements,

        @Schema(description = "Total de paginas disponibles", example = "1")
        int totalPages,

        @Schema(description = "Indica si esta es la ultima pagina", example = "true")
        boolean last
) {

    /**
     * Convierte una {@link Page} de entidades en una pagina de DTOs.
     *
     * @param page pagina devuelta por Spring Data
     * @param mapper funcion que transforma cada elemento en su DTO
     */
    public static <S, T> PagedResponse<T> from(Page<S> page, Function<S, T> mapper) {
        return new PagedResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    /** Construye una pagina a partir de una lista ya mapeada. */
    public static <T> PagedResponse<T> of(List<T> content, Page<?> page) {
        return new PagedResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
