package com.hungvers.idempotency.api.service;

import com.hungvers.idempotency.api.exception.ConnectionException;
import com.hungvers.idempotency.api.exception.IdempotencyConflictException;
import com.hungvers.idempotency.api.exception.RequestNotCompletedException;
import com.hungvers.idempotency.api.exception.RetryExecutionException;
import com.hungvers.idempotency.api.model.helper.Claimable;
import com.hungvers.idempotency.api.model.schema.IdempotencyRecord;
import com.hungvers.idempotency.api.model.task.LazyTask;
import com.hungvers.idempotency.api.service.port.*;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Đảm bảo tính idempotent cho các thao tác có side-effect, dựa trên
 * Redis làm store trung tâm cho việc claim và lưu kết quả.
 *
 * <p>Cơ chế hoạt động: khi nhận một idempotency key, service cố gắng
 * "claim" (chiếm quyền thực thi) key đó một cách atomic thông qua
 * {@link IdempotencyStore#tryClaim}. Chỉ request đầu tiên claim thành
 * công mới thực sự chạy {@code function}; các request trùng key đến
 * sau (kể cả đang chạy đồng thời) sẽ nhận lại kết quả đã cache hoặc
 * bị từ chối nếu request gốc chưa hoàn tất.
 *
 * <p>Record được lưu ở trạng thái {@code in-progress} trước khi
 * function thực thi (không phải sau khi có kết quả) để loại bỏ race
 * condition: nếu return trực tiếp sau khi compute xong, hai request
 * đồng thời với cùng key có thể cùng vượt qua bước kiểm tra tồn tại
 * và cùng chạy function - vi phạm chính mục đích của idempotency.
 *
 * <p><b>Lưu ý:</b> idempotency key đơn thuần không đủ để xác định
 * hai request là "giống nhau", client có thể gửi nhầm key cũ với
 * payload khác. Vì vậy request hiện tại còn được hash
 * ({@link IdempotencyHasher}) và so sánh với hash đã lưu; nếu khác,
 * đây được xem là xung đột logic không phải duplicate hợp lệ.
 */
public class IdempotencyService {
    private final IdempotencyStore store;
    private final IdempotencySerializer serializer;
    private final IdempotencyHasher hasher;
    private final Duration ttl;
    private final RetryService retry;
    private final Logger logger;

    public IdempotencyService(IdempotencyStore store, IdempotencySerializer serializer, IdempotencyHasher hasher, Duration ttl, RetryService retry, Logger logger) {
        this.store = Objects.requireNonNull(store);
        this.serializer = Objects.requireNonNull(serializer);
        this.hasher = Objects.requireNonNull(hasher);
        this.ttl = Objects.requireNonNull(ttl);
        this.retry = Objects.requireNonNull(retry);
        this.logger = Objects.requireNonNull(logger);
    }

    /**
     * Thực thi {@code function} đúng một lần cho mỗi {@code idempotencyKey},
     * bất kể có bao nhiêu request trùng key được gửi tới.
     *
     * <p>Nếu đây là request đầu tiên với key này, {@code function} sẽ được
     * thực thi và kết quả được lưu lại (best-effort, có retry).
     *
     * <p>Nếu {@code function} ném exception, hệ thống sẽ cố xoá record (có retry)
     * để cho phép request sau retry với cùng key. Nếu việc xoá cũng thất bại sau
     * khi hết số lần retry, record sẽ bị giữ nguyên ở trạng thái in-progress cho
     * đến khi TTL hết hạn, các request sau với cùng key sẽ nhận
     * {@link RequestNotCompletedException} trong khoảng thời gian đó.
     *
     * <p>Nếu {@code function} thực thi thành công nhưng việc lưu kết quả thất bại
     * sau khi hết số lần retry, business logic đã chạy xong nhưng record vẫn ở
     * trạng thái in-progress cho đến khi TTL hết hạn, các request sau với cùng
     * key sẽ nhận {@link RequestNotCompletedException} thay vì kết quả đã cache,
     * và có thể khiến {@code function} bị thực thi lại nếu client retry sau khi TTL hết hạn.
     *
     * <p>Việc claim record (bước đầu tiên, trước khi {@code function} chạy) cũng có
     * retry nếu store gặp lỗi kết nối tạm thời. Nếu hết số lần retry mà vẫn không
     * claim được, {@link ConnectionException} sẽ được ném ra - caller (ví dụ
     * {@code ClaimFailureIdempotencyHandler}) có thể chọn fail-open (bỏ qua bảo vệ
     * idempotency, thực thi trực tiếp) hoặc fail-closed (từ chối request) tuỳ nghiệp vụ.
     *
     * <p>Nếu key đã tồn tại (do request trước đó):
     * <ul>
     *   <li>nếu request trước chưa hoàn tất, ném {@link RequestNotCompletedException}
     *       - caller nên retry sau, không nên coi là lỗi vĩnh viễn;</li>
     *   <li>nếu payload khác với lần trước (hash không khớp), ném
     *       {@link IdempotencyConflictException} - đây là lỗi client, không nên retry;</li>
     *   <li>nếu khớp, kết quả đã cache được gán ngược vào {@code function}
     *       thông qua {@link LazyTask#putIfAbsent} mà không thực thi lại.</li>
     * </ul>
     *
     * @param idempotencyKey khoá duy nhất định danh request logic; phải do
     *                       client sinh ra và ổn định qua các lần retry
     *                       (ví dụ UUID sinh một lần, không sinh lại mỗi request)
     * @param function       tác vụ cần đảm bảo idempotent; {@link LazyTask#getRequest()}
     *                       phải trả về object hash ổn định (cùng input → cùng hash)
     *                       để việc phát hiện conflict hoạt động chính xác
     * @throws RequestNotCompletedException nếu một request khác với cùng key
     *                                       đang được xử lý và chưa hoàn tất
     * @throws IdempotencyConflictException nếu key trùng nhưng payload khác
     *                                       với lần gọi trước đó
     * @throws ConnectionException nếu không thể claim record sau khi hết số lần retry
     *                              do lỗi kết nối tới idempotency store
     */
    public void ensureIdempotency(String idempotencyKey, LazyTask<?, ?> function) {
        var hashedRequest = hasher.hash(function.getRequest());
        Claimable<IdempotencyRecord> result = claimWithRetry(
                idempotencyKey,
                () -> IdempotencyRecord.inProgress(hashedRequest, ttl)
        );

        var recorded = result.value();
        if (result.isClaimed()) {
            try {
                function.computeIfAbsent();
                var response = serializer.serialize(function.getResponse());
                saveWithRetry(idempotencyKey, recorded.completed(response));
            } catch (Exception e) {
                deleteWithRetry(idempotencyKey);
                throw e;
            }
        } else {
            if (!recorded.isCompleted())
                throw new RequestNotCompletedException();
            if (!recorded.isDuplicate(hashedRequest))
                throw new IdempotencyConflictException();
            function.putIfAbsent(recorded.response(), serializer);
        }
    }

    private Claimable<IdempotencyRecord> claimWithRetry(String idempotencyKey, Supplier<IdempotencyRecord> creator) {
        try {
            return retry.execute(
                    ConnectionException.class,
                    () -> store.tryClaim(idempotencyKey, creator)
            );
        } catch (RetryExecutionException e) {
            if (e.getCause() instanceof RuntimeException unwrapped)
                throw unwrapped;
            throw e;
        }
    }

    private void deleteWithRetry(String idempotencyKey) {
        try {
            retry.execute(ConnectionException.class, () -> store.delete(idempotencyKey));
        } catch (RetryExecutionException e) {
            var message = String.format("Failed to delete idempotency record key=%s after retries. " +
                    "Record will block retries until TTL expires.", idempotencyKey);
            logger.warn(message, e);
        }
    }

    private void saveWithRetry(String idempotencyKey, IdempotencyRecord completed) {
        try {
            retry.execute(ConnectionException.class, () -> store.save(idempotencyKey, completed));
        } catch (RetryExecutionException e) {
            var message = String.format("Failed to persist completed idempotency record key=%s after retries. " +
                    "Business logic already executed successfully; record may stay stale until TTL expires.", idempotencyKey);
            logger.warn(message, e);
        }
    }
}
