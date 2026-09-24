package components.retry;

import exception.RetryExecutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import service.port.RetryService;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@RequiredArgsConstructor
@Slf4j
public class SpringRetryService implements RetryService {
    private final int maxRetries;
    private final Duration delay;
    private final double multiplier;
    private final Map<Class<? extends Exception>, RetryTemplate> templates = new ConcurrentHashMap<>();

    @Override
    public void execute(Class<? extends Exception> errorType, Runnable runnable) throws RetryExecutionException {
        execute(errorType,
                () -> {
                    runnable.run();
                    return null;
                }
        );
    }

    @Override
    public <T> T execute(Class<? extends Exception> errorType, Supplier<T> supplier) throws RetryExecutionException {
        var template = templates.computeIfAbsent(errorType, this::buildTemplate);

        try {
            return template.execute(supplier::get);
        } catch (RetryException e) {
            log.warn("Retry exhausted after {} attempts for {}", maxRetries, errorType.getSimpleName());
            Throwable cause = e.getCause();
            throw new RetryExecutionException(cause);
        }
    }

    private RetryTemplate buildTemplate(Class<? extends Exception> errorType) {
        return new RetryTemplate(RetryPolicy.builder()
                .maxRetries(maxRetries)
                .delay(delay)
                .multiplier(multiplier)
                .includes(errorType)
                .build()
        );
    }
}
