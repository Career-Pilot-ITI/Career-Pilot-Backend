package com.careerpilot.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Executor used by long-running AI jobs (e.g. CV optimization).
 *
 * <p>Long LLM calls run off the request thread so the frontend can poll for
 * progress via {@link com.careerpilot.backend.service.IAiJobService}. The
 * bounded queue keeps a slow Ollama/OpenAI call from exhausting web threads.</p>
 */
@Configuration
public class AsyncConfig {

  @Bean(name = "taskExecutor")
  public ThreadPoolTaskExecutor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(4);
    executor.setQueueCapacity(50);
    executor.setThreadNamePrefix("ai-job-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(30);
    executor.initialize();
    return executor;
  }
}
