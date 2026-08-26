package com.mobilia.contracts.repository;

import com.mobilia.contracts.support.AbstractDatabaseTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprueba que las reglas del enunciado las hace cumplir <em>la base de datos</em>,
 * no solo el codigo Java.
 *
 * <p>Es una distincion importante: una validacion en el servicio protege frente
 * a los errores de la propia aplicacion, pero no frente a una carga masiva, un
 * script de mantenimiento o un segundo servicio que escriba en la misma base.
 * Estos tests se ejecutan en SQL nativo justamente para saltarse la capa Java y
 * verificar el ultimo nivel de defensa.</p>
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Restricciones del esquema")
class SchemaConstraintsIT extends AbstractDatabaseTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    @DisplayName("rechaza un segundo contrato ACTIVO sobre el mismo inmueble")
    void rejectsASecondActiveContractOnTheSameProperty() {
        // El inmueble 1 ya tiene el contrato activo CT-2024-001.
        assertThatThrownBy(() -> execute(
                "INSERT INTO contract (code, status, property_id) VALUES ('CT-DUP', 'ACTIVO', 1)"))
                .hasMessageContaining("uk_contract_one_active_per_property");
    }

    @Test
    @DisplayName("admite cualquier numero de contratos INACTIVOS sobre el mismo inmueble")
    void allowsManyInactiveContractsOnTheSameProperty() {
        assertThatCode(() -> execute(
                "INSERT INTO contract (code, status, property_id) VALUES ('CT-HIST', 'INACTIVO', 1)"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("rechaza reactivar un contrato si el inmueble ya tiene uno activo")
    void rejectsReactivatingAContractWhenAnotherIsAlreadyActive() {
        assertThatThrownBy(() -> execute(
                "UPDATE contract SET status = 'ACTIVO' WHERE code = 'CT-2022-014'"))
                .hasMessageContaining("uk_contract_one_active_per_property");
    }

    @Test
    @DisplayName("rechaza un segundo ARRENDATARIO en el mismo contrato")
    void rejectsASecondTenantOnTheSameContract() {
        assertThatThrownBy(() -> execute(
                "INSERT INTO contract_party (contract_id, person_id, role) VALUES (1, 5, 'ARRENDATARIO')"))
                .hasMessageContaining("uk_contract_party_single_tenant");
    }

    @Test
    @DisplayName("admite varios PROPIETARIOS en el mismo contrato")
    void allowsSeveralOwnersOnTheSameContract() {
        assertThatCode(() -> execute(
                "INSERT INTO contract_party (contract_id, person_id, role) VALUES (1, 7, 'PROPIETARIO')"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("rechaza a la misma persona dos veces con el mismo rol en un contrato")
    void rejectsTheSamePersonTwiceWithTheSameRole() {
        assertThatThrownBy(() -> execute(
                "INSERT INTO contract_party (contract_id, person_id, role) VALUES (1, 2, 'PROPIETARIO')"))
                .hasMessageContaining("uk_contract_party_unique_role");
    }

    @Test
    @DisplayName("admite a la misma persona con dos roles distintos en un contrato")
    void allowsTheSamePersonWithDifferentRoles() {
        assertThatCode(() -> execute(
                "INSERT INTO contract_party (contract_id, person_id, role) VALUES (1, 2, 'DEUDOR_SOLIDARIO')"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("rechaza un tipo de inmueble fuera de los valores admitidos")
    void rejectsAnUnknownPropertyType() {
        assertThatThrownBy(() -> execute(
                "INSERT INTO property (address, type) VALUES ('Calle 1 # 1-1', 'BODEGA')"))
                .hasMessageContaining("ck_property_type");
    }

    @Test
    @DisplayName("rechaza un estado de contrato fuera de los valores admitidos")
    void rejectsAnUnknownContractStatus() {
        assertThatThrownBy(() -> execute(
                "INSERT INTO contract (code, status, property_id) VALUES ('CT-X', 'PENDIENTE', 2)"))
                .hasMessageContaining("ck_contract_status");
    }

    @Test
    @DisplayName("rechaza dos personas con el mismo documento de identidad")
    void rejectsDuplicateDocumentNumbers() {
        assertThatThrownBy(() -> execute("""
                INSERT INTO person (first_name, last_name, document_number, email)
                VALUES ('Otro', 'Nombre', '1020304050', 'otro@example.com')
                """))
                .hasMessageContaining("uk_person_document_number");
    }

    /**
     * Ejecuta SQL nativo forzando el vaciado inmediato, para que la restriccion
     * salte dentro del test y no al cerrar la transaccion.
     */
    private void execute(String sql) {
        entityManager.createNativeQuery(sql).executeUpdate();
        entityManager.flush();
    }
}
