package com.example.demo.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
            // Set connection timeout to 5 seconds
            .setConnectTimeout(Duration.ofSeconds(5))
            // Set read timeout to 10 seconds
            .setReadTimeout(Duration.ofSeconds(10))
            .build();
    }
}
