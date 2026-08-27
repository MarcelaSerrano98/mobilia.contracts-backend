package com.mobilia.contracts.repository;

import com.mobilia.contracts.support.AbstractDatabaseTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica las cardinalidades <em>minimas</em> que exige el enunciado:
 *
 * <blockquote>«Cada contrato tiene cómo mínimo 2 personas
 * (1 arrendatario, 1 propietario)»</blockquote>
 *
 * <p><strong>Por que esto es un test y no una restriccion de la base de datos.</strong>
 * Un maximo se expresa con un indice unico: basta con impedir la segunda fila.
 * Un minimo no tiene equivalente declarativo en ningun motor relacional, porque
 * obligaria a que el contrato y sus partes nacieran en el mismo instante: al
 * insertar el contrato todavia no existe ninguna parte que apunte a el, de modo
 * que cualquier comprobacion en ese momento fallaria siempre.</p>
 *
 * <p>Las alternativas y por que se descartaron:</p>
 * <ul>
 *   <li><em>Disparadores</em> {@code BEFORE DELETE} sobre {@code contract_party}:
 *       cubririan el borrado de la ultima parte, pero no la insercion, y anaden
 *       logica invisible desde el codigo Java. Ademas MySQL no los activa en los
 *       borrados en cascada de una clave foranea, lo que genera un comportamiento
 *       dificil de anticipar.</li>
 *   <li><em>Validacion en la capa de servicio</em>: es la solucion correcta, pero
 *       requiere una ruta de escritura. Esta API es de solo lectura, asi que hoy
 *       no hay ningun punto donde colocarla.</li>
 * </ul>
 *
 * <p>Mientras no exista esa ruta de escritura, la garantia es esta comprobacion,
 * que se ejecuta en cada {@code mvn verify} sobre la totalidad de los datos. No
 * impide que un dato invalido entre, pero impide que pase desapercibido.</p>
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Invariantes de contrato")
class ContractInvariantsIT extends AbstractDatabaseTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    @DisplayName("todo contrato tiene exactamente un arrendatario")
    void everyContractHasExactlyOneTenant() {
        assertThat(codesFailing("""
                SELECT c.code
                FROM contract c
                    LEFT JOIN contract_party cp
                        ON cp.contract_id = c.id AND cp.role = 'ARRENDATARIO'
                GROUP BY c.id, c.code
                HAVING COUNT(cp.id) <> 1
                """))
                .as("contratos cuyo numero de arrendatarios no es exactamente 1")
                .isEmpty();
    }

    @Test
    @DisplayName("todo contrato tiene al menos un propietario")
    void everyContractHasAtLeastOneOwner() {
        assertThat(codesFailing("""
                SELECT c.code
                FROM contract c
                    LEFT JOIN contract_party cp
                        ON cp.contract_id = c.id AND cp.role = 'PROPIETARIO'
                GROUP BY c.id, c.code
                HAVING COUNT(cp.id) < 1
                """))
                .as("contratos sin ningun propietario")
                .isEmpty();
    }

    @Test
    @DisplayName("todo contrato tiene al menos dos personas distintas")
    void everyContractHasAtLeastTwoPeople() {
        assertThat(codesFailing("""
                SELECT c.code
                FROM contract c
                    LEFT JOIN contract_party cp ON cp.contract_id = c.id
                GROUP BY c.id, c.code
                HAVING COUNT(DISTINCT cp.person_id) < 2
                """))
                .as("contratos con menos de dos personas")
                .isEmpty();
    }

    @Test
    @DisplayName("los deudores solidarios son opcionales: hay contratos con y sin ellos")
    void guarantorsAreOptional() {
        // Comprueba que el juego de datos ejercita ambos casos. Si todos los
        // contratos tuvieran deudor solidario, la columna "vacia si no aplica"
        // de la tabla de resultados nunca se probaria de verdad.
        List<?> conDeudores = query("""
                SELECT c.code FROM contract c
                    JOIN contract_party cp ON cp.contract_id = c.id
                WHERE cp.role = 'DEUDOR_SOLIDARIO'
                GROUP BY c.id, c.code
                """);
        List<?> sinDeudores = codesFailing("""
                SELECT c.code
                FROM contract c
                    LEFT JOIN contract_party cp
                        ON cp.contract_id = c.id AND cp.role = 'DEUDOR_SOLIDARIO'
                GROUP BY c.id, c.code
                HAVING COUNT(cp.id) = 0
                """);

        assertThat(conDeudores).as("contratos con deudor solidario").isNotEmpty();
        assertThat(sinDeudores).as("contratos sin deudor solidario").isNotEmpty();
    }

    /**
     * Ejecuta una consulta que devuelve los codigos de los contratos que
     * <em>incumplen</em> la invariante. Una lista vacia significa que todos la
     * cumplen; si falla, el mensaje del test nombra los contratos concretos.
     */
    private List<?> codesFailing(String sql) {
        return query(sql);
    }

    private List<?> query(String sql) {
        return entityManager.createNativeQuery(sql).getResultList();
    }
}
