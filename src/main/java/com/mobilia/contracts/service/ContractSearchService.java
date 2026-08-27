package com.mobilia.contracts.service;

import com.mobilia.contracts.config.MobiliaProperties;
import com.mobilia.contracts.domain.Contract;
import com.mobilia.contracts.exception.InvalidSearchQueryException;
import com.mobilia.contracts.repository.ContractRepository;
import com.mobilia.contracts.web.dto.ContractSearchResponse;
import com.mobilia.contracts.web.dto.PagedResponse;
import com.mobilia.contracts.web.mapper.ContractMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContractSearchService {

    private final ContractRepository contractRepository;
    private final ContractMapper contractMapper;
    private final MobiliaProperties properties;

    /**
     * Busca contratos que contengan el texto indicado.
     *
     * @param rawQuery texto tal y como lo escribio la persona
     * @param page     indice de pagina, empezando en 0
     * @param size     numero de elementos por pagina
     * @return pagina de resultados, posiblemente vacia
     * @throws InvalidSearchQueryException si el texto es nulo o demasiado corto
     */
    public PagedResponse<ContractSearchResponse> search(String rawQuery, int page, int size) {
        String query = normalize(rawQuery);
        Pageable pageable = buildPageable(page, size);

        // Se pagina sobre identificadores porque es lo unico que puede
        // paginarse sin que el conteo se descuadre.
        Page<Long> matchingIds = contractRepository.findMatchingIds(toLikePattern(query), pageable);
        log.debug("Busqueda '{}': {} contratos coincidentes", query, matchingIds.getTotalElements());

        if (matchingIds.isEmpty()) {
            return PagedResponse.of(List.of(), matchingIds);
        }

        // Sin esta segunda consulta, recorrer las partes de cada contrato
        // dispararia una consulta por contrato (N+1).
        List<Contract> contracts = contractRepository.findAllWithPartiesByIdIn(matchingIds.getContent());

        List<ContractSearchResponse> rows = contracts.stream()
                .map(contractMapper::toSearchResponse)
                .toList();

        return PagedResponse.of(rows, matchingIds);
    }

    private String normalize(String rawQuery) {
        String query = rawQuery == null ? "" : rawQuery.trim();
        int minLength = properties.search().minQueryLength();

        if (query.length() < minLength) {
            throw new InvalidSearchQueryException(
                    "El texto de busqueda debe tener al menos %d caracteres.".formatted(minLength));
        }
        return query;
    }

    /**
     * Sin ordenacion a proposito: la fija el {@code ORDER BY} de la consulta, y
     * anadirla aqui haria que Spring Data concatenara un segundo ORDER BY.
     */
    private Pageable buildPageable(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.clamp(size, 1, properties.search().maxPageSize());
        return PageRequest.of(safePage, safeSize);
    }

    /**
     * Escapa los comodines que traiga el texto: sin ello, teclear {@code %}
     * devolveria todos los contratos. No es inyeccion SQL —el valor viaja como
     * parametro enlazado— sino un resultado que sorprende a quien busca.
     */
    private String toLikePattern(String query) {
        String escapeCharacter = ContractRepository.LIKE_ESCAPE_CHARACTER;
        String escaped = query
                .replace(escapeCharacter, escapeCharacter + escapeCharacter)
                .replace("%", escapeCharacter + "%")
                .replace("_", escapeCharacter + "_");
        return "%" + escaped + "%";
    }
}
