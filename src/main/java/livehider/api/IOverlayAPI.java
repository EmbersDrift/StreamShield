package livehider.api;

import livehider.component.IOverlayComponent;
import livehider.overlay.DummyGuiGraphics;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Public API for the overlay layer (so other mods can redirect their own components).
 * Ported from obs-overlay (MIT, author zziger).
 */
public interface IOverlayAPI {
    default GuiGraphics getGuiGraphics(IOverlayComponent component, GuiGraphics original) {
        return original;
    }

    default GuiGraphics getOverlayGuiGraphics() {
        return DummyGuiGraphics.INSTANCE;
    }
}
