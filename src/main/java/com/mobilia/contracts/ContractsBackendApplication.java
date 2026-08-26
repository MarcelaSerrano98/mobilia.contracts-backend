package com.mobilia.contracts;

import com.mobilia.contracts.config.MobiliaProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Punto de arranque de la aplicacion.
 *
 * <p>{@code @EnableConfigurationProperties} registra {@link MobiliaProperties}
 * como bean; sin esta anotacion el {@code record} anotado con
 * {@code @ConfigurationProperties} no se instanciaria.</p>
 */
@SpringBootApplication
@EnableConfigurationProperties(MobiliaProperties.class)
public class ContractsBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContractsBackendApplication.class, args);
    }
}
