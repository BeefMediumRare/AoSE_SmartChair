package helper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public class WrapperNullable<T> {
    private @Nullable
    T value_;

    public WrapperNullable() {
        value_ = null;
    }

    public WrapperNullable(@Nullable T value) {
        value_ = value;
    }

    public void set(@Nullable T value) {
        value_ = value;
    }

    public @Nonnull
    Optional<T> get() {
        return Optional.ofNullable(value_);
    }
}
