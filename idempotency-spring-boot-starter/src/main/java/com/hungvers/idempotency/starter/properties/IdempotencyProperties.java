package com.hungvers.idempotency.starter.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("idempotency")
@Getter
@Setter
public class IdempotencyProperties {
    // idempotency store
    private Duration ttl = Duration.ofHours(24);

    // idempotency.retry.*
    private Retry retry = new Retry();

    @Getter
    @Setter
    public static class Retry {
        private int maxRetries = 3;
        private Duration delay = Duration.ofMillis(500);
        private double multiplier = 2;
    }
}
