package model.helper;

public record Claimable<T>(
        boolean isClaimed,
        T value
) {
    public static <T> Claimable<T> of(boolean success, T value) {
        return new Claimable<>(success, value);
    }
}
