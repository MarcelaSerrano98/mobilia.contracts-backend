package com.mobilia.contracts.service;

import com.mobilia.contracts.config.MobiliaProperties;
import com.mobilia.contracts.domain.Contract;
import com.mobilia.contracts.domain.ContractStatus;
import com.mobilia.contracts.domain.PartyRole;
import com.mobilia.contracts.domain.Person;
import com.mobilia.contracts.domain.Property;
import com.mobilia.contracts.domain.PropertyType;
import com.mobilia.contracts.exception.InvalidSearchQueryException;
import com.mobilia.contracts.repository.ContractRepository;
import com.mobilia.contracts.web.dto.ContractSearchResponse;
import com.mobilia.contracts.web.dto.PagedResponse;
import com.mobilia.contracts.web.mapper.ContractMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static com.mobilia.contracts.support.TestDataFactory.addParty;
import static com.mobilia.contracts.support.TestDataFactory.contract;
import static com.mobilia.contracts.support.TestDataFactory.person;
import static com.mobilia.contracts.support.TestDataFactory.property;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de {@link ContractSearchService}.
 *
 * <p>Se sustituye el repositorio por un doble de prueba, pero se usa el
 * {@link ContractMapper} real: es una funcion pura sin dependencias, y
 * simularlo convertiria el test en una comprobacion de que el codigo llama a lo
 * que llama, en lugar de comprobar el resultado.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ContractSearchService")
class ContractSearchServiceTest {

    private static final int MIN_QUERY_LENGTH = 2;
    private static final int MAX_PAGE_SIZE = 100;

    @Mock
    private ContractRepository contractRepository;

    @Captor
    private ArgumentCaptor<String> patternCaptor;

    @Captor
    private ArgumentCaptor<Pageable> pageableCaptor;

    private ContractSearchService service;

    @BeforeEach
    void setUp() {
        MobiliaProperties properties = new MobiliaProperties(
                new MobiliaProperties.Cors(List.of("http://localhost:5173")),
                new MobiliaProperties.Search(MIN_QUERY_LENGTH, MAX_PAGE_SIZE));

        service = new ContractSearchService(contractRepository, new ContractMapper(), properties);
    }

    @Nested
    @DisplayName("validacion del texto de busqueda")
    class QueryValidation {

        @ParameterizedTest(name = "rechaza el texto \"{0}\"")
        @ValueSource(strings = {"", " ", "  ", "a", " b "})
        @DisplayName("rechaza un texto vacio o mas corto que el minimo")
        void rejectsTooShortQuery(String query) {
            assertThatThrownBy(() -> service.search(query, 0, 20))
                    .isInstanceOf(InvalidSearchQueryException.class)
                    .hasMessageContaining("al menos %d caracteres".formatted(MIN_QUERY_LENGTH));

            verify(contractRepository, never()).findMatchingIds(anyString(), any());
        }

        @Test
        @DisplayName("rechaza un texto nulo sin lanzar NullPointerException")
        void rejectsNullQuery() {
            assertThatThrownBy(() -> service.search(null, 0, 20))
                    .isInstanceOf(InvalidSearchQueryException.class);
        }

        @Test
        @DisplayName("recorta los espacios sobrantes antes de buscar")
        void trimsSurroundingWhitespace() {
            when(contractRepository.findMatchingIds(anyString(), any())).thenReturn(Page.empty());

            service.search("   Gomez   ", 0, 20);

            verify(contractRepository).findMatchingIds(patternCaptor.capture(), any());
            assertThat(patternCaptor.getValue()).isEqualTo("%Gomez%");
        }
    }

    @Nested
    @DisplayName("construccion del patron LIKE")
    class LikePattern {

        @Test
        @DisplayName("escapa el comodin % para que se busque como texto literal")
        void escapesPercentWildcard() {
            when(contractRepository.findMatchingIds(anyString(), any())).thenReturn(Page.empty());

            service.search("50%", 0, 20);

            verify(contractRepository).findMatchingIds(patternCaptor.capture(), any());
            assertThat(patternCaptor.getValue()).isEqualTo("%50!%%");
        }

        @Test
        @DisplayName("escapa el comodin _ para que se busque como texto literal")
        void escapesUnderscoreWildcard() {
            when(contractRepository.findMatchingIds(anyString(), any())).thenReturn(Page.empty());

            service.search("CT_2024", 0, 20);

            verify(contractRepository).findMatchingIds(patternCaptor.capture(), any());
            assertThat(patternCaptor.getValue()).isEqualTo("%CT!_2024%");
        }

        @Test
        @DisplayName("escapa el propio caracter de escape")
        void escapesTheEscapeCharacter() {
            when(contractRepository.findMatchingIds(anyString(), any())).thenReturn(Page.empty());

            service.search("hola!", 0, 20);

            verify(contractRepository).findMatchingIds(patternCaptor.capture(), any());
            assertThat(patternCaptor.getValue()).isEqualTo("%hola!!%");
        }
    }

