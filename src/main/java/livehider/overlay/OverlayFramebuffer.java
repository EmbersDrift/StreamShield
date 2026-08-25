package livehider.overlay;

import com.mojang.blaze3d.pipeline.RenderTarget;
import org.jetbrains.annotations.NotNull;

/**
 * Wraps the off-screen overlay target drawn to by redirected HUD components.
 * Ported from obs-overlay (MIT, author zziger).
 */
public final class OverlayFramebuffer {
    @NotNull
    public final RenderTarget object;
    public boolean dirty;

    OverlayFramebuffer(@NotNull RenderTarget object) {
        this.object = object;
        this.dirty = false;
    }
}
