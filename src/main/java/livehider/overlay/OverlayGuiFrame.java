package livehider.overlay;

/**
 * Separates a new GUI snapshot from display frames that reuse a host client's cached HUD.
 * A completed target remains presentable until a real update replaces it or its context changes.
 */
final class OverlayGuiFrame {
    private boolean invalidated = true;
    private boolean updateRequested;
    private boolean cleared;
    private boolean presentable;

    void beginFrame(boolean contextChanged) {
        if (contextChanged) invalidate();
        updateRequested = invalidated;
        cleared = false;
    }

    void extracted() {
        updateRequested = true;
    }

    boolean needsRender() {
        return updateRequested;
    }

    /** Returns true only for the first pass that needs to clear this frame's target. */
    boolean beginDraw() {
        updateRequested = true;
        presentable = false;
        boolean clear = !cleared;
        cleared = true;
        return clear;
    }

    void endDraw() {
        presentable = true;
        invalidated = false;
    }

    boolean canPresent() {
        return presentable;
    }

    void invalidate() {
        invalidated = true;
        updateRequested = true;
        cleared = false;
        presentable = false;
    }
}
