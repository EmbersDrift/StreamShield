package livehider.overlay;

import org.junit.jupiter.api.Test;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.*;

class HitboxCaptureScopeTest {
    @Test void scopeIsLimitedToHitboxExtraction() {
        assertFalse(HitboxCaptureScope.active());
        try (var scope = HitboxCaptureScope.enter()) { assertTrue(HitboxCaptureScope.active()); }
        assertFalse(HitboxCaptureScope.active());
    }

    @Test void nestedScopesAndDoubleCloseDoNotLeak() {
        try (var outer = HitboxCaptureScope.enter()) {
            var inner = HitboxCaptureScope.enter();
            inner.close();
            inner.close();
            assertTrue(HitboxCaptureScope.active());
        }
        assertFalse(HitboxCaptureScope.active());
    }

    @Test void anotherModsExceptionDoesNotTagSubsequentItemsOrGizmos() {
        assertThrows(IllegalStateException.class, () -> {
            try (var scope = HitboxCaptureScope.enter()) { throw new IllegalStateException("test"); }
        });
        assertFalse(HitboxCaptureScope.active());
    }

    @Test void renderThreadsDoNotShareCaptureScope() {
        try (var scope = HitboxCaptureScope.enter()) {
            assertFalse(CompletableFuture.supplyAsync(HitboxCaptureScope::active).join());
        }
    }
}
