package com.careerpilot.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "paymob")
@Getter
@Setter
public class PaymobConfig {
    private String baseUrl;
    private String secretKey;
    private String publicKey;
    private String hmacSecret;
    private String notificationUrl;
    private String redirectionUrl;
    private Map<String, Integer> integrationIds;
}