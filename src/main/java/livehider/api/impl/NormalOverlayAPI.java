package livehider.api.impl;

import livehider.api.IOverlayAPI;
import livehider.component.IOverlayComponent;
import livehider.overlay.OverlayRenderer;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Live API backed by the overlay renderer. Ported from obs-overlay (MIT, author zziger).
 */
public class NormalOverlayAPI implements IOverlayAPI {
    private final OverlayRenderer renderer;

    public NormalOverlayAPI(OverlayRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public GuiGraphics getGuiGraphics(IOverlayComponent component, GuiGraphics original) {
        return this.renderer.getGuiGraphics(component, original);
    }

    @Override
    public GuiGraphics getOverlayGuiGraphics() {
        return this.renderer.getGuiGraphics();
    }
}
