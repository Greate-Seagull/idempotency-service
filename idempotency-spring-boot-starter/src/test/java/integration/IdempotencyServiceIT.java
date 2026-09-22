package integration;

import components.store.RedisIdempotencyStore;
import exception.IdempotencyConflictException;
import exception.RequestNotCompletedException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import model.task.LazyTask;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.redis.core.StringRedisTemplate;
import service.IdempotencyService;
import service.port.IdempotencyStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class IdempotencyServiceIT extends AbstractRedisIntegrationTest {
    @SpringBootApplication
    // scanBasePackages from SpringBootApplication does not work
    @ComponentScan(basePackages = "components")
    public static class TestConfig {}

    private final IdempotencyService sut;
    private final IdempotencyStore store;

    @Autowired
    public IdempotencyServiceIT(StringRedisTemplate template, IdempotencyService sut, RedisIdempotencyStore store) {
        super(template);
        this.sut = sut;
        this.store = store;
    }

    @Getter
    @Setter
    @Accessors(fluent = true)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static class ServiceTest {
        private final IdempotencyService sut;
        private final IdempotencyStore store;

        private String idempotencyKey = "someKey";
        private LazyTask<?, ?> task = LazyTask.of("test", Boolean.class, "test"::equals);

        public static ServiceTest arrange(IdempotencyService sut, IdempotencyStore store) {
            return new ServiceTest(sut, store);
        }

        public void act() {
            sut.ensureIdempotency(
                    idempotencyKey,
                    task
            );
        }

        public void doAssert() {
            var result = store.find("someKey").orElseThrow();
            assertThat(result.isCompleted()).isTrue();
        }
    }

    @Test
    public void normal_function_is_cached_for_idempotency_successfully() {
        var task = LazyTask.of("test", Boolean.class, "test"::equals);
        var test = ServiceTest.arrange(sut, store)
                .task(task);

        test.act();

        test.doAssert();
        assertThat(task.getResponse()).isTrue();
    }

    @Test
    public void no_output_function_is_cached_for_idempotency_successfully() {
        var test = ServiceTest.arrange(sut, store)
                .task(LazyTask.noOutput("test", System.out::println));

        test.act();

        test.doAssert();
    }

    @Test
    public void no_input_function_is_cached_for_idempotency_successfully() {
        var task = LazyTask.noInput(String.class, () -> "test");
        var test = ServiceTest.arrange(sut, store)
                .task(task);

        test.act();

        test.doAssert();
        assertThat(task.getResponse()).isEqualTo("test");
    }

    @Test
    public void no_input_and_output_function_is_cached_for_idempotency_successfully() {
        var task = LazyTask.noInOut(() -> System.out.print("test"));
        var test = ServiceTest.arrange(sut, store)
                .task(task);

        test.act();

        test.doAssert();
    }

    @Test
    public void ran_function_with_same_key_and_input_does_not_run_again() {
        var sameKey = "someKey";
        var entityUnderTest = new ArrayList<String>();
        var firstTask = LazyTask.of("test", Boolean.class, entityUnderTest::add);
        var secondTask = LazyTask.of("test", Boolean.class, entityUnderTest::add);
        var firstRunner = ServiceTest.arrange(sut, store)
                .idempotencyKey(sameKey)
                .task(firstTask);
        var secondRunner = ServiceTest.arrange(sut, store)
                .idempotencyKey(sameKey)
                .task(secondTask);

        firstRunner.act();
        secondRunner.act();

        assertThat(firstTask.getResponse()).isEqualTo(secondTask.getResponse());
        assertThat(entityUnderTest).hasSize(1);
    }

    @Test
    public void different_key_with_same_input_for_same_function_is_valid() {
        var entityUnderTest = new ArrayList<String>();
        var firstTask = LazyTask.of("test", Boolean.class, entityUnderTest::add);
        var secondTask = LazyTask.of("test", Boolean.class, entityUnderTest::add);
        var firstRunner = ServiceTest.arrange(sut, store)
                .idempotencyKey("key1")
                .task(firstTask);
        var secondRunner = ServiceTest.arrange(sut, store)
                .idempotencyKey("key2")
                .task(secondTask);

        firstRunner.act();
        secondRunner.act();

        assertThat(firstTask.getResponse()).isEqualTo(secondTask.getResponse());
        assertThat(entityUnderTest).hasSize(2);
    }

    @Test
    public void different_key_with_different_input_for_same_function_is_valid() {
        var entityUnderTest = new ArrayList<String>();
        var firstTask = LazyTask.of("test", Boolean.class, entityUnderTest::add);
        var secondTask = LazyTask.of("query", Boolean.class, entityUnderTest::add);
        var firstRunner = ServiceTest.arrange(sut, store)
                .idempotencyKey("key1")
                .task(firstTask);
        var secondRunner = ServiceTest.arrange(sut, store)
                .idempotencyKey("key2")
                .task(secondTask);

        firstRunner.act();
        secondRunner.act();

        assertThat(firstTask.getResponse()).isEqualTo(secondTask.getResponse());
        assertThat(entityUnderTest).hasSize(2);
    }

    @Test
    public void different_key_and_input_and_function_is_valid() {
        var entityUnderTest = new ArrayList<String>();
        var firstTask = LazyTask.of("test", Boolean.class, entityUnderTest::add);
        var secondTask = LazyTask.of("test", Boolean.class, entityUnderTest::remove);
        var firstRunner = ServiceTest.arrange(sut, store)
                .idempotencyKey("key1")
                .task(firstTask);
        var secondRunner = ServiceTest.arrange(sut, store)
                .idempotencyKey("key2")
                .task(secondTask);

        firstRunner.act();
        secondRunner.act();

        assertThat(firstTask.getResponse()).isTrue();
        assertThat(secondTask.getResponse()).isTrue();
        assertThat(entityUnderTest).isEmpty();
    }

    @Test
    public void same_key_and_function_with_different_input_is_conflict() {
        var sameKey = "someKey";
        var entityUnderTest = new ArrayList<String>();
        var firstTask = LazyTask.of("test", Boolean.class, entityUnderTest::add);
        var secondTask = LazyTask.of("query", Boolean.class, entityUnderTest::add);
        var firstRunner = ServiceTest.arrange(sut, store)
                .idempotencyKey(sameKey)
                .task(firstTask);
        var secondRunner = ServiceTest.arrange(sut, store)
                .idempotencyKey(sameKey)
                .task(secondTask);

        firstRunner.act();
        ThrowableAssert.ThrowingCallable throwable = secondRunner::act;

        assertThatThrownBy(throwable).isInstanceOf(IdempotencyConflictException.class);
        assertThat(firstTask.getResponse()).isTrue();
        assertThat(secondTask.getResponse()).isNull();
        assertThat(entityUnderTest).hasSize(1);
    }

    // This case is supported in different way
//    @Test
//    public void same_key_and_input_with_different_function_is_conflict() {
//        var sameKey = "someKey";
//        var entityUnderTest = new ArrayList<String>();
//        var firstTask = LazyTask.of("test", Boolean.class, entityUnderTest::add);
//        var secondTask = LazyTask.of("test", Boolean.class, entityUnderTest::remove);
//        var firstRunner = ServiceTest.arrange(sut, store)
//                .idempotencyKey(sameKey)
//                .task(firstTask);
//        var secondRunner = ServiceTest.arrange(sut, store)
//                .idempotencyKey(sameKey)
//                .task(secondTask);
//
//        firstRunner.act();
//        ThrowableAssert.ThrowingCallable throwable = secondRunner::act;
//
//        assertThatThrownBy(throwable).isInstanceOf(IdempotencyConflictException.class);
//        assertThat(firstTask.getResponse()).isTrue();
//        assertThat(secondTask.getResponse()).isNull();
//        assertThat(entityUnderTest).hasSize(1);
//    }

    @Test
    public void when_client_provides_the_same_key_and_input_with_different_function_the_second_does_not_run() {
        var sameKey = "someKey";
        var entityUnderTest = new ArrayList<String>();
        var firstTask = LazyTask.of("test", Boolean.class, entityUnderTest::add);
        var secondTask = LazyTask.of("test", Boolean.class, entityUnderTest::remove);
        var firstRunner = ServiceTest.arrange(sut, store)
                .idempotencyKey(sameKey)
                .task(firstTask);
        var secondRunner = ServiceTest.arrange(sut, store)
                .idempotencyKey(sameKey)
                .task(secondTask);

        firstRunner.act();
        secondRunner.act();

        assertThat(firstTask.getResponse()).isTrue();
        assertThat(secondTask.getResponse()).isTrue();
        assertThat(entityUnderTest).hasSize(1);
    }

    @Test
    public void record_is_not_saved_when_function_throws() {
        var entityUnderTest = new ArrayList<String>();
        var mistake = LazyTask.of(1, String.class, i -> entityUnderTest.remove((int) i));
        var runner = ServiceTest.arrange(sut, store);

        ThrowableAssert.ThrowingCallable throwable = runner.task(mistake)::act;

        assertThatThrownBy(throwable).isInstanceOf(RuntimeException.class);
        assertThat(mistake.getResponse()).isNull();
        assertThat(entityUnderTest).isEmpty();
        assertThat(store.find(runner.idempotencyKey())).isEmpty();
    }

    @Test
    public void retry_is_safe_when_function_throws() {
        var entityUnderTest = new ArrayList<String>();
        entityUnderTest.add("test");
        var mistake = LazyTask.of(4, String.class, i -> entityUnderTest.remove((int) i));
        var retry = LazyTask.of(0, String.class, i -> entityUnderTest.remove((int) i));
        var runner = ServiceTest.arrange(sut, store);

        ThrowableAssert.ThrowingCallable throwable = runner.task(mistake)::act;
        assertThatThrownBy(throwable).isInstanceOf(RuntimeException.class);
        assertThat(mistake.getResponse()).isNull();

        runner.task(retry).act();
        assertThat(retry.getResponse()).isEqualTo("test");

        assertThat(entityUnderTest).isEmpty();
        assertThat(store.find(runner.idempotencyKey())).isPresent();
    }

    @RepeatedTest(20)
    public void service_behaves_correctly_under_stress() throws InterruptedException {
        var resource = new ArrayList<String>();
        var task = LazyTask.of("test", Boolean.class, resource::add);
        var runner = ServiceTest.arrange(sut, store)
                .task(task);

        var errors = fireConcurrentTasks(20, runner::act);

        assertThat(errors).hasOnlyElementsOfType(RequestNotCompletedException.class);
        assertThat(resource).hasSize(1);
    }

    private List<Throwable> fireConcurrentTasks(int threads, Runnable task) throws InterruptedException {
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);

        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();) {
            for(int i = 0; i < threads; i++) {
                pool.submit(() -> {
                    try {
                        ready.countDown();
                        start.await();
                        task.run();
                    } catch (Throwable e) {
                        errors.add(e);
                    } finally {
                        done.countDown();
                    }
                });
            }

            ready.await();
            start.countDown();
            done.await(10, TimeUnit.SECONDS);
        }

        return errors;
    }
}
