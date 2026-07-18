package com.careerpilot.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@ConfigurationProperties(prefix="tier-pricing")
@Setter
@Getter
public class TierPricingConfig {
    private Map<String,Double>prices; //"PLUS" -> price, "PRO" -> price
}
