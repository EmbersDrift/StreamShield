package livehider.component;

/**
 * A single HUD element that can be redirected to the overlay (stream-hidden).
 * Ported from obs-overlay (MIT, author zziger).
 */
public interface IOverlayComponent {
    boolean canAutoHide();

    String getId();

    boolean isOverlayEnabledDefault();

    boolean isOverlayEnabled();

    void setOverlayEnabled(boolean value);

    boolean isAutoHideEnabled();

    void setAutoHideEnabled(boolean value);

    boolean isHidden();
}
