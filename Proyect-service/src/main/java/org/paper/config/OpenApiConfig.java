// =====================================================
// OpenApiConfig.java
// =====================================================
package org.paper.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Paper SRL - API de Proyectos")
                        .version("1.0")
                        .description("""
                            API del microservicio de Proyectos para Paper SRL.
                            
                            **Funcionalidades:**
                            - Gestión de materiales y tipos de bolsa
                            - Creación y gestión de plantillas
                            - Gestión de logos de usuarios
                            - Creación y gestión de diseños personalizados
                            
                            **Nota:** Todos los endpoints requieren autenticación mediante Bearer Token (JWT de Keycloak).
                            La autenticación se maneja en el API Gateway.
                            """)
                        .contact(new Contact()
                                .name("Equipo Backend")
                                .email("backend@papersrl.com")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Ingresá el token JWT obtenido desde Keycloak")));
    }
}

