package com.careerpilot.backend.service;

import com.careerpilot.backend.controller.advice.JobScrapeException;
import com.careerpilot.backend.dto.response.ChocoDataJobResponse;

public interface ILinkedInJobScraperService {
  public ChocoDataJobResponse scrape(String jobIdOrUrl) throws JobScrapeException;
}
