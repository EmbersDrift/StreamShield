package livehider.mixin.components;

import livehider.LiveHider;
import livehider.component.AllDefaultOverlayComponents;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Redirects the TAB player list onto the overlay target. Ported from obs-overlay (MIT, author zziger).
 */
@Mixin(PlayerTabOverlay.class)
public class PlayerListHudMixin {
    @ModifyVariable(
        method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;ILnet/minecraft/world/scores/Scoreboard;Lnet/minecraft/world/scores/Objective;)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private GuiGraphicsExtractor drawStart(GuiGraphicsExtractor value) {
        return LiveHider.getAPI().getGuiGraphics(AllDefaultOverlayComponents.playerList, value);
    }
}
