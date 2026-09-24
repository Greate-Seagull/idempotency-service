package com.hungvers.idempotency.starter.config;

import com.hungvers.idempotency.api.service.IdempotencyService;
import com.hungvers.idempotency.api.service.port.IdempotencyHasher;
import com.hungvers.idempotency.api.service.port.IdempotencySerializer;
import com.hungvers.idempotency.api.service.port.IdempotencyStore;
import com.hungvers.idempotency.api.service.port.RetryService;
import com.hungvers.idempotency.starter.logger.Slf4jLogger;
import com.hungvers.idempotency.starter.properties.IdempotencyProperties;
import com.hungvers.idempotency.starter.retry.SpringRetryService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;

@AutoConfiguration
@EnableConfigurationProperties(IdempotencyProperties.class)
public class IdempotencyAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public IdempotencyService idempotencyService(
            IdempotencyStore store,
            IdempotencySerializer serializer,
            IdempotencyHasher hasher,
            IdempotencyProperties properties,
            RetryService retryService
    ) {
        return new IdempotencyService(
                store,
                serializer,
                hasher,
                properties.getTtl(),
                retryService,
                new Slf4jLogger(IdempotencyService.class)
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper defaultMapper() {
        return new ObjectMapper();
    }

    @Bean
    @ConditionalOnMissingBean
    public RetryService retryService(IdempotencyProperties properties) {
        return new SpringRetryService(
                properties.getRetry().getMaxRetries(),
                properties.getRetry().getDelay(),
                properties.getRetry().getMultiplier()
        );
    }
}
