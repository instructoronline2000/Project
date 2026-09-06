package com.training.oms.order.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI orderServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Order Service API")
                .description("Intermediate/Advanced module: order orchestration, Feign calls, Kafka Saga.")
                .version("v1"));
    }
}
