package livehider.overlay;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OverlayLifecycleTest {
    @Test void noNumberOfFramesCanSubstituteForResourceReadiness() {
        OverlayLifecycle lifecycle = new OverlayLifecycle();
        for (int frame = 0; frame < 10000; frame++) assertFalse(lifecycle.beginAttempt(true, false));
        assertEquals(OverlayLifecycle.State.WAITING, lifecycle.state());
        lifecycle.resourcesLoaded();
        assertTrue(lifecycle.beginAttempt(true, false));
        lifecycle.initialized();
        assertEquals(OverlayLifecycle.State.READY, lifecycle.state());
    }

    @Test void waitsForLoadingOverlayToFinishFading() {
        OverlayLifecycle lifecycle = new OverlayLifecycle();
        lifecycle.resourcesLoaded();
        assertFalse(lifecycle.beginAttempt(true, true));
        assertTrue(lifecycle.beginAttempt(true, false));
    }

    @Test void failureDoesNotRetryOnFramesOrResourceReloads() {
        OverlayLifecycle lifecycle = new OverlayLifecycle();
        lifecycle.resourcesLoaded();
        assertTrue(lifecycle.beginAttempt(true, false));
        assertTrue(lifecycle.fail("MH_ENABLE_FAILED"));
        for (int frame = 0; frame < 100; frame++) assertFalse(lifecycle.beginAttempt(true, false));
        lifecycle.resourcesLoading();
        lifecycle.resourcesLoaded();
        assertFalse(lifecycle.beginAttempt(true, false));
        assertEquals("MH_ENABLE_FAILED", lifecycle.failureCode());
        assertFalse(lifecycle.fail("COMPOSITE_FAILED")); // Keep the original error and notify only once.
    }

    @Test void explicitRetryAllowsExactlyOneNewAttempt() {
        OverlayLifecycle lifecycle = new OverlayLifecycle();
        lifecycle.resourcesLoaded();
        lifecycle.beginAttempt(true, false);
        lifecycle.fail("INITIALIZATION_FAILED");
        assertTrue(lifecycle.retry());
        assertTrue(lifecycle.beginAttempt(true, false));
        assertFalse(lifecycle.beginAttempt(true, false));
        lifecycle.initialized();
        assertEquals("NONE", lifecycle.failureCode());
        assertFalse(lifecycle.retry());
    }

    @Test void reloadSuspendsReadyRendererAndRequiresNewReadiness() {
        OverlayLifecycle lifecycle = new OverlayLifecycle();
        lifecycle.resourcesLoaded();
        lifecycle.beginAttempt(true, false);
        lifecycle.initialized();
        lifecycle.resourcesLoading();
        assertEquals(OverlayLifecycle.State.WAITING, lifecycle.state());
        assertFalse(lifecycle.beginAttempt(true, false));
        lifecycle.resourcesLoaded();
        assertTrue(lifecycle.beginAttempt(true, false));
    }

    @Test void unsupportedPlatformIsTerminalAndDoesNotAttemptNativeInitialization() {
        OverlayLifecycle lifecycle = new OverlayLifecycle();
        assertFalse(lifecycle.beginAttempt(false, false));
        lifecycle.resourcesLoaded();
        assertFalse(lifecycle.beginAttempt(false, false));
        assertEquals(OverlayLifecycle.State.UNSUPPORTED, lifecycle.state());
        assertFalse(lifecycle.retry());
    }

    @Test void shutdownCannotBeReactivatedByLateResourceCallback() {
        OverlayLifecycle lifecycle = new OverlayLifecycle();
        lifecycle.close();
        lifecycle.resourcesLoading();
        lifecycle.resourcesLoaded();
        lifecycle.initialized();
        assertFalse(lifecycle.beginAttempt(true, false));
        assertFalse(lifecycle.fail("COMPOSITE_FAILED"));
        assertFalse(lifecycle.retry());
        assertEquals(OverlayLifecycle.State.CLOSED, lifecycle.state());
    }
}
