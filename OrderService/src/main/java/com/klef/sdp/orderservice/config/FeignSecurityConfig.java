package com.klef.sdp.orderservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import feign.RequestInterceptor;

@Configuration
public class FeignSecurityConfig {

    @Bean
    RequestInterceptor forwardAuthenticatedRequest(
            @Value("${feasto.internal-service-token}") String serviceToken) {
        if (serviceToken.length() < 32) {
            throw new IllegalArgumentException(
                    "FEASTO_INTERNAL_SERVICE_TOKEN must contain at least 32 characters");
        }
        return template -> {
            template.removeHeader("Authorization");
            template.removeHeader("X-Feasto-Service-Token");
            template.header("X-Feasto-Service-Token", serviceToken);

            if (RequestContextHolder.getRequestAttributes()
                    instanceof ServletRequestAttributes attributes) {
                String authorization = attributes.getRequest().getHeader("Authorization");
                if (authorization != null && authorization.startsWith("Bearer ")) {
                    template.header("Authorization", authorization);
                }
            }
        };
    }
}
