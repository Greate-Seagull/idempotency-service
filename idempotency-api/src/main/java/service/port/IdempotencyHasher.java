package service.port;

/**
 * Sinh hash ổn định cho payload dùng làm tiêu chí phát hiện duplicate request.
 *
 * <p><b>Contract bắt buộc mọi implementation phải tuân thủ</b> — vi phạm
 * bất kỳ điều nào dưới đây sẽ khiến 2 payload tương đương về mặt logic
 * bị hash khác nhau, dẫn tới {@code IdempotencyConflictException} giả:
 * <ul>
 *   <li>Trim whitespace ở đầu/cuối mọi giá trị {@code String}</li>
 *   <li>Strip trailing zero cho mọi giá trị số thập phân
 *       (ví dụ {@code 1.50} và {@code 1.5} phải hash giống nhau)</li>
 *   <li>Sắp xếp key của object/map theo alphabet trước khi serialize,
 *       để thứ tự field trong JSON không ảnh hưởng tới hash</li>
 *   <li>Deterministic tuyệt đối: cùng input (sau normalize) phải luôn
 *       cho cùng output, không phụ thuộc thời điểm gọi hay locale</li>
 * </ul>
 */
public interface IdempotencyHasher {
    String hash(Object payload);
}
