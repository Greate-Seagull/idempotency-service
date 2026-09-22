# Contributing to idempotency-service

Cảm ơn bạn quan tâm đóng góp. Tài liệu này mô tả cấu trúc project, quy ước code, và quy trình đóng góp.

## 1. Nguyên tắc thiết kế Module
- `idempotency-api` chỉ chứa contract (interface, exception, class thuần), **không** thêm dependency ngoài JDK vào module này.
- Implementation cụ thể (Redis, Jackson) nằm ở module riêng, phụ thuộc `idempotency-api`, không ngược lại.
- Wiring Spring (annotation config, `@AutoConfiguration`, `application.yaml`) chỉ nằm ở `idempotency-spring-boot-starter`.

**Lưu ý**: Xem chi tiết lý do thiết kế tại [`docs/architecture.md`](./docs/architecture.md), đặc biệt mục **Rejected Alternatives** trước khi đề xuất thay đổi lớn về kiến trúc (tách microservice, thêm interface mới...), để tránh đề xuất lại hướng đã cân nhắc và loại bỏ.

## 2. Nguyên tắc thiết kế API công khai
- **Chỉ public những gì consumer bắt buộc phải gọi trực tiếp.** Mọi thứ khác giữ package-private. Bài test nhanh: *"Nếu xoá `public` khỏi class này, ví dụ trong README còn compile được không?"*, nếu còn, không cần public.
- **Interface chỉ tạo ở nơi có nhiều biến thể thật.** Không bọc interface cho class chỉ có 1 cách hiện thực hợp lý (xem lý do trong `docs/architecture.md`, mục *Interface riêng cho `IdempotencyService`*).
- **Mọi public class/method là contract tương thích ngược** kể từ khi publish. Đổi signature là breaking change, phải bump MAJOR version và ghi rõ trong `CHANGELOG.md`.
- **Không leak implementation detail vào tên/API của `idempotency-api`**, ví dụ tránh đặt tên gợi ý "cache" hay "Redis" cho type nằm ở `api` module.

## 3. Quy ước code
- **Constructor injection**, không dùng field injection (`@Value` trên field). Validate tham số bắt buộc bằng `Objects.requireNonNull` ngay trong constructor.
- **Immutable theo mặc định**, trừ khi có lý do rõ ràng cần mutable (ví dụ `LazyTask`, xem trade-off trong `docs/architecture.md`). Khi 1 class cố ý mutable, phải có Javadoc giải thích rõ vì sao, tránh gây hiểu lầm.
- **Staged builder** (ép thứ tự gọi tại compile-time) ưu tiên hơn fluent builder thường khi thứ tự các bước có ý nghĩa bắt buộc. Không thêm `@Getter`/`@Setter` (Lombok) tuỳ tiện lên các class dùng pattern này, dễ vô tình mở lỗ hổng bypass staged flow.
- **Không dùng Lombok trong `idempotency-api`**, giữ module này build được với plain `javac`, không ép mọi consumer/maintainer cần Lombok annotation processor.

## 4. Test
### Nhóm test bắt buộc khi thêm/sửa logic core
| Nhóm | Mô tả |
|---|---|
| Concurrency | Nhiều request đồng thời cùng key, chỉ 1 claim thành công |
| Exception handling | Function throw, record bị xoá, exception rethrow nguyên vẹn |
| Duplicate detection | Cùng key khác payload, `IdempotencyConflictException` |
| Not completed | Record chưa hoàn tất, `RequestNotCompletedException` |
| Cached response | Record đã hoàn tất, hash khớp, trả đúng response cache |

### Contract test cho extension point
Nếu bạn viết implementation mới cho `PayloadHasher`, `ResponseSerializer`, hoặc `IdempotencyStore`, **bắt buộc** extend contract test tương ứng trong `testFixtures` của `idempotency-api`:

```java
class MyPayloadHasherTest extends IdempotencyHasherContractTest {
    protected PayloadHasher hasher() { return new MyPayloadHasher(); }
}
```

Thêm dependency:
```kotlin
testImplementation(testFixtures(project(":idempotency-api")))
```

### Chạy test
```bash
./gradlew build test
```

## 5. Commit message, Conventional Commits

Bắt buộc để `release-please` tự sinh `CHANGELOG.md` và version đúng:
```bash
feat(api): add PayloadHasher interface
fix(redis-store): correct ttl not applied on save
feat(api)!: change IdempotencyStore.save signature
BREAKING CHANGE: save() now requires ttl parameter
```

- `feat:`, MINOR bump
- `fix:`, PATCH bump
- `feat!:` hoặc có `BREAKING CHANGE:` trong body, MAJOR bump
- Scope (`(api)`, `(redis-store)`...) khớp tên module bị ảnh hưởng, mỗi module có version/CHANGELOG độc lập.

## 6. Quy trình Pull Request

1. Fork/branch từ `master`.
2. Đảm bảo `./gradlew build test` pass, bao gồm mọi contract test liên quan.
3. Commit theo Conventional Commits.
4. Nếu thay đổi ảnh hưởng kiến trúc/ranh giới module, cập nhật `docs/architecture.md` cùng PR.
5. Mở PR, CI (`build`, `test`, Qodana) phải pass trước khi merge.
6. Merge vào `main`, `release-please` tự mở PR release; maintainer review và merge PR đó để trigger publish.

## 7. Versioning & Publish

- SemVer, version độc lập theo từng module.
- Artifact publish qua GitHub Packages, trigger tự động khi `release-please` tạo GitHub Release.
- Không publish thủ công, mọi release đi qua pipeline `release-please`, tag, CI publish.