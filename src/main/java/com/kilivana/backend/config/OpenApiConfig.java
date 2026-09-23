package com.kilivana.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${cors.allowed-origins:http://localhost:3000}")
    private String corsAllowedOrigins;

    @Bean
    public OpenAPI kilivanaOpenAPI() {
        Server devServer = new Server();
        devServer.setUrl("http://localhost:8080");
        devServer.setDescription("Development server");

        Contact contact = new Contact();
        contact.setName("Kilivana Team");
        contact.setEmail("info@kilivana.com");

        Info info = new Info()
                .title("Kilivana Backend API")
                .version("1.0.0")
                .description("Kilivana E-commerce Backend API with Admin, Ecommerce, and Logistics endpoints")
                .contact(contact);

        return new OpenAPI()
                .info(info)
                .servers(List.of(devServer));
    }
}
