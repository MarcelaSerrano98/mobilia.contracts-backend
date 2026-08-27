package com.mobilia.contracts.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Envoltura propia en vez de serializar {@code PageImpl}: Spring Data lo
 * desaconseja desde Boot 3.3 porque su forma JSON puede cambiar entre versiones
 * y romperia a los consumidores de la API.
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
