package com.mobilia.contracts.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MySQLContainer;
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
 *
 * <h2>Por que el contenedor NO lleva {@code @Container}</h2>
 *
 * <p>La extension de JUnit detiene los contenedores anotados con
 * {@code @Container} al terminar <strong>cada clase</strong> de test, mientras
 * que Spring cachea el contexto de aplicacion <strong>entre clases</strong>
 * para no volver a levantarlo. Las dos politicas entran en conflicto en cuanto
 * hay mas de dos clases de integracion:</p>
 *
 * <ol>
 *   <li>La primera clase arranca el contenedor y crea un contexto apuntando a
 *       su puerto. Al acabar, la extension detiene el contenedor.</li>
 *   <li>Una clase con anotaciones distintas crea un contexto nuevo y arranca
 *       otro contenedor, con otro puerto. Tambien funciona.</li>
 *   <li>Una tercera clase con las <em>mismas</em> anotaciones que la primera
 *       reutiliza aquel contexto cacheado, que sigue apuntando al puerto del
 *       contenedor ya detenido. El resultado es un
 *       {@code CannotCreateTransaction} tras esperar a una conexion que nunca
 *       llega.</li>
 * </ol>
 *
 * <p>La solucion es el patron <em>singleton</em> que documenta Testcontainers:
 * el contenedor se arranca una sola vez desde un bloque estatico y no lo
 * gestiona la extension, de modo que sigue vivo durante toda la ejecucion.
 * Al cerrar la JVM lo elimina Ryuk, el contenedor de limpieza que Testcontainers
 * levanta junto al primero.</p>
 */
@Testcontainers(disabledWithoutDocker = true)
public abstract class AbstractDatabaseTest {

    /**
     * {@code @ServiceConnection} inyecta automaticamente la url, el usuario y la
     * contrasenna del contenedor en el {@code DataSource} de Spring, sin
     * necesidad de un {@code @DynamicPropertySource} escrito a mano.
     */
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withCommand(
                    "--character-set-server=utf8mb4",
                    "--collation-server=utf8mb4_0900_ai_ci");

    static {
        // Se comprueba antes de arrancar: sin Docker, las clases hijas quedan
        // desactivadas y este bloque no debe hacer fallar la carga de la clase.
        if (isDockerAvailable()) {
            MYSQL.start();
        }
    }

    private static boolean isDockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable unavailable) {
            return false;
        }
    }
}