    @Nested
    @DisplayName("paginacion")
    class Paging {

        @Test
        @DisplayName("limita el tamanno de pagina al maximo configurado")
        void clampsPageSizeToConfiguredMaximum() {
            when(contractRepository.findMatchingIds(anyString(), any())).thenReturn(Page.empty());

            service.search("Gomez", 0, 5_000);

            verify(contractRepository).findMatchingIds(anyString(), pageableCaptor.capture());
            assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(MAX_PAGE_SIZE);
        }

        @Test
        @DisplayName("solicita la pagina sin ordenacion, porque la fija la consulta")
        void requestsAnUnsortedPage() {
            when(contractRepository.findMatchingIds(anyString(), any())).thenReturn(Page.empty());

            service.search("Gomez", 0, 20);

            verify(contractRepository).findMatchingIds(anyString(), pageableCaptor.capture());
            assertThat(pageableCaptor.getValue().getSort().isSorted()).isFalse();
        }

        @Test
        @DisplayName("no consulta los contratos completos si no hubo coincidencias")
        void skipsSecondQueryWhenThereAreNoMatches() {
            when(contractRepository.findMatchingIds(anyString(), any())).thenReturn(Page.empty());

            PagedResponse<ContractSearchResponse> result = service.search("zzzz", 0, 20);

            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isZero();
            verify(contractRepository, never()).findAllWithPartiesByIdIn(any());
        }
    }

    @Nested
    @DisplayName("mapeo del resultado")
    class ResultMapping {

        @Test
        @DisplayName("agrupa las partes por rol y conserva las que no coinciden con el texto")
        void groupsPartiesByRole() {
            Property property = property("Calle 45 # 12-34, Bogotá", PropertyType.APARTAMENTO);
            Contract contract = contract("CT-2024-001", ContractStatus.ACTIVO, property);

            addParty(contract, person("Juan Carlos", "Pérez Gómez", "1020304050", "juan@example.com"),
                    PartyRole.ARRENDATARIO);
            addParty(contract, person("María Elena", "Rodríguez Silva", "52123456", "maria@example.com"),
                    PartyRole.PROPIETARIO);
            addParty(contract, person("Ana", "Bermúdez Lara", "43112233", "ana@example.com"),
                    PartyRole.PROPIETARIO);
            addParty(contract, person("Ricardo", "Salazar Mesa", "91445566", "ricardo@example.com"),
                    PartyRole.DEUDOR_SOLIDARIO);

            Page<Long> ids = new PageImpl<>(List.of(1L), PageRequest.of(0, 20), 1);
            when(contractRepository.findMatchingIds(anyString(), any())).thenReturn(ids);
            when(contractRepository.findAllWithPartiesByIdIn(List.of(1L))).thenReturn(List.of(contract));

            // Se busca por el deudor solidario: el arrendatario y los propietarios
            // no contienen el texto, pero deben aparecer igualmente en la fila.
            PagedResponse<ContractSearchResponse> result = service.search("Salazar", 0, 20);

            assertThat(result.content()).hasSize(1);
            ContractSearchResponse row = result.content().getFirst();

            assertThat(row.contractCode()).isEqualTo("CT-2024-001");
            assertThat(row.contractStatus()).isEqualTo(ContractStatus.ACTIVO);
            assertThat(row.propertyAddress()).isEqualTo("Calle 45 # 12-34, Bogotá");
            assertThat(row.tenant().fullName()).isEqualTo("Juan Carlos Pérez Gómez");
            assertThat(row.owners())
                    .extracting("fullName")
                    .containsExactly("Ana Bermúdez Lara", "María Elena Rodríguez Silva");
            assertThat(row.guarantors())
                    .extracting("fullName")
                    .containsExactly("Ricardo Salazar Mesa");
        }

        @Test
        @DisplayName("devuelve una lista vacia de deudores solidarios cuando no los hay")
        void returnsEmptyGuarantorsWhenThereAreNone() {
            Property property = property("Carrera 70 # 23-15, Medellín", PropertyType.CASA);
            Contract contract = contract("CT-2024-002", ContractStatus.ACTIVO, property);

            addParty(contract, person("Andrés", "Gómez Ruiz", "79456123", "andres@example.com"),
                    PartyRole.ARRENDATARIO);
            addParty(contract, person("Diana", "Ospina Cano", "43998877", "diana@example.com"),
                    PartyRole.PROPIETARIO);

            Page<Long> ids = new PageImpl<>(List.of(2L), PageRequest.of(0, 20), 1);
            when(contractRepository.findMatchingIds(anyString(), any())).thenReturn(ids);
            when(contractRepository.findAllWithPartiesByIdIn(List.of(2L))).thenReturn(List.of(contract));

            ContractSearchResponse row = service.search("Gómez", 0, 20).content().getFirst();

            assertThat(row.guarantors()).isEmpty();
        }
    }
}
