package com.training.oms.product.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI productServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Product Service API")
                .description("Beginner/Intermediate module: catalog and inventory (stock reservation).")
                .version("v1"));
    }
}
