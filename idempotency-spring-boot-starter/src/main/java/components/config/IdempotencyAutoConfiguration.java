package components.config;

import components.properties.IdempotencyProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import service.IdempotencyService;
import service.port.IdempotencyHasher;
import service.port.IdempotencySerializer;
import service.port.IdempotencyStore;
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
            IdempotencyProperties properties
    ) {
        return new IdempotencyService(store, serializer, hasher, properties.getTtl());
    }

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper defaultMapper() {
        return new ObjectMapper();
    }
}
