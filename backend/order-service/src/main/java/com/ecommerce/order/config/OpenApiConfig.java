package com.ecommerce.order.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for API documentation.
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8083}")
    private String serverPort;

    @Bean
    public OpenAPI orderServiceOpenAPI() {
        Server localServer = new Server();
        localServer.setUrl("http://localhost:" + serverPort);
        localServer.setDescription("Local development server");

        Contact contact = new Contact();
        contact.setName("E-Commerce Platform Team");
        contact.setEmail("support@ecommerce.com");

        License license = new License();
        license.setName("Apache 2.0");
        license.setUrl("https://www.apache.org/licenses/LICENSE-2.0");

        Info info = new Info()
                .title("Order Service API")
                .version("1.0.0")
                .description("RESTful API for managing orders in the E-Commerce platform. " +
                        "This service handles order creation, payment processing, status updates, " +
                        "and inventory coordination with the Product Service.")
                .contact(contact)
                .license(license);

        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT authentication token");

        Components components = new Components()
                .addSecuritySchemes("bearerAuth", securityScheme);

        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer))
                .components(components);
    }
}
