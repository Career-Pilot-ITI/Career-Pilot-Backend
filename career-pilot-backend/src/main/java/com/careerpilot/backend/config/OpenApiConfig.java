package com.careerpilot.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.DateTimeSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI customOpenAPI() {
    Content errorContent = new Content()
        .addMediaType("application/json", new MediaType()
            .schema(errorEnvelopeSchema()));

    Server testServer = new Server()
        .url("https://career-pilot-backend-production.up.railway.app")
        .description("Testing Server");

    Server prodServer = new Server()
        .url("http://35.95.57.158/team1")
        .description("Production Server");

    Server localServer = new Server()
        .url("http://localhost:8080")
        .description("Local Development Server");

    return new OpenAPI()
        .servers(List.of(prodServer, testServer,localServer))
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
        .schema(errorEnvelopeSchema()));

    operation.getResponses()
        .addApiResponse("400", new ApiResponse()
            .description("Validation error or bad request")
            .content(content))
        .addApiResponse("401", new ApiResponse()
            .description("Missing or invalid JWT token")
            .content(content))
        .addApiResponse("403", new ApiResponse()
            .description("Access denied")
            .content(content))
        .addApiResponse("404", new ApiResponse()
            .description("Resource not found")
            .content(content))
        .addApiResponse("429", new ApiResponse()
            .description("Quota exceeded or rate limited")
            .content(content))
        .addApiResponse("500", new ApiResponse()
            .description("Internal server error")
            .content(content));
  }

  private Schema<?> errorEnvelopeSchema() {
    return new Schema<>()
        .type("object")
        .addProperty("message", new StringSchema())
        .addProperty("success", new BooleanSchema())
        .addProperty("timestamp", new DateTimeSchema())
        .addProperty("data", new Schema<>());
  }
}
