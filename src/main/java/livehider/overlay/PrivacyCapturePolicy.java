package livehider.overlay;

/** Fail-closed routing decision, separate from GL and UI state for regression testing. */
public final class PrivacyCapturePolicy {
    public enum Route { NORMAL, PRIVATE_OVERLAY, SAFE_RECOVERY, BLOCK }
    private PrivacyCapturePolicy() {}
    public static Route route(boolean protectedSession, boolean ready, boolean safeRecovery) {
        if (!protectedSession) return Route.NORMAL;
        if (ready) return Route.PRIVATE_OVERLAY;
        return safeRecovery ? Route.SAFE_RECOVERY : Route.BLOCK;
    }
}
