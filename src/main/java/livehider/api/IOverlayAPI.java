package livehider.api;

import livehider.component.IOverlayComponent;
import livehider.overlay.DummyGuiGraphics;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Public API for the overlay layer (so other mods can redirect their own components).
 * Ported from obs-overlay (MIT, author zziger).
 */
public interface IOverlayAPI {
    default GuiGraphicsExtractor getGuiGraphics(IOverlayComponent component, GuiGraphicsExtractor original) {
        return original;
    }

    default GuiGraphicsExtractor getOverlayGuiGraphics() {
        return DummyGuiGraphics.INSTANCE;
    }
}
