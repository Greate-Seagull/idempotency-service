package components.config;

import components.logger.Slf4jLogger;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import service.ClaimFailureIdempotencyHandler;
import service.IdempotencyService;

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
