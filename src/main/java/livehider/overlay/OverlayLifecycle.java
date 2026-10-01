package livehider.overlay;

/**
 * Resource-driven overlay lifecycle, independent of Minecraft and native libraries.
 * A failure is terminal until the user explicitly retries; frame ticks never retry it.
 */
public final class OverlayLifecycle {
    public enum State { WAITING, INITIALIZING, READY, FAILED, UNSUPPORTED, CLOSED }

    private State state = State.WAITING;
    private boolean resourcesReady;
    private String failureCode = "NONE";

    public synchronized State state() { return state; }
    public synchronized String failureCode() { return failureCode; }

    public synchronized void resourcesLoading() {
        resourcesReady = false;
        if (state == State.READY || state == State.INITIALIZING) {
            state = State.WAITING;
        }
    }

    public synchronized void resourcesLoaded() {
        resourcesReady = true;
    }

    public synchronized boolean beginAttempt(boolean platformSupported, boolean loadingOverlayVisible) {
        if (state != State.WAITING) return false;
        if (!platformSupported) {
            state = State.UNSUPPORTED;
            failureCode = "UNSUPPORTED_PLATFORM";
            return false;
        }
        if (!resourcesReady || loadingOverlayVisible) return false;
        state = State.INITIALIZING;
        return true;
    }

    public synchronized void initialized() {
        if (state == State.INITIALIZING) {
            state = State.READY;
            failureCode = "NONE";
        }
    }

    public synchronized boolean fail(String code) {
        if (state == State.CLOSED || state == State.FAILED || state == State.UNSUPPORTED) return false;
        state = State.FAILED;
        failureCode = code;
        return true;
    }

    public synchronized boolean retry() {
        if (state != State.FAILED) return false;
        state = State.WAITING;
        failureCode = "NONE";
        return true;
    }

    public synchronized void close() {
        state = State.CLOSED;
    }
}
