package com.mobilia.contracts.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base de los tests que necesitan una base de datos real.
 *
 * <p>Se levanta MySQL en un contenedor efimero en lugar de usar H2 en modo
 * compatible. El esquema depende de caracteristicas que solo existen en MySQL
 * (columnas generadas, indices unicos que ignoran NULL, colacion
 * {@code utf8mb4_0900_ai_ci}); probar sobre H2 validaria un esquema distinto
 * del que se despliega y daria una falsa sensacion de seguridad.</p>
 *
 * <p>{@code disabledWithoutDocker = true} hace que estos tests se
 * <em>omitan</em>, y no que fallen, en una maquina sin Docker. Asi
 * {@code mvn test} sigue siendo verde para quien evalue la prueba sin tener
 * Docker levantado, mientras que los tests unitarios se ejecutan siempre.</p>
 */
@Testcontainers(disabledWithoutDocker = true)
public abstract class AbstractDatabaseTest {

    /**
     * {@code @ServiceConnection} inyecta automaticamente la url, el usuario y la
     * contrasenna del contenedor en el {@code DataSource} de Spring, sin
     * necesidad de un {@code @DynamicPropertySource} escrito a mano.
     */
    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withCommand(
                    "--character-set-server=utf8mb4",
                    "--collation-server=utf8mb4_0900_ai_ci");
}
