package com.mobilia.contracts.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Habilita CORS para el front-end, que se sirve desde un origen distinto.
 *
 * <p>El navegador bloquea por defecto las peticiones entre origenes distintos
 * (misma politica de origen). Como el front-end corre en el puerto 5173 de Vite
 * y la API en el 8080, se trata de dos origenes diferentes y sin esta
 * configuracion la peticion nunca llegaria al servicio.</p>
 *
 * <p>Los origenes se leen de la configuracion en lugar de escribirse en el
 * codigo: en produccion no deben ser los mismos que en local.</p>
 */
@Configuration
@RequiredArgsConstructor
public class CorsConfig implements WebMvcConfigurer {

    private final MobiliaProperties properties;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(properties.cors().allowedOrigins().toArray(String[]::new))
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
