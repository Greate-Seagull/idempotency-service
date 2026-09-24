package components.config;

import components.logger.Slf4jLogger;
import components.properties.IdempotencyProperties;
import components.retry.SpringRetryService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import service.IdempotencyService;
import service.port.*;
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
