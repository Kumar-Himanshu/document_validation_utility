package com.utility.docvalidation.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger metadata for the service. The interactive UI is served at
 * {@code /swagger-ui.html} and the raw document at {@code /v3/api-docs}.
 */
@Configuration
public class OpenApiConfig {

    /**
     * @return the OpenAPI document describing this service
     */
    @Bean
    public OpenAPI documentValidationOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Document Validation Utility API")
                .version("1.0.0")
                .description("Offline format and checksum validation for PAN, Card and Aadhaar/VID. "
                        + "Values are never stored and are masked in logs.")
                .contact(new Contact().name("Document Validation Utility"))
                .license(new License().name("Proprietary")));
    }
}
