package com.mobilia.contracts.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * springdoc genera la especificacion desde los controladores; aqui solo se
 * completa la portada que muestra Swagger UI en {@code /swagger-ui.html}.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI mobiliaOpenApi() {
        return new OpenAPI().info(new Info()
                .title("API de contratos e inmuebles - Mobilia Software")
                .description("""
                        Servicio de consulta del historial de inmuebles y de las partes
                        asociadas a cada contrato de arrendamiento.
                        """)
                .version("v1")
                .contact(new Contact().name("Marcela Albarracin"))
                .license(new License().name("Prueba tecnica")));
    }
}
