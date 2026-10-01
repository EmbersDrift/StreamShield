package livehider.overlay;

/** Extraction-thread scope: tags hitbox geometry, never redirects render targets or item draws. */
public final class HitboxCaptureScope implements AutoCloseable {
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);
    private boolean closed;
    private HitboxCaptureScope() { DEPTH.set(DEPTH.get() + 1); }
    public static HitboxCaptureScope enter() { return new HitboxCaptureScope(); }
    public static boolean active() { return DEPTH.get() > 0; }
    @Override public void close() {
        if (closed) return;
        closed = true;
        int depth = DEPTH.get() - 1;
        if (depth == 0) DEPTH.remove(); else DEPTH.set(depth);
    }
}
