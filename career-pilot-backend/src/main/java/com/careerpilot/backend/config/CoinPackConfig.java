package com.careerpilot.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "coin-packs")
@Getter
@Setter
public class CoinPackConfig {
    private Map<Integer, Double> prices; // coinPackSize -> price in EGP
}