package org.paper.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {

        // Define los timeouts
        Duration connectTimeout = Duration.ofMillis(10000); // 5 segundos para conectar
        Duration readTimeout = Duration.ofMillis(100000);  // 10 segundos para leer la respuesta

        return builder
                .setConnectTimeout(connectTimeout)
                .setReadTimeout(readTimeout)
                .build();
    }
}