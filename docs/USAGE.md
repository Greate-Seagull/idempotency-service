## Quickstart

```java
@Service
@RequiredArgsConstructor
public class PaymentService {
private final IdempotencyService idempotencyService;

    public PaymentResponse charge(ChargeRequest request, String idempotencyKey) {
        var task = LazyTask.of(request, PaymentResponse.class, this::doCharge);
        idempotencyService.ensureIdempotency(idempotencyKey, task);
        return task.getResponse();
    }
}
```

## Recipes

### Sinh idempotency key ở phía client
Key phải được sinh MỘT LẦN và giữ nguyên qua các lần retry — không sinh mới
mỗi lần gửi request. Thường dùng UUID v4 sinh tại thời điểm user bấm submit.

Sai: `UUID.randomUUID().toString()` gọi lại mỗi lần retry → mất tác dụng
idempotency vì mỗi lần đều là key mới.

### Xử lý RequestNotCompletedException
Exception này nghĩa là có request khác cùng key đang xử lý (race condition
bình thường, không phải lỗi hệ thống). Nên retry với backoff ngắn (100-300ms),
không nên trả lỗi ngay cho client.

    catch (RequestNotCompletedException e) {
        // retry sau vài trăm ms, hoặc trả 409 kèm Retry-After header
    }

### Xử lý IdempotencyConflictException
Đây LÀ lỗi client — key bị tái sử dụng cho payload khác. Không retry.
Trả 422/409 kèm message rõ ràng, khuyến khích client sinh key mới.

## Pitfalls

- Đừng dùng key có thể trùng giữa các user khác nhau (ví dụ chỉ dùng
  timestamp) — nên prefix theo user/tenant nếu cần scope.
- Đừng gọi `ensureIdempotency` bên trong 1 transaction DB lớn hơn — nếu
  transaction rollback sau khi Redis đã claim, record sẽ "leak" cho tới
  khi TTL hết hạn.