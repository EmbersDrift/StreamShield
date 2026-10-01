package livehider.overlay;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ScopedRenderStateTest {
    @Test void restoresOtherModsTargetAfterSuccess() {
        var state = new AtomicReference<>("other-mod-target");
        try (var guard = new ScopedRenderState<>(state::get, state::set, "hitbox-only-target")) {
            assertEquals("hitbox-only-target", state.get());
        }
        assertEquals("other-mod-target", state.get());
    }

    @Test void restoresAfterDrawFailure() {
        var state = new AtomicReference<String>(null);
        assertThrows(IllegalStateException.class, () -> {
            try (var guard = new ScopedRenderState<>(state::get, state::set, "hitboxes")) {
                throw new IllegalStateException("failed draw");
            }
        });
        assertNull(state.get());
    }

    @Test void nestedScopesRestoreInOrderAndCloseIsIdempotent() {
        var state = new AtomicReference<>("world");
        try (var outer = new ScopedRenderState<>(state::get, state::set, "outer")) {
            var inner = new ScopedRenderState<>(state::get, state::set, "inner");
            inner.close();
            inner.close();
            assertEquals("outer", state.get());
        }
        assertEquals("world", state.get());
    }
}
