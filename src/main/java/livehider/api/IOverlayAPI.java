package livehider.api;

import livehider.component.IOverlayComponent;
import livehider.LiveHiderConfig;
import livehider.overlay.DummyGuiGraphics;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Public API for the overlay layer (so other mods can redirect their own components).
 * Ported from obs-overlay (MIT, author zziger).
 */
public interface IOverlayAPI {
    default GuiGraphicsExtractor getGuiGraphics(IOverlayComponent component, GuiGraphicsExtractor original) {
        LiveHiderConfig config = LiveHiderConfig.get();
        return config != null && config.hideHudWhenOverlayUnavailable && component.isOverlayEnabled()
            ? DummyGuiGraphics.INSTANCE : original;
    }

    default GuiGraphicsExtractor getOverlayGuiGraphics() {
        return DummyGuiGraphics.INSTANCE;
    }
}
