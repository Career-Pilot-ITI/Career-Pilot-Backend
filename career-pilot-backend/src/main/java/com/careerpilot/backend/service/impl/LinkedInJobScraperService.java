package com.careerpilot.backend.service.impl;

import com.careerpilot.backend.controller.advice.JobScrapeException;
import com.careerpilot.backend.dto.response.ChocoDataJobResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Slf4j
public class LinkedInJobScraperService {

  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;
  private final String apiKey;
  private final String baseUrl;

  public LinkedInJobScraperService(RestTemplate restTemplate,
      ObjectMapper objectMapper,
      @Value("${app.chocodata.api-key:}") String apiKey,
      @Value("${app.chocodata.base-url:https://api.chocodata.com/api/v1/linkedin/job}") String baseUrl) {
    this.restTemplate = restTemplate;
    this.objectMapper = objectMapper;
    this.apiKey = apiKey;
    this.baseUrl = baseUrl;
  }

  public ChocoDataJobResponse scrape(String jobIdOrUrl) {
    if (apiKey == null || apiKey.isBlank()) {
      throw new JobScrapeException.NotConfiguredException(
          "CHOCODATA_API_KEY is not set. Add it to enable LinkedIn job import.");
    }
    if (jobIdOrUrl == null || jobIdOrUrl.isBlank()) {
      throw new JobScrapeException.InvalidParamsException("Pass a job id or URL.");
    }

    UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl)
        .queryParam("api_key", apiKey)
        .queryParam(jobIdOrUrl.startsWith("http") ? "url" : "job_id", jobIdOrUrl);

    try {
      ResponseEntity<String> response = restTemplate.getForEntity(
          builder.build().encode().toUri(), String.class);
      ChocoDataJobResponse job = objectMapper.readValue(
          response.getBody(), ChocoDataJobResponse.class);
      if (job.title() == null && job.company() == null && job.description() == null) {
        throw new JobScrapeException.UnreachableException(
            "HTTP 200 but no recognizable job fields in the payload.");
      }
      return job;
    } catch (JobScrapeException e) {
      throw e;
    } catch (HttpStatusCodeException e) {
      throw mapHttpError(e.getStatusCode().value(), e.getResponseBodyAsString());
    } catch (Exception e) {
      log.warn("ChocoData request failed for {}: {}", jobIdOrUrl, e.getMessage());
      throw new JobScrapeException.UnreachableException("Request failed: " + e.getMessage());
    }
  }

  private JobScrapeException mapHttpError(int status, String body) {
    String snippet = body == null || body.length() <= 200 ? body : body.substring(0, 200);
    return switch (status) {
      case 400 -> new JobScrapeException.InvalidParamsException(
          "400 invalid_params: pass job_id or url. Response: " + snippet);
      case 401 -> new JobScrapeException.InvalidKeyException(
          "401 INVALID_API_KEY: key missing or not recognised.");
      case 402 -> new JobScrapeException.InsufficientCreditsException(
          "402 INSUFFICIENT_CREDITS: balance exhausted. Top up at chocodata.com.");
      case 429 -> new JobScrapeException.RateLimitedException(
          "429 RATE_LIMITED: over the plan's concurrency. Back off and retry.");
      case 404 -> new JobScrapeException.JobNotFoundException(
          "404 item_not_found: posting closed or the id never existed. Not billed.");
      case 502 -> new JobScrapeException.UnreachableException(
          "502 target_unreachable: LinkedIn refused this request. Retry shortly. Not billed.");
      default -> new JobScrapeException.UnreachableException(
          "HTTP " + status + ": " + snippet);
    };
  }
}
