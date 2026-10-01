package livehider.overlay;

/** World-only overlay: never composite over screens or resource-loading overlays. */
final class HitboxVisibility {
    private HitboxVisibility() {}

    static boolean canDraw(boolean hasWorld, boolean screenOpen, boolean loadingOverlay) {
        return hasWorld && !screenOpen && !loadingOverlay;
    }
}
