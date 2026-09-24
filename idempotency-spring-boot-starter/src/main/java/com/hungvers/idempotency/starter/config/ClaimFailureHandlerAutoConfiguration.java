package com.hungvers.idempotency.starter.config;

import com.hungvers.idempotency.api.service.ClaimFailureIdempotencyHandler;
import com.hungvers.idempotency.api.service.IdempotencyService;
import com.hungvers.idempotency.starter.logger.Slf4jLogger;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class ClaimFailureHandlerAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public ClaimFailureIdempotencyHandler claimFailureIdempotencyHandler(IdempotencyService service) {
        return new ClaimFailureIdempotencyHandler(
                service,
                new Slf4jLogger(ClaimFailureIdempotencyHandler.class)
        );
    }
}
