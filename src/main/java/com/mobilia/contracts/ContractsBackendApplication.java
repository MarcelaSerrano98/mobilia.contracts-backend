package com.mobilia.contracts;

import com.mobilia.contracts.config.MobiliaProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Sin {@code @EnableConfigurationProperties}, el {@code record} anotado con
 * {@code @ConfigurationProperties} no llegaria a instanciarse como bean.
 */
@SpringBootApplication
@EnableConfigurationProperties(MobiliaProperties.class)
public class ContractsBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContractsBackendApplication.class, args);
    }
}
