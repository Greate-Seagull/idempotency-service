package integration;

import com.hungvers.idempotency.api.exception.ConnectionException;
import com.hungvers.idempotency.api.exception.IdempotencyConflictException;
import com.hungvers.idempotency.api.model.failure.FailureMode;
import com.hungvers.idempotency.api.model.task.LazyTask;
import com.hungvers.idempotency.api.service.ClaimFailureIdempotencyHandler;
import com.hungvers.idempotency.api.service.IdempotencyService;
import com.hungvers.idempotency.api.service.port.Logger;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

@SpringBootTest
class ClaimFailureIdempotencyHandlerIT {
    @SpringBootApplication
    @ComponentScan(basePackages = "com/hungvers/idempotency/starter")
    public static class TestConfig {}

    private final ClaimFailureIdempotencyHandler handler;

    @Autowired
    ClaimFailureIdempotencyHandlerIT(ClaimFailureIdempotencyHandler handler) {
        this.handler = handler;
    }

    @Getter
    @Setter
    @Accessors(fluent = true)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static class ServiceTest {
        private final ClaimFailureIdempotencyHandler sut;

        private String idempotencyKey = "someKey";
        private LazyTask<?, ?> task = LazyTask.of("test", Boolean.class, "test"::equals);
        private FailureMode failureMode = FailureMode.FAIL_OPEN;

        public static ServiceTest arrange(ClaimFailureIdempotencyHandler sut) {
            return new ServiceTest(sut);
        }

        public void act() {
            sut.ensureIdempotency(
                    idempotencyKey,
                    task,
                    failureMode
            );
        }

        public void doAssert() {
        }
    }

    @Test
    void failOpen_shouldExecuteFunctionDirectly_whenStoreUnreachable() {
        var resource = new ArrayList<String>();
        var task = LazyTask.of("test", Boolean.class, resource::add);
        var test = ServiceTest.arrange(handler)
                .task(task)
                .failureMode(FailureMode.FAIL_OPEN);

        test.act();

        assertThat(resource).containsExactly("test");
    }

    @Test
    void failClosed_shouldRethrowConnectionException_andNotExecuteFunction() {
        var resource = new ArrayList<String>();
        var task = LazyTask.of("test", Boolean.class, resource::add);
        var test = ServiceTest.arrange(handler)
                .task(task)
                .failureMode(FailureMode.FAIL_CLOSED);

        ThrowableAssert.ThrowingCallable throwable = test::act;

        assertThatThrownBy(throwable).isInstanceOf(ConnectionException.class);
        assertThat(resource).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(FailureMode.class)
    void shouldPropagate_nonConnectionException_regardlessOfMode(FailureMode failureMode) {
        var inner = mock(IdempotencyService.class);
        var log = mock(Logger.class);
        var localHandler = new ClaimFailureIdempotencyHandler(inner, log);
        var test = ServiceTest.arrange(localHandler)
                .failureMode(failureMode);
        var thrown = new IdempotencyConflictException();
        doThrow(thrown).when(inner).ensureIdempotency(test.idempotencyKey(), test.task());

        ThrowableAssert.ThrowingCallable throwable = test::act;

        assertThatThrownBy(throwable).isInstanceOf(IdempotencyConflictException.class);
    }
}