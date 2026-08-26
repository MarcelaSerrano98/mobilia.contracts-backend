package com.mobilia.contracts.web;

import com.mobilia.contracts.domain.ContractStatus;
import com.mobilia.contracts.domain.PropertyType;
import com.mobilia.contracts.exception.GlobalExceptionHandler;
import com.mobilia.contracts.exception.InvalidSearchQueryException;
import com.mobilia.contracts.service.ContractSearchService;
import com.mobilia.contracts.web.dto.ContractSearchResponse;
import com.mobilia.contracts.web.dto.PagedResponse;
import com.mobilia.contracts.web.dto.PartyResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de la capa web.
 *
 * <p>{@code @WebMvcTest} levanta unicamente la porcion de contexto de Spring MVC
 * (controladores, conversion a JSON, validacion y manejo de errores). No arranca
 * ni la base de datos ni la capa de servicio, de modo que el test se ejecuta en
 * milisegundos y solo puede fallar por un problema de la capa web.</p>
 *
 * <p>{@link com.mobilia.contracts.config.MobiliaProperties} no se declara aqui:
 * lo registra el {@code @EnableConfigurationProperties} de la clase principal y
 * lo rellena {@code application.yml}. Declararlo ademas en el test produciria dos
 * beans del mismo tipo y el contexto no arrancaria.</p>
 */
@WebMvcTest(ContractController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("ContractController")
class ContractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContractSearchService contractSearchService;

    @Test
    @DisplayName("devuelve 200 y la fila con todos los campos que pide el enunciado")
    void returnsTheSearchResults() throws Exception {
        ContractSearchResponse row = new ContractSearchResponse(
                "CT-2024-001",
                ContractStatus.ACTIVO,
                "Calle 45 # 12-34 Apto 501, Bogotá",
                PropertyType.APARTAMENTO,
                new PartyResponse("Juan Carlos Pérez Gómez", "1020304050"),
                List.of(new PartyResponse("María Elena Rodríguez Silva", "52123456")),
                List.of());

        when(contractSearchService.search(anyString(), anyInt(), anyInt()))
                .thenReturn(new PagedResponse<>(List.of(row), 0, 20, 1, 1, true));

        mockMvc.perform(get("/api/v1/contracts/search").param("q", "Gomez"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].contractCode").value("CT-2024-001"))
                .andExpect(jsonPath("$.content[0].propertyAddress").value("Calle 45 # 12-34 Apto 501, Bogotá"))
                .andExpect(jsonPath("$.content[0].tenant.fullName").value("Juan Carlos Pérez Gómez"))
                .andExpect(jsonPath("$.content[0].owners[0].fullName").value("María Elena Rodríguez Silva"))
                .andExpect(jsonPath("$.content[0].guarantors").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("aplica los valores por defecto de paginacion cuando no se envian")
    void appliesDefaultPagination() throws Exception {
        when(contractSearchService.search(anyString(), anyInt(), anyInt()))
                .thenReturn(new PagedResponse<>(List.of(), 0, 20, 0, 0, true));

        mockMvc.perform(get("/api/v1/contracts/search").param("q", "Gomez"))
                .andExpect(status().isOk());

        verify(contractSearchService).search("Gomez", 0, 20);
    }

    @Test
    @DisplayName("devuelve 400 si falta el parametro q")
    void returnsBadRequestWhenQueryParameterIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/contracts/search"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Falta el parametro obligatorio 'q'."))
                .andExpect(jsonPath("$.path").value("/api/v1/contracts/search"));

        verify(contractSearchService, never()).search(anyString(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("devuelve 400 con el formato ApiError si el texto es demasiado corto")
    void returnsBadRequestWhenQueryIsTooShort() throws Exception {
        when(contractSearchService.search(eq("a"), anyInt(), anyInt()))
                .thenThrow(new InvalidSearchQueryException("El texto de busqueda debe tener al menos 2 caracteres."));

        mockMvc.perform(get("/api/v1/contracts/search").param("q", "a"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("El texto de busqueda debe tener al menos 2 caracteres."))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("devuelve 400 si page no es un numero")
    void returnsBadRequestWhenPageIsNotANumber() throws Exception {
        mockMvc.perform(get("/api/v1/contracts/search").param("q", "Gomez").param("page", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El parametro 'page' no admite el valor 'abc'."));
    }

    @Test
    @DisplayName("devuelve 400 si size supera el maximo declarado")
    void returnsBadRequestWhenPageSizeExceedsTheMaximum() throws Exception {
        mockMvc.perform(get("/api/v1/contracts/search").param("q", "Gomez").param("size", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
