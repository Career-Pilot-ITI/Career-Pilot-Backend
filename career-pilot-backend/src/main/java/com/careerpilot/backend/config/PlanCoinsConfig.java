package com.careerpilot.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "app.plans")
@Getter
@Setter
public class PlanCoinsConfig {
    private Map<String, Integer> coins; // tier -> seeded coin count (FREE, PLUS, PRO)
}
