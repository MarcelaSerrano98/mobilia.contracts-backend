package com.mobilia.contracts.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Parametros de configuracion propios de la aplicacion, bajo el prefijo
 * {@code mobilia} de {@code application.yml}.
 *
 * <p>Se usa {@code @ConfigurationProperties} y no {@code @Value} disperso por
 * el codigo: los valores quedan agrupados, tipados y validados al arrancar, de
 * modo que una configuracion incorrecta impide el arranque en lugar de fallar
 * en la primera peticion.</p>
 *
 * @param cors   origenes autorizados para el front-end
 * @param search limites de la operacion de busqueda
 */
@Validated
@ConfigurationProperties(prefix = "mobilia")
public record MobiliaProperties(Cors cors, Search search) {

    /**
     * @param allowedOrigins origenes desde los que se acepta una peticion del
     *                       navegador
     */
    public record Cors(@NotEmpty List<String> allowedOrigins) {
    }

    /**
     * @param minQueryLength longitud minima del texto buscado. Evita que una
     *                       cadena de un caracter recorra la tabla entera
     * @param maxPageSize    tope de elementos por pagina. Impide que un cliente
     *                       pida una pagina de tamanno arbitrario
     */
    public record Search(@Min(1) int minQueryLength, @Min(1) int maxPageSize) {
    }
}
