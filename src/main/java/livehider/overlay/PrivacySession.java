package livehider.overlay;

/** Keeps the last private GUI frame protected across deferred extraction/rendering. */
public final class PrivacySession {
    private boolean active;
    private int releaseFrames;
    public boolean active() { return active; }
    public boolean releasing() { return releaseFrames > 0; }
    public void arm() { active = true; releaseFrames = 0; }
    public void requestResume() { if (active) releaseFrames = 3; }
    public void nextFrame(boolean screenOpen) {
        if (releaseFrames == 0) return;
        if (screenOpen) { releaseFrames = 0; return; }
        if (--releaseFrames == 0) active = false;
    }
}
