package livehider.mixin.components;

import livehider.LiveHider;
import livehider.component.AllDefaultOverlayComponents;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Redirects the F3 debug overlay (when enabled) onto the overlay target.
 * Ported from obs-overlay (MIT, author zziger).
 */
@Mixin(DebugScreenOverlay.class)
public class DebugHudMixin {
    @ModifyVariable(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V", at = @At("HEAD"), argsOnly = true, index = 1)
    private GuiGraphicsExtractor drawStart(GuiGraphicsExtractor value) {
        return LiveHider.getAPI().getGuiGraphics(AllDefaultOverlayComponents.debugMenu, value);
    }
}
