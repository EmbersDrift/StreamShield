package livehider.component.type;

import net.minecraft.resources.Identifier;

/**
 * A HUD-pass component. Ported from obs-overlay (MIT, author zziger).
 */
public class HUDOverlayComponent extends DefaultOverlayComponent {
    public HUDOverlayComponent(Identifier id, boolean defaultOverlay, boolean canAutoHide) {
        super(id, defaultOverlay, canAutoHide);
    }
}
