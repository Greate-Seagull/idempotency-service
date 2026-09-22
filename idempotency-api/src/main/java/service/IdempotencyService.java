package service;

import exception.IdempotencyConflictException;
import exception.RequestNotCompletedException;
import model.helper.Claimable;
import model.schema.IdempotencyRecord;
import model.task.LazyTask;
import service.port.IdempotencyHasher;
import service.port.IdempotencySerializer;
import service.port.IdempotencyStore;

import java.time.Duration;
import java.util.Objects;

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
 * hai request là "giống nhau" — client có thể gửi nhầm key cũ với
 * payload khác. Vì vậy request hiện tại còn được hash
 * ({@link IdempotencyHasher}) và so sánh với hash đã lưu; nếu khác,
 * đây được xem là xung đột logic không phải duplicate hợp lệ.
 */
public class IdempotencyService {
    private final IdempotencyStore store;
    private final IdempotencySerializer serializer;
    private final IdempotencyHasher hasher;
    private final Duration ttl;

    public IdempotencyService(IdempotencyStore store, IdempotencySerializer serializer, IdempotencyHasher hasher, Duration ttl) {
        this.store = Objects.requireNonNull(store);
        this.serializer = Objects.requireNonNull(serializer);
        this.hasher = Objects.requireNonNull(hasher);
        this.ttl = Objects.requireNonNull(ttl);
    }

    /**
     * Thực thi {@code function} đúng một lần cho mỗi {@code idempotencyKey},
     * bất kể có bao nhiêu request trùng key được gửi tới.
     *
     * <p>Nếu đây là request đầu tiên với key này, {@code function} sẽ được
     * thực thi và kết quả được lưu lại. Nếu request thất bại (ném exception),
     * record sẽ bị xoá để cho phép retry với cùng key.
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
     */
    public void ensureIdempotency(String idempotencyKey, LazyTask<?, ?> function) {
        var hashedRequest = hasher.hash(function.getRequest());
        Claimable<IdempotencyRecord> result = store.tryClaim(
                idempotencyKey,
                () -> IdempotencyRecord.inProgress(hashedRequest, ttl)
        );

        var recorded = result.value();
        if (result.isClaimed()) {
            try {
                function.computeIfAbsent();
                var response = serializer.serialize(function.getResponse());
                store.save(idempotencyKey, recorded.completed(response));
            } catch (Exception e) {
                store.delete(idempotencyKey);
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
}
