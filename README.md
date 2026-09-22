# idempotency-service

Thư viện Java đảm bảo idempotency cho các tác vụ có side-effect, dựa trên cơ chế claim-based và Redis làm store trung tâm.

## Vì sao dùng thư viện này

Khi 1 request (hoặc tác vụ bất kỳ) có thể bị gửi trùng lặp, do client retry, network timeout, hoặc double-click, `idempotency-service` đảm bảo tác vụ đó chỉ thực thi **đúng một lần**, các lần gọi trùng sau sẽ nhận lại kết quả đã cache thay vì chạy lại side-effect (charge tiền, gửi email, tạo record...).

Không dùng Spring? Chỉ cần `idempotency-api` + tự cung cấp implementation.

## Cài đặt

### Spring Boot (khuyến nghị)

```kotlin
dependencies {
    implementation("com.hungvers.idempotency:idempotency-spring-boot-starter:VERSION")
}
```

```yaml
# application.yaml
idempotency:
  ttl: 24h
```

### Không dùng Spring

```kotlin
dependencies {
    implementation("com.hungvers.idempotency:idempotency-api:VERSION")
}
```

```java
var store = new RedisIdempotencyStore(redisTemplate, mapper, Duration.ofHours(24));
var serializer = new JacksonResponseSerializer(mapper);
var hasher = new JacksonPayloadHasher();
var service = new IdempotencyService(store, serializer, hasher, Duration.ofHours(24));
```

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

    private PaymentResponse doCharge(ChargeRequest request) {
        // business logic thật
    }
}
```

`LazyTask` có 4 static factory tuỳ theo tổ hợp input/output:

```java
LazyTask.of(request, ResponseType.class, function)   // có input, có output
LazyTask.noOutput(request, consumer)                  // có input, không output
LazyTask.noInput(ResponseType.class, supplier)        // không input, có output
LazyTask.noInOut(runnable)                             // không input, không output
```

## Xử lý lỗi

| Exception | Ý nghĩa | Nên làm gì |
|---|---|---|
| `RequestNotCompletedException` | Request khác cùng key đang xử lý, chưa xong | Retry với backoff ngắn (100-300ms) |
| `IdempotencyConflictException` | Key trùng nhưng payload khác lần trước | Lỗi client, không retry, yêu cầu key mới |

```java
try {
    return service.execute(request, PaymentResponse.class, this::doCharge);
} catch (RequestNotCompletedException e) {
    // retry sau vài trăm ms, hoặc trả 409 kèm Retry-After
} catch (IdempotencyConflictException e) {
    // trả 422/409, không retry
}
```

## Sinh Idempotency Key

Key phải được sinh **một lần** và giữ nguyên qua các lần retry, không sinh mới mỗi lần gửi request.

```java
// Đúng: UUID sinh một lần lúc user submit, giữ nguyên qua các lần retry
String key = UUID.randomUUID().toString();

// Sai: sinh key mới mỗi lần gọi, mất tác dụng idempotency
UUID.randomUUID().toString() // gọi lại mỗi lần retry
```

Nếu dùng cho REST endpoint multi-tenant, khuyến nghị ghép thêm `tenantId` + `method` + `path` vào key để tránh collision giữa các tenant/endpoint khác nhau, xem ví dụ tại [`docs/architecture.md`](./docs/architecture.md#api-idempotency-key-builder).

## Cấu hình (Spring Boot Starter)

| Property | Default | Mô tả |
|---|---|---|
| `idempotency.ttl` | `24h` | Thời gian record tồn tại trong Redis trước khi tự hết hạn |

## Tài liệu khác

- [`docs/architecture.md`](./docs/architecture.md), cơ chế hoạt động, quyết định thiết kế, lý do lựa chọn công nghệ
- [`CONTRIBUTING.md`](CONTRIBUTING.md), quy ước code, cách chạy test, quy trình đóng góp
- [`CHANGELOG.md`](./CHANGELOG.md), lịch sử thay đổi theo version (mỗi module có CHANGELOG riêng)

## License
Hungvers