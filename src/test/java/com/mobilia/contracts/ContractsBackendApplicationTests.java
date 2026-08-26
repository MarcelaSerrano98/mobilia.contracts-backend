package com.mobilia.contracts;

import com.mobilia.contracts.support.AbstractDatabaseTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Comprueba que el contexto completo de la aplicacion arranca: que Flyway aplica
 * las migraciones, que Hibernate valida las entidades contra el esquema
 * resultante y que todos los beans se resuelven.
 *
 * <p>Es el test que detecta, por ejemplo, que una entidad quedo desalineada con
 * su tabla despues de anadir una migracion.</p>
 */
@SpringBootTest
@DisplayName("Arranque de la aplicacion")
class ContractsBackendApplicationTests extends AbstractDatabaseTest {

    @Test
    @DisplayName("el contexto de Spring se carga y el esquema valida")
    void contextLoads() {
        // El propio arranque del contexto es la asercion: si Flyway fallara o si
        // una entidad no coincidiera con su tabla, este test no llegaria a
        // ejecutarse.
    }
}
