package com.hungvers.idempotency.api.service;

import com.hungvers.idempotency.api.exception.ConnectionException;
import com.hungvers.idempotency.api.exception.IdempotencyConflictException;
import com.hungvers.idempotency.api.exception.RequestNotCompletedException;
import com.hungvers.idempotency.api.model.failure.FailureMode;
import com.hungvers.idempotency.api.model.task.LazyTask;
import com.hungvers.idempotency.api.service.port.Logger;

import java.util.Objects;

/**
 * Wrapper cho {@link IdempotencyService}, xử lý trường hợp idempotency store
 * không thể kết nối được ({@link ConnectionException}) bằng một trong hai chiến lược,
 * tùy theo {@link FailureMode} truyền vào tại thời điểm gọi.
 *
 * <p><b>{@code FAIL_OPEN}</b>: khi store unreachable, bỏ qua bảo vệ idempotency
 * và thực thi {@code function} trực tiếp (không qua claim/lock), log warning kèm
 * {@code key}. Phù hợp khi business logic có thể chấp nhận rủi ro chạy trùng để
 * đổi lấy availability (ví dụ: thao tác đã tự idempotent ở tầng khác, hoặc
 * downtime của idempotency store không nên chặn toàn bộ hệ thống).
 *
 * <p><b>{@code FAIL_CLOSED}</b> (mọi giá trị khác {@code FAIL_OPEN}): ném lại
 * {@link ConnectionException} nguyên vẹn, không thực thi {@code function}.
 * Phù hợp khi việc chạy trùng gây hậu quả nghiêm trọng hơn việc từ chối request.
 */
public class ClaimFailureIdempotencyHandler {
    private final IdempotencyService inner;
    private final Logger log;

    public ClaimFailureIdempotencyHandler(IdempotencyService inner, Logger log) {
        this.inner = Objects.requireNonNull(inner);
        this.log = Objects.requireNonNull(log);
    }

    /**
     * Thực thi {@code function} thông qua {@code inner}, xử lý riêng trường hợp
     * idempotency store unreachable theo {@code mode}.
     *
     * @param key      idempotency key, dùng để log khi fail-open
     * @param function tác vụ cần đảm bảo idempotent
     * @param mode     chiến lược xử lý khi store unreachable;
     *                 {@link FailureMode#FAIL_OPEN} sẽ thực thi {@code function}
     *                 không qua bảo vệ, các giá trị khác sẽ rethrow exception gốc
     * @throws ConnectionException nếu store unreachable và {@code mode} không phải
     *                             {@code FAIL_OPEN}
     * @throws RequestNotCompletedException nếu một request khác cùng key đang
     *                                       xử lý (propagate từ {@code inner})
     * @throws IdempotencyConflictException nếu payload khác với lần gọi trước
     *                                       (propagate từ {@code inner})
     */
    public void ensureIdempotency(String key, LazyTask<?, ?> function, FailureMode mode) {
        try {
            inner.ensureIdempotency(key, function);
        } catch (ConnectionException e) {
            if (mode == FailureMode.FAIL_OPEN) {
                var message = String.format("Idempotency store unreachable, executing without protection. key=%s", key);
                log.warn(message, e);
                function.computeIfAbsent();
            } else {
                throw e;
            }
        }
    }
}
