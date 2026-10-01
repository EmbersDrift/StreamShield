package livehider.overlay;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** Restores the caller's state, including a previous mod's override, on every exit path. */
final class ScopedRenderState<T> implements AutoCloseable {
    private final T previous;
    private final Consumer<T> setter;
    private boolean closed;

    ScopedRenderState(Supplier<T> getter, Consumer<T> setter, T replacement) {
        this.previous = getter.get();
        this.setter = setter;
        setter.accept(replacement);
    }

    @Override public void close() {
        if (!closed) {
            closed = true;
            setter.accept(previous);
        }
    }
}
