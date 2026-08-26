package com.mobilia.contracts.repository;

import com.mobilia.contracts.domain.Contract;
import com.mobilia.contracts.support.AbstractDatabaseTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de integracion del repositorio contra un MySQL real.
 *
 * <p>Los datos son los que instala la migracion {@code V2__insert_seed_data.sql},
 * que Flyway aplica sobre el contenedor igual que en un entorno real.</p>
 *
 * <p>{@code replace = NONE} impide que Spring sustituya el {@code DataSource}
 * del contenedor por una base embebida, que es lo que hace {@code @DataJpaTest}
 * de forma predeterminada.</p>
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@DisplayName("ContractRepository (MySQL real)")
class ContractRepositoryIT extends AbstractDatabaseTest {

    @Autowired
    private ContractRepository contractRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private Statistics statistics;

    @BeforeEach
    void resetStatistics() {
        statistics = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
        statistics.clear();
    }

    @ParameterizedTest(name = "{0} -> encuentra {2}")
    @CsvSource(delimiter = '|', textBlock = """
            codigo del contrato      | %CT-2024%                    | 2
            direccion del inmueble   | %Bucaramanga%                | 1
            documento de identidad   | %80112233%                   | 2
            email                    | %valentina.gomez@example.com%| 1
            apellidos                | %Restrepo%                   | 2
            nombres                  | %Valentina%                  | 1
            """)
    @DisplayName("busca el texto en todos los campos exigidos por el enunciado")
    void findsMatchesInEveryRequiredField(String description, String pattern, int expected) {
        Page<Long> result = contractRepository.findMatchingIds(pattern, PageRequest.of(0, 20));

        assertThat(result.getTotalElements())
                .as("busqueda por %s", description)
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("la colacion hace que la busqueda ignore mayusculas y tildes")
    void searchIgnoresCaseAndAccents() {
        long withAccents = contractRepository.findMatchingIds("%Núñez%", PageRequest.of(0, 20))
                .getTotalElements();
        long withoutAccents = contractRepository.findMatchingIds("%NUNEZ%", PageRequest.of(0, 20))
                .getTotalElements();

        assertThat(withoutAccents).isEqualTo(withAccents).isEqualTo(2);
    }

    @Test
    @DisplayName("devuelve el contrato una sola vez aunque coincidan varias de sus partes")
    void returnsEachContractOnce() {
        // "Gómez" coincide con tres personas distintas del juego de datos.
        Page<Long> result = contractRepository.findMatchingIds("%Gómez%", PageRequest.of(0, 20));

        assertThat(result.getContent()).doesNotHaveDuplicates();
        assertThat(result.getContent()).hasSize((int) result.getTotalElements());
    }

    @Test
    @DisplayName("el escape impide que % introducido por la persona actue como comodin")
    void escapedWildcardIsTreatedAsLiteralText() {
        long escaped = contractRepository.findMatchingIds("%1!%%", PageRequest.of(0, 20))
                .getTotalElements();

        assertThat(escaped)
                .as("no existe ningun campo que contenga el texto literal '1%%'")
                .isZero();
    }

    @Test
    @DisplayName("carga contratos, inmueble, partes y personas en una unica consulta (sin N+1)")
    void loadsEverythingInASingleQuery() {
        List<Long> ids = contractRepository
                .findMatchingIds("%Gómez%", PageRequest.of(0, 20))
                .getContent();
        statistics.clear();

        List<Contract> contracts = contractRepository.findAllWithPartiesByIdIn(ids);

        // Se recorre todo el grafo que necesita el mapper. Si alguna asociacion
        // no viniera ya inicializada, aqui se dispararian consultas adicionales.
        contracts.forEach(contract -> {
            contract.getProperty().getAddress();
            contract.getParties().forEach(party -> party.getPerson().getFullName());
        });

        assertThat(statistics.getPrepareStatementCount())
                .as("recorrer el grafo completo no debe provocar consultas adicionales")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("no duplica contratos pese al JOIN FETCH de la coleccion de partes")
    void doesNotDuplicateContractsWhenFetchingCollections() {
        List<Long> ids = contractRepository
                .findMatchingIds("%Gómez%", PageRequest.of(0, 20))
                .getContent();

        List<Contract> contracts = contractRepository.findAllWithPartiesByIdIn(ids);

        assertThat(contracts).hasSameSizeAs(ids);
        assertThat(contracts).extracting(Contract::getCode).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("pagina de forma estable, sin repetir ni perder contratos")
    void paginatesConsistently() {
        Page<Long> firstPage = contractRepository.findMatchingIds("%Bogotá%", PageRequest.of(0, 2));
        Page<Long> secondPage = contractRepository.findMatchingIds("%Bogotá%", PageRequest.of(1, 2));

        assertThat(firstPage.getTotalElements()).isEqualTo(4);
        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(secondPage.getContent()).hasSize(2);
        assertThat(firstPage.getContent()).doesNotContainAnyElementsOf(secondPage.getContent());
    }
}
