package helper;

import javax.annotation.Nonnull;

public class Wrapper<T> {
    private @Nonnull
    T value_;

    public Wrapper(@Nonnull T value) {
        value_ = value;
    }

    public void set(@Nonnull T value) {
        value_ = value;
    }

    public @Nonnull
    T get() {
        return value_;
    }
}
