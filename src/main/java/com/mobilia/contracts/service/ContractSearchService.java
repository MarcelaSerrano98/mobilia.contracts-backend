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

/**
 * Caso de uso unico de la aplicacion: buscar contratos por un texto libre.
 *
 * <p>El enunciado pide localizar el texto en los nombres, apellidos, documento
 * de identidad o email de cualquier persona del contrato, en la direccion del
 * inmueble o en el codigo del contrato, y devolver el contrato completo.</p>
 */
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

        // Paso 1: que contratos coinciden. Se pagina sobre identificadores,
        // que es lo unico que puede paginarse sin distorsionar el conteo.
        Page<Long> matchingIds = contractRepository.findMatchingIds(toLikePattern(query), pageable);
        log.debug("Busqueda '{}': {} contratos coincidentes", query, matchingIds.getTotalElements());

        if (matchingIds.isEmpty()) {
            return PagedResponse.of(List.of(), matchingIds);
        }

        // Paso 2: cargar esos contratos con inmueble, partes y personas en una
        // sola consulta. Sin este paso, cada contrato provocaria consultas
        // adicionales al recorrer sus partes (problema N+1).
        List<Contract> contracts = contractRepository.findAllWithPartiesByIdIn(matchingIds.getContent());

        List<ContractSearchResponse> rows = contracts.stream()
                .map(contractMapper::toSearchResponse)
                .toList();

        return PagedResponse.of(rows, matchingIds);
    }

    /**
     * Valida y limpia el texto recibido.
     *
     * @throws InvalidSearchQueryException si no alcanza la longitud minima
     */
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
     * Construye la peticion de pagina aplicando el tope configurado.
     *
     * <p>La pagina se solicita <em>sin</em> ordenacion: el orden lo fija la
     * clausula {@code ORDER BY} de la propia consulta. Si se anadiera aqui,
     * Spring Data concatenaria un segundo {@code ORDER BY} y la consulta
     * dejaria de ser valida.</p>
     */
    private Pageable buildPageable(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.clamp(size, 1, properties.search().maxPageSize());
        return PageRequest.of(safePage, safeSize);
    }

    /**
     * Envuelve el texto en comodines, escapando antes los que el propio texto
     * pudiera contener.
     *
     * <p>Sin escapar, teclear {@code %} devolveria todos los contratos y
     * {@code _} actuaria como comodin de un unico caracter. Nada de esto es un
     * riesgo de inyeccion SQL (el valor viaja como parametro enlazado), pero si
     * un resultado que sorprende a quien busca.</p>
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
