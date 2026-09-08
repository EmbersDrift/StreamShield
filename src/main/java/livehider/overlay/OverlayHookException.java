package livehider.overlay;

/**
 * Thrown when the native swap hook cannot be initialized.
 * Ported from obs-overlay (license: MIT, author zziger).
 */
public class OverlayHookException extends RuntimeException {
    public OverlayHookException(String code) {
        super(code);
    }

    public String getCode() { return getMessage(); }
}
