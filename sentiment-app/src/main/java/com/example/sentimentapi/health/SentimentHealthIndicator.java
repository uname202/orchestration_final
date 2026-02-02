package com.example.sentimentapi.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class SentimentHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        return Health.up().withDetail("sentiment", "ok").build();
    }
}
