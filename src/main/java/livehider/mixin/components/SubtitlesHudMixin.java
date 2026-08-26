package livehider.mixin.components;

import livehider.LiveHider;
import livehider.component.AllDefaultOverlayComponents;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.SubtitleOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Redirects accessibility subtitles onto the overlay target. Ported from obs-overlay (MIT, author zziger).
 */
@Mixin(SubtitleOverlay.class)
public class SubtitlesHudMixin {
    @ModifyVariable(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V", at = @At("HEAD"), argsOnly = true)
    private GuiGraphicsExtractor drawStart(GuiGraphicsExtractor value) {
        return LiveHider.getAPI().getGuiGraphics(AllDefaultOverlayComponents.subtitles, value);
    }
}
