package com.mobilia.contracts.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * {@code @ConfigurationProperties} y no {@code @Value} disperso: al validarse
 * en el arranque, una configuracion incorrecta impide levantar la aplicacion en
 * lugar de fallar en la primera peticion.
 */
@Validated
@ConfigurationProperties(prefix = "mobilia")
public record MobiliaProperties(Cors cors, Search search) {

    public record Cors(@NotEmpty List<String> allowedOrigins) {
    }

    /**
     * @param minQueryLength evita que un solo caracter recorra la tabla entera
     * @param maxPageSize    impide que un cliente pida una pagina sin limite
     */
    public record Search(@Min(1) int minQueryLength, @Min(1) int maxPageSize) {
    }
}
