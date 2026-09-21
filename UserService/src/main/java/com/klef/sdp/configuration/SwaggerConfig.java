package com.klef.sdp.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class SwaggerConfig
{
    @Bean
    public OpenAPI customOpenAPI()
    {
        String securitySchemeName =
                "bearerAuth";

        return new OpenAPI()
            .info(
                new Info()
                    .title("Feasto User Service API")
                    .version("1.0")
                    .description(
                        "API documentation for Feasto User Service"
                    )
            )
            .addSecurityItem(
                new SecurityRequirement()
                    .addList(securitySchemeName)
            )
            .components(
                new Components()
                    .addSecuritySchemes(
                        securitySchemeName,
                        new SecurityScheme()
                            .name(securitySchemeName)
                            .type(
                                SecurityScheme.Type.HTTP
                            )
                            .scheme("bearer")
                            .bearerFormat("JWT")
                    )
            );
    }
}