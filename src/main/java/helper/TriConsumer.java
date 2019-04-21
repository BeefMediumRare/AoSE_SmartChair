package helper;

@FunctionalInterface
public interface TriConsumer<T,U,S> {
    void consume(T t, U u, S s);
}
