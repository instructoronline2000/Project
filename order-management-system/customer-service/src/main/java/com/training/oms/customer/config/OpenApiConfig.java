package com.training.oms.customer.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Powers the Swagger UI at /swagger-ui.html and the raw spec at /v3/api-docs. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customerServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Customer Service API")
                .description("Beginner module: CRUD for customers.")
                .version("v1"));
    }
}
