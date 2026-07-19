package com.careerpilot.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  private static final String API_RESPONSE_REF = "#/components/schemas/ApiResponse";

  @Bean
  public OpenAPI customOpenAPI() {
    Content errorContent = new Content()
        .addMediaType("application/json", new MediaType()
            .schema(new Schema<>().$ref(API_RESPONSE_REF)));

    return new OpenAPI()
        .info(new Info()
            .title("Career Pilot API")
            .description("Backend API for The Career Pilot  authentication & authorization service")
            .version("1.0.0"))
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
        .components(new Components()
            .addSecuritySchemes("bearerAuth", new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")));
  }

  @Bean
  public OpenApiCustomizer globalErrorResponses() {
    return openApi -> {
      if (openApi.getPaths() == null) return;
      openApi.getPaths().values().forEach(pathItem ->
          pathItem.readOperations().forEach(this::addStandardErrorResponses));
    };
  }

  private void addStandardErrorResponses(Operation operation) {
    String json = "application/json";
    Content content = new Content().addMediaType(json, new MediaType()
        .schema(new Schema<>().$ref(API_RESPONSE_REF)));

    operation.getResponses()
        .addApiResponse("400", new ApiResponse()
            .description("Validation error or bad request")
            .content(content))
        .addApiResponse("401", new ApiResponse()
            .description("Missing or invalid JWT token")
            .content(content))
        .addApiResponse("404", new ApiResponse()
            .description("Resource not found")
            .content(content))
        .addApiResponse("500", new ApiResponse()
            .description("Internal server error")
            .content(content));
  }
}
