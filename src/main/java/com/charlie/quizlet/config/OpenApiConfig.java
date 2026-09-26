package com.charlie.quizlet.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    /**
     * Every operation requires a Bearer token by default; public endpoints opt out with
     * an empty {@code @SecurityRequirements}.
     */
    @Bean
    OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Charlie Quizlet API")
                        .version("v1")
                        .description("Errors are returned as RFC 9457 Problem Details with "
                                + "`errorCode`, `errorMessage` and `errorDescription` (localized by the "
                                + "Accept-Language header, from table error_codes), plus `errors` per field "
                                + "when errorCode is COMMON_VALIDATION_FAILED."))
                .components(new Components().addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }
}
