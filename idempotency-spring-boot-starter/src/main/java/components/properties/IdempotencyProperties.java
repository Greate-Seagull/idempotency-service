package components.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("idempotency")
@Getter
@Setter
public class IdempotencyProperties {
    private Duration ttl = Duration.ofHours(24);
}
