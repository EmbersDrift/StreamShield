package livehider.mixin.components;

import livehider.LiveHider;
import livehider.component.AllDefaultOverlayComponents;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Redirects chat onto the overlay target. Ported from obs-overlay (MIT, author zziger).
 */
@Mixin(ChatComponent.class)
public class ChatHudMixin {
    @ModifyVariable(
        method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V",
        at = @At("HEAD"),
        argsOnly = true,
        index = 1
    )
    private GuiGraphicsExtractor drawStart(GuiGraphicsExtractor value) {
        return LiveHider.getAPI().getGuiGraphics(AllDefaultOverlayComponents.chat, value);
    }
}
