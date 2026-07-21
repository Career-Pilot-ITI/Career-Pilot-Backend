package com.careerpilot.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "session-pricing")
@Getter
@Setter
public class SessionPricingConfig {
    private int coinCost = 50; // default matches current hardcoded value
}